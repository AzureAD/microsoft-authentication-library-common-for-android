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
package com.microsoft.identity.common.crypto.wrappedsecretkey


/**
 * Legacy serializer for [WrappedSecretKey] that maintains backward compatibility.
 *
 * Handles the original format with raw key data only, applying default metadata
 * during deserialization for proper key reconstruction.
 *
 * **Format:**
 * - **ID**: 0 (legacy)
 * - **Structure**: Raw wrapped key bytes only
 * - **Defaults**: AES algorithm, RSA/ECB/PKCS1Padding transformation
 *
 * **Use cases:**
 * - Deserializing pre-metadata format keys
 * - Backward compatibility with existing storage
 * - Migration to newer metadata-aware formats
 *
 * @see IWrappedSecretKeySerializer
 */
class WrappedSecretKeyLegacySerializer : IWrappedSecretKeySerializer {

    companion object {
        /** Default algorithm used when no metadata is available */
        private const val DEFAULT_ALGORITHM = "AES"

        /** Default cipher transformation used when no metadata is available */
        private const val DEFAULT_CIPHER_TRANSFORMATION = "RSA/ECB/PKCS1Padding"

        /** Unique identifier for the legacy serialization format */
        const val ID = 0
    }

    /**
     * Serializes a [WrappedSecretKey] to the legacy format.
     *
     * In the legacy format, only the raw wrapped key data is stored without any metadata.
     * This maintains compatibility with older storage implementations that expect
     * direct key data without headers or metadata sections.
     *
     * **Output format:**
     * ```
     * [Raw Key Data]
     * ```
     *
     * @param wrappedSecretKey The wrapped secret key to serialize
     * @return Raw wrapped key data as byte array (no headers or metadata)
     */
    override fun serialize(wrappedSecretKey: WrappedSecretKey): ByteArray {
        return wrappedSecretKey.wrappedKeyData
    }

    /**
     * Deserializes byte data to a [WrappedSecretKey] using legacy format assumptions.
     *
     * Since the legacy format doesn't include metadata, this method applies default
     * values for algorithm and cipher transformation to reconstruct a complete
     * [WrappedSecretKey] instance. The default values are chosen to maintain
     * compatibility with the most common cryptographic configurations.
     *
     * **Default metadata applied:**
     * - Algorithm: "AES"
     * - Cipher transformation: "RSA/ECB/PKCS1Padding"
     *
     * @param wrappedSecretKeyByteArray The byte array containing the raw wrapped key data
     * @return [WrappedSecretKey] instance with legacy data and default metadata
     * @throws IllegalArgumentException if data is null or empty
     */
    override fun deserialize(wrappedSecretKeyByteArray: ByteArray): WrappedSecretKey {
        // Legacy format does not include metadata, use defaults
        return WrappedSecretKey(
            wrappedKeyData = wrappedSecretKeyByteArray,
            algorithm = DEFAULT_ALGORITHM,
            cipherTransformation = DEFAULT_CIPHER_TRANSFORMATION
        )
    }

    /**
     * The unique identifier for this serialization format.
     *
     * Returns 0 to indicate the legacy format, which is used by the
     * [WrappedSecretKeySerializerManager] for format detection and serializer selection.
     */
    override val id = ID
}