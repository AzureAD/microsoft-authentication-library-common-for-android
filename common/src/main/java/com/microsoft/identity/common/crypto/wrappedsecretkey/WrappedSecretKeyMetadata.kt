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
 * Metadata container for wrapped secret key cryptographic information.
 *
 * This data class encapsulates the essential cryptographic metadata associated with a wrapped secret key,
 * providing type-safe access to algorithm specifications and validation information.
 *
 * **Usage:**
 * Used internally by serializers to store and retrieve metadata during serialization/deserialization
 * processes, ensuring that cryptographic context is preserved across storage operations.
 *
 * @property algorithm The cryptographic algorithm used for the secret key (e.g., "AES", "DES")
 * @property cipherTransformation The complete cipher transformation specification including algorithm,
 *                                mode, and padding (e.g., "RSA/ECB/PKCS1Padding", "AES/CBC/PKCS5Padding")
 * @property keyLength The length of the wrapped key data in bytes, used for validation during deserialization
 *
 * @see WrappedSecretKey
 * @see IWrappedSecretKeySerializer
 * @see AbstractWrappedSecretKeySerializer
 */
data class WrappedSecretKeyMetadata(
    val algorithm: String,
    val cipherTransformation: String,
    val keyLength: Int
)
