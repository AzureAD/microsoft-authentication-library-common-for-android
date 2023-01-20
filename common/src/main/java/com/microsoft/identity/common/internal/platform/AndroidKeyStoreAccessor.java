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
package com.microsoft.identity.common.internal.platform;

import android.content.Context;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import androidx.annotation.NonNull;

import com.microsoft.identity.common.components.AndroidPlatformComponentsFactory;
import com.microsoft.identity.common.java.crypto.CryptoSuite;
import com.microsoft.identity.common.java.crypto.IDevicePopManager;
import com.microsoft.identity.common.java.crypto.IKeyAccessor;
import com.microsoft.identity.common.java.crypto.IKeyStoreKeyManager;
import com.microsoft.identity.common.java.crypto.RawKeyAccessor;
import com.microsoft.identity.common.java.crypto.SecureHardwareState;
import com.microsoft.identity.common.java.crypto.SigningAlgorithm;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.interfaces.IPlatformComponents;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.ProviderException;
import java.security.PublicKey;
import java.security.UnrecoverableEntryException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.text.ParseException;
import java.util.UUID;

import javax.crypto.KeyGenerator;

/**
 * This class is a static factory providing access to KeyStore objects.  Since all of the construction
 * in DevicePopManager is package private, and we're really interested in only a few operations, just
 * construct new instances here, and expose an interface that gives us the functionality that we need.
 */
public class AndroidKeyStoreAccessor {
    
    /**
     * The name of the KeyStore to use.
     */
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    public static final Charset UTF8 = Charset.forName("UTF-8");
    private static final int KEY_PURPOSES = KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT | KeyProperties.PURPOSE_SIGN;

    /**
     * For a given alias, construct an accessor for a KeyStore backed entry given that alias.
     *
     * @param context
     * @param alias   The key alias.
     * @param suite   The cipher type of this key.
     * @return a key accessor for the use of that particular key.
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     * @throws IOException
     */
    public static IKeyAccessor forAlias(@NonNull final Context context, @NonNull final String alias, @NonNull final CryptoSuite suite)
            throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, ClientException {
        final IPlatformComponents commonComponents = AndroidPlatformComponentsFactory.createFromContext(context);
        final IDevicePopManager popManager = commonComponents.getDevicePopManager(alias);
        if (suite.cipher() instanceof IDevicePopManager.Cipher) {
            if (!popManager.asymmetricKeyExists()) {
                popManager.generateAsymmetricKey();
            }
            return getKeyAccessor((IDevicePopManager.Cipher) suite.cipher(), suite.signingAlgorithm(), popManager);
        }
        final KeyStore instance = KeyStore.getInstance(ANDROID_KEYSTORE);
        instance.load(null);

        final AndroidDeviceKeyManager<KeyStore.SecretKeyEntry> keyManager = new AndroidDeviceKeyManager<>(instance, alias);
        return new AndroidSecretKeyAccessor(keyManager, suite) {
            @Override
            public byte[] sign(byte[] text) throws ClientException {
                throw new UnsupportedOperationException("This key instance does not support signing");
            }

            @Override
            public boolean verify(byte[] text, byte[] signature) throws ClientException {
                throw new UnsupportedOperationException("This key instance does not support verification");
            }
        };
    }

    private static final IKeyAccessor getKeyAccessor(@NonNull final IDevicePopManager.Cipher cipher,
                                                     @NonNull final SigningAlgorithm signingAlg,
                                                     @NonNull final IDevicePopManager popManager) {
        return new AsymmetricKeyAccessor() {

            @Override
            public IKeyStoreKeyManager<KeyStore.PrivateKeyEntry> getManager() {
                return popManager.getKeyManager();
            }

            @Override
            public String getPublicKey(IDevicePopManager.PublicKeyFormat format) throws ClientException {
                return popManager.getPublicKey(format);
            }

            @Override
            public PublicKey getPublicKey() throws UnrecoverableEntryException, NoSuchAlgorithmException, KeyStoreException {
                return popManager.getPublicKey();
            }

            @Override
            public byte[] encrypt(byte[] plaintext) throws ClientException {
                return popManager.encrypt(cipher, plaintext);
            }

            @Override
            public byte[] decrypt(byte[] ciphertext) throws ClientException {
                return popManager.decrypt(cipher, ciphertext);
            }

            @Override
            public byte[] sign(byte[] text) throws ClientException {
                return popManager.sign(signingAlg, text);
            }

            @Override
            public boolean verify(byte[] text, byte[] signature) throws ClientException {
                return popManager.verify(signingAlg, text, signature);
            }

            @Override
            public byte[] getThumbprint() throws ClientException {
                return popManager.getAsymmetricKeyThumbprint().getBytes(UTF8);
            }

            @Override
            public Certificate[] getCertificateChain() throws ClientException {
                return popManager.getCertificateChain();
            }

            @Override
            public SecureHardwareState getSecureHardwareState() throws ClientException {
                return popManager.getSecureHardwareState();
            }

            @Override
            public IKeyAccessor generateDerivedKey(byte[] label, byte[] ctx, CryptoSuite suite) throws ClientException {
                throw new UnsupportedOperationException("This operation is not supported by asymmetric keys");
            }
        };
    }

