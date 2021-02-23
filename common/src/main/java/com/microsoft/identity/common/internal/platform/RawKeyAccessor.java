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
package com.microsoft.identity.common.internal.platform;

import com.microsoft.identity.common.exception.ClientException;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import lombok.Builder;
import lombok.NonNull;

import static com.microsoft.identity.common.exception.ClientException.BAD_PADDING;
import static com.microsoft.identity.common.exception.ClientException.INVALID_ALG_PARAMETER;
import static com.microsoft.identity.common.exception.ClientException.INVALID_BLOCK_SIZE;
import static com.microsoft.identity.common.exception.ClientException.INVALID_KEY;
import static com.microsoft.identity.common.exception.ClientException.NO_SUCH_ALGORITHM;
import static com.microsoft.identity.common.exception.ClientException.NO_SUCH_PADDING;

/**
 * Key accessor for using a raw symmetric key.
 */
@Builder
public class RawKeyAccessor implements KeyAccessor {
    private final CryptoSuite suite;
    private final byte[] key;
    private static final SecureRandom mRandom = new SecureRandom();

    public byte[] getRawKey() {
        return Arrays.copyOf(key, key.length);
    }

    @Override
    public byte[] encrypt(byte[] plaintext) throws ClientException {
        final String errCode;
        final Exception exception;
        try {
            final SecretKeySpec key = new SecretKeySpec(this.key, suite.cipherName());
            final Cipher c = Cipher.getInstance(key.getAlgorithm());
            final byte[] iv = new byte[12];
            mRandom.nextBytes(iv);
            final IvParameterSpec ivSpec = new IvParameterSpec(iv);
            c.init(Cipher.ENCRYPT_MODE, key, ivSpec);
            c.update(plaintext);
            byte[] tmp = c.doFinal();
            final byte[] output = new byte[iv.length + tmp.length];
            System.arraycopy(iv, 0, output, 0, iv.length);
            System.arraycopy(tmp, 0, output, iv.length, tmp.length);
            return output;
        } catch (final NoSuchAlgorithmException e) {
            errCode = NO_SUCH_ALGORITHM;
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
        } catch (InvalidAlgorithmParameterException e) {
            errCode = INVALID_ALG_PARAMETER;
            exception = e;
        }
        throw new ClientException(errCode, exception.getMessage());
    }

    @Override
    public byte[] decrypt(byte[] ciphertext) throws ClientException {
        final String errCode;
        final Exception exception;
        try {
            final SecretKeySpec key = new SecretKeySpec(this.key, suite.cipherName());
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
        } catch (IllegalBlockSizeException e) {
            errCode = INVALID_BLOCK_SIZE;
            exception = e;
        } catch (BadPaddingException e) {
            errCode = BAD_PADDING;
            exception = e;
        } catch (InvalidKeyException e) {
            errCode = INVALID_KEY;
            exception = e;
        } catch (InvalidAlgorithmParameterException e) {
            errCode = INVALID_ALG_PARAMETER;
            exception = e;
        }
        throw new ClientException(errCode, exception.getMessage(), exception);
    }

    @Override
    public byte[] sign(byte[] text, IDevicePopManager.SigningAlgorithm alg) throws ClientException {
        final String errCode;
        final Exception exception;
        try {
            final SecretKeySpec key = new SecretKeySpec(this.key, suite.cipherName());
            Mac mac = Mac.getInstance(suite.macName());
            mac.init(key);
            return mac.doFinal(text);
        } catch (final NoSuchAlgorithmException e) {
            errCode = NO_SUCH_ALGORITHM;
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
        return new byte[0];
    }
}
