package com.microsoft.identity.common.internal.platform;

import com.microsoft.identity.common.exception.ClientException;

import java.security.KeyPair;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.UnrecoverableEntryException;

import lombok.Builder;
import lombok.experimental.Accessors;

/**
 * Accessor for asymmetric key that are not keystore backed.  This class holds a key pair and mediates
 * access to it according the the cipher suite provided on construction.
 */
@Builder
@Accessors(prefix  = "m")
public class KeyPairAccessor implements AsymmetricKeyAccessor {
    private final CryptoSuite mSuite;
    private final KeyPair mKeyPair;

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

    @Override
    public String getPublicKey(IDevicePopManager.PublicKeyFormat format) throws ClientException {
        return null;
    }

    @Override
    public PublicKey getPublicKey() throws UnrecoverableEntryException, NoSuchAlgorithmException, KeyStoreException {
        return null;
    }
}
