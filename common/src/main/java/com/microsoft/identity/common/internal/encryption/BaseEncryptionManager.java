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
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.util.Base64;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.adal.internal.cache.IWpjTelemetryCallback;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.exception.ErrorStrings;
import com.microsoft.identity.common.internal.logging.Logger;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.DigestException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

abstract class BaseEncryptionManager implements IEncryptionManager {
    private static final String TAG = BaseEncryptionManager.class.getSimpleName();

    /**
     * Key spec algorithm.
     */
    static final String KEYSPEC_ALGORITHM = "AES";

    /**
     * AES is 16 bytes (128 bits), thus PKCS#5 padding should not work, but in
     * Java AES/CBC/PKCS5Padding is default(!) algorithm name, thus PKCS5 here
     * probably doing PKCS7. We decide to go with Java default string.
     */
    private static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private static final String CURRENT_ACTIVE_BROKER = "current_active_broker";

    /**
     * IV Key length for AES-128.
     */
    public static final int DATA_KEY_LENGTH = 16;

    /**
     * 256 bits output for signing message.
     */
    public static final int HMAC_LENGTH = 32;

    /**
     * Length of the key version blob.
     */
    private static final int KEY_VERSION_BLOB_LENGTH = 4;

    /**
     * Indicate that token item is encrypted with the key persisted in AndroidKeyStore.
     */
    public static final String VERSION_ANDROID_KEY_STORE = "A001";

    /**
     * Indicate that the token item is encrypted with the user provided key.
     */
    public static final String VERSION_USER_DEFINED = "U001";

    /**
     * To keep track of encoding version and related flags.
     */
    private static final String ENCODE_VERSION = "E1";

    protected final Context mContext;
    private IWpjTelemetryCallback mTelemetryCallback;
    private final KeystoreEncryptedKeyManager mKeystoreEncryptedKeyManager;

    /**
     * Cached keys.
     */
    private EncryptionKey mEncryptionKey;
    private Map<KeyType, EncryptionKey> mDecryptionKeys;

    /**
     * Constructor for {@link StorageHelper}.
     *
     * @param context The {@link Context} to create {@link StorageHelper}.
     */
    protected BaseEncryptionManager(@NonNull final Context context,
                                    @NonNull final String keyFileName) {
        mContext = context.getApplicationContext();
        mKeystoreEncryptedKeyManager = new KeystoreEncryptedKeyManager(context, keyFileName);
        mTelemetryCallback = null;
        mEncryptionKey = null;
        mDecryptionKeys = new HashMap<>();
    }

    /**
     * Sets telemetry callback to the StorageHelper object.
     * This is currently exposed because Telemetry is not wired to Common yet.
     *
     * @param IWpjTelemetryCallback a callback object.
     */
    public synchronized void setTelemetryCallback(@Nullable final IWpjTelemetryCallback telemetryCallback) {
        if (mTelemetryCallback == null && telemetryCallback != null) {
            mTelemetryCallback = telemetryCallback;
        }
    }

    @Override
    public synchronized String encrypt(final String clearText)
            throws GeneralSecurityException, IOException {
        final String methodName = ":encrypt";

        if (StringExtensions.isNullOrBlank(clearText)) {
            throw new IllegalArgumentException("Input is empty or null");
        }

        Logger.verbose(TAG + methodName, "Starting encryption");

        // Try to read keystore key - to verify how often this is invoked before the migration is done.
        // TODO: remove this whole try-catch clause once the experiment is done.
        if (mTelemetryCallback != null) {
            try {
                final SecretKey key = loadSecretKey(KeyType.KEYSTORE_ENCRYPTED_KEY);
                if (key == null) {
                    mTelemetryCallback.logEvent(mContext, methodName, false, "KEY_ENCRYPTION_KEYSTORE_KEY_NOT_INITIALIZED");
                }
            } catch (final Exception e) {
                // Best effort.
                mTelemetryCallback.logEvent(mContext, methodName, false, "KEY_ENCRYPTION_KEYSTORE_KEY_FAILED_TO_LOAD");
            }
        }

        // Loading key only once for performance.
        if (mEncryptionKey == null) {
            mEncryptionKey = loadKeyForEncryption();
        }

        final String blobVersion = mEncryptionKey.getKeyType().getBlobVersion();
        Logger.verbose(TAG + methodName, "Encrypt version:" + blobVersion);
        final byte[] blobVersionBytes = blobVersion.getBytes(AuthenticationConstants.ENCODING_UTF8);
        final byte[] bytes = clearText.getBytes(AuthenticationConstants.ENCODING_UTF8);

        // IV: Initialization vector that is needed to start CBC
        final byte[] iv = new byte[DATA_KEY_LENGTH];
        new SecureRandom().nextBytes(iv);
        final IvParameterSpec ivSpec = new IvParameterSpec(iv);

        // Set to encrypt mode
        final Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        final Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, mEncryptionKey.getKey(), ivSpec);

