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

/**
 * EncryptionManager class for non-broker (ADAL and MSAL) flows.
 * */
public class MsalEncryptionManager extends EncryptionManagerBase {
    private static final String TAG = MsalEncryptionManager.class.getName();

    /**
     * Singleton object.
     * */
    private static MsalEncryptionManager sInstance;

    public static synchronized MsalEncryptionManager getInstance(@NonNull Context context) {
        if (sInstance == null) {
            sInstance = new MsalEncryptionManager(context, context.getPackageName());
        }

        return sInstance;
    }

    protected MsalEncryptionManager(@NonNull Context context,
                                    @NonNull String packageName) {
        super(context, packageName);
    }

    /**
     * Loads key for ADAL/MSAL.
     * If user-defined key is set, this will return user-defined key.
     * Otherwise, it will return a keystore-encrypted key.
     * */
    @Override
    public synchronized EncryptionKeys loadSecretKeyForEncryption() throws IOException,
            GeneralSecurityException {
        final String methodName = ":loadSecretKeyForEncryption";

        // Try to get user defined key (ADAL/MSAL).
        if (AuthenticationSettings.INSTANCE.getSecretKeyData() != null) {
            return new EncryptionKeys(loadSecretKey(IEncryptionManager.KeyType.ADAL_USER_DEFINED_KEY), VERSION_USER_DEFINED);
        }

        // Try loading existing keystore-encrypted key. If it doesn't exist, create a new one.
        try {
            SecretKey key = loadSecretKey(KeyType.KEYSTORE_ENCRYPTED_KEY);
            if (key != null) {
                return new EncryptionKeys(key, VERSION_ANDROID_KEY_STORE);
            }
        } catch (final IOException | GeneralSecurityException e) {
            // If we fail to load key, proceed and generate a new one.
            Logger.warn(TAG + methodName, "Failed to load key with exception: " + e.toString());
        }

        Logger.verbose(TAG + methodName, "Keystore-encrypted key does not exist, try to generate new keys.");
        return new EncryptionKeys(generateKeyStoreEncryptedKey(), VERSION_ANDROID_KEY_STORE);
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
            case ADAL_USER_DEFINED_KEY:
                return KeystoreEncryptedKeyManager.getSecretKey(AuthenticationSettings.INSTANCE.getSecretKeyData());

            case KEYSTORE_ENCRYPTED_KEY:
                return getKeyStoreEncryptedKey();

            default:
                Logger.verbose(TAG + methodName, "Unknown KeyType. This code should never be reached.");
                throw new GeneralSecurityException(ErrorStrings.UNKNOWN_ERROR);
        }
    }
}
