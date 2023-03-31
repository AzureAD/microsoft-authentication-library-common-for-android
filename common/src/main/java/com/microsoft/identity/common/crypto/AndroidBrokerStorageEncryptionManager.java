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
package com.microsoft.identity.common.crypto;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.annotation.Nullable;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.adal.internal.AuthenticationSettings;
import com.microsoft.identity.common.adal.internal.cache.StorageHelper;
import com.microsoft.identity.common.java.crypto.StorageEncryptionManager;
import com.microsoft.identity.common.java.crypto.key.AbstractSecretKeyLoader;
import com.microsoft.identity.common.java.crypto.key.PredefinedKeyLoader;
import com.microsoft.identity.common.java.telemetry.ITelemetryCallback;
import com.microsoft.identity.common.logging.Logger;

import java.util.ArrayList;
import java.util.List;

import lombok.NonNull;

import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_PACKAGE_NAME;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.BROKER_HOST_APP_PACKAGE_NAME;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME;

/**
 * Key Encryption Manager for Broker.
 */
public class AndroidBrokerStorageEncryptionManager extends StorageEncryptionManager {
    private static final String TAG = AndroidBrokerStorageEncryptionManager.class.getSimpleName();

    /**
     * Alias persisting the keypair in AndroidKeyStore.
     */
    /* package */ static final String KEY_STORE_ALIAS = "AdalKey";

    /**
     * A {@link SharedPreferences} key for storing decryption failure event.
     * see {@link AndroidBrokerStorageEncryptionManager#handleDecryptionFailure}
     */
    private static final String CURRENT_ACTIVE_BROKER_SHARED_PREF_KEY = "current_active_broker";

    protected final String LEGACY_AUTHENTICATOR_APP_KEY_ALIAS = "LEGACY_AUTHENTICATOR_APP_KEY";
    protected final String LEGACY_COMPANY_PORTAL_KEY_ALIAS = "LEGACY_COMPANY_PORTAL_KEY";

    private final Context mContext;
    private final ITelemetryCallback mTelemetryCallback;
    private final PredefinedKeyLoader mLegacyAuthAppKeyLoader;
    private final PredefinedKeyLoader mLegacyCPKeyLoader;
    private final AndroidWrappedKeyLoader mKeyStoreKeyLoader;

    public AndroidBrokerStorageEncryptionManager(@NonNull final Context context,
                                                 @Nullable final ITelemetryCallback telemetryCallback) {
        mContext = context;
        mTelemetryCallback = telemetryCallback;

        mLegacyAuthAppKeyLoader = new PredefinedKeyLoader(LEGACY_AUTHENTICATOR_APP_KEY_ALIAS,
                AuthenticationSettings.INSTANCE.getBrokerSecretKeys().get(AZURE_AUTHENTICATOR_APP_PACKAGE_NAME));

        mLegacyCPKeyLoader = new PredefinedKeyLoader(LEGACY_COMPANY_PORTAL_KEY_ALIAS,
                AuthenticationSettings.INSTANCE.getBrokerSecretKeys().get(COMPANY_PORTAL_APP_PACKAGE_NAME));

        mKeyStoreKeyLoader = new AndroidWrappedKeyLoader(KEY_STORE_ALIAS, context, telemetryCallback);
    }

    // Exposed for Robolectric.
    protected String getPackageName(){
        return mContext.getPackageName();
    }

