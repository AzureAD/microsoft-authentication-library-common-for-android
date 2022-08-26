package com.microsoft.identity.common.internal.ui.webview.challengehandlers;

import static com.yubico.yubikit.piv.Slot.AUTHENTICATION;
import static com.yubico.yubikit.piv.Slot.CARD_AUTH;
import static com.yubico.yubikit.piv.Slot.KEY_MANAGEMENT;
import static com.yubico.yubikit.piv.Slot.SIGNATURE;

import android.app.Activity;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.microsoft.identity.common.R;
import com.microsoft.identity.common.internal.telemetry.Telemetry;
import com.microsoft.identity.common.internal.ui.webview.ICertDetails;
import com.microsoft.identity.common.internal.ui.webview.ISmartcardCertBasedAuthManager;
import com.microsoft.identity.common.internal.ui.webview.YubiKitCertDetails;
import com.microsoft.identity.common.java.telemetry.events.PivProviderStatusEvent;
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
import com.yubico.yubikit.piv.PivSession;
import com.yubico.yubikit.piv.Slot;
import com.yubico.yubikit.piv.jca.PivPrivateKey;
import com.yubico.yubikit.piv.jca.PivProvider;

import java.io.IOException;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class YubiKitCertBasedAuthManager implements ISmartcardCertBasedAuthManager {

    private static final String TAG = YubiKitCertBasedAuthManager.class.getSimpleName();
    private static final String MDEVICE_NULL_ERROR_MESSAGE = "Instance UsbYubiKitDevice variable (mDevice) is null.";
    private static final String YUBIKEY_PROVIDER = "YKPiv";

    private final YubiKitManager mYubiKitManager;
    private UsbYubiKeyDevice mDevice;

    YubiKitCertBasedAuthManager(Activity activity) {
        mYubiKitManager = new YubiKitManager(activity.getApplicationContext());
    }

    @Override
    public void startDiscovery(IStartDiscoveryCallback startDiscoveryCallback) {
        mYubiKitManager.startUsbDiscovery(new UsbConfiguration(), new Callback<UsbYubiKeyDevice>() {
            @Override
            public void invoke(@NonNull UsbYubiKeyDevice device) {
                Logger.verbose(TAG, "A YubiKey device was connected");
                mDevice = device;
                startDiscoveryCallback.onStartDiscovery();

                mDevice.setOnClosed(new Runnable() {
                    @Override
                    public void run() {
                        Logger.verbose(TAG, "A YubiKey device was disconnected");
                        mDevice = null;
                        final PivProviderStatusEvent pivProviderStatusEvent = new PivProviderStatusEvent();
                        //Remove the YKPiv security provider if it was added.
                        if (Security.getProvider(YUBIKEY_PROVIDER) != null) {
                            Security.removeProvider(YUBIKEY_PROVIDER);
                            Telemetry.emit(pivProviderStatusEvent.putPivProviderRemoved(true));
                            Logger.info(TAG, "An instance of PivProvider was removed from Security static list upon YubiKey device connection being closed.");
                        } else {
                            Telemetry.emit(pivProviderStatusEvent.putPivProviderRemoved(false));
                            Logger.info(TAG, "An instance of PivProvider was not present in Security static list upon YubiKey device connection being closed.");
                        }
                        startDiscoveryCallback.onClosedConnection();
                    }

                });
            }
        });
    }

    @Override
    public void stopDiscovery() {
        mYubiKitManager.stopUsbDiscovery();
    }

    @Override
    public void attemptDeviceSession(@NonNull final ISessionCallback callback) {
        final String methodTag = TAG + "attemptDeviceSession:";
        if (mDevice == null) {
            Logger.error(methodTag, MDEVICE_NULL_ERROR_MESSAGE, null);
            callback.onException(new Exception());
        }
        //Request a connection from mDevice so that we can get a PivSession instance.
        mDevice.requestConnection(UsbSmartCardConnection.class, new Callback<Result<UsbSmartCardConnection, IOException>>() {
            @Override
            public void invoke(@NonNull final Result<UsbSmartCardConnection, IOException> value) {
                try {
                    final SmartCardConnection c = value.getValue();
                    final PivSession piv = new PivSession(c);
                    YubiKitSmartcardSession session = new YubiKitSmartcardSession(piv);
                    callback.onGetSession(session);
                } catch (final IOException | ApduException | ApplicationNotAvailableException e) {
                    Logger.error(methodTag, e.getMessage(), e);
                    callback.onException(e);
                }
            }
        });
    }

    @Override
    public boolean isDeviceConnected() {
        return mDevice != null;
    }


    public class YubiKitSmartcardSession implements ISmartcardSession{
        PivSession piv;

        public YubiKitSmartcardSession(PivSession p) {
            piv = p;
        }

        @NonNull
        @Override
        public List<ICertDetails> getCertDetailsList() throws Exception {
            //Create ArrayList that contains cert details only pertinent to the cert picker.
            final List<ICertDetails> certList = new ArrayList<>();
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
        private void getAndPutCertDetailsInList(@NonNull final Slot slot,
                                                @NonNull final PivSession piv,
                                                @NonNull final List<ICertDetails> certList)
                throws IOException, ApduException, BadResponseException {
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

        @Override
        public void verifyPin(char[] pin) throws Exception {
            piv.verifyPin(pin);
        }

        @Override
        public int getPinAttemptsRemaining() {
            return 0;
        }

        @Override
        public void prepareForAuth() {
            final String methodTag = TAG + ":PrepareForAuth";
            //Some telemetry
            //Need to add a PivProvider instance to the beginning of the array of Security providers in order for signature logic to occur.
            //Note that this provider is removed when the UsbYubiKeyDevice connection is closed.
            final PivProviderStatusEvent pivProviderStatusEvent = new PivProviderStatusEvent();
            if (Security.getProvider(YUBIKEY_PROVIDER) != null) {
                //Remove existing PivProvider.
                Security.removeProvider(YUBIKEY_PROVIDER);
                //The PivProvider instance is either unexpectedly being added elsewhere
                // or it isn't being removed properly upon CBA flow termination.
                Telemetry.emit(pivProviderStatusEvent.putIsExistingPivProviderPresent(true));
                Logger.info(methodTag, "Existing PivProvider was present in Security static list.");
            } else {
                //This is expected behavior.
                Telemetry.emit(pivProviderStatusEvent.putIsExistingPivProviderPresent(false));
                Logger.info(methodTag, "Security static list does not have existing PivProvider.");
            }
            //The position parameter is 1-based (1 maps to index 0).
            Security.insertProviderAt(new PivProvider(getPivProviderCallback()), 1);
            Logger.info(methodTag, "An instance of PivProvider was added to Security static list.");
        }

        @Override
        public PrivateKey getKeyForAuth(ICertDetails certDetails, char[] pin) throws Exception {
            final String methodTag = TAG + ":getKeyForAuth";
                if (!(certDetails instanceof YubiKitCertDetails)) {
                    throw new Exception();
                }
                //Using KeyStore methods in order to generate PivPrivateKey.
                //Loading null is needed for initialization.
                final KeyStore keyStore = KeyStore.getInstance(YUBIKEY_PROVIDER, new PivProvider(piv));
                keyStore.load(null);
                final Key key = keyStore.getKey(((YubiKitCertDetails)certDetails).getSlot().getStringAlias(), pin);
                if (!(key instanceof PivPrivateKey)) {
                    Logger.error(methodTag, "Private key retrieved from YKPiv keystore is not of type PivPrivateKey.", null);
                    throw new Exception("Private key retrieved from YKPiv keystore is not of type PivPrivateKey.");
                }
                //PivPrivateKey implements PrivateKey. Note that the PIN is copied in pivPrivateKey.
                return (PivPrivateKey) key;
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
                    //Show error dialog and cancel flow if mDevice is null.
                    if (mDevice == null) {
                        Logger.error(methodTag, MDEVICE_NULL_ERROR_MESSAGE, null);
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
            };
        }
    }
}
