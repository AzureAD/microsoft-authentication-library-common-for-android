package com.microsoft.identity.common.internal.throttling;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.microsoft.identity.common.internal.cache.ISharedPreferencesFileManager;
import com.microsoft.identity.common.internal.cache.SharedPreferencesFileManager;
import com.microsoft.identity.common.internal.providers.oauth2.TokenErrorResponse;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResult;
import com.microsoft.identity.common.internal.util.HeaderSerializationUtil;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.HeaderField.RETRY_AFTER;

public class ThrottlingManager {

    public static final long MAX_RETRY_AFTER = TimeUnit.HOURS.toMillis(1);
    public static final long DEFAULT_THROTTLING = TimeUnit.SECONDS.toMillis(120);

    public static final String THROTTLING_CAPABILITY_HEADER_KEY = "x-ms-lib-capability";
    public static final String THROTTLING_CAPABILITY_HEADER_VALUE = "retry-after, h429";

    /**
     * The name of the SharedPreferences file on disk for the throttling cache.
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

    private void tryThrottle(final Request request) {
        final ThrottlingInfo throttlingInfo = mThrottlingCache.loadThrottlingInfoForRequest(request);
        //TODO: Check if we need to throttle here and act accordingly
    }

    public void saveThrottlingInfo(final Request request, final TokenResult result) {
        final TokenErrorResponse tokenErrorResponse = result.getErrorResponse();
        final String headerJson = tokenErrorResponse.getResponseHeadersJson();
        final HashMap<String, List<String>> headerMap = HeaderSerializationUtil.fromJson(headerJson);
        final List<String> retryAfterValues = headerMap.get(RETRY_AFTER);
        String retryAfterValue = null;
        if (retryAfterValues != null && !retryAfterValues.isEmpty()) {
            retryAfterValue = retryAfterValues.get(0);
        }

        final int httpStatusCode = tokenErrorResponse.getStatusCode();

        if (httpStatusCode == 429 || httpStatusCode >= 500 || !TextUtils.isEmpty(retryAfterValue)) {
            final long retryValue = !TextUtils.isEmpty(retryAfterValue)
                    ? Integer.parseInt(retryAfterValue)
                    : ThrottlingManager.DEFAULT_THROTTLING;

            final ThrottlingInfo throttlingInfo = new ThrottlingInfo(
                    httpStatusCode, retryValue, tokenErrorResponse
            );

            //TODO: Save the throttling info object to cache to be used next time
        }
    }

}
