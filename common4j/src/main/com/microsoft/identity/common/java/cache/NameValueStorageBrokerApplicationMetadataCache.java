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
package com.microsoft.identity.common.java.cache;

import com.google.gson.reflect.TypeToken;
import com.microsoft.identity.common.java.constants.SpotbugsWarning;
import com.microsoft.identity.common.java.interfaces.IPlatformComponents;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.util.StringUtil;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.NonNull;

@SuppressFBWarnings(value = SpotbugsWarning.RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE, justification = "Lombok inserts more null checks than we need")
public class NameValueStorageBrokerApplicationMetadataCache
        extends NameValueStorageFileManagerSimpleCacheImpl<BrokerApplicationMetadata>
        implements IBrokerApplicationMetadataCache {

    private static final String TAG = NameValueStorageBrokerApplicationMetadataCache.class.getSimpleName();

    private static final String DEFAULT_APP_METADATA_CACHE_NAME = "com.microsoft.identity.app-meta-cache";

    private static final String KEY_CACHE_LIST = "app-meta-cache";

    public NameValueStorageBrokerApplicationMetadataCache(@NonNull final IPlatformComponents context) {
        super(context, DEFAULT_APP_METADATA_CACHE_NAME, KEY_CACHE_LIST, true);
    }

    @Override
    public Set<String> getAllClientIds() {
        final String methodName = ":getAllClientIds";

        final Set<String> allClientIds = new HashSet<>();

        for (final BrokerApplicationMetadata metadata : getAll()) {
            allClientIds.add(metadata.getClientId());
        }

        Logger.verbose(
                TAG + methodName,
                "Found ["
                        + allClientIds.size()
                        + "] client ids."
        );

        return allClientIds;
    }

    @Override
    public Set<String> getAllFociClientIds() {
        return getAllFociClientIds(false);
    }

    @Override
    public Set<String> getAllNonFociClientIds() {
        return getAllFociClientIds(true);
    }

    @Override
    public List<BrokerApplicationMetadata> getAllFociApplicationMetadata() {
        final Set<String> fociClientIds = getAllFociClientIds();

        final List<BrokerApplicationMetadata> result = new ArrayList<>();

        final List<BrokerApplicationMetadata> allMetadata = getAll();

        for (final BrokerApplicationMetadata metadata : allMetadata) {
            if (fociClientIds.contains(metadata.getClientId())) {
                result.add(metadata);
            }
        }

        return result;
    }

    /**
     * Returns a list of FoCI clientIds or non-FoCI clientIds if inverseMatch is true.
     *
     * @param inverseMatch If false, match FoCI. If true, match non-FoCI.
     * @return
     */
    private Set<String> getAllFociClientIds(final boolean inverseMatch) {
        final String methodName = ":getAllFociClientIds";

        final Set<String> allFociClientIds = new HashSet<>();

        for (final BrokerApplicationMetadata metadata : getAll()) {
            if (!inverseMatch) { // match FoCI
                if (!StringUtil.isNullOrEmpty(metadata.getFoci())) {
                    allFociClientIds.add(metadata.getClientId());
                }
            } else { // match non FoCI
                if (StringUtil.isNullOrEmpty(metadata.getFoci())) {
                    allFociClientIds.add(metadata.getClientId());
                }
            }
        }

        Logger.verbose(
                TAG + methodName,
                "Found ["
                        + allFociClientIds.size()
                        + "] client ids."
        );

        return allFociClientIds;
    }

    @Nullable
    @Override
    public BrokerApplicationMetadata getMetadata(@NonNull final String clientId,
                                                 @NonNull final String environment,
                                                 final int processUid) {
        final String methodName = ":getMetadata";

        final List<BrokerApplicationMetadata> allMetadata = getAll();
        BrokerApplicationMetadata result = null;

        for (final BrokerApplicationMetadata metadata : allMetadata) {
            if (clientId.equals(metadata.getClientId())
                    && environment.equals(metadata.getEnvironment())
                    && processUid == metadata.getUid()) {
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

    @Nullable
    public BrokerApplicationMetadata getMetadataWithUid(
                                                 final int processUid) {
        final String methodName = ":getMetadata";

        final List<BrokerApplicationMetadata> allMetadata = getAll();
        BrokerApplicationMetadata result = null;

        for (final BrokerApplicationMetadata metadata : allMetadata) {
            if (processUid == metadata.getUid()) {
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
                            + "]"
            );
        }

        return result;
    }

    public void remove(@NonNull final String clientId,
                       final int processUid) {
        final List<BrokerApplicationMetadata> allMetadata = getAll();

        for (final BrokerApplicationMetadata metadata : allMetadata) {
            if (clientId.equalsIgnoreCase(metadata.getClientId())
                    && processUid == metadata.getUid()) {
                remove(metadata);
            }
        }
    }

    @Override
    public Type getListTypeToken() {
        return TypeToken.getParameterized(List.class, BrokerApplicationMetadata.class).getType();
    }
}
