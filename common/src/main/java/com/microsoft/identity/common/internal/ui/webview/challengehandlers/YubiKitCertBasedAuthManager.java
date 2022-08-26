package com.microsoft.identity.common.internal.ui.webview.challengehandlers;

import android.app.Activity;

import androidx.annotation.NonNull;

import com.microsoft.identity.common.internal.telemetry.Telemetry;
import com.microsoft.identity.common.internal.ui.webview.ICertDetails;
import com.microsoft.identity.common.internal.ui.webview.ISmartcardCertBasedAuthManager;
import com.microsoft.identity.common.java.telemetry.events.PivProviderStatusEvent;
import com.microsoft.identity.common.logging.Logger;
import com.yubico.yubikit.android.YubiKitManager;
import com.yubico.yubikit.android.transport.usb.UsbConfiguration;
import com.yubico.yubikit.android.transport.usb.UsbYubiKeyDevice;
import com.yubico.yubikit.core.util.Callback;

import java.security.Security;
import java.util.List;

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
    public void stopDiscovery(IStopDiscoveryCallback stopDiscoveryCallback) {

    }

    @Override
    public boolean isDeviceConnected() {
        return false;
    }

    @NonNull
    @Override
    public List<ICertDetails> getCertDetailsList() {
        return null;
    }

    @Override
    public boolean verifyPin(char[] pin) {
        return false;
    }

    @Override
    public int getPinAttemptsRemaining() {
        return 3;
    }

    @Override
    public void attemptAuth(ICertDetails certDetails, char[] pin) {

    }
}
