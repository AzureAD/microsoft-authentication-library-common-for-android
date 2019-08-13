package com.microsoft.identity.common.internal.cache.registry;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.microsoft.identity.common.internal.logging.Logger;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DefaultBrokerApplicationRegistry implements IBrokerApplicationRegistry {

    private static final String TAG = DefaultBrokerApplicationRegistry.class.getSimpleName();

    private static final String DEFAULT_APP_REGISTRY_CACHE_NAME = "com.microsoft.identity.app-registry";
    private static final String KEY_APP_REGISTRY = "app-registry";
    private static final String EMPTY_ARRAY = "[]";

    private final SharedPreferences mSharedPrefs;

    private final Gson mGson = new Gson();

    public DefaultBrokerApplicationRegistry(@NonNull final Context context) {
        mSharedPrefs = context.getSharedPreferences(
                DEFAULT_APP_REGISTRY_CACHE_NAME,
                Context.MODE_PRIVATE
        );
    }

    @Override
    public BrokerApplicationRegistryData getMetadata(@NonNull final String clientId,
                                                     @Nullable final String environment,
                                                     final int processUid) {
        final String methodName = ":getMetadata";

        final List<BrokerApplicationRegistryData> allMetadata = getAll();
        BrokerApplicationRegistryData result = null;

        for (final BrokerApplicationRegistryData metadata : allMetadata) {
            if (clientId.equals(metadata.getClientId())
                    && processUid == metadata.getUid()
                    && (null == environment || environment.equals(metadata.getEnvironment()))) {
                Logger.verbose(
                        TAG + metadata,
                        "Metadata located."
                );

                result = metadata;
                break;
            }
        }

        if (null == result) {
            Logger.warn(
                    TAG + methodName,
                    "Metadata could not be found for clientId, environment: ["
                            + clientId
                            + ", "
                            + environment
                            + "]"
            );
        }

        return result;
    }

    @Override
    public boolean insert(BrokerApplicationRegistryData brokerApplicationRegistryData) {
        final String methodName = ":insert";

        final Set<BrokerApplicationRegistryData> allMetadata = new HashSet<>(getAll());
        Logger.verbose(
                TAG + methodName,
                "Existing metadata contained ["
                        + allMetadata.size()
                        + "] elements."
        );

        allMetadata.add(brokerApplicationRegistryData);

        Logger.verbose(
                TAG + methodName,
                "New metadata set size: ["
                        + allMetadata.size()
                        + "]"
        );

        final String json = mGson.toJson(allMetadata);

        Logger.verbose(
                TAG + methodName,
                "Writing cache entry."
        );

        final boolean success = mSharedPrefs.edit().putString(KEY_APP_REGISTRY, json).commit();

        if (success) {
            Logger.verbose(
                    TAG + methodName,
                    "Cache successfully updated."
            );
        } else {
            Logger.warn(
                    TAG + methodName,
                    "Error writing to cache."
            );
        }

        return success;
    }

    @Override
    public boolean remove(BrokerApplicationRegistryData brokerApplicationRegistryData) {
        final String methodName = ":remove";

        final Set<BrokerApplicationRegistryData> allMetadata = new HashSet<>(getAll());

        Logger.verbose(
                TAG + methodName,
                "Existing metadata contained ["
                        + allMetadata.size()
                        + "] elements."
        );

        final boolean removed = allMetadata.remove(brokerApplicationRegistryData);

        Logger.verbose(
                TAG + methodName,
                "New metadata set size: ["
                        + allMetadata.size()
                        + "]"
        );

        if (!removed) {
            // Nothing to do, wasn't cached in the first place!
            Logger.warn(
                    TAG + methodName,
                    "Nothing to delete -- cache entry is missing!"
            );

            return true;
        } else {
            final String json = mGson.toJson(allMetadata);

            Logger.verbose(
                    TAG + methodName,
                    "Writing new cache values..."
            );

            final boolean written = mSharedPrefs.edit().putString(KEY_APP_REGISTRY, json).commit();

            Logger.verbose(
                    TAG + methodName,
                    "Updated cache contents written? ["
                            + written
                            + "]"
            );

            return written;
        }
    }

    @Override
    public List<BrokerApplicationRegistryData> getAll() {
        final String methodName = ":getAll";
        final String jsonList = mSharedPrefs.getString(KEY_APP_REGISTRY, EMPTY_ARRAY);

        final Type listType = new TypeToken<List<BrokerApplicationRegistryData>>() {
        }.getType();

        final List<BrokerApplicationRegistryData> result = mGson.fromJson(
                jsonList,
                listType
        );

        Logger.verbose(
                TAG + methodName,
                "Found ["
                        + result.size()
                        + "] cache entries."
        );

        return result;
    }

    @Override
    public boolean clear() {
        final String methodName = ":clear";

        final boolean cleared = mSharedPrefs.edit().clear().commit();

        if (!cleared) {
            Logger.warn(
                    TAG + methodName,
                    "Failed to clear cache."
            );
        } else {
            Logger.verbose(
                    TAG + methodName,
                    "Cache successfully cleared."
            );
        }

        return cleared;
    }
}
