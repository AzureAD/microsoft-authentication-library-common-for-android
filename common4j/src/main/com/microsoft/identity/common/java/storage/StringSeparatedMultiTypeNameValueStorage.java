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
package com.microsoft.identity.common.java.storage;

import com.microsoft.identity.common.java.cache.IMultiTypeNameValueStorage;
import com.microsoft.identity.common.java.crypto.IKeyAccessor;
import com.microsoft.identity.common.java.interfaces.AbstractPerSeparatorMultiTypeNameValueStorage;
import com.microsoft.identity.common.java.interfaces.IPerSeparatorMultiTypeNameValueStorage;
import com.microsoft.identity.common.java.interfaces.IPlatformComponents;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.NonNull;

/**
 * A platform agnostic implementation of {@link IPerSeparatorMultiTypeNameValueStorage} where the
 * separator is of type {@link String}.
 */
@AllArgsConstructor
public class StringSeparatedMultiTypeNameValueStorage extends AbstractPerSeparatorMultiTypeNameValueStorage<String> {

    @NonNull
    private final IPlatformComponents mPlatformComponents;

    /**
     * Indicates if the contents of this storage should be encrypted or not.
     */
    private final boolean mShouldEncrypt;

    private static final int MAX_ITEM_COUNT = 25;

    /**
     * In-memory cache for storing per-string storage for each String. The idea is to not have
     * create a new instance of {@link IMultiTypeNameValueStorage} for each call to
     * {@link IPerSeparatorMultiTypeNameValueStorage#putString(Object, String, String)}.
     */
    private static final Map<String, IMultiTypeNameValueStorage> sStringSeparatedStorageCache =
            Collections.synchronizedMap(new LinkedHashMap<String, IMultiTypeNameValueStorage>(
                    2, 0.75f, true
            ) {
                @Override
                protected boolean removeEldestEntry(final Map.Entry<String, IMultiTypeNameValueStorage> eldest) {
                    return size() > MAX_ITEM_COUNT;
                }
            });

    @NonNull
    @Override
    protected synchronized IMultiTypeNameValueStorage getStoreForSeparator(@NonNull final String separator) {
        return sStringSeparatedStorageCache.computeIfAbsent(
                separator,
                key -> {
                    if (mShouldEncrypt) {
                        return mPlatformComponents.getStorageSupplier().getEncryptedFileStore(
                                separator
                        );
                    } else {
                        return mPlatformComponents.getStorageSupplier().getUnencryptedFileStore(
                                separator
                        );
                    }
                }
        );
    }
}
