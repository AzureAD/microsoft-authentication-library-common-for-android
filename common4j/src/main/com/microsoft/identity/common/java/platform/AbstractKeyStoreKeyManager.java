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
package com.microsoft.identity.common.java.platform;

import static com.microsoft.identity.common.java.exception.ClientException.INVALID_PROTECTION_PARAMS;
import static com.microsoft.identity.common.java.exception.ClientException.KEYSTORE_NOT_INITIALIZED;
import static com.microsoft.identity.common.java.exception.ClientException.NO_SUCH_ALGORITHM;
import static com.microsoft.identity.common.java.exception.ClientException.THUMBPRINT_COMPUTATION_FAILURE;

import com.microsoft.identity.common.java.crypto.IKeyStoreKeyManager;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.logging.Logger;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.util.Base64URL;

import java.nio.charset.Charset;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableEntryException;
import java.security.cert.Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.Date;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;

/**
 * A manager class for providing access to a particular entry in a KeyStore.
 *
 * @param <K> the type of KeyStore.Entry being managed.
 */
@Accessors(prefix = "m")
public abstract class AbstractKeyStoreKeyManager<K extends KeyStore.Entry> implements IKeyStoreKeyManager<K> {

    private static final String TAG = AbstractKeyStoreKeyManager.class.getSimpleName();

    private static final Charset UTF8 = Charset.forName("UTF-8");

    protected final KeyStore mKeyStore;

    @Getter
    private final String mKeyAlias;

    private final KeyStore.PasswordProtection mPasswordProtection;

    public AbstractKeyStoreKeyManager(@NonNull final KeyStore keyStore,
                                      @NonNull final String keyAlias,
                                      @Nullable final KeyStore.PasswordProtection passwordProtection) throws KeyStoreException {
        mKeyAlias = keyAlias;
        mKeyStore = keyStore;
        mPasswordProtection = passwordProtection;
    }

    @Override
    public boolean exists() {
        boolean exists = false;

        try {
            exists = mKeyStore.containsAlias(mKeyAlias);
        } catch (final KeyStoreException e) {
            Logger.error(
                    TAG,
                    "Error while querying KeyStore",
                    e
            );
        }

        return exists;

    }

    @Override
    public boolean hasThumbprint(final byte[] thumbprint) {
        try {
            return Arrays.equals(thumbprint, getThumbprint());
        } catch (final ClientException e) {
            return false;
        }
    }

    @Override
    public Date getCreationDate() throws ClientException {
        try {
            return mKeyStore.getCreationDate(mKeyAlias);
        } catch (final KeyStoreException e) {
            Logger.error(
                    TAG,
                    "Error while getting creation date for alias " + mKeyAlias,
                    e
            );
            throw new ClientException(ClientException.KEYSTORE_NOT_INITIALIZED, e.getMessage(), e);
        }
    }

    @Override
    public boolean clear() {
        boolean deleted = false;

        try {
            mKeyStore.deleteEntry(mKeyAlias);
            deleted = true;
        } catch (final KeyStoreException e) {
            Logger.error(
                    TAG,
                    "Error while clearing KeyStore",
                    e
            );
        }

        return deleted;
    }

    /**
     * Retrieve the entry stored in this particular alias.
     *
     * @return the Entry in question, or null if the entry does not exist.
     * @throws UnrecoverableEntryException
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     */
    @SuppressWarnings("unchecked")
    @Override
    public K getEntry() throws UnrecoverableEntryException, NoSuchAlgorithmException, KeyStoreException {
        return (K) mKeyStore.getEntry(mKeyAlias, mPasswordProtection);
    }

    @Override
    public void importKey(@NonNull final byte[] jwk, @NonNull final String algorithm) throws ClientException {
        throw new UnsupportedOperationException("This is not currently supported");
    }

