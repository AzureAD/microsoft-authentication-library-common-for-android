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

import java.io.IOException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

//Handles Certificate Based Authentication by means of certificates provisioned onto YubiKeys or the devices themselves.
//Note that CBA requires API >= 21 (YubiKit SDK min API = 19; ClientCertRequest class available with API >= 21)
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

    //creating nested class to hold details of a Certificate needed for the picker,
    // including subject, issuer, and slot.
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

    public ClientCertAuthChallengeHandler(@NonNull final Activity activity) {
        mActivity = activity;
        synchronized (deviceLock) {
            //set mDevice to null
            mDevice = null;
        }

        //Set mCurrentDialog to null
        synchronized (smartcardDialogLock) {
            mCurrentDialog = null;
        }
        //Create and start YubiKitManager for UsbDiscovery mode.
        //When in Usb Discovery mode, Yubikeys that plug into the device will be accessible
        // once the user provides permission via the Android permission dialog.
        mYubiKitManager = new YubiKitManager(mActivity.getApplicationContext());
        mYubiKitManager.startUsbDiscovery(new UsbConfiguration(), new Callback<UsbYubiKeyDevice>() {
            @Override
            public void invoke(@NonNull UsbYubiKeyDevice device) {
                Logger.info(TAG, "A YubiKey device was connected");
                synchronized (deviceLock) {
                    //set device
                    mDevice = device;
                    mDevice.setOnClosed(new Runnable() {
                        @Override
                        public void run() {
                            synchronized (deviceLock) {
                                Logger.info(TAG, "A YubiKey device was disconnected");
                                mDevice = null;
                                //Call onCancel on current dialog, if dialog is showing.
                                synchronized (smartcardDialogLock) {
                                    if (mCurrentDialog != null) {
                                        mCurrentDialog.onCancelCba();
                                        //Reset dialog to null
                                        mCurrentDialog = null;
                                    }
                                }
                            }
                        }
                    });
                }
            }
        });
    }

    //Called when a ClientCertRequest is received by the WebViewClient.
    @Override
    public Void processChallenge(@NonNull final ClientCertRequest request) {
        //final String methodTag = TAG + ":processChallenge";
        processChallengeInternal(request);
        return null;
    }
    //Offers the user to choose a certificate to authenticate with based on whether or not a YubiKey is plugged in and has permission to be connected.
    private void processChallengeInternal(@NonNull final ClientCertRequest request) {
        //final String methodTag = TAG + ":processChallengeInternal";
        // Check if a YubiKey device has been discovered.
        synchronized (deviceLock) {
            if (mDevice != null) {
                handleSmartcardCertAuth(request);
                return;
            }
        }
        //Else, proceed with user certificates stored on device.
        handleOnDeviceCertAuth(request);
    }

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
                        Logger.infoPII(methodTag,  "User has reached the maximum failed attempts allowed.");
                        return;
                    }
                    //Create List that contains cert details only pertinent to the cert picker.
                    final List<YubiKitCertDetails> certList = getCertDetailsFromKey(piv);
                    //If no certs were found, cancel flow.
                    if (certList.isEmpty()) {
                        request.cancel();
                        Logger.infoPII(methodTag,  "No PIV certificates found on YubiKey device.");
                        return;
                    }
                    //Build and show Smartcard cert picker, which also handles the rest of the smartcard CBA flow.
                    showSmartcardCertPickerDialog(certList, request);

                } catch(IOException e) {
                    Logger.errorPII(methodTag, "IOException", e);
                    request.cancel();
                } catch (ApduException e) {
                    Logger.errorPII(methodTag, "ApduException", e);
                    request.cancel();
                } catch (ApplicationNotAvailableException e) {
                    Logger.errorPII(methodTag, "ApplicationNotAvailableException", e);
                    request.cancel();
                }
            }
        });
    }

    //Helper method that returns a list of YubiKitCertDetails extracted from certificates located on the YubiKey.
    //This method should only be called within a callback upon creating a successful YubiKey device connection.
    private List<YubiKitCertDetails> getCertDetailsFromKey(@NonNull final PivSession piv) {
        //Create ArrayList that contains cert details only pertinent to the cert picker.
        List<YubiKitCertDetails> certList = new ArrayList<>();
        //We need to check every slot. If a slot is empty, an ApduException is called, in which case, we'll ignore it.
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

    //Helper method created to handle reading certificates off YubiKey.
    //This method should only be called within a callback upon creating a successful YubiKey device connection.
    private void getAndPutCertDetailsInList(@NonNull final Slot slot, @NonNull final PivSession piv, @NonNull final List<YubiKitCertDetails> certList) {
        final String methodTag = TAG + ":getAndPutCertDetailsInList";
        try {
            final X509Certificate cert =  piv.getCertificate(slot);
            //If there are no exceptions, add details of this cert to our certList.
            certList.add(new YubiKitCertDetails(cert.getIssuerDN().getName(), cert.getSubjectDN().getName(), slot));
        } catch (IOException e) {
            Logger.errorPII(methodTag,"IOException", e);
        } catch (BadResponseException e) {
            Logger.errorPII(methodTag,"BadResponseException", e);
        } catch (ApduException e) {
            //If sw is 0x6a82 (27266), This is a FILE_NOT_FOUND error, which we should ignore since this means the slot is merely empty.
            if (e.getSw() != 0x6a82) {
                Logger.errorPII(methodTag,"ApduException", e);
            }
        }
    }
    //Build and show Smartcard cert picker, which also handles the rest of the smartcard CBA flow.
    private void showSmartcardCertPickerDialog(@NonNull final List<YubiKitCertDetails> certList, @NonNull final ClientCertRequest request) {
        final SmartcardCertPickerDialog certPickerDialog = new SmartcardCertPickerDialog(certList, mActivity);
        certPickerDialog.setPositiveButtonListener(getSmartcardCertPickerDialogPositiveButtonListener(request));
        certPickerDialog.setNegativeButtonListener(new SmartcardCertPickerDialog.NegativeButtonListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick() {
                request.cancel();
                mCurrentDialog = null;
            }
        });
        certPickerDialog.setCancelCbaCallback(new SmartcardCertPickerDialog.CancelCbaCallback() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onCancel() {
                request.cancel();
            }
        });
        //Show the dialog.
        certPickerDialog.show();
        //Set current dialog to certPickerDialog
        synchronized (smartcardDialogLock) {
            mCurrentDialog = certPickerDialog;
        }
    }

    //Upon a positive button click in the cert picker, sets up the PIN prompt dialog.
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

    //Build and show PIN dialog.
    private void showPinDialog(@NonNull final YubiKitCertDetails certDetails, @NonNull final ClientCertRequest request) {
        final SmartcardPinDialog pinDialog = new SmartcardPinDialog(mActivity);
        pinDialog.setPositiveButtonListener(getSmartcardPinDialogPositiveButtonListener(certDetails, request));
        pinDialog.setNegativeButtonListener(getSmartcardPinDialogNegativeButtonListener(request));
        pinDialog.setCancelCbaCallback(new SmartcardPinDialog.CancelCbaCallback() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onCancel() {
                request.cancel();
            }
        });
        //Shows dialog and sets up other UI components.
        pinDialog.show();
        //Set currentDialog to pinDialog
        synchronized (smartcardDialogLock) {
            mCurrentDialog = pinDialog;
        }
    }

    //Upon a positive button click in the PIN prompt, verify the provided PIN and handle the results.
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
                            } catch(IOException e) {
                                Logger.errorPII(methodTag, "IOException", e);
                                request.cancel();
                                synchronized (smartcardDialogLock) {
                                    mCurrentDialog.dismiss();
                                }
                                mCurrentDialog = null;
                            } catch (ApduException e) {
                                Logger.errorPII(methodTag, "ApduException", e);
                                request.cancel();
                                synchronized (smartcardDialogLock) {
                                    mCurrentDialog.dismiss();
                                }
                                mCurrentDialog = null;
                            } catch (ApplicationNotAvailableException e) {
                                Logger.errorPII(methodTag, "ApplicationNotAvailableException", e);
                                request.cancel();
                                synchronized (smartcardDialogLock) {
                                    mCurrentDialog.dismiss();
                                }
                                mCurrentDialog = null;
                            }
                        }
                    });
                }
            }
        };

    }

    //Upon a negative button click in the PIN prompt, cancel flow and dismiss dialog.
    private SmartcardPinDialog.NegativeButtonListener getSmartcardPinDialogNegativeButtonListener(@NonNull final ClientCertRequest request) {
        //final String methodTag = TAG + ":getSmartcardPinDialogNegativeButtonListener";
        return new SmartcardPinDialog.NegativeButtonListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick() {
                request.cancel();
                synchronized (smartcardDialogLock) {
                    mCurrentDialog.dismiss();
                }
                mCurrentDialog = null;
            }
        };
    }

    //Check to see if PIN for smartcard is correct.
    //If so, proceed to attempt authentication.
    //Otherwise, handle the incorrect PIN based on how many PIN attempts are remaining.
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void verifySmartcardPin(@NonNull final char[] pin, @NonNull final YubiKitCertDetails certDetails, @NonNull final ClientCertRequest request, @NonNull final PivSession piv) throws ApduException, IOException {
        final String methodTag = TAG + ":verifySmartcardPin";
        //Call YubiKit method to verify PIN.
        try {
            piv.verifyPin(pin);
            //If pin is successfully verified, we will get the certificate and perform the rest of the logic for authentication.
            X509Certificate cert = piv.getCertificate(certDetails.getSlot());
            //TODO: do a confirmation that the cert retrieved above is the same cert that was picked earlier by comparing with YubiKitCertDetails
            //TODO: Complete authentication using cert and YubiKit sdk.
            //NOTE: below is for testing. This would get replaced by the logic for actual authentication.
            //START of code for testing
            Logger.infoPII(methodTag,  cert.getSubjectDN().getName());
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mActivity.getApplicationContext(), "Success!", Toast.LENGTH_LONG).show();
                    //Reset currentDialog to null
                    synchronized (smartcardDialogLock) {
                        mCurrentDialog = null;
                    }
                }
            });
            request.cancel();
            synchronized (smartcardDialogLock) {
                mCurrentDialog.dismiss();
            }
            //END of code for testing
        } catch (InvalidPinException e) {
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
            } else {
                //Update Dialog to indicate that an incorrect attempt was made.
                synchronized (smartcardDialogLock) {
                    ((SmartcardPinDialog) mCurrentDialog).setErrorMode();
                }
            }
        } catch (BadResponseException e) {
            Logger.errorPII(methodTag,"BadResponseException", e);
        }
    }

    //Sets up and shows dialog when user has exceeded the max attempts allowed to enter their YubiKey PIN.
    private void showMaxAttemptsDialog(){
        final String methodTag = TAG + ":showMaxAttemptsDialog";
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
        maxFailedAttemptsDialog.show();
        synchronized (smartcardDialogLock) {
            mCurrentDialog = maxFailedAttemptsDialog;
        }
        Logger.infoPII(methodTag,  "User has reached the maximum failed attempts allowed.");
    }


    // Handles the logic for on-device certificate based authentication.
    // Makes use of Android's KeyChain.choosePrivateKeyAlias method,
    // which shows a cert picker that allows users to choose their on-device user certificate to authenticate with.
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

    //Allows AzureActiveDirectoryWebViewClient to stop mYubiKitManager's discovery mode.
    public void stopYubiKitManagerUsbDiscovery() {
        //Stop UsbDiscovery for YubiKitManager
        //Should be called when host fragment is destroyed.
        mYubiKitManager.stopUsbDiscovery();
    }
}