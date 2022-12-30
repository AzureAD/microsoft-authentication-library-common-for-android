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
package com.microsoft.identity.common.internal.migration;


import com.microsoft.identity.common.adal.internal.cache.ADALTokenCacheItem;
import com.microsoft.identity.common.java.foci.FociQueryUtilities;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsTokenRequest;
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsTokenResponse;
import com.microsoft.identity.common.java.providers.oauth2.TokenErrorResponse;

import com.microsoft.identity.common.java.WarningType;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.cache.BrokerOAuth2TokenCache;
import com.microsoft.identity.common.java.cache.ICacheRecord;
import com.microsoft.identity.common.java.cache.ITokenCacheItem;
import com.microsoft.identity.common.java.controllers.BaseController;
import com.microsoft.identity.common.java.providers.microsoft.MicrosoftAccount;
import com.microsoft.identity.common.java.providers.microsoft.MicrosoftRefreshToken;
import com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.AzureActiveDirectory;
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Configuration;
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Strategy;
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsRefreshToken;
import com.microsoft.identity.common.java.providers.oauth2.OAuth2StrategyParameters;
import com.microsoft.identity.common.java.providers.oauth2.TokenResult;
import com.microsoft.identity.common.java.util.StringUtil;

import java.io.IOException;
import java.net.URL;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.microsoft.identity.common.internal.migration.AdalMigrationAdapter.loadCloudDiscoveryMetadata;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.NonNull;

public class TokenCacheItemMigrationAdapter {

    private static final String TAG = TokenCacheItemMigrationAdapter.class.getSimpleName();

    private static final String COMMON = "/common";

    /**
     * ExecutorService to handle background computation.
     */
    public static final ExecutorService sBackgroundExecutor = Executors.newCachedThreadPool();

    /**
     * For a list of supplied tokens, filter them to find the 'most preferred' when migrating.
     * Renew those tokens and provide them as the result in the v2 format.
     *
     * @param redirects  The mapping of clientIds to redirect_uris.
     * @param cacheItems The cache items to migrate.
     * @return The result.
     */
    public static List<Map.Entry<MicrosoftAccount, MicrosoftRefreshToken>> migrateTokens(
            @NonNull final Map<String, String> redirects,
            @NonNull final Collection<ADALTokenCacheItem> cacheItems) {
        final List<Map.Entry<MicrosoftAccount, MicrosoftRefreshToken>> result = new ArrayList<>();

        final boolean cloudMetadataLoaded = loadCloudDiscoveryMetadata();

        if (cloudMetadataLoaded) {
            final List<ADALTokenCacheItem> cacheItemsWithoutDuplicates = filterDuplicateTokens(
                    cacheItems
            );

            // Key is the clientId
            final Map<String, List<ADALTokenCacheItem>> tokensByClientId = splitTokensByClientId(
                    cacheItemsWithoutDuplicates
            );

            final Map<String, List<ADALTokenCacheItem>> filteredTokens = preferentiallySelectTokens(
                    tokensByClientId
            );

            // Flatten the Lists of tokens...
            final List<ADALTokenCacheItem> cacheItemsToRenew = new ArrayList<>();

            for (final List<ADALTokenCacheItem> cacheItemList : filteredTokens.values()) {
                cacheItemsToRenew.addAll(cacheItemList);
            }

            result.addAll(renewTokens(redirects, cacheItemsToRenew));
        }

        return result;
    }


