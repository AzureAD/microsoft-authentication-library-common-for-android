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

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Build;

import com.microsoft.identity.common.java.flighting.CommonFlight;
import com.microsoft.identity.common.java.flighting.CommonFlightsManager;
import com.microsoft.identity.common.logging.Logger;

import java.util.List;

import lombok.NonNull;

public class WorkProfileUtil {
    private static final String TAG = WorkProfileUtil.class.getSimpleName();

    /**
     * Helper method to check if we are in personal profile but a work profile managed by clouddpc
     * is available.
     * <a href="https://developers.google.com/android/management/work-profile-detection#detect_if_the_device_has_a_work_profile">Google Docs for intent used</a>
     * @param context context needed to check for intent
     * @return true if called in personal profile and a work profile managed by clouddpc exists, false otherwise
     */
    public static boolean checkIfIsInPersonalProfileButClouddpcWorkProfileAvailable(@NonNull final Context context) {
        // If the flight is not enabled, just return false
        if (!CommonFlightsManager.INSTANCE.getFlightsProvider().isFlightEnabled(CommonFlight.ENABLE_AM_API_WORKPROFILE_EXTRA_QUERY_PARAMETERS)) {
            return false;
        }
        
        try {
            final Intent intent = new Intent("com.google.android.apps.work.clouddpc.ACTION_DETECT_WORK_PROFILE");
            final List<ResolveInfo> activities = context.getPackageManager().queryIntentActivities(intent, 0);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                return activities.stream()
                        .anyMatch(
                                (ResolveInfo resolveInfo) -> resolveInfo.isCrossProfileIntentForwarderActivity());
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    return activities.stream()
                            .anyMatch(
                                    (ResolveInfo resolveInfo) -> resolveInfo.activityInfo.name.equals("com.android.internal.app.ForwardIntentToManagedProfile"));
                }
            }

            return false;
        } catch (Exception e) {
            // If we run into exception for any reason, we'll just return false
            Logger.warn(TAG, "Received an exception while trying to check if clouddpc work profile is available: " + e.getMessage());
            return false;
        }
    }
}
