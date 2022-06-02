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

import com.microsoft.identity.common.java.util.ResultFuture;
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
import java.io.Serializable;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;

//Handles Certificate Based Authentication by means of certificates provisioned onto YubiKeys or the devices themselves.
//Note that CBA requires API >= 21 (YubiKit SDK min API = 19; ClientCertRequest class available with API >= 21)
public final class ClientCertAuthChallengeHandler implements IChallengeHandler<ClientCertRequest, Void> {
    private static final String TAG = ClientCertAuthChallengeHandler.class.getSimpleName();
    private static final String ACCEPTABLE_ISSUER = "CN=MS-Organization-Access";
    private Activity mActivity;
    private final YubiKitManager mYubiKitManager;
    private UsbYubiKeyDevice mDevice;
    //Lockd to help facilitate synchronization
    private static final Object deviceLock = new Object();
    private static final Object smartcardDialogLock = new Object();

    private SmartcardDialog mCurrentDialog;

    //private ResultFuture<YubiKitCertDetails> mCertPickerResultFuture;

    //creating nested class to hold details of a Certificate needed for the picker,
    // including subject, issuer, and slot.
    public class YubiKitCertDetails implements Serializable {
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
            public void invoke(UsbYubiKeyDevice device) {
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
    //Offers the user to choose a certificate to authenticate with based on whether or not a YubiKey is plugged in and has permission to be connected.
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public Void processChallenge(@NonNull final ClientCertRequest request) {
        final String methodTag = TAG + ":processChallenge";
        // Check if a YubiKey device has been discovered and has permission to be connected.
        synchronized (deviceLock) {
            if (mDevice != null) {
                //A connection to the YubiKey needs to be made in order to read the certificates off it.
                mDevice.requestConnection(UsbSmartCardConnection.class, new Callback<Result<UsbSmartCardConnection, IOException>>() {
                    @Override
                    public void invoke(Result<UsbSmartCardConnection, IOException> value) {
                        try {
                            final SmartCardConnection c = value.getValue();
                            PivSession piv = new PivSession(c);
                            //Check if too many PIN attempts have been made.
                            //If so, go directly to Error dialog.
                            if (piv.getPinAttempts() == 0) {
                                SmartcardMaxFailedAttemptsDialog maxFailedAttemptsDialog = new SmartcardMaxFailedAttemptsDialog(mActivity);
                                maxFailedAttemptsDialog.show();
                                request.cancel();
                                Logger.infoPII(methodTag,  "User has reached the maximum failed attempts allowed.");
                                return;
                            }
                            //Create ArrayList that contains cert details only pertinent to the cert picker.
                            final ArrayList<YubiKitCertDetails> certList = new ArrayList<>();
                            //We need to check every slot. If a slot is empty, an ApduException is called, in which case, we'll ignore it.
                            //AUTHENTICATION (9A)
                            getAndPutCertDetailsInList(AUTHENTICATION, piv, certList);
                            //SIGNATURE (9C)
                            getAndPutCertDetailsInList(SIGNATURE, piv, certList);
                            //KEY_MANAGEMENT (9D)
                            getAndPutCertDetailsInList(KEY_MANAGEMENT, piv, certList);
                            //CARD_AUTH (9E)
                            getAndPutCertDetailsInList(CARD_AUTH, piv, certList);
                            //If no certs were found, cancel flow.
                            if (certList.isEmpty()) {
                                request.cancel();
                                Logger.infoPII(methodTag,  "No PIV certificates found on YubiKey device.");
                                return;
                            }
                            //Show Smartcard cert picker, which also handles the rest of the smartcard CBA flow.
                            final SmartcardCertPickerDialog certPickerDialog = new SmartcardCertPickerDialog(certList, mActivity);
                            certPickerDialog.setPositiveButtonListener(getSmartcardCertPickerDialogPositiveButtonListener(request));
                            certPickerDialog.setNegativeButtonListener(new SmartcardCertPickerDialog.NegativeButtonListener() {
                                @Override
                                public void onClick() {
                                    request.cancel();
                                }
                            });
                            certPickerDialog.setCancelCbaCallback(new SmartcardCertPickerDialog.CancelCbaCallback() {
                                @Override
                                public void onCancel() {
                                    request.cancel();
                                }
                            });

                            certPickerDialog.show();
                            //Set current dialog to certPickerDialog
                            synchronized (mCurrentDialog) {
                                mCurrentDialog = certPickerDialog;
                            }

                        } catch(IOException e) {
                            Logger.errorPII(methodTag, "IOException", e);
                        } catch (ApduException e) {
                            Logger.errorPII(methodTag, "ApduException", e);
                        } catch (ApplicationNotAvailableException e) {
                            Logger.errorPII(methodTag, "ApplicationNotAvailableException", e);
                        }
                    }
                });
                return null;
            }
        }
        //Else, proceed with user certificates stored on device.
        return handleOnDeviceCertAuth(request);
    }

    //Helper method created to handle reading certificates off YubiKey.
    //This method should only be called within a callback upon creating a successful YubiKey device connection.
    private void getAndPutCertDetailsInList(Slot slot, PivSession piv, ArrayList<YubiKitCertDetails> certList) {
        final String methodTag = TAG + ":getCert";
        try {
            X509Certificate cert =  piv.getCertificate(slot);
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

    //Upon a positive button click in the cert picker, sets up the PIN prompt dialog.
    public SmartcardCertPickerDialog.PositiveButtonListener getSmartcardCertPickerDialogPositiveButtonListener(final ClientCertRequest request) {
        //final String methodTag = TAG + ":getSmartcardCertPickerDialogPositiveButtonListener";
        return new SmartcardCertPickerDialog.PositiveButtonListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(final YubiKitCertDetails certDetails) {
                //Need to prompt user for pin and verify pin. The positive button listener will handle the rest of the CBA flow.
                final SmartcardPinDialog pinDialog = new SmartcardPinDialog(mActivity);
                pinDialog.setPositiveButtonListener(getSmartcardPinDialogPositiveButtonListener(certDetails, pinDialog, request));
                pinDialog.setNegativeButtonListener(new SmartcardPinDialog.NegativeButtonListener() {
                    @Override
                    public void onClick() {
                        request.cancel();
                        pinDialog.dismiss();
                    }
                });
                pinDialog.setCancelCbaCallback(new SmartcardPinDialog.CancelCbaCallback() {
                    @Override
                    public void onCancel() {
                        request.cancel();
                    }
                });
                pinDialog.show();
                //Set currentDialog to pinDialog
                synchronized (smartcardDialogLock) {
                    mCurrentDialog = pinDialog;
                }
            }
        };
    }

    //Upon a positive button click in the PIN prompt, verify the provided PIN and handle the results.
    public SmartcardPinDialog.PositiveButtonListener getSmartcardPinDialogPositiveButtonListener(final YubiKitCertDetails certDetails, final SmartcardPinDialog pinDialog,final ClientCertRequest request) {
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
                        public void invoke(Result<UsbSmartCardConnection, IOException> value) {
                            try {
                                final SmartCardConnection c = value.getValue();
                                final PivSession piv = new PivSession(c);
                                //Call YubiKit method to verify PIN.
                                try {
                                    piv.verifyPin(pin.toCharArray());
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
                                    pinDialog.dismiss();
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
                                        pinDialog.dismiss();
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
                                    } else {
                                        //Update Dialog to indicate that an incorrect attempt was made.
                                        pinDialog.setErrorMode();
                                    }
                                } catch (BadResponseException e) {
                                    Logger.errorPII(methodTag,"BadResponseException", e);
                                }

                            } catch(IOException e) {
                                Logger.errorPII(methodTag, "IOException", e);
                            } catch (ApduException e) {
                                Logger.errorPII(methodTag, "ApduException", e);
                            } catch (ApplicationNotAvailableException e) {
                                Logger.errorPII(methodTag, "ApplicationNotAvailableException", e);
                            }
                        }
                    });
                }
            }
        };

    }


    // Handles the logic for on-device certificate based authentication.
    // Makes use of Android's KeyChain.choosePrivateKeyAlias method,
    // which shows a cert picker that allows users to choose their on-device user certificate to authenticate with.
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private Void handleOnDeviceCertAuth(@NonNull final ClientCertRequest request) {
        final String methodTag = TAG + ":handleOnDeviceCertAuth";
        final Principal[] acceptableCertIssuers = request.getPrincipals();

        // When ADFS server sends null or empty issuers, we'll continue with cert prompt.
        if (acceptableCertIssuers != null) {
            for (Principal issuer : acceptableCertIssuers) {
                if (issuer.getName().contains(ACCEPTABLE_ISSUER)) {
                    //Checking if received acceptable issuers contain "CN=MS-Organization-Access"
                    Logger.info(methodTag,"Cancelling the TLS request, not respond to TLS challenge triggered by device authentication.");
                    request.cancel();
                    return null;
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

        return null;
    }

    //Allows AzureActiveDirectoryWebViewClient to stop mYubiKitManager's discovery mode.
    public void stopYubiKitManagerUsbDiscovery() {
        //Stop UsbDiscovery for YubiKitManager
        //Should be called when host fragment is destroyed.
        mYubiKitManager.stopUsbDiscovery();
    }
}