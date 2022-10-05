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
import com.microsoft.identity.common.java.opentelemetry.CryptoFactoryOperationName;
import com.microsoft.identity.common.java.opentelemetry.ICryptoOperationCallback;

import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.Signature;

import javax.crypto.Cipher;
import javax.crypto.Mac;

import lombok.NonNull;

/**
 * Representing a class for generating crypto objects.
 */
public interface ICryptoFactory {

    /**
     * Gets a {@link Signature} crypto object
     */
    @NonNull
    Signature getSignature(@NonNull final String algorithm) throws ClientException;

    /**
     * Gets a {@link Cipher} crypto object
     */
    @NonNull
    Cipher getCipher(@NonNull final String algorithm) throws ClientException;

    /**
     * Gets a {@link Mac} crypto object
     */
    @NonNull
    Mac getMac(@NonNull final String algorithm) throws ClientException;

    /**
     * Gets a {@link KeyPairGenerator} crypto object
     */
    @NonNull
    KeyPairGenerator getKeyPairGenerator(@NonNull final String algorithm) throws ClientException;

    /**
     * Gets a {@link KeyFactory} crypto object
     */
    @NonNull
    KeyFactory getKeyFactory(@NonNull final String algorithm) throws ClientException;
}
