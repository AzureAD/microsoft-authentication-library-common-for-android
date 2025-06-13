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

import com.microsoft.identity.common.internal.util.AndroidKeyStoreUtil;
import com.microsoft.identity.common.java.crypto.key.AES256KeyLoader;
import com.microsoft.identity.common.java.crypto.key.KeyUtil;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.util.CachedData;
import com.microsoft.identity.common.java.util.FileUtil;
import com.microsoft.identity.common.logging.Logger;

import org.jetbrains.annotations.NotNull;

import java.io.File;

import javax.crypto.SecretKey;

import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.NonNull;

/**
 * This class doesn't really use the KeyStore-generated key directly.
 * <p>
 * Instead, the actual key that we use to encrypt/decrypt data is 'wrapped/encrypted' with the keystore key
 * before it get saved to the file.
 */
public class NewAndroidWrappedKeyLoader extends AES256KeyLoader {
    private static final String TAG = NewAndroidWrappedKeyLoader.class.getSimpleName() + "#";

    /**
     * Should KeyStore and key file check for validity before every key load be skipped.
     */
    @SuppressFBWarnings("MS_SHOULD_BE_FINAL")
    public static boolean sSkipKeyInvalidationCheck = false;

    /**
     * Indicate that token item is encrypted with the key loaded in this class.
     */
    public static final String WRAPPED_KEY_KEY_IDENTIFIER = "A001";

    // Exposed for testing only.
    public static final int KEY_FILE_SIZE = 1024;

    // Exposed for testing only.
    public static final String SECRET_KEY_ALGORITHM_FILE = "key_algorithm_file";

    private final Context mContext;

    /**
     * Name of the key itself. Must be unique.
     */
    private final String mAlias;

    private final IKekManager mKekManager;

    /**
     * Name of the file contains the wrapped symmetric key used for encryption/decryption.
     * Must be unique.
     */
    private final String mFilePath;

    private final CachedData<SecretKey> mKeyCache = new CachedData<SecretKey>() {
        @Override
        public SecretKey getData() {
            if (!sSkipKeyInvalidationCheck &&
                    (!AndroidKeyStoreUtil.canLoadKey(mAlias) || !getKeyFile().exists())) {
                this.clear();
            }
            return super.getData();
        }
    };

    // Exposed for testing only.
    @NonNull
    /* package */ CachedData<SecretKey> getKeyCache() {
        return mKeyCache;
    }

    /**
     * Default constructor
     *
     * @param alias             Alias(name) of the wrapping key.
     * @param filePath          Path to the file for storing the wrapped key.
     * @param context           Android's {@link Context}
     */
    public NewAndroidWrappedKeyLoader(@NonNull final String alias,
                                      @NonNull final String filePath,
                                      @NonNull final Context context) {
        mAlias = alias;
        mFilePath = filePath;
        mContext = context;
        mKekManager = new AndroidKeyStoreRsaKekManager(mAlias, mContext);
    }

    @Override
    @NonNull
    public String getAlias() {
        return mAlias;
    }

    @Override
    @NonNull
    public String getKeyTypeIdentifier() {
        return WRAPPED_KEY_KEY_IDENTIFIER;
    }

    /**
     * If key is already generated, that one will be returned.
     * Otherwise, generate a new one and return.
     */
    @Override
    @NonNull
    public synchronized SecretKey getKey() throws ClientException {
        SecretKey key = mKeyCache.getData();

        if (key == null) {
            key = readSecretKeyFromStorage();
        }

        // If key doesn't exist, generate a new one.
        if (key == null) {
            key = generateRandomKey();
        }

        mKeyCache.setData(key);
        return key;
    }

    @NonNull
    protected SecretKey generateRandomKey() throws ClientException {
        final String methodTag = TAG + ":generateRandomKey";

        final SecretKey key = getSecretKeyGenerator().generateRandomKey();
        saveSecretKeyToStorage(key, key.getAlgorithm());

        Logger.info(methodTag, "New key is generated with thumbprint: " +
                KeyUtil.getKeyThumbPrint(key));

        return key;
    }

