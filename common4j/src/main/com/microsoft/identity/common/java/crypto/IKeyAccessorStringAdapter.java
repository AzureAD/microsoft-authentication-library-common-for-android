package com.microsoft.identity.common.java.crypto;

import com.microsoft.identity.common.java.exception.ClientException;

import lombok.NonNull;

public interface IKeyAccessorStringAdapter {
    /**
     * Encrypt a plaintext string, returning an encrypted encoded string.
     *
     * @param plainText the plaintext to encrypt.
     * @return the encoded ciphertext.
     */
    String encrypt(@NonNull final String plainText) throws ClientException;

    /**
     * Decrypt an encoded ciphertext, returning the decrypted values.
     *
     * @param cipherText the encoded ciphertext to decrypt.
     * @return the decrypted string.
     */
    String decrypt(@NonNull final String cipherText) throws ClientException;
}
