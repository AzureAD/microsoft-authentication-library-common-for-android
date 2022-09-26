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

import androidx.annotation.NonNull;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/**
 * An abstract manager that can control connections for a particular type of smartcard.
 */
public abstract class AbstractSmartcardCertBasedAuthManager {

    protected IConnectionCallback mConnectionCallback;
    protected IDiscoveryExceptionCallback mDiscoveryExceptionCallback;

    /**
     * Logic to prepare an Android device to detect smartcards via usb.
     */
    abstract void startDiscovery();

    /**
     * Cease usb discovery of smartcards.
     */
    abstract void stopDiscovery();

    /**
     * Request an instance of a session in order to carry out methods specific to ISmartcardSession.
     * @param callback Contains callbacks to run when a ISmartcardSession is successfully instantiated and when any exception is thrown due to a connection issue.
     */
    abstract void requestDeviceSession(@NonNull final ISessionCallback callback);

    /**
     * Returns boolean based on if a smartcard device is currently connected to the Android device and detected by our code.
     * @return true if a device is currently connected, false otherwise.
     */
    abstract boolean isDeviceConnected();

    /**
     * Runs implementation specific processes that may need to occur just before calling {@link android.webkit.ClientCertRequest#proceed(PrivateKey, X509Certificate[])}.
     */
    abstract void initBeforeProceedingWithRequest();

    /**
     * Cleanup to be done upon host activity being destroyed.
     */
    abstract void onDestroy();

    /**
     * Sets callbacks to be run for when a smartcard connection is started and ended.
     * @param connectionCallback an implementation of IConnectionCallback.
     */
    public void setConnectionCallback(final IConnectionCallback connectionCallback) {
        mConnectionCallback = connectionCallback;
    }

    /**
     * Sets callback to be run for when an exception is thrown during discovery start up.
     * @param discoveryExceptionCallback an implementation of IDiscoveryExceptionCallback.
     */
    public void setDiscoveryExceptionCallback(final IDiscoveryExceptionCallback discoveryExceptionCallback) {
        mDiscoveryExceptionCallback = discoveryExceptionCallback;
    }

    /**
     * Callback methods to be run upon initial connection and disconnection of a smartcard device.
     */
    interface IConnectionCallback {
        /**
         * Logic to be run upon initial connection of a smartcard device.
         */
        void onCreateConnection();

        /**
         * Logic to be run upon disconnection of a smartcard device.
         */
        void onClosedConnection();
    }

    /**
     * Callback method to be run upon an exception thrown during discovery start up.
     */
    interface IDiscoveryExceptionCallback {
        /**
         * Logic to be run when an exception is thrown.
         */
        void onException();
    }

    /**
     * Callback which will contain code to be run upon creation of a ISmartcardSession instance.
     */
    interface ISessionCallback {
        /**
         * Code depending on a ISmartcardSession instance to be run.
         * @param session ISmartcardSession instance.
         */
        void onGetSession(@NonNull final ISmartcardSession session) throws Exception;

        /**
         * Code to be run on any exception thrown during the process of instantiating or interacting with a ISmartcardSession.
         * @param e Exception thrown.
         */
        void onException(@NonNull final Exception e);
    }
}
