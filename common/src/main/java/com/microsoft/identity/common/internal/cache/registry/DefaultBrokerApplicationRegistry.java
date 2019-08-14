package com.microsoft.identity.common.internal.cache.registry;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.reflect.TypeToken;
import com.microsoft.identity.common.internal.cache.SharedPreferencesSimpleCacheImpl;
import com.microsoft.identity.common.internal.logging.Logger;

import java.lang.reflect.Type;
import java.util.List;

public class DefaultBrokerApplicationRegistry
        extends SharedPreferencesSimpleCacheImpl<BrokerApplicationRegistryData>
        implements IBrokerApplicationRegistry {

    private static final String TAG = DefaultBrokerApplicationRegistry.class.getSimpleName();

    private static final String DEFAULT_APP_REGISTRY_CACHE_NAME = "com.microsoft.identity.app-registry";
    private static final String KEY_APP_REGISTRY = "app-registry";

    public DefaultBrokerApplicationRegistry(@NonNull final Context context) {
        super(context, DEFAULT_APP_REGISTRY_CACHE_NAME, KEY_APP_REGISTRY);
    }

    @Override
    protected Type getListTypeToken() {
        return new TypeToken<List<BrokerApplicationRegistryData>>() {
        }.getType();
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
}
