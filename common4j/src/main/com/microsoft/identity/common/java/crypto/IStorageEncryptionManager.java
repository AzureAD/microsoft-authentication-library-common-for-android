package com.microsoft.identity.common.java.crypto;

import com.microsoft.identity.common.java.exception.ClientException;

import lombok.NonNull;

/**
 * Add helper functions which takes in parameter or produce results in a ready-to-store (String) form.
 */
public interface IStorageEncryptionManager extends IKeyAccessor {
    String encrypt (@NonNull final String plainText) throws ClientException;
    String decrypt (@NonNull final String cipherText) throws ClientException;
}
