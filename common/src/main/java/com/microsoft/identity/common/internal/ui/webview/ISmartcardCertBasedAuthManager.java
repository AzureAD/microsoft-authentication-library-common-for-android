package com.microsoft.identity.common.internal.ui.webview;

import androidx.annotation.NonNull;

import com.microsoft.identity.common.internal.ui.webview.challengehandlers.YubiKitCertBasedAuthManager;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.List;

public interface ISmartcardCertBasedAuthManager {

    public void startDiscovery(final IStartDiscoveryCallback startDiscoveryCallback);

    public void stopDiscovery();

    public void attemptDeviceSession(@NonNull final ISessionCallback callback);

    public interface IStartDiscoveryCallback {
        void onStartDiscovery();
        void onClosedConnection();
    }

    public boolean isDeviceConnected();

    public interface IConnectionCallback {
        void onConnection();
    }

    /**
     * Callback which will contain code to be run upon creation of a PivSession instance.
     */
    public interface ISessionCallback {
        /**
         * Code depending on PivSession instance to be run.
         * @param connection PivSession instance created from UsbSmartCardConnection.
         */
        void onGetSession(@NonNull final ISmartcardSession session);

        void onException(@NonNull final Exception e);
    }

    public interface ISmartcardSession {

        @NonNull
        public List<ICertDetails> getCertDetailsList() throws Exception;

        public void verifyPin(char[] pin) throws Exception;

        public int getPinAttemptsRemaining();

        public void prepareForAuth();

        public PrivateKey getKeyForAuth(ICertDetails certDetails, char[] pin) throws Exception;
    }

}
