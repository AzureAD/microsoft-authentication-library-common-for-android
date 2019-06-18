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
import android.util.Pair;

import com.microsoft.identity.common.exception.BaseException;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.internal.cache.MsalOAuth2TokenCache;
import com.microsoft.identity.common.internal.dto.IdTokenRecord;
import com.microsoft.identity.common.internal.dto.RefreshTokenRecord;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftAccount;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftRefreshToken;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import static com.microsoft.identity.common.internal.migration.TokenCacheItemMigrationAdapter.renewToken;
import static com.microsoft.identity.common.internal.migration.TokenCacheItemMigrationAdapter.sBackgroundExecutor;

public class TokenShareUtility implements ITokenShareInternal {

    private final String mClientId;
    private final String mRedirectUri;
    private final MsalOAuth2TokenCache mTokenCache;

    public TokenShareUtility(@NonNull final String clientId,
                             @NonNull final String redirectUri,
                             @NonNull final MsalOAuth2TokenCache cache) {
        mClientId = clientId;
        mRedirectUri = redirectUri;
        mTokenCache = cache;
    }

    /*
    Design questions:
        1. For back-compat, it would seem that MSAL needs to both receive v1 idtokens AND supply them.
           Is that true? What about when ADAL goes away? Will we add new API surface or version the
           payloads so we stop trying to convert everything between v1/v2 and back again?

           Brian's answer:
               I think we should beef-up this implementation to return v1 and v2 artifacts BUT the
               version check inside of "SSOStateSerializer" just throws exceptions if the version isn't
               "1" so that's ridiculous -- seemingly can't use this. Maybe we keep "version = 1" and
               just stuff more data in there and hope it doesn't break? ;-)
     */

    @Override
    public String getFamilyRefreshToken(@NonNull final String oid) throws BaseException {
        // TODO hit the cache and try to find any FRT for this OID, if you have it, bag it up and
        // ship it
        return null;
    }

    @Override
    public void saveFamilyRefreshToken(@NonNull final String tokenCacheItemJson) throws Exception {
        final Future<Pair<MicrosoftAccount, MicrosoftRefreshToken>> resultFuture =
                sBackgroundExecutor.submit(new Callable<Pair<MicrosoftAccount, MicrosoftRefreshToken>>() {
                    @Override
                    public Pair<MicrosoftAccount, MicrosoftRefreshToken> call() throws ClientException {
                        final TokenCacheItem cacheItemToRenew = SSOStateSerializer.deserialize(tokenCacheItemJson);

                        // We're going to 'hijack' this token and set our own clientId for renewal
                        // since these are FRTs, this is OK to do.
                        cacheItemToRenew.setClientId(mClientId);

                        return renewToken(mRedirectUri, cacheItemToRenew);
                    }
                });

        final Pair<MicrosoftAccount, MicrosoftRefreshToken> resultPair = resultFuture.get();

        // If an error is encountered while requesting new tokens, null is returned
        // Check the result, before proceeding to save into the cache...
        if (null != resultPair) {
            mTokenCache.setSingleSignOnState(
                    resultPair.first, // The account
                    resultPair.second // The refresh token
            );
        }
    }

    @SuppressWarnings("PMD.UnusedPrivateMethod")
    private static TokenCacheItem adapt(@NonNull final IdTokenRecord idTokenRecord,
                                        @NonNull final RefreshTokenRecord refreshTokenRecord) {
        final TokenCacheItem tokenCacheItem = new TokenCacheItem();
        tokenCacheItem.setAuthority(idTokenRecord.getAuthority());
        // TODO set clientId to _this_ runtime
        // TODO set the resource to be the scopes (adapted)
        tokenCacheItem.setRefreshToken(refreshTokenRecord.getSecret());
        tokenCacheItem.setRawIdToken(idTokenRecord.getSecret());
        tokenCacheItem.setFamilyClientId(refreshTokenRecord.getFamilyId());

        return tokenCacheItem;
    }
}
