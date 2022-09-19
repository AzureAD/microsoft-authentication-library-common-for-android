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

import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.microsoft.identity.common.internal.telemetry.Telemetry;
import com.microsoft.identity.common.java.telemetry.events.PivProviderStatusEvent;
import com.microsoft.identity.common.logging.Logger;
import com.yubico.yubikit.android.YubiKitManager;
import com.yubico.yubikit.android.transport.usb.UsbConfiguration;
import com.yubico.yubikit.android.transport.usb.UsbYubiKeyDevice;
import com.yubico.yubikit.android.transport.usb.connection.UsbSmartCardConnection;
import com.yubico.yubikit.core.smartcard.SmartCardConnection;
import com.yubico.yubikit.core.util.Callback;
import com.yubico.yubikit.core.util.Result;
import com.yubico.yubikit.piv.PivSession;
import com.yubico.yubikit.piv.jca.PivProvider;

import java.io.IOException;
import java.security.Security;
import java.util.concurrent.Callable;

/**
 * Utilizes YubiKit in order to detect and interact with YubiKeys for smartcard certificate based authentication.
 */
public class YubiKitCertBasedAuthManager implements ISmartcardCertBasedAuthManager {

    private static final String TAG = YubiKitCertBasedAuthManager.class.getSimpleName();
    private static final String MDEVICE_NULL_ERROR_MESSAGE = "Instance UsbYubiKitDevice variable (mDevice) is null.";
    private static final String YUBIKEY_PROVIDER = "YKPiv";

    private final YubiKitManager mYubiKitManager;
    private UsbYubiKeyDevice mDevice;
    //Lock to help facilitate synchronization
    private static final Object sDeviceLock = new Object();

    /**
     * Create new instance of YubiKitCertBasedAuthManager.
     * @param context current application context.
     */
    YubiKitCertBasedAuthManager(@NonNull final Context context) {
        mYubiKitManager = new YubiKitManager(context);
    }

    /**
     * Create and start YubiKitManager for UsbDiscovery mode.
     * When in Usb Discovery mode, Yubikeys that plug into the device will be accessible
     *  once the user provides permission via the Android permission dialog.
     * @param startDiscoveryCallback Contains callbacks to run when a YubiKey is connected and disconnected.
     */
    @Override
    public void startDiscovery(@NonNull final IStartDiscoveryCallback startDiscoveryCallback) {
        mYubiKitManager.startUsbDiscovery(new UsbConfiguration(), new Callback<UsbYubiKeyDevice>() {
            @Override
            public void invoke(@NonNull UsbYubiKeyDevice device) {
                Logger.verbose(TAG, "A YubiKey device was connected");
                synchronized (sDeviceLock) {
                    mDevice = device;
                    startDiscoveryCallback.onCreateConnection();

                    mDevice.setOnClosed(new Runnable() {
                        @Override
                        public void run() {
                            Logger.verbose(TAG, "A YubiKey device was disconnected");
                            synchronized (sDeviceLock) {
                                mDevice = null;
                            }
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
            }
        });
    }

    /**
     * Ceases usb discovery for YubiKeys.
     * Usually called when a host fragment is destroyed.
     */
    @Override
    public void stopDiscovery() {
        mYubiKitManager.stopUsbDiscovery();
    }

    /**
     * Request a PivSession instance in order to carry out methods
     *  implemented in YubiKitSmartcardSession.
     * @param callback Contains callbacks to run when a PivSession is successfully instantiated and when any exception is thrown due to a connection issue.
     */
    @Override
    public void requestDeviceSession(@NonNull final ISessionCallback callback) {
        final String methodTag = TAG + "requestDeviceSession:";
        synchronized (sDeviceLock) {
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
                        final YubiKitSmartcardSession session = new YubiKitSmartcardSession(piv);
                        callback.onGetSession(session);
                    } catch (final Exception e) {
                        callback.onException(e);
                    }
                }
            });
        }
    }

    /**
     * Checks if a YubiKey is currently connected.
     * @return true if YubiKey is currently connected. Otherwise, false.
     */
    @Override
    public boolean isDeviceConnected() {
        synchronized (sDeviceLock) {
            return mDevice != null;
        }
    }

    /**
     * Adds a PivProvider instance to the Java static Security List (and emits relevant telemetry).
     */
    @Override
    public void prepareForAuth() {
        final String methodTag = TAG + ":PrepareForAuth";
        //Need to add a PivProvider instance to the beginning of the array of Security providers in order for signature logic to occur.
        //Note that this provider is removed when the UsbYubiKeyDevice connection is closed.
        final PivProviderStatusEvent pivProviderStatusEvent = new PivProviderStatusEvent();
        if (Security.getProvider(YUBIKEY_PROVIDER) != null) {
            Security.removeProvider(YUBIKEY_PROVIDER);
            //The PivProvider instance is either unexpectedly being added elsewhere
            // or it isn't being removed properly upon CBA flow termination.
            Telemetry.emit(pivProviderStatusEvent.putIsExistingPivProviderPresent(true));
            Logger.info(methodTag, "Existing PivProvider was present in Security static list.");
        } else {
            Telemetry.emit(pivProviderStatusEvent.putIsExistingPivProviderPresent(false));
            Logger.info(methodTag, "Security static list does not have existing PivProvider.");
        }
        //The position parameter is 1-based (1 maps to index 0).
        Security.insertProviderAt(new PivProvider(getPivProviderCallback()), 1);
        Logger.info(methodTag, "An instance of PivProvider was added to Security static list.");
    }

    /**
     * Cleanup to be done upon host activity being destroyed.
     */
    @Override
    public void onDestroy() {
        stopDiscovery();
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
                synchronized (sDeviceLock) {
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
            }
        };
    }
}
