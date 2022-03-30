//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.common.internal.migration;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.java.exception.ServiceException;
import com.microsoft.identity.common.adal.internal.cache.ADALTokenCacheItem;
import com.microsoft.identity.common.java.providers.microsoft.MicrosoftAccount;
import com.microsoft.identity.common.java.providers.microsoft.MicrosoftRefreshToken;
import com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.AzureActiveDirectory;
import com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.AzureActiveDirectoryAccount;
import com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.ClientInfo;
import com.microsoft.identity.common.java.providers.oauth2.IDToken;
import com.microsoft.identity.common.logging.Logger;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.microsoft.identity.common.internal.migration.TokenCacheItemMigrationAdapter.migrateTokens;

/**
 * Adapts tokens from the ADAL cache format to the MSAL (common schema) format.
 */
public class AdalMigrationAdapter implements IMigrationAdapter<MicrosoftAccount, MicrosoftRefreshToken> {

    /**
     * Object lock to prevent multiple threads from running migration simultaneously.
     */
    private static final Object sLock = new Object();

    /**
     * The log tag of this class.
     */
    private static final String TAG = AdalMigrationAdapter.class.getSimpleName();

    /**
     * The name of the SharedPreferences file used by this class for tracking migration state.
     */
    private static final String MIGRATION_STATUS_SHARED_PREFERENCES =
            "com.microsoft.identity.client.migration_status";

    /**
     * The migration-state cache-key used to persist/check whether or not migration has occurred or not.
     */
    private static final String KEY_MIGRATION_STATUS = "adal-migration-complete";

    /**
     * The SharedPreferences used to tracking migration state.
     */
    private final SharedPreferences mSharedPrefs;

    /**
     * Force-override to initiate migration, even if it's already happened before.
     */
    private final boolean mForceMigration;

    private final Map<String, String> mRedirectsMap;

    /**
     * Constructs a new AdalMigrationAdapter.
     *
     * @param context Context used to track migration state.
     * @param force   Force migration to occur, even if it has run before.
     */
    public AdalMigrationAdapter(final Context context,
                                final Map<String, String> redirects,
                                final boolean force) {
        mSharedPrefs = context.getSharedPreferences(MIGRATION_STATUS_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        mRedirectsMap = redirects;
        mForceMigration = force;
    }

    @Override
    public List<Map.Entry<MicrosoftAccount, MicrosoftRefreshToken>> adapt(Map<String, String> cacheItems) {
        final List<Map.Entry<MicrosoftAccount, MicrosoftRefreshToken>> result = new ArrayList<>();

        synchronized (sLock) { // To prevent multiple threads from potentially running migration
            final boolean hasMigrated = getMigrationStatus();

            if (!hasMigrated && !mForceMigration) {
                // Initialize the InstanceDiscoveryMetadata so we know about all the clouds and possible /common endpoints
                final boolean cloudMetadataLoaded = loadCloudDiscoveryMetadata();

                if (cloudMetadataLoaded) {
                    // Convert the JSON to native ADALTokenCacheItem representation, original keys used to key the Map
                    Map<String, ADALTokenCacheItem> nativeCacheItems = deserialize(cacheItems);

                    result.addAll(
                            migrateTokens(mRedirectsMap, nativeCacheItems.values())
                    );

                    setMigrationStatus(true);
                }
            }
        }

        return result;
    }

    /**
     * Sets the migration-state in the SharedPreferences file.
     *
     * @param hasMigrated The status to set.
     */
    @SuppressLint("ApplySharedPref")
    public void setMigrationStatus(boolean hasMigrated) {
        mSharedPrefs.edit().putBoolean(KEY_MIGRATION_STATUS, hasMigrated).commit();
    }

    /**
     * Gets the migration-state from the SharedPreferences file.
     *
     * @return True, if migration has already happened. False otherwise.
     */
    public boolean getMigrationStatus() {
        return mSharedPrefs.getBoolean(KEY_MIGRATION_STATUS, false);
    }

    /**
     * Creates a {@link MicrosoftAccount} from the supplied {@link ADALTokenCacheItem}.
     *
     * @param refreshToken The credential used to derive the new account.
     * @return The newly created MicrosoftAccount.
     */
    @Nullable
    public static MicrosoftAccount createAccount(@NonNull final ADALTokenCacheItem refreshToken) {
        final String methodTag = TAG + ":createAccount";
        try {
            final String rawIdToken = refreshToken.getRawIdToken();
            final String uid = refreshToken.getUserInfo().getUserId();
            final String utid = refreshToken.getTenantId();
            final String environment = new URL(refreshToken.getAuthority()).getHost();

            final JsonObject clientInfo = new JsonObject();
            clientInfo.addProperty("uid", uid);
            clientInfo.addProperty("utid", utid);

            final String clientInfoJson = clientInfo.toString();
            final String base64EncodedClientInfo = new String(Base64.encode(clientInfoJson.getBytes(AuthenticationConstants.CHARSET_UTF8), 0),
                    AuthenticationConstants.CHARSET_UTF8);
            final ClientInfo clientInfoObj = new ClientInfo(base64EncodedClientInfo);
            final IDToken idToken = new IDToken(rawIdToken);

            AzureActiveDirectoryAccount account = new AzureActiveDirectoryAccount(idToken, clientInfoObj);
            account.setEnvironment(environment);

            return account;
        } catch (MalformedURLException | ServiceException e) {
            final String errorMsg = "Failed to create Account";
            Logger.error(
                    methodTag,
                    errorMsg,
                    null
            );
            Logger.errorPII(
                    methodTag,
                    errorMsg,
                    e
            );
            return null;
        }
    }

    /**
     * Converts the supplied Map of key/value JSON credentials into a Map of key/POJO.
     *
     * @param tokenCacheItems The credentials to inspect.
     * @return The deserialized credentials and their associated keys.
     */
    @VisibleForTesting
    Map<String, ADALTokenCacheItem> deserialize(final Map<String, String> tokenCacheItems) {
        final String methodTag = TAG + ":deserialize";
        final Map<String, ADALTokenCacheItem> result = new HashMap<>();

        final Gson gson = new Gson();
        for (final Map.Entry<String, String> entry : tokenCacheItems.entrySet()) {
            try {
                result.put(
                        entry.getKey(),
                        gson.fromJson(entry.getValue(), ADALTokenCacheItem.class)
                );
            } catch (final JsonSyntaxException e) {
                Logger.warn(
                        methodTag,
                        "Failed to deserialize ADAL cache entry. Skipping."
                );
            }
        }

        return result;
    }

    /**
     * Loads the InstanceDiscoveryMetadata.
     *
     * @return True, if the metadata loads successfully. False otherwise.
     */
    public static boolean loadCloudDiscoveryMetadata() {
        final String methodTag = TAG + ":loadCloudDiscoveryMetadata";

        if (!AzureActiveDirectory.isInitialized()) {
            try {
                AzureActiveDirectory.performCloudDiscovery();
            } catch (final IOException | URISyntaxException e) {
                Logger.error(
                        methodTag,
                        "Failed to load instance discovery metadata",
                        e
                );
            }
        }

        return AzureActiveDirectory.isInitialized();
    }
}
