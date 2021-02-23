package com.microsoft.identity.common.internal.platform;

import com.microsoft.identity.common.exception.ClientException;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.UnrecoverableEntryException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import lombok.Builder;
import lombok.experimental.Accessors;

import static com.microsoft.identity.common.exception.ClientException.BAD_PADDING;
import static com.microsoft.identity.common.exception.ClientException.INVALID_BLOCK_SIZE;
import static com.microsoft.identity.common.exception.ClientException.INVALID_KEY;
import static com.microsoft.identity.common.exception.ClientException.INVALID_PROTECTION_PARAMS;
import static com.microsoft.identity.common.exception.ClientException.IO_ERROR;
import static com.microsoft.identity.common.exception.ClientException.KEYSTORE_NOT_INITIALIZED;
import static com.microsoft.identity.common.exception.ClientException.NO_SUCH_ALGORITHM;
import static com.microsoft.identity.common.exception.ClientException.NO_SUCH_PADDING;

@Builder
@Accessors(prefix = "m")
public class SecretKeyAccessor implements KeyAccessor {
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private final DeviceKeyManager<KeyStore.SecretKeyEntry> mKeyManager;
    private final CryptoSuite suite;
    @Override
    public byte[] encrypt(byte[] plaintext) throws ClientException {
        final String errCode;
        final Exception exception;
        try {
            final KeyStore.SecretKeyEntry entry = mKeyManager.getEntry();
            SecretKey key = entry.getSecretKey();
            Cipher c = Cipher.getInstance(key.getAlgorithm());
            c.init(Cipher.ENCRYPT_MODE, key);
            return c.doFinal(plaintext);
        } catch (final UnrecoverableEntryException e) {
            errCode = INVALID_PROTECTION_PARAMS;
            exception = e;
        } catch (final NoSuchAlgorithmException e) {
            errCode = NO_SUCH_ALGORITHM;
            exception = e;
        } catch (final KeyStoreException e) {
            errCode = KEYSTORE_NOT_INITIALIZED;
            exception = e;
        } catch (final NoSuchPaddingException e) {
            errCode = NO_SUCH_PADDING;
            exception = e;
        } catch (IllegalBlockSizeException e) {
            errCode = INVALID_BLOCK_SIZE;
            exception = e;
        } catch (BadPaddingException e) {
            errCode = BAD_PADDING;
            exception = e;
        } catch (InvalidKeyException e) {
            errCode = INVALID_KEY;
            exception = e;
        }
        throw new ClientException(errCode, exception.getMessage(), exception);
    }

    @Override
    public byte[] decrypt(byte[] ciphertext) throws ClientException {
        final String errCode;
        final Exception exception;
        try {
            final KeyStore.SecretKeyEntry entry = mKeyManager.getEntry();
            SecretKey key = entry.getSecretKey();
            Cipher c = Cipher.getInstance(key.getAlgorithm());
            c.init(Cipher.DECRYPT_MODE, key);
            return c.doFinal(ciphertext);
        } catch (final UnrecoverableEntryException e) {
            errCode = INVALID_PROTECTION_PARAMS;
            exception = e;
        } catch (final NoSuchAlgorithmException e) {
            errCode = NO_SUCH_ALGORITHM;
            exception = e;
        } catch (final KeyStoreException e) {
            errCode = KEYSTORE_NOT_INITIALIZED;
            exception = e;
        } catch (final NoSuchPaddingException e) {
            errCode = NO_SUCH_PADDING;
            exception = e;
        } catch (IllegalBlockSizeException e) {
            errCode = INVALID_BLOCK_SIZE;
            exception = e;
        } catch (BadPaddingException e) {
            errCode = BAD_PADDING;
            exception = e;
        } catch (InvalidKeyException e) {
            errCode = INVALID_KEY;
            exception = e;
        }
        throw new ClientException(errCode, exception.getMessage());
    }

    @Override
    public byte[] sign(byte[] text, IDevicePopManager.SigningAlgorithm alg) throws ClientException {
        final String errCode;
        final Exception exception;
        try {
            final KeyStore.SecretKeyEntry entry = mKeyManager.getEntry();
            SecretKey key = entry.getSecretKey();
            Mac c = Mac.getInstance(suite.macName());
            c.init(key);
            return c.doFinal(text);
        } catch (final UnrecoverableEntryException e) {
            errCode = INVALID_PROTECTION_PARAMS;
            exception = e;
        } catch (final NoSuchAlgorithmException e) {
            errCode = NO_SUCH_ALGORITHM;
            exception = e;
        } catch (final KeyStoreException e) {
            errCode = KEYSTORE_NOT_INITIALIZED;
            exception = e;
        } catch (InvalidKeyException e) {
            errCode = INVALID_KEY;
            exception = e;
        }
        throw new ClientException(errCode, exception.getMessage());
    }

    @Override
    public boolean verify(byte[] text, IDevicePopManager.SigningAlgorithm alg, byte[] signature) throws ClientException {
        return Arrays.equals(signature, sign(text, alg));
    }

    @Override
    public byte[] getThumprint() throws ClientException {
        return mKeyManager.getThumbprint();
    }
}
