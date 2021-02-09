package com.microsoft.identity.common.internal.throttling;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.microsoft.identity.common.internal.cache.ISharedPreferencesFileManager;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.providers.oauth2.TokenErrorResponse;

public class SharedPreferencesThrottlingCache implements IThrottlingCache {

    private final static String TAG = SharedPreferencesThrottlingCache.class.getSimpleName();
    private final static String THROTTLING_INFO_CACHE_KEY_PREFIX = "_throttling-info";
    private final static String ERROR_RESPONSE_CACHE_KEY_PREFIX = "_error-response";

    private final Gson mGson;

    // SharedPreferences used to store request telemetry data
    private final ISharedPreferencesFileManager mSharedPreferencesFileManager;

    public SharedPreferencesThrottlingCache(@NonNull final ISharedPreferencesFileManager mSharedPreferencesFileManager) {
        this.mGson = new Gson();
        this.mSharedPreferencesFileManager = mSharedPreferencesFileManager;
    }

    @Override
    public void saveThrottlingInfoForRequest(@NonNull final Request request, @NonNull final ThrottlingInfo throttlingInfo) {
        final String cacheKey = serializeToJson(request);
        final String cacheValue = serializeToJson(throttlingInfo);
        mSharedPreferencesFileManager.putString(cacheKey, cacheValue);
    }

    @Override
    public void saveErrorResponseForRequest(Request request, TokenErrorResponse tokenErrorResponse) {

    }

    @Override
    @Nullable
    public ThrottlingInfo loadThrottlingForRequest(@NonNull final Request request) {
        final String methodName = ":loadThrottlingInfoFromCache";
        final String cacheKey = serializeToJson(request);

        try {
            final String cacheValue = mSharedPreferencesFileManager.getString(cacheKey);

            if (cacheValue == null) {
                Logger.info(TAG + methodName, "There is no last request telemetry saved in " +
                        "the cache. Returning NULL");

                return null;
            }

            final ThrottlingInfo throttlingInfo = mGson.fromJson(cacheValue, ThrottlingInfo.class);

            if (throttlingInfo == null) {
                Logger.warn(TAG + methodName, "Throttling info deserialization failed.");
            }

            return throttlingInfo;
        } catch (final JsonSyntaxException e) {
            Logger.error(TAG + methodName, "Throttling info deserialization failed.", e);
            return null;
        }
    }

    @Nullable
    @Override
    public TokenErrorResponse loadErrorResponseForRequest(Request request) {
        return null;
    }

    private String serializeToJson(final Object o) {
        JsonElement outboundElement = mGson.toJsonTree(o);
        JsonObject outboundObject = outboundElement.getAsJsonObject();

        return mGson.toJson(outboundObject);
    }
}
