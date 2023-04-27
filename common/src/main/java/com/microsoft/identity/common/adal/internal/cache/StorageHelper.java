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

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.adal.internal.AuthenticationSettings;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.java.crypto.key.KeyUtil;
import com.microsoft.identity.common.java.exception.ErrorStrings;
import com.microsoft.identity.common.java.util.ported.DateUtilities;
import com.microsoft.identity.common.internal.util.ProcessUtil;
import com.microsoft.identity.common.java.telemetry.ITelemetryCallback;
import com.microsoft.identity.common.logging.Logger;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.DigestException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.spec.AlgorithmParameterSpec;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.x500.X500Principal;

import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_PACKAGE_NAME;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.BROKER_HOST_APP_PACKAGE_NAME;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME;
import static com.microsoft.identity.common.java.crypto.key.KeyUtil.getKeyThumbPrint;
import static com.microsoft.identity.common.java.crypto.key.KeyUtil.getKeyThumbPrintFromHmacKey;
import static com.microsoft.identity.common.java.util.ported.DateUtilities.LOCALE_CHANGE_LOCK;
import static com.microsoft.identity.common.java.util.ported.DateUtilities.isLocaleCalendarNonGregorian;

import net.jcip.annotations.GuardedBy;

@Deprecated
public class StorageHelper implements IStorageHelper {
    private static final String TAG = "StorageHelper";
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    public static final AtomicReference<String> LAST_KNOWN_THUMBPRINT = new AtomicReference<>("");
    public static final AtomicBoolean FIRST_TIME = new AtomicBoolean(false);

    /**
     * A flag to turn on/off keystore encryption on Broker apps.
     */
    public static final boolean sShouldEncryptWithKeyStoreKey = false;

    /**
     * HMac key hashing algorithm.
     */
    private static final String HMAC_KEY_HASH_ALGORITHM = "SHA256";

    /**
     * Cert alias persisting the keypair in AndroidKeyStore.
     */
    private static final String KEY_STORE_CERT_ALIAS = "AdalKey";

    /**
     * Name of the file contains the symmetric key used for encryption/decryption.
     */
    private static final String ADALKS = "adalks";

    /**
     * Key spec algorithm.
     */
    private static final String KEYSPEC_ALGORITHM = "AES";

    /**
     * Algorithm for key wrapping.
     */
    private static final String WRAP_ALGORITHM = "RSA/ECB/PKCS1Padding";

    /**
     * AES is 16 bytes (128 bits), thus PKCS#5 padding should not work, but in
     * Java AES/CBC/PKCS5Padding is default(!) algorithm name, thus PKCS5 here
     * probably doing PKCS7. We decide to go with Java default string.
     */
    private static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private static final String CURRENT_ACTIVE_BROKER = "current_active_broker";

    private static final int KEY_SIZE = 256;

    /**
     * IV Key length for AES-128.
     */
    public static final int DATA_KEY_LENGTH = 16;

    /**
     * 256 bits output for signing message.
     */
    public static final int HMAC_LENGTH = 32;

    /**
     * Indicate that token item is encrypted with the key persisted in AndroidKeyStore.
     */
    public static final String VERSION_ANDROID_KEY_STORE = "A001";

    /**
     * Indicate that the token item is encrypted with the user provided key.
     */
    public static final String VERSION_USER_DEFINED = "U001";

    private static final int KEY_VERSION_BLOB_LENGTH = 4;

    /**
     * To keep track of encoding version and related flags.
     */
    private static final String ENCODE_VERSION = "E1";

    private static final int KEY_FILE_SIZE = 1024;

    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";

    /**
     * Type of Secret key to be used.
     */
    public enum KeyType {
        LEGACY_AUTHENTICATOR_APP_KEY,
        LEGACY_COMPANY_PORTAL_KEY,
        ADAL_USER_DEFINED_KEY,
        KEYSTORE_ENCRYPTED_KEY
    }

    /**
     * Encryption type of a given blob.
     */
    public enum EncryptionType {
        USER_DEFINED,
        ANDROID_KEY_STORE,
        UNENCRYPTED
    }

    private final Context mContext;
    private final SecureRandom mRandom;
    private ITelemetryCallback mTelemetryCallback;

    /**
     * Public and private keys that are generated in AndroidKeyStore.
     */
    @GuardedBy("this")
    private KeyPair mKeyPair;
    private String mBlobVersion;
    private SecretKey mEncryptionKey = null;
    private SecretKey mEncryptionHMACKey = null;
    private SecretKey mCachedKeyStoreEncryptedKey = null;

