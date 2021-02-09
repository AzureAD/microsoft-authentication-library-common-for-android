package com.microsoft.identity.common.internal.throttling;

import android.content.Context;

import androidx.annotation.NonNull;

import com.microsoft.identity.common.internal.cache.ISharedPreferencesFileManager;
import com.microsoft.identity.common.internal.cache.SharedPreferencesFileManager;

import java.util.concurrent.TimeUnit;

public class ThrottlingManager {

    public static final long MAX_RETRY_AFTER = TimeUnit.HOURS.toMillis(1);
    public static final long DEFAULT_THROTTLING = TimeUnit.SECONDS.toMillis(120);

    public static final String THROTTLING_CAPABILITY_HEADER_KEY = "x-ms-lib-capability";
    public static final String THROTTLING_CAPABILITY_HEADER_VALUE = "retry-after, h429";

    /**
     * The name of the SharedPreferences file on disk for the last request telemetry.
     */
    private static final String THROTTLING_CACHE_SHARED_PREFERENCES =
            "com.microsoft.identity.client.throttling_cache";

    private IThrottlingCache mThrottlingCache;

    public ThrottlingManager(@NonNull final Context context) {
        final ISharedPreferencesFileManager sharedPreferencesFileManager =
                new SharedPreferencesFileManager(
                        context,
                        THROTTLING_CACHE_SHARED_PREFERENCES
                );
        mThrottlingCache = new SharedPreferencesThrottlingCache(sharedPreferencesFileManager);
    }

}
