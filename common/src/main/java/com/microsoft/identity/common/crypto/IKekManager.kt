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
package com.microsoft.identity.common.crypto

import com.microsoft.identity.common.java.exception.ClientException
import javax.crypto.SecretKey

/**
 * Interface for key encryption key (KEK) generation and management.
 * Defines the contract for generating and managing key pairs used to wrap/unwrap secret keys.
 * Implementations can handle different algorithms and API versions.
 */
interface IKekManager {
    /**
     * Gets the appropriate cipher transformation to use with the generated keys.
     * The transformation string specifies the algorithm, mode, and padding to be
     * used for encryption/decryption operations.
     *
     * @return The cipher transformation string in the format "algorithm/mode/padding"
     */
    val cipherTransformation: String

    /**
     * Wraps (encrypts) a secret key using the Key Encryption Key managed by this interface.
     * The wrapped key can be safely stored and later unwrapped using the [.unwrapKey] method.
     *
     * @param keyToWrap The plaintext secret key that needs to be wrapped
     * @return The wrapped (encrypted) key as a byte array
     * @throws ClientException If wrapping fails due to cryptographic errors, key unavailability,
     * or insufficient permissions
     */
    @Throws(ClientException::class)
    fun wrapKey(keyToWrap: SecretKey): ByteArray

    /**
     * Unwraps (decrypts) a previously wrapped secret key using the Key Encryption Key.
     *
     * @param wrappedSecretKey The wrapped (encrypted) key as a byte array
     * @param secretKeyAlgorithm The algorithm name of the wrapped secret key (e.g., "AES")
     * needed to properly reconstruct the key after unwrapping
     * @return The unwrapped plaintext secret key
     * @throws ClientException If unwrapping fails due to cryptographic errors,
     * key unavailability, tampered wrapped key,
     * or insufficient permissions
     */
    @Throws(ClientException::class)
    fun unwrapKey(wrappedSecretKey: ByteArray, secretKeyAlgorithm: String): SecretKey

    /**
     * Checks if the Key Encryption Key exists and is accessible.
     * This can be used to verify if a KEK is available before attempting operations.
     *
     * @return true if the KEK exists and is accessible, false otherwise
     * @throws ClientException If checking for key existence fails due to
     * security framework errors or insufficient permissions
     */
    @Throws(ClientException::class)
    fun kekExists(): Boolean
}
