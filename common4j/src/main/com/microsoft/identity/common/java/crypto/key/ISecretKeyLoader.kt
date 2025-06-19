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
 * Interface defining how a [SecretKey] is loaded, cached, sourced, and used.
 *
 * [ISecretKeyLoader] provides a consistent abstraction layer for cryptographic key operations
 */
interface ISecretKeyLoader {
    /**
     * Returns this key's alias or name.
     *
     * The alias serves as a unique identifier for this key within the system.
     * It can be used for key storage, retrieval, and reference across the application.
     * Each key implementation must have a unique alias to avoid collisions.
     *
     * @return The unique key alias as a non-null String.
     */
    val alias: String

    /**
     * Gets an identifier of this key type.
     *
     * The key type identifier is used to distinguish between different key types
     * in the system. This value might be padded into encrypted strings to indicate
     * the key used for encryption, enabling correct key selection during decryption.
     *
     * @return The key type identifier as a non-null String.
     */
    val keyTypeIdentifier: String

    /**
     * Gets the cipher transformation string that is meant to be used with this key type.
     *
     * A cipher transformation string consists of three components:
     * - Algorithm: The base cryptographic algorithm (e.g., "AES", "RSA")
     * - Mode of operation: How the algorithm should process the data (e.g., "CBC", "GCM", "ECB")
     * - Padding scheme: How to handle data that doesn't align with the block size (e.g., "PKCS5Padding", "NoPadding")
     *
     * For example, "AES/CBC/PKCS5Padding" specifies the AES algorithm in CBC mode with PKCS5 padding.
     * The transformation specified must be compatible with the generated keys and supported by the
     * security provider being used.
     *
     * This transformation string is used directly with [javax.crypto.Cipher.getInstance] to create
     * the appropriate Cipher object for encryption and decryption operations.
     *
     * @return The complete cipher transformation string as a non-null String.
     */
    val cipherTransformation: String

    /**
     * Retrieves the secret key for encryption/decryption operations.
     *
     * This method handles the loading of an existing key or generation of a new key
     * if one doesn't exist. Key storage, caching, and platform-specific logic
     * should be encapsulated within implementations of this property.
     *
     * @return The [SecretKey] to be used for cryptographic operations.
     * @throws ClientException If an error occurs during key retrieval or generation,
     *                        including key store access issues or algorithm unavailability.
     */
    @get:Throws(ClientException::class)
    val key: SecretKey
}
