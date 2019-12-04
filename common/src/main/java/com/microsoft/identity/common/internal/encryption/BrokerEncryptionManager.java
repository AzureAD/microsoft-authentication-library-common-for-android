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

package com.microsoft.identity.common.internal.encryption;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.common.adal.internal.AuthenticationSettings;
import com.microsoft.identity.common.exception.ErrorStrings;
import com.microsoft.identity.common.internal.logging.Logger;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.SecretKey;

import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_PACKAGE_NAME;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME;

/**
 * Encryption manager for broker flows.
 */
public class BrokerEncryptionManager extends BaseEncryptionManager {
    private static final String TAG = BrokerEncryptionManager.class.getName();

    /**
     * A flag to turn on/off keystore encryption on Broker apps.
     */
    public static final boolean sShouldEncryptWithKeyStoreKey = false;

    /**
     * Name of the file contains the keystore-encrypted key for broker.
     */
    private static final String BROKERKS = "brokerks";

    /**
     * Name of the calling package. To be set by test cases only.
     */
    private static String sMockPackageName;

    /**
     * For test cases only. For testing keystore-encrypted key when it's turned off in production.
     */
    private boolean mforceEnableKeyStoreKey = false;

    /**
     * To test legacy keys in common's test project,
     * we have to set mockPackageName to be one of those broker apps.
     */
    public static void setMockPackageName(@NonNull String mockPackageName) {
        synchronized (BrokerEncryptionManager.class) {
            sMockPackageName = mockPackageName;
        }
    }

    private String getPackageName() {
        if (sMockPackageName != null) {
            return sMockPackageName;
        }

        return mContext.getPackageName();
    }

    public BrokerEncryptionManager(@NonNull final Context context) {
        super(context, BROKERKS);
    }

    // For test cases only.
    BrokerEncryptionManager(@NonNull final Context context, final boolean forceEnableKeyStoreKey) {
        super(context, BROKERKS);
        mforceEnableKeyStoreKey = forceEnableKeyStoreKey;
    }

    /**
     * Loads key for Broker.
     * If new key is enabled and exists, use new key.
     * Otherwise, use legacy key.
     */
    @NonNull
    @Override
    public synchronized EncryptionKey loadKeyForEncryption() throws IOException,
            GeneralSecurityException {
        final String methodName = ":loadKeyForEncryption";

        if (sShouldEncryptWithKeyStoreKey || mforceEnableKeyStoreKey) {
            // Try loading existing keystore-encrypted key.
            // If it doesn't exist nor it is not loadable, fall back to legacy keys.
            try {
                final SecretKey key = loadSecretKey(KeyType.KEYSTORE_ENCRYPTED_KEY);
                if (key != null) {
                    return new EncryptionKey(KeyType.KEYSTORE_ENCRYPTED_KEY, key);
                }
            } catch (final IOException | GeneralSecurityException e) {
                // If we fail to load key, proceed and generate a new one.
                Logger.warn(TAG + methodName, "Failed to load key with exception: " + e.toString());
            }
        }

        // Legacy keys.
        if (AZURE_AUTHENTICATOR_APP_PACKAGE_NAME.equalsIgnoreCase(getPackageName())) {
            final SecretKey key = loadSecretKey(KeyType.LEGACY_AUTHENTICATOR_APP_KEY);
            if (key != null) {
                return new EncryptionKey(KeyType.LEGACY_AUTHENTICATOR_APP_KEY, key);
            }
        } else if (COMPANY_PORTAL_APP_PACKAGE_NAME.equalsIgnoreCase(getPackageName())) {
            final SecretKey key = loadSecretKey(KeyType.LEGACY_COMPANY_PORTAL_KEY);
            if (key != null) {
                return new EncryptionKey(KeyType.LEGACY_COMPANY_PORTAL_KEY, key);
            }
        }

        Logger.verbose(TAG + methodName, "This line should never be reached.");
        throw new GeneralSecurityException(ErrorStrings.UNKNOWN_ERROR);
    }

    /**
     * Given the key type, load a secret key.
     *
     * @return SecretKey. Null if there isn't any.
     */
    @Nullable
    @Override
    public SecretKey loadSecretKey(@NonNull final KeyType keyType) throws IOException, GeneralSecurityException {
        final String methodName = ":loadSecretKey";

        switch (keyType) {
            case LEGACY_AUTHENTICATOR_APP_KEY:
                return getSecretKeyFromRawByteArray(AuthenticationSettings.INSTANCE.getBrokerSecretKeys().get(AZURE_AUTHENTICATOR_APP_PACKAGE_NAME));

            case LEGACY_COMPANY_PORTAL_KEY:
                return getSecretKeyFromRawByteArray(AuthenticationSettings.INSTANCE.getBrokerSecretKeys().get(COMPANY_PORTAL_APP_PACKAGE_NAME));

            case KEYSTORE_ENCRYPTED_KEY:
                return loadKeyStoreEncryptedKey();

            default:
                Logger.verbose(TAG + methodName, "Unknown KeyType. This line should never be reached.");
                throw new GeneralSecurityException(ErrorStrings.UNKNOWN_ERROR);
        }
    }

    /**
     * Get all the key type that could be potential candidates for decryption.
     **/
    @NonNull
    @Override
    public List<KeyType> getKeyTypesForDecryption(@NonNull final EncryptionType encryptionType) {
        final List<KeyType> keyTypes = new ArrayList<>();
        if (encryptionType == EncryptionType.USER_DEFINED) {
            if (AZURE_AUTHENTICATOR_APP_PACKAGE_NAME.equalsIgnoreCase(getPackageName())) {
                keyTypes.add(KeyType.LEGACY_AUTHENTICATOR_APP_KEY);
                keyTypes.add(KeyType.LEGACY_COMPANY_PORTAL_KEY);
            } else if (COMPANY_PORTAL_APP_PACKAGE_NAME.equalsIgnoreCase(getPackageName())) {
                keyTypes.add(KeyType.LEGACY_COMPANY_PORTAL_KEY);
                keyTypes.add(KeyType.LEGACY_AUTHENTICATOR_APP_KEY);
            }
        } else if (encryptionType == EncryptionType.ANDROID_KEY_STORE) {
            keyTypes.add(KeyType.KEYSTORE_ENCRYPTED_KEY);
        }
        return keyTypes;
    }
}
