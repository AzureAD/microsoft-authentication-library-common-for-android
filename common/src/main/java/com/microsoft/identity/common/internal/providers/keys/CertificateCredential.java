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
 * Represents a certificate credential
 *
 */
public class CertificateCredential {

    private final static int MIN_KEYSIZE_IN_BITS = 2048;
    private final PrivateKey mPrivateKey;
    private final String mClientId;
    private final X509Certificate mPublicCertificate;

    public PrivateKey getPrivateKey(){
        return this.mPrivateKey;
    }

    public String getClientId(){
        return this.mClientId;
    }

    public X509Certificate getPublicCertificate(){
        return this.mPublicCertificate;
    }

    private CertificateCredential(CertificateCredentialBuilder builder){
        this.mClientId = builder.mClientId;
        this.mPublicCertificate = builder.mCertificate;
        this.mPrivateKey = builder.mPrivateKey;
    }

    public static class CertificateCredentialBuilder {
        private String mClientId;
        private PrivateKey mPrivateKey;
        private KeyStoreConfiguration mKeyStoreConfiguration;
        private ClientCertificateMetadata mClientCertificateMetdata;
        private X509Certificate mCertificate;

        public CertificateCredentialBuilder(String clientId){
            this.mClientId = clientId;
        }

        public CertificateCredentialBuilder privateKey(PrivateKey key) {
            this.mPrivateKey = key;
            return this;
        }

        public CertificateCredentialBuilder keyStoreConfiguration(KeyStoreConfiguration keyStoreConfiguration) {
            this.mKeyStoreConfiguration = keyStoreConfiguration;
            return this;
        }

        public CertificateCredentialBuilder clientCertificateMetadata(ClientCertificateMetadata clientCertificateMetadata) {
            this.mClientCertificateMetdata = clientCertificateMetadata;
            return this;
        }

        public CertificateCredentialBuilder certificate(X509Certificate certificate) {
            this.mCertificate = certificate;
            return this;
        }

        public CertificateCredential build()
                throws NoSuchProviderException, KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException,
                 IOException, CertificateException{

            CertificateCredential cred = null;

            if(this.mClientId != null) {
                if (this.mCertificate != null && this.mPrivateKey != null) {
                    cred = new CertificateCredential(this);
                }else {
                    if(this.mClientCertificateMetdata != null && this.mKeyStoreConfiguration != null) {
                        getCertificateInfoFromStore(this.mKeyStoreConfiguration, this.mClientCertificateMetdata);
                        cred = new CertificateCredential(this);
                    }
                }
            }

            validateCertificateCredential(cred);

            return cred;

        }

        private void validateCertificateCredential(CertificateCredential cred) {
            //TODO: Add Logic for validating certificate credential - Verify not Null... which would be an invalid argument scenario
        }

        private void getCertificateInfoFromStore(KeyStoreConfiguration keyStoreConfiguration, ClientCertificateMetadata clientCertificateMetadata)
                throws NoSuchProviderException, KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException,
                IOException, CertificateException{

            final KeyStore keystore = KeyStore.getInstance(keyStoreConfiguration.getKeyStoreType(), keyStoreConfiguration.getKeyStoreProvider());
            keystore.load(null, null);

            PrivateKey key;

            //TODO: Adding logging for the two different cases.  The Microsoft Certificate Store does not require a password
            if(clientCertificateMetadata.getPassword() == null){
                key = (PrivateKey) keystore.getKey(clientCertificateMetadata.getAlias(), null);
            }else {
                key = (PrivateKey) keystore.getKey(clientCertificateMetadata.getAlias(),
                        clientCertificateMetadata.getPassword());
            }

            final X509Certificate publicCertificate = (X509Certificate) keystore
                    .getCertificate(clientCertificateMetadata.getAlias());

            this.mPrivateKey = key;
            this.mCertificate = publicCertificate;

        }


    }


}




