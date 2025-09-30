//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.common.crypto.wrappedsecretkey

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for [WrappedSecretKeySerializerManager].
 *
 * Tests the manager's ability to identify serializer IDs from byte data
 * and return appropriate serializer instances without using mocks.
 */
class WrappedSecretKeySerializerManagerTest {

    companion object {
        private const val TEST_ALGORITHM = "AES"
        private const val TEST_CIPHER_TRANSFORMATION = "RSA/ECB/PKCS1Padding"
        private val TEST_KEY_DATA = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)
    }

    private fun createTestWrappedSecretKey(): WrappedSecretKey {
        return WrappedSecretKey(
            wrappedKeyData = TEST_KEY_DATA,
            algorithm = TEST_ALGORITHM,
            cipherTransformation = TEST_CIPHER_TRANSFORMATION
        )
    }

    // ===== Tests for identifySerializer =====

    @Test
    fun `identifySerializer returns legacy ID for legacy format data`() {
        val legacyData = TEST_KEY_DATA // Raw key data without headers

        val serializerId = WrappedSecretKeySerializerManager.identifySerializer(legacyData)

        assertEquals(WrappedSecretKeyLegacySerializer.ID, serializerId)
    }

    @Test
    fun `identifySerializer returns legacy ID for empty data`() {
        val emptyData = byteArrayOf()

        val serializerId = WrappedSecretKeySerializerManager.identifySerializer(emptyData)

        assertEquals(WrappedSecretKeyLegacySerializer.ID, serializerId)
    }

    @Test
    fun `identifySerializer returns legacy ID for data too small for header`() {
        val smallData = byteArrayOf(1, 2, 3) // Less than minimum header size

        val serializerId = WrappedSecretKeySerializerManager.identifySerializer(smallData)

        assertEquals(WrappedSecretKeyLegacySerializer.ID, serializerId)
    }

    @Test
    fun `identifySerializer correctly identifies binary stream serializer format`() {
        val testKey = createTestWrappedSecretKey()
        val streamSerializer = WrappedSecretKeyBinaryStreamSerializer()
        val serializedData = streamSerializer.serialize(testKey)

        val serializerId = WrappedSecretKeySerializerManager.identifySerializer(serializedData)

        assertEquals(WrappedSecretKeyBinaryStreamSerializer.ID, serializerId)
    }

    @Test
    fun `identifySerializer handles corrupted header gracefully`() {
        val corruptedHeader = byteArrayOf(
            0x00, 0xFF.toByte(), 0x3C, 0x00, // Invalid header (missing 0xAB)
            0x00, 0x00, 0x00, 0x01, // Serializer ID
            0x00, 0x00, 0x00, 0x10, // Metadata length
            // ... rest would be metadata and key data
        )

        val serializerId = WrappedSecretKeySerializerManager.identifySerializer(corruptedHeader)

        assertEquals(WrappedSecretKeyLegacySerializer.ID, serializerId)
    }

    // ===== Tests for getSerializer =====

    @Test
    fun `getSerializer returns legacy serializer for ID 0`() {
        val serializer = WrappedSecretKeySerializerManager.getSerializer(WrappedSecretKeyLegacySerializer.ID)

        assertTrue(serializer is WrappedSecretKeyLegacySerializer)
        assertEquals(WrappedSecretKeyLegacySerializer.ID, serializer.id)
    }

    @Test
    fun `getSerializer returns binary stream serializer for ID 1`() {
        val serializer = WrappedSecretKeySerializerManager.getSerializer(WrappedSecretKeyBinaryStreamSerializer.ID)

        assertTrue(serializer is WrappedSecretKeyBinaryStreamSerializer)
        assertEquals(WrappedSecretKeyBinaryStreamSerializer.ID, serializer.id)
    }

    @Test
    fun `getSerializer throws IllegalArgumentException for unsupported ID`() {
        val unsupportedId = 999

        val exception = assertThrows(IllegalArgumentException::class.java) {
            WrappedSecretKeySerializerManager.getSerializer(unsupportedId)
        }

        assertTrue(exception.message!!.contains("Unsupported serializer ID: $unsupportedId"))
    }

    @Test
    fun `getSerializer throws IllegalArgumentException for negative ID`() {
        val negativeId = -1

        val exception = assertThrows(IllegalArgumentException::class.java) {
            WrappedSecretKeySerializerManager.getSerializer(negativeId)
        }

        assertTrue(exception.message!!.contains("Unsupported serializer ID: $negativeId"))
    }

    // ===== Integration Tests =====

    @Test
    fun `round trip serialization with legacy format works correctly`() {
        val originalKey = createTestWrappedSecretKey()
        val legacySerializer = WrappedSecretKeySerializerManager.getSerializer(WrappedSecretKeyLegacySerializer.ID)

        // Serialize
        val serializedData = legacySerializer.serialize(originalKey)

        // Identify format
        val detectedId = WrappedSecretKeySerializerManager.identifySerializer(serializedData)
        assertEquals(WrappedSecretKeyLegacySerializer.ID, detectedId)

        // Deserialize
        val retrievedSerializer = WrappedSecretKeySerializerManager.getSerializer(detectedId)
        val deserializedKey = retrievedSerializer.deserialize(serializedData)

        // Verify (note: legacy format uses default metadata)
        assertArrayEquals(originalKey.wrappedKeyData, deserializedKey.wrappedKeyData)
        assertEquals("AES", deserializedKey.algorithm) // Default from legacy serializer
        assertEquals("RSA/ECB/PKCS1Padding", deserializedKey.cipherTransformation) // Default from legacy serializer
    }

    @Test
    fun `round trip serialization with binary stream format preserves metadata`() {
        val originalKey = createTestWrappedSecretKey()
        val streamSerializer = WrappedSecretKeySerializerManager.getSerializer(WrappedSecretKeyBinaryStreamSerializer.ID)

        // Serialize
        val serializedData = streamSerializer.serialize(originalKey)

        // Identify format
        val detectedId = WrappedSecretKeySerializerManager.identifySerializer(serializedData)
        assertEquals(WrappedSecretKeyBinaryStreamSerializer.ID, detectedId)

        // Deserialize
        val retrievedSerializer = WrappedSecretKeySerializerManager.getSerializer(detectedId)
        val deserializedKey = retrievedSerializer.deserialize(serializedData)

        // Verify complete preservation
        assertArrayEquals(originalKey.wrappedKeyData, deserializedKey.wrappedKeyData)
        assertEquals(originalKey.algorithm, deserializedKey.algorithm)
        assertEquals(originalKey.cipherTransformation, deserializedKey.cipherTransformation)
    }

    @Test
    fun `manager correctly handles different key sizes`() {
        val largeKeyData = ByteArray(1024) { it.toByte() }
        val largeKey = WrappedSecretKey(largeKeyData, TEST_ALGORITHM, TEST_CIPHER_TRANSFORMATION)

        // Test with binary stream format
        val streamSerializer = WrappedSecretKeySerializerManager.getSerializer(WrappedSecretKeyBinaryStreamSerializer.ID)
        val serializedData = streamSerializer.serialize(largeKey)

        val detectedId = WrappedSecretKeySerializerManager.identifySerializer(serializedData)
        assertEquals(WrappedSecretKeyBinaryStreamSerializer.ID, detectedId)

        val retrievedSerializer = WrappedSecretKeySerializerManager.getSerializer(detectedId)
        val deserializedKey = retrievedSerializer.deserialize(serializedData)

        assertArrayEquals(largeKey.wrappedKeyData, deserializedKey.wrappedKeyData)
    }

    @Test
    fun `manager handles special characters in metadata correctly`() {
        val specialKey = WrappedSecretKey(
            wrappedKeyData = TEST_KEY_DATA,
            algorithm = "AES-256-GCM",
            cipherTransformation = "AES/GCM/NoPadding"
        )

        // Test with binary stream format
        val serializer = WrappedSecretKeySerializerManager.getSerializer(WrappedSecretKeyBinaryStreamSerializer.ID)
        val serializedData = serializer.serialize(specialKey)

        val detectedId = WrappedSecretKeySerializerManager.identifySerializer(serializedData)
        assertEquals(WrappedSecretKeyBinaryStreamSerializer.ID, detectedId)

        val retrievedSerializer = WrappedSecretKeySerializerManager.getSerializer(detectedId)
        val deserializedKey = retrievedSerializer.deserialize(serializedData)

        assertEquals(specialKey.algorithm, deserializedKey.algorithm)
        assertEquals(specialKey.cipherTransformation, deserializedKey.cipherTransformation)
    }

    // ===== Edge Case Tests =====

    @Test
    fun `identifySerializer handles minimum valid header size`() {
        // Create minimum valid header (12 bytes: header + serializer ID + metadata length)
        val minimalHeader = byteArrayOf(
            0x00, 0xFF.toByte(), 0x3C, 0xAB.toByte(), // Valid header
            0x00, 0x00, 0x00, 0x01, // Serializer ID = 1
            0x00, 0x00, 0x00, 0x00, // Metadata length = 0
            // No actual metadata or key data
        )

        val serializerId = WrappedSecretKeySerializerManager.identifySerializer(minimalHeader)

        assertEquals(1, serializerId) // Should detect ID 1 from header
    }

    @Test
    fun `getSerializer creates new instances each time`() {
        val serializer1 = WrappedSecretKeySerializerManager.getSerializer(WrappedSecretKeyBinaryStreamSerializer.ID)
        val serializer2 = WrappedSecretKeySerializerManager.getSerializer(WrappedSecretKeyBinaryStreamSerializer.ID)

        assertNotSame(serializer1, serializer2) // Different instances
        assertEquals(serializer1.id, serializer2.id) // Same ID
        assertEquals(serializer1::class, serializer2::class) // Same type
    }
}
