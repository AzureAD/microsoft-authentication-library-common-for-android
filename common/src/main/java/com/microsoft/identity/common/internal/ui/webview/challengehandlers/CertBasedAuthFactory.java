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

import androidx.annotation.NonNull;

/**
 * Instantiates handlers and managers for certificate based authentication.
 */
public class CertBasedAuthFactory {

    /**
     * Creates and returns an applicable instance of ISmartcardCertBasedAuthManager.
     * @param context Current application context.
     * @return An ISmartcardCertBasedAuthManager implementation instance.
     */
    @NonNull
    public static ISmartcardCertBasedAuthManager createSmartcardCertBasedAuthManager(@NonNull final Context context) {
        //Return instance of YubiKitCertBasedAuthManager, since this is the only implementation of
        // ISmartcardCertBasedAuthManager we have right now.
        return new YubiKitCertBasedAuthManager(context);
    }

    /**
     * Creates and returns an applicable instance of ICertBasedAuthChallengeHandler.
     * @param activity current host activity.
     * @param smartcardCertBasedAuthManager an instance of ISmartcardCertBasedAuthManager.
     * @param dialogHolder an instance of DialogHolder.
     * @return An ICertBasedAuthChallengeHandler implementation instance.
     */
    @NonNull
    public static ICertBasedAuthChallengeHandler createCertBasedAuthChallengeHandler(@NonNull final Activity activity,
                                                                                     @NonNull final ISmartcardCertBasedAuthManager smartcardCertBasedAuthManager,
                                                                                     @NonNull final DialogHolder dialogHolder) {
        if (smartcardCertBasedAuthManager.isDeviceConnected()) {
            return new SmartcardCertBasedAuthChallengeHandler(smartcardCertBasedAuthManager, dialogHolder);
        } else {
            return new OnDeviceCertBasedAuthChallengeHandler(activity);
        }
    }

    /**
     * Create and returns a new instance of DialogHolder.
     * @param activity current host activity.
     * @return an instance of DialogHolder.
     */
    @NonNull
    public static DialogHolder createDialogHolder(@NonNull final Activity activity) {
        return new DialogHolder(activity);
    }
}
