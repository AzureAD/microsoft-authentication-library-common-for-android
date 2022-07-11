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

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.microsoft.identity.common.R;
import com.microsoft.identity.common.internal.telemetry.Telemetry;
import com.microsoft.identity.common.java.telemetry.TelemetryEventStrings;
import com.microsoft.identity.common.java.telemetry.events.BaseEvent;
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
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Handles Certificate Based Authentication by means of certificates provisioned onto YubiKeys or the devices themselves.
 * Note that CBA requires API >= 21 (YubiKit SDK min API = 19; ClientCertRequest class available with API >= 21)
 */
public final class ClientCertAuthChallengeHandler implements IChallengeHandler<ClientCertRequest, Void> {
    private static final String TAG = ClientCertAuthChallengeHandler.class.getSimpleName();
    private static final String ACCEPTABLE_ISSUER = "CN=MS-Organization-Access";
    private static final String MDEVICE_NULL_ERROR_MESSAGE = "Instance UsbYubiKitDevice variable (mDevice) is null.";
    private static final String YKPIV = "YKPiv";
    private final Activity mActivity;
    private final YubiKitManager mYubiKitManager;
    private UsbYubiKeyDevice mDevice;
    private final DialogHolder mDialogHolder;
    //Lock to help facilitate synchronization
    private static final Object sDeviceLock = new Object();

    /**
     * Holds certificate found on YubiKey and its corresponding slot.
     */
    public static class YubiKitCertDetails {
        private final X509Certificate cert;
        private final Slot slot;

        /**
         * Creates new instance of YubiKitCertDetails.
         * @param cert Certificate found on YubiKey.
         * @param slot PIV slot on YubiKey where certificate is located.
         */
        public YubiKitCertDetails(X509Certificate cert, Slot slot) {
            this.cert = cert;
            this.slot = slot;
        }

        /**
         * Gets certificate.
         * @return certificate.
         */
        public X509Certificate getCertificate() {
            return cert;
        }


        /**
         * Gets PIV Slot where certificate is located.
         * @return Slot where certificate is located.
         */
        public Slot getSlot() {
            return slot;
        }
    }

    /**
     * Callback which will contain code to be run upon creation of a PivSession instance.
     */
    public interface IPivSessionCallback {
        /**
         * Code depending on PivSession instance to be run.
         * @param piv PivSession instance created from UsbSmartCardConnection.
         */
        void onGetSession(@NonNull final PivSession piv);
    }

