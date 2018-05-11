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
package com.microsoft.identity.common.unit;

import com.microsoft.identity.common.internal.providers.keys.CertificateCredential;
import com.microsoft.identity.common.internal.providers.keys.ClientCertificateMetadata;
import com.microsoft.identity.common.internal.providers.keys.KeyStoreConfiguration;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.KeyStoreSpi;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.Date;
import java.util.Enumeration;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({KeyStore.class, CertificateCredential.class, CertificateCredential.CertificateCredentialBuilder.class})
public class CertificateCredentialBuilder {

    public static final String WINDOWS_MY_KEYSTORE = "Windows-MY";
    public static final String WINDOWS_ROOT_KEYSTORE = "Windows-ROOT";
    public static final String CERTIFICATE_ALIAS = "AutomationCertificate";
    public static final char[] CERTIFICATE_PASSWORD = new char[]{'P', 'a', 's', 's'};
    public static final String CERTIFICATE_SUBJECT = "AutomationApp";
    public static final String WINDOWS_KEYSTORE_PROVIDER = "SunMSCAPI";
    public static final String CLIENT_ID = "ASDF";


    private Key privateKey = new RSAPrivateKey() {
        @Override
        public BigInteger getPrivateExponent() {
            return null;
        }

        @Override
        public String getAlgorithm() {
            return null;
        }

        @Override
        public String getFormat() {
            return null;
        }

        @Override
        public byte[] getEncoded() {
            return new byte[0];
        }

        @Override
        public BigInteger getModulus() {
            return null;
        }
    };

    private Certificate certificate = new X509Certificate() {
        @Override
        public void checkValidity() throws CertificateExpiredException, CertificateNotYetValidException {

        }

        @Override
        public void checkValidity(Date date) throws CertificateExpiredException, CertificateNotYetValidException {

        }

        @Override
        public int getVersion() {
            return 0;
        }

        @Override
        public BigInteger getSerialNumber() {
            return null;
        }

        @Override
        public Principal getIssuerDN() {
            return null;
        }

        @Override
        public Principal getSubjectDN() {
            return null;
        }

        @Override
        public Date getNotBefore() {
            return null;
        }

        @Override
        public Date getNotAfter() {
            return null;
        }

        @Override
        public byte[] getTBSCertificate() throws CertificateEncodingException {
            return new byte[0];
        }

        @Override
        public byte[] getSignature() {
            return new byte[0];
        }

        @Override
        public String getSigAlgName() {
            return null;
        }

        @Override
        public String getSigAlgOID() {
            return null;
        }

        @Override
        public byte[] getSigAlgParams() {
            return new byte[0];
        }

        @Override
        public boolean[] getIssuerUniqueID() {
            return new boolean[0];
        }

        @Override
        public boolean[] getSubjectUniqueID() {
            return new boolean[0];
        }

        @Override
        public boolean[] getKeyUsage() {
            return new boolean[0];
        }

        @Override
        public int getBasicConstraints() {
            return 0;
        }

        @Override
        public byte[] getEncoded() throws CertificateEncodingException {
            return new byte[0];
        }

        @Override
        public void verify(PublicKey publicKey) throws CertificateException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, SignatureException {

        }

        @Override
        public void verify(PublicKey publicKey, String s) throws CertificateException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, SignatureException {

        }

        @Override
        public String toString() {
            return CERTIFICATE_SUBJECT;
        }

        @Override
        public PublicKey getPublicKey() {
            return null;
        }

        @Override
        public boolean hasUnsupportedCriticalExtension() {
            return false;
        }

        @Override
        public Set<String> getCriticalExtensionOIDs() {
            return null;
        }

        @Override
        public Set<String> getNonCriticalExtensionOIDs() {
            return null;
        }

        @Override
        public byte[] getExtensionValue(String s) {
            return new byte[0];
        }
    };


    @Mock
    private KeyStore keyStoreMock;

    @InjectMocks
    private KeyStoreSpi keyStoreSpiMock = new KeyStoreSpi() {
        @Override
        public Key engineGetKey(String s, char[] chars) throws NoSuchAlgorithmException, UnrecoverableKeyException {
            return privateKey;
        }

        @Override
        public Certificate[] engineGetCertificateChain(String s) {
            return new Certificate[0];
        }

        @Override
        public Certificate engineGetCertificate(String s) {
            return null;
        }

        @Override
        public Date engineGetCreationDate(String s) {
            return null;
        }

        @Override
        public void engineSetKeyEntry(String s, Key key, char[] chars, Certificate[] certificates) throws KeyStoreException {

        }

        @Override
        public void engineSetKeyEntry(String s, byte[] bytes, Certificate[] certificates) throws KeyStoreException {

        }

        @Override
        public void engineSetCertificateEntry(String s, Certificate certificate) throws KeyStoreException {

        }

        @Override
        public void engineDeleteEntry(String s) throws KeyStoreException {

        }

        @Override
        public Enumeration<String> engineAliases() {
            return null;
        }

        @Override
        public boolean engineContainsAlias(String s) {
            return false;
        }

        @Override
        public int engineSize() {
            return 0;
        }

        @Override
        public boolean engineIsKeyEntry(String s) {
            return false;
        }

        @Override
        public boolean engineIsCertificateEntry(String s) {
            return false;
        }

        @Override
        public String engineGetCertificateAlias(Certificate certificate) {
            return null;
        }

        @Override
        public void engineStore(OutputStream outputStream, char[] chars) throws IOException, NoSuchAlgorithmException, CertificateException {

        }

        @Override
        public void engineLoad(InputStream inputStream, char[] chars) throws IOException, NoSuchAlgorithmException, CertificateException {

        }
    };


