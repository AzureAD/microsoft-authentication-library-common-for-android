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
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.LTW_APP_PACKAGE_NAME;

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
    protected final String LINK_TO_WINDOWS_KEY_ALIAS = "LINK_TO_WINDOWS_KEY";

    private final Context mContext;
    private final ITelemetryCallback mTelemetryCallback;
    private final PredefinedKeyLoader mLegacyAuthAppKeyLoader;
    private final PredefinedKeyLoader mLegacyCPKeyLoader;
    private final PredefinedKeyLoader mLTWKeyLoader;
    private final AndroidWrappedKeyLoader mKeyStoreKeyLoader;

    public AndroidBrokerStorageEncryptionManager(@NonNull final Context context,
                                                 @Nullable final ITelemetryCallback telemetryCallback) {
        mContext = context;
        mTelemetryCallback = telemetryCallback;

        mLegacyAuthAppKeyLoader = new PredefinedKeyLoader(LEGACY_AUTHENTICATOR_APP_KEY_ALIAS,
                AuthenticationSettings.INSTANCE.getBrokerSecretKeys().get(AZURE_AUTHENTICATOR_APP_PACKAGE_NAME));

        mLegacyCPKeyLoader = new PredefinedKeyLoader(LEGACY_COMPANY_PORTAL_KEY_ALIAS,
                AuthenticationSettings.INSTANCE.getBrokerSecretKeys().get(COMPANY_PORTAL_APP_PACKAGE_NAME));

        mLTWKeyLoader = new PredefinedKeyLoader(LINK_TO_WINDOWS_KEY_ALIAS,
                AuthenticationSettings.INSTANCE.getBrokerSecretKeys().get(LTW_APP_PACKAGE_NAME));

        mKeyStoreKeyLoader = new AndroidWrappedKeyLoader(KEY_STORE_ALIAS, context, telemetryCallback);
    }

    // Exposed for Robolectric.
    protected String getPackageName(){
        return mContext.getPackageName();
    }

    @Override
    public @NonNull AbstractSecretKeyLoader getKeyLoaderForEncryption() {
        if (StorageHelper.sShouldEncryptWithKeyStoreKey) {
            return mKeyStoreKeyLoader;
        }

        final String packageName = getPackageName();
        if (AZURE_AUTHENTICATOR_APP_PACKAGE_NAME.equalsIgnoreCase(packageName)) {
            return mLegacyAuthAppKeyLoader;
        }

        if (LTW_APP_PACKAGE_NAME.equalsIgnoreCase(packageName)) {
            return mLTWKeyLoader;
        }

        if (COMPANY_PORTAL_APP_PACKAGE_NAME.equalsIgnoreCase(packageName) ||
                BROKER_HOST_APP_PACKAGE_NAME.equalsIgnoreCase(packageName)) {
            return mLegacyCPKeyLoader;
        }

        throw new IllegalStateException("Matching encryption key not found, package name in use was " + packageName);
    }

    @Override
    public @NonNull List<AbstractSecretKeyLoader> getKeyLoaderForDecryption(@NonNull byte[] cipherText) {
        final String methodTag = TAG + ":getKeyLoaderForDecryption";
        final String packageName = getPackageName();

        final ArrayList<AbstractSecretKeyLoader> keyLoaders = new ArrayList<>();

        if (isEncryptedByThisKeyIdentifier(cipherText, PredefinedKeyLoader.USER_PROVIDED_KEY_IDENTIFIER)) {
            if (COMPANY_PORTAL_APP_PACKAGE_NAME.equalsIgnoreCase(packageName) ||
                    BROKER_HOST_APP_PACKAGE_NAME.equalsIgnoreCase(packageName)) {
                keyLoaders.add(mLegacyCPKeyLoader);
                keyLoaders.add(mLegacyAuthAppKeyLoader);
                keyLoaders.add(mLTWKeyLoader);
                return keyLoaders;
            } else if (AZURE_AUTHENTICATOR_APP_PACKAGE_NAME.equalsIgnoreCase(packageName)) {
                keyLoaders.add(mLegacyAuthAppKeyLoader);
                keyLoaders.add(mLegacyCPKeyLoader);
                keyLoaders.add(mLTWKeyLoader);
                return keyLoaders;
            } else if (LTW_APP_PACKAGE_NAME.equalsIgnoreCase(packageName)) {
                keyLoaders.add(mLTWKeyLoader);
                keyLoaders.add(mLegacyAuthAppKeyLoader);
                keyLoaders.add(mLegacyCPKeyLoader);
                return keyLoaders;
            } else {
                Logger.warn(methodTag, "Unexpected Broker package name. Cannot load key.");
                throw new IllegalStateException("Unexpected Broker package name. Cannot load key.");
            }
        }

        if (isEncryptedByThisKeyIdentifier(cipherText, AndroidWrappedKeyLoader.KEY_IDENTIFIER)) {
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
