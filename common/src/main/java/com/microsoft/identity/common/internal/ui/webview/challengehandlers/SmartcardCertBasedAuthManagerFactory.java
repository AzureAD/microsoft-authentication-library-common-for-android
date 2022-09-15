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

/**
 * Instantiates ISmartcardCertBasedAuthManagers for certificate based authentication.
 */
public class SmartcardCertBasedAuthManagerFactory {

    /**
     * Creates and returns an applicable instance of ISmartcardCertBasedAuthManager.
     * @param activity Current host activity.
     * @return A ISmartcardCertBasedAuthManager implementation instance.
     */
    public static ISmartcardCertBasedAuthManager createSmartcardCertBasedAuthManager(Activity activity) {
        //Return instance of YubiKitCertBasedAuthManager, since this is the only implementation of
        // ISmartcardCertBasedAuthManager we have right now.
        return new YubiKitCertBasedAuthManager(activity);
    }
}
