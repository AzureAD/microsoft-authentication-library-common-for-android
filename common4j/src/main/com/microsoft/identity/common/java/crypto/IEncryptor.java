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

import com.microsoft.identity.common.java.exception.ClientException;

import java.security.Key;

import lombok.NonNull;

/**
 * Interface for an Encryptor.
 */
public interface IEncryptor {

    /**
     * Encrypt the given byte array.
     *
     * @param key               the key to encrypt with.
     * @param encryptAlgorithm  algorithm to encrypt with.
     * @param iv                an initialization vector (IV).
     * @param dataToBeEncrypted the data to be encrypted.
     * @return a decrypted byte array.
     */
    byte[] encrypt(@NonNull final Key key,
                   @NonNull final String encryptAlgorithm,
                   final byte[] iv,
                   final byte[] dataToBeEncrypted) throws ClientException;

    /**
     * Encrypt the given byte array.
     *
     * @param key               the key to encrypt with.
     * @param encryptAlgorithm  algorithm to encrypt with.
     * @param iv                an initialization vector (IV).
     * @param dataToBeEncrypted the data to be encrypted.
     * @return a decrypted byte array.
     */
    byte[] encrypt(@NonNull final Key key,
                   @NonNull final String encryptAlgorithm,
                   final byte[] iv,
                   final byte[] dataToBeEncrypted,
                   final int tagLength,
                   final byte[] aad) throws ClientException;
}
