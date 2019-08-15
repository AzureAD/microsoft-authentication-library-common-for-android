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

import androidx.annotation.Nullable;

import java.util.List;
import java.util.Set;

public interface IBrokerApplicationMetadataCache extends ISimpleCache<BrokerApplicationMetadata> {

    /**
     * @return A Set of all ClientIds known to this cache. May be empty, but never null.
     */
    Set<String> getAllClientIds();

    /**
     * @return The Set of all FoCI clientIds.
     */
    Set<String> getAllFociClientIds();

    /**
     * @return The Set of all non-FoCI clientIds.
     */
    Set<String> getAllNonFociClientIds();

    /**
     * @return All of the BrokerApplicationMetadata where the app is FoCI.
     */
    List<BrokerApplicationMetadata> getAllFociApplicationMetadata();

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
}
