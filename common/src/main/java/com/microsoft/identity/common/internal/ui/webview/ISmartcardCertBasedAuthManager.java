package com.microsoft.identity.common.internal.ui.webview;

import androidx.annotation.NonNull;

import java.security.cert.X509Certificate;
import java.util.List;

public interface ISmartcardCertBasedAuthManager {

    public void startDiscovery(final IStartDiscoveryCallback startDiscoveryCallback);

    public void stopDiscovery(final IStopDiscoveryCallback stopDiscoveryCallback);

    public boolean isDeviceConnected();

    @NonNull
    public List<ICertDetails> getCertDetailsList();

    public boolean verifyPin(char[] pin);

    public int getPinAttemptsRemaining();

    public void attemptAuth(ICertDetails certDetails, char[] pin);

    public interface IStartDiscoveryCallback {
        void onStartDiscovery();
        void onClosedConnection();
    }

    public interface IStopDiscoveryCallback {
        void onStopDiscovery();
    }

}
