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

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.security.KeyPairGeneratorSpec;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.common.exception.ErrorStrings;
import com.microsoft.identity.common.internal.logging.Logger;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.security.auth.x500.X500Principal;

/**
 * Create/Save/load keystore-encrypted key.
 */
public class KeystoreEncryptedKeyManager {
    private static final String TAG = KeystoreEncryptedKeyManager.class.getSimpleName();

    /**
     * Size of the key.
     **/
    private static final int KEY_SIZE = 256;

    /**
     * Type of the KeyStore.
     */
    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";

    /**
     * Cert alias persisting the keypair in AndroidKeyStore.
     */
    private static final String KEY_STORE_CERT_ALIAS = "AdalKey";

    /**
     * Algorithm for key wrapping.
     */
    private static final String WRAP_ALGORITHM = "RSA/ECB/PKCS1Padding";

    /**
     * Size of the file contains the keystore-encrypted key.
     */
    private static final int KEY_FILE_SIZE = 1024;

    private Context mContext;
    private String mKeyFileName;

    public KeystoreEncryptedKeyManager(@NonNull final Context context,
                                       @NonNull final String keyFileName) {
        mContext = context;
        mKeyFileName = keyFileName;
    }

    /**
     * Generate a new secret key and save to storage.
     */
    protected SecretKey createKeyAndSave() throws GeneralSecurityException, IOException {
        synchronized (KeystoreEncryptedKeyManager.class) {
            final SecretKey key = generateSecretKey();
            saveKey(key);
            return key;
        }
    }

    /**
     * Encrypt the given unencrypted symmetric key with Keystore key and save to storage.
     */
    protected void saveKey(@NonNull SecretKey unencryptedKey)
            throws GeneralSecurityException, IOException {
        synchronized (KeystoreEncryptedKeyManager.class) {
            KeyPair keyPair = readKeyPair();
            if (keyPair == null) {
                keyPair = generateKeyPairFromAndroidKeyStore();
            }

            final byte[] keyWrapped = wrap(unencryptedKey, keyPair);
            writeKeyData(keyWrapped);
        }
    }

    /**
     * Load the saved keystore-encrypted key. Will only do read operation.
     *
     * @return SecretKey. Null if there isn't any.
     * @throws GeneralSecurityException
     * @throws IOException
     */
    @Nullable
    protected SecretKey loadKey() throws GeneralSecurityException, IOException {
        final String methodName = ":loadKeyStoreEncryptedKey";

        synchronized (KeystoreEncryptedKeyManager.class) {
            try {
                return getUnwrappedSecretKey();
            } catch (final GeneralSecurityException | IOException e) {
                // Reset KeyPair info so that new request will generate correct KeyPairs.
                // All tokens with previous SecretKey are not possible to decrypt.
                Logger.error(TAG + methodName, ErrorStrings.ANDROIDKEYSTORE_FAILED, e);
                deleteKeyFile();
                resetKeyPairFromAndroidKeyStore();
                throw e;
            }
        }
    }

    /**
     * Delete the key file.
     */
    public void deleteKeyFile() {
        final String methodName = ":deleteKeyFile";

        synchronized (KeystoreEncryptedKeyManager.class) {
            final File keyFile = getKeyFile();
            if (keyFile.exists()) {
                Logger.verbose(TAG + methodName, "Delete KeyFile");
                if (!keyFile.delete()) {
                    Logger.verbose(TAG + methodName, "Delete KeyFile failed");
                }
            }
        }
    }

