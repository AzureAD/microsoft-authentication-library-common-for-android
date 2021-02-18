package com.microsoft.identity.common.internal.platform;

import com.microsoft.identity.common.exception.ClientException;

/**
 * An interface for interacting with a particular cipher instance.  This is suitable
 * for any particular mechanism, as it assumes that all the key management and other
 * implementation details are being handled by the underlying implementations.  It exposes
 * basically only the standard cryptographic operations, encrypt, decrypt, sign, and verify.
 */
public interface Key {
    /**
     * Encrypt a plaintext blob, returning an encrypted byte array.
     * @param plaintext the plaintext to encrypt.
     * @return the encrypted byte array.
     */
    public byte[] encrypt(byte[] plaintext) throws ClientException;

    /**
     * Decrypt a blob of ciphertext, returning the decrypted values.
     * @param ciphertext the blob of ciphertext to decrypt.
     * @return the decrypted byte array.
     */
    public byte[] decrypt(byte[] ciphertext) throws ClientException;

    /**
     * Sign a block of data, returning the signature.
     * @param text the data to sign.
     * @return the signature, as a byte array.
     */
    public byte[] sign(byte[] text) throws ClientException;

    /**
     * Verify a signature, returning the
     * @param text
     * @param signature
     * @return
     */
    public boolean verify(byte[] text, byte[] signature) throws ClientException;
}
