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
import java.nio.ByteBuffer

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
                // New format: Store metadata header + raw key data
                val metadata = JSONObject().apply {
                    put("algorithm", algorithm)
                    put("cipherTransformation", cipherTransformation)
                    put("version", NEW_FORMAT_VERSION)
                    put("keyDataLength", byteArray.size)
                }
                val metadataBytes = metadata.toString().toByteArray(Charsets.UTF_8)

                // Use ByteBuffer for cleaner header writing
                val output = ByteBuffer.allocate(4 + metadataBytes.size + byteArray.size)
                    .putInt(metadataBytes.size)  // Write header length (4 bytes, big-endian)
                    .put(metadataBytes)          // Write metadata
                    .put(byteArray)              // Write raw key data
                    .array()

                FileUtil.writeDataToFile(output, file)
                Logger.info(methodTag, "Key successfully stored on disk using optimized new format.")
            } else {
                // Old format: Store only raw key bytes
                FileUtil.writeDataToFile(byteArray, file)
                Logger.info(methodTag, "Key successfully stored on disk using old format.")
            }
        } catch (e: Exception) {
            Logger.error(methodTag, "Failed to store key on disk", e)
            throw ClientException(ClientException.IO_ERROR, "Failed to store key on disk", e)
        }
    }

    companion object {
        private const val TAG = "WrappedSecretKey"
        private const val NEW_FORMAT_VERSION = "1.0"
        private const val DEFAULT_ALGORITHM = "AES"
        private const val DEFAULT_CIPHER_TRANSFORMATION = "RSA/ECB/PKCS1Padding"

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
                throw ClientException(ClientException.IO_ERROR, "Failed to load key from file", e)
            }
        }

        /**
         * Determines if the raw data is in new binary format or old binary format.
         *
         * @param rawData The raw bytes read from file
         * @return true if data is in new format, false if old format
         */
        private fun isNewFormat(rawData: ByteArray): Boolean {
            return try {
                // New format starts with 4-byte header length, followed by JSON metadata
                if (rawData.size < 4) return false

                val buffer = ByteBuffer.wrap(rawData)
                val headerLength = buffer.getInt()

                // Sanity check: header length should be reasonable
                if (headerLength < 10 || headerLength > rawData.size - 4) return false

                // Try to parse the metadata JSON
                val metadataBytes = ByteArray(headerLength)
                buffer.get(metadataBytes)
                val jsonString = String(metadataBytes, Charsets.UTF_8)
                val json = JSONObject(jsonString)

                // Check if it has the expected new format fields
                json.has("algorithm") && json.has("cipherTransformation") && json.has("version")
            } catch (e: Exception) {
                // If parsing fails, assume it's old format
                false
            }
        }

        /**
         * Loads wrapped secret key from new binary format.
         *
         * @param rawData The raw binary data
         * @return WrappedSecretKey instance
         */
        private fun loadFromNewFormat(rawData: ByteArray): WrappedSecretKey {
            val methodTag = "$TAG:loadFromNewFormat"
            Logger.info(methodTag, "Loading key using optimized new binary format")

            val buffer = ByteBuffer.wrap(rawData)
            val headerLength = buffer.getInt()

            // Extract and parse metadata
            val metadataBytes = ByteArray(headerLength)
            buffer.get(metadataBytes)
            val jsonString = String(metadataBytes, Charsets.UTF_8)
            val json = JSONObject(jsonString)

            val algorithm = json.getString("algorithm")
            val cipherTransformation = json.getString("cipherTransformation")
            val keyDataLength = json.optInt("keyDataLength", buffer.remaining())

            // Extract raw key data
            val keyBytes = ByteArray(keyDataLength)
            buffer.get(keyBytes)

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