        final byte[] encrypted = cipher.doFinal(bytes);

        // Mac output to sign encryptedData+IV. Keyversion is not included
        // in the digest. It defines what to use for Mac Key.
        mac.init(mEncryptionKey.getHMACKey());
        mac.update(blobVersionBytes);
        mac.update(encrypted);
        mac.update(iv);
        final byte[] macDigest = mac.doFinal();

        // Init array to store blobVersion, encrypted data, iv, macdigest
        final byte[] blobVerAndEncryptedDataAndIVAndMacDigest = new byte[blobVersionBytes.length
                + encrypted.length + iv.length + macDigest.length];
        System.arraycopy(blobVersionBytes, 0, blobVerAndEncryptedDataAndIVAndMacDigest, 0,
                blobVersionBytes.length);
        System.arraycopy(encrypted, 0, blobVerAndEncryptedDataAndIVAndMacDigest,
                blobVersionBytes.length, encrypted.length);
        System.arraycopy(iv, 0, blobVerAndEncryptedDataAndIVAndMacDigest, blobVersionBytes.length
                + encrypted.length, iv.length);
        System.arraycopy(macDigest, 0, blobVerAndEncryptedDataAndIVAndMacDigest, blobVersionBytes.length
                + encrypted.length + iv.length, macDigest.length);

        final String encryptedText = new String(Base64.encode(blobVerAndEncryptedDataAndIVAndMacDigest,
                Base64.NO_WRAP), AuthenticationConstants.ENCODING_UTF8);
        Logger.verbose(TAG + methodName, "Finished encryption");

