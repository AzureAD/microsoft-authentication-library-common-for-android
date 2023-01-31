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
import androidx.annotation.Nullable;

import com.microsoft.identity.common.adal.tokensharing.SSOStateSerializer;
import com.microsoft.identity.common.java.WarningType;
import com.microsoft.identity.common.java.exception.BaseException;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.exception.ServiceException;
import com.microsoft.identity.common.java.authscheme.BearerAuthenticationSchemeInternal;
import com.microsoft.identity.common.adal.internal.cache.ADALTokenCacheItem;
import com.microsoft.identity.common.java.cache.ICacheRecord;
import com.microsoft.identity.common.java.cache.MsalOAuth2TokenCache;
import com.microsoft.identity.common.java.dto.AccountRecord;
import com.microsoft.identity.common.java.dto.IdTokenRecord;
import com.microsoft.identity.common.java.dto.RefreshTokenRecord;
import com.microsoft.identity.common.java.providers.microsoft.MicrosoftAccount;
import com.microsoft.identity.common.java.providers.microsoft.MicrosoftIdToken;
import com.microsoft.identity.common.java.providers.microsoft.MicrosoftRefreshToken;
import com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.AzureActiveDirectoryIdToken;
import com.microsoft.identity.common.java.providers.oauth2.IDToken;
import com.microsoft.identity.common.logging.Logger;
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

import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.OAuth2.ID_TOKEN_OBJECT_ID;
import static com.microsoft.identity.common.adal.internal.tokensharing.ITokenShareResultInternal.TokenShareExportFormatInternal.RAW;
import static com.microsoft.identity.common.adal.internal.tokensharing.ITokenShareResultInternal.TokenShareExportFormatInternal.SSO_STATE_SERIALIZER_BLOB;
import static com.microsoft.identity.common.internal.migration.AdalMigrationAdapter.loadCloudDiscoveryMetadata;
import static com.microsoft.identity.common.internal.migration.TokenCacheItemMigrationAdapter.renewToken;
import static com.microsoft.identity.common.internal.migration.TokenCacheItemMigrationAdapter.sBackgroundExecutor;
import static com.microsoft.identity.common.java.exception.ClientException.TOKEN_CACHE_ITEM_NOT_FOUND;

public class TokenShareUtility implements ITokenShareInternal {

    private static final String TAG = TokenShareUtility.class.getSimpleName();
    private static final Map<String, String> sClaimRemapper = new HashMap<>();
    private static final String CONSUMERS_ENDPOINT = "https://login.microsoftonline.com/consumers";

    private enum Environment {
        // Use the preferred_cache name for ADAL backcompat
        WORLDWIDE("https://login.windows.net/common"),
        GALLATIN("https://login.partner.microsoftonline.cn/common"),
        BLACKFOREST("https://login.microsoftonline.de/common"),
        ITAR("https://login.microsoftonline.us/common");

        private String mCommonEndpoint;

        Environment(final String commonEndpoint) {
            mCommonEndpoint = commonEndpoint;
        }

        @NonNull
        static Environment toEnvironment(@NonNull final String envString) throws ClientException {
            final String methodTag = TAG + ":toEnvironment";
            switch (envString) {
                case "login.microsoftonline.com":
                case "login.windows.net":
                case "login.microsoft.com":
                case "sts.windows.net":
                    return Environment.WORLDWIDE;
                case "login.chinacloudapi.cn":
                case "login.partner.microsoftonline.cn":
                    return Environment.GALLATIN;
                case "login.usgovcloudapi.net":
                case "login.microsoftonline.us":
                    return Environment.ITAR;
                case "login.microsoftonline.de":
                    return Environment.BLACKFOREST;
                default:
                    Logger.warn(methodTag, "Unable to map provided env to enum: " + envString);
                    throw new ClientException("Unrecognized environment");
            }
        }

        String getCommonEndpoint() {
            return mCommonEndpoint;
        }
    }

    static {
        applyV1ToV2Mappings();
    }

    private static void applyV1ToV2Mappings() {
        sClaimRemapper.put(
                IDToken.PREFERRED_USERNAME, // v2 value
                AzureActiveDirectoryIdToken.UPN // v1 value
        );
    }

