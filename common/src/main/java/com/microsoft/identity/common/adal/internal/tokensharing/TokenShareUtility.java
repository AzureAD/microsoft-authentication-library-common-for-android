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
import com.microsoft.identity.common.exception.ServiceException;
import com.microsoft.identity.common.internal.cache.ICacheRecord;
import com.microsoft.identity.common.internal.cache.MsalOAuth2TokenCache;
import com.microsoft.identity.common.internal.dto.AccountRecord;
import com.microsoft.identity.common.internal.dto.IdTokenRecord;
import com.microsoft.identity.common.internal.dto.RefreshTokenRecord;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftAccount;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftIdToken;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftRefreshToken;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectoryIdToken;
import com.microsoft.identity.common.internal.providers.oauth2.IDToken;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.PlainHeader;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import static com.microsoft.identity.common.exception.ClientException.TOKEN_CACHE_ITEM_NOT_FOUND;
import static com.microsoft.identity.common.internal.migration.TokenCacheItemMigrationAdapter.renewToken;
import static com.microsoft.identity.common.internal.migration.TokenCacheItemMigrationAdapter.sBackgroundExecutor;

public class TokenShareUtility implements ITokenShareInternal {

    private static final String TAG = TokenShareUtility.class.getSimpleName();
    private static final Map<String, String> mClaimRemapper = new HashMap<>();

    /*
     * Populate v1/v2 IdToken claims mapping here.
     */
    static {
        mClaimRemapper.put(
                IDToken.PREFERRED_USERNAME, // v2 value
                AzureActiveDirectoryIdToken.UPN // v1 value
        );

        // TODO something about iss/idp?
        // TODO something about subject & preferred_username?
    }

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

    @Override
    @NonNull
    public String getFamilyRefreshToken(@NonNull final String oid) throws BaseException {
        final String methodName = ":getFamilyRefreshToken";

        // First hit the cache to get the sought AccountRecord...
        final AccountRecord localAccountRecord = mTokenCache.getAccountByLocalAccountId(
                null,
                mClientId,
                oid
        );

        // Check it's not null
        if (null == localAccountRecord) {
            // Unrecognized OID, cannot supply a token.
            throw new ClientException(TOKEN_CACHE_ITEM_NOT_FOUND);
        }

        // Query the cache for the IdTokenRecord, RefreshTokenRecord, etc.
        final ICacheRecord cacheRecord = mTokenCache.load(
                mClientId,
                null, // wildcard (*)
                localAccountRecord
        );

        // Inspect the result for completeness...
        if (null == cacheRecord.getRefreshToken() || null == cacheRecord.getIdToken()) {
            Logger.warn(
                    TAG + methodName,
                    "That's strange, we had an AccountRecord for OID: "
                            + oid
                            + " but couldn't find tokens for them."
            );

            throw new ClientException(TOKEN_CACHE_ITEM_NOT_FOUND);
        }

        final TokenCacheItem cacheItemToExport = adapt(
                cacheRecord.getIdToken(),
                cacheRecord.getRefreshToken()
        );

        // Ship it
        return SSOStateSerializer.serialize(cacheItemToExport);
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
    @NonNull
    private static TokenCacheItem adapt(@NonNull final IdTokenRecord idTokenRecord,
                                        @NonNull final RefreshTokenRecord refreshTokenRecord) throws ServiceException {
        final TokenCacheItem tokenCacheItem = new TokenCacheItem();
        tokenCacheItem.setClientId(refreshTokenRecord.getClientId());
        tokenCacheItem.setAuthority(idTokenRecord.getAuthority());
        tokenCacheItem.setRefreshToken(refreshTokenRecord.getSecret());
        tokenCacheItem.setResource(null); // TODO Does this need to be present? You can't turn scopes into a resource AFAIK...
        tokenCacheItem.setRawIdToken(mintV1IdTokenFromRawV2IdToken(idTokenRecord.getSecret()));
        tokenCacheItem.setFamilyClientId(refreshTokenRecord.getFamilyId());

        return tokenCacheItem;
    }

    @NonNull
    private static String mintV1IdTokenFromRawV2IdToken(@NonNull final String rawV2IdToken) throws ServiceException {
        final Map<String, ?> v2TokenClaims = IDToken.parseJWT(rawV2IdToken);

        // We're going to overwrite some fields to make this v1 compat and then wrap it back up
        final JWTClaimsSet.Builder claimsSetBuilder = new JWTClaimsSet.Builder();

        for (final Map.Entry<String, ?> claimEntry : v2TokenClaims.entrySet()) {
            final String claimKey = claimEntry.getKey();
            Object claimValue = claimEntry.getValue();

            if (MicrosoftIdToken.VERSION.equals(claimKey)) {
                claimValue = "1";
            }

            claimsSetBuilder.claim(
                    remap( // Assign v2 mappings
                            claimKey
                    ),
                    claimValue
            );
        }

        final JWTClaimsSet v1TokenClaims = claimsSetBuilder.build();

        final PlainHeader plainHeader = new PlainHeader(
                JOSEObjectType.JWT,
                null, // unspecified content type
                null, // unspecified critical header
                null,  // no custom params
                null // no baseUrl
        );

        final JWT outboundJwt = new PlainJWT(plainHeader, v1TokenClaims);

        // Serialize and return the result
        return outboundJwt.serialize();
    }

    @NonNull
    private static String remap(@NonNull final String claimKey) {
        String remappedValue = mClaimRemapper.get(claimKey);

        if (null == remappedValue) {
            remappedValue = claimKey;
        }

        return remappedValue;
    }
}
