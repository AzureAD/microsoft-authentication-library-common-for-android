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

import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.Provider;
import java.security.Signature;

import javax.annotation.Nullable;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import lombok.NonNull;

public interface ICryptoFactory {

    @NonNull
    Signature getSignature(@NonNull final String algorithm) throws ClientException;

    @NonNull
    Cipher getCipher(@NonNull final String algorithm) throws ClientException;

    @NonNull
    Mac getMac(@NonNull final String algorithm) throws ClientException;

    @NonNull
    KeyPairGenerator getKeyPairGenerator(@NonNull final String algorithm) throws ClientException;

    @NonNull
    KeyStore getCustomKeyStore() throws ClientException;

    @NonNull
    KeyFactory getKeyFactory(@NonNull final String algorithm) throws ClientException;

    // KeyManager decides which authentication credentials should be sent to the remote host for authentication during SSL handshake
    @NonNull KeyManagerFactory getKeyManagerFactory(@NonNull String algorithm) throws ClientException;

    // TrustManager determines whether remote connection should be trusted or not
    @NonNull
    TrustManagerFactory getTrustManagerFactory(@NonNull final String algorithm) throws ClientException;

    @NonNull
    SSLContext getSSLContext(@NonNull final String algorithm) throws ClientException;

    void setProviderForWebView();

    void unsetProviderForWebView();

    // When this is set, Other operations that are relying on the default provider will be blocked
    // until removeOverriddenProvider() is invoked.
    void overrideDefaultProvider(@NonNull final Provider provider);

    void removeOverriddenDefaultProvider();
}