    private final String mClientId;
    private final String mRedirectUri;
    @SuppressWarnings(WarningType.rawtype_warning)
    private final MsalOAuth2TokenCache mTokenCache;

    public TokenShareUtility(@NonNull final String clientId,
                             @NonNull final String redirectUri,
                             @SuppressWarnings(WarningType.rawtype_warning) @NonNull final MsalOAuth2TokenCache cache) {
        mClientId = clientId;
        mRedirectUri = redirectUri;
        mTokenCache = cache;
    }

    @Override
    public ITokenShareResultInternal getOrgIdFamilyRefreshTokenWithMetadata(@NonNull final String identifier) throws BaseException {
        final ICacheRecord cacheRecord = getCacheRecordForIdentifier(identifier);

        throwIfCacheRecordIncomplete(identifier, cacheRecord);

        final ADALTokenCacheItem cacheItemToExport = adapt(
                cacheRecord.getIdToken(),
                cacheRecord.getRefreshToken()
        );

        // Ship it
        return new TokenShareResultInternal(
                cacheRecord,
                SSOStateSerializer.serialize(cacheItemToExport),
                SSO_STATE_SERIALIZER_BLOB
        );
    }

    @Override
    @NonNull
    @SuppressWarnings("unchecked")
    public String getOrgIdFamilyRefreshToken(@NonNull final String identifier) throws BaseException {
        return getOrgIdFamilyRefreshTokenWithMetadata(identifier).getRefreshToken();
    }

