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

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.IvParameterSpec;

import lombok.AllArgsConstructor;
import lombok.NonNull;

/**
 * A basic Decryptor class.
 */
@AllArgsConstructor
public class BasicDecryptor implements IDecryptor {

    private final ICryptoFactory mCryptoFactory;

    @Override
    public byte[] decrypt(@NonNull final Key key,
                          @NonNull final String decryptAlgorithm,
                          final byte[] iv,
                          byte[] dataToBeDecrypted) throws ClientException {
        return performCryptoOperationAndUploadTelemetry(
                CryptoObjectName.Cipher,
                decryptAlgorithm,
                mCryptoFactory,
                new ICryptoOperation<byte[]>() {
                    @Override
                    public byte[] perform() throws ClientException {
                        return decryptWithCipher(key, decryptAlgorithm, iv, dataToBeDecrypted);
                    }
                }
        );
    }

    private byte[] decryptWithCipher(@NonNull final Key key,
                                     @NonNull final String decryptAlgorithm,
                                     final byte[] iv,
                                     byte[] dataToBeDecrypted)
            throws ClientException {
        final Cipher cipher = mCryptoFactory.getCipher(decryptAlgorithm);
        try{
            if (iv != null && iv.length > 0) {
                final IvParameterSpec ivSpec = new IvParameterSpec(iv);
                cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);
            } else {
                cipher.init(Cipher.DECRYPT_MODE, key);
            }
                return cipher.doFinal(dataToBeDecrypted);
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
