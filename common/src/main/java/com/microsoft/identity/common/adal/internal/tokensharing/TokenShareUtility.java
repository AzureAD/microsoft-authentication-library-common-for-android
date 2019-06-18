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
import com.microsoft.identity.common.internal.dto.IdTokenRecord;
import com.microsoft.identity.common.internal.dto.RefreshTokenRecord;

public class TokenShareUtility implements ITokenShareInternal {

    private MsalOAuth2TokenCache mTokenCache;

    public TokenShareUtility(@NonNull final MsalOAuth2TokenCache cache) {
        mTokenCache = cache;
    }

    /*
    Design questions:
        1. When saving these tokens, do I persist them for my own clientId it so that this app can
           use it? Or should I try to parse the clientId out of the input and save it under that?
           Should I add a FoCI lookup that is agnostic of clientIds completely?

           Brian's answer:
               Add parsing to determine the _other_ app's client id. Parse it and save under that.
               If you have an RT for this user already, delete it. Then change the RT lookup to return 
               the RT of another client if the current one is FOCI.

        2. If I already have an IdToken/RT for the current user and TSL tries to save a new one,
           do I delete the one I have so that I maintain the 1 RT per user rule?

           Brian's answer:
               I think 'yes', maintain 1 RT per user. I don't want to potentially delete a good token for a bad one though...
               What about...
               If you already have tokens for this user and they're not expired, do nothing.
               If you already have tokens for this user and they're expired, save the new tokens (see next question).
               If you don't have tokens for this user, save the incoming (see next question).

        3. When saving, what do I do about information I don't have such as home_account_id? (This is part of the
           cache-key).

           Brian's answer:
               ...? (I would say, "make a request" but that's a lot to go wrong, slow, etc.)
               If I already have records in the cache for this OID, I could potentially 'peek' into
               them to try and get home_account_id info but that seems ill-advised.

        4. For back-compat, it would seem that MSAL needs to both receive v1 idtokens AND supply them.
           Is that true? What about when ADAL goes away? Will we add new API surface or version the
           payloads so we stop trying to convert everything between v1/v2 and back again?

           Brian's answer:
               I think we should beef-up this implementation to return v1 and v2 artifacts BUT the
               version check inside of "SSOStateSerializer" just throws exceptions if the version isn't
               "1" so that's ridiculous -- seemingly can't use this. Maybe we keep "version = 1" and
               just stuff more data in there and hope it doesn't break? ;-)
     */

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
        // TODO How does this work for information we don't have? Such as homeTenantId?
    }

    private static TokenCacheItem adapt(@NonNull final IdTokenRecord idTokenRecord,
                                        @NonNull final RefreshTokenRecord refreshTokenRecord) {
        final TokenCacheItem tokenCacheItem = new TokenCacheItem();
        tokenCacheItem.setAuthority(idTokenRecord.getAuthority());
        tokenCacheItem.setRefreshToken(refreshTokenRecord.getSecret());
        tokenCacheItem.setRawIdToken(idTokenRecord.getSecret());
        tokenCacheItem.setFamilyClientId(refreshTokenRecord.getFamilyId());

        return tokenCacheItem;
    }
}
