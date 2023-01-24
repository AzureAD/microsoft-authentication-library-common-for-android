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

package com.microsoft.identity.common.internal.broker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.os.Build;
import android.util.Base64;

import androidx.annotation.NonNull;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.logging.Logger;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Gets information about calling activity.
 */
public class PackageHelper {
    private static final String TAG = "CallerInfo";

    private PackageManager mPackageManager;

    /**
     * Creates helper to check caller info.
     *
     * @param packageManager The android PackageManager
     */
    public PackageHelper(final PackageManager packageManager) {
        mPackageManager = packageManager;
    }

    /**
     * Reads first signature in the list for given package name.
     *
     * @param packageName name of the package for which signature should be returned
     * @return signature for package
     */
    public String getCurrentSignatureForPackage(final String packageName) {
        final String methodTag = TAG + ":getCurrentSignatureForPackage";
        try {
            return getCurrentSignatureForPackage(getPackageInfo(mPackageManager, packageName));
        } catch (NameNotFoundException e) {
            Logger.error(methodTag, "Calling App's package does not exist in PackageManager. ", "", e);
        }
        return null;
    }

    /**
     * Reads first signature in the list for given package name.
     *
     * @param packageInfo package for which signature should be returned
     * @return signature for package
     */
    public static String getCurrentSignatureForPackage(final PackageInfo packageInfo) {
        final String methodTag = TAG + ":getCurrentSignatureForPackage";
        try {
            final Signature[] signatures = getSignatures(packageInfo);
            if (signatures != null && signatures.length > 0) {
                final Signature signature = signatures[0];
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                return Base64.encodeToString(md.digest(), Base64.NO_WRAP);
            }
        } catch (NoSuchAlgorithmException e) {
            Logger.error(methodTag, "Digest SHA algorithm does not exists. ", "", e);
        }
        return null;
    }

    /**
     * Gets the kernel user-ID that has been assigned to this application.
     *
     * @param packageName for which the user id has to be returned
     * @return UID user id
     */
    public int getUIDForPackage(final String packageName) {
        final String methodTag = TAG + ":getUIDForPackage";
        int callingUID = 0;
        try {
            final ApplicationInfo info = mPackageManager.getApplicationInfo(packageName, 0);
            if (info != null) {
                callingUID = info.uid;
            }
        } catch (NameNotFoundException e) {
            Logger.error(methodTag, "Package is not found. ", "Package name: " + packageName, e);
        }
        return callingUID;
    }

    /**
     * Check if the given package is installed and enabled.
     *
     * @param packageName the package name to look up.
     * @return true if the package is installed and enabled. Otherwise, returns false.
     */
    public static boolean isPackageInstalledAndEnabled(final Context context, final String packageName) {
        final String methodTag = TAG + ":isPackageInstalledAndEnabled";
        boolean enabled = false;
        PackageManager pm = context.getPackageManager();
        try {
            ApplicationInfo applicationInfo = pm.getApplicationInfo(packageName, 0);
            if (applicationInfo != null) {
                enabled = applicationInfo.enabled;
            }
        } catch (NameNotFoundException e) {
            Logger.error(methodTag, "Package is not found. Package name: " + packageName, e);
        }

        Logger.info(methodTag, " Is package installed and enabled? [" + enabled + "]");
        return enabled;
    }

    /**
     * Gets redirect uri for broker.
     *
     * @param packageName     application package name
     * @param signatureDigest application signature
     * @return broker redirect url
     */
    public static String getBrokerRedirectUrl(final String packageName, final String signatureDigest) {
        final String methodTag = TAG + ":getBrokerRedirectUrl";
        if (!StringExtensions.isNullOrBlank(packageName)
                && !StringExtensions.isNullOrBlank(signatureDigest)) {
            try {
                return String.format("%s://%s/%s", AuthenticationConstants.Broker.REDIRECT_PREFIX,
                        URLEncoder.encode(packageName, AuthenticationConstants.ENCODING_UTF8),
                        URLEncoder.encode(signatureDigest, AuthenticationConstants.ENCODING_UTF8));
            } catch (UnsupportedEncodingException e) {
                // This encoding issue will happen at the beginning of API call,
                // if it is not supported on this device. ADAL uses one encoding
                // type.
                Logger.error(methodTag, "", "Encoding is not supported", e);
            }
        }
        return "";
    }

    /**
     * Helper method to get signatures in a back-compatible way
     *
     * @param packageInfo A packageInfo instance with the flag PackageManager.GET_SIGNING_CERTIFICATES/PackageManager.GET_SIGNATURES set
     * @return Signature[] or null
     */
    public static Signature[] getSignatures(final PackageInfo packageInfo) {
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

    public static int getPackageManagerSignaturesFlag() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return PackageManager.GET_SIGNING_CERTIFICATES;
        }

        return PackageManager.GET_SIGNATURES;
    }

    public static PackageInfo getPackageInfo(@NonNull PackageManager packageManager, @NonNull String packageName) throws PackageManager.NameNotFoundException {
        return packageManager.getPackageInfo(packageName, getPackageManagerSignaturesFlag());
    }

    public static Signature[] getSignatures(@NonNull Context context) throws PackageManager.NameNotFoundException {
        return getSignatures(getPackageInfo(context.getPackageManager(), context.getPackageName()));
    }
}