    /**
     * Constructor for {@link StorageHelper}.
     *
     * @param context The {@link Context} to create {@link StorageHelper}.
     */
    public StorageHelper(@NonNull final Context context) {
        this(context, null);
    }

    /**
     * Constructor for {@link StorageHelper}.
     *
     * @param context The {@link Context} to create {@link StorageHelper}.
     *                TODO: Remove this suppression: https://android-developers.blogspot.com/2013/08/some-securerandom-thoughts.html
     */
    @SuppressLint("TrulyRandom")
    public StorageHelper(@NonNull final Context context, @Nullable final ITelemetryCallback telemetryCallback) {
        mContext = context.getApplicationContext();
        mRandom = new SecureRandom();
        mTelemetryCallback = telemetryCallback;
    }

    // Exposed to be overridden by mock tests.
    protected String getPackageName() {
        return mContext.getPackageName();
    }

    // Exposed to be overridden by mock tests.
    protected boolean isBrokerProcess() {
        return ProcessUtil.isBrokerProcess(mContext);
    }

    @Override
    public String encrypt(final String clearText)
            throws GeneralSecurityException, IOException {
        final String methodTag = TAG + ":encrypt";

        if (StringExtensions.isNullOrBlank(clearText)) {
            throw new IllegalArgumentException("Input is empty or null");
        }

        Logger.verbose(methodTag, "Starting encryption");

        // load key for encryption if not loaded
        mEncryptionKey = loadSecretKeyForEncryption();
        mEncryptionHMACKey = getHMacKey(mEncryptionKey);
        logIfKeyHasChanged(mEncryptionHMACKey);

        Logger.verbose(methodTag, "Encrypt version:" + mBlobVersion);
        final byte[] blobVersion = mBlobVersion.getBytes(AuthenticationConstants.CHARSET_UTF8);
        final byte[] bytes = clearText.getBytes(AuthenticationConstants.CHARSET_UTF8);

        // IV: Initialization vector that is needed to start CBC
        final byte[] iv = new byte[DATA_KEY_LENGTH];
        mRandom.nextBytes(iv);
        final IvParameterSpec ivSpec = new IvParameterSpec(iv);

        // Set to encrypt mode
        final Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        final Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, mEncryptionKey, ivSpec);

        final byte[] encrypted = cipher.doFinal(bytes);

        // Mac output to sign encryptedData+IV. Keyversion is not included
        // in the digest. It defines what to use for Mac Key.
        mac.init(mEncryptionHMACKey);
        mac.update(blobVersion);
        mac.update(encrypted);
        mac.update(iv);
        final byte[] macDigest = mac.doFinal();

        // Init array to store blobVersion, encrypted data, iv, macdigest
        final byte[] blobVerAndEncryptedDataAndIVAndMacDigest = new byte[blobVersion.length
                + encrypted.length + iv.length + macDigest.length];
        System.arraycopy(blobVersion, 0, blobVerAndEncryptedDataAndIVAndMacDigest, 0,
                blobVersion.length);
        System.arraycopy(encrypted, 0, blobVerAndEncryptedDataAndIVAndMacDigest,
                blobVersion.length, encrypted.length);
        System.arraycopy(iv, 0, blobVerAndEncryptedDataAndIVAndMacDigest, blobVersion.length
                + encrypted.length, iv.length);
        System.arraycopy(macDigest, 0, blobVerAndEncryptedDataAndIVAndMacDigest, blobVersion.length
                + encrypted.length + iv.length, macDigest.length);

        final String encryptedText = new String(Base64.encode(blobVerAndEncryptedDataAndIVAndMacDigest,
                Base64.NO_WRAP), AuthenticationConstants.CHARSET_UTF8);
        Logger.verbose(methodTag, "Finished encryption");

