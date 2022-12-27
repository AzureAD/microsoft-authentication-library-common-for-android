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
package com.microsoft.identity.common.java.crypto;

import com.microsoft.identity.common.java.AuthenticationConstants;
import com.microsoft.identity.common.java.crypto.IKeyAccessor;
import com.microsoft.identity.common.java.crypto.SP800108KeyGen;
import com.microsoft.identity.common.java.crypto.SecureHardwareState;
import com.microsoft.identity.common.java.exception.ClientException;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;

import static com.microsoft.identity.common.java.exception.ClientException.BAD_PADDING;
import static com.microsoft.identity.common.java.exception.ClientException.INVALID_ALG_PARAMETER;
import static com.microsoft.identity.common.java.exception.ClientException.INVALID_BLOCK_SIZE;
import static com.microsoft.identity.common.java.exception.ClientException.INVALID_KEY;
import static com.microsoft.identity.common.java.exception.ClientException.IO_ERROR;
import static com.microsoft.identity.common.java.exception.ClientException.NO_SUCH_ALGORITHM;
import static com.microsoft.identity.common.java.exception.ClientException.NO_SUCH_PADDING;

/**
 * Key accessor for using a raw symmetric key.
 */
@Builder
@Getter
@AllArgsConstructor
@Accessors(prefix = "m")
@SuppressFBWarnings("EI_EXPOSE_REP")
public class RawKeyAccessor implements IKeyAccessor {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * The cryptoSuite to use with this RawKeyAccessor.
     */
    @NonNull
    private final CryptoSuite mSuite;
    /**
     * The byte array that backs this key.
     */
    @NonNull
    private final byte[] mKey;

    /**
     * The alias for the stored key, may be null.
     */
    private final String mAlias;

    /**
     * @return the raw bytes of the stored key.
     */
    public byte[] getRawKey() {
        return Arrays.copyOf(mKey, mKey.length);
    }

    @Override
    public byte[] encrypt(@NonNull final byte[] plaintext) throws ClientException {
        final String errCode;
        final Exception exception;
        try {
            final SecretKeySpec keySpec = new SecretKeySpec(mKey, mSuite.cipher().name());
            final Cipher c = Cipher.getInstance(keySpec.getAlgorithm());
            final byte[] iv = new byte[12];
            SECURE_RANDOM.nextBytes(iv);
            final IvParameterSpec ivSpec = new IvParameterSpec(iv);
            c.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            byte[] text = c.update(plaintext);
            byte[] tmp = c.doFinal();
            final byte[] output = new byte[iv.length + text.length + tmp.length];
            System.arraycopy(iv, 0, output, 0, iv.length);
            System.arraycopy(text, 0, output, iv.length, text.length);
            System.arraycopy(tmp, 0, output, iv.length + text.length, tmp.length);
            return output;
        } catch (final NoSuchAlgorithmException e) {
            errCode = NO_SUCH_ALGORITHM;
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
        } catch (final InvalidAlgorithmParameterException e) {
            errCode = INVALID_ALG_PARAMETER;
            exception = e;
        }
        throw new ClientException(errCode, exception.getMessage());
    }

    @Override
    public byte[] decrypt(@NonNull final byte[] ciphertext) throws ClientException {
        final String errCode;
        final Exception exception;
        try {
            final SecretKeySpec key = new SecretKeySpec(this.mKey, mSuite.cipher().name());
            final Cipher c = Cipher.getInstance(key.getAlgorithm());
            final IvParameterSpec ivSpec = new IvParameterSpec(ciphertext, 0, 12);
            c.init(Cipher.DECRYPT_MODE, key, ivSpec);
            byte[] out = Arrays.copyOfRange(ciphertext, 12, ciphertext.length);
            return c.doFinal(out);
        } catch (final NoSuchAlgorithmException e) {
            errCode = NO_SUCH_ALGORITHM;
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
            final SecretKeySpec key = new SecretKeySpec(this.mKey, mSuite.cipher().name());
            Mac mac = Mac.getInstance(mSuite.macName());
            mac.init(key);
            return mac.doFinal(text);
        } catch (final NoSuchAlgorithmException e) {
            errCode = NO_SUCH_ALGORITHM;
            exception = e;
        } catch (final InvalidKeyException e) {
            errCode = INVALID_KEY;
            exception = e;
        }
        throw new ClientException(errCode, exception.getMessage());
    }

