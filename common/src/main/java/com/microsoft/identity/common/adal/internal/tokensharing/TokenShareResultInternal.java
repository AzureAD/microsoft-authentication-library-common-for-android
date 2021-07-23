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
package com.microsoft.identity.common.adal.internal.tokensharing;

import androidx.annotation.NonNull;

import com.microsoft.identity.common.java.cache.ICacheRecord;

public class TokenShareResultInternal implements ITokenShareResultInternal {

    private final ICacheRecord mCacheRecord;
    private final String mRefreshToken;
    private final String mFormat;

    /**
     * Constructs a new {@link TokenShareResultInternal}.
     *
     * @param cacheRecord  The {@link ICacheRecord} used to build this result.
     * @param refreshToken The rt string, in the designated format.
     * @param format       The format of the rt string.
     */
    protected TokenShareResultInternal(@NonNull final ICacheRecord cacheRecord,
                                       @NonNull final String refreshToken,
                                       @NonNull final String format) {
        mCacheRecord = cacheRecord;
        mRefreshToken = refreshToken;
        mFormat = format;
    }

    @Override
    public ICacheRecord getCacheRecord() {
        return mCacheRecord;
    }

    @Override
    public String getFormat() {
        return mFormat;
    }

    @Override
    public String getRefreshToken() {
        return mRefreshToken;
    }
}