        return getEncodeVersionLengthPrefix() + ENCODE_VERSION + encryptedText;
    }

    @Override
    public synchronized String decrypt(final String encryptedBlob) throws GeneralSecurityException, IOException {
        final String methodName = ":decrypt";
        Logger.verbose(TAG + methodName, "Starting decryption");

        if (StringExtensions.isNullOrBlank(encryptedBlob)) {
            throw new IllegalArgumentException("Input is empty or null");
        }

        final EncryptionType encryptionType = getEncryptionType(encryptedBlob);
        if (encryptionType == EncryptionType.UNENCRYPTED) {
            Logger.warn(TAG + methodName, "This string is not encrypted. Finished decryption.");
            return encryptedBlob;
        }

        // Try to read keystore key - to verify how often this is invoked before the migration is done.
        // TODO: remove this whole try-catch clause once the experiment is done.
        if (mTelemetryCallback != null) {
            try {
                final SecretKey key = loadSecretKey(KeyType.KEYSTORE_ENCRYPTED_KEY);
                if (key == null) {
                    mTelemetryCallback.logEvent(mContext, methodName, false, "KEY_DECRYPTION_KEYSTORE_KEY_NOT_INITIALIZED");
                }
            } catch (final Exception e) {
                // Best effort.
                mTelemetryCallback.logEvent(mContext, methodName, false, "KEY_DECRYPTION_KEYSTORE_KEY_FAILED_TO_LOAD");
            }
        }

        final List<KeyType> decryptionKeyTypes = getKeyTypesForDecryption(encryptionType);
        for (final KeyType keyType : decryptionKeyTypes) {
            try {
                EncryptionKey key = mDecryptionKeys.get(keyType);

                // Only load key when it's not in the cache.
                if (key == null) {
                    final SecretKey secretKey = loadSecretKey(keyType);
                    if (secretKey == null){
                        continue;
                    }

                    key = new EncryptionKey(keyType, secretKey);
                    mDecryptionKeys.put(keyType, key);
                }

                final String result = decryptWithSecretKey(encryptedBlob, key);
                Logger.verbose(TAG + methodName, "Finished decryption with keyType:" + keyType.name());
                return result;
            } catch (GeneralSecurityException | IOException e) {
                emitDecryptionFailureTelemetryIfNeeded(keyType, e);
            }

        }

        Logger.info(
                TAG + methodName,
                "Tried all decryption keys and decryption still fails. Throw an exception.");

        throw new GeneralSecurityException(ErrorStrings.DECRYPTION_FAILED);
    }

    // This is to make sure that Decryption error failure is only emitted once - to avoid bombarding ARIA.
    private void emitDecryptionFailureTelemetryIfNeeded(@NonNull final KeyType keyType,
                                                        @NonNull final Exception exception) {
        final String methodName = ":emitDecryptionFailureTelemetryIfNeeded";
        final SharedPreferences sharedPreferences = PreferenceManager.
                getDefaultSharedPreferences(mContext);
        final String previousActiveBroker = sharedPreferences.getString(
                CURRENT_ACTIVE_BROKER,
                ""
        );

        if (!previousActiveBroker.equalsIgnoreCase(mContext.getPackageName())) {
            final String message = "Decryption failed with key: " + keyType.name()
                    + " Active broker: " + mContext.getPackageName()
                    + " Exception: " + exception.toString();

            Logger.info(TAG + methodName, message);

            if (mTelemetryCallback != null) {
                mTelemetryCallback.logEvent(mContext,
                        AuthenticationConstants.TelemetryEvents.DECRYPTION_ERROR,
                        true,
                        message);
            }

            sharedPreferences.edit().putString(CURRENT_ACTIVE_BROKER, mContext.getPackageName()).apply();
        }
    }

    /**
     * Determine type of encryption performed on the given data blob.
     * NOTE: If it cannot verify the keyVersion, it will assume that this data is not encrypted.
     */
    public static EncryptionType getEncryptionType(@NonNull final String data) throws UnsupportedEncodingException {
        final String methodName = ":getEncryptionType";

        final byte[] bytes;
        try {
            bytes = getByteArrayFromEncryptedBlob(data);
        } catch (Exception e) {
            Logger.error(TAG + methodName, "This data is not an encrypted blob. Treat as unencrypted data.", e);
            return EncryptionType.UNENCRYPTED;
        }

        try {
            final String keyVersion = new String(
                    bytes,
                    0,
                    KEY_VERSION_BLOB_LENGTH,
                    AuthenticationConstants.ENCODING_UTF8
            );

            if (VERSION_USER_DEFINED.equalsIgnoreCase(keyVersion)) {
                return EncryptionType.USER_DEFINED;
            } else if (VERSION_ANDROID_KEY_STORE.equalsIgnoreCase(keyVersion)) {
                return EncryptionType.ANDROID_KEY_STORE;
            }
        } catch (UnsupportedEncodingException e) {
            Logger.error(TAG + methodName, "Failed to extract keyVersion.", e);
            throw e;
        }

        return EncryptionType.UNENCRYPTED;
    }

    private static byte[] getByteArrayFromEncryptedBlob(@NonNull final String encryptedBlob) {
        int encodeVersionLength = encryptedBlob.charAt(0) - 'a';
        validateEncodeVersion(encryptedBlob, encodeVersionLength);

        return Base64.decode(
                encryptedBlob.substring(1 + encodeVersionLength),
                Base64.DEFAULT
        );
    }

    @NonNull
    private String decryptWithSecretKey(@NonNull final String encryptedBlob,
                                        @NonNull final EncryptionKey encryptionKey)
            throws GeneralSecurityException, IOException {
        final SecretKey secretKey = encryptionKey.getKey();
        final SecretKey hmacKey = encryptionKey.getHMACKey();

        final byte[] bytes = getByteArrayFromEncryptedBlob(encryptedBlob);

        // byte input array: encryptedData-iv-macDigest
        final int ivIndex = bytes.length - DATA_KEY_LENGTH - HMAC_LENGTH;
        final int macIndex = bytes.length - HMAC_LENGTH;
        final int encryptedLength = ivIndex - KEY_VERSION_BLOB_LENGTH;

        if (ivIndex < 0 || macIndex < 0 || encryptedLength < 0) {
            throw new IOException("Invalid byte array input for decryption.");
        }

        // Calculate digest again and compare to the appended value
        // incoming message: version+encryptedData+IV+Digest
        // Digest of EncryptedData+IV excluding key Version and digest
        final Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        final Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(hmacKey);
        mac.update(bytes, 0, macIndex);
        final byte[] macDigest = mac.doFinal();

        // Compare digest of input message and calculated digest
        assertHMac(bytes, macIndex, bytes.length, macDigest);

        // Get IV related bytes from the end and set to decrypt mode with
        // that IV.
        // It is using same cipher for different version since version# change
        // will mean upgrade to AndroidKeyStore and new Key.
        cipher.init(
                Cipher.DECRYPT_MODE,
                secretKey,
                new IvParameterSpec(bytes, ivIndex, DATA_KEY_LENGTH)
        );

        // Decrypt data bytes from 0 to ivindex
        final String decrypted = new String(
                cipher.doFinal(
                        bytes,
                        KEY_VERSION_BLOB_LENGTH,
                        encryptedLength
                ),
                AuthenticationConstants.ENCODING_UTF8
        );

        return decrypted;
    }

    private static void validateEncodeVersion(String encryptedBlob, int encodeVersionLength) {
        if (encodeVersionLength <= 0) {
            throw new IllegalArgumentException(
                    String.format(
                            "Encode version length: '%s' is not valid, it must be greater of equal to 0",
                            encodeVersionLength
                    )
            );
        }

        if (!encryptedBlob.substring(1, 1 + encodeVersionLength).equals(ENCODE_VERSION)) {
            throw new IllegalArgumentException(
                    String.format(
                            "Unsupported encode version received. Encode version supported is: '%s'",
                            ENCODE_VERSION
                    )
            );
        }
    }

    private char getEncodeVersionLengthPrefix() {
        return (char) ('a' + ENCODE_VERSION.length());
    }

    private void assertHMac(final byte[] digest, final int start, final int end, final byte[] calculated)
            throws DigestException {
        if (calculated.length != (end - start)) { //NOPMD
            throw new IllegalArgumentException("Unexpected HMAC length");
        }

        byte result = 0;
        // It does not fail fast on the first not equal byte to protect against
        // timing attack.
        for (int i = start; i < end; i++) {
            result |= calculated[i - start] ^ digest[i];
        }

        if (result != 0) {
            throw new DigestException();
        }
    }

    /**
     * Generate a new set of keystore key.
     * This will also invalidate key cache.
     */
    public synchronized SecretKey generateKeyStoreEncryptedKey()
            throws GeneralSecurityException, IOException {
        final String methodName = ":generateKeyStoreEncryptedKey";
        final SecretKey key = mKeystoreEncryptedKeyManager.createKeyAndSave();
        Logger.info(TAG + methodName, "New keystore-encrypted key is generated.");
        invalidateCachedKeys();
        return key;
    }

    /**
     * Saves the given keystore-encrypted to storage.
     * This will also invalidate key cache.
     */
    public synchronized void saveKeyStoreEncryptedKey(@NonNull final SecretKey secretKey)
            throws GeneralSecurityException, IOException {
        final String methodName = ":saveKeyStoreEncryptedKey";
        mKeystoreEncryptedKeyManager.saveKey(secretKey);
        Logger.info(TAG + methodName, "New keystore-encrypted key is saved.");
        invalidateCachedKeys();
    }

    /**
     * Deletes existing keystore-encrypted from storage.
     * This will also invalidate key cache.
     */
    public synchronized void deleteKeyStoreEncryptedKey() {
        final String methodName = ":deleteKeyStoreEncryptedKey";
        mKeystoreEncryptedKeyManager.deleteKeyFile();
        Logger.info(TAG + methodName, "Existing keystore-encrypted key is deleted.");
        invalidateCachedKeys();
    }

    /**
     * Loads a keystore-encrypted key from storage.
     */
    synchronized SecretKey loadKeyStoreEncryptedKey() throws GeneralSecurityException, IOException {
        return mKeystoreEncryptedKeyManager.loadKey();
    }

    public static String serializeSecretKey(@NonNull final SecretKey key) {
        return Base64.encodeToString(key.getEncoded(), Base64.DEFAULT);
    }

    public static SecretKey deserializeSecretKey(@NonNull final String serializedKey) {
        return getSecretKeyFromRawByteArray(Base64.decode(serializedKey, Base64.DEFAULT));
    }

    static SecretKey getSecretKeyFromRawByteArray(final byte[] rawBytes) {
        if (rawBytes == null) {
            return null;
        }

        return new SecretKeySpec(rawBytes, BaseEncryptionManager.KEYSPEC_ALGORITHM);
    }

    private synchronized void invalidateCachedKeys() {
        mEncryptionKey = null;
        mDecryptionKeys.clear();
    }
}