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

import java.nio.ByteBuffer

/**
 * Manager for handling different versions of [WrappedSecretKey] serialization formats.
 *
 * This object provides centralized management for serializing and deserializing wrapped secret keys
 * across different format versions. It handles version detection, serializer selection, and
 * maintains backward compatibility between legacy and modern formats.
 *
 * **Supported formats:**
 * - **Version 0 (Legacy)**: Raw key data only, no metadata or header
 * - **Version 1 (JSON)**: Header + JSON metadata + raw key data
 * - **Future versions**: Extensible design for new serialization formats
 *
 * **Header structure for new formats:**
 * ```
 * [Header ID: 4 bytes][Metadata Length: 4 bytes][Metadata][Raw Key Data]
 * ```
 *
 * **Version encoding:**
 * The header identifier uses the first 3 bytes (0x00FF3C) as a format identifier,
 * with the last byte encoding the version number (0x00-0xFF), allowing for 256 different versions.
 *
 * **Version detection algorithm:**
 * 1. Check if data has minimum header size
 * 2. Extract header identifier (first 4 bytes)
 * 3. Mask and compare first 3 bytes against known format identifier
 * 4. Extract version from last byte, or assume legacy format (version 0)
 *
 * @see IWrappedSecretKeySerializer
 * @see WrappedSecretKey
 * @see WrappedSecretKeyLegacySerializer
 * @see WrappedSecretKeyJsonObjectSerializer
 */
object WrappedSecretKeySerializerManager {
    /** Size in bytes for the header identifier field */
    const val HEADER_ID_FIELD_SIZE_BYTES = Int.SIZE_BYTES

    /** Size in bytes for the metadata length field */
    const val METADATA_LENGTH_FIELD_SIZE_BYTES = Int.SIZE_BYTES

    /** Base header identifier for new format (first 3 bytes), with version byte as 0x00 */
    private const val NEW_FORMAT_HEADER_IDENTIFIER = 0x00FF3C00

    /** Mask to isolate the first 3 bytes of the header for format identification */
    private const val NEW_FORMAT_HEADER_MASK = 0xFFFFFF00 // Mask to compare only first 3 bytes

    /** Mask to extract the version byte (last byte) from the header identifier */
    private const val VERSION_BYTE_MASK = 0x000000FF

    /**
     * Extracts the serializer version from the header of wrapped key data.
     *
     * The version is stored in the last byte of the 4-byte header identifier.
     * This allows for backward compatibility when introducing new serialization formats.
     *
     * **Version detection logic:**
     * 1. If data is too small for a header, assume legacy format (version 0)
     * 2. Extract the 4-byte header identifier
     * 3. Use bit masking to compare only the first 3 bytes against the known format identifier
     * 4. If matched, extract the version from the last byte; otherwise, return legacy version
     *
     * @param rawData The raw bytes read from file containing the wrapped key data
     * @return The version number (0-255) if the data uses the new format, 0 if legacy format or invalid data
     */
    fun getVersion(rawData: ByteArray): Int {
        if ((rawData.size < HEADER_ID_FIELD_SIZE_BYTES + METADATA_LENGTH_FIELD_SIZE_BYTES)) {
            return WrappedSecretKeyLegacySerializer.VERSION
        }
        val buffer = ByteBuffer.wrap(rawData)
        val headerValue = buffer.getInt()
        // Mask out the version byte (last byte) and compare only the first 3 bytes
        return if ((headerValue and NEW_FORMAT_HEADER_MASK.toInt()) == NEW_FORMAT_HEADER_IDENTIFIER) {
            headerValue and VERSION_BYTE_MASK // Return the version byte
        } else {
            WrappedSecretKeyLegacySerializer.VERSION // Legacy format
        }
    }

    /**
     * Returns the appropriate serializer instance for the specified version.
     *
     * Creates and returns a serializer that can handle the specified format version.
     * This factory method ensures that the correct serialization strategy is used
     * for each supported format version.
     *
     * **Supported versions:**
     * - Version 0: [WrappedSecretKeyLegacySerializer] for legacy format
     * - Version 1: [WrappedSecretKeyJsonObjectSerializer] for JSON metadata format
     *
     * @param version The serialization format version number
     * @return An [IWrappedSecretKeySerializer] instance capable of handling the specified version
     * @throws IllegalArgumentException if the version is not supported
     */
    fun getSerializer(version: Int): IWrappedSecretKeySerializer {
        return when (version) {
            WrappedSecretKeyJsonObjectSerializer.VERSION -> WrappedSecretKeyJsonObjectSerializer()
            WrappedSecretKeyLegacySerializer.VERSION -> WrappedSecretKeyLegacySerializer()
            else -> throw IllegalArgumentException("Unsupported WrappedSecretKey version: $version")
        }
    }
}