    @Override
    public byte[] getThumbprint() throws ClientException {
        final Exception exception;
        final String errCode;

        try {
            final K entry = getEntry();

            if (entry instanceof KeyStore.PrivateKeyEntry) {
                return getRsaThumbprint((KeyStore.PrivateKeyEntry) entry).getBytes(UTF8);
            } else if (entry instanceof KeyStore.SecretKeyEntry) {
                return getSecretKeyThumbprint((KeyStore.SecretKeyEntry) entry);
            } else {
                throw new UnsupportedOperationException("Get thumbprint currently not supported for " +
                        "key of type: " + entry.getClass().getCanonicalName());
            }
        } catch (final KeyStoreException e) {
            exception = e;
            errCode = KEYSTORE_NOT_INITIALIZED;
        } catch (final NoSuchAlgorithmException e) {
            exception = e;
            errCode = NO_SUCH_ALGORITHM;
        } catch (final UnrecoverableEntryException e) {
            exception = e;
            errCode = INVALID_PROTECTION_PARAMS;
        } catch (final JOSEException e) {
            exception = e;
            errCode = THUMBPRINT_COMPUTATION_FAILURE;
        }

        throw new ClientException(
                errCode,
                exception.getMessage(),
                exception
        );
    }

    @Override
    public Certificate[] getCertificateChain() throws ClientException {
        final Exception exception;
        final String errCode;

        try {
            return mKeyStore.getCertificateChain(mKeyAlias);
        } catch (final KeyStoreException e) {
            exception = e;
            errCode = KEYSTORE_NOT_INITIALIZED;
        }

        final ClientException clientException = new ClientException(
                errCode,
                exception.getMessage(),
                exception
        );

        Logger.error(
                TAG,
                clientException.getMessage(),
                clientException
        );

        throw clientException;
    }

    /**
     * Given an RSA private key entry, get the RSA thumbprint.
     *
     * @param entry the entry to compute the thumbprint for.
     * @return A String that would be identicative of this specific key.
     * @throws JOSEException If there is a computation problem.
     */
    public static String getRsaThumbprint(@NonNull final KeyStore.PrivateKeyEntry entry) throws JOSEException {
        final KeyPair rsaKeyPair = getKeyPairForEntry(entry);
        final RSAKey rsaKey = getRsaKeyForKeyPair(rsaKeyPair);
        return getThumbprintForRsaKey(rsaKey);
    }

    /**
     * Given a {@link java.security.KeyStore.SecretKeyEntry}, get the thumbprint.
     *
     * @param entry the {@link java.security.KeyStore.SecretKeyEntry}
     * @return the thumbprint of the key
     */
    public static byte[] getSecretKeyThumbprint(@NonNull final KeyStore.SecretKeyEntry entry) {
        try {
            final SecretKey key = ((KeyStore.SecretKeyEntry) entry).getSecretKey();
            final Cipher cipher = Cipher.getInstance(key.getAlgorithm());
            final MessageDigest digest = MessageDigest.getInstance("SHA256");
            return digest.digest(cipher.doFinal((key.getAlgorithm() + cipher.getBlockSize() + cipher.getParameters()).getBytes(UTF8)));
        } catch (final NoSuchAlgorithmException | NoSuchPaddingException | BadPaddingException | IllegalBlockSizeException e) {
            Logger.error("KeyAccessor:newInstance", null, "Exception while getting key entry", e);
            return null;
        }
    }

    /**
     * For the supplied {@link KeyStore.Entry}, get a corresponding {@link KeyPair} instance.
     *
     * @param entry The Keystore.Entry to use.
     * @return The resulting KeyPair.
     */
    public static KeyPair getKeyPairForEntry(@NonNull final KeyStore.PrivateKeyEntry entry) {
        final PrivateKey privateKey = entry.getPrivateKey();
        final PublicKey publicKey = entry.getCertificate().getPublicKey();
        return new KeyPair(publicKey, privateKey);
    }

    /**
     * Gets the corresponding {@link RSAKey} for the supplied {@link KeyPair}.
     *
     * @param keyPair The KeyPair to use.
     * @return The resulting RSAKey.
     */
    public static RSAKey getRsaKeyForKeyPair(@NonNull final KeyPair keyPair) {
        if (keyPair.getPublic() instanceof RSAPublicKey) {
            return new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                    .keyUse(null)
                    .build();
        } else {
            throw new UnsupportedOperationException("Cannot get RSAKey for " +
                    "key of type: " + keyPair.getPublic().getClass().getCanonicalName());
        }
    }

    /**
     * Given a {@link RSAKey}, compute its thumbprint.
     *
     * @param rsaKey the {@link RSAKey}
     * @return the thumbprint of the key
     * @throws JOSEException if an error occurs while computing thumbprint
     */
    public static String getThumbprintForRsaKey(@NonNull RSAKey rsaKey) throws JOSEException {
        final Base64URL thumbprint = rsaKey.computeThumbprint();
        return thumbprint.toString();
    }
}
