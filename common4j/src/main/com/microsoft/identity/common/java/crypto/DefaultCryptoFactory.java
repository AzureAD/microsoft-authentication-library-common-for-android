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

import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.Signature;

import javax.crypto.Cipher;
import javax.crypto.Mac;

import lombok.NonNull;

/**
 * A Factory class that generates/utilizes platform's default crypto objects.
 */
public class DefaultCryptoFactory implements ICryptoFactory {
    @Override
    public @NonNull Signature getSignature(@NonNull String algorithm) throws ClientException {
        return ProviderFactory.getSignature(algorithm, null);
    }

    @Override
    public @NonNull Cipher getCipher(@NonNull String algorithm) throws ClientException {
        return ProviderFactory.getCipher(algorithm, null);
    }

    @Override
    public @NonNull Mac getMac(@NonNull String algorithm) throws ClientException {
        return ProviderFactory.getMac(algorithm, null);
    }

    @Override
    public @NonNull KeyPairGenerator getKeyPairGenerator(@NonNull String algorithm) throws ClientException {
        return ProviderFactory.getKeyPairGenerator(algorithm, null);
    }

    @Override
    public @NonNull KeyFactory getKeyFactory(@NonNull String algorithm) throws ClientException {
        return ProviderFactory.getKeyFactory(algorithm, null);
    }
}
