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
package com.microsoft.identity.common.java.logging;

import com.microsoft.identity.common.java.AuthenticationConstants;
import com.microsoft.identity.common.java.platform.Device;
import com.microsoft.identity.common.java.util.StringUtil;

import cz.msebera.httpclient.android.BuildConfig;
import lombok.NonNull;

/**
 * Simple helper class to pull product telemetry information from diagnostic context.
 */
public class ProductHelper {
    private static final String TAG = ProductHelper.class.getSimpleName();

    /**
     * The String to be returned if the value is not set.
     */
    protected static final String NOT_SET = "NOT_SET";

    /**
     * Returns the product (library) name by accessing it from DiagnosticContext.
     * @return product name from DiagnosticContext.
     */
    @NonNull
    public static String getProduct() {
        final String methodName = ":getProduct";

        final String product = DiagnosticContext.INSTANCE.getRequestContext().get(AuthenticationConstants.SdkPlatformFields.PRODUCT);
        if (StringUtil.isNullOrEmpty(product)) {
            Logger.warn(TAG + methodName, "Product is not set.", null);
            return NOT_SET;
        } else {
            return product;
        }
    }

    /**
     * Returns the product (library) version by accessing it from DiagnosticContext.
     * @return product version from DiagnosticContext.
     */
    @NonNull
    public static String getProductVersion() {
        final String methodName = ":getProductVersion";

        final String version = DiagnosticContext.INSTANCE.getRequestContext().get(AuthenticationConstants.SdkPlatformFields.VERSION);
        if (StringUtil.isNullOrEmpty(version)) {
            Logger.warn(TAG + methodName, "Product version is not set.", null);
            return StringUtil.isNullOrEmpty(BuildConfig.VERSION_NAME) ? "1.5.9-default" : BuildConfig.VERSION_NAME + "-default";
        } else {
            return version;
        }
    }
}
