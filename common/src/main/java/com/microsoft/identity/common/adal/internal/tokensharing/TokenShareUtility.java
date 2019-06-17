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

import android.support.annotation.NonNull;

import com.microsoft.identity.common.exception.BaseException;
import com.microsoft.identity.common.internal.cache.MsalOAuth2TokenCache;

public class TokenShareUtility implements ITokenShareInternal {

    private MsalOAuth2TokenCache mTokenCache;

    public TokenShareUtility(@NonNull final MsalOAuth2TokenCache cache) {
        mTokenCache = cache;
    }

    @Override
    public String getFamilyRefreshToken(String oid) throws BaseException {
        // TODO hit the cache and try to find any FRT for this OID, if you have it, bag it up and
        // ship it
        return null;
    }

    @Override
    public void saveFamilyRefreshToken(String tokenCacheItemJson) throws BaseException {
        final TokenCacheItem cacheItemToSave = SSOStateSerializer.deserialize(tokenCacheItemJson);

        // The supplied TokenCacheItem will be in the v1 format, convert it to v2 and save it...
    }
}
