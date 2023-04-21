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

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.security.KeyPairGeneratorSpec;

import androidx.annotation.RequiresApi;

import com.microsoft.identity.common.internal.util.AndroidKeyStoreUtil;
import com.microsoft.identity.common.java.crypto.key.AES256KeyLoader;
import com.microsoft.identity.common.java.crypto.key.KeyUtil;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.util.CachedData;
import com.microsoft.identity.common.java.util.FileUtil;
import com.microsoft.identity.common.logging.Logger;

import java.io.File;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Calendar;
import java.util.Locale;

import javax.crypto.SecretKey;
import javax.security.auth.x500.X500Principal;

import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.NonNull;

/**
 * This class doesn't really use the KeyStore-generated key directly.
 * <p>
 * Instead, the actual key that we use to encrypt/decrypt data is 'wrapped/encrypted' with the keystore key
 * before it get saved to the file.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class AndroidWrappedKeyLoader extends AES256KeyLoader {
    private static final String TAG = AndroidWrappedKeyLoader.class.getSimpleName() + "#";

    /**
     * Should KeyStore and key file check for validity before every key load be skipped.
     */
    @SuppressFBWarnings("MS_SHOULD_BE_FINAL")
    public static boolean sSkipKeyInvalidationCheck = false;

    /**
     * Alias for this type of key.
     */
    /* package */ static final String KEYSTORE_KEY_ALIAS = "KEYSTORE_KEY";

    /**
     * Algorithm for key wrapping.
     */
    private static final String WRAP_ALGORITHM = "RSA/ECB/PKCS1Padding";

    /**
     * Algorithm for the wrapping key itself.
     */
    private static final String WRAP_KEY_ALGORITHM = "RSA";

    /**
     * Indicate that token item is encrypted with the key loaded in this class.
     */
    public static final String WRAPPED_KEY_KEY_IDENTIFIER = "A001";

    // Exposed for testing only.
    /* package */ static final int KEY_FILE_SIZE = 1024;

    private final Context mContext;

    /**
     * Name of the key itself. Must be unique.
     */
    private final String mAlias;

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
    public AndroidWrappedKeyLoader(@NonNull final String alias,
                                   @NonNull final String filePath,
                                   @NonNull final Context context) {
        mAlias = alias;
        mFilePath = filePath;
        mContext = context;
    }

    @Override
    @NonNull
    public String getAlias() {
        return KEYSTORE_KEY_ALIAS;
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

    @Override
    @NonNull
    protected SecretKey generateRandomKey() throws ClientException {
        final String methodTag = TAG + ":generateRandomKey";

        final SecretKey key = super.generateRandomKey();
        saveSecretKeyToStorage(key);

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
    /* package */ SecretKey readSecretKeyFromStorage() throws ClientException {
        final String methodTag = TAG + ":readSecretKeyFromStorage";
        try {
            final KeyPair keyPair = AndroidKeyStoreUtil.readKey(mAlias);
            if (keyPair == null) {
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
                mKeyCache.clear();
                return null;
            }

            final SecretKey key = AndroidKeyStoreUtil.unwrap(wrappedSecretKey, getKeySpecAlgorithm(), keyPair, WRAP_ALGORITHM);

            Logger.info(methodTag, "New key is generated with thumbprint: " +
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
    private void saveSecretKeyToStorage(@NonNull final SecretKey unencryptedKey) throws ClientException {
        final String methodTag = TAG + ":saveSecretKeyToStorage";
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
        KeyPair keyPair = AndroidKeyStoreUtil.readKey(mAlias);
        if(keyPair == null){
            Logger.info(methodTag, "No existing keypair. Generating a new one.");
            keyPair = AndroidKeyStoreUtil.generateKeyPair(
                    WRAP_KEY_ALGORITHM,
                    getSpecForKeyStoreKey(mContext, mAlias));
        }

        final byte[] keyWrapped = AndroidKeyStoreUtil.wrap(unencryptedKey, keyPair, WRAP_ALGORITHM);
        FileUtil.writeDataToFile(keyWrapped, getKeyFile());
    }

    /**
     * Wipe all the data associated from this key.
     */
    // VisibleForTesting
    public void deleteSecretKeyFromStorage() throws ClientException {
        AndroidKeyStoreUtil.deleteKey(mAlias);
        FileUtil.deleteFile(getKeyFile());
        mKeyCache.clear();
    }

    /**
     * Generate a self-signed cert and derive an AlgorithmParameterSpec from that.
     * This is for the key to be generated in {@link KeyStore} via {@link KeyPairGenerator}
     *
     * @param context an Android {@link Context} object.
     * @return a {@link AlgorithmParameterSpec} for the keystore key (that we'll use to wrap the secret key).
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private static AlgorithmParameterSpec getSpecForKeyStoreKey(@NonNull final Context context,
                                                                @NonNull final String alias) {
        // Generate a self-signed cert.
        final String certInfo = String.format(Locale.ROOT, "CN=%s, OU=%s",
                alias,
                context.getPackageName());

        final Calendar start = Calendar.getInstance();
        final Calendar end = Calendar.getInstance();
        final int certValidYears = 100;
        end.add(Calendar.YEAR, certValidYears);

        return new KeyPairGeneratorSpec.Builder(context)
                .setAlias(alias)
                .setSubject(new X500Principal(certInfo))
                .setSerialNumber(BigInteger.ONE)
                .setStartDate(start.getTime())
                .setEndDate(end.getTime())
                .build();
    }

    /**
     * Get the file that stores the wrapped key.
     */
    private File getKeyFile() {
        return new File(
                mContext.getDir(mContext.getPackageName(), Context.MODE_PRIVATE),
                mFilePath);
    }
}