    private static List<Map.Entry<MicrosoftAccount, MicrosoftRefreshToken>> renewTokens(
            @NonNull final Map<String, String> redirects,
            @NonNull final List<ADALTokenCacheItem> filteredTokens) {
        final List<Map.Entry<MicrosoftAccount, MicrosoftRefreshToken>> result = new ArrayList<>();
        final int tokenCount = filteredTokens.size();

        // Create a CountDownLatch to parallelize these requests
        final CountDownLatch latch = new CountDownLatch(tokenCount);

        for (int ii = 0; ii < tokenCount; ii++) {
            final int subIndex = ii;
            sBackgroundExecutor.submit(new Runnable() {
                @Override
                public void run() {
                    final ADALTokenCacheItem targetCacheItemToRenew = filteredTokens.get(subIndex);

                    final Map.Entry<MicrosoftAccount, MicrosoftRefreshToken> renewedKeyValuePair = renewToken(
                            redirects.get(targetCacheItemToRenew.getClientId()),
                            targetCacheItemToRenew
                    );

                    if (null != renewedKeyValuePair) {
                        result.add(
                                renewedKeyValuePair
                        );
                    }

                    latch.countDown();
                }
            });
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            // Shouldn't happen
            Logger.error(
                    TAG,
                    "Interrupted while requesting tokens...",
                    e
            );
            Thread.currentThread().interrupt();
        }

        return result;
    }

    @Nullable
    public static Map.Entry<MicrosoftAccount, MicrosoftRefreshToken> renewToken(
            @Nullable final String redirectUri,
            @NonNull final ITokenCacheItem targetCacheItemToRenew) {
        Map.Entry<MicrosoftAccount, MicrosoftRefreshToken> resultKeyValuePair = null;

        if (!StringUtil.isNullOrEmpty(redirectUri)) {
            try {
                final String authority = targetCacheItemToRenew.getAuthority();
                final String clientId = targetCacheItemToRenew.getClientId();
                final String refreshToken = targetCacheItemToRenew.getRefreshToken();

                final MicrosoftStsOAuth2Configuration config = new MicrosoftStsOAuth2Configuration();
                config.setAuthorityUrl(new URL(authority));

                // Create a correlation_id for the request
                final UUID correlationId = UUID.randomUUID();

                final String scopes;

                if (StringUtil.isNullOrEmpty(targetCacheItemToRenew.getResource())) {
                    scopes = BaseController.getDelimitedDefaultScopeString();
                } else {
                    scopes = getScopesForTokenRequest(
                            targetCacheItemToRenew.getResource()
                    );
                }

                // Create the strategy
                final OAuth2StrategyParameters strategyParameters = OAuth2StrategyParameters.builder().build();
                final MicrosoftStsOAuth2Strategy strategy = new MicrosoftStsOAuth2Strategy(config, strategyParameters);

                final MicrosoftStsTokenRequest tokenRequest = FociQueryUtilities.createTokenRequest(
                        clientId,
                        scopes,
                        refreshToken,
                        redirectUri,
                        strategy,
                        correlationId,
                        "2"
                );

                final TokenResult tokenResult = strategy.requestToken(tokenRequest);

                if (tokenResult.getSuccess()) {
                    final MicrosoftStsTokenResponse tokenResponse = (MicrosoftStsTokenResponse) tokenResult.getTokenResponse();
                    tokenResponse.setClientId(clientId);

                    // Create the Account to save...
                    final MicrosoftAccount account = strategy.createAccount(tokenResponse);

                    // Create the refresh token...
                    final MicrosoftRefreshToken msStsRt = new MicrosoftStsRefreshToken(tokenResponse);
                    msStsRt.setEnvironment(
                            AzureActiveDirectory.getAzureActiveDirectoryCloud(
                                    new URL(authority)
                            ).getPreferredCacheHostName()
                    );

                    resultKeyValuePair = new AbstractMap.SimpleEntry<>(account, msStsRt);
                } else {
                    Logger.warn(
                            TAG,
                            correlationId.toString(),
                            "TokenRequest was unsuccessful."
                    );

                    if (null != tokenResult.getErrorResponse()) {
                        logTokenResultError(correlationId, tokenResult);
                    }
                }
            } catch (Exception e) {
                Logger.errorPII(
                        TAG,
                        "Failed to request new refresh token...",
                        e
                );
            }
        }

        return resultKeyValuePair;
    }

