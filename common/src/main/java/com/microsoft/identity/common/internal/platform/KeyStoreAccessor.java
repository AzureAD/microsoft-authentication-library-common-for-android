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

import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.util.Supplier;
import com.nimbusds.jwt.EncryptedJWT;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.text.ParseException;
import java.util.UUID;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

/**
 * This class is a static factory providing access to KeyStore objects.  Since all of the construction
 * in DevicePopManager is package private, and we're really interested in only a few operations, just
 * construct new instances here, and expose an interface that gives us the functionality that we need.
 */
public class KeyStoreAccessor {
    /**
     * The name of the KeyStore to use.
     */
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    public static final Charset UTF8 = Charset.forName("UTF-8");
    private static final int KEY_PURPOSES =  KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT | KeyProperties.PURPOSE_SIGN ;

    /**
     * For a given alias, construct an accessor for a KeyStore backed entry given that alias.
     *
     * @param context
     * @param alias The key alias.
     * @param suite The cipher type of this key.
     * @return a key accessor for the use of that particular key.
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     * @throws IOException
     */
    public static KeyAccessor forAlias(Context context, final String alias, final CryptoSuite suite) throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, ClientException {
        final IDevicePopManager popManager = new DevicePopManager(alias);
        if (suite instanceof IDevicePopManager.Cipher) {
            if (!popManager.asymmetricKeyExists()) {
                popManager.generateAsymmetricKey(context);
            }
            return getKeyAccessor((IDevicePopManager.Cipher) suite, popManager);
        }
        final KeyStore instance = KeyStore.getInstance(ANDROID_KEYSTORE);
        final DeviceKeyManager<KeyStore.SecretKeyEntry> keyManager = new DeviceKeyManager<>(instance, alias, symmetricThumbprint(alias, instance));
        return new SecretKeyAccessor(keyManager, suite) {
            @Override
            public byte[] sign(byte[] text, IDevicePopManager.SigningAlgorithm alg) throws ClientException {
                throw new UnsupportedOperationException("This key instance does not support signing");
            }

            @Override
            public boolean verify(byte[] text, IDevicePopManager.SigningAlgorithm alg, byte[] signature) throws ClientException {
                throw new UnsupportedOperationException("This key instance does not support verification");
            }
        };
    }

    private static final KeyAccessor getKeyAccessor(final IDevicePopManager.Cipher cipher, final IDevicePopManager popManager) {
        return new AsymmetricKeyAccessor() {

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
            public byte[] sign(byte[] text, IDevicePopManager.SigningAlgorithm alg) throws ClientException {
                return popManager.sign(alg, text);
            }

            @Override
            public boolean verify(byte[] text, IDevicePopManager.SigningAlgorithm alg, byte[] signature) throws ClientException {
                return popManager.verify(alg, text, signature);
            }

            @Override
            public byte[] getThumprint() throws ClientException {
                return popManager.getAsymmetricKeyThumbprint().getBytes(UTF8);
            }
        };
    }

    /**
     * Construct an accessor for a KeyStore backed entry using a random alias.
     *
     * @param context
     * @param cipher The cipher type of this key.
     * @return a key accessor for the use of that particular key.
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     * @throws IOException
     */
    public static KeyAccessor newInstance(Context context, final IDevicePopManager.Cipher cipher) throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, ClientException {
        String alias = UUID.randomUUID().toString();
        final IDevicePopManager popManager = new DevicePopManager(alias);
        popManager.generateAsymmetricKey(context);
        return getKeyAccessor(cipher, popManager);
    }

