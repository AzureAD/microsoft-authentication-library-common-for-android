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
package com.microsoft.identity.common.java.crypto.key

import com.microsoft.identity.common.java.exception.ClientException
import javax.crypto.SecretKey

/**
 * Interface defining how a [SecretKey] is loaded/cached/sourced/used.
 * Implementations of this interface provide concrete strategies for key generation,
 * loading, and management across different platforms and API levels.
 */
interface ISecretKeyLoader {
    /**
     * Returns this key's alias/name.
     * Each key will have a unique alias/name.
     *
     * @return The key alias.
     */
    val alias: String

    /**
     * Gets an identifier of this key type.
     * This might be padded into the encrypted string.
     *
     * @return The key type identifier.
     */
    val keyTypeIdentifier: String

    /**
     * Gets the cipher algorithm that is meant to be used with this key type.
     *
     * @return The cipher algorithm name.
     */
    val cipherAlgorithm: String


    val secretKeyGenerator: ISecretKeyGenerator

    @get:Throws(ClientException::class)
    val key: SecretKey
}