    @NonNull
    public static List<ADALTokenCacheItem> filterDuplicateTokens(
            @NonNull final Collection<ADALTokenCacheItem> cacheItems) {
        final List<ADALTokenCacheItem> cacheItemsFiltered = new ArrayList<>();

        // Key is the rt secret value
        final Map<String, ADALTokenCacheItem> cacheItemMap = new HashMap<>();

        for (final ADALTokenCacheItem cacheItem : cacheItems) {
            if (null == cacheItem.getResource()) {
                Logger.warn(
                        TAG,
                        "Skipping resourceless token."
                );

                continue;
            }

            if (null == cacheItemMap.get(cacheItem.getRefreshToken())) {
                cacheItemMap.put(cacheItem.getRefreshToken(), cacheItem);
            }

            if (null != cacheItemMap.get(cacheItem.getRefreshToken())
                    && cacheItem.getAuthority().contains(COMMON)) {
                // Prefer the home-tenant token over the tenanted...
                cacheItemMap.put(cacheItem.getRefreshToken(), cacheItem);
            }
        }

        cacheItemsFiltered.addAll(cacheItemMap.values());

        return cacheItemsFiltered;
    }

    /**
     * For the supplied List of {@link ADALTokenCacheItem}, sort it into a Map keyed on the
     * clientId of the contained tokens.
     *
     * @param cacheItemsIn The cache items to inspect.
     * @return The input cache items, sorted into 'buckets' based on clientId.
     */
    @NonNull
    public static Map<String, List<ADALTokenCacheItem>> splitTokensByClientId(
            @NonNull final List<ADALTokenCacheItem> cacheItemsIn) {
        final String methodTag = TAG + ":splitTokensByClientId";

        Logger.verbose(
                methodTag,
                "Splitting ["
                        + cacheItemsIn.size()
                        + "] cache items."
        );

        final Map<String, List<ADALTokenCacheItem>> cacheItemsOut = new HashMap<>();

        for (final ADALTokenCacheItem cacheItem : cacheItemsIn) {
            if (null == cacheItemsOut.get(cacheItem.getClientId())) {
                cacheItemsOut.put(
                        cacheItem.getClientId(),
                        new ArrayList<ADALTokenCacheItem>()
                );
            }

            cacheItemsOut.get(cacheItem.getClientId()).add(cacheItem);
        }

        return cacheItemsOut;
    }

    /**
     * Given the supplied Map of tokens (keyed by clientId), iterate over the list of tokens to
     * select the 'most preferred' one to migrate. Preference order is:
     * 1. FRT
     * 2. MRRT
     * 3. Regular RT
     *
     * @param tokensByClientId The candidate tokens to inspect.
     * @return The reduced Map of input tokens, contain only the most preferred tokens to migrate.
     */
    public static Map<String, List<ADALTokenCacheItem>> preferentiallySelectTokens(
            @NonNull final Map<String, List<ADALTokenCacheItem>> tokensByClientId) {
        final String methodTag = TAG + ":preferentiallySelectTokens";
        // Key is client id
        final Map<String, List<ADALTokenCacheItem>> result = new HashMap<>();

        for (final Map.Entry<String, List<ADALTokenCacheItem>> entry : tokensByClientId.entrySet()) {
            final String clientId = entry.getKey();
            final List<ADALTokenCacheItem> tokens = entry.getValue();

            ADALTokenCacheItem refreshToken = findFrt(tokens);

            if (null == refreshToken) {
                Logger.verbose(
                        methodTag,
                        "FRT was null. Try MRRT."
                );

                refreshToken = findMrrt(tokens);
            }

            if (null == refreshToken) {
                Logger.verbose(
                        methodTag,
                        "MRRT was null. Try RT."
                );

                refreshToken = findRt(tokens);
            }

            if (null != refreshToken) {
                // We've selected the 'best' token. Stick it in the result...
                if (null == result.get(clientId)) {
                    result.put(clientId, new ArrayList<ADALTokenCacheItem>());
                }

                result.get(clientId).add(refreshToken);
            } else {
                Logger.warn(
                        methodTag,
                        "Refresh token could not be located."
                );
            }
        }

        return result;
    }

