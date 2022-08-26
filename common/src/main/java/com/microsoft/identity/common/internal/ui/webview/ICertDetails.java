package com.microsoft.identity.common.internal.ui.webview;

import androidx.annotation.NonNull;

import com.yubico.yubikit.piv.Slot;

import java.security.cert.X509Certificate;

import javax.annotation.Nonnull;

public interface ICertDetails {

    /**
     * Gets certificate.
     * @return certificate.
     */
    @NonNull
    public X509Certificate getCertificate();

}
