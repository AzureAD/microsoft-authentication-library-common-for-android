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
package com.microsoft.identity.common.java.crypto;

import java.security.KeyStore;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.Cipher;

/**
 * Interface for cryptoSuite definitions.  Designed to span the Cipher enum in use in DevicePopManager
 * to allow for inclusion of symmetric cipher definitions and include the name of a MAC algorithm.
 */
public interface CryptoSuite {
    /**
     * @return the name of the cipher used for this crypto suite.  Should be suitable for use in Cipher.getInstance();
     */
    Algorithm cipher();

    /**
     * @return the name of the MAC used for this crypto suite.  Should be suitable for use in Mac.getInstance();
     */
    String macName();

    /**
     * @return true if this suite uses an asymmetric key.
     */
    boolean isAsymmetric();

    /**
     * @return the class of entry that is used by the a KeyStore to store this credential.
     */
    Class<? extends KeyStore.Entry> keyClass();

    /**
     * @return the key size for this instance.
     */
    int keySize();

    /**
     * @return the signing algorithm desired by this suite.
     */
    SigningAlgorithm signingAlgorithm();

    /**
     * @return an AlgorithmParameterSpec if needed, null if not required.  The atguments here are
     * actually dependent on the cipher in question - in the case of AES-GCM, this is the signature
     * size, the iv (as a byte[] buffer), and optionally the start and length of the iv in the buffer.
     *
     * This should be a map instead, and we should establish some names.
     * <ul>
     *     <li>iv - an initialization vector as byte[] buffer</li>
     *     <li>ivStart, ivEnd - start and end position of the data in the iv buffer</li>
     *     <li>cipher - the Cipher object being operated on</li>
     * </ul>
     *
     * The reason for this is that we can reference classes in the platform-specific code that would
     * cause the cross-platform code to need platform-specific functionality in order to be able to
     * compile.  You can't talk about GCMParmeterSpec on Android pre-19, for instance.
     */
    AlgorithmParameterSpec cryptoSpec(Object... args);

    /**
     * Performs any extra initialization steps needed on this cipher.
     * Some ciphers have extra initialization steps that need to be performed post Cipher.init.
     * The most germane example is updateAAD in the GCM cipher series, which updates the cipher
     * state based on a buffer of data in order to insist that it is known to both parties.  Similar
     * to cryptoSpec, this should be a map:
     *
     * This should be a map instead, and we should establish some names.
     *
     * <ul>
     *     <li>aad - an initialization vector as byte[] buffer</li>
     *     <li>aadStart, aadEnd - start and end position of the data in the iv buffer</li>
     *     <li>cipher - the Cipher object being operated on</li>
     * </ul>
     *
     * Again, this is done to shield the cross-platform code from needing to know about platform
     * details.
     */
    void initialize(Cipher cipher, Object... args);
}
