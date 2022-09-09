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
package com.microsoft.identity.common.shadows;

import android.app.Activity;

import androidx.annotation.NonNull;

import com.microsoft.identity.common.internal.ui.webview.challengehandlers.ICertDetails;
import com.microsoft.identity.common.internal.ui.webview.challengehandlers.ISmartcardCertBasedAuthManager;
import com.microsoft.identity.common.internal.ui.webview.challengehandlers.ISmartcardSession;
import com.microsoft.identity.common.internal.ui.webview.challengehandlers.SmartcardCertBasedAuthManagerFactory;
import com.microsoft.identity.common.internal.ui.webview.challengehandlers.YubiKitCertBasedAuthManager;

import org.junit.Test;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Implements(SmartcardCertBasedAuthManagerFactory.class)
public class ShadowSmartcardCertBasedAuthManagerFactory {

    @Implementation
    public static ISmartcardCertBasedAuthManager getSmartcardCertBasedAuthManager(Activity activity) {
        //Return instance of YubiKitCertBasedAuthManager, since this is the only implementation of
        // ISmartcardCertBasedAuthManager we have right now.
        TestSmartcardCertBasedAuthManager testSmartcardCertBasedAuthManager = new TestSmartcardCertBasedAuthManager();
        return testSmartcardCertBasedAuthManager;
    }

    private static class TestSmartcardCertBasedAuthManager implements ISmartcardCertBasedAuthManager {

        private IStartDiscoveryCallback mStartDiscoveryCallback;
        private boolean mIsConnected;

        public TestSmartcardCertBasedAuthManager() {
            mIsConnected = false;
        }

        @Override
        public void startDiscovery(IStartDiscoveryCallback startDiscoveryCallback) {
            mStartDiscoveryCallback = startDiscoveryCallback;
            mockConnect();
        }

        @Override
        public void stopDiscovery() {
            mockDisconnect();
        }

        @Override
        public void attemptDeviceSession(@NonNull ISessionCallback callback) {
            try {
                callback.onGetSession(new TestSmartcardSession());
            } catch (Exception e) {
                callback.onException(e);
            }
        }

        @Override
        public boolean isDeviceConnected() {
            return mIsConnected;
        }

        @Override
        public void prepareForAuth() {
            //Don't need anything
        }

        public void mockConnect() {
            if (mStartDiscoveryCallback != null) {
                mStartDiscoveryCallback.onStartDiscovery();
                mIsConnected = true;
            }
        }

        public void mockDisconnect() {
            if (mStartDiscoveryCallback != null) {
                mStartDiscoveryCallback.onClosedConnection();
                mIsConnected = false;
            }
        }
    }

    private static class TestSmartcardSession implements ISmartcardSession {

        private final List<ICertDetails> mCertDetailsList;
        private final char[] mPin;
        private int mPinAttemptsRemaining;

        public TestSmartcardSession() {
            mCertDetailsList = new ArrayList<>();
            mPin = new char[]{'1', '2', '3', '4', '5', '6'};
            mPinAttemptsRemaining = 3;
        }

        @NonNull
        @Override
        public List<ICertDetails> getCertDetailsList() throws Exception {
            return mCertDetailsList;
        }

        @Override
        public boolean verifyPin(char[] pin) throws Exception {
            if (Arrays.equals(mPin, pin)) {
                return true;
            } else {
                mPinAttemptsRemaining = mPinAttemptsRemaining > 0 ? mPinAttemptsRemaining - 1 : 0;
                return false;
            }
        }

        @Override
        public int getPinAttemptsRemaining() throws Exception {
            return mPinAttemptsRemaining;
        }

        @NonNull
        @Override
        public PrivateKey getKeyForAuth(ICertDetails certDetails, char[] pin) throws Exception {
            return new PrivateKey() {
                @Override
                public String getAlgorithm() {
                    return null;
                }

                @Override
                public String getFormat() {
                    return null;
                }

                @Override
                public byte[] getEncoded() {
                    return new byte[0];
                }
            };
        }

        public void addCert(@NonNull final X509Certificate cert) {
            mCertDetailsList.add(new ICertDetails() {
                @NonNull
                @Override
                public X509Certificate getCertificate() {
                    return cert;
                }
            });
        }

        public void resetPinAttemptsRemaining() {
            mPinAttemptsRemaining = 3;
        }
    }
}
