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

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.security.KeyChain;
import android.security.KeyChainAliasCallback;
import android.security.KeyChainException;
import android.util.Log;
import android.webkit.ClientCertRequest;

import androidx.annotation.NonNull;

import com.microsoft.identity.common.logging.Logger;
import com.yubico.yubikit.android.YubiKitManager;
import com.yubico.yubikit.android.transport.usb.UsbConfiguration;
import com.yubico.yubikit.android.transport.usb.UsbYubiKeyDevice;
import com.yubico.yubikit.android.transport.usb.connection.UsbSmartCardConnection;
import com.yubico.yubikit.core.smartcard.SmartCardConnection;
import com.yubico.yubikit.core.util.Callback;
import com.yubico.yubikit.core.util.Result;
import com.yubico.yubikit.piv.PivSession;

import java.io.IOException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

public class EoClientCertAuthChallengeHandler extends ClientCertAuthChallengeHandler {
    private static final String TAG = EoClientCertAuthChallengeHandler.class.getSimpleName();
    private Activity mActivity;
    private final YubiKitManager mYubiKitManager;

    public EoClientCertAuthChallengeHandler(@NonNull final Activity activity) {
        super(activity);
        mActivity = activity;
        //Create and start YubiKitManager
        mYubiKitManager = new YubiKitManager(mActivity.getApplicationContext());
        mYubiKitManager.startUsbDiscovery(new UsbConfiguration(), new Callback<UsbYubiKeyDevice>() {
            @Override
            public void invoke(UsbYubiKeyDevice device) {
                Log.i(TAG, "A YubiKey device was connected");
                device.setOnClosed(new Runnable() {
                    @Override
                    public void run() {
                        Log.i(TAG, "A YubiKey device was disconnected");
                    }
                });
            }
        });

    }
    //processChallenge is the same for now.
    //But this method will eventually include logic for YubiKeys as well.
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public Void processChallenge(@NonNull final ClientCertRequest request) {
        final String methodTag = TAG + ":processChallenge";
        return handleOnDeviceCertAuth(request, TAG);
    }

    //Allows AzureActiveDirectoryWebViewCLient to stop mYubiKitManager's discovery mode.
    public void stopYubiKitManagerUsbDiscovery() {
        //Stop UsbDiscovery for YubiKitManager
        //Should be called when host fragment is destroyed.
        mYubiKitManager.stopUsbDiscovery();
    }
}
