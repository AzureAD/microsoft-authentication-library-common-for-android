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
import com.microsoft.identity.common.java.crypto.key.AES256KeyLoader;
import com.microsoft.identity.common.java.crypto.key.AbstractSecretKeyLoader;
import com.microsoft.identity.common.java.crypto.key.PredefinedKeyLoader;
import com.microsoft.identity.common.java.telemetry.ITelemetryCallback;
import com.microsoft.identity.common.logging.Logger;

import java.util.Collections;
import java.util.List;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.NonNull;

/**
 * Key Encryption Manager for ADAL & MSAL.
 * */
public class AndroidAuthSdkStorageEncryptionManager extends StorageEncryptionManager {
    private static final String TAG = AndroidAuthSdkStorageEncryptionManager.class.getSimpleName();

    /**
     * Alias persisting the keypair in AndroidKeyStore.
     */
    /* package */ static final String KEY_STORE_ALIAS = "AdalKey";

    private final PredefinedKeyLoader mPredefinedKeyLoader;
    private final AndroidWrappedKeyLoader mKeyStoreKeyLoader;

    public AndroidAuthSdkStorageEncryptionManager(@NonNull final Context context,
                                                  @Nullable final ITelemetryCallback telemetryCallback) {
        if (AuthenticationSettings.INSTANCE.getSecretKeyData() == null) {
            mPredefinedKeyLoader = null;
        } else {
            mPredefinedKeyLoader = new PredefinedKeyLoader("USER_DEFINED_KEY",
                    AuthenticationSettings.INSTANCE.getSecretKeyData());
        }

        mKeyStoreKeyLoader = new AndroidWrappedKeyLoader(KEY_STORE_ALIAS, context, telemetryCallback);
    }

    @Override
    public @NonNull AES256KeyLoader getKeyLoaderForEncryption() {
        if (mPredefinedKeyLoader != null) {
            Logger.info(TAG, "getKeyLoaderForEncryption returning predefined key loader");
            return mPredefinedKeyLoader;
        }
        Logger.info(TAG, "getKeyLoaderForEncryption returning mKeyStoreKeyLoader");
        return mKeyStoreKeyLoader;
    }

    @Override
    public @NonNull List<AbstractSecretKeyLoader> getKeyLoaderForDecryption(@NonNull byte[] cipherText) {
        final String methodTag = TAG + ":getKeyLoaderForDecryption";
        Logger.info(methodTag, "starting to getKeyLoaderForDecryption");
        final String keyIdentifier = getKeyIdentifierFromCipherText(cipherText);
        if (PredefinedKeyLoader.USER_PROVIDED_KEY_IDENTIFIER.equalsIgnoreCase(keyIdentifier)) {
            if (mPredefinedKeyLoader != null) {
                Logger.info(TAG, "getKeyLoaderForEncryption returning mPredefinedKeyLoader "+ keyIdentifier);
                return Collections.<AbstractSecretKeyLoader>singletonList(mPredefinedKeyLoader);
            } else {
                throw new IllegalStateException(
                        "Cipher Text is encrypted by USER_PROVIDED_KEY_IDENTIFIER, " +
                                "but mPredefinedKeyLoader is null.");
            }
        } else if (AndroidWrappedKeyLoader.KEY_IDENTIFIER.equalsIgnoreCase(keyIdentifier)) {
            Logger.info(TAG, "getKeyLoaderForEncryption returning mKeyStoreKeyLoader "+keyIdentifier);
            return Collections.<AbstractSecretKeyLoader>singletonList(mKeyStoreKeyLoader);
        }

        Logger.warn(methodTag, "Cannot find a matching key to decrypt the given blob");
        return Collections.emptyList();
    }
}
