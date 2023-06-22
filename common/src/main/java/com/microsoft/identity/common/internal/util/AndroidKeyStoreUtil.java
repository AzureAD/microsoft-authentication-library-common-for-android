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
package com.microsoft.identity.common.internal.util;

import android.os.Build;

import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.util.ported.DateUtilities;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Locale;

import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.NonNull;

import static com.microsoft.identity.common.java.util.ported.DateUtilities.LOCALE_CHANGE_LOCK;
import static com.microsoft.identity.common.java.util.ported.DateUtilities.isLocaleCalendarNonGregorian;
import static com.microsoft.identity.common.java.exception.ClientException.ANDROID_KEYSTORE_UNAVAILABLE;
import static com.microsoft.identity.common.java.exception.ClientException.CERTIFICATE_LOAD_FAILURE;
import static com.microsoft.identity.common.java.exception.ClientException.INVALID_ALG_PARAMETER;
import static com.microsoft.identity.common.java.exception.ClientException.INVALID_BLOCK_SIZE;
import static com.microsoft.identity.common.java.exception.ClientException.INVALID_KEY;
import static com.microsoft.identity.common.java.exception.ClientException.INVALID_KEY_MISSING;
import static com.microsoft.identity.common.java.exception.ClientException.IO_ERROR;
import static com.microsoft.identity.common.java.exception.ClientException.NO_SUCH_ALGORITHM;
import static com.microsoft.identity.common.java.exception.ClientException.NO_SUCH_PADDING;
import static com.microsoft.identity.common.java.exception.ClientException.NO_SUCH_PROVIDER;

/**
 * Utility class for Key operations in Android.
 */
public class AndroidKeyStoreUtil {
    private static final String TAG = AndroidKeyStoreUtil.class.getSimpleName();

    /**
     * KeyStore type for Android.
     * {@see https://developer.android.com/training/articles/keystore#UsingAndroidKeyStore}
     */
    private static final String ANDROID_KEY_STORE_TYPE = "AndroidKeyStore";

    private AndroidKeyStoreUtil() {
    }

    private static KeyStore mKeyStore;

