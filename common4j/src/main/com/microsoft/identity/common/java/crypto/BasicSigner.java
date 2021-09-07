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

import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.exception.ErrorStrings;
import com.microsoft.identity.common.java.logging.Logger;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import lombok.NonNull;

/**
 * A basic Signer class.
 */
public class BasicSigner implements ISigner{

    private static final String TAG = BasicSigner.class.getSimpleName();

    @Override
    public byte[] sign(@NonNull final PrivateKey key,
                       @NonNull final String signingAlgorithm,
                       final byte[] dataToBeSigned) throws ClientException {
        final Signature signer;
        try {
            signer = Signature.getInstance(signingAlgorithm);
            signer.initSign(key);
            signer.update(dataToBeSigned);
            return signer.sign();
        } catch (final NoSuchAlgorithmException e) {
            throw new ClientException(ClientException.NO_SUCH_ALGORITHM, e.getMessage(), e);
        } catch (final SignatureException e) {
            throw new ClientException(ClientException.SIGNING_FAILURE, e.getMessage(), e);
        } catch (final InvalidKeyException e) {
            throw new ClientException(ClientException.INVALID_KEY, e.getMessage(), e);
        }
    }

    public byte[] signWithHMac(final byte[] keyData,
                               @NonNull final String hmacAlgorithm,
                               final byte[] dataToBeSigned) throws ClientException {
        final String methodName = "signWithDerivedKey";
        try {
            final Mac sha256HMAC = Mac.getInstance(hmacAlgorithm);
            final SecretKeySpec secretKey = new SecretKeySpec(keyData, hmacAlgorithm);
            sha256HMAC.init(secretKey);
            return sha256HMAC.doFinal(dataToBeSigned);
        } catch (final NoSuchAlgorithmException e) {
            final String errorString = hmacAlgorithm + " algorithm does not exist " + e.getMessage();
            Logger.error(TAG + methodName, errorString, e);
            throw new ClientException(ClientException.NO_SUCH_ALGORITHM, errorString, e);
        } catch (final IllegalStateException e) {
            Logger.error(TAG + methodName, e.getMessage(), e);
            throw new ClientException(ErrorStrings.ENCRYPTION_ERROR, e.getMessage(), e);
        } catch (final InvalidKeyException e) {
            final String errorString = "Key is invalid for signing " + e.getMessage();
            Logger.error(TAG + methodName, errorString, e);
            throw new ClientException(ClientException.INVALID_KEY, errorString, e);
        }
    }
}
