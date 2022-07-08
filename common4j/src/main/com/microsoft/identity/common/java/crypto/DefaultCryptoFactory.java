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
import com.microsoft.identity.common.java.util.StringUtil;

import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.security.Signature;
import java.security.UnrecoverableKeyException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.Nullable;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import lombok.NonNull;

// Use the default provider for every operations.
public class DefaultCryptoFactory implements ICryptoFactory {

    private Provider mOverriddenDefaultProvider;
    private final ReadWriteLock mLock = new ReentrantReadWriteLock();

    @Override
    @NonNull
    public Signature getSignature(@NonNull final String algorithm) throws ClientException {
        mLock.readLock().lock();
        try {
            return ProviderFactory.getSignature(algorithm, null);
        } finally {
            mLock.readLock().unlock();
        }
    }

    @Override
    @NonNull
    public Cipher getCipher(@NonNull final String algorithm) throws ClientException {
        mLock.readLock().lock();
        try {
            return ProviderFactory.getCipher(algorithm, null);
        } finally {
            mLock.readLock().unlock();
        }
    }

    @Override
    @NonNull
    public Mac getMac(@NonNull String algorithm) throws ClientException {
        mLock.readLock().lock();
        try {
            return ProviderFactory.getMac(algorithm, null);
        } finally {
            mLock.readLock().unlock();
        }
    }

    @Override
    @NonNull
    public KeyPairGenerator getKeyPairGenerator(@NonNull final String algorithm) throws ClientException {
        mLock.readLock().lock();
        try {
            return ProviderFactory.getKeyPairGenerator(algorithm, null);
        } finally {
            mLock.readLock().unlock();
        }
    }

    @Override
    @NonNull
    public KeyStore getCustomKeyStore() throws ClientException {
        mLock.readLock().lock();
        try {
            return ProviderFactory.getCustomKeyStore(null);
        } finally {
            mLock.readLock().unlock();
        }
    }

    @Override
    @NonNull
    public KeyFactory getKeyFactory(@NonNull final String algorithm) throws ClientException {
        mLock.readLock().lock();
        try {
            return ProviderFactory.getKeyFactory(algorithm, null);
        } finally {
            mLock.readLock().unlock();
        }
    }

    @Override
    @NonNull
    public KeyManagerFactory getKeyManagerFactory(@NonNull final String algorithm) throws ClientException {
        mLock.readLock().lock();
        try {
            return ProviderFactory.getKeyManagerFactory(algorithm, null);
        } finally {
            mLock.readLock().unlock();
        }
    }

    @Override
    @NonNull
    public TrustManagerFactory getTrustManagerFactory(@NonNull final String algorithm)
            throws ClientException {
        mLock.readLock().lock();
        try {
            return ProviderFactory.getTrustManagerFactory(algorithm, null);
        } finally {
            mLock.readLock().unlock();
        }
    }

    @Override
    @NonNull
    public SSLContext getSSLContext(@NonNull String algorithm) throws ClientException {
        mLock.readLock().lock();
        try {
            return ProviderFactory.getSSLContext(algorithm, null);
        } finally {
            mLock.readLock().unlock();
        }
    }

    @Override
    public void setProviderForWebView() {
        // do nothing.
    }

    @Override
    public void unsetProviderForWebView() {
        // do nothing.
    }

    @Override
    public void overrideDefaultProvider(@NonNull final Provider provider) {
        mLock.writeLock().lock();
        if (mOverriddenDefaultProvider != null) {
            Security.removeProvider(mOverriddenDefaultProvider.getName());
            mOverriddenDefaultProvider = null;
        }

        if (Security.getProvider(provider.getName()) != null) {
            // The provider already exists in the list.
            return;
        }

        mOverriddenDefaultProvider = provider;
        Security.insertProviderAt(mOverriddenDefaultProvider, 1);
    }

    @Override
    public void removeOverriddenDefaultProvider() {
        mLock.writeLock().lock();
        try {
            if (mOverriddenDefaultProvider != null) {
                Security.removeProvider(mOverriddenDefaultProvider.getName());
                mOverriddenDefaultProvider = null;
            }
        } finally {
            mLock.writeLock().unlock();
        }
    }
}
