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
package com.microsoft.identity.common.crypto

import com.microsoft.identity.common.crypto.wrappedsecretkey.WrappedSecretKey
import com.microsoft.identity.common.crypto.wrappedsecretkey.WrappedSecretKeySerializerManager
import com.microsoft.identity.common.java.flighting.CommonFlight
import com.microsoft.identity.common.java.flighting.CommonFlightsManager
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer

class WrappedSecretKeyTest {

    private val testKeyBytes = "test-key-data-12345".toByteArray()
    private val testAlgorithm = "AES"
    private val testCipherTransformation = "RSA/ECB/PKCS1Padding"

    companion object {
        // New format constants matching the implementation
        private const val NEW_FORMAT_HEADER_IDENTIFIER = 0x00FF3C01
        private const val FORMAT_VERSION_1 = 1
    }

    @Before
    fun setUp() {
        mockkObject(CommonFlightsManager)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun constructorCreatesWrappedSecretKeyWithCorrectProperties() {
        val wrappedKey = WrappedSecretKey(testKeyBytes, testAlgorithm, testCipherTransformation)

        assertArrayEquals(testKeyBytes, wrappedKey.wrappedKeyData)
        assertEquals(testAlgorithm, wrappedKey.algorithm)
        assertEquals(testCipherTransformation, wrappedKey.cipherTransformation)
    }

    @Test
    fun serializeAndDeserializeWithNewFormatPreservesData() {
        val originalKey = WrappedSecretKey(testKeyBytes, testAlgorithm, testCipherTransformation)

        // Mock flight to enable new format (version 1)
        every { CommonFlightsManager.getFlightsProvider().getIntValue(CommonFlight.WRAPPED_SECRET_KEY_SERIALIZER_VERSION) } returns 1

        // Serialize and deserialize
        val serializedData = originalKey.serialize()
        val deserializedKey = WrappedSecretKey.deserialize(serializedData)

        assertArrayEquals(originalKey.wrappedKeyData, deserializedKey.wrappedKeyData)
        assertEquals(originalKey.algorithm, deserializedKey.algorithm)
        assertEquals(originalKey.cipherTransformation, deserializedKey.cipherTransformation)
    }

    @Test
    fun serializeAndDeserializeWithLegacyFormatPreservesKeyData() {
        val originalKey = WrappedSecretKey(testKeyBytes, testAlgorithm, testCipherTransformation)

        // Mock flight to use legacy format (version 0)
        every { CommonFlightsManager.getFlightsProvider().getIntValue(CommonFlight.WRAPPED_SECRET_KEY_SERIALIZER_VERSION) } returns 0

        // Serialize and deserialize
        val serializedData = originalKey.serialize()
        val deserializedKey = WrappedSecretKey.deserialize(serializedData)

        assertArrayEquals(originalKey.wrappedKeyData, deserializedKey.wrappedKeyData)
        assertEquals("AES", deserializedKey.algorithm) // Default value for legacy format
        assertEquals("RSA/ECB/PKCS1Padding", deserializedKey.cipherTransformation) // Default value for legacy format
    }

    @Test
    fun newFormatCreatesLargerDataThanLegacyFormat() {
        val wrappedKey = WrappedSecretKey(testKeyBytes, testAlgorithm, testCipherTransformation)

        // Test new format
        every { CommonFlightsManager.getFlightsProvider().getIntValue(CommonFlight.WRAPPED_SECRET_KEY_SERIALIZER_VERSION) } returns 1
        val newFormatData = wrappedKey.serialize()

        // Test legacy format
        every { CommonFlightsManager.getFlightsProvider().getIntValue(CommonFlight.WRAPPED_SECRET_KEY_SERIALIZER_VERSION) } returns 0
        val legacyFormatData = wrappedKey.serialize()

        assertTrue("New format should create larger data due to metadata",
                newFormatData.size > legacyFormatData.size)
        assertEquals("Legacy format should only contain key bytes",
                testKeyBytes.size, legacyFormatData.size)
    }

    @Test
    fun newFormatContainsValidBinaryStructure() {
        val wrappedKey = WrappedSecretKey(testKeyBytes, testAlgorithm, testCipherTransformation)

        // Mock flight to enable new format
        every { CommonFlightsManager.getFlightsProvider().getIntValue(CommonFlight.WRAPPED_SECRET_KEY_SERIALIZER_VERSION) } returns 1

        val serializedData = wrappedKey.serialize()

        // Read and verify the binary structure
        val buffer = ByteBuffer.wrap(serializedData)

        // Verify header identifier (4 bytes)
        val headerIdentifier = buffer.getInt()
        assertEquals("Header identifier should match expected value", NEW_FORMAT_HEADER_IDENTIFIER, headerIdentifier)

        // Verify metadata length (4 bytes)
        val metadataLength = buffer.getInt()
        assertTrue("Metadata length should be reasonable", metadataLength > 0 && metadataLength < serializedData.size)

        // Read and verify metadata
        val metadataBytes = ByteArray(metadataLength)
        buffer.get(metadataBytes)

        val metadata = JSONObject(String(metadataBytes, Charsets.UTF_8))
        assertEquals(testAlgorithm, metadata.getString("algorithm"))
        assertEquals(testCipherTransformation, metadata.getString("cipherTransformation"))
        assertEquals(FORMAT_VERSION_1, metadata.getInt("version"))
        assertEquals(testKeyBytes.size, metadata.getInt("keyDataLength"))

        // Verify remaining data is the key
        val remainingKeyData = ByteArray(buffer.remaining())
        buffer.get(remainingKeyData)
        assertArrayEquals(testKeyBytes, remainingKeyData)
    }

    @Test
    fun largeKeyDataIsHandledCorrectly() {
        val largeKeyData = ByteArray(8192) { it.toByte() } // 8KB test data
        val wrappedKey = WrappedSecretKey(largeKeyData, testAlgorithm, testCipherTransformation)

        // Mock flight to enable new format
        every { CommonFlightsManager.getFlightsProvider().getIntValue(CommonFlight.WRAPPED_SECRET_KEY_SERIALIZER_VERSION) } returns 1

        // Serialize and deserialize
        val serializedData = wrappedKey.serialize()
        val deserializedKey = WrappedSecretKey.deserialize(serializedData)

        assertArrayEquals(largeKeyData, deserializedKey.wrappedKeyData)
        assertEquals(testAlgorithm, deserializedKey.algorithm)
        assertEquals(testCipherTransformation, deserializedKey.cipherTransformation)
    }

    @Test
    fun legacyFormatDetectionWorksWithBinaryData() {
        val binaryData = ByteArray(256) { (it % 256).toByte() } // Random binary data

        // Deserialize raw binary data (simulating legacy format)
        val deserializedKey = WrappedSecretKey.deserialize(binaryData)

        assertArrayEquals(binaryData, deserializedKey.wrappedKeyData)
        assertEquals("AES", deserializedKey.algorithm)
        assertEquals("RSA/ECB/PKCS1Padding", deserializedKey.cipherTransformation)
    }

    @Test
    fun corruptedNewFormatFallsBackToLegacyFormat() {
        // Create invalid new format data (wrong header identifier)
        val invalidData = ByteBuffer.allocate(20)
                .putInt(0x12345678) // Wrong header identifier
                .putInt(100)        // Metadata length larger than available data
                .put("invalid".toByteArray())
                .array()

        val deserializedKey = WrappedSecretKey.deserialize(invalidData)

        assertArrayEquals(invalidData, deserializedKey.wrappedKeyData) // Should load as raw bytes (legacy format)
        assertEquals("AES", deserializedKey.algorithm)
        assertEquals("RSA/ECB/PKCS1Padding", deserializedKey.cipherTransformation)
    }

    @Test
    fun serializeUsesLegacyFormatWhenFlightIsZero() {
        val wrappedKey = WrappedSecretKey(testKeyBytes, testAlgorithm, testCipherTransformation)

        // Mock flight to use legacy format
        every { CommonFlightsManager.getFlightsProvider().getIntValue(CommonFlight.WRAPPED_SECRET_KEY_SERIALIZER_VERSION) } returns 0

        val serializedData = wrappedKey.serialize()

        // Verify legacy format: data should only contain raw key bytes
        assertArrayEquals(testKeyBytes, serializedData)
    }

    @Test
    fun serializeUsesNewFormatWhenFlightIsOne() {
        val wrappedKey = WrappedSecretKey(testKeyBytes, testAlgorithm, testCipherTransformation)

        // Mock flight to enable new format
        every { CommonFlightsManager.getFlightsProvider().getIntValue(CommonFlight.WRAPPED_SECRET_KEY_SERIALIZER_VERSION) } returns 1

        val serializedData = wrappedKey.serialize()

        // Verify new format: data should be larger than just key bytes
        assertTrue("New format data should be larger than raw key", serializedData.size > testKeyBytes.size)

        // Verify it can be deserialized correctly
        val deserializedKey = WrappedSecretKey.deserialize(serializedData)
        assertArrayEquals(testKeyBytes, deserializedKey.wrappedKeyData)
        assertEquals(testAlgorithm, deserializedKey.algorithm)
        assertEquals(testCipherTransformation, deserializedKey.cipherTransformation)
    }

    /**
     * Test backward compatibility: key serialized with new format can be read when flight is changed back.
     */
    @Test
    fun testBackwardCompatibility_NewFormatToLegacy() {
        val originalKey = WrappedSecretKey(testKeyBytes, testAlgorithm, testCipherTransformation)

        // Phase 1: Serialize with new format (version 1)
        every { CommonFlightsManager.getFlightsProvider().getIntValue(CommonFlight.WRAPPED_SECRET_KEY_SERIALIZER_VERSION) } returns 1
        val serializedData = originalKey.serialize()

        // Validate key is serialized with new format
        val buffer = ByteBuffer.wrap(serializedData)
        val headerIdentifier = buffer.getInt()
        assertEquals("Key should be serialized with new format", NEW_FORMAT_HEADER_IDENTIFIER, headerIdentifier)

        // Phase 2: Deserialize (automatic format detection should work regardless of flight value)
        val deserializedKey = WrappedSecretKey.deserialize(serializedData)
        assertArrayEquals("Key data should be preserved", originalKey.wrappedKeyData, deserializedKey.wrappedKeyData)
        assertEquals("Algorithm should be preserved from metadata", originalKey.algorithm, deserializedKey.algorithm)
        assertEquals("Cipher transformation should be preserved from metadata", originalKey.cipherTransformation, deserializedKey.cipherTransformation)
    }

    /**
     * Test forward compatibility: key serialized with legacy format can be read when flight is enabled.
     */
    @Test
    fun testForwardCompatibility_LegacyToNewFormat() {
        val originalKey = WrappedSecretKey(testKeyBytes, testAlgorithm, testCipherTransformation)

        // Phase 1: Serialize with legacy format (version 0)
        every { CommonFlightsManager.getFlightsProvider().getIntValue(CommonFlight.WRAPPED_SECRET_KEY_SERIALIZER_VERSION) } returns 0
        val serializedData = originalKey.serialize()

        // Validate key is serialized with legacy format
        assertArrayEquals("Key should be serialized with legacy format", testKeyBytes, serializedData)

        // Phase 2: Deserialize (should work regardless of current flight value)
        val deserializedKey = WrappedSecretKey.deserialize(serializedData)
        assertArrayEquals("Key data should be preserved", originalKey.wrappedKeyData, deserializedKey.wrappedKeyData)
        assertEquals("Should use default algorithm for legacy format", "AES", deserializedKey.algorithm)
        assertEquals("Should use default cipher transformation for legacy format", "RSA/ECB/PKCS1Padding", deserializedKey.cipherTransformation)
    }

    /**
     * Test that validates serialized data uses new format structure.
     */
    @Test
    fun validateNewFormatStructure() {
        val wrappedKey = WrappedSecretKey(testKeyBytes, testAlgorithm, testCipherTransformation)

        // Enable new format
        every { CommonFlightsManager.getFlightsProvider().getIntValue(CommonFlight.WRAPPED_SECRET_KEY_SERIALIZER_VERSION) } returns 1

        val serializedData = wrappedKey.serialize()

        // Validate the structure manually
        val buffer = ByteBuffer.wrap(serializedData)

        // Check header identifier
        val headerIdentifier = buffer.getInt()
        assertEquals("New format should have correct header identifier", NEW_FORMAT_HEADER_IDENTIFIER, headerIdentifier)

        // Check metadata
        val metadataLength = buffer.getInt()
        val metadataBytes = ByteArray(metadataLength)
        buffer.get(metadataBytes)
        val metadata = JSONObject(String(metadataBytes, Charsets.UTF_8))

        assertTrue("Metadata should contain algorithm", metadata.has("algorithm"))
        assertTrue("Metadata should contain cipherTransformation", metadata.has("cipherTransformation"))
        assertTrue("Metadata should contain version", metadata.has("version"))
        assertTrue("Metadata should contain keyDataLength", metadata.has("keyDataLength"))

        assertEquals("Algorithm should match", testAlgorithm, metadata.getString("algorithm"))
        assertEquals("Cipher transformation should match", testCipherTransformation, metadata.getString("cipherTransformation"))
        assertEquals("Version should be 1", FORMAT_VERSION_1, metadata.getInt("version"))
        assertEquals("Key data length should match", testKeyBytes.size, metadata.getInt("keyDataLength"))
    }

    /**
     * Test that validates serialized data uses legacy format structure.
     */
    @Test
    fun validateLegacyFormatStructure() {
        val wrappedKey = WrappedSecretKey(testKeyBytes, testAlgorithm, testCipherTransformation)

        // Use legacy format
        every { CommonFlightsManager.getFlightsProvider().getIntValue(CommonFlight.WRAPPED_SECRET_KEY_SERIALIZER_VERSION) } returns 0

        val serializedData = wrappedKey.serialize()

        // Validate contains only raw key bytes
        assertArrayEquals("Legacy format should contain only raw key bytes", testKeyBytes, serializedData)
        assertEquals("Legacy format size should equal key size", testKeyBytes.size, serializedData.size)
    }

    /**
     * Test version detection from serialized data.
     */
    @Test
    fun testVersionDetection() {
        val wrappedKey = WrappedSecretKey(testKeyBytes, testAlgorithm, testCipherTransformation)

        // Test new format version detection
        every { CommonFlightsManager.getFlightsProvider().getIntValue(CommonFlight.WRAPPED_SECRET_KEY_SERIALIZER_VERSION) } returns 1
        val newFormatData = wrappedKey.serialize()
        val detectedNewVersion = WrappedSecretKeySerializerManager.getVersion(newFormatData)
        assertEquals("Should detect version 1 for new format", 1, detectedNewVersion)

        // Test legacy format version detection
        every { CommonFlightsManager.getFlightsProvider().getIntValue(CommonFlight.WRAPPED_SECRET_KEY_SERIALIZER_VERSION) } returns 0
        val legacyFormatData = wrappedKey.serialize()
        val detectedLegacyVersion = WrappedSecretKeySerializerManager.getVersion(legacyFormatData)
        assertEquals("Should detect version 0 for legacy format", 0, detectedLegacyVersion)

        // Test raw binary data (should be detected as legacy)
        val rawData = "random-binary-data".toByteArray()
        val detectedRawVersion = WrappedSecretKeySerializerManager.getVersion(rawData)
        assertEquals("Should detect version 0 for raw data", 0, detectedRawVersion)
    }

    /**
     * Test edge case where metadata has corrupted length in new format.
     */
    @Test(expected = NegativeArraySizeException::class)
    fun corruptedMetadataLengthThrowsException() {
        // Create new format with corrupted metadata length
        val corruptedData = ByteBuffer.allocate(16)
                .putInt(NEW_FORMAT_HEADER_IDENTIFIER) // Correct header identifier
                .putInt(-1)                           // Invalid metadata length (negative)
                .put("test".toByteArray())
                .array()

        // Should throw exception because it detects new format but has invalid metadata length
        WrappedSecretKey.deserialize(corruptedData)
    }

    /**
     * Test edge case where metadata length exceeds remaining data.
     */
    @Test(expected = BufferUnderflowException::class)
    fun metadataLengthLargerThanRemainingDataThrowsException() {
        // Create new format with metadata length larger than remaining data
        val corruptedData = ByteBuffer.allocate(16)
                .putInt(NEW_FORMAT_HEADER_IDENTIFIER) // Correct header identifier
                .putInt(1000)                         // Metadata length larger than remaining data
                .put("test".toByteArray())
                .array()

        // Should throw exception because metadata length exceeds remaining buffer
        WrappedSecretKey.deserialize(corruptedData)
    }

    /**
     * Test very small data handling (less than header size).
     */
    @Test
    fun smallDataIsDetectedAsLegacyFormat() {
        val smallData = "test".toByteArray() // Less than 8 bytes needed for header

        val deserializedKey = WrappedSecretKey.deserialize(smallData)

        assertArrayEquals("Should load as raw bytes", smallData, deserializedKey.wrappedKeyData)
        assertEquals("Should use default algorithm", "AES", deserializedKey.algorithm)
        assertEquals("Should use default cipher transformation", "RSA/ECB/PKCS1Padding", deserializedKey.cipherTransformation)
    }
}
