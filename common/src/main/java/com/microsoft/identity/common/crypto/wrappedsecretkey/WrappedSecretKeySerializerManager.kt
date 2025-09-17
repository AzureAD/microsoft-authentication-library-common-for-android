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

/**
 * Manager for handling different IDs of [WrappedSecretKey] serialization formats.
 *
 * This object provides centralized management for serializing and deserializing wrapped secret keys
 * across different format IDs. It handles ID detection, serializer selection, and
 * maintains backward compatibility between legacy and modern formats.
 *
 * **Supported formats:**
 * - **ID 0 (Legacy)**: wrappedSecretKey data only, no metadata or header
 * - **ID 1 (Binary Stream)**: metadata with structured header
 * - **Future IDs**: Extensible design for new serialization formats
 *
 * **Header structure for new formats:**
 * ```
 * [Magic Bytes: 4 bytes][Serializer ID: 4 bytes][Metadata Length: 4 bytes][Metadata][wrappedSecretKey]
 *
 * @see IWrappedSecretKeySerializer
 * @see WrappedSecretKey
 * @see WrappedSecretKeyLegacySerializer
 * @see WrappedSecretKeyBinaryStreamSerializer
 */
object WrappedSecretKeySerializerManager {
    private const val TAG = "WrappedSecretKeySerializerManager"

    /**
     * Extracts the serializer ID from wrapped secret key data.
     *
     * Analyzes the byte array to determine which serialization format was used.
     * This enables automatic format detection and backward compatibility across different versions.
     *
     * **Detection logic:**
     * 1. Check if data contains metadata format magic bytes (0x00FF3CAB)
     * 2. If magic bytes found, extract serializer ID from header
     * 3. If no magic bytes or insufficient data, assume legacy format (ID 0)
     *
     * **Supported formats:**
     * - **ID 0**: Legacy format (raw wrapped key data only)
     * - **ID 1**: Binary stream with metadata header
     *
     * @param wrappedSecretKeyByteArray The serialized wrapped secret key data to analyze
     * @return The detected serializer ID (0 for legacy, 1+ for metadata formats)
     * @see WrappedSecretKeySerializerWithMetadata.getSerializerIdFromByteArray
     */
    fun identifySerializer(wrappedSecretKeyByteArray: ByteArray): Int {
        val methodTag = "$TAG:identifySerializer"
        val serializerId = WrappedSecretKeySerializerWithMetadata
            .getSerializerIdFromByteArray(wrappedSecretKeyByteArray)
        Logger.info(methodTag, "Detected serializer ID: $serializerId")
        return serializerId ?: WrappedSecretKeyLegacySerializer.ID // Legacy format
    }

    /**
     * Returns the appropriate serializer instance for the specified ID.
     *
     * Creates and returns a serializer that can handle the specified format ID.
     * This factory method ensures that the correct serialization strategy is used
     * for each supported format ID.
     *
     * **Supported IDs:**
     * - ID 0: [WrappedSecretKeyLegacySerializer] for legacy format
     * - ID 1: [WrappedSecretKeyBinaryStreamSerializer] for binary stream format
     *
     * @param serializerId The serialization format ID number
     * @return An [IWrappedSecretKeySerializer] instance capable of handling the specified ID
     * @throws IllegalArgumentException if the ID is not supported
     */
    fun getSerializer(serializerId: Int): IWrappedSecretKeySerializer {
        val methodTag = "$TAG:getSerializer"
        Logger.info(methodTag, "Getting serializer for ID: $serializerId")
        return when (serializerId) {
            WrappedSecretKeyBinaryStreamSerializer.ID -> WrappedSecretKeyBinaryStreamSerializer()
            WrappedSecretKeyLegacySerializer.ID -> WrappedSecretKeyLegacySerializer()
            else -> throw IllegalArgumentException("Unsupported serializer ID: $serializerId")
        }
    }
}