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
import com.microsoft.identity.common.java.opentelemetry.AttributeName
import com.microsoft.identity.common.java.opentelemetry.SpanExtension

/**
 * Represents a wrapped secret key with cryptographic metadata.
 *
 * Encapsulates an encrypted secret key with algorithm and cipher transformation metadata.
 * Supports multiple serialization formats controlled by flight configuration for backward compatibility.
 *
 * **Formats:**
 * - ID 0: Legacy (raw wrappedSecretKey data only)
 * - ID 1: Binary stream with metadata (UTF strings + int)
 * - Future IDs: Extensible via flight config
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
     * @return Serialized byte array using the configured serializer ID
     */
    fun serialize(): ByteArray {
        val serializerId = CommonFlightsManager.getFlightsProvider()
            .getIntValue(CommonFlight.WRAPPED_SECRET_KEY_SERIALIZER_VERSION)
        SpanExtension.current().setAttribute(
            AttributeName.secret_key_wrapping_serializer_id.name,
            serializerId.toLong()
        )
        val serializedKey = measureAndRecordTelemetry {
            WrappedSecretKeySerializerManager
                .getSerializer(serializerId)
                .serialize(this)
        }
        SpanExtension.current().setAttribute(
            AttributeName.secret_key_size.name,
            serializedKey.size.toLong()
        )
        return serializedKey
    }

    companion object {
        /**
         * Deserializes a wrapped key with automatic format detection.
         *
         * @param data The serialized byte data
         * @return Reconstructed [WrappedSecretKey]
         * @throws IllegalArgumentException if serializer ID is unsupported
         */
        fun deserialize(data: ByteArray): WrappedSecretKey {
            val serializerId = WrappedSecretKeySerializerManager.identifySerializer(data)
            SpanExtension.current().apply {
                setAttribute(
                    AttributeName.secret_key_wrapping_serializer_id.name,
                    serializerId.toLong()
                )
                setAttribute(
                    AttributeName.secret_key_size.name,
                    data.size.toLong()
                )
            }
            return measureAndRecordTelemetry {
                WrappedSecretKeySerializerManager
                    .getSerializer(serializerId)
                    .deserialize(data)
            }
        }

        /**
         * Executes an operation while measuring its duration and recording telemetry.
         *
         * Records the elapsed time to the specified attribute in the current span.
         *
         * @param T The return type of the operation
         * @param operation The operation to execute and measure
         * @return The result of the operation
         */
        private inline fun <T> measureAndRecordTelemetry(operation: () -> T): T {
            val startTime = System.nanoTime()
            val result = operation()
            val elapsedTime = System.nanoTime() - startTime
            SpanExtension.current().setAttribute(
                AttributeName.secret_key_serialization_duration.name,
                elapsedTime
            )
            return result
        }
    }
}