    /**
     * generates a random SecretKey.
     *
     * @return SecretKey.
     * @throws NoSuchAlgorithmException
     */
    public static SecretKey generateSecretKey() throws NoSuchAlgorithmException {
        final KeyGenerator keygen = KeyGenerator.getInstance(BaseEncryptionManager.KEYSPEC_ALGORITHM);
        keygen.init(KEY_SIZE, new SecureRandom());
        return keygen.generateKey();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private synchronized void resetKeyPairFromAndroidKeyStore() throws KeyStoreException,
            NoSuchAlgorithmException, CertificateException, IOException {
        final KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
        keyStore.load(null);
        keyStore.deleteEntry(KEY_STORE_CERT_ALIAS);
    }

    @Nullable
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private synchronized SecretKey getUnwrappedSecretKey()
            throws GeneralSecurityException, IOException {
        final String methodName = ":getUnwrappedSecretKey";
        Logger.verbose(TAG + methodName, "Reading SecretKey");

        final SecretKey unwrappedSecretKey;
        final byte[] wrappedSecretKey = readKeyData();
        if (wrappedSecretKey == null) {
            Logger.verbose(TAG + methodName, "Key data is null");
            return null;
        }

        final KeyPair keyPair = readKeyPair();
        if (keyPair == null) {
            Logger.verbose(TAG + methodName, "Existing KeyPair not found.");
            return null;
        }

        unwrappedSecretKey = unwrap(wrappedSecretKey, keyPair);
        Logger.verbose(TAG + methodName, "Finished reading SecretKey");
        return unwrappedSecretKey;
    }

    /**
     * Generates a new KeyPair in AndroidKeyStore.
     *
     * @return a new KeyPair.
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private synchronized KeyPair generateKeyPairFromAndroidKeyStore()
            throws GeneralSecurityException {
        final String methodName = ":generateKeyPairFromAndroidKeyStore";

        try {
            Logger.verbose(TAG + methodName, "Generate KeyPair from AndroidKeyStore");
            final Calendar start = Calendar.getInstance();
            final Calendar end = Calendar.getInstance();
            final int certValidYears = 100;
            end.add(Calendar.YEAR, certValidYears);

            // self signed cert stored in AndroidKeyStore to asym. encrypt key
            // to a file
            final KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA",
                    ANDROID_KEY_STORE);
            generator.initialize(getKeyPairGeneratorSpec(start.getTime(), end.getTime()));

            final KeyPair keyPair = generator.generateKeyPair();
            return keyPair;
        } catch (final GeneralSecurityException e) {
            Logger.error(TAG + methodName, "Failed to generate keypair", e);
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
            Logger.error(TAG + methodName, "Failed to generate keypair", e);
            throw new KeyStoreException(e);
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
        final String methodName = ":readKeyPair";
        Logger.verbose(TAG + methodName, "Reading Key entry");

        try {
            final KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);

            final Certificate cert = keyStore.getCertificate(KEY_STORE_CERT_ALIAS);
            final Key privateKey = keyStore.getKey(KEY_STORE_CERT_ALIAS, null);

            if (cert == null || privateKey == null) {
                Logger.verbose(TAG + methodName, "Key entry doesn't exist.");
                return null;
            }

            final KeyPair keyPair = new KeyPair(cert.getPublicKey(), (PrivateKey) privateKey);
            return keyPair;
        } catch (final GeneralSecurityException | IOException e) {
            Logger.error(TAG + methodName, "Failed to read keypair", e);
            throw e;
        } catch (final RuntimeException e) {
            // There is an issue in android keystore that resets keystore
            // Issue 61989:  AndroidKeyStore deleted after changing screen lock type
            // https://code.google.com/p/android/issues/detail?id=61989
            // in this case getEntry throws
            // java.lang.RuntimeException: error:0D07207B:asn1 encoding routines:ASN1_get_object:header too long
            // handle it as regular KeyStoreException
            Logger.error(TAG + methodName, "Failed to read keypair", e);
            throw new KeyStoreException(e);
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private AlgorithmParameterSpec getKeyPairGeneratorSpec(@NonNull final Date start,
                                                           @NonNull final Date end) {
        final String certInfo = String.format(Locale.ROOT, "CN=%s, OU=%s", KEY_STORE_CERT_ALIAS,
                mContext.getPackageName());
        return new KeyPairGeneratorSpec.Builder(mContext)
                .setAlias(KEY_STORE_CERT_ALIAS)
                .setSubject(new X500Principal(certInfo))
                .setSerialNumber(BigInteger.ONE)
                .setStartDate(start)
                .setEndDate(end)
                .build();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @SuppressLint("GetInstance")
    private byte[] wrap(@NonNull final SecretKey key,
                        @NonNull final KeyPair keyPair) throws GeneralSecurityException {
        final String methodName = ":wrap";

        Logger.verbose(TAG + methodName, "Wrap secret key.");
        final Cipher wrapCipher = Cipher.getInstance(WRAP_ALGORITHM);
        wrapCipher.init(Cipher.WRAP_MODE, keyPair.getPublic());
        return wrapCipher.wrap(key);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @SuppressLint("GetInstance")
    private SecretKey unwrap(@NonNull final byte[] keyBlob,
                             @NonNull final KeyPair keyPair) throws GeneralSecurityException {
        final Cipher wrapCipher = Cipher.getInstance(WRAP_ALGORITHM);
        wrapCipher.init(Cipher.UNWRAP_MODE, keyPair.getPrivate());
        try {
            return (SecretKey) wrapCipher.unwrap(keyBlob,
                    BaseEncryptionManager.KEYSPEC_ALGORITHM,
                    Cipher.SECRET_KEY);
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

    private void writeKeyData(@NonNull final byte[] data) throws IOException {
        final String methodName = ":writeKeyData";

        Logger.verbose(TAG + methodName, "Writing key data to a file");
        final File keyFile = getKeyFile();
        final OutputStream out = new FileOutputStream(keyFile);
        try {
            out.write(data);
        } finally {
            out.close();
        }
    }

    @Nullable
    private byte[] readKeyData() throws IOException {
        final String methodName = ":readKeyData";

        final File keyFile = getKeyFile();
        if (!keyFile.exists()) {
            return null;
        }

        Logger.verbose(TAG + methodName, "Reading key data from a file");
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

    private File getKeyFile(){
        return new File(mContext.getDir(mContext.getPackageName(), Context.MODE_PRIVATE),
                mKeyFileName);
    }
}
