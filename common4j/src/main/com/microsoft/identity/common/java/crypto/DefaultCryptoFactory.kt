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
package com.microsoft.identity.common.java.crypto

import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.opentelemetry.CryptoFactoryName
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.Signature
import javax.crypto.Cipher
import javax.crypto.Mac

/**
 * A Factory class that generates/utilizes platform's default crypto objects.
 */
class DefaultCryptoFactory : ICryptoFactory {
    override val telemetryClassName: CryptoFactoryName
        get() = CryptoFactoryName.DefaultCryptoFactory

    @Throws(ClientException::class)
    override fun getSignature(algorithm: String): Signature {
        return ProviderFactory.getSignature(algorithm, null)
    }

    @Throws(ClientException::class)
    override fun getCipher(algorithm: String): Cipher {
        return ProviderFactory.getCipher(algorithm, null)
    }

    @Throws(ClientException::class)
    override fun getMac(algorithm: String): Mac {
        return ProviderFactory.getMac(algorithm, null)
    }

    @Throws(ClientException::class)
    override fun getKeyPairGenerator(algorithm: String): KeyPairGenerator {
        return ProviderFactory.getKeyPairGenerator(algorithm, null)
    }

    @Throws(ClientException::class)
    override fun getKeyFactory(algorithm: String): KeyFactory {
        return ProviderFactory.getKeyFactory(algorithm, null)
    }

    @Throws(ClientException::class)
    override fun getMessageDigest(algorithm: String): MessageDigest {
        return ProviderFactory.getMessageDigest(algorithm, null)
    }
}
