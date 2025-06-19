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
package com.microsoft.identity.common.java.crypto.key

import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.logging.Logger
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

/**
 * Implementation of [ISecretKeyGenerator] for AES-256 keys.
 * This class provides functionality to generate random AES-256 secret keys or
 * create them from raw byte arrays.
 */
class AES256SecretKeyGenerator : ISecretKeyGenerator {

    companion object {
        private val TAG = AES256SecretKeyGenerator::class.java.simpleName
        private const val AES_ALGORITHM = "AES"
        private const val AES_KEY_SIZE = 256
    }

    /**
     * Returns the key size in bits (256 for AES-256).
     * @return Key size as an integer value.
     */
    override val keySize: Int
        get() = AES_KEY_SIZE

    /**
     * Returns the algorithm name for the key specification.
     * @return String representation of the algorithm name ("AES").
     */
    override val keyAlgorithm: String
        get() = AES_ALGORITHM

    /**
     * Generates a random AES-256 secret key.
     * Uses [KeyGenerator] to create a cryptographically secure random key.
     *
     * @return A randomly generated [SecretKey] instance.
     * @throws ClientException If the algorithm is not available on the current platform.
     */
    @Throws(ClientException::class)
    override fun generateRandomKey(): SecretKey {
        val methodTag = "$TAG:generateRandomKey"
        try {
            val keygen = KeyGenerator.getInstance(keyAlgorithm)
            keygen.init(keySize, SecureRandom())
            return keygen.generateKey()
        } catch (e: NoSuchAlgorithmException) {
            val clientException = ClientException(
                ClientException.NO_SUCH_ALGORITHM,
                e.message,
                e
            )
            Logger.error(methodTag, clientException.errorCode, e)
            throw clientException
        }
    }

    /**
     * Creates an AES-256 secret key from the provided raw bytes.
     *
     * @param rawBytes The raw byte array to create the key from.
     * @return A [SecretKey] created from the provided raw bytes.
     */
    override fun generateKeyFromRawBytes(rawBytes: ByteArray): SecretKey {
        return SecretKeySpec(rawBytes, keyAlgorithm)
    }
}
