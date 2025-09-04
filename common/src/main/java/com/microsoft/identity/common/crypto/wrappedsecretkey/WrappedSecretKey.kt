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

import com.microsoft.identity.common.java.flighting.CommonFlight
import com.microsoft.identity.common.java.flighting.CommonFlightsManager

/**
 * Represents a wrapped secret key with cryptographic metadata.
 *
 * Encapsulates an encrypted secret key with algorithm and cipher transformation metadata.
 * Supports multiple serialization formats controlled by flight configuration for backward compatibility.
 *
 * **Formats:**
 * - Version 0: Legacy (raw key data only)
 * - Version 1: JSON metadata format
 * - Future versions: Extensible via flight config
 *
 * @property wrappedKeyData The encrypted secret key bytes
 * @property algorithm The key algorithm (e.g., "AES")
 * @property cipherTransformation The cipher transformation (e.g., "RSA/ECB/PKCS1Padding")
 *
 * @see WrappedSecretKeySerializerManager
 * @see CommonFlight.WRAPPED_SECRET_KEY_SERIALIZER_VERSION
 */
data class WrappedSecretKey(
    val wrappedKeyData: ByteArray,
    val algorithm: String,
    val cipherTransformation: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WrappedSecretKey

        if (!wrappedKeyData.contentEquals(other.wrappedKeyData)) return false
        if (algorithm != other.algorithm) return false
        if (cipherTransformation != other.cipherTransformation) return false

        return true
    }

    override fun hashCode(): Int {
        var result = wrappedKeyData.contentHashCode()
        result = 31 * result + algorithm.hashCode()
        result = 31 * result + cipherTransformation.hashCode()
        return result
    }

    /**
     * Serializes the key using the format specified by flight configuration.
     *
     * @return Serialized byte array
     */
    fun serialize(): ByteArray {
        val serializerVersion = CommonFlightsManager.getFlightsProvider()
            .getIntValue(CommonFlight.WRAPPED_SECRET_KEY_SERIALIZER_VERSION)
        return WrappedSecretKeySerializerManager
            .getSerializer(serializerVersion)
            .serialize(this)
    }

    companion object {
        /**
         * Deserializes a wrapped key with automatic format detection.
         *
         * @param data The serialized byte data
         * @return Reconstructed [WrappedSecretKey]
         * @throws IllegalArgumentException if format is unsupported
         */
        fun deserialize(data: ByteArray): WrappedSecretKey {
            val serializerVersion = WrappedSecretKeySerializerManager.getVersion(data)
            return WrappedSecretKeySerializerManager
                .getSerializer(serializerVersion)
                .deserialize(data)
        }
    }
}
