package com.microsoft.identity.common.adal.internal;

import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;

/**
 * Work place join related ClientCertificateConfiguration is required to respond device challenge.
 */
public interface IDeviceCertificate {

    /**
     * Checks valid issuer for cert authorities.
     *
     * @param certAuthorities list of cert authorities
     * @return status if valid issue
     */
    boolean isValidIssuer(final List<String> certAuthorities);

    /**
     * Gets ClientCertificateConfiguration.
     *
     * @return {@link X509Certificate}
     */
    X509Certificate getCertificate();

    /**
     * Gets RSA private key.
     *
     * @return RSA private key
     */
    RSAPrivateKey getRSAPrivateKey();

    /**
     * Gets thumbPrint for ClientCertificateConfiguration.
     *
     * @return thumbPrint for ClientCertificateConfiguration.
     */
    String getThumbPrint();

    /**
     * Gets RSA public key.
     *
     * @return RSA public key.
     */
    RSAPublicKey getRSAPublicKey();
}