    /**
     * For the supplied List of ADALTokenCacheItems, return the first item which is an RT or
     * null, if none exists.
     *
     * @param cacheItems The List of ADALTokenCacheItems to inspect.
     * @return The first occurring RT or null, if none can be found.
     */
    @Nullable
    public static ADALTokenCacheItem findRt(
            @NonNull final List<ADALTokenCacheItem> cacheItems) {
        final String methodTag = TAG + ":findRt";
        ADALTokenCacheItem result = null;

        for (final ADALTokenCacheItem cacheItem : cacheItems) {
            if (!StringUtil.isNullOrEmpty(cacheItem.getRefreshToken())) {
                result = cacheItem;

                Logger.verbose(
                        methodTag,
                        "RT found."
                );

                break;
            }
        }

        return result;
    }

    /**
     * For the supplied List of ADALTokenCacheItems, return the first item which is an MRRT or
     * null, if none exists.
     *
     * @param cacheItems The List of ADALTokenCacheItems to inspect.
     * @return The first occurring MRRT or null, if none can be found.
     */
    @Nullable
    public static ADALTokenCacheItem findMrrt(
            @NonNull final List<ADALTokenCacheItem> cacheItems) {
        final String methodTag = TAG + ":findMrrt";
        ADALTokenCacheItem result = null;

        for (final ADALTokenCacheItem cacheItem : cacheItems) {
            if (!StringUtil.isNullOrEmpty(cacheItem.getRefreshToken())
                    && cacheItem.getIsMultiResourceRefreshToken()) {
                result = cacheItem;

                Logger.verbose(
                        methodTag,
                        "Mrrt found."
                );

                break;
            }
        }

        return result;
    }

    /**
     * For the supplied List of ADALTokenCacheItems, return the first item which is an FRT or
     * null, if none exists.
     *
     * @param cacheItems The List of ADALTokenCacheItems to inspect.
     * @return The first occurring FRT or null, if none can be found.
     */
    @Nullable
    public static ADALTokenCacheItem findFrt(
            @NonNull final List<ADALTokenCacheItem> cacheItems) {
        final String methodTag = TAG + ":findFrt";
        ADALTokenCacheItem result = null;

        for (final ADALTokenCacheItem cacheItem : cacheItems) {
            if (!StringUtil.isNullOrEmpty(cacheItem.getRefreshToken())
                    && !StringUtil.isNullOrEmpty(cacheItem.getFamilyClientId())) {
                result = cacheItem;

                Logger.verbose(
                        methodTag,
                        "Frt found."
                );

                break;
            }
        }

        return result;
    }

    /**
     * Prepares the scopes the use in the request. The default scopes for the resource are used.
     *
     * @param v1Resource The resource for which scopes should be added.
     * @return The scopes to include in the token request.
     * @see <a href="https://docs.microsoft.com/en-us/azure/active-directory/develop/v2-permissions-and-consent#the-default-scope">The /.default scope</a>
     */
    @NonNull
    public static String getScopesForTokenRequest(@NonNull final String v1Resource) {
        String scopes = MicrosoftStsOAuth2Strategy.getScopeFromResource(v1Resource);

        // Add the default scopes, as they will not be present
        scopes += " " + BaseController.getDelimitedDefaultScopeString();

        return scopes;
    }

    /**
     * Logs errors from the {@link TokenResult}.
     *
     * @param correlationId The correlation id of the request.
     * @param tokenResult   The TokenResult whose errors should be logged.
     */
    public static void logTokenResultError(@NonNull final UUID correlationId,
                                           @NonNull final TokenResult tokenResult) {
        final TokenErrorResponse tokenErrorResponse = tokenResult.getErrorResponse();

        Logger.warn(
                TAG,
                correlationId.toString(),
                "Status code: ["
                        + tokenErrorResponse.getStatusCode()
                        + "]"
        );

        Logger.warn(
                TAG,
                correlationId.toString(),
                "Error description: ["
                        + tokenErrorResponse.getErrorDescription()
                        + "]"
        );
    }
}
