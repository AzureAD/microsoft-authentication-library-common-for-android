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

import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.flighting.CommonFlight
import com.microsoft.identity.common.java.flighting.CommonFlightsManager
import com.microsoft.identity.common.java.util.FileUtil
import com.microsoft.identity.common.logging.Logger
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer

/**
 * Represents a wrapped secret key with metadata for algorithm and cipher transformation.
 *
 * This class supports both old and new storage formats for wrapped keys.
 * The new format includes metadata header for better compatibility and extensibility.
 */
class WrappedSecretKey(
    val byteArray: ByteArray,
    val algorithm: String,
    val cipherTransformation: String
) {

    /**
     * Stores the wrapped secret key to file.
     *
     * @param file The file to store the key data
     */
    fun storeOnFile(file: File) {
        val methodTag = "WrappedSecretKey:storeOnFile"
        try {
            val useNewFormat =
                CommonFlightsManager
                    .getFlightsProvider()
                    .isFlightEnabled(CommonFlight.ENABLE_NEW_WRAPPED_SECRET_KEY_FORMAT)

            if (useNewFormat) {
                storeOnFileNewFormat(file)
            } else {
                FileUtil.writeDataToFile(byteArray, file)
            }
        } catch (e: Exception) {
           val errorMessage = "Failed to store key on disk"
            Logger.error(methodTag, errorMessage , e)
            throw ClientException(ClientException.IO_ERROR, errorMessage , e)
            throw ClientException(ClientException.IO_ERROR, "Failed to store key on disk", e)
        }
    }

    /**
     * Stores the wrapped secret key in the new binary format.
     *
     * The new format structure is:
     * - Header identifier (4 bytes)
     * - Metadata length (4 bytes)
     * - Protobuf-serialized metadata
     * - Raw key data
     *
     * @param file The file to store the key data
     */
    private fun storeOnFileNewFormat(file: File) {
        val methodTag = "$TAG:storeOnFileNewFormat"

        // New format: Store metadata header + raw key data
        val metadata = JSONObject().apply {
            put("algorithm", algorithm)
            put("cipherTransformation", cipherTransformation)
            put("version", FORMAT_VERSION_1)
            put("keyDataLength", byteArray.size)
        }

        val metadataBytes = metadata.toString().toByteArray(Charsets.UTF_8)

        // Use ByteBuffer for cleaner header writing
        val output = ByteBuffer.allocate(Int.SIZE_BYTES + Int.SIZE_BYTES + metadataBytes.size + byteArray.size)
            .putInt(NEW_FORMAT_HEADER_IDENTIFIER)  // Write header length (4 bytes, big-endian)
            .putInt(metadataBytes.size)  // Write metadata length (4 bytes, big-endian)
            .put(metadataBytes)          // Write metadata
            .put(byteArray)              // Write raw key data
            .array()

        FileUtil.writeDataToFile(output, file)
        Logger.info(methodTag, "Key successfully stored on disk using optimized new format.")
    }


    companion object {

        private const val TAG = "WrappedSecretKey"
        private const val FORMAT_VERSION_1 = 1
        private const val DEFAULT_ALGORITHM = "AES"
        private const val DEFAULT_CIPHER_TRANSFORMATION = "RSA/ECB/PKCS1Padding"
        private const val NEW_FORMAT_HEADER_IDENTIFIER = 0x00FF12AB

        /**
         * Loads a wrapped secret key from file, automatically detecting the storage format.
         *
         * @param file The file containing the wrapped key data
         * @param fileSize Maximum size to read from the file
         * @return WrappedSecretKey instance or null if file doesn't exist or is empty
         */
        fun loadFromFile(file: File, fileSize: Int): WrappedSecretKey? {
            val methodTag = "$TAG:loadFromFile"

            if (!file.exists()) {
                Logger.warn(methodTag, "Key file does not exist")
                return null
            }

            try {
                val rawData = FileUtil.readFromFile(file, fileSize)
                if (rawData == null || rawData.isEmpty()) {
                    Logger.warn(methodTag, "Key file is empty")
                    return null
                }

                return if (isNewFormat(rawData)) {
                    loadFromNewFormat(rawData)
                } else {
                    loadFromOldFormat(rawData)
                }
            } catch (e: Exception) {
                Logger.error(methodTag, "Failed to load key from file", e)
                throw ClientException(ClientException.KEY_LOAD_FAILURE, "Failed to load key from file", e)
            }
        }

        /**
         * Determines if the raw data uses the new wrapped key format.
         *
         * @param rawData The raw bytes read from file
         * @return true if data is in new format, false if old format
         */
        private fun isNewFormat(rawData: ByteArray): Boolean {
            if (rawData.size < 8) return false
            val buffer = ByteBuffer.wrap(rawData)
            return buffer.getInt() == NEW_FORMAT_HEADER_IDENTIFIER
        }

        /**
         * Loads wrapped secret key from new binary format.
         *
         * The new format structure is:
         * - Header identifier (4 bytes)
         * - Metadata length (4 bytes)
         * - Protobuf-serialized metadata
         * - Raw key data
         *
         * @param rawData The raw binary data
         * @return WrappedSecretKey instance
         * @throws ClientException if the data format is invalid or parsing fails
         */
        @Throws(IOException::class)
        private fun loadFromNewFormat(rawData: ByteArray): WrappedSecretKey {
            val methodTag = "$TAG:loadFromNewFormat"
            Logger.info(methodTag, "Loading key using optimized new binary format")
            val buffer = ByteBuffer.wrap(rawData)

            // Skip header identifier (already validated in isNewFormat)
            buffer.getInt()

            // Read metadata length
            val metadataLength = buffer.getInt()

            // Extract and parse protobuf metadata
            val metadataBytes = ByteArray(metadataLength)
            buffer.get(metadataBytes)

            val jsonString = String(metadataBytes, Charsets.UTF_8)
            val json = JSONObject(jsonString)
            val algorithm = json.getString("algorithm")
            val cipherTransformation = json.getString("cipherTransformation")
            val keyDataLength = json.getInt("keyDataLength")

            // Validate key data length
            if (keyDataLength != buffer.remaining()) {
                Logger.warn(methodTag, "Key data length mismatch. Expected: $keyDataLength, Actual: ${buffer.remaining()}")
            }

            val keyBytes = ByteArray(buffer.remaining())
            buffer.get(keyBytes)

            Logger.verbose(methodTag, "Successfully loaded key with algorithm: $algorithm, transformation: $cipherTransformation")
            return WrappedSecretKey(keyBytes, algorithm, cipherTransformation)
        }

        /**
         * Loads wrapped secret key from old binary format.
         * Uses default values for algorithm and cipher transformation.
         *
         * @param rawData The raw key bytes
         * @return WrappedSecretKey instance with default algorithm and cipher transformation
         */
        private fun loadFromOldFormat(rawData: ByteArray): WrappedSecretKey {
            val methodTag = "$TAG:loadFromOldFormat"
            Logger.info(methodTag, "Loading key using old format with default algorithm and cipher transformation")

            return WrappedSecretKey(
                byteArray = rawData,
                algorithm = DEFAULT_ALGORITHM,
                cipherTransformation = DEFAULT_CIPHER_TRANSFORMATION
            )
        }
    }
}
