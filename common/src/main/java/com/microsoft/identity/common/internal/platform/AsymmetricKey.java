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

import com.microsoft.identity.common.java.crypto.SecureHardwareState;
import com.microsoft.identity.common.java.exception.ClientException;

import java.security.cert.Certificate;
import java.util.Date;

/**
 * Represents an asymmetric key. Underlying storage and algorithm is unspecified at this interface
 * but may be extended or implemented by subclasses/subinterfaces defining ECC, RSA, DSA, or other.
 */
public interface AsymmetricKey extends Key {

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
     * Gets the public key associated with this asymmetric key.
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

    /**
     * Encrypts a String using the underlying key material.
     *
     * @param plaintext The text to encrypt.
     * @return The ciphertext derived from the underlying material + cipher.
     * @throws ClientException If an error is encountered during encryption.
     */
    String encrypt(String plaintext) throws ClientException;

    /**
     * Decrypts a String using the underlying key material.
     *
     * @param ciphertext The ciphertext to decrypt.
     * @return The decrypted value.
     * @throws ClientException If the supplied ciphertext cannot be decrypted.
     */
    String decrypt(String ciphertext) throws ClientException;

    /**
     * Gets the {@link SecureHardwareState} of this DevicePopManager.
     *
     * @return The SecureHardwareState.
     * @throws ClientException If the underlying key material cannot be inspected.
     */
    SecureHardwareState getSecureHardwareState() throws ClientException;

    /**
     * Returns the certificate chain associated with the given alias.
     *
     * @return The certificate chain (with the device pop key certificate first, following by zero
     * or more certificate authorities), or null if the current key does not contain a certificate
     * chain.
     * @throws ClientException If the underlying key material cannot be inspected.
     */
    Certificate[] getCertificateChain() throws ClientException;
}
