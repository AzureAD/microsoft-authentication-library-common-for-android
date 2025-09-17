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

import com.microsoft.identity.common.logging.Logger
import java.nio.ByteBuffer

/**
 * Abstract base class for wrapped secret key serializers that include metadata in their format.
 *
 * This class provides the common infrastructure for serialization formats that store metadata
 * alongside the wrapped key data. It handles the standard header structure, format detection,
 * and provides a template method pattern for concrete serializers to implement their specific
 * metadata encoding strategies.
 *
 * **Binary format structure:**
 * ```
 * [Header ID: 4 bytes][Serializer ID: 4 bytes][Metadata Length: 4 bytes][Metadata][Raw Key Data]
 * ```
 *
 * **Header components:**
 * - **Header ID**: Fixed identifier (0x00FF3CAB) for format recognition
 * - **Serializer ID**: Unique identifier for the specific serializer implementation
 * - **Metadata Length**: Size of the metadata section in bytes
 * - **Metadata**: Format-specific metadata (JSON, binary, etc.)
 * - **Raw Key Data**: The actual wrapped secret key bytes
 *
 * @see IWrappedSecretKeySerializer
 * @see WrappedSecretKeySerializerManager
 */

abstract class WrappedSecretKeySerializerWithMetadata: IWrappedSecretKeySerializer {

    companion object {
        const val TAG = "WrappedSecretKeySerializerWithMetadata"

        /** Size in bytes for the header identifier field */
        const val HEADER_ID_FIELD_SIZE_BYTES = Int.SIZE_BYTES

        /** Size in bytes for the metadata ID field */
        const val METADATA_ID_SIZE_BYTES = Int.SIZE_BYTES

        /** Size in bytes for the metadata length field */
        const val METADATA_LENGTH_FIELD_SIZE_BYTES = Int.SIZE_BYTES

        /** Base header identifier for new format */
        const val NEW_FORMAT_HEADER_IDENTIFIER = 0x00FF3CAB

        /**
         * Determines if the byte array was serialized with metadata format.
         *
         * Checks if the data contains the expected header structure and identifier
         * to distinguish between legacy format (raw key data only) and metadata
         * format (header + metadata + key data).
         *
         * @param wrappedSecretKeyByteArray The serialized data to examine
         * @return true if the data uses metadata format, false if legacy format
         */
        private fun isSerializedWithMetadata(wrappedSecretKeyByteArray: ByteArray): Boolean {
            val methodTag = "$TAG:isNewFormat"
            if ((wrappedSecretKeyByteArray.size <
                        HEADER_ID_FIELD_SIZE_BYTES +
                        METADATA_ID_SIZE_BYTES +
                        METADATA_LENGTH_FIELD_SIZE_BYTES)) {
                Logger.warn(methodTag, "Data too small to contain header, assuming legacy format")
                return false
            }
            val headerId = ByteBuffer.wrap(wrappedSecretKeyByteArray, 0, HEADER_ID_FIELD_SIZE_BYTES).int
            return headerId == NEW_FORMAT_HEADER_IDENTIFIER
        }

        /**
         * Extracts the serializer ID from metadata format byte array.
         *
         * Reads the serializer ID from the header section of metadata format data.
         * This ID is used by the [WrappedSecretKeySerializerManager] to select
         * the appropriate deserializer for the data.
         *
         * @param wrappedSecretKeyByteArray The serialized data to examine
         * @return The serializer ID if metadata format is detected, null if legacy format
         */
        fun getSerializerIdFromByteArray(wrappedSecretKeyByteArray: ByteArray): Int? {
            if (!isSerializedWithMetadata(wrappedSecretKeyByteArray)) {
                return null
            }
            return ByteBuffer.wrap(wrappedSecretKeyByteArray, HEADER_ID_FIELD_SIZE_BYTES, HEADER_ID_FIELD_SIZE_BYTES).int
        }
    }

    /**
     * Serializes the metadata portion of a wrapped secret key.
     *
     * Concrete implementations must provide format-specific metadata serialization.
     * The metadata should include all information necessary to reconstruct the
     * cryptographic context (algorithm, cipher transformation, etc.).
     *
     * @param wrappedSecretKey The wrapped secret key containing metadata to serialize
     * @return The serialized metadata as a byte array
     */
    abstract fun serializeMetadata(wrappedSecretKey: WrappedSecretKey): ByteArray

    /**
     * Deserializes metadata from its byte array representation.
     *
     * Concrete implementations must provide format-specific metadata deserialization
     * that corresponds to their [serializeMetadata] implementation.
     *
     * @param metadataByteArray The serialized metadata bytes
     * @return The deserialized metadata object
     * @throws Exception if metadata format is invalid or corrupted
     */
    abstract fun deserializeMetadata(metadataByteArray: ByteArray): WrappedSecretKeyMetadata

    override fun serialize(wrappedSecretKey: WrappedSecretKey): ByteArray {

        val metadataBytes = serializeMetadata(wrappedSecretKey)

        // Use ByteBuffer for cleaner header writing
        val bufferSize = HEADER_ID_FIELD_SIZE_BYTES +
                METADATA_LENGTH_FIELD_SIZE_BYTES +
                METADATA_ID_SIZE_BYTES +
                metadataBytes.size +
                wrappedSecretKey.wrappedKeyData.size
        return ByteBuffer.allocate(bufferSize)
            .putInt(NEW_FORMAT_HEADER_IDENTIFIER)  // Write header length (4 bytes, big-endian)
            .putInt(id)
            .putInt(metadataBytes.size)  // Write metadata length (4 bytes, big-endian)
            .put(metadataBytes)          // Write metadata
            .put(wrappedSecretKey.wrappedKeyData)         // Write raw key data
            .array()
    }

    override fun deserialize(wrappedSecretKeyByteArray: ByteArray): WrappedSecretKey {
        val methodTag = "$TAG:loadFromNewFormat"
        Logger.info(methodTag, "Loading key using JSON metadata format")
        val buffer = ByteBuffer.wrap(wrappedSecretKeyByteArray)

        // Skip header identifier (already validated in isNewFormat)
        buffer.getInt()

        // Read metadata length
        val metadataLength = buffer.getInt()

        // Extract and parse JSON metadata
        val metadataBytes = ByteArray(metadataLength)
        buffer.get(metadataBytes)

        val metadata = deserializeMetadata(metadataBytes)


        // Validate key data length
        if (metadata.keyLength != buffer.remaining()) {
            Logger.warn(
                methodTag,
                "Key data length mismatch. Expected: $metadata.keyLength, Actual: ${buffer.remaining()}"
            )
        }

        val keyBytes = ByteArray(buffer.remaining())
        buffer.get(keyBytes)

        Logger.verbose(
            methodTag,
            "Successfully loaded key with algorithm: ${metadata.algorithm}, transformation: ${metadata.cipherTransformation}"
        )
        return WrappedSecretKey(keyBytes, metadata.algorithm, metadata.cipherTransformation)
    }
}