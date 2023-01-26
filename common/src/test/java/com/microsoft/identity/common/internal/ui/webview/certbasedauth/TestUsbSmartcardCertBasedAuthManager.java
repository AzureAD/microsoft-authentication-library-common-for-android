package com.microsoft.identity.common.internal.ui.webview.certbasedauth;

import android.app.Activity;

import androidx.annotation.NonNull;

import com.microsoft.identity.common.java.opentelemetry.ICertBasedAuthTelemetryHelper;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

class TestUsbSmartcardCertBasedAuthManager extends AbstractUsbSmartcardCertBasedAuthManager {

    private boolean mIsConnected;
    private final List<ICertDetails> mCertDetailsList;
    private int mPinAttemptsRemaining;

    public TestUsbSmartcardCertBasedAuthManager(@NonNull final List<X509Certificate> certList) {
        mIsConnected = false;
        //Attempts remaining is usually 3, but 2 attempts is all that's necessary for testing.
        mPinAttemptsRemaining = 2;
        //Convert cert list into certDetails list.
        mCertDetailsList = new ArrayList<>();
        for (X509Certificate cert : certList) {
            mCertDetailsList.add(new ICertDetails() {
                @NonNull
                @Override
                public X509Certificate getCertificate() {
                    return cert;
                }
            });
        }
    }

    @Override
    boolean startDiscovery(@NonNull Activity activity) {
        mockConnect();
        return false;
    }

    @Override
    void stopDiscovery(@NonNull Activity activity) {
        mockDisconnect();
    }

    @Override
    void requestDeviceSession(@NonNull ISessionCallback callback) {
        try {
            callback.onGetSession(new TestSmartcardSession(mCertDetailsList, mPinAttemptsRemaining, new TestSmartcardSession.ITestSessionCallback() {
                @Override
                public void onIncorrectAttempt() {
                    mPinAttemptsRemaining--;
                }
            }));
        } catch (@NonNull final Exception e) {
            callback.onException(e);
        }
    }

    @Override
    boolean isDeviceConnected() {
        return mIsConnected;
    }

    @Override
    void initBeforeProceedingWithRequest(@NonNull ICertBasedAuthTelemetryHelper telemetryHelper) {

    }

    @Override
    void onDestroy(@NonNull Activity activity) {
        stopDiscovery(activity);
    }

    public void mockConnect() {
        mIsConnected = true;
        if (mConnectionCallback != null) {
            mConnectionCallback.onCreateConnection();
        }
    }

    public void mockDisconnect() {
        mIsConnected = false;
        if (mConnectionCallback != null) {
            mConnectionCallback.onClosedConnection();
        }
    }
}
