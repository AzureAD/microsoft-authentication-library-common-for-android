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
import android.content.SharedPreferences;
import android.os.Build;
import android.security.KeyPairGeneratorSpec;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import androidx.annotation.RequiresApi;

import com.microsoft.identity.common.internal.util.AndroidKeyStoreUtil;
import com.microsoft.identity.common.java.controllers.ExceptionAdapter;
import com.microsoft.identity.common.java.crypto.key.AES256KeyLoader;
import com.microsoft.identity.common.java.crypto.key.KeyUtil;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.flighting.CommonFlight;
import com.microsoft.identity.common.java.flighting.CommonFlightsManager;
import com.microsoft.identity.common.java.opentelemetry.AttributeName;
import com.microsoft.identity.common.java.opentelemetry.OTelUtility;
import com.microsoft.identity.common.java.opentelemetry.SpanExtension;
import com.microsoft.identity.common.java.opentelemetry.SpanName;
import com.microsoft.identity.common.java.util.CachedData;
import com.microsoft.identity.common.java.util.FileUtil;
import com.microsoft.identity.common.logging.Logger;

import java.io.File;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.ProviderException;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Calendar;
import java.util.Locale;

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
public class AndroidWrappedKeyLoader extends AES256KeyLoader {
    private static final String TAG = AndroidWrappedKeyLoader.class.getSimpleName() + "#";

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
                mKeyCache.clear();
                return null;
            }

            final SecretKey key = AndroidKeyStoreUtil.unwrap(wrappedSecretKey, getKeySpecAlgorithm(), keyPair, WRAP_ALGORITHM);

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
            final SharedPreferences preferences = mContext.getSharedPreferences(mAlias, Context.MODE_PRIVATE);
            final boolean useNewKeyGenSpecForWrap2 = preferences.getBoolean("EnableNewKeyGenSpecForWrap2", false);
            Logger.info(methodTag, "No existing keypair. Generating a new one. Preference value: " + useNewKeyGenSpecForWrap2);
            preferences.edit().putBoolean("EnableNewKeyGenSpecForWrap2", !useNewKeyGenSpecForWrap2).apply();
            if (useNewKeyGenSpecForWrap2) {
                Logger.info(methodTag, "Using EnableNewKeyGenSpecForWrap2 for keypair generation.");
                final Span span = OTelUtility.createSpanFromParent(SpanName.KeyPairGeneration.name(), SpanExtension.current().getSpanContext());
                try (final Scope scope = SpanExtension.makeCurrentSpan(span)) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        Logger.info(methodTag, "Using new spec for keypair generation.");
                        // Use the new KeyPairGeneratorSpec for API 23 and above.
                        final int purposes = KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT;
                        final KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(mAlias, purposes)
                                .setKeySize(2048)
                                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
                                .build();
                        keyPair = AndroidKeyStoreUtil.generateKeyPair(
                                WRAP_KEY_ALGORITHM, spec);
                        Logger.info(methodTag, "Successfully generated keypair with new spec.");
                        span.setAttribute(AttributeName.key_pair_gen_successful_method.name(), "new_key_gen_spec_2");
                    } else {
                        // Use the legacy KeyPairGeneratorSpec for API 22 and below.
                        Logger.info(methodTag, "Using legacy spec for keypair generation for < 23");
                        keyPair = AndroidKeyStoreUtil.generateKeyPair(
                                WRAP_KEY_ALGORITHM, getLegacySpecForKeyStoreKey(mContext, mAlias));
                        Logger.info(methodTag, "Successfully generated keypair with legacy spec.");
                        span.setAttribute(AttributeName.key_pair_gen_successful_method.name(), "new_key_gen_spec_2_legacy");
                    }
                    span.setStatus(StatusCode.OK);
                } catch (final Throwable e) {
                    span.setAttribute(AttributeName.keypair_gen_exception.name(), e.getClass().getSimpleName());
                    span.recordException(e);
                    throw ExceptionAdapter.clientExceptionFromException(e);
                } finally {
                    span.end();
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && CommonFlightsManager.INSTANCE.getFlightsProvider().isFlightEnabled(CommonFlight.ENABLE_NEW_KEY_GEN_SPEC_FOR_WRAP)) {
                final long keypairGenStartTime = System.currentTimeMillis();
                final Span span = OTelUtility.createSpanFromParent(SpanName.KeyPairGeneration.name(), SpanExtension.current().getSpanContext());
                try (final Scope scope = SpanExtension.makeCurrentSpan(span)) {
                    keyPair = attemptKeyPairGeneration(mAlias, true, keypairGenStartTime);
                    Logger.info(methodTag, "Successfully generated keypair with new KeyPairGeneratorSpec with wrap purpose.");
                    span.setAttribute(AttributeName.key_pair_gen_successful_method.name(), "new_key_gen_spec_with_wrap");
                    span.setStatus(StatusCode.OK);
                } catch (final ProviderException e) {
                    if ("SecureKeyImportUnavailableException".equals(e.getClass().getSimpleName())) {
                        Logger.warn(methodTag, "Wrap purpose may not be supported. Retrying without wrap.");
                        try {
                            keyPair = attemptKeyPairGeneration(mAlias, false, keypairGenStartTime);
                            Logger.info(methodTag, "Successfully generated keypair with new KeyPairGeneratorSpec without wrap purpose.");
                            span.setAttribute(AttributeName.key_pair_gen_successful_method.name(), "new_key_gen_spec_without_wrap");
                            span.setStatus(StatusCode.OK);
                        } catch (final Exception ex) {
                            // 2nd fallback to legacy keygen spec
                            Logger.warn(methodTag, "Second attempt without wrap also failed. Falling back to legacy spec."+ ex);
                            keyPair = generateKeyPairWithLegacySpec(mAlias, keypairGenStartTime);
                            if (e.getMessage() != null) {
                                span.setAttribute(AttributeName.keypair_gen_exception.name(), e.getMessage());
                            }
                            span.setAttribute(AttributeName.key_pair_gen_successful_method.name(), "legacy_key_gen_spec");
                            span.setStatus(StatusCode.OK);
                        }
                    } else {
                        Logger.warn(methodTag, "Some unknown exception occurred. Running legacy keygen spec logic."+ e);
                        keyPair = generateKeyPairWithLegacySpec(mAlias, keypairGenStartTime);
                        span.setAttribute(AttributeName.key_pair_gen_successful_method.name(), "legacy_key_gen_spec");
                        span.setStatus(StatusCode.OK);
                    }
                } catch (final Throwable throwable) {
                    Logger.warn(methodTag, "Unexpected error with new KeyPairGeneratorSpec. Falling back to legacy spec. "+ throwable);
                    keyPair = generateKeyPairWithLegacySpec(mAlias, keypairGenStartTime);
                    if (throwable.getMessage() != null) {
                        span.setAttribute(AttributeName.keypair_gen_exception.name(), throwable.getMessage());
                    }
                    span.setAttribute(AttributeName.key_pair_gen_successful_method.name(), "legacy_key_gen_spec");
                    span.setStatus(StatusCode.OK);
                } finally {
                    span.end();
                }
            }
            else {
                // If flight for using new keygen spec is not enabled, use the legacy spec.
                Logger.info(methodTag, "Using legacy spec for keypair generation directly.");
                keyPair = AndroidKeyStoreUtil.generateKeyPair(
                        WRAP_KEY_ALGORITHM, getLegacySpecForKeyStoreKey(mContext, mAlias));
            }
        }
        final byte[] keyWrapped = AndroidKeyStoreUtil.wrap(unencryptedKey, keyPair, WRAP_ALGORITHM);
        FileUtil.writeDataToFile(keyWrapped, getKeyFile());
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private KeyPair attemptKeyPairGeneration(@NonNull final String alias, boolean useWrapPurpose, long keypairGenStartTime) throws ClientException{
        KeyPair keyPair = AndroidKeyStoreUtil.generateKeyPair(
                WRAP_KEY_ALGORITHM, getSpecForKeyStoreKey(alias, useWrapPurpose));
        recordKeyGenerationTime(keypairGenStartTime);
        return keyPair;
    }

    private KeyPair generateKeyPairWithLegacySpec(@NonNull final String alias, long keypairGenStartTime) throws ClientException{
        try {
            final KeyPair keyPair = AndroidKeyStoreUtil.generateKeyPair(
                    WRAP_KEY_ALGORITHM, getLegacySpecForKeyStoreKey(mContext, alias));
            recordKeyGenerationTime(keypairGenStartTime);
            return keyPair;
        } catch (final ClientException e) {
            SpanExtension.current().recordException(e);
            SpanExtension.current().setStatus(StatusCode.ERROR);
            Logger.error(TAG + ":generateKeyPairWithLegacySpec", "Error generating keypair with legacy spec.", e);
            throw e;
        }
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
        mKeyCache.clear();
    }

    /**
     * Generate a self-signed cert and derive an AlgorithmParameterSpec from that.
     * This is for the key to be generated in {@link KeyStore} via {@link KeyPairGenerator}
     * Note : This is now only for API level < 28
     *
     * @param context an Android {@link Context} object.
     * @return a {@link AlgorithmParameterSpec} for the keystore key (that we'll use to wrap the secret key).
     */
    private static AlgorithmParameterSpec getLegacySpecForKeyStoreKey(@NonNull final Context context,
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
     * Generate a self-signed cert and derive an AlgorithmParameterSpec from that.
     * This is for the key to be generated in {@link KeyStore} via {@link KeyPairGenerator}
     *
     * @param alias   the alias for the key.
     * @param tryPurposeWrap whether to try to use the wrap purpose in the key generation spec.
     * @return a {@link AlgorithmParameterSpec} for the keystore key (that we'll use to wrap the secret key).
     */
    @RequiresApi(api = Build.VERSION_CODES.P)
    private static AlgorithmParameterSpec getSpecForKeyStoreKey(@NonNull final String alias, boolean tryPurposeWrap) {
        int purposes = KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT;
        if (tryPurposeWrap) {
            purposes |= KeyProperties.PURPOSE_WRAP_KEY;
        }
        return new KeyGenParameterSpec.Builder(alias, purposes)
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
}
