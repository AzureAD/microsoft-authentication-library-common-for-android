// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.common.internal.ui.webview.challengehandlers;

import static com.yubico.yubikit.piv.Slot.AUTHENTICATION;
import static com.yubico.yubikit.piv.Slot.CARD_AUTH;
import static com.yubico.yubikit.piv.Slot.KEY_MANAGEMENT;
import static com.yubico.yubikit.piv.Slot.SIGNATURE;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.security.KeyChain;
import android.security.KeyChainAliasCallback;
import android.security.KeyChainException;
import android.util.Log;
import android.webkit.ClientCertRequest;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.microsoft.identity.common.logging.Logger;
import com.yubico.yubikit.android.YubiKitManager;
import com.yubico.yubikit.android.transport.usb.UsbConfiguration;
import com.yubico.yubikit.android.transport.usb.UsbYubiKeyDevice;
import com.yubico.yubikit.android.transport.usb.connection.UsbSmartCardConnection;
import com.yubico.yubikit.core.application.ApplicationNotAvailableException;
import com.yubico.yubikit.core.application.BadResponseException;
import com.yubico.yubikit.core.smartcard.ApduException;
import com.yubico.yubikit.core.smartcard.SmartCardConnection;
import com.yubico.yubikit.core.util.Callback;
import com.yubico.yubikit.core.util.Result;
import com.yubico.yubikit.piv.InvalidPinException;
import com.yubico.yubikit.piv.PivSession;
import com.yubico.yubikit.piv.Slot;
import com.yubico.yubikit.piv.jca.PivPrivateKey;
import com.yubico.yubikit.piv.jca.PivProvider;

import java.io.IOException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles Certificate Based Authentication by means of certificates provisioned onto YubiKeys or the devices themselves.
 * Note that CBA requires API >= 21 (YubiKit SDK min API = 19; ClientCertRequest class available with API >= 21)
 */
public final class ClientCertAuthChallengeHandler implements IChallengeHandler<ClientCertRequest, Void> {
    private static final String TAG = ClientCertAuthChallengeHandler.class.getSimpleName();
    private static final String ACCEPTABLE_ISSUER = "CN=MS-Organization-Access";
    private final Activity mActivity;
    private final YubiKitManager mYubiKitManager;
    private UsbYubiKeyDevice mDevice;
    private SmartcardDialog mCurrentDialog;
    //Locks to help facilitate synchronization
    private static final Object deviceLock = new Object();
    private static final Object smartcardDialogLock = new Object();

    /**
     * Nested class to hold details of a certificate needed for the certificate picker, including subject, issuer, and slot.
     */
    public static class YubiKitCertDetails {
        private final String issuerText;
        private final String subjectText;
        private final Slot slot;

        public YubiKitCertDetails(String issuerText, String subjectText, Slot slot) {
            this.issuerText = issuerText;
            this.subjectText = subjectText;
            this.slot = slot;
        }

        public String getIssuerText() {
            return issuerText;
        }

        public String getSubjectText() {
            return subjectText;
        }

        public Slot getSlot() {
            return slot;
        }
    }

