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

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.webkit.ClientCertRequest;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.microsoft.identity.common.java.opentelemetry.CertBasedAuthTelemetryHelper;
import com.microsoft.identity.common.logging.Logger;
import com.yubico.yubikit.android.transport.nfc.NfcConfiguration;
import com.yubico.yubikit.android.transport.nfc.NfcNotAvailable;
import com.yubico.yubikit.android.transport.nfc.NfcSmartCardConnection;
import com.yubico.yubikit.android.transport.nfc.NfcYubiKeyDevice;
import com.yubico.yubikit.android.transport.nfc.NfcYubiKeyManager;
import com.yubico.yubikit.core.smartcard.SmartCardConnection;
import com.yubico.yubikit.core.util.Callback;
import com.yubico.yubikit.core.util.Result;
import com.yubico.yubikit.piv.PivSession;

import java.io.IOException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.concurrent.Callable;

public class YubiKitNfcSmartcardCertBasedAuthManager extends AbstractNfcSmartcardCertBasedAuthManager {
    private static final String TAG = YubiKitNfcSmartcardCertBasedAuthManager.class.getSimpleName();
    private static final String DEVICE_ERROR_MESSAGE = "No NFC device is currently connected.";
    private static final int NFC_TIMEOUT = 5000;

    private final NfcYubiKeyManager mNfcYubiKitManager;
    private NfcYubiKeyDevice mNfcDevice;
    private byte[] mTagId;
    //Lock to help facilitate synchronization
    private static final Object sDeviceLock = new Object();

    public YubiKitNfcSmartcardCertBasedAuthManager(@NonNull final Context context) throws NfcNotAvailable {
        mNfcYubiKitManager = new NfcYubiKeyManager(context.getApplicationContext(), null);
        isDeviceChanged = false;
    }

    /**
     * Logic to prepare an Android device to detect smartcards via NFC.
     * @param activity current host activity.
     * @return true if user needs to turn on NFC capabilities;
     * false if NFC discovery successfully started or device doesn't have NFC capabilities.
     */
    @Override
    boolean startDiscovery(@NonNull final Activity activity) {
        try {
            mNfcYubiKitManager.enable(
                    activity,
                    new NfcConfiguration().timeout(NFC_TIMEOUT),
                    new Callback<NfcYubiKeyDevice>() {
                        @Override
                        public void invoke(@NonNull NfcYubiKeyDevice value) {
                            mNfcDevice = value;
                            final byte[] tagId = mNfcDevice.getTag().getId();
                            isDeviceChanged = (mTagId != null && !Arrays.equals(mTagId, tagId));
                            mTagId = tagId;
                            if (mConnectionCallback != null) {
                                mConnectionCallback.onCreateConnection();
                            }
                        }
                    });
            return false;
        } catch (@NonNull final NfcNotAvailable e) {
            //User will not be blocked from seeing the regular smartcard prompt,
            // but appropriate reminder dialog should be shown.
            Logger.info(TAG, "Device has NFC functionality turned off.");
            return true;
        }
    }

    /**
     * Cease NFC discovery of smartcards.
     * @param activity current host activity.
     */
    @Override
    void stopDiscovery(@NonNull Activity activity) {
        synchronized (sDeviceLock) {
            if (isDeviceConnected()) {
                mNfcDevice.remove(new Runnable() {
                    @Override
                    public void run() {
                        mNfcDevice = null;
                        mNfcYubiKitManager.disable(activity);
                    }
                });
                return;
            }
        }
        mNfcYubiKitManager.disable(activity);
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
                mNfcDevice.requestConnection(NfcSmartCardConnection.class, new Callback<Result<NfcSmartCardConnection, IOException>>() {
                    @Override
                    public void invoke(@NonNull Result<NfcSmartCardConnection, IOException> value) {
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
     * Checks if a YubiKey is currently connected via NFC.
     * @return true if YubiKey is currently connected. Otherwise, false.
     */
    @Override
    boolean isDeviceConnected() {
        synchronized (sDeviceLock) {
            return mNfcDevice != null;
        }
    }

    /**
     * Runs implementation specific processes that may need to occur just before calling {@link ClientCertRequest#proceed(PrivateKey, X509Certificate[])}.
     * @param telemetryHelper CertBasedAuthTelemetryHelper instance.
     */
    @Override
    void initBeforeProceedingWithRequest(@NonNull CertBasedAuthTelemetryHelper telemetryHelper) {
        //Need to add a PivProvider instance to the beginning of the array of Security providers in order for signature logic to occur.
        //Note that this provider is removed when the UsbYubiKeyDevice connection is closed.
        YubiKeyPivProviderManager.addPivProvider(telemetryHelper, getPivProviderCallback());
    }

    /**
     * Cleanup to be done upon host activity being destroyed.
     * @param activity current host activity.
     */
    @Override
    void onDestroy(@NonNull final Activity activity) {
        //Make sure that any stray PivProviders are removed.
        YubiKeyPivProviderManager.removePivProvider();
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
                        mNfcDevice.requestConnection(NfcSmartCardConnection.class, new Callback<Result<NfcSmartCardConnection, IOException>>() {
                            @Override
                            public void invoke(@NonNull final Result<NfcSmartCardConnection, IOException> value) {
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
