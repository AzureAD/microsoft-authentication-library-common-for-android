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
package com.microsoft.identity.common.internal.broker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.text.TextUtils;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.adal.internal.AuthenticationSettings;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.exception.ErrorStrings;
import com.microsoft.identity.common.internal.logging.Logger;

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
import java.util.List;

public class BrokerValidator {

    private static final String TAG = "BrokerValidator";

    private final Context mContext;
    private final String mCompanyPortalSignature;

    /**
     * Constructs a new BrokerValidator.
     *
     * @param context The Context of the host application.
     */
    public BrokerValidator(final Context context) {
        mContext = context;
        mCompanyPortalSignature = AuthenticationSettings.INSTANCE.getBrokerSignature();
    }

    /**
     * Verifies that the installed broker package's signing certificate hash matches the known
     * release certificate hash.
     *
     * If signature hash verification fails, this will throw a Client exception containing the cause of error, which could contain a list of mismatch hashes.
     *
     * @param brokerPackageName The broker package to inspect.
     * @return SignatureHash of brokerPackageName, if the verification succeeds..
     */
    public String verifySignatureAndThrow(final String brokerPackageName) throws ClientException {
        try {
            // Read all the certificates associated with the package name. In higher version of
            // android sdk, package manager will only returned the cert that is used to sign the
            // APK. Even a cert is claimed to be issued by another certificates, sdk will return
            // the signing cert. However, for the lower version of android, it will return all the
            // certs in the chain. We need to verify that the cert chain is correctly chained up.
            final List<X509Certificate> certs = readCertDataForBrokerApp(brokerPackageName);

            // Verify the cert list contains the cert we trust.
            final String signatureHash = verifySignatureHash(certs);

            // Perform the certificate chain validation. If there is only one cert returned,
            // no need to perform certificate chain validation.
            if (certs.size() > 1) {
                verifyCertificateChain(certs);
            }

            return signatureHash;
        } catch (NameNotFoundException e) {
            throw new ClientException(ErrorStrings.APP_PACKAGE_NAME_NOT_FOUND, e.getMessage(), e);
        } catch (NoSuchAlgorithmException e) {
            throw new ClientException(ErrorStrings.NO_SUCH_ALGORITHM, e.getMessage(), e);
        } catch (final IOException | GeneralSecurityException e) {
            throw new ClientException(ErrorStrings.BROKER_VERIFICATION_FAILED, e.getMessage(), e);
        }
    }

    /**
     * Verifies that the installed broker package's signing certificate hash matches the known
     * release certificate hash.
     *
     * @param brokerPackageName The broker package to inspect.
     * @return True if the certificate hash is known. False otherwise.
     */
    public boolean verifySignature(final String brokerPackageName) {
        final String methodName = ":verifySignature";
        try {
            return verifySignatureAndThrow(brokerPackageName) != null;
        } catch (final ClientException e){
            Logger.error(TAG + methodName, e.getErrorCode() + ": " + e.getMessage(), e);
        }

        return false;
    }

    private String verifySignatureHash(final List<X509Certificate> certs) throws NoSuchAlgorithmException,
            CertificateEncodingException, ClientException {

        final StringBuilder hashListStringBuilder = new StringBuilder();

        for (final X509Certificate x509Certificate : certs) {
            final MessageDigest messageDigest = MessageDigest.getInstance("SHA");
            messageDigest.update(x509Certificate.getEncoded());

            // Check the hash for signer cert is the same as what we hardcoded.
            final String signatureHash = Base64.encodeToString(messageDigest.digest(), Base64.NO_WRAP);

            hashListStringBuilder.append(signatureHash);
            hashListStringBuilder.append(',');

            if (mCompanyPortalSignature.equals(signatureHash)
                    || AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_SIGNATURE.equals(signatureHash)) {
                return signatureHash;
            }
        }

        throw new ClientException(ErrorStrings.BROKER_APP_VERIFICATION_FAILED, "SignatureHashes: " + hashListStringBuilder.toString());
    }

    @SuppressLint("PackageManagerGetSignatures")
    private List<X509Certificate> readCertDataForBrokerApp(final String brokerPackageName)
            throws NameNotFoundException, ClientException, IOException,
            GeneralSecurityException {
        final PackageInfo packageInfo = mContext.getPackageManager().getPackageInfo(brokerPackageName,
                PackageManager.GET_SIGNATURES);
        if (packageInfo == null) {
            throw new ClientException(ErrorStrings.APP_PACKAGE_NAME_NOT_FOUND,
                    "No broker package existed.");
        }

        if (packageInfo.signatures == null || packageInfo.signatures.length == 0) {
            throw new ClientException(ErrorStrings.BROKER_APP_VERIFICATION_FAILED,
                    "No signature associated with the broker package.");
        }

        final List<X509Certificate> certificates = new ArrayList<>(packageInfo.signatures.length);
        for (final Signature signature : packageInfo.signatures) {
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
                throw new ClientException(ErrorStrings.BROKER_APP_VERIFICATION_FAILED);
            }
        }

        return certificates;
    }

    private void verifyCertificateChain(final List<X509Certificate> certificates)
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

    // Will throw if there is more than one self-signed cert found.
    private X509Certificate getSelfSignedCert(final List<X509Certificate> certs)
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
            throw new ClientException(ErrorStrings.BROKER_APP_VERIFICATION_FAILED,
                    "Multiple self signed certs found or no self signed cert existed.");
        }

        return selfSignedCert;
    }


    public static boolean isValidBrokerRedirect(@Nullable final String redirectUri,
                                                @NonNull final Context context,
                                                @NonNull final String packageName) {
        return !TextUtils.isEmpty(redirectUri) &&
                redirectUri.equalsIgnoreCase(getBrokerRedirectUri(context, packageName));
    }


    /**
     * Helper method to get Broker Redirect Uri
     *
     * @param context
     * @param packageName
     * @return String : Broker Redirect Uri
     */
    public static String getBrokerRedirectUri(final Context context, final String packageName) {
        final PackageHelper info = new PackageHelper(context.getPackageManager());
        final String signatureDigest = info.getCurrentSignatureForPackage(packageName);
        return PackageHelper.getBrokerRedirectUrl(packageName,
                signatureDigest);
    }
}