    /**
     * Creates new instance of ClientCertAuthChallengeHandler and starts usb discovery for YubiKeys.
     * @param activity current host activity.
     */
    public ClientCertAuthChallengeHandler(@NonNull final Activity activity) {
        mActivity = activity;
        //Create and start YubiKitManager for UsbDiscovery mode.
        //When in Usb Discovery mode, Yubikeys that plug into the device will be accessible
        // once the user provides permission via the Android permission dialog.
        mYubiKitManager = new YubiKitManager(mActivity.getApplicationContext());
        mYubiKitManager.startUsbDiscovery(new UsbConfiguration(), new Callback<UsbYubiKeyDevice>() {
            @Override
            public void invoke(@NonNull UsbYubiKeyDevice device) {
                Logger.verbose(TAG, "A YubiKey device was connected");
                synchronized (deviceLock) {
                    //set device
                    mDevice = device;
                    mDevice.setOnClosed(new Runnable() {
                        @Override
                        public void run() {
                            synchronized (deviceLock) {
                                Logger.verbose(TAG, "A YubiKey device was disconnected");
                                mDevice = null;
                                //Call onCancel on current dialog, if dialog is showing.
                                synchronized (smartcardDialogLock) {
                                    if (mCurrentDialog != null) {
                                        mCurrentDialog.onCancelCba();
                                        //Reset dialog to null
                                        mCurrentDialog = null;
                                        //TODO: maybe show a dialog that tells user they unplugged too early? I'm not sure this is necessary since I'm thinking in a lot of cases, the user wanted to cancel current flow by unplugging.
                                        Logger.verbose(TAG, "YubiKey was disconnected while dialog was still displayed.");
                                    }
                                }
                            }
                        }
                    });
                }
            }
        });
    }

    /**
     * Called when a ClientCertRequest is received by the AzureActiveDirectoryWebViewClient.
     * Prompts the user to choose a certificate to authenticate with based on whether or not a YubiKey is plugged in and has permission to be connected.
     * @param request ClientCertRequest received from AzureActiveDirectoryWebViewClient.onReceivedClientCertRequest.
     * @return null in either case.
     */
    @Override
    public Void processChallenge(@NonNull final ClientCertRequest request) {
        //final String methodTag = TAG + ":processChallenge";
        synchronized (deviceLock) {
            if (mDevice != null) {
                handleSmartcardCertAuth(request);
                return null;
            }
        }
        //Else, proceed with user certificates stored on device.
        handleOnDeviceCertAuth(request);
        return null;
    }

