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
import com.microsoft.identity.common.java.opentelemetry.CryptoFactoryOperationName;
import com.microsoft.identity.common.java.opentelemetry.ICryptoOperationCallback;

import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.experimental.Accessors;

/**
 * A very basic Signer class.
 */
@AllArgsConstructor
@Accessors(prefix = "m")
public class BasicSigner implements ISigner {

    private final ICryptoFactory mCryptoFactory;

    public byte[] sign(@NonNull final PrivateKey key,
                       @NonNull final String signingAlgorithm,
                       final byte[] dataToBeSigned) throws ClientException {
        return performCryptoTaskAndUploadTelemetry(
                CryptoFactoryOperationName.Signature,
                signingAlgorithm,
                mCryptoFactory,
                new ICryptoOperationCallback<byte[]>() {
                    @Override
                    public byte[] perform() throws ClientException {
                        return signWithSignature(key, signingAlgorithm, dataToBeSigned);
                    }
                }
        );
    }

    private byte[] signWithSignature(@NonNull final PrivateKey key,
                                     @NonNull final String signingAlgorithm,
                                     final byte[] dataToBeSigned) throws ClientException {
        try {
            final Signature signer = mCryptoFactory.getSignature(signingAlgorithm);
            signer.initSign(key);
            signer.update(dataToBeSigned);
            return signer.sign();
        } catch (final SignatureException e) {
            throw new ClientException(ClientException.SIGNING_FAILURE, e.getMessage(), e);
        } catch (final InvalidKeyException e) {
            throw new ClientException(ClientException.INVALID_KEY, e.getMessage(), e);
        }
    }
}