    /**
     * Creates new instance of ClientCertAuthChallengeHandler and starts usb discovery for YubiKeys.
     * @param activity current host activity.
     */
    public ClientCertAuthChallengeHandler(@NonNull final Activity activity) {
        mActivity = activity;
        //Create instance of DialogHolder.
        mDialogHolder = new DialogHolder(mActivity);
        //Create and start YubiKitManager for UsbDiscovery mode.
        //When in Usb Discovery mode, Yubikeys that plug into the device will be accessible
        // once the user provides permission via the Android permission dialog.
        mYubiKitManager = new YubiKitManager(mActivity.getApplicationContext());
        mYubiKitManager.startUsbDiscovery(new UsbConfiguration(), new Callback<UsbYubiKeyDevice>() {
            @Override
            public void invoke(@NonNull UsbYubiKeyDevice device) {
                Logger.verbose(TAG, "A YubiKey device was connected");
                synchronized (sDeviceLock) {
                    //set device
                    mDevice = device;
                    //Reset DialogHolder to null if necessary.
                    //In this case, DialogHolder would be an ErrorDialog if not null.
                    mDialogHolder.dismissDialog();
                    //Make sure instance variables are reset/set properly and requests are canceled.
                    mDevice.setOnClosed(new Runnable() {
                        @Override
                        public void run() {
                            synchronized (sDeviceLock) {
                                Logger.verbose(TAG, "A YubiKey device was disconnected");
                                mDevice = null;
                                //Remove the YKPiv security provider if it was added.
                                //Note that Security.removeProvider will silently return if YKPiv doesn't exist.
                                Security.removeProvider(YKPIV);
                                //Show an error dialog only if a dialog is still showing.
                                if (mDialogHolder.isDialogShowing()) {
                                    //Call Cancel Cba callback
                                    mDialogHolder.onCancelCba();
                                    //Show dialog informing users that they have unplugged their device too early.
                                    mDialogHolder.showErrorDialog(R.string.smartcard_early_unplug_dialog_title, R.string.smartcard_early_unplug_dialog_message);
                                }
                                Logger.verbose(TAG, "YubiKey was disconnected while dialog was still displayed.");
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
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public Void processChallenge(@NonNull final ClientCertRequest request) {
        //final String methodTag = TAG + ":processChallenge";
        synchronized (sDeviceLock) {
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
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void handleSmartcardCertAuth(@NonNull final ClientCertRequest request) {
        final String methodTag = TAG + ":handleSmartcardCertAuth";
        //A connection to the YubiKey needs to be made in order to read the certificates off it.
        getActivePivSessionAsync(request, new IPivSessionCallback() {
            @Override
            public void onGetSession(@NonNull PivSession piv) {
                try {
                    //Check if there are no PIN attempts remaining.
                    if (piv.getPinAttempts() == 0) {
                        Logger.info(methodTag,  "User has reached the maximum failed attempts allowed.");
                        mDialogHolder.showErrorDialog(R.string.smartcard_max_attempt_dialog_title, R.string.smartcard_max_attempt_dialog_message);
                        request.cancel();
                        return;
                    }
                    //Create List that contains cert details only pertinent to the cert picker.
                    final List<YubiKitCertDetails> certList = getCertDetailsFromKey(piv);
                    //If no certs were found, cancel flow.
                    if (certList.isEmpty()) {
                        Logger.info(methodTag,  "No PIV certificates found on YubiKey device.");
                        //Show error dialog stating that no PIV certificates were found on the YubiKey.
                        mDialogHolder.showErrorDialog(R.string.smartcard_no_cert_dialog_title, R.string.smartcard_no_cert_dialog_message);
                        request.cancel();
                        return;
                    }
                    //Build and show Smartcard cert picker, which also handles the rest of the smartcard CBA flow.
                    mDialogHolder.showCertPickerDialog(
                            certList,
                            getSmartcardCertPickerDialogPositiveButtonListener(request),
                            new SmartcardCertPickerDialog.CancelCbaCallback() {
                                @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                                @Override
                                public void onCancel() {
                                    mDialogHolder.dismissDialog();
                                    request.cancel();
                                }
                            });
                } catch(final IOException | ApduException | BadResponseException e) {
                    Logger.error(methodTag, e.getMessage(), e);
                    //Show general error dialog.
                    mDialogHolder.showErrorDialog(R.string.smartcard_general_error_dialog_title, R.string.smartcard_general_error_dialog_message);
                    request.cancel();
                }
            }
        });
    }

    /**
     * Requests a connection from UsbYubiKeyDevice instance in order to run IPivSessionCallback code that depends on a PivSession instance.
     * @param request ClientCertRequest received from AzureActiveDirectoryWebViewClient.onReceivedClientCertRequest.
     * @param callback Callback code that utilizes PivSession.
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void getActivePivSessionAsync(@NonNull final ClientCertRequest request, @NonNull final IPivSessionCallback callback){
        final String methodTag = TAG + "getActivePivSessionAsync:";
        //If mDevice is null, show a general error dialog and cancel request.
        if (mDevice == null) {
            Logger.error(methodTag, MDEVICE_NULL_ERROR_MESSAGE, null);
            mDialogHolder.showErrorDialog(R.string.smartcard_general_error_dialog_title, R.string.smartcard_general_error_dialog_message);
            request.cancel();
            return;
        }
        //Request a connection from mDevice so that we can get a PivSession instance.
        mDevice.requestConnection(UsbSmartCardConnection.class, new Callback<Result<UsbSmartCardConnection, IOException>>() {
            @Override
            public void invoke(@NonNull final Result<UsbSmartCardConnection, IOException> value) {
                try {
                    final SmartCardConnection c = value.getValue();
                    final PivSession piv = new PivSession(c);
                    //Call session callback.
                    callback.onGetSession(piv);
                } catch (final IOException | ApduException | ApplicationNotAvailableException e) {
                    Logger.error(methodTag, e.getMessage(), e);
                    //Show general error dialog.
                    mDialogHolder.showErrorDialog(R.string.smartcard_general_error_dialog_title, R.string.smartcard_general_error_dialog_message);
                    request.cancel();
                }
            }
        });
    }

    /**
     * Helper method that returns a list of YubiKitCertDetails extracted from certificates located on the YubiKey.
     * This method should only be called within a callback upon creating a successful YubiKey device connection.
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
            //If there are no exceptions, add this cert to our certList.
            certList.add(new YubiKitCertDetails(cert, slot));
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
     * Upon a positive button click in the certificate picker, the listener will proceed with a PIN prompt dialog.
     * @param request ClientCertRequest received from AzureActiveDirectoryWebViewClient.onReceivedClientCertRequest.
     * @return A PositiveButtonListener to be set for a SmartcardCertPickerDialog.
     */
    private SmartcardCertPickerDialog.PositiveButtonListener getSmartcardCertPickerDialogPositiveButtonListener(@NonNull final ClientCertRequest request) {
        return new SmartcardCertPickerDialog.PositiveButtonListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(@NonNull final YubiKitCertDetails certDetails) {
                //Need to prompt user for pin and verify pin. The positive button listener will handle the rest of the CBA flow.
                mDialogHolder.showPinDialog(
                        getSmartcardPinDialogPositiveButtonListener(certDetails, request),
                        new SmartcardPinDialog.CancelCbaCallback() {
                            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                            @Override
                            public void onCancel() {
                                mDialogHolder.dismissDialog();
                                request.cancel();
                            }
                        });
            }
        };
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
            public void onClick(@NonNull final String pin) {
                synchronized (sDeviceLock) {
                    // Need to request a PivSession in order to access certs on YubiKey.
                    getActivePivSessionAsync(request, new IPivSessionCallback() {
                        @Override
                        public void onGetSession(@NonNull PivSession piv) {
                            try {
                                //Verify PIN and handle results
                                verifySmartcardPin(pin.toCharArray(), certDetails, request, piv);
                            } catch (final IOException | ApduException | BadResponseException e) {
                                Logger.error(methodTag, e.getMessage(), e);
                                //Show general error dialog.
                                mDialogHolder.showErrorDialog(R.string.smartcard_general_error_dialog_title, R.string.smartcard_general_error_dialog_message);
                                request.cancel();
                            }

                        }
                    });
                }
            }
        };
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
            //If pin is successfully verified, we will use the certificate to perform the rest of the logic for authentication.
            //Complete authentication using cert and YubiKit sdk.
            useSmartcardCertForAuth(certDetails.getCertificate(), pin, certDetails.getSlot().getStringAlias(), piv, request);
        } catch (final InvalidPinException e) {
            // An incorrect Pin attempt.
            // We need to retrieve the number of pin attempts before proceeding.
            final int pinAttemptsRemaining = piv.getPinAttempts();
            // If the number of attempts is 0, no more attempts will be allowed.
            if (pinAttemptsRemaining == 0) {
                //We must display a dialog informing the user that they have made too many incorrect attempts,
                // and the user will need to figure out a way to reset their key outside of our library.
                Logger.info(methodTag,  "User has reached the maximum failed attempts allowed.");
                mDialogHolder.showErrorDialog(R.string.smartcard_max_attempt_dialog_title, R.string.smartcard_max_attempt_dialog_message);
                request.cancel();
            } else {
                //Update Dialog to indicate that an incorrect attempt was made.
                mDialogHolder.setPinDialogErrorMode();
            }
        }
    }

    /**
     * Attempts to authenticate using smartcard certificate.
     * @param cert Chosen certificate for authentication.
     * @param pin char array containing PIN.
     * @param slotAlias Name of slot where certificate is located.
     * @param piv A PivSession created from a SmartCardConnection that can interact with certificates located in the PIV slots on the YubiKey.
     * @param request ClientCertRequest received from AzureActiveDirectoryWebViewClient.onReceivedClientCertRequest.
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void useSmartcardCertForAuth(@NonNull final X509Certificate cert,
                                         @NonNull final char[] pin,
                                         @NonNull final String slotAlias,
                                         @NonNull final PivSession piv,
                                         @NonNull final ClientCertRequest request) {
        final String methodTag = TAG + "useSmartcardCertForAuth:";
        //Need to add a PivProvider instance to the beginning of the array of Security providers in order for signature logic to occur.
        //Note that this provider is removed when the UsbYubiKeyDevice connection is closed.
        //First check if a PivProvider instance is already present in the static list.
        if (Security.getProvider(YKPIV) != null) {
            //Remove existing PivProvider.
            Security.removeProvider(YKPIV);
            //Telemet and log this, as the PivProvider instance is either unexpectedly being added elsewhere
            // or it isn't being removed properly upon CBA flow termination.
            Telemetry.emit((BaseEvent) new BaseEvent().put(
                    TelemetryEventStrings.Key.IS_EXISTING_PIVPROVIDER_PRESENT,
                    String.valueOf(true)
            ));
            Logger.info(methodTag, "Existing PivProvider was present in Security static list.");
        } else {
            //Telemet and log that PivProvider is not already present in Security static list, which is expected.
            Telemetry.emit((BaseEvent) new BaseEvent().put(
                    TelemetryEventStrings.Key.IS_EXISTING_PIVPROVIDER_PRESENT,
                    String.valueOf(false)
            ));
            Logger.info(methodTag, "Existing PivProvider was NOT present in Security static list.");
        }
        //The position parameter is 1-based (1 maps to index 0).
        Security.insertProviderAt(new PivProvider(getPivProviderCallback()), 1);
        try {
            //Using KeyStore methods in order to generate PivPrivateKey.
            //Loading null is needed for initialization.
            final KeyStore keyStore = KeyStore.getInstance(YKPIV, new PivProvider(piv));
            keyStore.load(null);
            //Make sure Key is of type PivPrivateKey
            final Key key = keyStore.getKey(slotAlias, pin);
            if (!(key instanceof PivPrivateKey)) {
                //Show general error dialog and cancel request.
                Logger.error(methodTag, "Private key retrieved from YKPiv keystore is not of type PivPrivateKey.", null);
                mDialogHolder.showErrorDialog(R.string.smartcard_general_error_dialog_title, R.string.smartcard_general_error_dialog_message);
                request.cancel();
                return;
            }
            //PivPrivateKey implements PrivateKey
            final PivPrivateKey pivPrivateKey = (PivPrivateKey) key;
            //Cert chain only needs the cert to be used for authentication.
            final X509Certificate[] chain = new X509Certificate[]{cert};
            //Clear current dialog.
            mDialogHolder.dismissDialog();
            //Call proceed on ClientCertRequest with PivPrivateKey and cert chain.
            request.proceed(pivPrivateKey, chain);
        } catch (UnrecoverableKeyException | CertificateException | KeyStoreException | IOException | NoSuchAlgorithmException e) {
            Logger.error(methodTag, e.getMessage(), e);
            //Show general error dialog.
            mDialogHolder.showErrorDialog(R.string.smartcard_general_error_dialog_title, R.string.smartcard_general_error_dialog_message);
            request.cancel();
        }
    }

    /**
     * Used to provide PivProvider constructor a Callback that will establish a new PivSession when it is needed.
     * @return A Callback which returns a Callback that will return a new PivSession instance.
     */
    private Callback<Callback<Result<PivSession, Exception>>> getPivProviderCallback() {
        final String methodTag = TAG + "getPivProviderCallback:";
        return new Callback<Callback<Result<PivSession, Exception>>>() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void invoke(@NonNull final Callback<Result<PivSession, Exception>> callback) {
                synchronized (sDeviceLock) {
                    //Show error dialog and cancel flow if mDevice is null.
                    if (mDevice == null) {
                        Logger.error(methodTag, MDEVICE_NULL_ERROR_MESSAGE, null);
                        //TODO: want to see if we can show an error dialog somehow in this scenario.
                        //Invoke result failure.
                        callback.invoke(Result.failure(new Exception(MDEVICE_NULL_ERROR_MESSAGE)));
                        return;
                    }
                    mDevice.requestConnection(UsbSmartCardConnection.class, new Callback<Result<UsbSmartCardConnection, IOException>>() {
                        @Override
                        public void invoke(@NonNull final Result<UsbSmartCardConnection, IOException> value) {
                            callback.invoke(Result.of(new Callable<PivSession>() {
                                @Override
                                public PivSession call() throws Exception {
                                    return new PivSession(value.getValue());
                                }
                            }));
                        }
                    });
                }
            }
        };
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