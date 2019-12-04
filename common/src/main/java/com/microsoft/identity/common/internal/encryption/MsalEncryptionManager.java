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

/**
 * EncryptionManager class for non-broker (ADAL and MSAL) flows.
 */
public class MsalEncryptionManager extends BaseEncryptionManager {
    private static final String TAG = MsalEncryptionManager.class.getName();

    /**
     * Name of the file contains the keystore-encrypted key for ADAL/MSAL.
     */
    private static final String ADALKS = "adalks";

    public MsalEncryptionManager(@NonNull Context context) {
        super(context, ADALKS);
    }

    /**
     * Loads key for ADAL/MSAL.
     * If user-defined key is set, this will return user-defined key.
     * Otherwise, it will return a keystore-encrypted key.
     */
    @NonNull
    @Override
    public synchronized EncryptionKey loadKeyForEncryption() throws IOException,
            GeneralSecurityException {
        final String methodName = ":loadKeyForEncryption";

        // Try to get user defined key (ADAL/MSAL).
        final SecretKey userDefinedKey = loadSecretKey(KeyType.ADAL_USER_DEFINED_KEY);
        if (userDefinedKey != null) {
            return new EncryptionKey(KeyType.ADAL_USER_DEFINED_KEY, userDefinedKey);
        }

        // Try loading existing keystore-encrypted key. If it doesn't exist, create a new one.
        try {
            final SecretKey key = loadSecretKey(KeyType.KEYSTORE_ENCRYPTED_KEY);
            if (key != null) {
                return new EncryptionKey(KeyType.KEYSTORE_ENCRYPTED_KEY, key);
            }
        } catch (final IOException | GeneralSecurityException e) {
            // If we fail to load key, proceed and generate a new one.
            Logger.warn(TAG + methodName, "Failed to load key with exception: " + e.toString());
        }

        Logger.verbose(TAG + methodName, "Keystore-encrypted key does not exist, try to generate new keys.");
        return new EncryptionKey(KeyType.KEYSTORE_ENCRYPTED_KEY, generateKeyStoreEncryptedKey());
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
            case ADAL_USER_DEFINED_KEY:
                return getSecretKeyFromRawByteArray(AuthenticationSettings.INSTANCE.getSecretKeyData());

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
        final List<KeyType> keyTypeList = new ArrayList<>();
        if (encryptionType == EncryptionType.USER_DEFINED) {
            keyTypeList.add(KeyType.ADAL_USER_DEFINED_KEY);
        } else if (encryptionType == EncryptionType.ANDROID_KEY_STORE) {
            keyTypeList.add(KeyType.KEYSTORE_ENCRYPTED_KEY);
        }
        return keyTypeList;
    }

}
