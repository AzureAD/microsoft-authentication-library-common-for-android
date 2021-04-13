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
package com.microsoft.identity.common.internal.platform;

import com.microsoft.identity.common.exception.ClientException;

import java.security.cert.Certificate;

import lombok.NonNull;

/**
 * Interface for utilizing keys.
 */
public interface KeyAccessor {
    /**
     * Encrypt a plaintext blob, returning an encrypted byte array.
     * @param plaintext the plaintext to encrypt.
     * @return the encrypted byte array.
     */
    byte[] encrypt(byte[] plaintext) throws ClientException;

    /**
     * Decrypt a blob of ciphertext, returning the decrypted values.
     * @param ciphertext the blob of ciphertext to decrypt.
     * @return the decrypted byte array.
     */
    byte[] decrypt(byte[] ciphertext) throws ClientException;

    /**
     * Sign a block of data, returning the signature.
     * @param text the data to sign.
     * @return the signature, as a byte array.
     */
    byte[] sign(byte[] text) throws ClientException;

    /**
     * Verify a signature, returning the
     * @param text
     * @param signature
     * @return
     */
    boolean verify(byte[] text, byte[] signature) throws ClientException;

    /**
     * @return a thumbprint for this key.
     */
    byte[] getThumprint() throws ClientException;


    /**
     * @return the certificate chain for this key, if it exists.
     */
    Certificate[] getCertificateChain() throws ClientException;

    /**
     * @return a {@link SecureHardwareState} object representing the status of this key.
     */
    SecureHardwareState getSecureHardwareState() throws ClientException;

    /**
     * Using the provided cryptosuite, generate a new derived key accessor given the label and the
     * context.
     */
    KeyAccessor generateDerivedKey(final byte[] label, final byte[] ctx, CryptoSuite suite) throws ClientException;
 }
