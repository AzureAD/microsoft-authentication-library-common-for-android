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

import static com.microsoft.identity.common.java.crypto.key.AES256SecretKeyGenerator.AES_ALGORITHM;

import android.content.Context;
import android.os.Build;
import android.security.KeyPairGeneratorSpec;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;

import com.microsoft.identity.common.internal.util.AndroidKeyStoreUtil;
import com.microsoft.identity.common.java.controllers.ExceptionAdapter;
import com.microsoft.identity.common.java.crypto.key.AES256SecretKeyGenerator;
import com.microsoft.identity.common.java.crypto.key.ISecretKeyProvider;
import com.microsoft.identity.common.java.crypto.key.KeyUtil;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.flighting.CommonFlight;
import com.microsoft.identity.common.java.flighting.CommonFlightsManager;
import com.microsoft.identity.common.java.opentelemetry.AttributeName;
import com.microsoft.identity.common.java.opentelemetry.OTelUtility;
import com.microsoft.identity.common.java.opentelemetry.SpanExtension;
import com.microsoft.identity.common.java.opentelemetry.SpanName;
import com.microsoft.identity.common.java.util.FileUtil;
import com.microsoft.identity.common.java.util.StringUtil;
import com.microsoft.identity.common.logging.Logger;

import java.io.File;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.crypto.SecretKey;
import javax.security.auth.x500.X500Principal;

import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Scope;
import lombok.NonNull;

/**
 * This class doesn't really use the KeyStore-generated key directly.
 * <p>
 * Instead, the actual key that we use to encrypt/decrypt data is 'wrapped/encrypted' with the keystore key
 * before it get saved to the file.
 */
public class NewAndroidWrappedKeyProvider implements ISecretKeyProvider {

    /**
     * AES is 16 bytes (128 bits), thus PKCS#5 padding should not work, but in
     * Java AES/CBC/PKCS5Padding is default(!) algorithm name, thus PKCS5 here
     * probably doing PKCS7. We decide to go with Java default string.
     */
    private static final String CIPHER_TRANSFORMATION = "AES/CBC/PKCS5Padding";

    private static final String TAG = NewAndroidWrappedKeyProvider.class.getSimpleName() + "#";

    /**
     * Should KeyStore and key file check for validity before every key load be skipped.
     */
    @SuppressFBWarnings("MS_SHOULD_BE_FINAL")
    public static boolean sSkipKeyInvalidationCheck = false;

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

    /**
     * SecretKey cache. Maps wrapped secret key file path to the SecretKey.
     */
    private static final ConcurrentMap<String, SecretKey> sKeyCacheMap = new ConcurrentHashMap<>();
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

    // Exposed for testing only.
    @Nullable
    @VisibleForTesting
    /* package */ SecretKey getKeyFromCache() {
        return sKeyCacheMap.get(mFilePath);
    }

    // Exposed for testing only.
    @VisibleForTesting
    /* package */ void clearKeyFromCache() {
        sKeyCacheMap.remove(mFilePath);
    }

    /**
     * Default constructor
     *
     * @param alias             Alias(name) of the wrapping key.
     * @param filePath          Path to the file for storing the wrapped key.
     * @param context           Android's {@link Context}
     */
    public NewAndroidWrappedKeyProvider(@NonNull final String alias,
                                     @NonNull final String filePath,
                                     @NonNull final Context context) {
        mAlias = alias;
        mFilePath = filePath;
        mContext = context;
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
        final String methodTag = TAG + ":getKey";
        if (!sSkipKeyInvalidationCheck &&
                (!AndroidKeyStoreUtil.canLoadKey(mAlias) || !this.getKeyFile().exists())) {
            sKeyCacheMap.remove(mFilePath);
        }

        SecretKey key = sKeyCacheMap.get(mFilePath);
        if (key != null) {
            return key;
        }

        Logger.info(methodTag, "Key not in cache or cache is empty, loading key from storage");
        key = readSecretKeyFromStorage();

        // If key doesn't exist, generate a new one.
        if (key == null) {
            Logger.info(methodTag, "Key does not exist in storage, generating a new key");
            key = generateRandomKey();
        }

        sKeyCacheMap.put(mFilePath, key);
        return key;
    }

