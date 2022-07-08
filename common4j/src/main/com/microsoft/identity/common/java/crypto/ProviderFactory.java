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
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Signature;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

import javax.annotation.Nullable;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import lombok.NonNull;

public class ProviderFactory {

    @NonNull
    public static Signature getSignature(@NonNull final String algorithm,
                                         @Nullable final Provider provider) throws ClientException {
        try {
            if (provider != null) {
                return Signature.getInstance(algorithm, provider);
            }
            return Signature.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new ClientException(ClientException.NO_SUCH_ALGORITHM, e.getMessage(), e);
        }
    }

    @NonNull
    public static Cipher getCipher(@NonNull final String algorithm,
                                   @Nullable final Provider provider) throws ClientException {
        try {
            if (provider != null) {
                return Cipher.getInstance(algorithm, provider);
            }
            return Cipher.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new ClientException(ClientException.NO_SUCH_ALGORITHM, e.getMessage(), e);
        } catch (NoSuchPaddingException e) {
            throw new ClientException(ClientException.NO_SUCH_PADDING, e.getMessage(), e);
        }
    }

    @NonNull
    public static Mac getMac(@NonNull final String algorithm,
                             @Nullable final Provider provider) throws ClientException {
        try {
            if (provider != null) {
                return Mac.getInstance(algorithm, provider);
            }
            return Mac.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new ClientException(ClientException.NO_SUCH_ALGORITHM, e.getMessage(), e);
        }
    }

    @NonNull
    public static KeyPairGenerator getKeyPairGenerator(@NonNull final String algorithm,
                                                       @Nullable final Provider provider) throws ClientException {
        try {
            if (provider != null) {
                return KeyPairGenerator.getInstance(algorithm, provider);
            }
            return KeyPairGenerator.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new ClientException(ClientException.NO_SUCH_ALGORITHM, e.getMessage(), e);
        }
    }

    @NonNull
    public static KeyStore getCustomKeyStore(@Nullable final Provider provider) throws ClientException {
        try {
            if (provider != null) {
                return KeyStore.getInstance(KeyStore.getDefaultType(), provider);
            }
            return KeyStore.getInstance(KeyStore.getDefaultType());
        } catch (KeyStoreException e) {
            throw new ClientException(ClientException.KEYSTORE_NOT_INITIALIZED, e.getMessage(), e);
        }
    }

    @NonNull
    public static KeyFactory getKeyFactory(@NonNull final String algorithm,
                                           @Nullable final Provider provider) throws ClientException {
        try {
            if (provider != null) {
                return KeyFactory.getInstance(algorithm, provider);
            }
            return KeyFactory.getInstance(algorithm);
        } catch (final NoSuchAlgorithmException e) {
            throw new ClientException(ClientException.NO_SUCH_ALGORITHM, e.getMessage(), e);
        }
    }

    @NonNull
    public static KeyManagerFactory getKeyManagerFactory(@NonNull final String algorithm,
                                                         @Nullable final Provider provider) throws ClientException {
        try {
            if (provider != null) {
                return KeyManagerFactory.getInstance(algorithm, provider);
            }
            return KeyManagerFactory.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new ClientException(ClientException.NO_SUCH_ALGORITHM, e.getMessage(), e);
        }
    }

    @NonNull
    public static TrustManagerFactory getTrustManagerFactory(@NonNull final String algorithm,
                                                             @Nullable final Provider provider) throws ClientException {
        try {
            if (provider != null) {
                return TrustManagerFactory.getInstance(algorithm, provider);
            }
            return TrustManagerFactory.getInstance(algorithm);
        } catch (final NoSuchAlgorithmException e) {
            throw new ClientException(ClientException.NO_SUCH_ALGORITHM, e.getMessage(), e);
        }
    }

    @NonNull
    public static CertificateFactory getCertificateFactory(@NonNull final String algorithm,
                                                           @Nullable final Provider provider) throws ClientException {
        try {
            if (provider != null) {
                return CertificateFactory.getInstance(algorithm, provider);
            }
            return CertificateFactory.getInstance(algorithm);
        } catch (final CertificateException e) {
            throw new ClientException(ClientException.CERTIFICATE_LOAD_FAILURE, e.getMessage(), e);
        }
    }

    @NonNull
    public static SSLContext getSSLContext(@NonNull final String algorithm,
                                           @Nullable final Provider provider) throws ClientException {
        try {
            if (provider != null) {
                return SSLContext.getInstance(algorithm, provider);
            }
            return SSLContext.getInstance(algorithm);
        } catch (final NoSuchAlgorithmException e) {
            throw new ClientException(ClientException.NO_SUCH_ALGORITHM, e.getMessage(), e);
        }
    }
}
