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

package com.microsoft.identity.common.internal.util;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.UserManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.common.logging.Logger;

import java.util.Set;

public final class AccountManagerUtil {
    private static final String TAG = AccountManagerUtil.class.getSimpleName();

    private static final String MANIFEST_PERMISSION_MANAGE_ACCOUNTS = "android.permission.MANAGE_ACCOUNTS";

    private AccountManagerUtil() {
    }

    /**
     * To verify if the caller can use to AccountManager to communicate to broker.
     *
     * @param context       an Android context.
     * @param accountTypes  AccountManager account type to check (if they're being controlled by MDM).
     */
    public static boolean canUseAccountManagerOperation(final Context context,
                                                        final Set<String> accountTypes) {
        final String methodTag = TAG + ":canUseAccountManagerOperation:";

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Check user policy
            final UserManager userManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
            if (userManager.hasUserRestriction(UserManager.DISALLOW_MODIFY_ACCOUNTS)) {
                Logger.verbose(methodTag, "UserManager.DISALLOW_MODIFY_ACCOUNTS is enabled for this user.");
                return false;
            }

            // Check if our account type is disabled.
            final DevicePolicyManager devicePolicyManager = getDevicePolicyManager(context);
            if (devicePolicyManager != null) {
                final String[] accountTypesWithManagementDisabled = devicePolicyManager.getAccountTypesWithManagementDisabled();
                if (accountTypesWithManagementDisabled != null) {
                    for (final String disabledAccountType : accountTypesWithManagementDisabled) {
                        if (accountTypes.contains(disabledAccountType)) {
                            Logger.info(methodTag, "Account type " + disabledAccountType +
                                    " is disabled by MDM.");
                            return false;
                        }
                    }
                }
            }

            // Before Android 6.0, the MANAGE_ACCOUNTS permission is required in the app's manifest xml file.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return true;
            } else {
                return isPermissionGranted(context, MANIFEST_PERMISSION_MANAGE_ACCOUNTS);
            }
        }

        // Unable to determine - treat this as false.
        // If the restriction exists and we make an accountManager call, then the OS will pop a dialog up.
        Logger.verbose(methodTag,
                "Cannot verify. Skipping AccountManager operation.");
        return false;
    }

    @Nullable
    private static DevicePolicyManager getDevicePolicyManager(Context context) {
        final String methodTag = TAG + ":getDevicePolicyManager:";
        try {
            return (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        } catch (@NonNull final Throwable t){
            Logger.verbose(methodTag, "Cannot get DevicePolicyManager.");
            return null;
        }
    }

    public static boolean isPermissionGranted(@NonNull final Context context,
                                              @NonNull final String permissionName) {
        final String methodTag = TAG + ":isPermissionGranted";
        final PackageManager pm = context.getPackageManager();
        final boolean isGranted = pm.checkPermission(permissionName, context.getPackageName())
                == PackageManager.PERMISSION_GRANTED;
        Logger.verbose(methodTag, "is " + permissionName + " granted? [" + isGranted + "]");
        return isGranted;
    }
}
