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

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.internal.util.AndroidKeyStoreUtil;
import com.microsoft.identity.common.java.crypto.key.AES256KeyLoader;
import com.microsoft.identity.common.java.crypto.key.KeyUtil;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.telemetry.ITelemetryCallback;
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
     * Indicate that token item is encrypted with the key persisted in AndroidKeyStore.
     */
    public static final String KEY_IDENTIFIER = "A001";

    /**
     * Name of the file contains the symmetric key used for encryption/decryption.
     */
    /* package */ static final String KEY_FILE_PATH = "adalks";

    /* package */ static final int KEY_FILE_SIZE = 1024;

    private final Context mContext;
    private final ITelemetryCallback mTelemetryCallback;

    private final String mAlias;

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
     * @param alias             Alias(name) of this key
     * @param context           Android's {@link Context}
     * @param telemetryCallback a callback object for emitting telemetry events to Broker.
     */
    public AndroidWrappedKeyLoader(@NonNull final String alias,
                                   @NonNull final Context context,
                                   @Nullable final ITelemetryCallback telemetryCallback) {
        mAlias = alias;
        mContext = context;
        mTelemetryCallback = telemetryCallback;
    }

    @Override
    @NonNull
    public String getAlias() {
        return KEYSTORE_KEY_ALIAS;
    }

    @Override
    @NonNull
    public String getKeyTypeIdentifier() {
        return KEY_IDENTIFIER;
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
    protected SecretKey generateRandomKey() throws ClientException {
        final String methodTag = TAG + ":generateRandomKey";

        final SecretKey key = super.generateRandomKey();
        saveSecretKeyToStorage(key);

        logEvent(methodTag,
                AuthenticationConstants.TelemetryEvents.KEY_CREATED,
                false,
                "New key is generated.");

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
            final KeyPair keyPair = readKeyStoreKeyPair();
            if (keyPair == null) {
                Logger.info(methodTag, "key does not exist in keystore");
                deleteSecretKeyFromStorage();
                return null;
            }

            final byte[] wrappedSecretKey = FileUtil.readFromFile(getKeyFile(), KEY_FILE_SIZE);
            if (wrappedSecretKey == null) {
                Logger.warn(methodTag, "Key file is empty");
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
        KeyPair keyPair = readKeyStoreKeyPair();
        if(keyPair == null){
            Logger.info(methodTag, "No existing keypair. Generating a new one.");
            keyPair = generateKeyStoreKeyPair();
        }

        final byte[] keyWrapped = AndroidKeyStoreUtil.wrap(unencryptedKey, keyPair, WRAP_ALGORITHM);
        FileUtil.writeDataToFile(keyWrapped, getKeyFile());
    }

    /**
     * Wipe all the data associated from this key.
     */
    private void deleteSecretKeyFromStorage() throws ClientException {
        AndroidKeyStoreUtil.deleteKey(mAlias);
        FileUtil.deleteFile(getKeyFile());
        mKeyCache.clear();
    }


    /**
     * Generate the key in {@link KeyStore}.
     *
     * @return KeyPair. Null if there isn't any.
     */
    @NonNull
    private synchronized KeyPair generateKeyStoreKeyPair()
            throws ClientException {
        final String methodTag = TAG + ":generateKeyStoreKeyPair";
        try {
            logFlowStart(methodTag, AuthenticationConstants.TelemetryEvents.KEYSTORE_WRITE_START);
            final KeyPair keyPair = AndroidKeyStoreUtil.generateKeyPair(
                    WRAP_KEY_ALGORITHM,
                    getSpecForKeyStoreKey(mContext, mAlias));
            logFlowSuccess(methodTag, AuthenticationConstants.TelemetryEvents.KEYSTORE_WRITE_END, "");
            return keyPair;
        } catch (final ClientException e) {
            logFlowError(methodTag, AuthenticationConstants.TelemetryEvents.KEYSTORE_WRITE_END, e.toString(), e);
            throw e;
        }
    }

    /**
     * Read KeyPair from {@link KeyStore}..
     *
     * @return KeyPair. Null if there isn't any.
     */
    @Nullable
    private synchronized KeyPair readKeyStoreKeyPair()
            throws ClientException {
        final String methodTag = TAG + ":readKeyStoreKeyPair";
        try {
            logFlowStart(methodTag, AuthenticationConstants.TelemetryEvents.KEYSTORE_READ_START);

            final KeyPair keyPair = AndroidKeyStoreUtil.readKey(mAlias);
            if (keyPair == null) {
                logFlowSuccess(methodTag, AuthenticationConstants.TelemetryEvents.KEYSTORE_READ_END, "KeyStore is empty.");
            }

            logFlowSuccess(methodTag, AuthenticationConstants.TelemetryEvents.KEYSTORE_READ_END, "KeyStore KeyPair is loaded.");
            return keyPair;
        } catch (final ClientException e) {
            logFlowError(methodTag, AuthenticationConstants.TelemetryEvents.KEYSTORE_READ_END, e.toString(), e);
            throw e;
        }
    }

    /**
     * Generate a self-signed cert and derive an AlgorithmParameterSpec from that.
     * This is for the key to be generated in {@link KeyStore} via {@link KeyPairGenerator}
     *
     * @param context an Android {@link Context} object.
     * @return a {@link AlgorithmParameterSpec} for the keystore key (that we'll use to wrap the secret key).
     */
    @SuppressWarnings("deprecation")
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
                AndroidWrappedKeyLoader.KEY_FILE_PATH);
    }


    /**
     * Since Common isn't wired to telemetry yet at the point of implementation (July 18, 2019)
     * We use these functions to pass telemetry events to the calling ad-accounts.
     */
    private void logEvent(@NonNull final String methodTag,
                          @NonNull final String operationName,
                          final boolean isFailed,
                          @NonNull final String reason) {
        Logger.verbose(methodTag, operationName + ": " + reason);
        if (mTelemetryCallback != null) {
            mTelemetryCallback.logEvent(operationName, isFailed, reason);
        }
    }

    private void logFlowStart(@NonNull final String methodTag,
                              @NonNull final String operationName) {
        Logger.verbose(methodTag, operationName + " started.");
        if (mTelemetryCallback != null) {
            mTelemetryCallback.logEvent(operationName, false, "");
        }
    }

    private void logFlowSuccess(@NonNull final String methodTag,
                                @NonNull final String operationName,
                                @NonNull final String reason) {
        Logger.verbose(methodTag, operationName + " successfully finished: " + reason);
        if (mTelemetryCallback != null) {
            mTelemetryCallback.logEvent(operationName, false, reason);
        }
    }

    private void logFlowError(@NonNull final String methodTag,
                              @NonNull final String operationName,
                              @NonNull final String reason,
                              @Nullable Exception e) {
        Logger.error(methodTag, operationName + " failed: " + reason, e);
        if (mTelemetryCallback != null) {
            mTelemetryCallback.logEvent(operationName, true, reason);
        }
    }
}
