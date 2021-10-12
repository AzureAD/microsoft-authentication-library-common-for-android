//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.common.java.crypto;

/**
 * Information about the backing of an underlying keystore.
 */
public enum SecureHardwareState {

    /**
     * Returned if the underlying private key resides inside secure hardware (e.g., Trusted
     * Execution Environment (TEE) or Secure Element (SE)) and its hardware backing has been
     * attested.
     *
     * @see <a href="https://developer.android.com/training/articles/security-key-attestation">Security Key Attestation</a>
     */
    TRUE_ATTESTED,

    /**
     * Returned if the underlying private key resides inside secure hardware (e.g., Trusted
     * Execution Environment (TEE) or Secure Element (SE)). No mechanism of attestation is
     * provided or specified.
     */
    TRUE_UNATTESTED,

    /**
     * The the underlying private key is not inside secure hardware.
     */
    FALSE,

    /**
     * It is unknown where the underlying key resides, due to lack of API support for
     * determination.
     */
    UNKNOWN_DOWNLEVEL,

    /**
     * It is unknown where the underlying key resides, due to an error during keystore
     * interrogation.
     */
    UNKNOWN_QUERY_ERROR
}