    @NonNull
    protected SecretKey generateRandomKey() throws ClientException {
        final String methodTag = TAG + ":generateRandomKey";

        final SecretKey key = AES256SecretKeyGenerator.INSTANCE.generateRandomKey();
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
    /* package */ synchronized SecretKey readSecretKeyFromStorage() throws ClientException {
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
                sKeyCacheMap.remove(mFilePath);
                return null;
            }

            final SecretKey key = AndroidKeyStoreUtil.unwrap(wrappedSecretKey, AES_ALGORITHM, keyPair, WRAP_ALGORITHM, null);

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
        if (keyPair == null) {
            Logger.info(methodTag, "No existing keypair. Generating a new one.");
            final Span span = OTelUtility.createSpanFromParent(SpanName.KeyPairGeneration.name(), SpanExtension.current().getSpanContext());
            try (final Scope ignored = SpanExtension.makeCurrentSpan(span)) {
                keyPair = generateNewKeyPair();
                span.setStatus(StatusCode.OK);
            } catch (final ClientException e) {
                span.setStatus(StatusCode.ERROR);
                span.recordException(e);
                throw e;
            } finally {
                span.end();
            }
        }
        final byte[] keyWrapped = AndroidKeyStoreUtil.wrap(unencryptedKey, keyPair, WRAP_ALGORITHM, null);
        FileUtil.writeDataToFile(keyWrapped, getKeyFile());
    }