    /**
     * Construct an accessor for a KeyStore backed entry using a random alias.
     * @param cipher The cipher type of this key.
     * @param needRawAccess whether we need access to the raw key for, as an example, using it for SP800 derivation
     * @return a key accessor for the use of that particular key.
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     * @throws IOException
     */
    public static KeyAccessor newInstance(SymmetricCipher cipher, boolean needRawAccess) throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, ClientException, NoSuchProviderException, InvalidAlgorithmParameterException {
        String alias = UUID.randomUUID().toString();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M && !needRawAccess) {
            final KeyStore instance = KeyStore.getInstance(ANDROID_KEYSTORE);
            instance.load(null);
            String[] params = cipher.cipherName().split("/");
            KeyGenerator generator = KeyGenerator.getInstance(params[0], ANDROID_KEYSTORE);
            KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(alias, KEY_PURPOSES)
                    .setKeySize(cipher.keySize())
                    .setBlockModes(params[1])
                    .setEncryptionPaddings(params[2])
                    .setKeySize(cipher.keySize())
                    .build();
            generator.init(spec);
            generator.generateKey();

            final DeviceKeyManager<KeyStore.SecretKeyEntry> keyManager = new DeviceKeyManager<>(instance, alias, symmetricThumbprint(alias, instance));
            return new SecretKeyAccessor(keyManager, cipher) {
                @Override
                public byte[] sign(byte[] text, IDevicePopManager.SigningAlgorithm alg) throws ClientException {
                    throw new UnsupportedOperationException("This key instance does not support signing");
                }

                @Override
                public boolean verify(byte[] text, IDevicePopManager.SigningAlgorithm alg, byte[] signature) throws ClientException {
                    throw new UnsupportedOperationException("This key instance does not support verification");
                }
            };
        } else {
            KeyGenerator generator = KeyGenerator.getInstance("AES");
            generator.init(cipher.mKeySize);
            byte[] key = generator.generateKey().getEncoded();
            return new RawKeyAccessor(cipher, key);
        }
    }

    public static Supplier<byte[]> symmetricThumbprint(String alias, KeyStore instance) {
        return new Supplier<byte[]>() {
            @Override
            public byte[] get() {
                try {
                    KeyStore.Entry entry = instance.getEntry(alias, null);
                    if (entry instanceof KeyStore.SecretKeyEntry) {
                        final SecretKey key = ((KeyStore.SecretKeyEntry) entry).getSecretKey();
                        final Cipher cipher = Cipher.getInstance(key.getAlgorithm());
                        final MessageDigest digest = MessageDigest.getInstance("SHA256");
                        return digest.digest(cipher.doFinal((key.getAlgorithm() + cipher.getBlockSize() + cipher.getParameters()).getBytes(UTF8)));
                    } else {
                        return null;
                    }
                } catch (KeyStoreException | BadPaddingException | NoSuchAlgorithmException | IllegalBlockSizeException | UnrecoverableEntryException | NoSuchPaddingException e) {
                    Logger.error("KeyAccessor:newInstance", null, "Exception while getting key entry", e);
                    return null;
                }
            }
        };
    }

    /**
     * Construct an accessor for a KeyStore backed entry using a random alias.
     *
     * TODO: implement and fix this.
     * @param cipher The cipher type of this key.
     * @return a key accessor for the use of that particular key.
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     * @throws IOException
     */
    /*
    public static KeyAccessor newInstance(AsymmetricCipher cipher) throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, ClientException, NoSuchProviderException, InvalidAlgorithmParameterException {
        String alias = UUID.randomUUID().toString();
        final KeyStore instance;
        final PrivateKey entry;
        if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.M) {
            instance = KeyStore.getInstance(ANDROID_KEYSTORE);
            instance.load(null);
            KeyGenerator generator = KeyGenerator.getInstance(cipher.cipherName(), ANDROID_KEYSTORE);
            KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(alias, KEY_PURPOSES)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .setKeySize(256)
                    .build();
            generator.init(spec);
        } else {
            instance = KeyStore.getInstance("PKCS12");
            instance.load(null);
            KeyGenerator generator = KeyGenerator.getInstance(cipher.cipherName(), "PKCS12");
            generator.init(cipher.keySize());
            generator.generateKey();
        }
        final DeviceKeyManager<KeyStore.PrivateKeyEntry> keyManager = new DeviceKeyManager<>(instance, alias, new Supplier<byte[]>() {
                @Override
                public byte[] get() {
                    DevicePopManager.getRsaThumbprint(keyManager.getEntry());
                };
            });

        return new AsymmetricKeyAccessor() {
            @Override
            public String getPublicKey(IDevicePopManager.PublicKeyFormat format) throws ClientException {
                try {
                    return keyManager.getEntry();
                } catch (UnrecoverableEntryException | NoSuchAlgorithmException | KeyStoreException e) {
                    throw new ClientException(ClientException.UNKNOWN_ERROR, e.getMessage(), e);
                }
            }

            @Override
            public PublicKey getPublicKey() throws UnrecoverableEntryException, NoSuchAlgorithmException, KeyStoreException {
                return null;
            }

            @Override
            public byte[] encrypt(byte[] plaintext) throws ClientException {
                return new byte[0];
            }

            @Override
            public byte[] decrypt(byte[] ciphertext) throws ClientException {
                return new byte[0];
            }

            @Override
            public byte[] sign(byte[] text, IDevicePopManager.SigningAlgorithm alg) throws ClientException {
                return new byte[0];
            }

            @Override
            public boolean verify(byte[] text, IDevicePopManager.SigningAlgorithm alg, byte[] signature) throws ClientException {
                return false;
            }

            @Override
            public byte[] getThumprint() throws ClientException {
                return new byte[0];
            }
        }
    }
*/

    public static KeyAccessor importSymmetricKey(Context context, SymmetricCipher cipher, String keyAlias, String key_jwe, KeyAccessor stk_accessor) throws ParseException, CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, ClientException {
        EncryptedJWT jwt = EncryptedJWT.parse(key_jwe);
        byte[] encryptedKey = jwt.getEncryptedKey().decode();
        //JWEHeader header = jwt.getHeader();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //TODO: add code to interpret the header algorithm.
            KeyAccessor accessor = KeyStoreAccessor.forAlias(context, keyAlias, IDevicePopManager.Cipher.RSA_ECB_OAEPWithSHA_256AndMGF1Padding);
            byte[] rawKey = accessor.decrypt(encryptedKey);
            return new RawKeyAccessor(cipher, rawKey);
        }
        throw new UnsupportedOperationException("This operation is not yet supported");
    }
}
