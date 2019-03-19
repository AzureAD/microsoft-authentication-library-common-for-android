package com.microsoft.identity.common.internal.migration;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;

import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.internal.cache.ADALTokenCacheItem;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftAccount;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftRefreshToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.microsoft.identity.common.internal.migration.AdalMigrationAdapter.loadCloudDiscoveryMetadata;

public class TokenCacheItemMigrationAdapter {

    private static final String TAG = TokenCacheItemMigrationAdapter.class.getSimpleName();
    public static final String COMMON = "/common";

    private final Context mContext;

    public TokenCacheItemMigrationAdapter(@NonNull final Context context) {
        mContext = context;
    }

    /**
     * For a list of supplied tokens, filter them to find the 'most preferred' when migrating.
     * Renew those tokens and provide them as the result in the v2 format.
     *
     * @param redirects  The mapping of clientIds to redirect_uris.
     * @param cacheItems The cache items to migrate.
     * @return The result.
     */
    public List<Pair<MicrosoftAccount, MicrosoftRefreshToken>> migrateTokens(
            @NonNull final Map<String, String> redirects,
            @NonNull final List<ADALTokenCacheItem> cacheItems) {
        final List<Pair<MicrosoftAccount, MicrosoftRefreshToken>> result = new ArrayList<>();

        final boolean cloudMetadataLoaded = loadCloudDiscoveryMetadata();

        if (cloudMetadataLoaded) {
            final List<ADALTokenCacheItem> cacheItemsWithoutDuplicates = filterDuplicateTokens(
                    cacheItems
            );

            final Map<String, List<ADALTokenCacheItem>> tokensByClientId = splitTokensByClientId(
                    cacheItemsWithoutDuplicates
            );

            final Map<String, List<ADALTokenCacheItem>> filteredTokens = preferentiallySelectTokens(
                    tokensByClientId
            );

            // TODO renew these tokens...
        }

        return result;
    }

    @NonNull
    public static List<ADALTokenCacheItem> filterDuplicateTokens(
            @NonNull final List<ADALTokenCacheItem> cacheItems) {
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
        final String methodName = ":splitTokensByClientId";

        Logger.verbose(
                TAG + methodName,
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
        final String methodName = ":preferentiallySelectTokens";
        // Key is client id
        final Map<String, List<ADALTokenCacheItem>> result = new HashMap<>();

        for (final Map.Entry<String, List<ADALTokenCacheItem>> entry : tokensByClientId.entrySet()) {
            final String clientId = entry.getKey();
            final List<ADALTokenCacheItem> tokens = entry.getValue();

            ADALTokenCacheItem refreshToken = findFrt(tokens);

            if (null == refreshToken) {
                Logger.verbose(
                        TAG + methodName,
                        "FRT was null. Try MRRT."
                );

                refreshToken = findMrrt(tokens);
            }

            if (null == refreshToken) {
                Logger.verbose(
                        TAG + methodName,
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
                        TAG + methodName,
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
        final String methodName = ":findRt";
        ADALTokenCacheItem result = null;

        for (final ADALTokenCacheItem cacheItem : cacheItems) {
            if (!StringExtensions.isNullOrBlank(cacheItem.getRefreshToken())) {
                result = cacheItem;

                Logger.verbose(
                        TAG + methodName,
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
        final String methodName = ":findMrrt";
        ADALTokenCacheItem result = null;

        for (final ADALTokenCacheItem cacheItem : cacheItems) {
            if (!StringExtensions.isNullOrBlank(cacheItem.getRefreshToken())
                    && cacheItem.getIsMultiResourceRefreshToken()) {
                result = cacheItem;

                Logger.verbose(
                        TAG + methodName,
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
        final String methodName = ":findFrt";
        ADALTokenCacheItem result = null;

        for (final ADALTokenCacheItem cacheItem : cacheItems) {
            if (!StringExtensions.isNullOrBlank(cacheItem.getRefreshToken())
                    && !StringExtensions.isNullOrBlank(cacheItem.getFamilyClientId())) {
                result = cacheItem;

                Logger.verbose(
                        TAG + methodName,
                        "Frt found."
                );

                break;
            }
        }

        return result;
    }
}
