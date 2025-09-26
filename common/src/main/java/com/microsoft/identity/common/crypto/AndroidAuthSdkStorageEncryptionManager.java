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

import com.microsoft.identity.common.adal.internal.AuthenticationSettings;
import com.microsoft.identity.common.java.crypto.StorageEncryptionManager;
import com.microsoft.identity.common.java.crypto.key.ISecretKeyProvider;
import com.microsoft.identity.common.java.crypto.key.PredefinedKeyProvider;
import com.microsoft.identity.common.logging.Logger;

import java.util.Collections;
import java.util.List;

import lombok.NonNull;

/**
 * Key Encryption Manager for ADAL & MSAL.
 * Which supports both predefined key (if provided by the calling app),
 * and KeyStore-wrapped key.
 * */
public class AndroidAuthSdkStorageEncryptionManager extends StorageEncryptionManager {
    private static final String TAG = AndroidAuthSdkStorageEncryptionManager.class.getSimpleName();

    /**
     * Alias persisting the keypair in AndroidKeyStore.
     */
    public static final String WRAPPING_KEY_ALIAS = "AdalKey";

    /**
     * Name of the file contains the symmetric key used for encryption/decryption.
     */
    public static final String WRAPPED_KEY_FILE_NAME = "adalks";

    private final PredefinedKeyProvider mPredefinedKeyProvider;
    private final ISecretKeyProvider mKeyStoreKeyProvider;

    public AndroidAuthSdkStorageEncryptionManager(@NonNull final Context context) {
        if (AuthenticationSettings.INSTANCE.getSecretKeyData() == null) {
            mPredefinedKeyProvider = null;
        } else {
            mPredefinedKeyProvider = new PredefinedKeyProvider("USER_DEFINED_KEY",
                    AuthenticationSettings.INSTANCE.getSecretKeyData());
        }

        mKeyStoreKeyProvider = KeyStoreBackedSecretKeyProviderFactory.INSTANCE.create(
                WRAPPING_KEY_ALIAS,
                WRAPPED_KEY_FILE_NAME,
                context
        );
    }

    @Override
    @NonNull
    public ISecretKeyProvider getKeyProviderForEncryption() {
        if (mPredefinedKeyProvider != null) {
            return mPredefinedKeyProvider;
        }

        return mKeyStoreKeyProvider;
    }

    @Override
    @NonNull
    public List<ISecretKeyProvider> getKeyProviderForDecryption(byte[] cipherText) {
        final String methodTag = TAG + ":getKeyLoaderForDecryption";

        final String keyIdentifier = getKeyIdentifierFromCipherText(cipherText);
        if (PredefinedKeyProvider.USER_PROVIDED_KEY_IDENTIFIER.equalsIgnoreCase(keyIdentifier)) {
            if (mPredefinedKeyProvider != null) {
                return Collections.singletonList(mPredefinedKeyProvider);
            } else {
                throw new IllegalStateException(
                        "Cipher Text is encrypted by USER_PROVIDED_KEY_IDENTIFIER, " +
                                "but mPredefinedKeyProvider is null.");
            }
        } else if (mKeyStoreKeyProvider.getKeyTypeIdentifier().equalsIgnoreCase(keyIdentifier)) {
            return Collections.singletonList(mKeyStoreKeyProvider);
        }

        Logger.warn(methodTag,
                "Cannot find a matching key to decrypt the given blob. Key Identifier = " + keyIdentifier);
        return Collections.emptyList();
    }
}
