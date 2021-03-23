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

package com.microsoft.identity.common.internal.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;

import androidx.annotation.NonNull;

/**
 * Helper class with some utility methods to get PackageInfo and signatures in a back-compatible way
 */
public class SignUtil {

    /**
     * Helper method to get signatures in a back-compatible way
     *
     * @param packageInfo A packageInfo instance with the flag PackageManager.GET_SIGNING_CERTIFICATES/PackageManager.GET_SIGNATURES set
     * @return Signature[] or null
     */
    public static Signature[] getSignatures(PackageInfo packageInfo) {
        if (packageInfo == null) {
            return null;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (packageInfo.signingInfo == null) {
                return null;
            }
            if (packageInfo.signingInfo.hasMultipleSigners()) {
                return packageInfo.signingInfo.getApkContentsSigners();
            } else {
                return packageInfo.signingInfo.getSigningCertificateHistory();
            }
        }

        return packageInfo.signatures;
    }

    public static int getPackageManagerFlag() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return PackageManager.GET_SIGNING_CERTIFICATES;
        }

        return PackageManager.GET_SIGNATURES;
    }

    public static PackageInfo getPackageInfo(@NonNull PackageManager packageManager, @NonNull String packageName) throws PackageManager.NameNotFoundException {
        return packageManager.getPackageInfo(packageName, getPackageManagerFlag());
    }

    public static PackageInfo getPackageInfo(@NonNull Context context, @NonNull String packageName) throws PackageManager.NameNotFoundException {
        return getPackageInfo(context.getPackageManager(), packageName);
    }

    public static PackageInfo getPackageInfo(@NonNull Context context) throws PackageManager.NameNotFoundException {
        return getPackageInfo(context, context.getPackageName());
    }

    public static Signature[] getSignatures(@NonNull Context context) throws PackageManager.NameNotFoundException {
        return getSignatures(getPackageInfo(context));
    }
}