    private static synchronized KeyStore getKeyStore()
            throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
        if (mKeyStore == null){
            mKeyStore = KeyStore.getInstance(ANDROID_KEY_STORE_TYPE);
            mKeyStore.load(null);
        }
        return mKeyStore;
    }

    /**
     * Generates a {@link KeyPair} with the given algorithm and {@link AlgorithmParameterSpec}
     * inside a {@link KeyStore} of the given type.
     *
     * @param algorithm     algorithm of the key to be generated (e.g. "RSA")
     * @param algorithmSpec {@link AlgorithmParameterSpec} of the key to be generated.
     * @return the generated {@link KeyPair}
     */
    @NonNull
    public static synchronized KeyPair generateKeyPair(
            @NonNull final String algorithm,
            @NonNull final AlgorithmParameterSpec algorithmSpec) throws ClientException {
        final String methodTag = TAG + ":generateKeyPair";

        synchronized (isLocaleCalendarNonGregorian(Locale.getDefault()) ? LOCALE_CHANGE_LOCK : new Object()) {
            final Exception exception;
            final String errCode;

            // Due to the following bug in lower API versions of keystore, locale workarounds may
            // need to be applied
            // https://issuetracker.google.com/issues/37095309
            final Locale currentLocale = Locale.getDefault();
            applyKeyStoreLocaleWorkarounds(currentLocale);

            try {
                Logger.info(methodTag, "Generating KeyPair from KeyStore");

                // Generate a key with the given algorithm spec
                final KeyPairGenerator generator = KeyPairGenerator.getInstance(algorithm, ANDROID_KEY_STORE_TYPE);
                generator.initialize(algorithmSpec);

                final KeyPair keyPair = generator.generateKeyPair();
                if (keyPair == null) {
                    Logger.error(methodTag, "Failed to generate a keypair. " +
                            "The way we're generating it might be incorrect.", null);
                    throw new ClientException(INVALID_KEY, "Failed to generate a keypair");
                }
                return keyPair;
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
                errCode = ANDROID_KEYSTORE_UNAVAILABLE;
                exception = e;
            } catch (final NoSuchAlgorithmException e) {
                errCode = NO_SUCH_ALGORITHM;
                exception = e;
            } catch (final InvalidAlgorithmParameterException e) {
                errCode = INVALID_ALG_PARAMETER;
                exception = e;
            } catch (final NoSuchProviderException e) {
                errCode = NO_SUCH_PROVIDER;
                exception = e;
            } finally {
                // Reset to our default locale after generating keys
                Locale.setDefault(currentLocale);
            }

            final ClientException clientException = new ClientException(
                    errCode,
                    exception.getMessage(),
                    exception
            );

            Logger.error(
                    methodTag,
                    errCode,
                    exception
            );

            throw clientException;
        }
    }

    /**
     * Check if key of the given alias can be loaded in AndroidKeyStore.
     *
     * @return true if it does, false otherwise.
     */
    public static synchronized boolean canLoadKey(@NonNull final String keyAlias) {
        final String methodTag = TAG + ":hasKey";
        try {
            return getKeyStore().containsAlias(keyAlias);
        } catch (final KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException e) {
            Logger.error(methodTag, "Failed to check keystore key", e);
            return false;
        }
    }

    /**
     * Read KeyPair from AndroidKeyStore.
     *
     * @return KeyPair. Null if there isn't any.
     */
    @Nullable
    public static synchronized KeyPair readKey(@NonNull final String keyAlias)
            throws ClientException {
        final String methodTag = TAG + ":readKeyPair";
        Logger.verbose(methodTag, "Reading Key from KeyStore");

        final Exception exception;
        final String errCode;
        try {
            final KeyStore keyStore = getKeyStore();

            // We intentionally check the private key first, due to crash stacks hit in the wild
            // when checking for the public certificate when it does not exist.
            // `KeyStore exception android.os.ServiceSpecificException: (code 7)`
            // https://stackoverflow.com/questions/52024752/android-9-keystore-exception-android-os-servicespecificexception
            final Key privateKey = keyStore.getKey(keyAlias, null);
            if (privateKey == null) {
                Logger.verbose(methodTag, "Private key entry doesn't exist.");
                return null;
            }

            final Certificate cert = keyStore.getCertificate(keyAlias);
            if (cert == null) {
                Logger.verbose(methodTag, "Public key entry doesn't exist.");
                return null;
            }

            Logger.verbose(methodTag, "Key read from KeyStore");
            return new KeyPair(cert.getPublicKey(), (PrivateKey) privateKey);
        } catch (final RuntimeException e) {
            // There is an issue in android keystore that resets keystore
            // Issue 61989:  AndroidKeyStore deleted after changing screen lock type
            // https://code.google.com/p/android/issues/detail?id=61989
            // in this case getEntry throws
            // java.lang.RuntimeException: error:0D07207B:asn1 encoding routines:ASN1_get_object:header too long
            // handle it as regular KeyStoreException
            errCode = ANDROID_KEYSTORE_UNAVAILABLE;
            exception = e;
        } catch (final IOException e) {
            errCode = IO_ERROR;
            exception = e;
        } catch (final CertificateException e) {
            errCode = CERTIFICATE_LOAD_FAILURE;
            exception = e;
        } catch (final NoSuchAlgorithmException e) {
            errCode = NO_SUCH_ALGORITHM;
            exception = e;
        } catch (final KeyStoreException e) {
            errCode = ANDROID_KEYSTORE_UNAVAILABLE;
            exception = e;
        } catch (final UnrecoverableKeyException e) {
            errCode = INVALID_KEY_MISSING;
            exception = e;
        }

        final ClientException clientException = new ClientException(
                errCode,
                exception.getMessage(),
                exception
        );

        Logger.error(
                methodTag,
                errCode,
                exception
        );

        throw clientException;
    }

    /**
     * See: https://issuetracker.google.com/issues/37095309
     */
    public static synchronized void applyKeyStoreLocaleWorkarounds(@NonNull final Locale currentLocale) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M
                && DateUtilities.isLocaleCalendarNonGregorian(currentLocale)) {
            Locale.setDefault(Locale.ENGLISH);
        }
    }

    /**
     * Deletes the entry identified by the given alias from a given keystore.
     *
     * @param aliasOfKeyToDelete alias for the key to be deleted.
     */
    public static synchronized void deleteKey(
            @NonNull final String aliasOfKeyToDelete)
            throws ClientException {
        final String methodTag = TAG + ":deleteKeyFromKeyStore";

        final Exception exception;
        final String errCode;
        try {
            final KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE_TYPE);
            keyStore.load(null);
            keyStore.deleteEntry(aliasOfKeyToDelete);
            return;
        } catch (final KeyStoreException e) {
            errCode = ANDROID_KEYSTORE_UNAVAILABLE;
            exception = e;
        } catch (final CertificateException e) {
            errCode = CERTIFICATE_LOAD_FAILURE;
            exception = e;
        } catch (final NoSuchAlgorithmException e) {
            errCode = NO_SUCH_ALGORITHM;
            exception = e;
        } catch (final IOException e) {
            errCode = IO_ERROR;
            exception = e;
        }

        final ClientException clientException = new ClientException(
                errCode,
                exception.getMessage(),
                exception
        );

        Logger.error(
                methodTag,
                errCode,
                exception
        );

        throw clientException;
    }

    /**
     * Wrap (encrypt) a given secret key.
     *
     * @param key           key to be wrapped.
     * @param keyToWrap     the key used to wrap.
     * @param wrapAlgorithm the algorithm used to wrap the key.
     * @return the wrapped key data blob.
     */
    public static byte[] wrap(@NonNull final SecretKey key,
                              @NonNull final KeyPair keyToWrap,
                              @NonNull final String wrapAlgorithm)
            throws ClientException {
        final String methodTag = TAG + ":wrap";

        final Exception exception;
        final String errCode;
        try {
            Logger.verbose(methodTag, "Wrap secret key with a KeyPair.");
            final Cipher wrapCipher = Cipher.getInstance(wrapAlgorithm);
            wrapCipher.init(Cipher.WRAP_MODE, keyToWrap.getPublic());
            return wrapCipher.wrap(key);
        } catch (final NoSuchPaddingException e) {
            errCode = NO_SUCH_PADDING;
            exception = e;
        } catch (final NoSuchAlgorithmException e) {
            errCode = NO_SUCH_ALGORITHM;
            exception = e;
        } catch (final InvalidKeyException e) {
            errCode = INVALID_KEY;
            exception = e;
        } catch (final IllegalBlockSizeException e) {
            errCode = INVALID_BLOCK_SIZE;
            exception = e;
        }

        final ClientException clientException = new ClientException(
                errCode,
                exception.getMessage(),
                exception
        );

        Logger.error(
                methodTag,
                errCode,
                exception
        );

        throw clientException;
    }

    /**
     * Unwrap (decrypt) a previously wrapped key.
     *
     * @param wrappedKeyBlob       the data blob containing the key to be unwrapped.
     * @param wrappedKeyAlgorithm  the algorithm of the wrapped key.
     * @param keyPairForUnwrapping the key to be used to unwrap.
     * @param wrapAlgorithm        the algorithm used to wrap the key.
     * @return the unwrapped key.
     */
    public static SecretKey unwrap(@NonNull final byte[] wrappedKeyBlob,
                                   @NonNull final String wrappedKeyAlgorithm,
                                   @NonNull final KeyPair keyPairForUnwrapping,
                                   @NonNull final String wrapAlgorithm) throws ClientException {
        final String methodTag = TAG + ":unwrap";
        final Exception exception;
        final String errCode;
        try {
            final Cipher wrapCipher = Cipher.getInstance(wrapAlgorithm);
            wrapCipher.init(Cipher.UNWRAP_MODE, keyPairForUnwrapping.getPrivate());
            return (SecretKey) wrapCipher.unwrap(wrappedKeyBlob, wrappedKeyAlgorithm, Cipher.SECRET_KEY);
        } catch (final IllegalArgumentException e) {
            // There is issue with Android KeyStore when lock screen type is changed which could
            // potentially wipe out keystore.
            // Here are the two top exceptions that could be thrown due to the above issue:
            // 1) Caused by: java.security.InvalidKeyException: javax.crypto.BadPaddingException:
            //    error:0407106B:rsa routines:RSA_padding_check_PKCS1_type_2:block type is not 02
            // 2) Caused by: java.lang.IllegalArgumentException: key.length == 0
            // Issue 61989: AndroidKeyStore deleted after changing screen lock type
            // https://code.google.com/p/android/issues/detail?id=61989
            // To avoid app crashing from 2), re-throw it as checked exception
            errCode = ANDROID_KEYSTORE_UNAVAILABLE;
            exception = e;
        } catch (final NoSuchPaddingException e) {
            errCode = NO_SUCH_PADDING;
            exception = e;
        } catch (final NoSuchAlgorithmException e) {
            errCode = NO_SUCH_ALGORITHM;
            exception = e;
        } catch (final InvalidKeyException e) {
            errCode = INVALID_KEY;
            exception = e;
        }

        final ClientException clientException = new ClientException(
                errCode,
                exception.getMessage(),
                exception
        );

        Logger.error(
                methodTag,
                errCode,
                exception
        );

        throw clientException;
    }

}
