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

import android.os.Build;
import android.security.keystore.KeyProperties;
import android.security.keystore.KeyProtection;

import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.util.Supplier;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.util.Arrays;
import java.util.Date;

import javax.crypto.spec.SecretKeySpec;

import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * A manager class for providing access to a particular entry in a KeyStore.
 * @param <K> the type of KeyStore.Entry being managed.
 */
@Accessors(prefix = "m")
public class DeviceKeyManager<K extends KeyStore.Entry> implements IKeyManager<K> {

    private static final String TAG = DeviceKeyManager.class.getSimpleName();
    private final KeyStore mKeyStore;

    @Builder
    public DeviceKeyManager(KeyStore keyStore, String keyAlias, Supplier<byte[]> thumbprintSupplier) throws KeyStoreException {
        this.mKeyAlias = keyAlias;
        this.mThumbprintSupplier = thumbprintSupplier;
        this.mKeyStore = keyStore;
    }

    @Getter
    private final String mKeyAlias;
    private final Supplier<byte[]> mThumbprintSupplier;

    @Override
    public boolean exists() {
        boolean exists = false;

        try {
            exists = mKeyStore.containsAlias(mKeyAlias);
        } catch (final KeyStoreException e) {
            Logger.error(
                    TAG,
                    "Error while querying KeyStore",
                    e
            );
        }

        return exists;

    }

    @Override
    public boolean hasThumbprint(byte[] thumbprint) {
        return Arrays.equals(thumbprint, mThumbprintSupplier.get());
    }

    @Override
    public Date getCreationDate() throws ClientException {
        try {
            return mKeyStore.getCreationDate(mKeyAlias);
        } catch (KeyStoreException e) {
            Logger.error(
                    TAG,
                    "Error while getting creation date for alias " + mKeyAlias,
                    e
            );
            throw new ClientException(ClientException.KEYSTORE_NOT_INITIALIZED, e.getMessage(), e);
        }
    }

    @Override
    public boolean clear() {
        boolean deleted = false;

        try {
            mKeyStore.deleteEntry(mKeyAlias);
            deleted = true;
        } catch (final KeyStoreException e) {
            Logger.error(
                    TAG,
                    "Error while clearing KeyStore",
                    e
            );
        }

        return deleted;
    }


    /**
     * Retrieve the entry stored in this particular alias.
     * @return the Entry in question, or null if the entry does not exist.
     * @throws UnrecoverableEntryException
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     */
    @Override
    public K getEntry() throws UnrecoverableEntryException, NoSuchAlgorithmException, KeyStoreException {
        return (K) mKeyStore.getEntry(mKeyAlias, null);
    }

    /**
     *
     */
    @Override
    public void importKey(byte[] jwk, String algorithm) throws ClientException {
        final SecretKeySpec key = new SecretKeySpec(jwk, algorithm);
        final String errCode;
        final Exception exception;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                mKeyStore.setEntry(mKeyAlias, new KeyStore.SecretKeyEntry(key),
                        new KeyProtection.Builder(KeyProperties.PURPOSE_DECRYPT | KeyProperties.PURPOSE_ENCRYPT |
                                KeyProperties.PURPOSE_SIGN).build());
                return;
            } catch (KeyStoreException e) {
              errCode = ClientException.KEYSTORE_NOT_INITIALIZED;
              exception = e;
            }
            throw new ClientException(errCode, exception.getMessage());
        }
    }

    @Override
    public byte[] getThumbprint() {
        return mThumbprintSupplier.get();
    }
}
