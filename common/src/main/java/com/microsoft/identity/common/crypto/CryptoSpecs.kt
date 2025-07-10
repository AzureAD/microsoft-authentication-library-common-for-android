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
 * A base interface for cryptographic specifications.
 *
 * This interface provides a common structure for different types of cryptographic parameter
 * specifications used throughout the application. It ensures that any specification class
 * includes an [AlgorithmParameterSpec], which is a standard Java Security class for specifying
 * algorithm parameters.
 */
interface CryptoSpec {
    val algorithmParameterSpec: AlgorithmParameterSpec?
}

/**
 * Data class to hold cipher parameter specifications for encryption and decryption operations.
 *
 * This class defines the components needed to create a [javax.crypto.Cipher] instance,
 * including the algorithm, block mode, and padding scheme. It also constructs the full
 * transformation string required by the Cipher API.
 *
 * @property algorithmParameterSpec The algorithm parameter specification (e.g., [javax.crypto.spec.OAEPParameterSpec]),
 * which can be null if not required by the transformation.
 * @property algorithm The name of the cryptographic algorithm (e.g., "RSA").
 * @property mode The block cipher mode of operation (e.g., "ECB", "CBC").
 * @property padding The padding scheme used for the cipher (e.g., "PKCS1Padding", "OAEPwithSHA-256andMGF1Padding").
 */
data class CipherSpec(
    override val algorithmParameterSpec: AlgorithmParameterSpec?,
    val algorithm: String,
    val mode: String,
    val padding: String,
) : CryptoSpec {
    /**
     * The full transformation string (e.g., "RSA/ECB/PKCS1Padding") used to initialize a
     * [javax.crypto.Cipher] instance.
     */
    val transformation = "$algorithm/$mode/$padding"

    override fun toString(): String {
        return "CipherSpec(transformation='$transformation')"
    }
}

/**
 * Data class to hold parameter specifications for cryptographic key generation.
 *
 * This class encapsulates all the necessary information to generate a new cryptographic key pair,
 * including a description for logging, the algorithm, the padding scheme to be associated with the key,
 * and the detailed algorithm parameter specification.
 *
 * @property description A descriptive string for the specification, useful for logging and debugging.
 * @property algorithm The key generation algorithm, typically "RSA".
 * @property encryptionPadding The encryption padding scheme that the generated key will support
 * (e.g., [android.security.keystore.KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1]).
 * @property algorithmParameterSpec The detailed key generation parameter specification, such as
 * [android.security.keystore.KeyGenParameterSpec] or [android.security.KeyPairGeneratorSpec].
 */
data class KeyGenSpec(
    val description: String,
    val algorithm: String,
    val encryptionPadding: String,
    override val algorithmParameterSpec: AlgorithmParameterSpec,
) : CryptoSpec {
    override fun toString(): String {
        return "KeyGenSpec(description='$description', algorithm='$algorithm', encryptionPadding='$encryptionPadding')"
    }
}
