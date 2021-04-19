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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.text.TextUtils;
import android.util.Base64;

import androidx.annotation.NonNull;

import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.exception.ErrorStrings;
import com.microsoft.identity.common.internal.broker.PackageHelper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import static com.microsoft.identity.common.exception.ErrorStrings.BROKER_APP_VERIFICATION_FAILED;

/**
 * A set of utility methods for handleing and verifying android packages.  This used to be isolated
 * to only handling the broker validation, but we will want this behavior elsewhere, so all the
 * methods have been factored here.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PackageUtils {


    /**
     * Find all the signatures for packages with a given name.
     *
     * @param packageName the name of the package to find.
     * @param context     the android context to use to search.
     * @return the {@link List} of {@link X509Certificate} entries for packages with the given name;
     * @throws PackageManager.NameNotFoundException if the package was not in package manager.
     * @throws ClientException                      if the package could not be found or had a bad certificate.
     * @throws IOException                          if there was a storage read error.
     * @throws GeneralSecurityException             if there was a problem with accessing apis.
     */
    @SuppressLint("PackageManagerGetSignatures")
    @SuppressWarnings("deprecation")
    public static final List<X509Certificate> readCertDataForApp(final String packageName,
                                                                 final Context context)
            throws PackageManager.NameNotFoundException, ClientException, IOException,
            GeneralSecurityException {

        final PackageInfo packageInfo = PackageHelper.getPackageInfo(context.getPackageManager(), packageName);
        if (packageInfo == null) {
            throw new ClientException(ErrorStrings.APP_PACKAGE_NAME_NOT_FOUND,
                    "No such package existing : " + packageName);
        }

        final Signature[] signatures = PackageHelper.getSignatures(packageInfo);
        if (signatures == null || signatures.length == 0) {
            throw new ClientException(BROKER_APP_VERIFICATION_FAILED,
                    "No signature associated with the package : " + packageName);
        }

        final List<X509Certificate> certificates = new ArrayList<>(signatures.length);
        for (final Signature signature : signatures) {
            final byte[] rawCert = signature.toByteArray();
            final InputStream certStream = new ByteArrayInputStream(rawCert);

            final CertificateFactory certificateFactory;
            final X509Certificate x509Certificate;
            try {
                certificateFactory = CertificateFactory.getInstance("X509");
                x509Certificate = (X509Certificate) certificateFactory.generateCertificate(
                        certStream);
                certificates.add(x509Certificate);
            } catch (final CertificateException e) {
                throw new ClientException(BROKER_APP_VERIFICATION_FAILED);
            }
        }

        return certificates;
    }

    /**
     * Given a iterator of signature hashes, verify that one of them is present in the list
     * of certificates.
     *
     * @param certs       A {@link List} of {@link X509Certificate} objects to examine
     * @param validHashes an {@link Iterator<String>} of acceptable hashes.
     * @return a valid hash, if it is found.
     * @throws NoSuchAlgorithmException     if a certificate could not be examined.
     * @throws CertificateEncodingException if a certificate was corrupt.
     * @throws ClientException              if no valid hash was found in the list.
     */
    public static final String verifySignatureHash(final @NonNull List<X509Certificate> certs,
                                                   final @NonNull Iterator<String> validHashes)
            throws NoSuchAlgorithmException,
            CertificateEncodingException, ClientException {

        final StringBuilder hashListStringBuilder = new StringBuilder();

        for (final X509Certificate x509Certificate : certs) {
            final MessageDigest messageDigest = MessageDigest.getInstance("SHA");
            messageDigest.update(x509Certificate.getEncoded());

            // Check the hash for signer cert is the same as what we hardcoded.
            final String signatureHash = Base64.encodeToString(messageDigest.digest(), Base64.NO_WRAP);

            hashListStringBuilder.append(signatureHash);
            hashListStringBuilder.append(',');

            //The next of the iterator should be overridden to get the signatureHash.
            while (validHashes.hasNext()) {
                final String hash = validHashes.next();
                if (!TextUtils.isEmpty(hash) && hash.equals(signatureHash)) {
                    return signatureHash;
                }
            }
        }

        throw new ClientException(BROKER_APP_VERIFICATION_FAILED, "SignatureHashes: " + hashListStringBuilder.toString());
    }

    /**
     * Validate a certificate chain.  This will search for a self
     *
     * @param certificates a {@link List} of {@link X509Certificate} objects to validate.
     * @throws GeneralSecurityException if we aren't allowed to access certificates.
     * @throws ClientException          if any number other than 1 self signed certificate is found.
     */
    public static final void verifyCertificateChain(final List<X509Certificate> certificates)
            throws GeneralSecurityException, ClientException {
        // create certificate chain, find the self signed cert first and chain all the way back
        // to the signer cert. Also perform certificate signing validation when chaining them back.
        final X509Certificate issuerCert = getSelfSignedCert(certificates);
        final TrustAnchor trustAnchor = new TrustAnchor(issuerCert, null);
        final PKIXParameters pkixParameters = new PKIXParameters(Collections.singleton(trustAnchor));
        pkixParameters.setRevocationEnabled(false);
        final CertPath certPath = CertificateFactory.getInstance("X.509")
                .generateCertPath(certificates);

        final CertPathValidator certPathValidator = CertPathValidator.getInstance("PKIX");
        certPathValidator.validate(certPath, pkixParameters);
    }

    /**
     * Find a self-signed certificate in the @{link List} of {@link X509Certificate}s.  This method
     * will throw an exception if there are less or more than preciselt one self-signed certificate.
     *
     * @param certs a {link List} of certificates to examine.
     * @return the only self-signed {@link X509Certificate} in the {@link List}
     * @throws ClientException if any number other than 1 self signed certificate is found.
     */
    public static final X509Certificate getSelfSignedCert(final List<X509Certificate> certs)
            throws ClientException {
        int count = 0;
        X509Certificate selfSignedCert = null;
        for (final X509Certificate x509Certificate : certs) {
            if (x509Certificate.getSubjectDN().equals(x509Certificate.getIssuerDN())) {
                selfSignedCert = x509Certificate;
                count++;
            }
        }

        if (count > 1 || selfSignedCert == null) {
            throw new ClientException(BROKER_APP_VERIFICATION_FAILED,
                    "Multiple self signed certs found or no self signed cert existed.");
        }

        return selfSignedCert;
    }

    public static String signatureVerificationAndThrow(final String packageName, final Context context,
                                                       final Iterator<String> validPackageSignatures)
            throws ClientException {
        try {
            // Read all the certificates associated with the package name. In higher version of
            // android sdk, package manager will only returned the cert that is used to sign the
            // APK. Even a cert is claimed to be issued by another certificates, sdk will return
            // the signing cert. However, for the lower version of android, it will return all the
            // certs in the chain. We need to verify that the cert chain is correctly chained up.
            final List<X509Certificate> certs = readCertDataForApp(packageName, context);

            final String signatureHash = PackageUtils.verifySignatureHash(certs, validPackageSignatures);

            if (certs.size() > 1) {
                PackageUtils.verifyCertificateChain(certs);
            }

            return signatureHash;
        } catch (PackageManager.NameNotFoundException e) {
            throw new ClientException(ErrorStrings.APP_PACKAGE_NAME_NOT_FOUND, e.getMessage(), e);
        } catch (NoSuchAlgorithmException e) {
            throw new ClientException(ErrorStrings.NO_SUCH_ALGORITHM, e.getMessage(), e);
        } catch (final IOException | GeneralSecurityException e) {
            throw new ClientException(ErrorStrings.BROKER_VERIFICATION_FAILED, e.getMessage(), e);
        }
    }
}
