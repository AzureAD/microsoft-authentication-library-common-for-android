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
 * Representing a class for generating crypto objects.
 */
interface ICryptoFactory {

    /**
     * Gets a class name for emitting telemetry events.
     */
    val telemetryClassName: CryptoFactoryName

    /**
     * Gets a [Signature] crypto object
     */
    @Throws(ClientException::class)
    fun getSignature(algorithm: String): Signature

    /**
     * Gets a [Cipher] crypto object
     */
    @Throws(ClientException::class)
    fun getCipher(algorithm: String): Cipher

    /**
     * Gets a [Mac] crypto object
     */
    @Throws(ClientException::class)
    fun getMac(algorithm: String): Mac

    /**
     * Gets a [KeyPairGenerator] crypto object
     */
    @Throws(ClientException::class)
    fun getKeyPairGenerator(algorithm: String): KeyPairGenerator

    /**
     * Gets a [KeyFactory] crypto object
     */
    @Throws(ClientException::class)
    fun getKeyFactory(algorithm: String): KeyFactory

    /**
     * Gets a [MessageDigest] crypto object
     */
    @Throws(ClientException::class)
    fun getMessageDigest(algorithm: String): MessageDigest
}