        return getEncodeVersionLengthPrefix() + ENCODE_VERSION + encryptedText;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public String testThumbprint() throws IOException, GeneralSecurityException {
        final SecretKey secretKey = loadSecretKeyForEncryption();
        return getKeyThumbPrint(secretKey);
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public boolean testKeyChange() throws IOException, GeneralSecurityException {
        final SecretKey secretKey = loadSecretKeyForEncryption();
        return logIfKeyHasChanged(getHMacKey(secretKey));
    }

    @Override
    public String decrypt(final String encryptedBlob) throws GeneralSecurityException, IOException {
        final String methodTag = TAG + ":decrypt";
        Logger.verbose(methodTag, "Starting decryption");

        if (StringExtensions.isNullOrBlank(encryptedBlob)) {
            throw new IllegalArgumentException("Input is empty or null");
        }

        if (getEncryptionType(encryptedBlob) == EncryptionType.UNENCRYPTED) {
            Logger.warn(methodTag, "This string is not encrypted. Finished decryption.");
            return encryptedBlob;
        }

        // Try to read keystore key - to verify how often this is invoked before the migration is done.
        // TODO: remove this whole try-catch clause once the experiment is done.
        if (mTelemetryCallback != null) {
            try {
                final SecretKey key = loadSecretKey(KeyType.KEYSTORE_ENCRYPTED_KEY);
                if (key == null) {
                    mTelemetryCallback.logEvent(methodTag, false, "KEY_DECRYPTION_KEYSTORE_KEY_NOT_INITIALIZED");
                }
            } catch (final Exception e) {
                // Best effort.
                mTelemetryCallback.logEvent(methodTag, false, "KEY_DECRYPTION_KEYSTORE_KEY_FAILED_TO_LOAD");
            }
        }

        final String packageName = getPackageName();
        final List<KeyType> keysForDecryptionType = getKeysForDecryptionType(encryptedBlob, packageName);

        final byte[] bytes = getByteArrayFromEncryptedBlob(encryptedBlob);
        for (final KeyType keyType : keysForDecryptionType) {
            try {
                final SecretKey secretKey = loadSecretKey(keyType);
                if (secretKey == null) {
                    continue;
                }

                String result = decryptWithSecretKey(bytes, secretKey);
                Logger.verbose(methodTag, "Finished decryption with keyType:" + keyType.name());
                return result;
            } catch (GeneralSecurityException | IOException e) {
                emitDecryptionFailureTelemetryIfNeeded(keyType, e);
            }
        }

        Logger.info(
                methodTag,
                "Tried all decryption keys and decryption still fails. Throw an exception.");

        throw new GeneralSecurityException(ErrorStrings.DECRYPTION_FAILED);
    }

    // This is to make sure that Decryption error failure is only emitted once - to avoid bombarding ARIA.
    private void emitDecryptionFailureTelemetryIfNeeded(@NonNull final KeyType keyType,
                                                        @NonNull final Exception exception) {
        final String methodTag = TAG + ":emitDecryptionFailureTelemetryIfNeeded";
        final SharedPreferences sharedPreferences = PreferenceManager.
                getDefaultSharedPreferences(mContext);
        final String previousActiveBroker = sharedPreferences.getString(
                CURRENT_ACTIVE_BROKER,
                ""
        );
        final String activeBroker = mContext.getPackageName();

        if (!previousActiveBroker.equalsIgnoreCase(activeBroker)) {
            final String message = "Decryption failed with key: " + keyType.name()
                    + " Active broker: " + activeBroker
                    + " Exception: " + exception.toString();

            Logger.info(methodTag, message);

            if (mTelemetryCallback != null) {
                mTelemetryCallback.logEvent(
                        AuthenticationConstants.TelemetryEvents.DECRYPTION_ERROR,
                        true,
                        message);
            }

            sharedPreferences.edit().putString(CURRENT_ACTIVE_BROKER, activeBroker).apply();
        }
    }

    /**
     * Determine type of encryption performed on the given data blob.
     * NOTE: If it cannot verify the keyVersion, it will assume that this data is not encrypted.
     */
    public EncryptionType getEncryptionType(@NonNull final String data) throws UnsupportedEncodingException {

        final byte[] bytes;
        try {
            bytes = getByteArrayFromEncryptedBlob(data);
        } catch (Exception e) {
            return EncryptionType.UNENCRYPTED;
        }

        final String keyVersion = new String(
                bytes,
                0,
                KEY_VERSION_BLOB_LENGTH,
                AuthenticationConstants.CHARSET_UTF8
        );

        if (VERSION_USER_DEFINED.equalsIgnoreCase(keyVersion)) {
            return EncryptionType.USER_DEFINED;
        } else if (VERSION_ANDROID_KEY_STORE.equalsIgnoreCase(keyVersion)) {
            return EncryptionType.ANDROID_KEY_STORE;
        }

        return EncryptionType.UNENCRYPTED;
    }

    private byte[] getByteArrayFromEncryptedBlob(@NonNull final String encryptedBlob) {
        int encodeVersionLength = encryptedBlob.charAt(0) - 'a';
        validateEncodeVersion(encryptedBlob, encodeVersionLength);

        return Base64.decode(
                encryptedBlob.substring(1 + encodeVersionLength),
                Base64.DEFAULT
        );
    }

    /**
     * Get all the key type that could be potential candidates for decryption.
     **/
    @NonNull
    public List<KeyType> getKeysForDecryptionType(@NonNull final String encryptedBlob,
                                                  @NonNull final String packageName) throws IOException {
        List<KeyType> keyTypeList = new ArrayList<>();

        EncryptionType encryptionType = getEncryptionType(encryptedBlob);

        if (encryptionType == EncryptionType.USER_DEFINED) {
            if (isBrokerProcess()) {
                if (COMPANY_PORTAL_APP_PACKAGE_NAME.equalsIgnoreCase(packageName) ||
                        BROKER_HOST_APP_PACKAGE_NAME.equalsIgnoreCase(packageName)) {
                    keyTypeList.add(KeyType.LEGACY_COMPANY_PORTAL_KEY);
                    keyTypeList.add(KeyType.LEGACY_AUTHENTICATOR_APP_KEY);
                } else if (AZURE_AUTHENTICATOR_APP_PACKAGE_NAME.equalsIgnoreCase(packageName)) {
                    keyTypeList.add(KeyType.LEGACY_AUTHENTICATOR_APP_KEY);
                    keyTypeList.add(KeyType.LEGACY_COMPANY_PORTAL_KEY);
                } else {
                    throw new IllegalStateException("Unexpected Broker package name.");
                }
            } else {
                keyTypeList.add(KeyType.ADAL_USER_DEFINED_KEY);
            }
        } else if (encryptionType == EncryptionType.ANDROID_KEY_STORE) {
            keyTypeList.add(KeyType.KEYSTORE_ENCRYPTED_KEY);
        }

        return keyTypeList;
    }

    @NonNull
    private String decryptWithSecretKey(@NonNull final byte[] bytes,
                                        @NonNull final SecretKey secretKey)
            throws GeneralSecurityException, IOException {
        final SecretKey hmacKey = getHMacKey(secretKey);

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

        logIfKeyHasChanged(hmacKey);

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
                AuthenticationConstants.CHARSET_UTF8
        );

        return decrypted;
    }

