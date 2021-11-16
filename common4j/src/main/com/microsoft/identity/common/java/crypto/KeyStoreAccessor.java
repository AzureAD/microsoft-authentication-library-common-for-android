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
package com.microsoft.identity.common.java.crypto;

import com.microsoft.identity.common.internal.platform.SecretKeyAccessor;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.interfaces.IPlatformComponents;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.util.ported.Supplier;

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
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.text.ParseException;
import java.util.UUID;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

/**
 * This class is a static factory providing access to KeyStore objects.  Since all of the construction
 * in DevicePopManager is package private, and we're really interested in only a few operations, just
 * construct new instances here, and expose an interface that gives us the functionality that we need.
 */
@SuperBuilder
@Accessors(prefix = "m")
@Getter
@AllArgsConstructor
public abstract class KeyStoreAccessor implements IKeyStoreAccessor {
    /**
     * The name of the KeyStore to use.
     */
    private final String mKeyStoreName;
    private final IPlatformComponents mPlatformComponents;
    public static final Charset UTF8 = Charset.forName("UTF-8");

    /**
     * For a given alias, construct an accessor for a KeyStore backed entry given that alias.
     *
     * @param commonComponents
     * @param alias The key alias.
     * @param suite The cipher type of this key.
     * @return a key accessor for the use of that particular key.
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     * @throws IOException
     */
    @Override
    public IKeyAccessor forAlias(@NonNull final IPlatformComponents commonComponents, @NonNull final String alias, @NonNull final CryptoSuite suite)
            throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, ClientException {
        final IDevicePopManager popManager = commonComponents.getDevicePopManager(alias);
        if (suite.cipher() instanceof IDevicePopManager.Cipher) {
            if (!popManager.asymmetricKeyExists()) {
                popManager.generateAsymmetricKey();
            }
            return getKeyAccessor((IDevicePopManager.Cipher) suite.cipher(), suite.signingAlgorithm(), popManager);
        }
        final KeyStore instance = KeyStore.getInstance(mKeyStoreName);
        final IKeyStoreKeyManager<KeyStore.SecretKeyEntry> keyManager = mPlatformComponents.getSymmetricKeyManager(instance, alias, symmetricThumbprint(alias, instance));
        return new SecretKeyAccessor(keyManager, suite);
    }

    private IKeyAccessor getKeyAccessor(@NonNull final IDevicePopManager.Cipher cipher,
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
     * @param commonComponents
     * @param cipher The cipher type of this key.
     * @param signingAlg
     * @return a key accessor for the use of that particular key.
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     * @throws IOException
     */
    @Override
    public IKeyAccessor newInstance(@NonNull final IPlatformComponents commonComponents,
                                    @NonNull final IDevicePopManager.Cipher cipher,
                                    @NonNull final SigningAlgorithm signingAlg)
            throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, ClientException {
        final String alias = UUID.randomUUID().toString();
        final IDevicePopManager popManager = commonComponents.getDevicePopManager(alias);
        popManager.generateAsymmetricKey();
        return getKeyAccessor(cipher, signingAlg, popManager);
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
    @Override
    public IKeyAccessor newInstance(@NonNull final CryptoSuite cipher, @NonNull final boolean needRawAccess)
            throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, ClientException,
                   NoSuchProviderException, InvalidAlgorithmParameterException {
        if (needRawAccess) {
            final String alias = UUID.randomUUID().toString();
            KeyGenerator generator = KeyGenerator.getInstance(cipher.cipher().name().split("/")[0]);
            generator.init(cipher.keySize());
            byte[] key = generator.generateKey().getEncoded();
            return RawKeyAccessor.builder()
                    .suite(cipher)
                    .key(key)
                    .alias(alias).build();
        } else {
            final String alias = UUID.randomUUID().toString();
            KeyGenerator generator = KeyGenerator.getInstance(cipher.cipher().name().split("/")[0]);
            generator.init(cipher.keySize());
            KeyStore instance = KeyStore.getInstance(getKeyStoreName());

            final IKeyStoreKeyManager<KeyStore.SecretKeyEntry> keyManager = mPlatformComponents.getSymmetricKeyManager(instance, alias, symmetricThumbprint(alias, instance));
            return new SecretKeyAccessor(keyManager, cipher);

        }
    }

    /**
     * Compute a thumbprint of a symmetric key for a given alias.  The impetus here is that the key
     * itself is inaccessible, but we would still like to identify a particular instance.  We can
     * encrypt a fixed value based on the cipher parameters with the key, and take a SHA256 digest
     * of it.  This could be inspection resistant enough to identify different keys without exposing
     * the actual key.
     * @param alias The alias of the key to thumbprint.
     * @param instance the KeyStore to get the key from.
     * @return A supplier that can compute the thumbprint for the key on demand.
     */
    public static Supplier<byte[]> symmetricThumbprint(@NonNull final String alias, @NonNull final KeyStore instance) {
        return new Supplier<byte[]>() {
            @Nullable
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
                } catch (final KeyStoreException | BadPaddingException | NoSuchAlgorithmException
                        | IllegalBlockSizeException | UnrecoverableEntryException | NoSuchPaddingException e) {
                    Logger.error("KeyAccessor:newInstance", null, "Exception while getting key entry", e);
                    return null;
                }
            }
        };
    }

    /**
     * Import a symmetric key into the appropriate keystore.  Currently not implemented until we
     * discover how to use this key - current protocols preclude the use of the keystore.
     * @param context The android application context - this is only actually important for downlevel
     *                and could be forked.
     * @param cipher the SymmetricCipher being imported.
     * @param keyAlias the alias under which to import the cipher
     * @param key_jwe the JWE string containing the key.
     * @param stk_accessor the accessor for the STK to use to decrypt the key, if required.
     * @return A key accessor for the imported session key
     * @throws ClientException if there is a failure while importing.
     */
    @Override
    public IKeyAccessor importSymmetricKey(@NonNull final IPlatformComponents context,
                                           @NonNull final CryptoSuite cipher,
                                           @NonNull final String keyAlias,
                                           @NonNull final String key_jwe,
                                           @NonNull final IKeyAccessor stk_accessor)
            throws ParseException, CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, ClientException {
        throw new UnsupportedOperationException("This operation is not yet supported");
    }
}
