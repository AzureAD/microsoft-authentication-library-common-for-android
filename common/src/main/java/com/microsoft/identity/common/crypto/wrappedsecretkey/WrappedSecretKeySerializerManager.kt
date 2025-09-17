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
 * [Header ID: 4 bytes][Serializer ID: 4 bytes][Metadata Length: 4 bytes][Metadata][wrappedSecretKey]
 * ```
 *
 * **ID encoding:**
 * The header identifier uses the first 3 bytes (0x00FF3CAB) as a format identifier,
 * with the serializer ID stored separately, allowing for 256 different format IDs.
 *
 * **ID detection algorithm:**
 * 1. Check if data has minimum header size
 * 2. Extract header identifier (first 4 bytes)
 * 3. Compare first 3 bytes against known format identifier
 * 4. Extract serializer ID from header, or assume legacy format (ID 0)
 *
 * @see IWrappedSecretKeySerializer
 * @see WrappedSecretKey
 * @see WrappedSecretKeyLegacySerializer
 * @see WrappedSecretKeyBinaryStreamSerializer
 */
object WrappedSecretKeySerializerManager {
    private const val TAG = "WrappedSecretKeySerializerManager"

    /**
     * Extracts the serializer ID from the header of wrapped key data.
     *
     * The ID is stored in the header identifier of metadata format data.
     * This allows for backward compatibility when introducing new serialization formats.
     *
     * **ID detection logic:**
     * 1. If data is too small for a header, assume legacy format (ID 0)
     * 2. Extract the 4-byte header identifier
     * 3. Use bit masking to compare only the first 3 bytes against the known format identifier
     * 4. If matched, extract the ID from the header; otherwise, return legacy ID
     *
     * @param wrappedSecretKeyByteArray The byte array to inspect
     * @return The serializer ID (0-255) if the data uses the new format, 0 if legacy format or invalid data
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