    /**
     * Generate a new key pair wrapping key, based on API level uses different spec to generate
     * the key pair.
     * @return a key pair
     */
    @NonNull
    private KeyPair generateNewKeyPair() throws ClientException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return generateNewKeyPairAPI28AndAbove();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return generateNewKeyPairAPI23AndAbove();
        } else {
            return generateKeyPairWithLegacySpec();
        }
    }

    /**
     * Call this for API level >= 28. Starting level API 28 PURPOSE_WRAP_KEY is added. Based on flights
     * this method may or may not use the PURPOSE_WRAP_KEY along with PURPOSE_ENCRYPT and PURPOSE_DECRYPT. The logic
     * if (wrap key flight enabled) use all three purposes
     * else if (new key gen flight enabled) use only encrypt and decrypt purposes
     * else use legacy spec.
     * @return key pair
     */
    @RequiresApi(Build.VERSION_CODES.P)
    @NonNull
    private KeyPair generateNewKeyPairAPI28AndAbove() throws ClientException {
        if (CommonFlightsManager.INSTANCE.getFlightsProvider().isFlightEnabled(CommonFlight.ENABLE_NEW_KEY_GEN_SPEC_FOR_WRAP_WITH_PURPOSE_WRAP_KEY)) {
            return generateWrappingKeyPair_WithPurposeWrapKey();
        } else if (CommonFlightsManager.INSTANCE.getFlightsProvider().isFlightEnabled(CommonFlight.ENABLE_NEW_KEY_GEN_SPEC_FOR_WRAP_WITHOUT_PURPOSE_WRAP_KEY)) {
            return generateWrappingKeyPair();
        } else {
            return generateKeyPairWithLegacySpec();
        }
    }

    /**
     * Call this for API level >= 23. Based on flight new key gen spec is used else legacy which
     * is deprecated starting API 23.
     * @return key pair
     */
    @RequiresApi(Build.VERSION_CODES.M)
    @NonNull
    private KeyPair generateNewKeyPairAPI23AndAbove() throws ClientException {
        if (CommonFlightsManager.INSTANCE.getFlightsProvider().isFlightEnabled(CommonFlight.ENABLE_NEW_KEY_GEN_SPEC_FOR_WRAP_WITHOUT_PURPOSE_WRAP_KEY)) {
            return generateWrappingKeyPair();
        } else {
            return generateKeyPairWithLegacySpec();
        }
    }

    /**
     * Generate a new key pair wrapping key based on legacy logic. Call this for API < 23 or as fallback
     * until new key gen specs are stable.
     * @return key pair generated with legacy spec
     * @throws ClientException if there is an error generating the key pair.
     */
    @NonNull
    private KeyPair generateKeyPairWithLegacySpec() throws ClientException{
        final Span span = SpanExtension.current();
        try {
            final AlgorithmParameterSpec keyPairGenSpec = getLegacySpecForKeyStoreKey();
            final KeyPair keyPair = attemptKeyPairGeneration(keyPairGenSpec);
            span.setAttribute(AttributeName.key_pair_gen_successful_method.name(), "legacy_key_gen_spec");
            return keyPair;
        } catch (final Throwable e) {
            Logger.error(TAG + ":generateKeyPairWithLegacySpec", "Error generating keypair with legacy spec.", e);
            throw ExceptionAdapter.clientExceptionFromException(e);
        }
    }

    /**
     * Generate a new key pair wrapping key, based on API level >= 28. This method uses new key gen spec
     * with PURPOSE_WRAP_KEY. If this fails, it will fallback to generateWrappingKeyPair() which does not use
     * PURPOSE_WRAP_KEY (still uses new key gen spec).
     */
    @RequiresApi(Build.VERSION_CODES.P)
    private KeyPair generateWrappingKeyPair_WithPurposeWrapKey() throws ClientException {
        final String methodTag = TAG + ":generateWrappingKeyPair_WithPurposeWrapKey";
        final Span span = SpanExtension.current();
        try {
            Logger.info(methodTag, "Generating new keypair with new spec with purpose_wrap_key");
            int purposes = KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT | KeyProperties.PURPOSE_WRAP_KEY;
            final AlgorithmParameterSpec keyPairGenSpec = getSpecForWrappingKey(purposes);
            final KeyPair keyPair = attemptKeyPairGeneration(keyPairGenSpec);
            span.setAttribute(AttributeName.key_pair_gen_successful_method.name(), "new_key_gen_spec_with_wrap");
            return keyPair;
        } catch (final Throwable e) {
            Logger.error(methodTag, "Error generating keypair with new spec with purpose_wrap_key." +
                    "Attempting without purpose_wrap_key." , e);
            if (!StringUtil.isNullOrEmpty(e.getMessage())) {
                span.setAttribute(AttributeName.keypair_gen_exception.name(), e.getMessage());
            }
            return generateWrappingKeyPair();
        }
    }

    /**
     * Generate a new key pair wrapping key, based on API level >= 23. This method uses new key gen spec
     * with purposes PURPOSE_ENCRYPT and PURPOSE_DECRYPT. If this fails, it will fallback to generateKeyPairWithLegacySpec()
     * which uses olg key gen spec.
     */
    @RequiresApi(Build.VERSION_CODES.M)
    private KeyPair generateWrappingKeyPair() throws ClientException {
        final String methodTag = TAG + ":generateWrappingKeyPair";
        final Span span = SpanExtension.current();
        try {
            Logger.info(methodTag, "Generating new keypair with new spec without wrap key");
            int purposes = KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT;
            final AlgorithmParameterSpec keyPairGenSpec = getSpecForWrappingKey(purposes);
            final KeyPair keyPair = attemptKeyPairGeneration(keyPairGenSpec);
            span.setAttribute(AttributeName.key_pair_gen_successful_method.name(), "new_key_gen_spec_without_wrap");
            return keyPair;
        } catch (final Throwable e) {
            Logger.error(methodTag, "Error generating keypair with new spec." +
                    "Attempting with legacy spec.", e);
            if (!StringUtil.isNullOrEmpty(e.getMessage())) {
                span.setAttribute(AttributeName.keypair_gen_exception.name(), e.getMessage());
            }
            return generateKeyPairWithLegacySpec();
        }
    }

    private KeyPair attemptKeyPairGeneration(@NonNull final AlgorithmParameterSpec keyPairGenSpec) throws ClientException{
        final long keypairGenStartTime = System.currentTimeMillis();
        final KeyPair keyPair = AndroidKeyStoreUtil.generateKeyPair(
                WRAP_KEY_ALGORITHM, keyPairGenSpec);
        recordKeyGenerationTime(keypairGenStartTime);
        return keyPair;
    }

    private void recordKeyGenerationTime(long keypairGenStartTime) {
        long elapsedTime = System.currentTimeMillis() - keypairGenStartTime;
        SpanExtension.current().setAttribute(AttributeName.elapsed_time_keypair_generation.name(), elapsedTime);
    }

    /**
     * Wipe all the data associated from this key.
     */
    // VisibleForTesting
    public void deleteSecretKeyFromStorage() throws ClientException {
        AndroidKeyStoreUtil.deleteKey(mAlias);
        FileUtil.deleteFile(getKeyFile());
        sKeyCacheMap.remove(mFilePath);
    }

    /**
     * Generate a self-signed cert and derive an AlgorithmParameterSpec from that.
     * This is for the key to be generated in {@link KeyStore} via {@link KeyPairGenerator}
     * Note : This is now only for API level < 23 or as fallback.

     * @return a {@link AlgorithmParameterSpec} for the keystore key (that we'll use to wrap the secret key).
     */
    private AlgorithmParameterSpec getLegacySpecForKeyStoreKey() {
        // Generate a self-signed cert.
        final String certInfo = String.format(Locale.ROOT, "CN=%s, OU=%s",
                mAlias,
                mContext.getPackageName());

        final Calendar start = Calendar.getInstance();
        final Calendar end = Calendar.getInstance();
        final int certValidYears = 100;
        end.add(Calendar.YEAR, certValidYears);

        return new KeyPairGeneratorSpec.Builder(mContext)
                .setAlias(mAlias)
                .setSubject(new X500Principal(certInfo))
                .setSerialNumber(BigInteger.ONE)
                .setStartDate(start.getTime())
                .setEndDate(end.getTime())
                .build();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private AlgorithmParameterSpec getSpecForWrappingKey(int purposes) {
        return new KeyGenParameterSpec.Builder(mAlias, purposes)
                .setKeySize(2048)
                .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
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

    @NonNull
    @Override
    public String getCipherTransformation() {
        return CIPHER_TRANSFORMATION;
    }
}