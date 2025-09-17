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

/**
 * Interface for serializing and deserializing [WrappedSecretKey] objects.
 *
 * Each implementation handles a specific format and provides bidirectional
 * conversion between [WrappedSecretKey] objects and their byte array representations.
 *
 * @see WrappedSecretKey
 * @see WrappedSecretKeySerializerManager
 */
interface IWrappedSecretKeySerializer {

    /**
     * Serializes a [WrappedSecretKey] into its byte array representation.
     *
     * @param wrappedSecretKey The wrapped secret key to serialize
     * @return The serialized byte array representation
     * @throws IllegalArgumentException if the input is invalid
     */
    fun serialize(wrappedSecretKey: WrappedSecretKey): ByteArray

    /**
     * Deserializes a byte array into a [WrappedSecretKey] object.
     *
     * @param wrappedSecretKeyByteArray The serialized byte array to deserialize
     * @return The reconstructed [WrappedSecretKey] object
     * @throws Exception if deserialization fails due to invalid format or corruption
     */
    fun deserialize(wrappedSecretKeyByteArray: ByteArray): WrappedSecretKey

    /**
     * The unique identifier for this serialization format.
     *
     * Used by [WrappedSecretKeySerializerManager] for format detection and serializer selection.
     *
     * @return The unique serializer format identifier (0-255)
     */
    val id: Int
}
