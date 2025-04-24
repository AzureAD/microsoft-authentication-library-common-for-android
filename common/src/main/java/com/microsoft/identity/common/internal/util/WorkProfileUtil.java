package com.microsoft.identity.common.internal.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Build;

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
        try {
            Intent intent = new Intent("com.google.android.apps.work.clouddpc.ACTION_DETECT_WORK_PROFILE");
            List<ResolveInfo> activities = context.getPackageManager().queryIntentActivities(intent, 0);

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
