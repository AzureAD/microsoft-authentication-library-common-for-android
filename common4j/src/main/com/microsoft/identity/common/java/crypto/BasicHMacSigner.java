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

import static com.microsoft.identity.common.java.opentelemetry.CryptoFactoryTelemetryHelper.performCryptoTaskAndUploadTelemetry;

import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.exception.ErrorStrings;
import com.microsoft.identity.common.java.opentelemetry.CryptoFactoryOperationName;
import com.microsoft.identity.common.java.opentelemetry.ICryptoOperationCallback;

import java.security.InvalidKeyException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.experimental.Accessors;

/**
 * A very basic HMAC Signer class.
 */
@AllArgsConstructor
@Accessors(prefix = "m")
public class BasicHMacSigner implements IHMacSigner {

    private final ICryptoFactory mCryptoFactory;

    public byte[] sign(final byte[] keyData,
                       @NonNull final String hmacAlgorithm,
                       final byte[] dataToBeSigned) throws ClientException {
        return performCryptoTaskAndUploadTelemetry(
                CryptoFactoryOperationName.Mac,
                hmacAlgorithm,
                mCryptoFactory,
                new ICryptoOperationCallback<byte[]>() {
                    @Override
                    public byte[] perform() throws ClientException {
                        return signWithMac(keyData, hmacAlgorithm, dataToBeSigned);
                    }
                }
        );
    }

    public byte[] signWithMac(final byte[] keyData,
                              @NonNull final String hmacAlgorithm,
                              final byte[] dataToBeSigned) throws ClientException {
        try {
            final Mac sha256HMAC = mCryptoFactory.getMac(hmacAlgorithm);
            final SecretKeySpec secretKey = new SecretKeySpec(keyData, hmacAlgorithm);
            sha256HMAC.init(secretKey);
            return sha256HMAC.doFinal(dataToBeSigned);
        } catch (final IllegalStateException e) {
            throw new ClientException(ErrorStrings.ENCRYPTION_ERROR, e.getMessage(), e);
        } catch (final InvalidKeyException e) {
            throw new ClientException(ClientException.INVALID_KEY, e.getMessage(), e);
        }
    }
}