    private void throwIfCacheRecordIncomplete(@NonNull final String identifier,
                                              @NonNull final ICacheRecord cacheRecord) throws ClientException {
        // Inspect the result for completeness...
        if (null == cacheRecord.getRefreshToken() || null == cacheRecord.getIdToken()) {
            final String methodTag = TAG + ":throwIfCacheRecordIncomplete";

            Logger.warn(
                    methodTag,
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
                null, // wildcard (*),
                null,
                null,
                localAccountRecord,
                new BearerAuthenticationSchemeInternal() // Auth scheme is inconsequential - only using RT
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
        final String methodTag = TAG + ":saveOrgIdFamilyRefreshToken";

        final Future<Map.Entry<MicrosoftAccount, MicrosoftRefreshToken>> resultFuture =
                sBackgroundExecutor.submit(new Callable<Map.Entry<MicrosoftAccount, MicrosoftRefreshToken>>() {
                    @Override
                    public Map.Entry<MicrosoftAccount, MicrosoftRefreshToken> call() throws ClientException {
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

                        // Check that instance discovery metadata is loaded before making the request...
                        final boolean cloudMetadataLoaded = loadCloudDiscoveryMetadata();

                        if (!cloudMetadataLoaded) {
                            Logger.warn(
                                    methodTag,
                                    "Failed to load cloud metadata, aborting."
                            );

                            return null;
                        }

                        return renewToken(mRedirectUri, cacheItemToRenew);
                    }
                });

        final Map.Entry<MicrosoftAccount, MicrosoftRefreshToken> result = resultFuture.get();

        saveResult(result);
    }

    @Override
    public ITokenShareResultInternal getMsaFamilyRefreshTokenWithMetadata(@NonNull final String identifier) throws Exception {
        final ICacheRecord cacheRecord = getCacheRecordForIdentifier(identifier);

        throwIfCacheRecordIncomplete(identifier, cacheRecord);

        final ITokenShareResultInternal result = new TokenShareResultInternal(
                cacheRecord,
                cacheRecord.getRefreshToken().getSecret(),
                RAW
        );

        return result;
    }

    @SuppressWarnings("unchecked")
    private void saveResult(@Nullable final Map.Entry<MicrosoftAccount, MicrosoftRefreshToken> result)
            throws ClientException {
        // If an error is encountered while requesting new tokens, null is returned
        // Check the result, before proceeding to save into the cache...
        if (null != result) {
            mTokenCache.setSingleSignOnState(
                    result.getKey(), // The account
                    result.getValue() // The refresh token
            );
        }
    }

    @Override
    public String getMsaFamilyRefreshToken(@NonNull final String identifier) throws Exception {
        return getMsaFamilyRefreshTokenWithMetadata(identifier).getRefreshToken();
    }

    @Override
    public void saveMsaFamilyRefreshToken(@NonNull final String refreshToken) throws Exception {
        final String methodTag = TAG + ":saveMsaFamilyRefreshToken";

        final Future<Map.Entry<MicrosoftAccount, MicrosoftRefreshToken>> resultFuture =
                sBackgroundExecutor.submit(new Callable<Map.Entry<MicrosoftAccount, MicrosoftRefreshToken>>() {
                    @Override
                    public Map.Entry<MicrosoftAccount, MicrosoftRefreshToken> call() throws ClientException {
                        final ADALTokenCacheItem cacheItemToRenew = createTokenCacheItem(
                                refreshToken,
                                CONSUMERS_ENDPOINT
                        );

                        // Check that instance discovery metadata is loaded before making the request...
                        final boolean cloudMetadataLoaded = loadCloudDiscoveryMetadata();

                        if (!cloudMetadataLoaded) {
                            Logger.warn(
                                    methodTag,
                                    "Failed to load cloud metadata, aborting."
                            );

                            return null;
                        }

                        return renewToken(mRedirectUri, cacheItemToRenew);
                    }
                });

        final Map.Entry<MicrosoftAccount, MicrosoftRefreshToken> resultKeyValuePair = resultFuture.get();

        saveResult(resultKeyValuePair);
    }

    private ADALTokenCacheItem createTokenCacheItem(@NonNull final String refreshToken,
                                                    @NonNull final String authority) {
        final ADALTokenCacheItem cacheItem = new ADALTokenCacheItem();

        // Set only the minimally required properties...
        cacheItem.setAuthority(authority);
        cacheItem.setClientId(mClientId);
        cacheItem.setRefreshToken(refreshToken);

        return cacheItem;
    }

    @SuppressWarnings("PMD.UnusedPrivateMethod")
    @NonNull
    private static ADALTokenCacheItem adapt(@NonNull final IdTokenRecord idTokenRecord,
                                            @NonNull final RefreshTokenRecord refreshTokenRecord) throws BaseException {
        final ADALTokenCacheItem tokenCacheItem = new ADALTokenCacheItem();
        tokenCacheItem.setClientId(refreshTokenRecord.getClientId());
        tokenCacheItem.setRefreshToken(refreshTokenRecord.getSecret());
        tokenCacheItem.setRawIdToken(mintV1IdTokenFromRawV2IdToken(idTokenRecord.getSecret()));
        tokenCacheItem.setFamilyClientId(refreshTokenRecord.getFamilyId());

        final String authority;

        // In order to support ADAL cache lookups when the cache is empty, always use /common
        // when the outbound token is from the home tenant
        if (isFromHomeTenant(idTokenRecord)) {
            authority = Environment.toEnvironment(refreshTokenRecord.getEnvironment()).getCommonEndpoint();
        } else {
            authority = idTokenRecord.getAuthority();
        }

        tokenCacheItem.setAuthority(authority);

        return tokenCacheItem;
    }

    private static boolean isFromHomeTenant(@NonNull final IdTokenRecord idTokenRecord) {
        final String methodTag = TAG + ":isFromHomeTenant";
        boolean isHomeTenant;

        // If the home account id contains the OID, then this is the user's home tenant...
        final String homeAccountId = idTokenRecord.getHomeAccountId();

        // In order to get the claims, we need to first parse the token....
        try {
            final Map<String, ?> tokenClaims = IDToken.parseJWT(idTokenRecord.getSecret());
            final String oid = (String) tokenClaims.get(ID_TOKEN_OBJECT_ID);

            if (null != oid) {
                isHomeTenant = homeAccountId.contains(oid);
            } else {
                Logger.warn(
                        methodTag,
                        "OID claims was missing from token."
                );

                isHomeTenant = false;
            }
        } catch (final ServiceException e) {
            Logger.warn(
                    methodTag,
                    "Failed to parse IdToken."
            );

            isHomeTenant = false;
        }

        return isHomeTenant;
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
        String remappedValue = sClaimRemapper.get(claimKey);

        if (null == remappedValue) {
            remappedValue = claimKey;
        }

        return remappedValue;
    }
}
