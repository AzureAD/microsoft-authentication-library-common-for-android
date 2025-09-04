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
 * Interface for serializing and deserializing WrappedSecretKey objects.
 *
 * Implementations can define different serialization formats and versions.
 */
interface IWrappedSecretKeySerializer {

    /**
     * Serialize the given WrappedSecretKey into a byte array.
     *
     * @param wrappedSecretKey The WrappedSecretKey to serialize.
     * @return The serialized byte array representation of the WrappedSecretKey.
     */
    fun serialize(wrappedSecretKey: WrappedSecretKey): ByteArray

    /**
     * Deserialize the given byte array into a WrappedSecretKey object.
     *
     * @param data The byte array to deserialize.
     * @return The deserialized WrappedSecretKey object.
     * @throws Exception if deserialization fails.
     */
    fun deserialize(data: ByteArray): WrappedSecretKey

    /**
     * Get the version of the serialization format used by this serializer.
     *
     * @return The version number as an integer.
     */
    fun getVersion(): Int
}
