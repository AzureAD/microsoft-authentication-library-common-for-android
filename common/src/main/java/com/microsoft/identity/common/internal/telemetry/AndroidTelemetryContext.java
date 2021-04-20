package com.microsoft.identity.common.internal.telemetry;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.content.pm.PackageInfoCompat;

import com.microsoft.identity.common.internal.cache.SharedPreferencesFileManager;
import com.microsoft.identity.common.java.interfaces.IKeyPairStorage;
import com.microsoft.identity.common.java.internal.telemetry.AbstractTelemetryContext;
import com.microsoft.identity.common.java.internal.telemetry.TelemetryEventStrings;
import com.microsoft.identity.common.logging.Logger;

import lombok.NonNull;

/**
 * TelemetryContext for Android.
 * Containing Android Metadata. It also persists data in SharedPreferences.
 * */
public class AndroidTelemetryContext extends AbstractTelemetryContext {

    private static final String TAG = AndroidTelemetryContext.class.getName();
    private static final String SHARED_PREFS_NAME = "com.microsoft.common.telemetry-properties";

    public AndroidTelemetryContext(@NonNull final Context context) {
        super(
                new IKeyPairStorage() {
                    final SharedPreferencesFileManager mSharedPrefs =
                            SharedPreferencesFileManager.getSharedPreferences(context, SHARED_PREFS_NAME, -1, null);

                    @Override
                    public String get(@NonNull String key) {
                        return mSharedPrefs.getString(key);
                    }

                    @Override
                    public void put(@lombok.NonNull String key, String value) {
                        mSharedPrefs.putString(key, value);
                    }
                });

        addApplicationInfo(context);
        addDeviceInfo(Build.MANUFACTURER, Build.MODEL, Build.DEVICE);
        addOsInfo();
    }

    private void addApplicationInfo(@NonNull final Context context) {
        try {
            final PackageManager packageManager = context.getPackageManager();
            final PackageInfo packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
            long versionCode = PackageInfoCompat.getLongVersionCode(packageInfo);

            super.addApplicationInfo(packageInfo.applicationInfo.packageName,
                    packageInfo.versionName,
                    String.valueOf(versionCode));
        } catch (final PackageManager.NameNotFoundException e) {
            //Not throw the exception to break the auth request when getting the app's telemetry
            Logger.warn(TAG, "Unable to find the app's package name from PackageManager.");
        }
    }

    private void addOsInfo() {
        super.addOsInfo(TelemetryEventStrings.Os.OS_NAME, Build.VERSION.RELEASE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            put(TelemetryEventStrings.Os.SECURITY_PATCH, Build.VERSION.SECURITY_PATCH);
        }
    }

    public void isNetworkDisabledFromOptimizations(final boolean isDozed) {
        put(TelemetryEventStrings.Key.POWER_OPTIMIZATION, String.valueOf(isDozed));
    }

    public void isNetworkConnected(final boolean isConnected) {
        put(TelemetryEventStrings.Key.NETWORK_CONNECTION, String.valueOf(isConnected));
    }
}
