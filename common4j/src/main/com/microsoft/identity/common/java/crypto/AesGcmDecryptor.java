package com.microsoft.identity.common.java.crypto;

import com.microsoft.identity.common.java.exception.ClientException;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;

import lombok.AllArgsConstructor;
import lombok.NonNull;

@AllArgsConstructor
public class AesGcmDecryptor implements IDecryptor {

    private final ICryptoFactory mCryptoFactory;

    @Override
    public byte[] decrypt(@NonNull final Key key,
                          @NonNull final String decryptAlgorithm,
                          @NonNull final byte[] iv,
                          @NonNull final byte[] dataToBeDecrypted,
                          @NonNull final byte[] tag,
                          final byte[] aad) throws ClientException {
        final Cipher cipher = mCryptoFactory.getCipher(decryptAlgorithm);
        try{
            GCMParameterSpec spec = new GCMParameterSpec(tag.length * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);
            cipher.updateAAD(aad);
            byte[] cipherText = new byte[dataToBeDecrypted.length + tag.length];
            System.arraycopy(dataToBeDecrypted, 0, cipherText, 0, dataToBeDecrypted.length);
            System.arraycopy(tag, 0, cipherText, dataToBeDecrypted.length, tag.length);
            return cipher.doFinal(cipherText);
        } catch (final BadPaddingException e) {
            throw new ClientException(ClientException.BAD_PADDING, e.getMessage(), e);
        } catch (final IllegalBlockSizeException e) {
            throw new ClientException(ClientException.INVALID_BLOCK_SIZE, e.getMessage(), e);
        } catch (final InvalidKeyException e) {
            throw new ClientException(ClientException.INVALID_KEY, e.getMessage(), e);
        } catch (final InvalidAlgorithmParameterException e) {
            throw new ClientException(ClientException.INVALID_ALG_PARAMETER, e.getMessage(), e);
        }
    }
}
