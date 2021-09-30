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
}
