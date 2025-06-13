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
import javax.crypto.SecretKey

/**
 * Interface for secret key generation.
 * Implementations of this interface provide functionality to generate cryptographic
 * secret keys either randomly or from raw byte arrays.
 */
interface ISecretKeyGenerator {
    /**
     * The size of the key in bits.
     * This is used when generating random keys to determine the key strength.
     */
    val keySize: Int

    /**
     * The algorithm name for the key.
     * This should be compatible with cryptographic providers, such as
     * those used with KeyGenerator.getInstance(algorithm).
     */
    val keyAlgorithm: String

    /**
     * Generates a cryptographically secure random secret key.
     *
     * @return A randomly generated [SecretKey] instance.
     * @throws ClientException If an error occurs during key generation,
     *                        such as when the algorithm is not available.
     */
    @Throws(ClientException::class)
    fun generateRandomKey(): SecretKey

    /**
     * Creates a secret key from the provided raw bytes.
     *
     * @param rawBytes The raw byte array to create the key from.
     * @return A [SecretKey] created from the provided raw bytes.
     */
    fun generateKeyFromRawBytes(rawBytes: ByteArray): SecretKey
}