    @Override
    public boolean verify(@NonNull final byte[] text,
                          @NonNull final byte[] signature) throws ClientException {
        return Arrays.equals(signature, sign(text));
    }

    @Override
    public byte[] getThumbprint() throws ClientException {
        final SecretKey keySpec = new SecretKeySpec(mKey, mSuite.cipher().name());
        final Cipher cipher;
        final String errCode;
        final Exception exception;
        try {
            cipher = Cipher.getInstance(keySpec.getAlgorithm());
            final MessageDigest digest = MessageDigest.getInstance("SHA256");
            return digest.digest(cipher.doFinal((keySpec.getAlgorithm() + cipher.getBlockSize() + cipher.getParameters()).getBytes(AuthenticationConstants.CHARSET_UTF8)));
        } catch (final NoSuchAlgorithmException e) {
            errCode = NO_SUCH_ALGORITHM;
            exception = e;
        } catch (final NoSuchPaddingException e) {
            errCode = NO_SUCH_PADDING;
            exception = e;
        } catch (final BadPaddingException e) {
            errCode = BAD_PADDING;
            exception = e;
        } catch (final IllegalBlockSizeException e) {
            errCode = INVALID_BLOCK_SIZE;
            exception = e;
        }
        throw new ClientException(errCode, exception.getMessage(), exception);
    }

    @Override
    @Nullable
    public Certificate[] getCertificateChain() {
        return null;
    }

    @Override
    public SecureHardwareState getSecureHardwareState() {
        return SecureHardwareState.FALSE;
    }

    /**
     * Given this raw key, generate a derived key from it.  If we close on a KDF for hardware keys,
     * this can get promoted to the symmetric key interface.
     * @param label the label for the generated key.
     * @param ctx the context bytes for the generated key.
     * @return a new key, generated from the previous one.
     * @throws ClientException if something goes wrong during generation.
     */
    public byte[] generateDerivedKey(final byte[] label, @NonNull final byte[] ctx) throws ClientException {
        try {
            return new SP800108KeyGen(new DefaultCryptoFactory())
                    .generateDerivedKey(mKey, label, ctx);
        } catch (IOException e) {
            throw new ClientException(IO_ERROR, e.getMessage(), e);
        } catch (InvalidKeyException e) {
            throw new ClientException(INVALID_KEY, e.getMessage(), e);
        } catch (NoSuchAlgorithmException e) {
            throw new ClientException(NO_SUCH_ALGORITHM, e.getMessage(), e);
        }
    }

    /**
     * Given this raw key, generate a derived key from it.  If we close on a KDF for hardware keys,
     * this can get promoted to the symmetric key interface.  Derived keys have a null alias, since
     * they should generally not be persisted.
     * @param label the label for the generated key.
     * @param ctx the context bytes for the generated key.
     * @param suite the ciphersuite to use for the generated key.
     * @return a new key, generated from the previous one.
     * @throws ClientException if something goes wrong during generation.
     */
    @Override
    public IKeyAccessor generateDerivedKey(@NonNull final byte[] label, @NonNull final byte[] ctx,
                                          @NonNull final CryptoSuite suite) throws ClientException {
        try {
            return new RawKeyAccessor(
                    suite,
                    new SP800108KeyGen(new DefaultCryptoFactory())
                            .generateDerivedKey(mKey, label, ctx), null);
        } catch (IOException e) {
            throw new ClientException(IO_ERROR, e.getMessage(), e);
        } catch (InvalidKeyException e) {
            throw new ClientException(INVALID_KEY, e.getMessage(), e);
        } catch (NoSuchAlgorithmException e) {
            throw new ClientException(NO_SUCH_ALGORITHM, e.getMessage(), e);
        }
    }

}
