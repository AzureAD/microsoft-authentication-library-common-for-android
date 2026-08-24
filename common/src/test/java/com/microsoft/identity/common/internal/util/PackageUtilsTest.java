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
package com.microsoft.identity.common.internal.util;

import static com.microsoft.identity.common.java.exception.ClientException.BROKER_VERIFICATION_FAILED_ERROR;
import static com.microsoft.identity.common.java.exception.ErrorStrings.APP_PACKAGE_NAME_NOT_FOUND;
import static com.microsoft.identity.common.java.exception.ErrorStrings.BROKER_APP_VERIFICATION_FAILED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.util.Base64;

import androidx.test.core.app.ApplicationProvider;

import com.microsoft.identity.common.java.exception.ClientException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.Principal;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;

@RunWith(RobolectricTestRunner.class)
public class PackageUtilsTest {
    private static final String TEST_PACKAGE_NAME = "com.contoso.test";
    private static final int TEST_UID = 12345;

    @Before
    public void setUp() {
        com.microsoft.identity.common.java.logging.Logger.setLogLevel(
                com.microsoft.identity.common.java.logging.Logger.LogLevel.NO_LOG);
    }

    @After
    public void tearDown() {
        com.microsoft.identity.common.java.logging.Logger.setLogLevel(
                com.microsoft.identity.common.java.logging.Logger.LogLevel.VERBOSE);
    }

