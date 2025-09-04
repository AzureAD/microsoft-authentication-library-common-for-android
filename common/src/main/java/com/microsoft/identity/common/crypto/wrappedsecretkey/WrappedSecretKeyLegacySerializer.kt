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
 * Legacy serializer for [WrappedSecretKey] that maintains backward compatibility with the original format.
 *
 * This serializer handles the legacy format where only the raw wrapped key data is stored
 * without any additional metadata such as algorithm or cipher transformation information.
 * When deserializing legacy data, it applies default values for missing metadata to ensure
 * proper key reconstruction.
 *
 * **Format characteristics:**
 * - Version: 0 (legacy)
 * - Data: Raw wrapped key bytes only
 * - No metadata header
 * - Default algorithm: AES
 * - Default cipher transformation: RSA/ECB/PKCS1Padding
 *
 * This serializer is essential for maintaining compatibility when migrating from older
 * key storage formats to newer metadata-aware formats.
 *
 * @see IWrappedSecretKeySerializer
 * @see WrappedSecretKey
 */
class WrappedSecretKeyLegacySerializer : IWrappedSecretKeySerializer {

    companion object {
        /** Version identifier for the legacy format */
        const val VERSION = 0

        /** Default algorithm used when no metadata is available */
        private const val DEFAULT_ALGORITHM = "AES"

        /** Default cipher transformation used when no metadata is available */
        private const val DEFAULT_CIPHER_TRANSFORMATION = "RSA/ECB/PKCS1Padding"
    }

    /**
     * Serializes a [WrappedSecretKey] to the legacy format.
     *
     * In the legacy format, only the raw wrapped key data is stored without any metadata.
     * This maintains compatibility with older storage implementations.
     *
     * @param wrappedSecretKey The wrapped secret key to serialize
     * @return Raw wrapped key data as byte array
     */
    override fun serialize(wrappedSecretKey: WrappedSecretKey): ByteArray {
        return wrappedSecretKey.wrappedKeyData
    }

    /**
     * Deserializes byte data to a [WrappedSecretKey] using legacy format assumptions.
     *
     * Since the legacy format doesn't include metadata, this method applies default
     * values for algorithm and cipher transformation to reconstruct a complete
     * [WrappedSecretKey] instance.
     *
     * @param data Raw wrapped key data from legacy format
     * @return [WrappedSecretKey] instance with legacy data and default metadata
     */
    override fun deserialize(data: ByteArray): WrappedSecretKey {
        // Legacy format does not include metadata, use defaults
        return WrappedSecretKey(
            wrappedKeyData = data,
            algorithm = DEFAULT_ALGORITHM,
            cipherTransformation = DEFAULT_CIPHER_TRANSFORMATION
        )
    }

    /**
     * Returns the version identifier for this serializer.
     *
     * @return Version 0, indicating the legacy format
     */
    override fun getVersion(): Int {
        return VERSION
    }
}