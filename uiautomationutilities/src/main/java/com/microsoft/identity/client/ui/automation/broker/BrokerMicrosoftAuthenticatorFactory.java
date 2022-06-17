//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.client.ui.automation.broker;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import androidx.test.core.app.ApplicationProvider;

/**
 * Factory class to create broker implementations based on the version of Authenticator app under test
 */
public class BrokerMicrosoftAuthenticatorFactory {

    public final static String LATEST_VERSION_NUMBER = "6.2206.3949";

    public BrokerMicrosoftAuthenticator getAuthenticator() {
        final Context context = ApplicationProvider.getApplicationContext();
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo("com.azure.authenticator", 0);
            // authenticator app follows version number format - V.YYMM.XXXX
            // V is the major version, YYMM are last two digits of an year followed by month
            // XXXX is number of hours passed after Jan1st 00.00 of current year
            // String comparison of versions should work for this format
            if (packageInfo.versionName.compareTo(LATEST_VERSION_NUMBER) >= 0) {
                // Use latest automation code as this is the new updated authenticator app
                return new BrokerAuthenticatorUpdatedVersionImpl();
            } else {
                return new BrokerAuthenticatorPreviousVersionImpl();
            }
        } catch (final PackageManager.NameNotFoundException e) {
            throw new AssertionError(e);
        }
    }
}
