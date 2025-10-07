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

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

/**
 * Binary stream-based serializer for [WrappedSecretKey] with metadata support.
 *
 * Uses DataInputStream/DataOutputStream for compact binary metadata encoding.
 * More efficient than JSON but less human-readable.
 *
 * **Metadata:** UTF strings for algorithm/transformation, int for key length
 *
 * @see IWrappedSecretKeySerializer
 */
class WrappedSecretKeyBinaryStreamSerializer: AbstractWrappedSecretKeySerializer() {

    companion object {
        /** Unique identifier for the binary stream serialization format */
        const val ID = 1
    }

    /**
     * The unique identifier for this serialization format.
     */
    override val id = ID

    /**
     * Serializes wrapped secret key metadata to binary format.
     *
     * @param wrappedSecretKey The wrapped secret key containing metadata to serialize
     * @return Binary encoded metadata as byte array
     */
    @Throws(java.io.IOException::class)
    override fun serializeMetadata(wrappedSecretKey: WrappedSecretKey): ByteArray {
        return ByteArrayOutputStream().use { bytesOutputStream ->
            DataOutputStream(bytesOutputStream).use { dos ->
                // Write algorithm (writeUTF includes length prefix)
                dos.writeUTF(wrappedSecretKey.algorithm)
                // Write cipher transformation (writeUTF includes length prefix)
                dos.writeUTF(wrappedSecretKey.cipherTransformation)
                // Write key data length
                dos.writeInt(wrappedSecretKey.wrappedKeyData.size)
            }
            bytesOutputStream.toByteArray()
        }
    }

    /**
     * Deserializes binary metadata to create a [WrappedSecretKeyMetadata] object.
     *
     * @param metadataByteArray Binary encoded metadata bytes
     * @return [WrappedSecretKeyMetadata] object with extracted information
     * @throws java.io.IOException if binary format is invalid or corrupted
     */
    @Throws(java.io.IOException::class)
    override fun deserializeMetadata(metadataByteArray: ByteArray): WrappedSecretKeyMetadata {
        return ByteArrayInputStream(metadataByteArray).use { byteArrayInputStream ->
            DataInputStream(byteArrayInputStream).use { dis ->
                val algorithm = dis.readUTF()
                val cipherTransformation = dis.readUTF()
                val keyLength = dis.readInt()

                WrappedSecretKeyMetadata(
                    algorithm = algorithm,
                    cipherTransformation = cipherTransformation,
                    keyLength = keyLength
                )
            }
        }
    }
}