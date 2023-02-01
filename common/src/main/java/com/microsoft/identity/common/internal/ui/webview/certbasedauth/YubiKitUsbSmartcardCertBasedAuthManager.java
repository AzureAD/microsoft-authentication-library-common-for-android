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
package com.microsoft.identity.common.internal.ui.webview.certbasedauth;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.webkit.ClientCertRequest;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.microsoft.identity.common.java.opentelemetry.ICertBasedAuthTelemetryHelper;
import com.microsoft.identity.common.logging.Logger;
import com.yubico.yubikit.android.transport.usb.UsbConfiguration;
import com.yubico.yubikit.android.transport.usb.UsbYubiKeyDevice;
import com.yubico.yubikit.android.transport.usb.UsbYubiKeyManager;
import com.yubico.yubikit.android.transport.usb.connection.UsbSmartCardConnection;
import com.yubico.yubikit.core.smartcard.SmartCardConnection;
import com.yubico.yubikit.core.util.Callback;
import com.yubico.yubikit.core.util.Result;
import com.yubico.yubikit.piv.PivSession;

import java.io.IOException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.concurrent.Callable;

public class YubiKitUsbSmartcardCertBasedAuthManager extends AbstractUsbSmartcardCertBasedAuthManager {
    private static final String TAG = YubiKitUsbSmartcardCertBasedAuthManager.class.getSimpleName();
    private static final String DEVICE_ERROR_MESSAGE = "No USB device is currently connected.";

    private final UsbYubiKeyManager mUsbYubiKeyManager;
    private UsbYubiKeyDevice mUsbDevice;

    //Lock to help facilitate synchronization
    private static final Object sDeviceLock = new Object();

    public YubiKitUsbSmartcardCertBasedAuthManager(@NonNull final Context context) {
        mUsbYubiKeyManager = new UsbYubiKeyManager(context.getApplicationContext());
    }

    /**
     * Create and start YubiKitManager for UsbDiscovery mode.
     * When in Usb Discovery mode, Yubikeys that plug into the device will be accessible
     *  once the user provides permission via the Android permission dialog.
     */
    @Override
    boolean startDiscovery(@NonNull final Activity activity) {
        mUsbYubiKeyManager.enable(new UsbConfiguration(), new Callback<UsbYubiKeyDevice>() {
            @Override
            public void invoke(@NonNull UsbYubiKeyDevice device) {
                Logger.verbose(TAG, "A YubiKey device was connected via usb.");
                synchronized (sDeviceLock) {
                    mUsbDevice = device;
                    if (mConnectionCallback != null) {
                        mConnectionCallback.onCreateConnection();
                    }

                    mUsbDevice.setOnClosed(new Runnable() {
                        @Override
                        public void run() {
                            Logger.verbose(TAG, "A YubiKey device was disconnected via usb.");
                            synchronized (sDeviceLock) {
                                mUsbDevice = null;
                            }
                            YubiKeyPivProviderManager.removePivProvider();
                            if (mConnectionCallback != null) {
                                mConnectionCallback.onClosedConnection();
                            }
                        }

                    });
                }
            }
        });
        return true;
    }

    /**
     * Ceases usb discovery for YubiKeys.
     * Usually called when a host fragment is destroyed.
     */
    @Override
    void stopDiscovery(@NonNull final Activity activity) {
        //Usb discovery is meant to be on for the duration of the authentication WebView being active.
        //Therefore, discovery for Usb should only be stopped upon the WebView being terminated.
        //Note that this differs from the Nfc implementation, where Nfc discovery is only turned on
        // at specific times where it is necessary to connect.
        synchronized (sDeviceLock) {
            mUsbDevice = null;
            mUsbYubiKeyManager.disable();
        }
    }

    /**
     * Request a PivSession instance in order to carry out methods
     *  implemented in YubiKitSmartcardSession.
     * @param callback Contains callbacks to run when a PivSession is successfully instantiated and when any exception is thrown due to a connection issue.
     */
    @Override
    void requestDeviceSession(@NonNull ISessionCallback callback) {
        final String methodTag = TAG + "requestDeviceSession:";
        synchronized (sDeviceLock) {
            if (isDeviceConnected()) {
                //Request a connection from mUsbDevice so that we can get a PivSession instance.
                mUsbDevice.requestConnection(UsbSmartCardConnection.class, new Callback<Result<UsbSmartCardConnection, IOException>>() {
                    @Override
                    public void invoke(@NonNull final Result<UsbSmartCardConnection, IOException> value) {
                        try {
                            final SmartCardConnection c = value.getValue();
                            final PivSession piv = new PivSession(c);
                            final YubiKitSmartcardSession session = new YubiKitSmartcardSession(piv);
                            callback.onGetSession(session);
                        } catch (final Exception e) {
                            callback.onException(e);
                        }
                    }
                });
                return;
            }
            Logger.error(methodTag, DEVICE_ERROR_MESSAGE, null);
            callback.onException(new Exception());
        }
    }

    /**
     * Checks if a YubiKey is currently connected via USB.
     * @return true if YubiKey is currently connected. Otherwise, false.
     */
    @Override
    boolean isDeviceConnected() {
        synchronized (sDeviceLock) {
            return mUsbDevice != null;
        }
    }

    /**
     * Runs implementation specific processes that may need to occur just before calling {@link ClientCertRequest#proceed(PrivateKey, X509Certificate[])}.
     * @param telemetryHelper CertBasedAuthTelemetryHelper instance.
     */
    @Override
    void initBeforeProceedingWithRequest(@NonNull ICertBasedAuthTelemetryHelper telemetryHelper) {
        YubiKeyPivProviderManager.addPivProvider(telemetryHelper, getPivProviderCallback());
    }

    /**
     * Cleanup to be done upon host activity being destroyed.
     * @param activity current host activity.
     */
    @Override
    void onDestroy(@NonNull final Activity activity) {
        stopDiscovery(activity);
    }

    /**
     * Used to provide PivProvider constructor a Callback that will establish a new PivSession when it is needed.
     * @return A Callback which returns a Callback that will return a new PivSession instance.
     */
    @NonNull
    Callback<Callback<Result<PivSession, Exception>>> getPivProviderCallback() {
        final String methodTag = TAG + "getPivProviderCallback:";
        return new Callback<Callback<Result<PivSession, Exception>>>() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void invoke(@NonNull final Callback<Result<PivSession, Exception>> callback) {
                synchronized (sDeviceLock) {
                    if (isDeviceConnected()) {
                        mUsbDevice.requestConnection(UsbSmartCardConnection.class, new Callback<Result<UsbSmartCardConnection, IOException>>() {
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
                        return;
                    }
                    Logger.error(methodTag, DEVICE_ERROR_MESSAGE, null);
                    callback.invoke(Result.failure(new Exception(DEVICE_ERROR_MESSAGE)));
                }
            }
        };
    }
}