    @Before
    public void setup() throws Exception, KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException {

        Whitebox.setInternalState(keyStoreMock, "initialized", true);
        Whitebox.setInternalState(keyStoreMock, "keyStoreSpi", keyStoreSpiMock);
        when(keyStoreMock.getKey(CERTIFICATE_ALIAS, null)).thenReturn(privateKey);
        when(keyStoreMock.getKey(CERTIFICATE_ALIAS, CERTIFICATE_PASSWORD)).thenReturn(privateKey);
        when(keyStoreMock.getCertificate(CERTIFICATE_ALIAS)).thenReturn(certificate);

    }

    @Test
    public void test_CertificateBuilder_LookupCertificate_IsCorrect() throws
            KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException, NoSuchProviderException,
            IOException, CertificateException {

        PowerMockito.mockStatic(KeyStore.class);
        when(KeyStore.getInstance(WINDOWS_MY_KEYSTORE, WINDOWS_KEYSTORE_PROVIDER)).thenReturn(keyStoreMock);

        CertificateCredential cred = new CertificateCredential.CertificateCredentialBuilder(CLIENT_ID)
                .keyStoreConfiguration(new KeyStoreConfiguration(WINDOWS_MY_KEYSTORE, WINDOWS_KEYSTORE_PROVIDER, null))
                .clientCertificateMetadata(new ClientCertificateMetadata(CERTIFICATE_ALIAS, null))
                .build();

        assertEquals(privateKey, cred.getPrivateKey());
        assertEquals(certificate, cred.getPublicCertificate());
        assertEquals(CLIENT_ID, cred.getClientId());

    }

    @Test
    public void test_CertificateBuilder_LookupCertificateWithPassword_IsCorrect() throws
            KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException, NoSuchProviderException,
            IOException, CertificateException {

        PowerMockito.mockStatic(KeyStore.class);
        when(KeyStore.getInstance(WINDOWS_MY_KEYSTORE, WINDOWS_KEYSTORE_PROVIDER)).thenReturn(keyStoreMock);

        CertificateCredential cred = new CertificateCredential.CertificateCredentialBuilder(CLIENT_ID)
                .keyStoreConfiguration(new KeyStoreConfiguration(WINDOWS_MY_KEYSTORE, WINDOWS_KEYSTORE_PROVIDER, null))
                .clientCertificateMetadata(new ClientCertificateMetadata(CERTIFICATE_ALIAS, CERTIFICATE_PASSWORD))
                .build();

        assertEquals(privateKey, cred.getPrivateKey());
        assertEquals(certificate, cred.getPublicCertificate());
        assertEquals(CLIENT_ID, cred.getClientId());

    }

    @Test
    public void test_CertificateBuilder_ProvideCertificate_IsCorrect() throws CertificateException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, NoSuchProviderException, IOException {

        CertificateCredential credential = new CertificateCredential.CertificateCredentialBuilder(CLIENT_ID)
                .certificate((X509Certificate) certificate)
                .privateKey((PrivateKey) privateKey)
                .build();

        assertEquals(privateKey, credential.getPrivateKey());
        assertEquals(certificate, credential.getPublicCertificate());
        assertEquals(CLIENT_ID, credential.getClientId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_CertificateBuilder_ClientIdNull_ExpectException() throws CertificateException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, NoSuchProviderException, IOException {
        CertificateCredential credential = new CertificateCredential.CertificateCredentialBuilder(null)
                .certificate((X509Certificate) certificate)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_CertificateBuilder_NoPrivateKey_ExpectException() throws CertificateException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, NoSuchProviderException, IOException {
        CertificateCredential credential = new CertificateCredential.CertificateCredentialBuilder(CLIENT_ID)
                .certificate((X509Certificate) certificate)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_CertificateBuilder_NoCertificate_ExpectException() throws CertificateException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, NoSuchProviderException, IOException {
        CertificateCredential credential = new CertificateCredential.CertificateCredentialBuilder(CLIENT_ID)
                .privateKey((PrivateKey) privateKey)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_CertificateBuilder_NoCertificateMetadata_ExpectException() throws CertificateException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, NoSuchProviderException, IOException {
        PowerMockito.mockStatic(KeyStore.class);
        when(KeyStore.getInstance(WINDOWS_MY_KEYSTORE, WINDOWS_KEYSTORE_PROVIDER)).thenReturn(keyStoreMock);

        CertificateCredential cred = new CertificateCredential.CertificateCredentialBuilder(CLIENT_ID)
                .keyStoreConfiguration(new KeyStoreConfiguration(WINDOWS_MY_KEYSTORE, WINDOWS_KEYSTORE_PROVIDER, null))
                .build();

    }

    @Test(expected = IllegalArgumentException.class)
    public void test_CertificateBuilder_NoKeyStoreConfiguration_ExpectException() throws CertificateException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, NoSuchProviderException, IOException {
        PowerMockito.mockStatic(KeyStore.class);
        when(KeyStore.getInstance(WINDOWS_MY_KEYSTORE, WINDOWS_KEYSTORE_PROVIDER)).thenReturn(keyStoreMock);

        CertificateCredential cred = new CertificateCredential.CertificateCredentialBuilder(CLIENT_ID)
                .keyStoreConfiguration(new KeyStoreConfiguration(WINDOWS_MY_KEYSTORE, WINDOWS_KEYSTORE_PROVIDER, null))
                .build();

    }

}