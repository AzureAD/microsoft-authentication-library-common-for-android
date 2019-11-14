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

package com.microsoft.identity.common.adal.internal.cache;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import com.microsoft.identity.common.adal.internal.AuthenticationSettings;
import com.microsoft.identity.common.exception.ErrorStrings;
import com.microsoft.identity.common.internal.logging.Logger;

import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.crypto.SecretKey;

import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_PACKAGE_NAME;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME;

/**
 * Encryption manager for broker flows.
 * */
public class BrokerEncryptionManager extends EncryptionManagerBase {
    private static final String TAG = BrokerEncryptionManager.class.getName();

    /**
     * A flag to turn on/off keystore encryption on Broker apps.
     */
    public static final boolean sShouldEncryptWithKeyStoreKey = false;

    /**
     * Singleton object.
     * */
    private static BrokerEncryptionManager sInstance;

    public static synchronized BrokerEncryptionManager getInstance(@NonNull Context context) {
        if (sInstance == null) {
            sInstance = new BrokerEncryptionManager(context);
        }

        return sInstance;
    }

    protected BrokerEncryptionManager(@NonNull Context context) {
        super(context);
    }

    /**
     * Loads key for Broker.
     * If the legacy key is set and is enabled, use legacy key.
     * Otherwise, it will return a keystore-encrypted key.
     * */
    @Override
    public synchronized Pair<SecretKey, String> loadSecretKeyForEncryption() throws IOException,
            GeneralSecurityException {
        final String methodName = ":loadSecretKeyForEncryption";

        if (sShouldEncryptWithKeyStoreKey){
            // Try loading existing keystore-encrypted key. If it doesn't exist, create a new one.
            try {
                SecretKey key = loadSecretKey(KeyType.KEYSTORE_ENCRYPTED_KEY);
                if (key != null) {
                    return new Pair<>(key, VERSION_ANDROID_KEY_STORE);
                }
            } catch (final IOException | GeneralSecurityException e) {
                // If we fail to load key, proceed and generate a new one.
                Logger.warn(TAG + methodName, "Failed to load key with exception: " + e.toString());
            }
        }

        // If the keystore-encrypted key is not yet generated nor migrated, uses legacy key.
        if (AZURE_AUTHENTICATOR_APP_PACKAGE_NAME.equalsIgnoreCase(getPackageName())) {
            return new Pair<>(loadSecretKey(KeyType.LEGACY_AUTHENTICATOR_APP_KEY), VERSION_USER_DEFINED);
        } else {
            return new Pair<>(loadSecretKey(KeyType.LEGACY_COMPANY_PORTAL_KEY), VERSION_USER_DEFINED);
        }
    }

    /**
     * Given the key type, load a secret key.
     *
     * @return SecretKey. Null if there isn't any.
     */
    @Nullable
    public SecretKey loadSecretKey(@NonNull final KeyType keyType) throws IOException, GeneralSecurityException {
        final String methodName = ":loadSecretKey";

        switch (keyType) {
            case LEGACY_AUTHENTICATOR_APP_KEY:
                return getSecretKey(AuthenticationSettings.INSTANCE.getBrokerSecretKeys().get(AZURE_AUTHENTICATOR_APP_PACKAGE_NAME));

            case LEGACY_COMPANY_PORTAL_KEY:
                return getSecretKey(AuthenticationSettings.INSTANCE.getBrokerSecretKeys().get(COMPANY_PORTAL_APP_PACKAGE_NAME));

            case KEYSTORE_ENCRYPTED_KEY:
                return loadKeyStoreEncryptedKey();

            default:
                Logger.verbose(TAG + methodName, "Unknown KeyType. This code should never be reached.");
                throw new GeneralSecurityException(ErrorStrings.UNKNOWN_ERROR);
        }
    }
}
