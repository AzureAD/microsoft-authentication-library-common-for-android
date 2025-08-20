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

import com.microsoft.identity.common.java.flighting.CommonFlight
import com.microsoft.identity.common.java.flighting.CommonFlightsManager
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.ByteBuffer

class WrappedSecretKeyTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testKeyBytes = "test-key-data-12345".toByteArray()
    private val testAlgorithm = "AES"
    private val testCipherTransformation = "RSA/ECB/PKCS1Padding"

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

        assertArrayEquals(testKeyBytes, wrappedKey.byteArray)
        assertEquals(testAlgorithm, wrappedKey.algorithm)
        assertEquals(testCipherTransformation, wrappedKey.cipherTransformation)
    }

    @Test
    fun loadFromFileReturnsNullWhenFileDoesNotExist() {
        val nonExistentFile = File("non-existent-file.dat")

        val result = WrappedSecretKey.loadFromFile(nonExistentFile, 1024)

        assertNull(result)
    }

    @Test
    fun loadFromFileReturnsNullWhenFileIsEmpty() {
        val testFile = tempFolder.newFile("empty-file.dat")

        val result = WrappedSecretKey.loadFromFile(testFile, 1024)

        assertNull(result)
    }

    @Test
    fun roundTripStoreAndLoadWithNewFormatPreservesData() {
        val testFile = tempFolder.newFile("test-new-format.dat")
        val originalKey = WrappedSecretKey(testKeyBytes, testAlgorithm, testCipherTransformation)

        // Mock flight to enable new format
        every { CommonFlightsManager.getFlightsProvider().isFlightEnabled(CommonFlight.ENABLE_NEW_WRAPPED_SECRET_KEY_FORMAT) } returns true

        // Store with new format (controlled by flight)
        originalKey.storeOnFile(testFile)

        // Load back
        val loadedKey = WrappedSecretKey.loadFromFile(testFile, 1024)

        assertNotNull(loadedKey)
        assertArrayEquals(originalKey.byteArray, loadedKey!!.byteArray)
        assertEquals(originalKey.algorithm, loadedKey.algorithm)
        assertEquals(originalKey.cipherTransformation, loadedKey.cipherTransformation)
    }

    @Test
    fun roundTripStoreAndLoadWithOldFormatPreservesKeyData() {
        val testFile = tempFolder.newFile("test-old-format.dat")
        val originalKey = WrappedSecretKey(testKeyBytes, testAlgorithm, testCipherTransformation)

        // Mock flight to disable new format (use old format)
        every { CommonFlightsManager.getFlightsProvider().isFlightEnabled(CommonFlight.ENABLE_NEW_WRAPPED_SECRET_KEY_FORMAT) } returns false

        // Store with old format (controlled by flight)
        originalKey.storeOnFile(testFile)

        // Load back (should use default values for algorithm and cipher)
        val loadedKey = WrappedSecretKey.loadFromFile(testFile, 1024)

        assertNotNull(loadedKey)
        assertArrayEquals(originalKey.byteArray, loadedKey!!.byteArray)
        assertEquals("AES", loadedKey.algorithm) // Default value
        assertEquals("RSA/ECB/PKCS1Padding", loadedKey.cipherTransformation) // Default value
    }

    @Test
    fun newFormatStoresLargerFileThanOldFormat() {
        val newFormatFile = tempFolder.newFile("new-format.dat")
        val oldFormatFile = tempFolder.newFile("old-format.dat")
        val wrappedKey = WrappedSecretKey(testKeyBytes, testAlgorithm, testCipherTransformation)

        // Test new format
        every { CommonFlightsManager.getFlightsProvider().isFlightEnabled(CommonFlight.ENABLE_NEW_WRAPPED_SECRET_KEY_FORMAT) } returns true
        wrappedKey.storeOnFile(newFormatFile)

        // Test old format
        every { CommonFlightsManager.getFlightsProvider().isFlightEnabled(CommonFlight.ENABLE_NEW_WRAPPED_SECRET_KEY_FORMAT) } returns false
        wrappedKey.storeOnFile(oldFormatFile)

        assertTrue("New format should create larger file due to metadata",
                newFormatFile.length() > oldFormatFile.length())
        assertEquals("Old format should only contain key bytes",
                testKeyBytes.size.toLong(), oldFormatFile.length())
    }

    @Test
    fun newFormatFileContainsValidBinaryStructure() {
        val testFile = tempFolder.newFile("binary-structure.dat")
        val wrappedKey = WrappedSecretKey(testKeyBytes, testAlgorithm, testCipherTransformation)

        // Mock flight to enable new format
        every { CommonFlightsManager.getFlightsProvider().isFlightEnabled(CommonFlight.ENABLE_NEW_WRAPPED_SECRET_KEY_FORMAT) } returns true

        wrappedKey.storeOnFile(testFile)

        // Read and verify the binary structure
        val fileBytes = testFile.readBytes()
        val buffer = ByteBuffer.wrap(fileBytes)

        val headerLength = buffer.getInt()
        assertTrue("Header length should be reasonable", headerLength > 0 && headerLength < fileBytes.size)

        val metadataBytes = ByteArray(headerLength)
        buffer.get(metadataBytes)

        val metadata = JSONObject(String(metadataBytes, Charsets.UTF_8))
        assertEquals(testAlgorithm, metadata.getString("algorithm"))
        assertEquals(testCipherTransformation, metadata.getString("cipherTransformation"))
        assertEquals("1.0", metadata.getString("version"))
        assertEquals(testKeyBytes.size, metadata.getInt("keyDataLength"))

        val remainingKeyData = ByteArray(buffer.remaining())
        buffer.get(remainingKeyData)
        assertArrayEquals(testKeyBytes, remainingKeyData)
    }

    @Test
    fun largeKeyDataIsHandledCorrectly() {
        val testFile = tempFolder.newFile("large-key.dat")
        val largeKeyData = ByteArray(8192) { it.toByte() } // 8KB test data
        val wrappedKey = WrappedSecretKey(largeKeyData, testAlgorithm, testCipherTransformation)

        // Mock flight to enable new format
        every { CommonFlightsManager.getFlightsProvider().isFlightEnabled(CommonFlight.ENABLE_NEW_WRAPPED_SECRET_KEY_FORMAT) } returns true

        // Store and load
        wrappedKey.storeOnFile(testFile)
        val loadedKey = WrappedSecretKey.loadFromFile(testFile, 10240)

        assertNotNull(loadedKey)
        assertArrayEquals(largeKeyData, loadedKey!!.byteArray)
        assertEquals(testAlgorithm, loadedKey.algorithm)
        assertEquals(testCipherTransformation, loadedKey.cipherTransformation)
    }

    @Test
    fun oldFormatDetectionWorksWithBinaryData() {
        val testFile = tempFolder.newFile("binary-data.dat")
        val binaryData = ByteArray(256) { (it % 256).toByte() } // Random binary data

        // Write raw binary data (simulating old format)
        testFile.writeBytes(binaryData)

        val loadedKey = WrappedSecretKey.loadFromFile(testFile, 1024)

        assertNotNull(loadedKey)
        assertArrayEquals(binaryData, loadedKey!!.byteArray)
        assertEquals("AES", loadedKey.algorithm)
        assertEquals("RSA/ECB/PKCS1Padding", loadedKey.cipherTransformation)
    }

    @Test
    fun newFormatWithMissingKeyDataLengthUsesFallback() {
        val testFile = tempFolder.newFile("missing-length.dat")

        // Create metadata without keyDataLength
        val metadata = JSONObject().apply {
            put("algorithm", testAlgorithm)
            put("cipherTransformation", testCipherTransformation)
            put("version", "1.0")
            // Intentionally omit keyDataLength
        }
        val metadataBytes = metadata.toString().toByteArray(Charsets.UTF_8)

        // Write binary format manually
        val buffer = ByteBuffer.allocate(4 + metadataBytes.size + testKeyBytes.size)
                .putInt(metadataBytes.size)
                .put(metadataBytes)
                .put(testKeyBytes)
        testFile.writeBytes(buffer.array())

        val loadedKey = WrappedSecretKey.loadFromFile(testFile, 1024)

        assertNotNull(loadedKey)
        assertArrayEquals(testKeyBytes, loadedKey!!.byteArray)
        assertEquals(testAlgorithm, loadedKey.algorithm)
        assertEquals(testCipherTransformation, loadedKey.cipherTransformation)
    }

    @Test
    fun corruptedNewFormatFallsBackToOldFormat() {
        val testFile = tempFolder.newFile("corrupted.dat")

        // Create invalid new format data
        val invalidData = ByteBuffer.allocate(20)
                .putInt(100) // Header length larger than available data
                .put("invalid".toByteArray())
                .array()
        testFile.writeBytes(invalidData)

        val loadedKey = WrappedSecretKey.loadFromFile(testFile, 1024)

        assertNotNull(loadedKey)
        assertArrayEquals(invalidData, loadedKey!!.byteArray) // Should load as raw bytes
        assertEquals("AES", loadedKey.algorithm)
        assertEquals("RSA/ECB/PKCS1Padding", loadedKey.cipherTransformation)
    }

    @Test
    fun storeOnFileUsesOldFormatWhenFlightDisabled() {
        val testFile = tempFolder.newFile("flight-disabled.dat")
        val wrappedKey = WrappedSecretKey(testKeyBytes, testAlgorithm, testCipherTransformation)

        // Mock flight to disable new format
        every { CommonFlightsManager.getFlightsProvider().isFlightEnabled(CommonFlight.ENABLE_NEW_WRAPPED_SECRET_KEY_FORMAT) } returns false

        wrappedKey.storeOnFile(testFile)

        // Verify old format: file should only contain raw key bytes
        val fileBytes = testFile.readBytes()
        assertArrayEquals(testKeyBytes, fileBytes)
    }

    @Test
    fun storeOnFileUsesNewFormatWhenFlightEnabled() {
        val testFile = tempFolder.newFile("flight-enabled.dat")
        val wrappedKey = WrappedSecretKey(testKeyBytes, testAlgorithm, testCipherTransformation)

        // Mock flight to enable new format
        every { CommonFlightsManager.getFlightsProvider().isFlightEnabled(CommonFlight.ENABLE_NEW_WRAPPED_SECRET_KEY_FORMAT) } returns true

        wrappedKey.storeOnFile(testFile)

        // Verify new format: file should be larger than just key bytes
        val fileBytes = testFile.readBytes()
        assertTrue("New format file should be larger than raw key", fileBytes.size > testKeyBytes.size)

        // Verify it can be loaded correctly
        val loadedKey = WrappedSecretKey.loadFromFile(testFile, 1024)
        assertNotNull(loadedKey)
        assertArrayEquals(testKeyBytes, loadedKey!!.byteArray)
        assertEquals(testAlgorithm, loadedKey.algorithm)
        assertEquals(testCipherTransformation, loadedKey.cipherTransformation)
    }

    /**
     * Test that verifies backward compatibility when rolling back the ENABLE_NEW_WRAPPED_SECRET_KEY_FORMAT flight.
     * This test ensures that keys stored with the new format can still be read when the flight is disabled,
     * which is critical for production rollback scenarios.
     */
    @Test
    fun flightRollbackScenario_keyStoredWithNewFormatCanBeReadWithFlightDisabled() {
        val testFile = tempFolder.newFile("rollback-scenario.dat")
        val originalKey = WrappedSecretKey(testKeyBytes, testAlgorithm, testCipherTransformation)

        // Step 1: Enable flight and store key using new format
        every { CommonFlightsManager.getFlightsProvider().isFlightEnabled(CommonFlight.ENABLE_NEW_WRAPPED_SECRET_KEY_FORMAT) } returns true

        originalKey.storeOnFile(testFile)

        // Verify the file was created with new format (contains metadata)
        val fileBytes = testFile.readBytes()
        assertTrue("File should be larger than raw key bytes due to metadata header",
                   fileBytes.size > testKeyBytes.size)

        // Verify it can be read with flight enabled
        val keyWithFlightEnabled = WrappedSecretKey.loadFromFile(testFile, 1024)
        assertNotNull("Key should be readable with flight enabled", keyWithFlightEnabled)
        assertArrayEquals("Key data should match", originalKey.byteArray, keyWithFlightEnabled!!.byteArray)
        assertEquals("Algorithm should match", originalKey.algorithm, keyWithFlightEnabled.algorithm)
        assertEquals("Cipher transformation should match", originalKey.cipherTransformation, keyWithFlightEnabled.cipherTransformation)

        // Step 2: Disable flight (simulate rollback) and verify key can still be read
        every { CommonFlightsManager.getFlightsProvider().isFlightEnabled(CommonFlight.ENABLE_NEW_WRAPPED_SECRET_KEY_FORMAT) } returns false

        val keyAfterRollback = WrappedSecretKey.loadFromFile(testFile, 1024)
        assertNotNull("Key should still be readable after flight rollback", keyAfterRollback)
        assertArrayEquals("Key data should remain intact after rollback", originalKey.byteArray, keyAfterRollback!!.byteArray)
        assertEquals("Algorithm should be preserved from metadata", originalKey.algorithm, keyAfterRollback.algorithm)
        assertEquals("Cipher transformation should be preserved from metadata", originalKey.cipherTransformation, keyAfterRollback.cipherTransformation)

        // Verify that the format detection correctly identifies this as new format
        // even when the flight is disabled (loadFromFile should still work)
        assertEquals("Both reads should return identical key data",
                     keyWithFlightEnabled.byteArray.contentToString(),
                     keyAfterRollback.byteArray.contentToString())
        assertEquals("Both reads should return identical algorithm",
                     keyWithFlightEnabled.algorithm,
                     keyAfterRollback.algorithm)
        assertEquals("Both reads should return identical cipher transformation",
                     keyWithFlightEnabled.cipherTransformation,
                     keyAfterRollback.cipherTransformation)
    }

    /**
     * Test that verifies forward compatibility when enabling the ENABLE_NEW_WRAPPED_SECRET_KEY_FORMAT flight.
     * This test ensures that keys stored with the old format can still be read when the flight is enabled,
     * which is important when rolling out the new feature.
     */
    @Test
    fun flightRolloutScenario_keyStoredWithOldFormatCanBeReadWithFlightEnabled() {
        val testFile = tempFolder.newFile("rollout-scenario.dat")
        val originalKey = WrappedSecretKey(testKeyBytes, testAlgorithm, testCipherTransformation)

        // Step 1: Disable flight and store key using old format
        every { CommonFlightsManager.getFlightsProvider().isFlightEnabled(CommonFlight.ENABLE_NEW_WRAPPED_SECRET_KEY_FORMAT) } returns false

        originalKey.storeOnFile(testFile)

        // Verify the file was created with old format (only raw key bytes)
        val fileBytes = testFile.readBytes()
        assertArrayEquals("Old format should contain only raw key bytes", testKeyBytes, fileBytes)

        // Verify it can be read with flight disabled (uses default algorithm and cipher)
        val keyWithFlightDisabled = WrappedSecretKey.loadFromFile(testFile, 1024)
        assertNotNull("Key should be readable with flight disabled", keyWithFlightDisabled)
        assertArrayEquals("Key data should match", originalKey.byteArray, keyWithFlightDisabled!!.byteArray)
        assertEquals("Should use default algorithm", "AES", keyWithFlightDisabled.algorithm)
        assertEquals("Should use default cipher transformation", "RSA/ECB/PKCS1Padding", keyWithFlightDisabled.cipherTransformation)

        // Step 2: Enable flight (simulate rollout) and verify key can still be read
        every { CommonFlightsManager.getFlightsProvider().isFlightEnabled(CommonFlight.ENABLE_NEW_WRAPPED_SECRET_KEY_FORMAT) } returns true

        val keyAfterRollout = WrappedSecretKey.loadFromFile(testFile, 1024)
        assertNotNull("Key should still be readable after flight rollout", keyAfterRollout)
        assertArrayEquals("Key data should remain intact after rollout", originalKey.byteArray, keyAfterRollout!!.byteArray)
        assertEquals("Should still use default algorithm for old format", "AES", keyAfterRollout.algorithm)
        assertEquals("Should still use default cipher transformation for old format", "RSA/ECB/PKCS1Padding", keyAfterRollout.cipherTransformation)

        // Verify that both reads return the same data (backward compatibility maintained)
        assertEquals("Both reads should return identical key data",
                     keyWithFlightDisabled.byteArray.contentToString(),
                     keyAfterRollout.byteArray.contentToString())
        assertEquals("Both reads should return identical algorithm",
                     keyWithFlightDisabled.algorithm,
                     keyAfterRollout.algorithm)
        assertEquals("Both reads should return identical cipher transformation",
                     keyWithFlightDisabled.cipherTransformation,
                     keyAfterRollout.cipherTransformation)
    }
}

