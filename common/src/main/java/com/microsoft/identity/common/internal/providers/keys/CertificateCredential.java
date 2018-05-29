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
package com.microsoft.identity.common.internal.providers.keys;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Represents a certificate credential.
 */
public final class CertificateCredential {

    private static final int MIN_KEYSIZE_IN_BITS = 2048; //NOPMD Suppressing PMD warning for unused constant
    private final PrivateKey mPrivateKey;
    private final String mClientId;
    private final X509Certificate mPublicCertificate;

    /**
     * @return mPrivateKey of the CertificateCredential object
     */
    public PrivateKey getPrivateKey() {
        return mPrivateKey;
    }

    /**
     * @return mClientId of the CertificateCredential object
     */
    public String getClientId() {
        return mClientId;
    }

    /**
     * @return mPublicCertificate of the CertificateCredential object
     */
    public X509Certificate getPublicCertificate() {
        return mPublicCertificate;
    }

    private CertificateCredential(CertificateCredentialBuilder builder) {
        mClientId = builder.mClientId;
        mPublicCertificate = builder.mCertificate;
        mPrivateKey = builder.mPrivateKey;
    }

    public static class CertificateCredentialBuilder {
        private String mClientId;
        private PrivateKey mPrivateKey;
        private KeyStoreConfiguration mKeyStoreConfiguration;
        private ClientCertificateMetadata mClientCertificateMetdata;
        private X509Certificate mCertificate;

        /**
         * Constructor of CertificateCredentialBuilder.
         *
         * @param clientId String
         */
        public CertificateCredentialBuilder(String clientId) {
            mClientId = clientId;
        }

        /**
         * Get the CertificateCredentialBuilder from PrivateKey.
         *
         * @param key PrivateKey
         * @return CertificateCredentialBuilder
         */
        public CertificateCredentialBuilder privateKey(PrivateKey key) {
            mPrivateKey = key;
            return this;
        }

        /**
         * Get the CertificateCredentialBuilder from KeyStoreConfiguration.
         *
         * @param keyStoreConfiguration KeyStoreConfiguration
         * @return CertificateCredentialBuilder
         */
        public CertificateCredentialBuilder keyStoreConfiguration(KeyStoreConfiguration keyStoreConfiguration) {
            mKeyStoreConfiguration = keyStoreConfiguration;
            return this;
        }

        /**
         * Get the CertificateCredentialBuilder from ClientCertificateMetadata.
         *
         * @param clientCertificateMetadata ClientCertificateMetadata
         * @return CertificateCredentialBuilder
         */
        public CertificateCredentialBuilder clientCertificateMetadata(ClientCertificateMetadata clientCertificateMetadata) {
            mClientCertificateMetdata = clientCertificateMetadata;
            return this;
        }

        /**
         * Get the CertificateCredentialBuilder from X509Certificate.
         *
         * @param certificate X509Certificate
         * @return CertificateCredentialBuilder
         */
        public CertificateCredentialBuilder certificate(X509Certificate certificate) {
            mCertificate = certificate;
            return this;
        }

        /**
         * Get the CertificateCredential object.
         *
         * @return CertificateCredential
         * @throws NoSuchProviderException   thrown when a particular security provider is requested but is not available in the environment.
         * @throws KeyStoreException         generic KeyStore exception.
         * @throws NoSuchAlgorithmException  thrown when a particular cryptographic algorithm is requested but is not available in the environment.
         * @throws UnrecoverableKeyException thrown if a key in the keystore cannot be recovered.
         * @throws IOException               thrown if failed or interrupted I/O operations happen.
         * @throws CertificateException      thrown if one of a variety of certificate problems happen.
         */
        public CertificateCredential build()
                throws NoSuchProviderException, KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException,
                IOException, CertificateException {

            CertificateCredential cred = null;

            if (mClientId != null) {
                if (mCertificate != null && mPrivateKey != null) {
                    cred = new CertificateCredential(this);
                } else {
                    if (mClientCertificateMetdata != null && mKeyStoreConfiguration != null) {
                        getCertificateInfoFromStore(mKeyStoreConfiguration, mClientCertificateMetdata);
                        cred = new CertificateCredential(this);
                    }
                }
            }

            validateCertificateCredential(cred);

            return cred;

        }

        private void validateCertificateCredential(CertificateCredential cred) {
            //TODO: Add Logic for validating certificate credential - Verify not Null... which would be an invalid argument scenario
            if (cred == null) {
                throw new IllegalArgumentException("Client ID, Certificate and PrivateKey OR KeyStoreConfiguration and Certificate Metadata are required");
            }

        }

        private void getCertificateInfoFromStore(KeyStoreConfiguration keyStoreConfiguration, ClientCertificateMetadata clientCertificateMetadata)
                throws NoSuchProviderException, KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException,
                IOException, CertificateException {

            KeyStore keystore = KeyStore.getInstance(keyStoreConfiguration.getKeyStoreType(), keyStoreConfiguration.getKeyStoreProvider());
            keystore.load(null, null);

            PrivateKey key;

            //TODO: Adding logging for the two different cases.  The Microsoft Certificate Store does not require a password
            if (clientCertificateMetadata.getPassword() == null) {
                key = (PrivateKey) keystore.getKey(clientCertificateMetadata.getAlias(), null);
            } else {
                key = (PrivateKey) keystore.getKey(clientCertificateMetadata.getAlias(),
                        clientCertificateMetadata.getPassword());
            }

            final X509Certificate publicCertificate = (X509Certificate) keystore
                    .getCertificate(clientCertificateMetadata.getAlias());

            mPrivateKey = key;
            mCertificate = publicCertificate;

        }


    }


}




