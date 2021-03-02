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

import androidx.annotation.Nullable;

import com.microsoft.identity.common.exception.ClientException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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

import lombok.Builder;
import lombok.NonNull;

import static com.microsoft.identity.common.exception.ClientException.BAD_PADDING;
import static com.microsoft.identity.common.exception.ClientException.INVALID_ALG_PARAMETER;
import static com.microsoft.identity.common.exception.ClientException.INVALID_BLOCK_SIZE;
import static com.microsoft.identity.common.exception.ClientException.INVALID_KEY;
import static com.microsoft.identity.common.exception.ClientException.NO_SUCH_ALGORITHM;
import static com.microsoft.identity.common.exception.ClientException.NO_SUCH_PADDING;
import static com.microsoft.identity.common.internal.platform.KeyStoreAccessor.UTF8;

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
    public byte[] encrypt(@NonNull final byte[] plaintext) throws ClientException {
        final String errCode;
        final Exception exception;
        try {
            final SecretKeySpec keySpec = new SecretKeySpec(key, suite.cipherName());
            final Cipher c = Cipher.getInstance(keySpec.getAlgorithm());
            final byte[] iv = new byte[12];
            mRandom.nextBytes(iv);
            final IvParameterSpec ivSpec = new IvParameterSpec(iv);
            c.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
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
    public byte[] sign(@NonNull final byte[] text, @Nullable final IDevicePopManager.SigningAlgorithm alg) throws ClientException {
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
        } catch (final InvalidKeyException e) {
            errCode = INVALID_KEY;
            exception = e;
        }
        throw new ClientException(errCode, exception.getMessage());
    }

    @Override
    public boolean verify(@NonNull final byte[] text, @Nullable final IDevicePopManager.SigningAlgorithm alg,
                          @NonNull final byte[] signature) throws ClientException {
        return Arrays.equals(signature, sign(text, alg));
    }

    @Override
    public byte[] getThumprint() throws ClientException {
        final SecretKey keySpec = new SecretKeySpec(key, suite.cipherName());
        final Cipher cipher;
        final String errCode;
        final Exception exception;
        try {
            cipher = Cipher.getInstance(keySpec.getAlgorithm());
            final MessageDigest digest = MessageDigest.getInstance("SHA256");
            return digest.digest(cipher.doFinal((keySpec.getAlgorithm() + cipher.getBlockSize() + cipher.getParameters()).getBytes(UTF8)));
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
    public Certificate[] getCertificateChain() {
        return null;
    }

    @Override
    public SecureHardwareState getSecureHardwareState() {
        return SecureHardwareState.FALSE;
    }

    private static byte[] deriveKey(byte[] keyDerivationKey, byte[] fixedInput)
            throws IOException, NoSuchAlgorithmException, InvalidKeyException {
        byte ctr;
        byte[] cHMAC;
        byte[] keyDerivated;
        byte[] dataInput;

        int len;
        int numCurrentElements;
        int numCurrentElementsBytes;
        int outputSizeBit = 256;

        numCurrentElements = 0;
        ctr = 1;
        keyDerivated = new byte[outputSizeBit / 8];
        final SecretKeySpec keySpec = new SecretKeySpec(keyDerivationKey, "HmacSHA256");
        final Mac hmacSHA256 = Mac.getInstance("HmacSHA256");

        do {
            dataInput = updateDataInput(ctr, fixedInput);
            hmacSHA256.reset();
            hmacSHA256.init(keySpec);
            hmacSHA256.update(dataInput);
            cHMAC = hmacSHA256.doFinal();
            if (256 >= outputSizeBit) {
                len = outputSizeBit;
            } else {
                len = Math.min(256, outputSizeBit - numCurrentElements);
            }

            numCurrentElementsBytes = numCurrentElements / 8;
            System.arraycopy(cHMAC, 0, keyDerivated, numCurrentElementsBytes, 32);
            numCurrentElements = numCurrentElements + len;
            ctr++;
        } while (numCurrentElements < outputSizeBit);
        return keyDerivated;
    }

    public byte[] generateDerivedKey(@NonNull final byte[] label, @NonNull final byte[] ctx)
            throws IOException, InvalidKeyException, NoSuchAlgorithmException {
        if (ctx == null) {
            return null;
        }
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        stream.write(label);
        stream.write(0x0);
        stream.write(ctx);

        ByteBuffer bigEndianInt = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(256);
        stream.write(bigEndianInt.array());

        byte[] pbDerivedKey = deriveKey(key, stream.toByteArray());
        return Arrays.copyOf(pbDerivedKey, 32);
    }

    private static byte[] updateDataInput(final byte ctr, @NonNull final byte[] fixedInput) throws IOException {
        ByteArrayOutputStream tmpFixedInput = new ByteArrayOutputStream(fixedInput.length + 4);
        tmpFixedInput.write(ctr >>> 24);
        tmpFixedInput.write(ctr >>> 16);
        tmpFixedInput.write(ctr >>> 8);
        tmpFixedInput.write(ctr);

        tmpFixedInput.write(fixedInput);
        return tmpFixedInput.toByteArray();
    }
}
