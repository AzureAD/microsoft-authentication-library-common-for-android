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
package com.microsoft.identity.common.internal.cache;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.List;
import java.util.Set;

public interface IBrokerApplicationMetadataCache {

    /**
     * @return A Set of all ClientIds known to this cache. May be empty, but never null.
     */
    Set<String> getAllClientIds();

    /**
     * For the supplied criteria, return the {@link BrokerApplicationMetadata} which corresponds to it.
     *
     * @param clientId    The target client id.
     * @param environment The target environment.
     * @param processUid  The uid of the app calling broker.
     * @return The matching {@link BrokerApplicationMetadata} or null if a match cannot be found.
     */
    @Nullable
    BrokerApplicationMetadata getMetadata(String clientId, String environment, int processUid);

    /**
     * Inserts a new entry in the cache.
     *
     * @param metadata The metadata to save.
     * @return True, if saved. False otherwise.
     */
    boolean insert(BrokerApplicationMetadata metadata);

    /**
     * Removes an existing entry in the cache.
     *
     * @param metadata The metadata to remove.
     * @return True if removed or does not exist. False otherwise.
     */
    boolean remove(BrokerApplicationMetadata metadata);

    /**
     * Returns all entries in the app metadata cache.
     *
     * @return A List of {@link BrokerApplicationMetadata}.
     */
    @NonNull
    List<BrokerApplicationMetadata> getAll();

    /**
     * Removes all entries in the app metadata cache.
     *
     * @return True if the cache has been successfully cleared. False otherwise.
     */
    boolean clear();
}
