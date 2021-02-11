package com.microsoft.identity.common.internal.platform;

import com.microsoft.identity.common.exception.ClientException;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.UUID;

/**
 * This class is a static factory providing access to KeyStore objects.  Since all of the construction
 * in DevicePopManager is package private, and we're really interested in only a few operations, just
 * construct new instances here, and expose an interface that gives us the functionality that we need.
 */
public class KeyStoreAccessor {
    /**
     * For a given alias, construct an accessor for a KeyStore backed entry given that alias.
     * @param alias The key alias.
     * @param cipher The cipher type of this key.
     * @return a key accessor for the use of that particular key.
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     * @throws IOException
     */
    public static KeyAccessor forAlias(final String alias, final IDevicePopManager.Cipher cipher) throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
        final IDevicePopManager popManager = new DevicePopManager(alias);
        return getKeyAccessor(cipher, popManager);
    }

    private static final KeyAccessor getKeyAccessor(final IDevicePopManager.Cipher cipher, final IDevicePopManager popManager) {
        return new KeyAccessor() {

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
        };
    }

    /**
     * Construct an accessor for a KeyStore backed entry using a random alias.
     * @param cipher The cipher type of this key.
     * @return a key accessor for the use of that particular key.
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     * @throws IOException
     */
    public static KeyAccessor newInstance(final IDevicePopManager.Cipher cipher) throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
        String alias = UUID.randomUUID().toString();
        final IDevicePopManager popManager = new DevicePopManager(alias);
        return getKeyAccessor(cipher, popManager);
    }

}
