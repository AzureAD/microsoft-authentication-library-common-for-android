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

import static com.microsoft.identity.common.java.opentelemetry.CryptoFactoryTelemetryHelper.performCryptoOperationAndUploadTelemetry;

import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.opentelemetry.CryptoObjectName;
import com.microsoft.identity.common.java.opentelemetry.ICryptoOperation;

import lombok.AllArgsConstructor;
import lombok.experimental.Accessors;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * This class is a key derivation function for SP800 108 key generation.
 */
@AllArgsConstructor
@Accessors(prefix = "m")
public class SP800108KeyGen {

    static final byte[] BIG_ENDIAN_INT_256 = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(256).array();

    private final ICryptoFactory mCryptoFactory;

    private static final String HMAC_ALGORITHM = "HMacSHA256";

    /**
     * Generate a derived key given a starting key.
     * @param key the basis for the key material.
     * @param label a label for the key.
     * @param ctx the key context.
     * @return a derived key.
     * @throws IOException
     * @throws InvalidKeyException
     * @throws NoSuchAlgorithmException
     */
    public byte[] generateDerivedKey(final byte[] key,
                                     final byte[] label,
                                     final byte[] ctx)
            throws IOException, InvalidKeyException, NoSuchAlgorithmException, ClientException {

        final SecretKeySpec keySpec = new SecretKeySpec(key, "HmacSHA256");

        return generateDerivedKey(keySpec, label, ctx);
    }

    /**
     * Generate a derived key given a starting key.
     * @param secretKey the key to derive from.
     * @param label a label for the key.
     * @param ctx the key context.
     * @return a derived key.
     * @throws IOException
     * @throws InvalidKeyException
     * @throws NoSuchAlgorithmException
     */
    public byte[] generateDerivedKey(final SecretKey secretKey,
                                     final byte[] label,
                                     final byte[] ctx)
            throws IOException, InvalidKeyException, NoSuchAlgorithmException, ClientException {
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        stream.write(label);
        stream.write(0x0);
        stream.write(ctx);

        stream.write(BIG_ENDIAN_INT_256);

        byte[] pbDerivedKey = performCryptoOperationAndUploadTelemetry(
                CryptoObjectName.Mac,
                HMAC_ALGORITHM,
                mCryptoFactory,
                new ICryptoOperation<byte[]>() {
                    @Override
                    public byte[] perform() throws ClientException {
                        try {
                            return constructNewKey(secretKey, stream.toByteArray());
                        } catch (IOException e) {
                            throw new ClientException(ClientException.IO_ERROR, e.getMessage(), e);
                        } catch (InvalidKeyException e) {
                            throw new ClientException(ClientException.INVALID_KEY, e.getMessage(), e);
                        }
                    }
                }
        );

        return Arrays.copyOf(pbDerivedKey, 32);
    }

    private byte[] constructNewKey(final SecretKey secretKey,
                                   final byte[] fixedInput)
            throws IOException, InvalidKeyException, ClientException {
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
        final Mac hmacSHA256 = mCryptoFactory.getMac("HmacSHA256");

        do {
            dataInput = updateDataInput(ctr, fixedInput);
            hmacSHA256.init(secretKey);
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

            // Reset so that it can be reused in the next iteration.
            hmacSHA256.reset();
        } while (numCurrentElements < outputSizeBit);
        return keyDerivated;
    }

    private static byte[] updateDataInput(final byte ctr,
                                          final byte[] fixedInput) throws IOException {
        final ByteArrayOutputStream tmpFixedInput = new ByteArrayOutputStream(fixedInput.length + 4);
        tmpFixedInput.write(ctr >>> 24);
        tmpFixedInput.write(ctr >>> 16);
        tmpFixedInput.write(ctr >>> 8);
        tmpFixedInput.write(ctr);

        tmpFixedInput.write(fixedInput);
        return tmpFixedInput.toByteArray();
    }
}
