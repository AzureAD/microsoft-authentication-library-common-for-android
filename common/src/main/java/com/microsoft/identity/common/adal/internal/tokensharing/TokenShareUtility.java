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
import android.support.annotation.Nullable;
import android.util.Pair;

import com.microsoft.identity.common.exception.BaseException;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.exception.ServiceException;
import com.microsoft.identity.common.internal.cache.ADALTokenCacheItem;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import static com.microsoft.identity.common.exception.ClientException.TOKEN_CACHE_ITEM_NOT_FOUND;
import static com.microsoft.identity.common.internal.migration.TokenCacheItemMigrationAdapter.renewToken;
import static com.microsoft.identity.common.internal.migration.TokenCacheItemMigrationAdapter.sBackgroundExecutor;

public class TokenShareUtility implements ITokenShareInternal {

    private static final String TAG = TokenShareUtility.class.getSimpleName();
    private static final Map<String, String> mClaimRemapper = new HashMap<>();

    static {
        applyV1ToV2Mappings();
    }

    private static void applyV1ToV2Mappings() {
        mClaimRemapper.put(
                IDToken.PREFERRED_USERNAME, // v2 value
                AzureActiveDirectoryIdToken.UPN // v1 value
        );
    }

    private final String mClientId;
    private final String mRedirectUri;
    private final String mDefaultAuthority;
    private final MsalOAuth2TokenCache mTokenCache;

    public TokenShareUtility(@NonNull final String clientId,
                             @NonNull final String redirectUri,
                             @NonNull final String defaultAuthority,
                             @NonNull final MsalOAuth2TokenCache cache) {
        mClientId = clientId;
        mRedirectUri = redirectUri;
        mDefaultAuthority = defaultAuthority;
        mTokenCache = cache;
    }

    @Override
    @NonNull
    @SuppressWarnings("unchecked")
    public String getOrgIdFamilyRefreshToken(@NonNull final String identifier) throws BaseException {
        final ICacheRecord cacheRecord = getCacheRecordForIdentifier(identifier);

        throwIfCacheRecordIncomplete(identifier, cacheRecord);

        final ADALTokenCacheItem cacheItemToExport = adapt(
                cacheRecord.getIdToken(),
                cacheRecord.getRefreshToken()
        );

        // Ship it
        return SSOStateSerializer.serialize(cacheItemToExport);
    }

    private void throwIfCacheRecordIncomplete(@NonNull final String identifier,
                                              @NonNull final ICacheRecord cacheRecord) throws ClientException {
        // Inspect the result for completeness...
        if (null == cacheRecord.getRefreshToken() || null == cacheRecord.getIdToken()) {
            final String methodName = ":throwIfCacheRecordIncomplete";

            Logger.warn(
                    TAG + methodName,
                    "That's strange, we had an AccountRecord for identifier: "
                            + identifier
                            + " but couldn't find tokens for them."
            );

            throw new ClientException(TOKEN_CACHE_ITEM_NOT_FOUND);
        }
    }

    private ICacheRecord getCacheRecordForIdentifier(@NonNull final String identifier) throws ClientException {
        final AccountRecord localAccountRecord = getAccountRecordForIdentifier(identifier);

        // Query the cache for the IdTokenRecord, RefreshTokenRecord, etc.
        return mTokenCache.load(
                mClientId,
                null, // wildcard (*)
                localAccountRecord
        );
    }

    @SuppressWarnings("unchecked")
    private AccountRecord getAccountRecordForIdentifier(@NonNull final String identifier) throws ClientException {
        // First hit the cache to get the sought AccountRecord...
        AccountRecord localAccountRecord = mTokenCache.getAccountByLocalAccountId(
                null,
                mClientId,
                identifier
        );

        if (null == localAccountRecord) {
            // We didn't find an OID match, try using the supplied value as a username...
            final List<AccountRecord> accountRecords = mTokenCache.getAccountsByUsername(
                    null,
                    mClientId,
                    identifier
            );

            // Any arbitrary AccountRecord will do for this user since these are FRTs
            if (!accountRecords.isEmpty()) {
                localAccountRecord = accountRecords.get(0);
            }
        }

        // Check it's not null
        if (null == localAccountRecord) {
            // Unrecognized identifier, cannot supply a token.
            throw new ClientException(TOKEN_CACHE_ITEM_NOT_FOUND);
        }

        return localAccountRecord;
    }

