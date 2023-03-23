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

import androidx.annotation.NonNull;

import com.microsoft.identity.common.java.opentelemetry.ICertBasedAuthTelemetryHelper;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

class TestNfcSmartcardCertBasedAuthManager extends AbstractNfcSmartcardCertBasedAuthManager {

    private final List<ICertDetails> mCertDetailsList;
    private boolean mIsConnected;
    private int mPinAttemptsRemaining;

    public TestNfcSmartcardCertBasedAuthManager(@NonNull final List<X509Certificate> certList) {
        mIsConnected = false;
        //Attempts remaining is usually 3, but 2 attempts is all that's necessary for testing.
        mPinAttemptsRemaining = 2;
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
    boolean startDiscovery(@NonNull final Activity activity) {
        return false;
    }

    @Override
    void stopDiscovery(@NonNull final Activity activity) {
    }

    @Override
    void requestDeviceSession(@NonNull final ISessionCallback callback) {
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
    void initBeforeProceedingWithRequest(@NonNull final ICertBasedAuthTelemetryHelper telemetryHelper) {}

    @Override
    void onDestroy(@NonNull final Activity activity) {
        stopDiscovery(activity);
    }

    public void mockConnect(final boolean isChanged) {
        mIsConnected = true;
        isDeviceChanged = isChanged;
        if (mConnectionCallback != null) {
            mConnectionCallback.onCreateConnection();
        }
    }
}
