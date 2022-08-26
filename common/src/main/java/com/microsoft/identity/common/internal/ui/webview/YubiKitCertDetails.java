package com.microsoft.identity.common.internal.ui.webview;

import androidx.annotation.NonNull;

import com.yubico.yubikit.piv.Slot;

import java.security.cert.X509Certificate;

import javax.annotation.Nonnull;

/**
 * Holds certificate found on YubiKey and its corresponding slot.
 */
public class YubiKitCertDetails implements ICertDetails {
    private final X509Certificate cert;
    private final Slot slot;

    /**
     * Creates new instance of YubiKitCertDetails.
     * @param cert Certificate found on YubiKey.
     * @param slot PIV slot on YubiKey where certificate is located.
     */
    public YubiKitCertDetails(@NonNull final X509Certificate cert,
                              @NonNull final Slot slot) {
        this.cert = cert;
        this.slot = slot;
    }

    /**
     * Gets certificate.
     * @return certificate.
     */
    @Override
    @NonNull
    public X509Certificate getCertificate() {
        return cert;
    }


    /**
     * Gets PIV Slot where certificate is located.
     * @return Slot where certificate is located.
     */
    @Nonnull
    public Slot getSlot() {
        return slot;
    }
}

