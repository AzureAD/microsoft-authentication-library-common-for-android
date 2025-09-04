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

import com.microsoft.identity.common.crypto.wrappedsecretkey.WrappedSecretKeySerializerManager.HEADER_ID_FIELD_SIZE_BYTES
import com.microsoft.identity.common.crypto.wrappedsecretkey.WrappedSecretKeySerializerManager.METADATA_LENGTH_FIELD_SIZE_BYTES
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.logging.Logger
import org.json.JSONObject
import java.nio.ByteBuffer

/**
 * JSON-based serializer for [WrappedSecretKey] that stores metadata in JSON format.
 *
 * This serializer implements version 1 of the wrapped secret key format, storing metadata
 * as JSON alongside the raw wrapped key data. This format provides better structure and
 * extensibility compared to the legacy format while maintaining human-readable metadata.
 *
 * **Format structure:**
 * ```
 * [Header ID: 4 bytes][Metadata Length: 4 bytes][JSON Metadata][Raw Key Data]
 * ```
 *
 * **JSON Metadata fields:**
 * - `algorithm`: The key algorithm (e.g., "AES")
 * - `cipherTransformation`: The cipher transformation used (e.g., "RSA/ECB/PKCS1Padding")
 * - `version`: Format version (1 for this serializer)
 * - `keyDataLength`: Length of the raw key data for validation
 *
 * **Format characteristics:**
 * - Version: 1
 * - Header identifier: 0x00FF3C01
 * - Metadata encoding: UTF-8 JSON
 * - Supports backward compatibility validation
 * - Human-readable metadata for debugging
 *
 * This serializer bridges the gap between the legacy format (version 0) and more
 * advanced binary formats, providing structured metadata while maintaining readability.
 *
 * @see IWrappedSecretKeySerializer
 * @see WrappedSecretKey
 * @see WrappedSecretKeyLegacySerializer
 */
class WrappedSecretKeyJsonObjectSerializer: IWrappedSecretKeySerializer {

    companion object {
        private const val TAG = "WrappedSecretKeyJsonObjectSerializer"

        /** Version identifier for the JSON-based format */
        const val VERSION = 1

        /** Header identifier used to recognize this format */
        private const val NEW_FORMAT_HEADER_IDENTIFIER = 0x00FF3C01
    }

    /**
     * Serializes a [WrappedSecretKey] to the JSON metadata format.
     *
     * Creates a structured binary format that includes a header identifier, metadata length,
     * JSON-encoded metadata, and the raw wrapped key data. The metadata includes algorithm,
     * cipher transformation, version, and key data length for validation purposes.
     *
     * @param wrappedSecretKey The wrapped secret key to serialize
     * @return Serialized byte array in the JSON metadata format
     */
    override fun serialize(wrappedSecretKey: WrappedSecretKey): ByteArray {
        // New format: Store metadata header + raw key data
        val metadata = JSONObject().apply {
            put("algorithm", wrappedSecretKey.algorithm)
            put("cipherTransformation", wrappedSecretKey.cipherTransformation)
            put("version", VERSION)
            put("keyDataLength", wrappedSecretKey.wrappedKeyData.size)
        }

        val metadataBytes = metadata.toString().toByteArray(Charsets.UTF_8)

        // Use ByteBuffer for cleaner header writing
        val bufferSize = HEADER_ID_FIELD_SIZE_BYTES + METADATA_LENGTH_FIELD_SIZE_BYTES +
                metadataBytes.size + wrappedSecretKey.wrappedKeyData.size
        return ByteBuffer.allocate(bufferSize)
            .putInt(NEW_FORMAT_HEADER_IDENTIFIER)  // Write header length (4 bytes, big-endian)
            .putInt(metadataBytes.size)  // Write metadata length (4 bytes, big-endian)
            .put(metadataBytes)          // Write metadata
            .put(wrappedSecretKey.wrappedKeyData)         // Write raw key data
            .array()
    }

    /**
     * Deserializes byte data to a [WrappedSecretKey] from the JSON metadata format.
     *
     * Parses the structured binary format to extract the JSON metadata and raw key data.
     * The method validates the metadata length and key data length for integrity checking.
     *
     * **Data format expected:**
     * 1. Header identifier (4 bytes) - already validated by caller
     * 2. Metadata length (4 bytes) - indicates JSON metadata size
     * 3. JSON metadata (variable) - UTF-8 encoded JSON with key information
     * 4. Raw key data (remaining bytes) - the actual wrapped key bytes
     *
     * @param data The serialized binary data containing header, metadata, and key data
     * @return [WrappedSecretKey] instance reconstructed from the serialized data
     * @throws ClientException if the data format is invalid, JSON parsing fails, or validation errors occur
     */
    override fun deserialize(data: ByteArray): WrappedSecretKey {
        val methodTag = "$TAG:loadFromNewFormat"
        Logger.info(methodTag, "Loading key using JSON metadata format")
        val buffer = ByteBuffer.wrap(data)

        // Skip header identifier (already validated in isNewFormat)
        buffer.getInt()

        // Read metadata length
        val metadataLength = buffer.getInt()

        // Extract and parse JSON metadata
        val metadataBytes = ByteArray(metadataLength)
        buffer.get(metadataBytes)

        val jsonString = String(metadataBytes, Charsets.UTF_8)
        val json = JSONObject(jsonString)
        val algorithm = json.getString("algorithm")
        val cipherTransformation = json.getString("cipherTransformation")
        val keyDataLength = json.getInt("keyDataLength")

        // Validate key data length
        if (keyDataLength != buffer.remaining()) {
            Logger.warn(
                methodTag,
                "Key data length mismatch. Expected: $keyDataLength, Actual: ${buffer.remaining()}"
            )
        }

        val keyBytes = ByteArray(buffer.remaining())
        buffer.get(keyBytes)

        Logger.verbose(
            methodTag,
            "Successfully loaded key with algorithm: $algorithm, transformation: $cipherTransformation"
        )
        return WrappedSecretKey(keyBytes, algorithm, cipherTransformation)
    }

    /**
     * Returns the version identifier for this serializer.
     *
     * @return Version 1, indicating the JSON metadata format
     */
    override fun getVersion(): Int {
        return VERSION
    }
}
