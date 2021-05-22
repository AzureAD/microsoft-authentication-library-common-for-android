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
package java.com.microsoft.identity.labapi.utilities.authentication.common.port;

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

@Getter
@Accessors(prefix = "m")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CertificateCredential {
    private final PrivateKey mPrivateKey;
    private final X509Certificate mPublicCertificate;

    public CertificateCredential create(@NonNull final PrivateKey privateKey,
                                        @NonNull final X509Certificate certificate) {
        return new CertificateCredential(privateKey, certificate);
    }

    @SneakyThrows
    public CertificateCredential create(@NonNull final KeyStoreConfiguration keyStoreConfiguration,
                                        @NonNull final ClientCertificateMetadata clientCertificateMetadata) {
        return getCertificateInfoFromStore(keyStoreConfiguration, clientCertificateMetadata);
    }

    private CertificateCredential getCertificateInfoFromStore(@NonNull final KeyStoreConfiguration keyStoreConfiguration,
                                                              @NonNull final ClientCertificateMetadata clientCertificateMetadata)
            throws NoSuchProviderException, KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException,
            IOException, CertificateException {

        final KeyStore keystore = KeyStore.getInstance(
                keyStoreConfiguration.getKeyStoreType(), keyStoreConfiguration.getKeyStoreProvider()
        );

        keystore.load(null, keyStoreConfiguration.getKeyStorePassword());

        final PrivateKey key;

        //TODO: Adding logging for the two different cases.  The Microsoft Certificate Store does not require a password
        if (clientCertificateMetadata.getPassword() == null) {
            key = (PrivateKey) keystore.getKey(clientCertificateMetadata.getAlias(), null);
        } else {
            key = (PrivateKey) keystore.getKey(clientCertificateMetadata.getAlias(),
                    clientCertificateMetadata.getPassword());
        }

        final X509Certificate publicCertificate = (X509Certificate) keystore
                .getCertificate(clientCertificateMetadata.getAlias());

        return new CertificateCredential(key, publicCertificate);
    }
}
