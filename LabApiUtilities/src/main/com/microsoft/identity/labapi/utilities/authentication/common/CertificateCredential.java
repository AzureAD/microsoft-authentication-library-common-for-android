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
package com.microsoft.identity.labapi.utilities.authentication.common;

import com.microsoft.identity.labapi.utilities.authentication.exception.LabApiException;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;

import static com.microsoft.identity.labapi.utilities.authentication.exception.LabError.CERTIFICATE_NOT_FOUND_IN_KEY_STORE;

/**
 * Represents a certificate credential used for creating a client assertion.
 *
 * Lomboked from original source located at:
 * https://github.com/AzureAD/microsoft-authentication-library-common-for-android/blob/dev/common/src/main/java/com/microsoft/identity/common/internal/providers/keys/CertificateCredential.java
 */
@Getter
@Accessors(prefix = "m")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CertificateCredential {

    @NonNull
    private final PrivateKey mPrivateKey;

    @NonNull
    private final X509Certificate mPublicCertificate;

    /**
     * Create a Certificate Credential using a Private Key and X509 Certificate.
     *
     * @param privateKey  the {@link PrivateKey} to sign the assertion
     * @param certificate the {@link X509Certificate} used for thumbprint
     * @return a Certificate Credential object
     */
    public static CertificateCredential create(@NonNull final PrivateKey privateKey,
                                               @NonNull final X509Certificate certificate) {
        return new CertificateCredential(privateKey, certificate);
    }

    /**
     * Create a Certificate Credential using a KeyStore Configuration and Client Certificate
     * Metadata.
     *
     * @param keyStoreConfiguration     the {@link KeyStoreConfiguration} to access KeyStore
     * @param clientCertificateMetadata the {@link ClientCertificateMetadata} of the cert
     * @return a Certificate Credential object
     */
    @SneakyThrows
    public static CertificateCredential create(@NonNull final KeyStoreConfiguration keyStoreConfiguration,
                                               @NonNull final ClientCertificateMetadata clientCertificateMetadata) throws LabApiException {
        return getCertificateInfoFromStore(keyStoreConfiguration, clientCertificateMetadata);
    }

    private static CertificateCredential getCertificateInfoFromStore(@NonNull final KeyStoreConfiguration keyStoreConfiguration,
                                                                     @NonNull final ClientCertificateMetadata clientCertificateMetadata)
            throws NoSuchProviderException, KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException,
            IOException, CertificateException, LabApiException {

        final KeyStore keystore = KeyStore.getInstance(
                keyStoreConfiguration.getKeyStoreType(), keyStoreConfiguration.getKeyStoreProvider()
        );

        keystore.load(null, keyStoreConfiguration.getKeyStorePassword());

        final PrivateKey key;

        if (clientCertificateMetadata.getPassword() == null) {
            // The Microsoft Certificate Store does not require a password
            key = (PrivateKey) keystore.getKey(clientCertificateMetadata.getAlias(), null);
        } else {
            key = (PrivateKey) keystore.getKey(clientCertificateMetadata.getAlias(),
                    clientCertificateMetadata.getPassword());
        }

        final X509Certificate publicCertificate = (X509Certificate) keystore
                .getCertificate(clientCertificateMetadata.getAlias());

        if (key == null || publicCertificate == null) {
            throw new LabApiException(CERTIFICATE_NOT_FOUND_IN_KEY_STORE);
        }

        return new CertificateCredential(key, publicCertificate);
    }
}
