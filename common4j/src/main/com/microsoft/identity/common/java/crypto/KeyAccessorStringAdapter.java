package com.microsoft.identity.common.java.crypto;

import com.microsoft.identity.common.java.exception.ClientException;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;

import static com.microsoft.identity.common.java.AuthenticationConstants.ENCODING_UTF8;

/**
 * Add helper functions which takes in parameter or produce results in a ready-to-store (String) form.
 */
@AllArgsConstructor
public class KeyAccessorStringAdapter {

    public IKeyAccessor mKeyAcccesor;

    public String encrypt(@NonNull String plainText) throws ClientException{
        final byte[] result = mKeyAcccesor.encrypt(plainText.getBytes(ENCODING_UTF8));
        return new String(result, ENCODING_UTF8);
    }

    public String decrypt(@NonNull String cipherText) throws ClientException {
        final byte[] result = mKeyAcccesor.decrypt(cipherText.getBytes(ENCODING_UTF8));
        return new String(result, ENCODING_UTF8);
    }
}