    @Test
    public void createCertificateFromByteArray_whenBytesAreNotCertificate_throwsCertificateException() {
        assertThrows(CertificateException.class, () ->
                PackageUtils.createCertificateFromByteArray("not-a-certificate".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    public void verifySignatureHash_whenBase64HashMatches_returnsHash() throws Exception {
        final X509Certificate certificate = certificateWithEncodedBytes(new byte[]{1, 2, 3});
        final String expectedHash = sha512Base64(certificate.getEncoded());

        assertEquals(expectedHash, PackageUtils.verifySignatureHash(
                Collections.singletonList(certificate),
                Collections.singletonList(expectedHash).iterator()));
    }

    @Test
    public void verifySignatureHash_whenColonSeparatedHexHashMatches_returnsBase64Hash() throws Exception {
        final X509Certificate certificate = certificateWithEncodedBytes(new byte[]{4, 5, 6});
        final byte[] digest = MessageDigest.getInstance("SHA-512").digest(certificate.getEncoded());
        final String expectedHash = Base64.encodeToString(digest, Base64.NO_WRAP);

        assertEquals(expectedHash, PackageUtils.verifySignatureHash(
                Collections.singletonList(certificate),
                Collections.singletonList(toColonSeparatedHex(digest)).iterator()));
    }

    @Test
    public void verifySignatureHash_whenNoHashMatches_throwsClientExceptionWithObservedHash()
            throws Exception {
        final X509Certificate certificate = certificateWithEncodedBytes(new byte[]{7, 8, 9});

        final ClientException exception = assertThrows(ClientException.class, () ->
                PackageUtils.verifySignatureHash(
                        Collections.singletonList(certificate),
                        Arrays.asList("", "not-matching").iterator()));

        assertEquals(BROKER_VERIFICATION_FAILED_ERROR, exception.getErrorCode());
        assertTrue(exception.getMessage().contains(BROKER_APP_VERIFICATION_FAILED));
        assertTrue(exception.getMessage().contains(sha512Base64(certificate.getEncoded())));
    }

    @Test
    public void verifyCertificateChain_whenNoSelfSignedCertificate_throwsClientException()
            throws Exception {
        final X509Certificate certificate = certificateWithSubjectAndIssuer("subject", "issuer");

        final ClientException exception = assertThrows(ClientException.class, () ->
                PackageUtils.verifyCertificateChain(Collections.singletonList(certificate)));

        assertEquals(BROKER_APP_VERIFICATION_FAILED, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("Multiple self signed certs"));
    }

    @Test
    public void verifyCertificateChain_whenMultipleSelfSignedCertificates_throwsClientException()
            throws Exception {
        final X509Certificate first = certificateWithSubjectAndIssuer("same", "same");
        final X509Certificate second = certificateWithSubjectAndIssuer("other", "other");

        final ClientException exception = assertThrows(ClientException.class, () ->
                PackageUtils.verifyCertificateChain(Arrays.asList(first, second)));

        assertEquals(BROKER_APP_VERIFICATION_FAILED, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("Multiple self signed certs"));
    }

    @Test
    @Config(sdk = 27)
    public void readCertDataForApp_whenPackageInfoIsNull_throwsPackageNameNotFound()
            throws Exception {
        final Context context = mockContextWithPackageManager();
        when(context.getPackageManager().getPackageInfo(eq(TEST_PACKAGE_NAME), anyInt()))
                .thenReturn(null);

        final ClientException exception = assertThrows(ClientException.class, () ->
                PackageUtils.readCertDataForApp(TEST_PACKAGE_NAME, context));

        assertEquals(APP_PACKAGE_NAME_NOT_FOUND, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("No broker package existed"));
    }

    @Test
    @Config(sdk = 27)
    public void readCertDataForApp_whenPackageHasNoSignatures_throwsVerificationFailed()
            throws Exception {
        final Context context = mockContextWithPackageManager();
        final PackageInfo packageInfo = new PackageInfo();
        packageInfo.signatures = new Signature[0];
        when(context.getPackageManager().getPackageInfo(eq(TEST_PACKAGE_NAME), anyInt()))
                .thenReturn(packageInfo);

        final ClientException exception = assertThrows(ClientException.class, () ->
                PackageUtils.readCertDataForApp(TEST_PACKAGE_NAME, context));

        assertEquals(BROKER_APP_VERIFICATION_FAILED, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("No signature associated"));
    }

    @Test
    @Config(sdk = 27)
    public void readCertDataForApp_whenSignatureCannotBeParsed_throwsVerificationFailed()
            throws Exception {
        final Context context = mockContextWithPackageManager();
        final PackageInfo packageInfo = new PackageInfo();
        packageInfo.signatures = new Signature[]{
                new Signature("not-a-certificate".getBytes(StandardCharsets.UTF_8))
        };
        when(context.getPackageManager().getPackageInfo(eq(TEST_PACKAGE_NAME), anyInt()))
                .thenReturn(packageInfo);

        final ClientException exception = assertThrows(ClientException.class, () ->
                PackageUtils.readCertDataForApp(TEST_PACKAGE_NAME, context));

        assertEquals(BROKER_APP_VERIFICATION_FAILED, exception.getErrorCode());
    }

    @Test
    @Config(sdk = 27)
    public void readCertDataForApp_whenPackageManagerThrows_propagatesNameNotFound()
            throws Exception {
        final Context context = mockContextWithPackageManager();
        when(context.getPackageManager().getPackageInfo(eq(TEST_PACKAGE_NAME), anyInt()))
                .thenThrow(new PackageManager.NameNotFoundException(TEST_PACKAGE_NAME));

        assertThrows(PackageManager.NameNotFoundException.class, () ->
                PackageUtils.readCertDataForApp(TEST_PACKAGE_NAME, context));
    }

    @Test
    public void getPackageName_whenUidMapsToPackages_returnsFirstPackage() {
        final Context context = ApplicationProvider.getApplicationContext();
        Shadows.shadowOf(context.getPackageManager()).setPackagesForUid(
                TEST_UID,
                TEST_PACKAGE_NAME,
                "com.contoso.second");

        assertEquals(TEST_PACKAGE_NAME, PackageUtils.getPackageName(context, TEST_UID));
    }

    @Test
    public void getPackageName_whenUidHasNoPackages_returnsNull() {
        assertNull(PackageUtils.getPackageName(
                ApplicationProvider.getApplicationContext(),
                TEST_UID));
    }

    @Test
    public void getPackageName_whenPackageManagerThrows_returnsNull() {
        final Context context = mock(Context.class);
        final PackageManager packageManager = mock(PackageManager.class);
        when(context.getPackageManager()).thenReturn(packageManager);
        when(packageManager.getPackagesForUid(TEST_UID)).thenThrow(new RuntimeException("boom"));

        assertNull(PackageUtils.getPackageName(context, TEST_UID));
    }

    private static Context mockContextWithPackageManager() {
        final Context context = mock(Context.class);
        when(context.getPackageManager()).thenReturn(mock(PackageManager.class));
        return context;
    }

    private static X509Certificate certificateWithEncodedBytes(final byte[] encoded)
            throws Exception {
        final X509Certificate certificate = mock(X509Certificate.class);
        when(certificate.getEncoded()).thenReturn(encoded);
        return certificate;
    }

    private static X509Certificate certificateWithSubjectAndIssuer(final String subject,
                                                                   final String issuer) {
        final X509Certificate certificate = mock(X509Certificate.class);
        when(certificate.getSubjectDN()).thenReturn(principal(subject));
        when(certificate.getIssuerDN()).thenReturn(principal(issuer));
        return certificate;
    }

    private static Principal principal(final String name) {
        return new Principal() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public boolean equals(final Object object) {
                return object instanceof Principal
                        && name.equals(((Principal) object).getName());
            }

            @Override
            public int hashCode() {
                return name.hashCode();
            }
        };
    }

    private static String sha512Base64(final byte[] bytes) throws Exception {
        return Base64.encodeToString(
                MessageDigest.getInstance("SHA-512").digest(bytes),
                Base64.NO_WRAP);
    }

    private static String toColonSeparatedHex(final byte[] bytes) {
        final StringBuilder builder = new StringBuilder();
        for (final byte value : bytes) {
            if (builder.length() > 0) {
                builder.append(':');
            }
            builder.append(String.format("%02x", value & 0xff));
        }
        return builder.toString();
    }
}