    /**
     * Collects YubiKitCertDetails from PIV certificates on YubiKey.
     * If certificates are found on YubiKey, the method proceeds with the smartcard certificate picker.
     * Otherwise, appropriate error dialogs are shown.
     * @param request ClientCertRequest received from AzureActiveDirectoryWebViewClient.onReceivedClientCertRequest.
     */
    private void handleSmartcardCertAuth(@NonNull final ClientCertRequest request) {
        final String methodTag = TAG + ":handleSmartcardCertAuth";
        //A connection to the YubiKey needs to be made in order to read the certificates off it.
        mDevice.requestConnection(UsbSmartCardConnection.class, new Callback<Result<UsbSmartCardConnection, IOException>>() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void invoke(@NonNull Result<UsbSmartCardConnection, IOException> value) {
                try {
                    final SmartCardConnection c = value.getValue();
                    PivSession piv = new PivSession(c);
                    //Check if too many PIN attempts have been made.
                    if (piv.getPinAttempts() == 0) {
                        showMaxAttemptsDialog();
                        request.cancel();
                        Logger.info(methodTag,  "User has reached the maximum failed attempts allowed.");
                        return;
                    }
                    //Create List that contains cert details only pertinent to the cert picker.
                    final List<YubiKitCertDetails> certList = getCertDetailsFromKey(piv);
                    //If no certs were found, cancel flow.
                    if (certList.isEmpty()) {
                        request.cancel();
                        Logger.info(methodTag,  "No PIV certificates found on YubiKey device.");
                        //TODO: show error dialog stating that no PIV certificates were found on the YubiKey.
                        return;
                    }
                    //Build and show Smartcard cert picker, which also handles the rest of the smartcard CBA flow.
                    showSmartcardCertPickerDialog(certList, request);

                } catch(final IOException e) {
                    Logger.error(methodTag, "IOException", e);
                    request.cancel();
                    //TODO: create and show error dialog here.
                } catch (final ApduException e) {
                    Logger.error(methodTag, "ApduException", e);
                    request.cancel();
                    //TODO: create and show error dialog here.
                } catch (final ApplicationNotAvailableException e) {
                    Logger.error(methodTag, "ApplicationNotAvailableException", e);
                    request.cancel();
                    //TODO: create and show error dialog here.
                } catch (BadResponseException e) {
                    Logger.error(methodTag, "BadResponseException", e);
                    request.cancel();
                    //TODO: create and show error dialog here.
                }
            }
        });
    }

    /**
     * Helper method that returns a list of YubiKitCertDetails extracted from certificates located on the YubiKey.
     * This method should only be called within a callback upon creating a successful YubiKey device connection.
     *
     * @param piv A PivSession created from a SmartCardConnection that can interact with certificates located in the PIV slots on the YubiKey.
     * @return A List holding the YubiKitCertDetails of the certificates found on the YubiKey.
     * @throws IOException          in case of connection error
     * @throws ApduException        in case of an error response from the YubiKey
     * @throws BadResponseException in case of incorrect YubiKey response
     */
    private List<YubiKitCertDetails> getCertDetailsFromKey(@NonNull final PivSession piv) throws IOException, ApduException, BadResponseException {
        //Create ArrayList that contains cert details only pertinent to the cert picker.
        final List<YubiKitCertDetails> certList = new ArrayList<>();
        //We need to check all four PIV slots.
        //AUTHENTICATION (9A)
        getAndPutCertDetailsInList(AUTHENTICATION, piv, certList);
        //SIGNATURE (9C)
        getAndPutCertDetailsInList(SIGNATURE, piv, certList);
        //KEY_MANAGEMENT (9D)
        getAndPutCertDetailsInList(KEY_MANAGEMENT, piv, certList);
        //CARD_AUTH (9E)
        getAndPutCertDetailsInList(CARD_AUTH, piv, certList);

        return certList;
    }

    /**
     * Helper method that handles reading certificates off YubiKey.
     * This method should only be called within a callback upon creating a successful YubiKey device connection.
     * @param slot A PIV slot from which to read the certificate.
     * @param piv A PivSession created from a SmartCardConnection that can interact with certificates located in the PIV slots on the YubiKey.
     * @param certList A List collecting the YubiKitCertDetails of the certificates found on the YubiKey.
     * @throws IOException          in case of connection error
     * @throws ApduException        in case of an error response from the YubiKey
     * @throws BadResponseException in case of incorrect YubiKey response
     */
    private void getAndPutCertDetailsInList(@NonNull final Slot slot, @NonNull final PivSession piv, @NonNull final List<YubiKitCertDetails> certList) throws IOException, ApduException, BadResponseException {
        final String methodTag = TAG + ":getAndPutCertDetailsInList";
        try {
            final X509Certificate cert =  piv.getCertificate(slot);
            //If there are no exceptions, add details of this cert to our certList.
            certList.add(new YubiKitCertDetails(cert.getIssuerDN().getName(), cert.getSubjectDN().getName(), slot));
        } catch (final ApduException e) {
            //If sw is 0x6a82 (27266), This is a FILE_NOT_FOUND error, which we should ignore since this means the slot is merely empty.
            if (e.getSw() == 0x6a82) {
                Logger.verbose(methodTag, slot + " slot is empty.");
            } else {
                throw e;
            }
        }
    }

    /**
     * Build and show Smartcard certificate picker, which also handles the rest of the smartcard CBA flow.
     * @param certList A List holding the YubiKitCertDetails of the certificates found on the YubiKey.
     * @param request ClientCertRequest received from AzureActiveDirectoryWebViewClient.onReceivedClientCertRequest.
     */
    private void showSmartcardCertPickerDialog(@NonNull final List<YubiKitCertDetails> certList, @NonNull final ClientCertRequest request) {
        final SmartcardCertPickerDialog certPickerDialog = new SmartcardCertPickerDialog(certList, mActivity);
        certPickerDialog.setPositiveButtonListener(getSmartcardCertPickerDialogPositiveButtonListener(request));
        certPickerDialog.setNegativeButtonListener(new SmartcardCertPickerDialog.NegativeButtonListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick() {
                request.cancel();
                synchronized (smartcardDialogLock) {
                    mCurrentDialog = null;
                }
            }
        });
        certPickerDialog.setCancelCbaCallback(new SmartcardCertPickerDialog.CancelCbaCallback() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onCancel() {
                request.cancel();
            }
        });
        //Set current dialog to certPickerDialog and show.
        synchronized (smartcardDialogLock) {
            mCurrentDialog = certPickerDialog;
            certPickerDialog.show();
        }
    }

    /**
     * Upon a positive button click in the certificate picker, the listener will proceed with a PIN prompt dialog.
     * @param request ClientCertRequest received from AzureActiveDirectoryWebViewClient.onReceivedClientCertRequest.
     * @return A PositiveButtonListener to be set for a SmartcardCertPickerDialog.
     */
    private SmartcardCertPickerDialog.PositiveButtonListener getSmartcardCertPickerDialogPositiveButtonListener(@NonNull final ClientCertRequest request) {
        //final String methodTag = TAG + ":getSmartcardCertPickerDialogPositiveButtonListener";
        return new SmartcardCertPickerDialog.PositiveButtonListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(final YubiKitCertDetails certDetails) {
                //Need to prompt user for pin and verify pin. The positive button listener will handle the rest of the CBA flow.
                showPinDialog(certDetails, request);
            }
        };
    }

    /**
     * Build and show smartcard PIN dialog.
     * @param certDetails YubiKitCertDetails of the selected certificate from the SmartcardCertPickerDialog.
     * @param request ClientCertRequest received from AzureActiveDirectoryWebViewClient.onReceivedClientCertRequest.
     */
    private void showPinDialog(@NonNull final YubiKitCertDetails certDetails, @NonNull final ClientCertRequest request) {
        final SmartcardPinDialog pinDialog = new SmartcardPinDialog(mActivity);
        pinDialog.setPositiveButtonListener(getSmartcardPinDialogPositiveButtonListener(certDetails, request));
        pinDialog.setNegativeButtonListener(new SmartcardPinDialog.NegativeButtonListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick() {
                request.cancel();
            }
        });
        pinDialog.setCancelCbaCallback(new SmartcardPinDialog.CancelCbaCallback() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onCancel() {
                request.cancel();
            }
        });
        //Set currentDialog to pinDialog and show.
        synchronized (smartcardDialogLock) {
            mCurrentDialog = pinDialog;
            //Shows dialog and sets up other UI components.
            pinDialog.show();
        }
    }

    /**
     * Upon a positive button click in the smartcard PIN dialog, verify the provided PIN and handle the results.
     * @param certDetails YubiKitCertDetails of the selected certificate from the SmartcardCertPickerDialog.
     * @param request ClientCertRequest received from AzureActiveDirectoryWebViewClient.onReceivedClientCertRequest.
     * @return A PositiveButtonListener to be set for a SmartcardPinDialog.
     */
    private SmartcardPinDialog.PositiveButtonListener getSmartcardPinDialogPositiveButtonListener(@NonNull final YubiKitCertDetails certDetails, @NonNull final ClientCertRequest request) {
        final String methodTag = TAG + ":getSmartcardPinDialogPositiveButtonListener";

        return new SmartcardPinDialog.PositiveButtonListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(final String pin) {
                synchronized (deviceLock) {
                    // Need to request a SmartCardConnection in order to access certs on YubiKey.
                    mDevice.requestConnection(UsbSmartCardConnection.class, new Callback<Result<UsbSmartCardConnection, IOException>>() {
                        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                        @Override
                        public void invoke(@NonNull Result<UsbSmartCardConnection, IOException> value) {
                            try {
                                final SmartCardConnection c = value.getValue();
                                final PivSession piv = new PivSession(c);
                                //Verify PIN and handle results
                                verifySmartcardPin(pin.toCharArray(), certDetails, request, piv);
                            } catch(final IOException e) {
                                Logger.error(methodTag, "IOException", e);
                                CancelCbaAndResetCurrentDialog();
                                //TODO: create and show error dialog here.
                            } catch (final ApduException e) {
                                Logger.error(methodTag, "ApduException", e);
                                CancelCbaAndResetCurrentDialog();
                                //TODO: create and show error dialog here.
                            } catch (final ApplicationNotAvailableException e) {
                                Logger.error(methodTag, "ApplicationNotAvailableException", e);
                                CancelCbaAndResetCurrentDialog();
                                //TODO: create and show error dialog here.
                            } catch (final BadResponseException e) {
                                Logger.error(methodTag, "BadResponseException", e);
                                CancelCbaAndResetCurrentDialog();
                                //TODO: create and show error dialog here.
                            }
                        }
                    });
                }
            }
        };

    }

    /**
     * Call current dialog's onCancelCba method and reset it back to null.
     */
    private void CancelCbaAndResetCurrentDialog() {
        synchronized (smartcardDialogLock) {
            if (mCurrentDialog != null) {
                mCurrentDialog.onCancelCba();
                mCurrentDialog = null;
            }
        }
    }

    /**
     * Checks to see if PIN for smartcard is correct.
     * If so, proceed to attempt authentication.
     * Otherwise, handle the incorrect PIN based on how many PIN attempts are remaining.
     * @param pin char array containing PIN attempt.
     * @param certDetails YubiKitCertDetails of the selected certificate from the SmartcardCertPickerDialog.
     * @param request ClientCertRequest received from AzureActiveDirectoryWebViewClient.onReceivedClientCertRequest.
     * @param piv A PivSession created from a SmartCardConnection that can interact with certificates located in the PIV slots on the YubiKey.
     * @throws IOException          in case of connection error
     * @throws ApduException        in case of an error response from the YubiKey
     * @throws BadResponseException in case of incorrect YubiKey response
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void verifySmartcardPin(@NonNull final char[] pin, @NonNull final YubiKitCertDetails certDetails, @NonNull final ClientCertRequest request, @NonNull final PivSession piv) throws IOException, ApduException, BadResponseException {
        final String methodTag = TAG + ":verifySmartcardPin";
        //Call YubiKit method to verify PIN.
        try {
            piv.verifyPin(pin);
            //If pin is successfully verified, we will get the certificate and perform the rest of the logic for authentication.
            X509Certificate cert = piv.getCertificate(certDetails.getSlot());
            //TODO: do a confirmation that the cert retrieved above is the same cert that was picked earlier by comparing with YubiKitCertDetails
            //TODO: Complete authentication using cert and YubiKit sdk.
            //Keystore
            PivProvider pivProvider = new PivProvider(piv);
            Security.insertProviderAt(pivProvider, 1);
            KeyStore keyStore = KeyStore.getInstance("YKPiv", pivProvider);
            keyStore.load(null);
            //try private key
            Slot slot = certDetails.getSlot();
            String alias = slot.getStringAlias();
            PivPrivateKey privateKey = (PivPrivateKey) keyStore.getKey(alias, pin);
            //X509Certificate[] chain = (X509Certificate[]) keyStore.getCertificateChain(alias); <- This line ends up blocking on line 129 of UsbYubiKeyDevice
            X509Certificate[] chain = new X509Certificate[]{cert};
            synchronized (smartcardDialogLock) {
                mCurrentDialog.dismiss();
                mCurrentDialog = null;
            }
            request.proceed(privateKey, chain);

        } catch (final InvalidPinException e) {
            // An incorrect Pin attempt.
            // We need to retrieve the number of pin attempts before proceeding.
            int pinAttemptsRemaining = piv.getPinAttempts();
            // If the number of attempts is 0, no more attempts will be allowed.
            if (pinAttemptsRemaining == 0) {
                //We must display a dialog informing the user that they have made too many incorrect attempts,
                // and the user will need to figure out a way to reset their key outside of our library.
                request.cancel();
                synchronized (smartcardDialogLock) {
                    mCurrentDialog.dismiss();
                }
                showMaxAttemptsDialog();
                Logger.info(methodTag,  "User has reached the maximum failed attempts allowed.");
            } else {
                //Update Dialog to indicate that an incorrect attempt was made.
                synchronized (smartcardDialogLock) {
                    ((SmartcardPinDialog) mCurrentDialog).setErrorMode();
                }
            }
        } catch (UnrecoverableKeyException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sets up and shows dialog when user has exceeded the max attempts allowed to enter their YubiKey PIN.
     */
    private void showMaxAttemptsDialog(){
        //final String methodTag = TAG + ":showMaxAttemptsDialog";
        SmartcardMaxFailedAttemptsDialog maxFailedAttemptsDialog = new SmartcardMaxFailedAttemptsDialog(mActivity);
        maxFailedAttemptsDialog.setPositiveButtonListener(new SmartcardMaxFailedAttemptsDialog.PositiveButtonListener() {
            @Override
            public void onClick() {
                //Reset currentDialog to null
                synchronized (smartcardDialogLock) {
                    mCurrentDialog = null;
                }
            }
        });
        //set current dialog to maxFailedAttemptsDialog and show.
        synchronized (smartcardDialogLock) {
            mCurrentDialog = maxFailedAttemptsDialog;
            maxFailedAttemptsDialog.show();
        }
    }

    /**
     * Handles the logic for on-device certificate based authentication.
     * Makes use of Android's KeyChain.choosePrivateKeyAlias method, which shows a certificate picker that allows users to choose their on-device user certificate to authenticate with.
     * @param request ClientCertRequest received from AzureActiveDirectoryWebViewClient.onReceivedClientCertRequest.
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void handleOnDeviceCertAuth(@NonNull final ClientCertRequest request) {
        final String methodTag = TAG + ":handleOnDeviceCertAuth";
        final Principal[] acceptableCertIssuers = request.getPrincipals();

        // When ADFS server sends null or empty issuers, we'll continue with cert prompt.
        if (acceptableCertIssuers != null) {
            for (Principal issuer : acceptableCertIssuers) {
                if (issuer.getName().contains(ACCEPTABLE_ISSUER)) {
                    //Checking if received acceptable issuers contain "CN=MS-Organization-Access"
                    Logger.info(methodTag,"Cancelling the TLS request, not respond to TLS challenge triggered by device authentication.");
                    request.cancel();
                    return;
                }
            }
        }

        KeyChain.choosePrivateKeyAlias(mActivity, new KeyChainAliasCallback() {
                    @Override
                    public void alias(String alias) {
                        if (alias == null) {
                            Logger.info(methodTag,"No certificate chosen by user, cancelling the TLS request.");
                            request.cancel();
                            return;
                        }

                        try {
                            final X509Certificate[] certChain = KeyChain.getCertificateChain(
                                    mActivity.getApplicationContext(), alias);
                            final PrivateKey privateKey = KeyChain.getPrivateKey(
                                    mActivity, alias);

                            Logger.info(methodTag,"Certificate is chosen by user, proceed with TLS request.");
                            request.proceed(privateKey, certChain);
                            return;
                        } catch (final KeyChainException e) {
                            Logger.errorPII(methodTag,"KeyChain exception", e);
                        } catch (final InterruptedException e) {
                            Logger.errorPII(methodTag,"InterruptedException exception", e);
                        }

                        request.cancel();
                    }
                },
                request.getKeyTypes(),
                request.getPrincipals(),
                request.getHost(),
                request.getPort(),
                null);
    }

    /**
     * Allows AzureActiveDirectoryWebViewClient to stop the local YubiKitManager's discovery mode.
     */
    public void stopYubiKitManagerUsbDiscovery() {
        //Stop UsbDiscovery for YubiKitManager
        //Should be called when host fragment is destroyed.
        mYubiKitManager.stopUsbDiscovery();
    }
}