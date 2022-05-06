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

import static com.microsoft.identity.common.java.exception.ClientException.INVALID_PROTECTION_PARAMS;
import static com.microsoft.identity.common.java.exception.ClientException.KEYSTORE_NOT_INITIALIZED;
import static com.microsoft.identity.common.java.exception.ClientException.NO_SUCH_ALGORITHM;
import static com.microsoft.identity.common.java.exception.ClientException.UNKNOWN_ERROR;

import android.os.Build;
import android.security.keystore.KeyInfo;

import com.microsoft.identity.common.java.crypto.SecureHardwareState;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.platform.AbstractKeyStoreKeyManager;
import com.microsoft.identity.common.java.util.Supplier;
import com.microsoft.identity.common.logging.Logger;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableEntryException;
import java.security.cert.Certificate;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;

import lombok.Builder;
import lombok.NonNull;
import lombok.experimental.Accessors;

/**
 * A manager class for providing access to a particular entry in a KeyStore.
 *
 * @param <K> the type of KeyStore.Entry being managed.
 */
@Accessors(prefix = "m")
public class AndroidDeviceKeyManager<K extends KeyStore.Entry> extends AbstractKeyStoreKeyManager<K> {

    private static final String TAG = AndroidDeviceKeyManager.class.getSimpleName();

    @Builder
    public AndroidDeviceKeyManager(@NonNull final KeyStore keyStore, @NonNull final String keyAlias) throws KeyStoreException {
        super(keyStore, keyAlias, null);
    }

    @Override
    public void storeAsymmetricKey(@NonNull final PrivateKey privateKey, @NonNull final Certificate[] certChain) {
        // Android Keystore stores the key in the keystore during key generation and we don't need
        // to explicitly invoke setKeyEntry. Other KeyStores such as the BKS keystore doesn't do
        // that and we need to explicitly invoke setKeyEntry and that's where this method comes in.
        // I implemented the functionality in the abstract class because if we add more
        // implementations in the future then they probably work similar to BKS implementation
        // because most KeyStores require calling setKeyEntry to save the key into the key store).
        throw new UnsupportedOperationException("This is not currently supported");
    }

    /**
     * Gets the {@link SecureHardwareState} of this key.
     *
     * @return The SecureHardwareState.
     * @throws ClientException If the underlying key material cannot be inspected.
     */
    @Override
    public SecureHardwareState getSecureHardwareState() throws ClientException {
        final String methodTag = TAG + ":getSecureHardwareState";
        final String errCode;
        final Exception exception;

        try {
            KeyStore.Entry entry = getEntry();
            if (entry instanceof KeyStore.PrivateKeyEntry) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    try {
                        final PrivateKey privateKey = ((KeyStore.PrivateKeyEntry) entry).getPrivateKey();
                        final KeyFactory factory = KeyFactory.getInstance(
                                privateKey.getAlgorithm(), mKeyStore.getProvider()
                        );
                        final KeyInfo info = factory.getKeySpec(privateKey, KeyInfo.class);
                        final boolean isInsideSecureHardware = info.isInsideSecureHardware();
                        Logger.info(methodTag, "PrivateKey is secure hardware backed? " + isInsideSecureHardware);
                        return isInsideSecureHardware
                                ? SecureHardwareState.TRUE_UNATTESTED
                                : SecureHardwareState.FALSE;
                    } catch (final NoSuchAlgorithmException | InvalidKeySpecException e) {
                        Logger.error(methodTag, "Failed to query secure hardware state.", e);
                        return SecureHardwareState.UNKNOWN_QUERY_ERROR;
                    }
                } else {
                    Logger.info(methodTag, "Cannot query secure hardware state (API unavailable <23)");
                }

                return SecureHardwareState.UNKNOWN_DOWNLEVEL;
            } else if (entry instanceof KeyStore.SecretKeyEntry) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    try {
                        final SecretKey privateKey = ((KeyStore.SecretKeyEntry) entry).getSecretKey();
                        final SecretKeyFactory factory = SecretKeyFactory.getInstance(
                                privateKey.getAlgorithm(), mKeyStore.getProvider()
                        );
                        final KeyInfo info = (KeyInfo) factory.getKeySpec(privateKey, KeyInfo.class);
                        final boolean isInsideSecureHardware = info.isInsideSecureHardware();
                        Logger.info(methodTag, "SecretKey is secure hardware backed? " + isInsideSecureHardware);
                        return isInsideSecureHardware
                                ? SecureHardwareState.TRUE_UNATTESTED
                                : SecureHardwareState.FALSE;
                    } catch (final NoSuchAlgorithmException | InvalidKeySpecException e) {
                        Logger.error(methodTag, "Failed to query secure hardware state.", e);
                        return SecureHardwareState.UNKNOWN_QUERY_ERROR;
                    }
                } else {
                    Logger.info(methodTag, "Cannot query secure hardware state (API unavailable <23)");
                }
                return SecureHardwareState.UNKNOWN_DOWNLEVEL;
            } else {
                throw new ClientException(UNKNOWN_ERROR, "Cannot handle entries of type " + entry.getClass().getCanonicalName());
            }
        } catch (final KeyStoreException e) {
            errCode = KEYSTORE_NOT_INITIALIZED;
            exception = e;
        } catch (final NoSuchAlgorithmException e) {
            errCode = NO_SUCH_ALGORITHM;
            exception = e;
        } catch (final UnrecoverableEntryException e) {
            errCode = INVALID_PROTECTION_PARAMS;
            exception = e;
        }

        final ClientException clientException = new ClientException(
                errCode,
                exception.getMessage(),
                exception
        );

        Logger.error(
                methodTag,
                errCode,
                exception
        );

        throw clientException;
    }

}
