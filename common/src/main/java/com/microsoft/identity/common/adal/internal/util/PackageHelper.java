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

package com.microsoft.identity.common.adal.internal.util;

import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.os.Bundle;
import android.util.Base64;

import com.microsoft.identity.common.adal.internal.ADALError;
import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.internal.logging.Logger;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Gets information about calling activity.
 */
public class PackageHelper {
    private static final String TAG = "CallerInfo";

    private Context mContext;

    private final AccountManager mAcctManager; //NOPMD

    /**
     * Creates helper to check caller info.
     *
     * @param ctx The android app/activity context
     */
    public PackageHelper(Context ctx) {
        mContext = ctx;
        mAcctManager = AccountManager.get(mContext);
    }

    /**
     * Gets metadata information from AndroidManifest file.
     *
     * @param packageName
     * @param component
     * @param metaDataName
     * @return MetaData
     */
    @SuppressLint("WrongConstant")
    Object getValueFromMetaData(final String packageName, final ComponentName component,
                                final String metaDataName) {

        String methodName = "getValueFromMetadata";
        try {
            Logger.info(TAG + methodName,"Calling package:" + packageName);
            if (component != null) {
                Logger.verbose(TAG + methodName,"component:" + component.flattenToString());
                ActivityInfo ai = mContext.getPackageManager().getActivityInfo(component,
                        PackageManager.GET_ACTIVITIES | PackageManager.GET_META_DATA);
                if (ai != null) {
                    Bundle metaData = ai.metaData;
                    if (metaData == null) {
                        Logger.verbose(TAG + methodName,"metaData is null. Unable to get meta data for "
                                + metaDataName);
                    } else {
                        Object value = (Object)metaData.get(metaDataName);
                        return value;
                    }
                }
            } else {
                Logger.verbose(TAG + methodName,"calling component is null.");
            }
        } catch (NameNotFoundException e) {
            Logger.error(TAG + methodName,"ActivityInfo is not found", e);
        }
        return null;
    }

    /**
     * Reads first signature in the list for given package name.
     *
     * @param packagename name of the package for which signature should be returned
     * @return signature for package
     */
    @SuppressLint("PackageManagerGetSignatures")
    public String getCurrentSignatureForPackage(final String packagename) {
        String methodName = "getCurrentSignatureForPackage";
        try {
            PackageInfo info = mContext.getPackageManager().getPackageInfo(packagename,
                    PackageManager.GET_SIGNATURES);
            if (info != null && info.signatures != null && info.signatures.length > 0) {
                Signature signature = info.signatures[0];
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                return Base64.encodeToString(md.digest(), Base64.NO_WRAP);
                // Server side needs to register all other tags. ADAL will
                // send one of them.
            }
        } catch (NameNotFoundException e) {
            Logger.error(TAG + methodName,"Calling App's package does not exist in PackageManager.", e);
        } catch (NoSuchAlgorithmException e) {
            Logger.error(TAG + methodName,"Digest SHA algorithm does not exists. ", e);
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
        String methodName = "getUIDForPackage";
        int callingUID = 0;
        try {
            final ApplicationInfo info = mContext.getPackageManager().getApplicationInfo(packageName, 0);
            if (info != null) {
                callingUID = info.uid;
            }
        } catch (NameNotFoundException e) {
            Logger.error(TAG + methodName,"Package is not found. Package name: "+ packageName, e);
        }
        return callingUID;
    }

    /**
     * Check if the given package is installed and enabled.
     *
     * @param packageName the package name to look up.
     * @return true if the package is installed and enabled. Otherwise, returns false.
     */
    public boolean isPackageInstalledAndEnabled(final String packageName) {
        final String methodName = "#isPackageInstalledAndEnabled";
        Boolean enabled = false;
        PackageManager pm = mContext.getPackageManager();
        try {
            ApplicationInfo applicationInfo = pm.getApplicationInfo(packageName, 0);
            if (applicationInfo != null) {
                enabled = applicationInfo.enabled;
            }
        } catch (NameNotFoundException e) {
            Logger.error(TAG + methodName, "Package is not found. Package name: " + packageName, e);
        }

        Logger.verbose(TAG + methodName, " Is package installed and enabled? [" + enabled + "]");
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
        String methodName = "getBrokerRedirectUrl";
        if (!StringExtensions.isNullOrBlank(packageName)
                && !StringExtensions.isNullOrBlank(signatureDigest)) {
            // If the caller is the Authenticator, then use the broker redirect URI.
            if (packageName.equals(AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_PACKAGE_NAME) &&
                    signatureDigest.equals(AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_SIGNATURE)) {
                return AuthenticationConstants.Broker.BROKER_REDIRECT_URI;
            }

            try {
                return String.format("%s://%s/%s", AuthenticationConstants.Broker.REDIRECT_PREFIX,
                        URLEncoder.encode(packageName, AuthenticationConstants.ENCODING_UTF8),
                        URLEncoder.encode(signatureDigest, AuthenticationConstants.ENCODING_UTF8));
            } catch (UnsupportedEncodingException e) {
                // This encoding issue will happen at the beginning of API call,
                // if it is not supported on this device. ADAL uses one encoding
                // type.
                Logger.error(TAG + methodName,ADALError.ENCODING_IS_NOT_SUPPORTED.getDescription(), e);
            }
        }
        return "";
    }
}

