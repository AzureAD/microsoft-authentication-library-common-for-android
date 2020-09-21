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
package com.microsoft.identity.common.internal.platform;

import com.microsoft.identity.common.exception.ClientException;

import java.util.Date;

/**
 * Represents an RSA asymmetric key. This object represents a single keypair instance inside of an
 * underlying keystore which may or may not be hardware backed depending on OS version, key size,
 * and TPM/HSM chipset.
 */
public interface AsymmetricKey {

    /**
     * Gets the alias which refers to this key at its originating keystore.
     *
     * @return The alias of this key.
     */
    String getAlias();

    /**
     * Returns the creation date of the asymmetric key entry backing this instance.
     *
     * @return The asymmetric key creation date.
     * @throws ClientException If no asymmetric key exists.
     */
    Date getCreatedOn() throws ClientException;

    /**
     * Gets the thumbprint of the current KeyPair.
     *
     * @return The thumbprint.
     */
    String getThumbprint() throws ClientException;

    /**
     * Gets an RFC-7517 compliant public key as a minified JWK.
     * <p>
     * Sample value:
     * <pre>
     * {
     * 	"kty": "RSA",
     * 	"e": "AQAB",
     * 	"n": "tMqJ7Oxh3PdLaiEc28w....HwES9Q"
     * }
     * </pre>
     */
    String getPublicKey() throws ClientException;

    /**
     * Signs an arbitrary piece of String data.
     *
     * @return The input data, signed by our private key.
     */
    String sign(String data) throws ClientException;

    /**
     * Verifies a signature previously made by this private key.
     *
     * @param plainText    The input to verify.
     * @param signatureStr The signature against which the plainText should be evaluated.
     * @return True if the input was signed by our private key. False otherwise.
     */
    boolean verify(String plainText, String signatureStr);

}
