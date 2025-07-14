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

import java.security.spec.AlgorithmParameterSpec

/**
 * Cipher parameter specification for cryptographic operations.
 *
 * Encapsulates the components required to initialize a [javax.crypto.Cipher] instance:
 * algorithm, mode, padding, and optional algorithm parameters.
 *
 * @property algorithmParameterSpec Optional algorithm parameters (e.g., [javax.crypto.spec.OAEPParameterSpec])
 * @property algorithm Cryptographic algorithm (e.g., "RSA")
 * @property mode Block cipher mode (e.g., "ECB", "CBC")
 * @property padding Padding scheme (e.g., "PKCS1Padding", "OAEPwithSHA-256andMGF1Padding")
 *
 * @see <a href="https://developer.android.com/reference/javax/crypto/Cipher">javax.crypto.Cipher</a>
 */
data class CipherSpec(
    val algorithmParameterSpec: AlgorithmParameterSpec?,
    private val algorithm: String,
    private val mode: String,
    val padding: String,
) {
    /**
     * Complete transformation string for [javax.crypto.Cipher.getInstance].
     */
    val transformation = "$algorithm/$mode/$padding"

    override fun toString(): String {
        return "CipherSpec(transformation='$transformation')"
    }
}