    @Override
    public @NonNull AbstractSecretKeyLoader getKeyLoaderForEncryption() {
        Logger.info(TAG, "starting to getKeyLoaderForDecryption 0 param");
        if (StorageHelper.sShouldEncryptWithKeyStoreKey) {
            Logger.info(TAG, "sShouldEncryptWithKeyStoreKey=true");
            return mKeyStoreKeyLoader;
        }

        final String packageName = getPackageName();
        if (AZURE_AUTHENTICATOR_APP_PACKAGE_NAME.equalsIgnoreCase(packageName)) {
            Logger.info(TAG, "AZURE_AUTHENTICATOR_APP_PACKAGE_NAME=true");
            return mLegacyAuthAppKeyLoader;
        }

        if (COMPANY_PORTAL_APP_PACKAGE_NAME.equalsIgnoreCase(packageName) ||
                BROKER_HOST_APP_PACKAGE_NAME.equalsIgnoreCase(packageName)) {
            Logger.info(TAG, "COMPANY_PORTAL_APP_PACKAGE_NAME=true");
            return mLegacyCPKeyLoader;
        }

        throw new IllegalStateException("Matching encryption key not found, package name in use was " + packageName);
    }

    @Override
    public @NonNull List<AbstractSecretKeyLoader> getKeyLoaderForDecryption(@NonNull byte[] cipherText) {
        final String methodTag = TAG + ":getKeyLoaderForDecryption";
        final String packageName = getPackageName();
        Logger.info(methodTag, "starting to getKeyLoaderForDecryption");
        final ArrayList<AbstractSecretKeyLoader> keyLoaders = new ArrayList<>();

        if (isEncryptedByThisKeyIdentifier(cipherText, PredefinedKeyLoader.USER_PROVIDED_KEY_IDENTIFIER)) {
            Logger.info(methodTag, "USER_PROVIDED_KEY_IDENTIFIER=true");
            if (COMPANY_PORTAL_APP_PACKAGE_NAME.equalsIgnoreCase(packageName) ||
                    BROKER_HOST_APP_PACKAGE_NAME.equalsIgnoreCase(packageName)) {
                Logger.info(methodTag, "adding CP and AuthApp key loaders");
                keyLoaders.add(mLegacyCPKeyLoader);
                keyLoaders.add(mLegacyAuthAppKeyLoader);
                return keyLoaders;
            } else if (AZURE_AUTHENTICATOR_APP_PACKAGE_NAME.equalsIgnoreCase(packageName)) {
                Logger.info(methodTag, "adding  AuthApp and CP key loaders");
                keyLoaders.add(mLegacyAuthAppKeyLoader);
                keyLoaders.add(mLegacyCPKeyLoader);
                return keyLoaders;
            } else {
                Logger.warn(methodTag, "Unexpected Broker package name. Cannot load key.");
                throw new IllegalStateException("Unexpected Broker package name. Cannot load key.");
            }
        }

        if (isEncryptedByThisKeyIdentifier(cipherText, AndroidWrappedKeyLoader.KEY_IDENTIFIER)) {
            Logger.info(methodTag, "KEY_IDENTIFIER=true");
            keyLoaders.add(mKeyStoreKeyLoader);
            return keyLoaders;
        }

        Logger.warn(methodTag, "Cannot find a matching key to decrypt the given blob");
        return keyLoaders;
    }

    @Override
    protected void handleDecryptionFailure(@NonNull final String keyAlias,
                                           @NonNull final Exception exception) {
        final String methodTag = TAG + ":handleDecryptionFailure";
        final SharedPreferences sharedPreferences = PreferenceManager.
                getDefaultSharedPreferences(mContext);
        final String previousActiveBroker = sharedPreferences.getString(
                CURRENT_ACTIVE_BROKER_SHARED_PREF_KEY,
                ""
        );
        final String activeBroker = mContext.getPackageName();

        // We don't want to emit the same value multiple time.
        if (!previousActiveBroker.equalsIgnoreCase(activeBroker)) {
            final String message = "Decryption failed with key: " + keyAlias
                    + " Active broker: " + activeBroker
                    + " Exception: " + exception.toString();

            Logger.info(methodTag, message);

            if (mTelemetryCallback != null) {
                mTelemetryCallback.logEvent(
                        AuthenticationConstants.TelemetryEvents.DECRYPTION_ERROR,
                        true,
                        message);
            }

            sharedPreferences.edit().putString(CURRENT_ACTIVE_BROKER_SHARED_PREF_KEY, activeBroker).apply();
        }
    }
}
