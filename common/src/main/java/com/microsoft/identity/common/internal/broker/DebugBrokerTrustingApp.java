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
package com.microsoft.identity.common.internal.broker;

import android.content.Context;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

/**
 * Represents packageName and SignatureHash of an application that can trust debug brokers.
 */
@Data
@AllArgsConstructor
public class DebugBrokerTrustingApp {

    private static final String MSAL_TEST_APP_PACKAGE_NAME = "com.microsoft.identity.client.msal.testapp";
    private static final String MSAL_AUTOMATION_APP_PACKAGE_NAME = "com.microsoft.identity.client.msal.automationapp";
    private static final String MSAL_TEST_APP_SIGNATURE = "1wIqXSqBj7w+h11ZifsnqwgyKrY=";
    private static final String MSAL_AUTOMATION_APP_SIGNATURE = "1wIqXSqBj7w+h11ZifsnqwgyKrY=";

    public static final DebugBrokerTrustingApp MSAL_TEST_APP = new DebugBrokerTrustingApp(
            MSAL_TEST_APP_PACKAGE_NAME,
            MSAL_TEST_APP_SIGNATURE);

    public static final DebugBrokerTrustingApp MSAL_AUTOMATION_APP = new DebugBrokerTrustingApp(
            MSAL_AUTOMATION_APP_PACKAGE_NAME,
            MSAL_AUTOMATION_APP_SIGNATURE
    );

    @NonNull
    private String packageName;
    @NonNull
    private String signatureHash;

    /**
     * Verify whether the application can trust debug brokers in release builds.
     *
     * @param context the application context
     * @return a boolean that represents whether the context belongs to the {{@link #DebugBrokerTrustingApp(String, String)}}
     */
    public boolean verify(Context context) {
        final String packageName = context.getApplicationContext().getPackageName();
        final PackageHelper info = new PackageHelper(context.getPackageManager());
        final String signatureDigest = info.getCurrentSignatureForPackage(packageName);

        return this.signatureHash.equals(signatureDigest) && this.packageName.equals(packageName);
    }
}
