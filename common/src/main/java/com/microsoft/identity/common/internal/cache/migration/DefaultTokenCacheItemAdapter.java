package com.microsoft.identity.common.internal.cache.migration;

import android.support.annotation.NonNull;
import android.util.Base64;
import android.util.Pair;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.exception.ServiceException;
import com.microsoft.identity.common.internal.cache.ADALTokenCacheItem;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftAccount;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftRefreshToken;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectoryAccount;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.ClientInfo;
import com.microsoft.identity.common.internal.providers.oauth2.IDToken;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultTokenCacheItemAdapter implements ITokenCacheItemAdapter {

    @Override
    public List<Pair<MicrosoftAccount, MicrosoftRefreshToken>> adapt(final Map<String, String> tokenCacheItems) {
        // Deserialize the key/value JSON into a List of key/POJO
        final List<Pair<String, ADALTokenCacheItem>> adalTokenCacheItems = deserialize(tokenCacheItems);
        final Map<MicrosoftAccount, List<ADALTokenCacheItem>> cacheItemsByAccount = segmentByAccount(adalTokenCacheItems);
        return adaptCacheItems(cacheItemsByAccount);
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private List<Pair<MicrosoftAccount, MicrosoftRefreshToken>> adaptCacheItems(final Map<MicrosoftAccount, List<ADALTokenCacheItem>> cacheItemsByAccount) {
        final List<Pair<MicrosoftAccount, MicrosoftRefreshToken>> result = new ArrayList<>();
        for (final Map.Entry<MicrosoftAccount, List<ADALTokenCacheItem>> entry : cacheItemsByAccount.entrySet()) {
            final MicrosoftAccount currentAccount = entry.getKey();

            // Fields needed to create a fully-formed RT
            String rawRefreshToken = null;
            ClientInfo clientInfo = null;
            String scope = "openid profile offline_access"; // Use default, since doesn't matter
            String clientId = null;
            boolean isFrt = false;
            String environment = null;
            try {
                clientInfo = new ClientInfo(currentAccount.getClientInfo());
            } catch (ServiceException e) {
                continue;
            }

            final List<ADALTokenCacheItem> cacheItems = entry.getValue();

            for (final ADALTokenCacheItem cacheItem : cacheItems) {
                if (!StringExtensions.isNullOrBlank(cacheItem.getRefreshToken())) {
                    rawRefreshToken = cacheItem.getRefreshToken();
                    isFrt = cacheItem.getIsMultiResourceRefreshToken();
                    clientId = cacheItem.getClientId();
                    try {
                        environment = new URL(cacheItem.getAuthority()).getHost();
                    } catch (MalformedURLException e) {
                        continue;
                    }
                    break;
                }
            }

            final MicrosoftRefreshToken refreshToken = new MicrosoftRefreshToken(
                    rawRefreshToken,
                    clientInfo,
                    scope,
                    clientId,
                    isFrt,
                    environment
            );

            result.add(new Pair<>(currentAccount, refreshToken));
        }

        return result;
    }

    private Map<MicrosoftAccount, List<ADALTokenCacheItem>> segmentByAccount(final List<Pair<String, ADALTokenCacheItem>> adalTokenCacheItems) {
        Map<MicrosoftAccount, List<ADALTokenCacheItem>> result = new HashMap<>();
        // For now, this is only going to work with accounts where /common is the authority...
        // because I need the home account id!
        final List<Pair<String, ADALTokenCacheItem>> commonAuthorityCacheItems = filterByAuthority(
                "https://login.windows.net/common",
                adalTokenCacheItems
        );

        // Filter this list into buckets based on the UserInfo.uniqueId
        final Map<String, List<Pair<String, ADALTokenCacheItem>>> userMappedTokens = filterByUser(commonAuthorityCacheItems);
        result = transform(userMappedTokens);

        return result;
    }

    private Map<MicrosoftAccount, List<ADALTokenCacheItem>> transform(Map<String, List<Pair<String, ADALTokenCacheItem>>> userMappedTokens) {
        Map<MicrosoftAccount, List<ADALTokenCacheItem>> result = new HashMap<>();

        for (Map.Entry<String, List<Pair<String, ADALTokenCacheItem>>> entry : userMappedTokens.entrySet()) {
            final List<ADALTokenCacheItem> cacheItems = new ArrayList<>(); // the cache items for the current user
            for (final Pair<String, ADALTokenCacheItem> pair : entry.getValue()) {
                cacheItems.add(pair.second);
            }
            result.put(createAccountFromCacheItems(cacheItems), cacheItems);
        }

        return result;
    }

    private MicrosoftAccount createAccountFromCacheItems(final List<ADALTokenCacheItem> cacheItems) {
        String rawIdToken = null;
        String uid = null;
        String utid = null;
        String environment = null;

        for (final ADALTokenCacheItem cacheItem : cacheItems) {
            if (!StringExtensions.isNullOrBlank(cacheItem.getRawIdToken())) {
                rawIdToken = cacheItem.getRawIdToken();
                break;
            }
        }

        for (final ADALTokenCacheItem cacheItem : cacheItems) {
            if (!StringExtensions.isNullOrBlank(cacheItem.getUserInfo().getUserId())) {
                uid = cacheItem.getUserInfo().getUserId();
                break;
            }
        }

        for (final ADALTokenCacheItem cacheItem : cacheItems) {
            if (!StringExtensions.isNullOrBlank(cacheItem.getTenantId())) {
                utid = cacheItem.getTenantId();
                break;
            }
        }

        for (final ADALTokenCacheItem cacheItem : cacheItems) {
            if (!StringExtensions.isNullOrBlank(cacheItem.getAuthority())) {
                try {
                    environment = new URL(cacheItem.getAuthority()).getHost();
                } catch (MalformedURLException e) {
                    break;
                }
            }
        }

        if (StringExtensions.isNullOrBlank(rawIdToken)
                || StringExtensions.isNullOrBlank(uid)
                || StringExtensions.isNullOrBlank(utid)) {
            throw new RuntimeException("Can't make this account...");
        }

        JsonObject clientInfo = new JsonObject();
        clientInfo.addProperty("uid", uid);
        clientInfo.addProperty("utid", utid);
        final String clientInfoJson = clientInfo.toString();
        final String base64EncodedClientInfo = new String(Base64.encode(clientInfoJson.getBytes(), 0));
        try {
            final ClientInfo clientInfoObj = new ClientInfo(base64EncodedClientInfo);
            final IDToken idToken = new IDToken(rawIdToken);
            AzureActiveDirectoryAccount account = new AzureActiveDirectoryAccount(idToken, clientInfoObj);
            account.setEnvironment(environment);
            return account;
        } catch (ServiceException e) {
            e.printStackTrace();
        }

        throw new RuntimeException("Failed to construct account!");
    }

    private static Map<String, List<Pair<String, ADALTokenCacheItem>>> filterByUser(List<Pair<String, ADALTokenCacheItem>> cacheItems) {
        final Map<String, List<Pair<String, ADALTokenCacheItem>>> result = new HashMap<>();

        for (final Pair<String, ADALTokenCacheItem> tokenCacheItemPair : cacheItems) {
            final String userId = tokenCacheItemPair.second.getUserInfo().getUserId();
            if (null == result.get(userId)) {
                result.put(userId, new ArrayList<Pair<String, ADALTokenCacheItem>>() {{
                    add(tokenCacheItemPair);
                }});
            } else {
                result.get(userId).add(tokenCacheItemPair);
            }
        }

        return result;
    }

    private List<Pair<String, ADALTokenCacheItem>> filterByAuthority(@NonNull final String authority,
                                                                     final List<Pair<String, ADALTokenCacheItem>> adalTokenCacheItems) {
        final List<Pair<String, ADALTokenCacheItem>> filteredList = new ArrayList<>();

        for (final Pair<String, ADALTokenCacheItem> tokenCacheItemPair : adalTokenCacheItems) {
            if (authority.equalsIgnoreCase(tokenCacheItemPair.second.getAuthority())) {
                filteredList.add(tokenCacheItemPair);
            }
        }

        return filteredList;
    }

    /**
     * Deserializes the provided Map of key/json into a List of Pair where Pair.first == cache key
     * and Pair.second is the associated ADALTokenCacheItem in serialized (native POJO) form.
     *
     * @param tokenCacheItems The key/value pairs, as returned from SharedPreferences.
     * @return The keys/native objects as a List.
     */
    private List<Pair<String, ADALTokenCacheItem>> deserialize(final Map<String, String> tokenCacheItems) {
        final List<Pair<String, ADALTokenCacheItem>> result = new ArrayList<>();

        final Gson gson = new Gson();
        for (final Map.Entry<String, String> entry : tokenCacheItems.entrySet()) {
            result.add(
                    new Pair<>(
                            entry.getKey(),
                            gson.fromJson(entry.getValue(), ADALTokenCacheItem.class)
                    )
            );
        }

        return result;
    }

}
