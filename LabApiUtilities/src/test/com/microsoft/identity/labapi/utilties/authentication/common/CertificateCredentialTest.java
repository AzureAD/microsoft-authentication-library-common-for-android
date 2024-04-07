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
package com.microsoft.identity.labapi.utilties.authentication.common;

import com.microsoft.identity.labapi.utilities.authentication.common.CertificateCredential;
import com.microsoft.identity.labapi.utilities.authentication.common.ClientCertificateMetadata;
import com.microsoft.identity.labapi.utilities.authentication.common.KeyStoreConfiguration;
import com.microsoft.identity.labapi.utilities.exception.LabApiException;
import com.microsoft.identity.labapi.utilities.exception.LabError;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Set;

/**
 * A test to validate that we can create instances of {@link CertificateCredential}.
 */
public class CertificateCredentialTest {

    private final static String CERTIFICATE_ALIAS_INVALID = "SomeRandomCertThatShouldNotExist";
    private final static String CERTIFICATE_ALIAS_VALID = "AutomationRunner";
    private final static String KEYSTORE_TYPE = "Windows-MY";
    private final static String KEYSTORE_PROVIDER = "SunMSCAPI";

    @Test
    public void testCanCreateCertificateCredentialFromPrivateKeyAndCertificate() {
        final CertificateCredential certificateCredential = CertificateCredential.create(
                new PrivateKey() {
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
                },
                new X509Certificate() {
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
                    public void verify(PublicKey key) throws CertificateException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, SignatureException {

                    }

                    @Override
                    public void verify(PublicKey key, String sigProvider) throws CertificateException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, SignatureException {

                    }

                    @Override
                    public String toString() {
                        return null;
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
                    public byte[] getExtensionValue(String oid) {
                        return new byte[0];
                    }
                }
        );

        Assert.assertNotNull(certificateCredential);
        Assert.assertNotNull(certificateCredential.getPrivateKey());
        Assert.assertNotNull(certificateCredential.getPublicCertificate());
    }

    @Ignore
    @Test
    public void testCanCertificateCredentialFromKeyStoreConfigurationAndCertificateMetadata() {
        final CertificateCredential certificateCredential;
        try {
            certificateCredential = CertificateCredential.create(
                    new KeyStoreConfiguration(
                            KEYSTORE_TYPE, KEYSTORE_PROVIDER, null
                    ),
                    new ClientCertificateMetadata(CERTIFICATE_ALIAS_VALID, null)
            );
        } catch (LabApiException e) {
            throw new AssertionError(e);
        }

        Assert.assertNotNull(certificateCredential);
        Assert.assertNotNull(certificateCredential.getPrivateKey());
        Assert.assertNotNull(certificateCredential.getPublicCertificate());
    }

    @Test
    public void testCannotCreateCertificateCredentialIfCertNotFoundInKeyStore() {
        try {
            CertificateCredential.create(
                    new KeyStoreConfiguration(
                            KEYSTORE_TYPE, KEYSTORE_PROVIDER, null
                    ),
                    new ClientCertificateMetadata(CERTIFICATE_ALIAS_INVALID, null)
            );
            Assert.fail("We weren't expecting to hit this line...exception should've already occurred");
        } catch (final LabApiException e) {
            Assert.assertNotNull(e);
            Assert.assertEquals(LabError.CERTIFICATE_NOT_FOUND_IN_KEY_STORE, e.getErrorCode());
        }
    }

}