    /**
     * Load the saved keystore-encrypted key. Will only do read operation.
     *
     * @return SecretKey. Null if there isn't any.
     */
    @Nullable
    /* package */ synchronized SecretKey readSecretKeyFromStorage() throws ClientException {
        final String methodTag = TAG + ":readSecretKeyFromStorage";
        try {

            if (!mKekManager.kekExists()) {
                Logger.info(methodTag, "key does not exist in keystore");
                deleteSecretKeyFromStorage();
                return null;
            }

            final byte[] wrappedSecretKey = FileUtil.readFromFile(getKeyFile(), KEY_FILE_SIZE);
            if (wrappedSecretKey == null) {
                Logger.warn(methodTag, "Key file is empty");
                // Do not delete the KeyStoreKeyPair even if the key file is empty. This caused credential cache
                // to be deleted in Office because of sharedUserId allowing keystore to be shared amongst apps.
                FileUtil.deleteFile(getKeyFile());
                FileUtil.deleteFile(getKeyAlgorithmFile());
                mKeyCache.clear();
                return null;
            }

            String keyAlgorithm = FileUtil.readStringFromFile(getKeyAlgorithmFile());
            if (keyAlgorithm == null || keyAlgorithm.isEmpty()) {
                keyAlgorithm = getSecretKeyGenerator().getKeyAlgorithm();
            }


            final SecretKey key = mKekManager.unwrapKey(wrappedSecretKey, keyAlgorithm);


            Logger.info(methodTag, "Key is loaded with thumbprint: " +
            KeyUtil.getKeyThumbPrint(key));

            return key;
        } catch (final ClientException e) {
            // Reset KeyPair info so that new request will generate correct KeyPairs.
            // All tokens with previous SecretKey are not possible to decrypt.
            Logger.warn(methodTag, "Error when loading key from Storage, " +
                    "wipe all existing key data ");
            deleteSecretKeyFromStorage();
            throw e;
        }
    }

    /**
     * Encrypt the given unencrypted symmetric key with Keystore key and save to storage.
     */
    private void saveSecretKeyToStorage(@NonNull final SecretKey unencryptedKey,
                                        @NonNull final String keyAlgorithm) throws ClientException {
        /*
         * !!WARNING!!
         * Multiple apps as of Today (1/4/2022) can still share a linux user id, by configuring
         * the sharedUserId attribute in their Android Manifest file.  If multiple apps reference
         * the same value for sharedUserId and are signed with the same keys, they will use
         * the same AndroidKeyStore and may obtain access to the files and shared preferences
         * of other applications by invoking createPackageContext.
         *
         * Support for sharedUserId is deprecated, however some applications still use this Android capability.
         * See: https://developer.android.com/guide/topics/manifest/manifest-element
         *
         * To address apps in this scenario we will attempt to load an existing KeyPair
         * instead of immediately generating a new key pair.  This will use the same keypair
         * to encrypt the symmetric key generated separately for each
         * application using a shared linux user id... and avoid these applications from
         * stomping/overwriting one another's keypair.
         */
        final byte[] keyWrapped = mKekManager.wrapKey(unencryptedKey);
        FileUtil.writeDataToFile(keyWrapped, getKeyFile());
        FileUtil.writeStringToFile(keyAlgorithm, getKeyAlgorithmFile());
    }
    /**
     * Wipe all the data associated from this key.
     */
    // VisibleForTesting
    public void deleteSecretKeyFromStorage() throws ClientException {
        AndroidKeyStoreUtil.deleteKey(mAlias);
        FileUtil.deleteFile(getKeyFile());
        FileUtil.deleteFile(getKeyAlgorithmFile());
        mKeyCache.clear();
    }


    /**
     * Get the file that stores the wrapped key.
     */
    private File getKeyFile() {
        return new File(
                mContext.getDir(mContext.getPackageName(), Context.MODE_PRIVATE),
                mFilePath);
    }

    /**
     * Get the file that stores the wrapped key.
     */
    private File getKeyAlgorithmFile() {
        return new File(
                mContext.getDir(mContext.getPackageName(), Context.MODE_PRIVATE),
                SECRET_KEY_ALGORITHM_FILE);
    }

    @Override
    public @NotNull String getCipherTransformation() {
        return mKekManager.getCipherTransformation();
    }
}
