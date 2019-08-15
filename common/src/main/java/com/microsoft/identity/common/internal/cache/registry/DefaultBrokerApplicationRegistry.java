//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.common.internal.cache.registry;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.reflect.TypeToken;
import com.microsoft.identity.common.internal.cache.SharedPreferencesSimpleCacheImpl;
import com.microsoft.identity.common.internal.logging.Logger;

import java.lang.reflect.Type;
import java.util.List;

/**
 * A basic registry (key/value) style data store for tracking info about apps which bind to the
 * broker.
 */
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