    /**
     * Construct an accessor for a KeyStore backed entry using a random alias.
     *
     * @param context
     * @param cipher     The cipher type of this key.
     * @param signingAlg
     * @return a key accessor for the use of that particular key.
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     * @throws IOException
     */
    public static IKeyAccessor newInstance(@NonNull final Context context,
                                           @NonNull final IDevicePopManager.Cipher cipher,
                                           @NonNull final SigningAlgorithm signingAlg)
            throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, ClientException {
        final String alias = UUID.randomUUID().toString();
        final IPlatformComponents commonComponents = AndroidPlatformComponentsFactory.createFromContext(context);
        final IDevicePopManager popManager = commonComponents.getDevicePopManager(alias);
        popManager.generateAsymmetricKey();
        return getKeyAccessor(cipher, signingAlg, popManager);
    }

    public static IKeyAccessor newInstance(@NonNull final SymmetricCipher cipher, @NonNull final boolean needRawAccess)
            throws InvalidAlgorithmParameterException, CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, ClientException, NoSuchProviderException {
        return newInstance(cipher, needRawAccess, UUID.randomUUID().toString());
    }

    /**
     * Construct an accessor for a KeyStore backed entry using a random alias.
     *
     * @param cipher        The cipher type of this key.
     * @param needRawAccess whether we need access to the raw key for, as an example, using it for SP800 derivation
     * @return a key accessor for the use of that particular key.
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     * @throws IOException
     */
    public static IKeyAccessor newInstance(@NonNull final SymmetricCipher cipher,
                                           @NonNull final boolean needRawAccess,
                                           @NonNull final String alias)
            throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, ClientException,
            NoSuchProviderException, InvalidAlgorithmParameterException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !needRawAccess) {
            final KeyStore instance = KeyStore.getInstance(ANDROID_KEYSTORE);
            instance.load(null);
            final String[] params = cipher.cipher().name().split("/");
            final KeyGenerator generator = KeyGenerator.getInstance(params[0], ANDROID_KEYSTORE);
            KeyGenParameterSpec spec = null;
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    spec = new KeyGenParameterSpec.Builder(alias, KEY_PURPOSES)
                            .setIsStrongBoxBacked(true)
                            .setKeySize(cipher.keySize())
                            .setBlockModes(params[1])
                            .setEncryptionPaddings(params[2])
                            .setKeySize(cipher.keySize())
                            .build();
                } else {
                    spec = new KeyGenParameterSpec.Builder(alias, KEY_PURPOSES)
                            .setKeySize(cipher.keySize())
                            .setBlockModes(params[1])
                            .setEncryptionPaddings(params[2])
                            .setKeySize(cipher.keySize())
                            .build();
                }
                generator.init(spec);
                generator.generateKey();
            } catch (final ProviderException e) {
                if (e.getClass().getSimpleName().equals("StrongBoxUnavailableException")) {
                    spec = null;
                } else {
                    throw e;
                }
            }
            if (spec == null) {
                spec = new KeyGenParameterSpec.Builder(alias, KEY_PURPOSES)
                        .setKeySize(cipher.keySize())
                        .setBlockModes(params[1])
                        .setEncryptionPaddings(params[2])
                        .setKeySize(cipher.keySize())
                        .build();
                generator.init(spec);
                generator.generateKey();
            }

            final AndroidDeviceKeyManager<KeyStore.SecretKeyEntry> keyManager = new AndroidDeviceKeyManager<>(instance, alias);
            return new AndroidSecretKeyAccessor(keyManager, cipher) {
                @Override
                public byte[] sign(byte[] text) throws ClientException {
                    throw new UnsupportedOperationException("This key instance does not support signing");
                }

                @Override
                public boolean verify(byte[] text, byte[] signature) throws ClientException {
                    throw new UnsupportedOperationException("This key instance does not support verification");
                }
            };
        } else {
            KeyGenerator generator = KeyGenerator.getInstance("AES");
            generator.init(cipher.mKeySize);
            byte[] key = generator.generateKey().getEncoded();
            return RawKeyAccessor.builder()
                    .suite(cipher)
                    .key(key)
                    .alias(alias).build();
        }
    }

    /**
     * Import a symmetric key into the appropriate keystore.  Currently not implemented until we
     * discover how to use this key - current protocols preclude the use of the keystore.
     *
     * @param context      The android application context - this is only actually important for downlevel
     *                     and could be forked.
     * @param cipher       the SymmetricCipher being imported.
     * @param keyAlias     the alias under which to import the cipher
     * @param key_jwe      the JWE string containing the key.
     * @param stk_accessor the accessor for the STK to use to decrypt the key, if required.
     * @return A key accessor for the imported session key
     * @throws ClientException if there is a failure while importing.
     */
    public static IKeyAccessor importSymmetricKey(@NonNull final Context context,
                                                  @NonNull final SymmetricCipher cipher,
                                                  @NonNull final String keyAlias,
                                                  @NonNull final String key_jwe,
                                                  @NonNull final IKeyAccessor stk_accessor)
            throws ParseException, CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, ClientException {
        throw new UnsupportedOperationException("This operation is not yet supported");
    }
}