    private boolean logIfKeyHasChanged(@NonNull final SecretKey hmacKey) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        final String methodTag = TAG + ":logIfKeyHasChanged";
        final String keyThumbPrint = getKeyThumbPrintFromHmacKey(hmacKey);
        if (!LAST_KNOWN_THUMBPRINT.get().equals(keyThumbPrint)) {
            LAST_KNOWN_THUMBPRINT.set(keyThumbPrint);
            if (!FIRST_TIME.compareAndSet(false, true)) {
                Logger.info(methodTag, "Using key with thumbprint that has changed " + keyThumbPrint);
                return true;
            }
        }
        return false;
    }

    private void validateEncodeVersion(String encryptedBlob, int encodeVersionLength) {
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

    @Override
    public synchronized SecretKey loadSecretKeyForEncryption() throws IOException,
            GeneralSecurityException {
        final String methodTag = TAG + ":loadSecretKeyForEncryption";

        // Loading key only once for performance. If API is upgraded, it will
        // restart the device anyway. It will load the correct key for new API.
        if (mEncryptionKey != null && mEncryptionHMACKey != null) {
            return mEncryptionKey;
        }

        // The current app runtime is the broker; load its secret key.
        if (!sShouldEncryptWithKeyStoreKey && isBrokerProcess()) {
            // Try to read keystore key - to verify how often this is invoked before the migration is done.
            // TODO: remove this whole try-catch clause once the experiment is done.
            if (mTelemetryCallback != null) {
                try {
                    final SecretKey key = loadSecretKey(KeyType.KEYSTORE_ENCRYPTED_KEY);
                    if (key == null) {
                        mTelemetryCallback.logEvent(methodTag, false, "KEY_ENCRYPTION_KEYSTORE_KEY_NOT_INITIALIZED");
                    }
                } catch (final Exception e) {
                    // Best effort.
                    mTelemetryCallback.logEvent(methodTag, false, "KEY_ENCRYPTION_KEYSTORE_KEY_FAILED_TO_LOAD");
                }
            }

            setBlobVersion(VERSION_USER_DEFINED);
            if (AZURE_AUTHENTICATOR_APP_PACKAGE_NAME.equalsIgnoreCase(getPackageName())) {
                return loadSecretKey(KeyType.LEGACY_AUTHENTICATOR_APP_KEY);
            } else {
                return loadSecretKey(KeyType.LEGACY_COMPANY_PORTAL_KEY);
            }
        }

        // Try to get user defined key (ADAL/MSAL).
        if (AuthenticationSettings.INSTANCE.getSecretKeyData() != null) {
            setBlobVersion(VERSION_USER_DEFINED);
            return loadSecretKey(KeyType.ADAL_USER_DEFINED_KEY);
        }

        // Try loading existing keystore-encrypted key. If it doesn't exist, create a new one.
        setBlobVersion(VERSION_ANDROID_KEY_STORE);
        try {
            SecretKey key = loadSecretKey(KeyType.KEYSTORE_ENCRYPTED_KEY);
            if (key != null) {
                return key;
            }
        } catch (final IOException | GeneralSecurityException e) {
            // If we fail to load key, proceed and generate a new one.
        }

        Logger.verbose(methodTag, "Keystore-encrypted key does not exist, try to generate new keys.");
        return generateKeyStoreEncryptedKey();
    }

    /**
     * A function for setting mBlobVersion.
     * Exposed for test cases.
     */
    protected void setBlobVersion(@NonNull String blobVersion) {
        mBlobVersion = blobVersion;
    }

    /**
     * Given the key type, load a secret key.
     *
     * @return SecretKey. Null if there isn't any.
     */
    @Nullable
    public SecretKey loadSecretKey(@NonNull final KeyType keyType) throws IOException, GeneralSecurityException {
        final String methodTag = TAG + ":loadSecretKey";

        switch (keyType) {
            case LEGACY_AUTHENTICATOR_APP_KEY:
                return getSecretKey(AuthenticationSettings.INSTANCE.getBrokerSecretKeys().get(AZURE_AUTHENTICATOR_APP_PACKAGE_NAME));

            case LEGACY_COMPANY_PORTAL_KEY:
                return getSecretKey(AuthenticationSettings.INSTANCE.getBrokerSecretKeys().get(COMPANY_PORTAL_APP_PACKAGE_NAME));

            case ADAL_USER_DEFINED_KEY:
                return getSecretKey(AuthenticationSettings.INSTANCE.getSecretKeyData());

            case KEYSTORE_ENCRYPTED_KEY:
                return loadKeyStoreEncryptedKey();
        }

        Logger.verbose(methodTag, "Unknown KeyType. This code should never be reached.");
        throw new GeneralSecurityException(ErrorStrings.UNKNOWN_ERROR);
    }

    /**
     * Encrypt the given unencrypted symmetric key with Keystore key and save to storage.
     */
    public synchronized void saveKeyStoreEncryptedKey(@NonNull SecretKey unencryptedKey) throws GeneralSecurityException, IOException {
        if (mKeyPair == null) {
            mKeyPair = generateKeyPairFromAndroidKeyStore();
        }

        final byte[] keyWrapped = wrap(unencryptedKey);
        writeKeyData(keyWrapped);
    }

    /**
     * Generate a new keystore-encrypted key and save to storage.
     */
    public synchronized SecretKey generateKeyStoreEncryptedKey() throws GeneralSecurityException, IOException {
        final String methodTag = TAG + ":generateKeyStoreEncryptedKey";
        mCachedKeyStoreEncryptedKey = generateSecretKey();
        saveKeyStoreEncryptedKey(mCachedKeyStoreEncryptedKey);

        logEvent(methodTag,
                AuthenticationConstants.TelemetryEvents.KEY_CREATED,
                false,
                "New key is generated.");

        return mCachedKeyStoreEncryptedKey;
    }

    /**
     * Load the saved keystore-encrypted key. Will only do read operation.
     *
     * @return SecretKey. Null if there isn't any.
     * @throws GeneralSecurityException
     * @throws IOException
     */
    @Nullable
    private synchronized SecretKey loadKeyStoreEncryptedKey()
            throws GeneralSecurityException, IOException {
        final String methodTag = TAG + ":loadKeyStoreEncryptedKey";
        if (mCachedKeyStoreEncryptedKey != null) {
            return mCachedKeyStoreEncryptedKey;
        }

        try {
            mCachedKeyStoreEncryptedKey = getUnwrappedSecretKey();
        } catch (final GeneralSecurityException | IOException e) {
            // Reset KeyPair info so that new request will generate correct KeyPairs.
            // All tokens with previous SecretKey are not possible to decrypt.
            Logger.error(methodTag, ErrorStrings.ANDROIDKEYSTORE_FAILED, e);
            mKeyPair = null;
            mCachedKeyStoreEncryptedKey = null;
            deleteKeyFile();
            resetKeyPairFromAndroidKeyStore();
            throw e;
        }

        return mCachedKeyStoreEncryptedKey;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private synchronized KeyPair generateKeyPairFromAndroidKeyStore()
            throws GeneralSecurityException, IOException {
        final String methodTag = TAG + ":generateKeyPairFromAndroidKeyStore";

        synchronized (isLocaleCalendarNonGregorian(Locale.getDefault()) ? LOCALE_CHANGE_LOCK : new Object()) {
            // Due to the following bug in lower API versions of keystore, locale workarounds may
            // need to be applied
            // https://issuetracker.google.com/issues/37095309
            final Locale currentLocale = Locale.getDefault();
            applyKeyStoreLocaleWorkarounds(currentLocale);

            /*
            !!WARNING!!

            Multiple apps as of Today (1/4/2022) can still share a linux user id, by configuring the sharedUserId attribute in their
            Android Manifest file.  If multiple apps reference the same value for sharedUserId and are signed with the same keys
            they will use the same AndroidKeyStore and may obtain access to the files and shared preferences of other applications
            by invoking createPackageContext.

            Support for sharedUserId is deprecated, however some applications still use this Android capability.
            See: https://developer.android.com/guide/topics/manifest/manifest-element

            To address apps in this scenario we will attempt to load an existing KeyPair instead of immediately generating
            a new key pair.  This will use the same keypair to encrypt the symmetric key generated separately for each
            application using a shared linux user id... and avoid these applications from "stomping"/overwriting
            one another's keypair
             */

            KeyPair existingPair = readKeyPair();
            if (existingPair != null) {
                Logger.verbose(methodTag, "Existing keypair was found.  Returning existing key rather than generating new one.");
                return existingPair;
            }

            try {
                logFlowStart(methodTag, AuthenticationConstants.TelemetryEvents.KEYSTORE_WRITE_START);

                final KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
                keyStore.load(null);

                Logger.verbose(methodTag, "Generate KeyPair from AndroidKeyStore");

                final Calendar start = Calendar.getInstance();
                final Calendar end = Calendar.getInstance();
                final int certValidYears = 100;
                end.add(Calendar.YEAR, certValidYears);

                // self signed cert stored in AndroidKeyStore to asym. encrypt key
                // to a file
                final KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA",
                        ANDROID_KEY_STORE);
                generator.initialize(getKeyPairGeneratorSpec(mContext, start.getTime(), end.getTime()));

                final KeyPair keyPair = generator.generateKeyPair();

                logFlowSuccess(methodTag, AuthenticationConstants.TelemetryEvents.KEYSTORE_WRITE_END, "");
                return keyPair;
            } catch (final GeneralSecurityException | IOException e) {
                logFlowError(methodTag, AuthenticationConstants.TelemetryEvents.KEYSTORE_WRITE_END, e.toString(), e);
                throw e;
            } catch (final IllegalStateException e) {
                // There is an issue with AndroidKeyStore when attempting to generate keypair
                // if user doesn't have pin/passphrase setup for their lock screen.
                // Issue 177459 : AndroidKeyStore KeyPairGenerator fails to generate
                // KeyPair after toggling lock type, even without setting the encryptionRequired
                // flag on the KeyPairGeneratorSpec.
                // https://code.google.com/p/android/issues/detail?id=177459
                // The thrown exception in this case is:
                // java.lang.IllegalStateException: could not generate key in keystore
                // To avoid app crashing, re-throw as checked exception
                logFlowError(methodTag, AuthenticationConstants.TelemetryEvents.KEYSTORE_WRITE_END, e.toString(), e);
                throw new KeyStoreException(e);
            } finally {
                // Reset to our default locale after generating keys
                Locale.setDefault(currentLocale);
            }
        }
    }

    public static synchronized void applyKeyStoreLocaleWorkarounds(@NonNull final Locale currentLocale) {
        // See: https://issuetracker.google.com/issues/37095309
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M
                && DateUtilities.isLocaleCalendarNonGregorian(currentLocale)) {
            Locale.setDefault(Locale.ENGLISH);
        }
    }

    /**
     * Read KeyPair from AndroidKeyStore.
     *
     * @return KeyPair. Null if there isn't any.
     */
    @Nullable
    private synchronized KeyPair readKeyPair()
            throws GeneralSecurityException, IOException {
        final String methodTag = TAG + ":readKeyPair";
        Logger.verbose(methodTag, "Reading Key entry");

        try {
            logFlowStart(methodTag, AuthenticationConstants.TelemetryEvents.KEYSTORE_READ_START);

            final KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);

            final Certificate cert = keyStore.getCertificate(KEY_STORE_CERT_ALIAS);
            final Key privateKey = keyStore.getKey(KEY_STORE_CERT_ALIAS, null);

            if (cert == null || privateKey == null) {
                logFlowSuccess(methodTag, AuthenticationConstants.TelemetryEvents.KEYSTORE_READ_END, "KeyStore is empty.");
                Logger.verbose(methodTag, "Key entry doesn't exist.");
                return null;
            }

            final KeyPair keyPair = new KeyPair(cert.getPublicKey(), (PrivateKey) privateKey);
            logFlowSuccess(methodTag, AuthenticationConstants.TelemetryEvents.KEYSTORE_READ_END, "KeyStore KeyPair is loaded.");
            return keyPair;
        } catch (final GeneralSecurityException | IOException e) {
            logFlowError(methodTag, AuthenticationConstants.TelemetryEvents.KEYSTORE_READ_END, e.toString(), e);
            throw e;
        } catch (final RuntimeException e) {
            // There is an issue in android keystore that resets keystore
            // Issue 61989:  AndroidKeyStore deleted after changing screen lock type
            // https://code.google.com/p/android/issues/detail?id=61989
            // in this case getEntry throws
            // java.lang.RuntimeException: error:0D07207B:asn1 encoding routines:ASN1_get_object:header too long
            // handle it as regular KeyStoreException
            logFlowError(methodTag, AuthenticationConstants.TelemetryEvents.KEYSTORE_READ_END, e.toString(), e);
            throw new KeyStoreException(e);
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @SuppressWarnings("deprecation")
    private AlgorithmParameterSpec getKeyPairGeneratorSpec(final Context context, final Date start, final Date end) {
        final String certInfo = String.format(Locale.ROOT, "CN=%s, OU=%s", KEY_STORE_CERT_ALIAS,
                getPackageName());
        return new android.security.KeyPairGeneratorSpec.Builder(context)
                .setAlias(KEY_STORE_CERT_ALIAS)
                .setSubject(new X500Principal(certInfo))
                .setSerialNumber(BigInteger.ONE)
                .setStartDate(start)
                .setEndDate(end)
                .build();
    }

    private static SecretKey getSecretKey(final byte[] rawBytes) {
        if (rawBytes == null) {
            throw new IllegalArgumentException("rawBytes");
        }

        return new SecretKeySpec(rawBytes, KEYSPEC_ALGORITHM);
    }

    /**
     * Derive HMAC key from given key.
     *
     * @param key SecretKey from which HMAC key has to be derived
     * @return SecretKey
     * @throws NoSuchAlgorithmException
     */
    private SecretKey getHMacKey(final SecretKey key) throws NoSuchAlgorithmException {
        // Some keys may not produce byte[] with getEncoded
        final byte[] encodedKey = key.getEncoded();
        if (encodedKey != null) {
            final MessageDigest digester = MessageDigest.getInstance(HMAC_KEY_HASH_ALGORITHM);
            return new SecretKeySpec(digester.digest(encodedKey), KEYSPEC_ALGORITHM);
        }

        return key;
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
     * generate secretKey to store after wrapping with KeyStore.
     *
     * @return SecretKey.
     * @throws NoSuchAlgorithmException
     */
    protected SecretKey generateSecretKey() throws NoSuchAlgorithmException {
        final KeyGenerator keygen = KeyGenerator.getInstance(KEYSPEC_ALGORITHM);
        keygen.init(KEY_SIZE, mRandom);
        return keygen.generateKey();
    }

    @Nullable
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private synchronized SecretKey getUnwrappedSecretKey()
            throws GeneralSecurityException, IOException {
        final String methodTag = TAG + ":getUnwrappedSecretKey";
        Logger.verbose(methodTag, "Reading SecretKey");

        final SecretKey unwrappedSecretKey;
        final byte[] wrappedSecretKey = readKeyData();
        if (wrappedSecretKey == null) {
            Logger.verbose(methodTag, "Key data is null");
            return null;
        }

        // androidKeyStore can store app specific self signed cert.
        // Asymmetric cryptography is used to protect the session key
        // used for Encryption and HMac
        mKeyPair = readKeyPair();
        if (mKeyPair == null) {
            return null;
        }

        unwrappedSecretKey = unwrap(wrappedSecretKey);
        Logger.verbose(methodTag, "Finished reading SecretKey");
        return unwrappedSecretKey;
    }

    public void deleteKeyFile() {
        final String methodTag = TAG + ":deleteKeyFile";

        final File keyFile = new File(mContext.getDir(getPackageName(),
                Context.MODE_PRIVATE), ADALKS);
        if (keyFile.exists()) {
            Logger.verbose(methodTag, "Delete KeyFile");
            if (!keyFile.delete()) {
                Logger.verbose(methodTag, "Delete KeyFile failed");
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    protected synchronized void resetKeyPairFromAndroidKeyStore() throws KeyStoreException,
            NoSuchAlgorithmException, CertificateException, IOException {
        final KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
        keyStore.load(null);
        keyStore.deleteEntry(KEY_STORE_CERT_ALIAS);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @SuppressLint("GetInstance")
    private synchronized byte[] wrap(final SecretKey key) throws GeneralSecurityException {
        final String methodTag = TAG + ":wrap";

        Logger.verbose(methodTag, "Wrap secret key.");
        final Cipher wrapCipher = Cipher.getInstance(WRAP_ALGORITHM);
        wrapCipher.init(Cipher.WRAP_MODE, mKeyPair.getPublic());
        return wrapCipher.wrap(key);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @SuppressLint("GetInstance")
    private synchronized SecretKey unwrap(final byte[] keyBlob) throws GeneralSecurityException {
        final Cipher wrapCipher = Cipher.getInstance(WRAP_ALGORITHM);
        wrapCipher.init(Cipher.UNWRAP_MODE, mKeyPair.getPrivate());
        try {
            return (SecretKey) wrapCipher.unwrap(keyBlob, KEYSPEC_ALGORITHM, Cipher.SECRET_KEY);
        } catch (final IllegalArgumentException exception) {
            // There is issue with Android KeyStore when lock screen type is changed which could
            // potentially wipe out keystore.
            // Here are the two top exceptions that could be thrown due to the above issue:
            // 1) Caused by: java.security.InvalidKeyException: javax.crypto.BadPaddingException:
            //    error:0407106B:rsa routines:RSA_padding_check_PKCS1_type_2:block type is not 02
            // 2) Caused by: java.lang.IllegalArgumentException: key.length == 0
            // Issue 61989: AndroidKeyStore deleted after changing screen lock type
            // https://code.google.com/p/android/issues/detail?id=61989
            // To avoid app crashing from 2), re-throw it as checked exception
            throw new KeyStoreException(exception);
        }
    }

    private void writeKeyData(final byte[] data) throws IOException {
        final String methodTag = TAG + ":writeKeyData";

        Logger.verbose(methodTag, "Writing key data to a file");
        final File keyFile = new File(mContext.getDir(getPackageName(), Context.MODE_PRIVATE),
                ADALKS);
        final OutputStream out = new FileOutputStream(keyFile);
        try {
            out.write(data);
        } finally {
            out.close();
        }
    }

    @Nullable
    private byte[] readKeyData() throws IOException {
        final String methodTag = TAG + ":readKeyData";

        final File keyFile = new File(mContext.getDir(getPackageName(), Context.MODE_PRIVATE),
                ADALKS);
        if (!keyFile.exists()) {
            return null;
        }

        Logger.verbose(methodTag, "Reading key data from a file");
        final InputStream in = new FileInputStream(keyFile);
        try {
            final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            final byte[] buffer = new byte[KEY_FILE_SIZE];
            int count;
            while ((count = in.read(buffer)) != -1) {
                bytes.write(buffer, 0, count);
            }

            return bytes.toByteArray();
        } finally {
            in.close();
        }
    }

    public String serializeSecretKey(@NonNull final SecretKey key) {
        return Base64.encodeToString(key.getEncoded(), Base64.DEFAULT);
    }

    public SecretKey deserializeSecretKey(@NonNull final String serializedKey) {
        return getSecretKey(Base64.decode(serializedKey, Base64.DEFAULT));
    }

    /**
     * Since Common isn't wired to telemetry yet at the point of implementation (July 18, 2019)
     * We use these functions to pass telemetry events to the calling ad-accounts.
     */
    private void logEvent(@NonNull final String methodTag,
                          @NonNull final String operationName,
                          @NonNull final boolean isFailed,
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