    @Override
    public void saveOrgIdFamilyRefreshToken(@NonNull final String ssoStateSerializerBlob) throws Exception {
        final Future<Pair<MicrosoftAccount, MicrosoftRefreshToken>> resultFuture =
                sBackgroundExecutor.submit(new Callable<Pair<MicrosoftAccount, MicrosoftRefreshToken>>() {
                    @Override
                    public Pair<MicrosoftAccount, MicrosoftRefreshToken> call() throws ClientException {
                        final ADALTokenCacheItem cacheItemToRenew = SSOStateSerializer.deserialize(ssoStateSerializerBlob);

                        // We're going to 'hijack' this token and set our own clientId for renewal
                        // since these are FRTs, this is OK to do.
                        cacheItemToRenew.setClientId(mClientId);

                        // The FRT's shared by TSL likely won't contain a resource. If they do,
                        // assign this value to null - TokenCacheItemMigrationAdapter will add the default
                        // scopes for us so that we may renew this token.
                        // You may be tempted to use graph as the resource, but this doesn't
                        // necessarily work for all sovereign clouds.
                        cacheItemToRenew.setResource(null);

                        return renewToken(mRedirectUri, cacheItemToRenew);
                    }
                });

        final Pair<MicrosoftAccount, MicrosoftRefreshToken> resultPair = resultFuture.get();

        saveResult(resultPair);
    }

    @SuppressWarnings("unchecked")
    private void saveResult(@Nullable final Pair<MicrosoftAccount, MicrosoftRefreshToken> resultPair)
            throws ClientException {
        // If an error is encountered while requesting new tokens, null is returned
        // Check the result, before proceeding to save into the cache...
        if (null != resultPair) {
            mTokenCache.setSingleSignOnState(
                    resultPair.first, // The account
                    resultPair.second // The refresh token
            );
        }
    }

    @Override
    public String getMsaFamilyRefreshToken(@NonNull final String identifier) throws Exception {
        final ICacheRecord cacheRecord = getCacheRecordForIdentifier(identifier);

        throwIfCacheRecordIncomplete(identifier, cacheRecord);

        return cacheRecord.getRefreshToken().getSecret();
    }

    @Override
    public void saveMsaFamilyRefreshToken(@NonNull final String refreshToken) throws Exception {
        final Future<Pair<MicrosoftAccount, MicrosoftRefreshToken>> resultFuture =
                sBackgroundExecutor.submit(new Callable<Pair<MicrosoftAccount, MicrosoftRefreshToken>>() {
                    @Override
                    public Pair<MicrosoftAccount, MicrosoftRefreshToken> call() throws ClientException {
                        final ADALTokenCacheItem cacheItemToRenew = createTokenCacheItem(refreshToken);
                        return renewToken(mRedirectUri, cacheItemToRenew);
                    }
                });

        final Pair<MicrosoftAccount, MicrosoftRefreshToken> resultPair = resultFuture.get();

        saveResult(resultPair);
    }

    private ADALTokenCacheItem createTokenCacheItem(@NonNull final String refreshToken) {
        final ADALTokenCacheItem cacheItem = new ADALTokenCacheItem();

        // Set only the minimally required properties...
        cacheItem.setAuthority(mDefaultAuthority);
        cacheItem.setClientId(mClientId);
        cacheItem.setRefreshToken(refreshToken);

        return cacheItem;
    }

    @SuppressWarnings("PMD.UnusedPrivateMethod")
    @NonNull
    private static ADALTokenCacheItem adapt(@NonNull final IdTokenRecord idTokenRecord,
                                            @NonNull final RefreshTokenRecord refreshTokenRecord) throws ServiceException {
        final ADALTokenCacheItem tokenCacheItem = new ADALTokenCacheItem();
        tokenCacheItem.setClientId(refreshTokenRecord.getClientId());
        tokenCacheItem.setAuthority(idTokenRecord.getAuthority());
        tokenCacheItem.setRefreshToken(refreshTokenRecord.getSecret());
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
