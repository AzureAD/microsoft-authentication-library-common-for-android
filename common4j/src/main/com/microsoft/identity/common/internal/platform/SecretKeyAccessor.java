package com.microsoft.identity.common.internal.platform;

import static com.microsoft.identity.common.java.exception.ClientException.BAD_PADDING;
import static com.microsoft.identity.common.java.exception.ClientException.INVALID_ALG_PARAMETER;
import static com.microsoft.identity.common.java.exception.ClientException.INVALID_BLOCK_SIZE;
import static com.microsoft.identity.common.java.exception.ClientException.INVALID_KEY;
import static com.microsoft.identity.common.java.exception.ClientException.INVALID_PROTECTION_PARAMS;
import static com.microsoft.identity.common.java.exception.ClientException.KEYSTORE_NOT_INITIALIZED;
import static com.microsoft.identity.common.java.exception.ClientException.NO_SUCH_ALGORITHM;
import static com.microsoft.identity.common.java.exception.ClientException.NO_SUCH_PADDING;

import com.microsoft.identity.common.java.crypto.CryptoSuite;
import com.microsoft.identity.common.java.crypto.IKeyAccessor;
import com.microsoft.identity.common.java.crypto.IKeyStoreKeyManager;
import com.microsoft.identity.common.java.crypto.IManagedKeyAccessor;
import com.microsoft.identity.common.java.crypto.SecureHardwareState;
import com.microsoft.identity.common.java.exception.ClientException;

import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.Certificate;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Accessors(prefix = "m")
@AllArgsConstructor
public class SecretKeyAccessor implements IManagedKeyAccessor<KeyStore.SecretKeyEntry> {
    private static final Charset UTF8 = Charset.forName("UTF-8");
    protected final IKeyStoreKeyManager<KeyStore.SecretKeyEntry> mKeyManager;
    private final CryptoSuite mSuite;

    @Override
    public byte[] encrypt(@NonNull final byte[] plaintext) throws ClientException {
        final String errCode;
        final Exception exception;
        try {
            final KeyStore.SecretKeyEntry entry = mKeyManager.getEntry();
            final SecretKey key = entry.getSecretKey();
            final Cipher c = Cipher.getInstance(mSuite.cipher().name());
            c.init(Cipher.ENCRYPT_MODE, key);
            final byte[] iv = c.getIV();
            final byte[] enc = c.doFinal(plaintext);
            final byte[] out = new byte[iv.length + enc.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(enc, 0, out, iv.length, enc.length);
            return out;
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
        } catch (final IllegalBlockSizeException e) {
            errCode = INVALID_BLOCK_SIZE;
            exception = e;
        } catch (final BadPaddingException e) {
            errCode = BAD_PADDING;
            exception = e;
        } catch (final InvalidKeyException e) {
            errCode = INVALID_KEY;
            exception = e;
        }
        throw new ClientException(errCode, exception.getMessage(), exception);
    }

    @Override
    public byte[] decrypt(@NonNull final byte[] ciphertext) throws ClientException {
        final String errCode;
        final Exception exception;
        try {
            final KeyStore.SecretKeyEntry entry = mKeyManager.getEntry();
            final SecretKey key = entry.getSecretKey();
            final Cipher c = Cipher.getInstance(mSuite.cipher().name());
            final GCMParameterSpec ivSpec = new GCMParameterSpec(128, ciphertext, 0, 12);
            c.init(Cipher.DECRYPT_MODE, key, ivSpec);
            final byte[] out = Arrays.copyOfRange(ciphertext, 12, ciphertext.length);
            return c.doFinal(out);
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
        } catch (final BadPaddingException e) {
            errCode = BAD_PADDING;
            exception = e;
        } catch (final InvalidKeyException e) {
            errCode = INVALID_KEY;
            exception = e;
        } catch (final InvalidAlgorithmParameterException e) {
            errCode = INVALID_ALG_PARAMETER;
            exception = e;
        }
        throw new ClientException(errCode, exception.getMessage(), exception);
    }

    @Override
    public byte[] sign(@NonNull final byte[] text) throws ClientException {
        final String errCode;
        final Exception exception;
        try {
            final KeyStore.SecretKeyEntry entry = mKeyManager.getEntry();
            SecretKey key = entry.getSecretKey();
            Mac c = Mac.getInstance(mSuite.macName());
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
        } catch (final InvalidKeyException e) {
            errCode = INVALID_KEY;
            exception = e;
        }
        throw new ClientException(errCode, exception.getMessage(), exception);
    }

    @Override
    public boolean verify(@NonNull final byte[] text, @NonNull final byte[] signature) throws ClientException {
        return Arrays.equals(signature, sign(text));
    }

    @Override
    public byte[] getThumbprint() throws ClientException {
        return mKeyManager.getThumbprint();
    }

    @Override
    public Certificate[] getCertificateChain() throws ClientException {
        return mKeyManager.getCertificateChain();
    }

    @Override
    public SecureHardwareState getSecureHardwareState() throws ClientException {
        return mKeyManager.getSecureHardwareState();
    }

    @Override
    public IKeyAccessor generateDerivedKey(final byte[] label, final byte[] ctx, final CryptoSuite suite) throws ClientException {
        throw new UnsupportedOperationException("This operation is not supported by inaccessible keys");
    }

    @Override
    public IKeyStoreKeyManager<KeyStore.SecretKeyEntry> getManager() {
        return mKeyManager;
    }

}
