package com.microsoft.identity.common.shadows;

import android.util.Log;

import com.yubico.yubikit.core.application.ApplicationNotAvailableException;
import com.yubico.yubikit.core.application.BadResponseException;
import com.yubico.yubikit.core.smartcard.ApduException;
import com.yubico.yubikit.core.smartcard.SmartCardConnection;
import com.yubico.yubikit.piv.PivSession;
import com.yubico.yubikit.piv.Slot;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.io.IOException;
import java.security.cert.X509Certificate;

@Implements(PivSession.class)
public class ShadowPivSession {

    private X509Certificate mAuthentication;
    private X509Certificate mSignature;
    private X509Certificate mKeyManagement;
    private X509Certificate mCardAuth;

    @Implementation
    protected void __constructor__(SmartCardConnection connection) throws IOException, ApduException, ApplicationNotAvailableException {
        mAuthentication = null;
        mSignature = null;
        mKeyManagement = null;
        mCardAuth = null;
    }

    /**
     * Writes an X.509 certificate to a slot on the YubiKey.
     *
     * @param slot        Key reference '9A', '9C', '9D', or '9E'. {@link Slot}.
     * @param certificate certificate to write
     * @throws IOException   in case of connection error
     * @throws ApduException in case of an error response from the YubiKey
     */
    @Implementation
    public void putCertificate(Slot slot, X509Certificate certificate) throws IOException, ApduException {
        if (slot == Slot.AUTHENTICATION) {
            mAuthentication = certificate;
        } else if (slot == Slot.SIGNATURE) {
            mSignature = certificate;
        } else if (slot == Slot.KEY_MANAGEMENT) {
            mKeyManagement = certificate;
        } else if (slot == Slot.CARD_AUTH) {
            mCardAuth = certificate;
        } //else it isn't one of the 4 applicable slots, so do nothing.
    }

    /**
     * Reads the X.509 certificate stored in a slot.
     *
     * @param slot Key reference '9A', '9C', '9D', or '9E'. {@link Slot}.
     * @return certificate instance
     * @throws IOException          in case of connection error
     * @throws ApduException        in case of an error response from the YubiKey
     * @throws BadResponseException in case of incorrect YubiKey response
     */
    @Implementation
    public X509Certificate getCertificate(Slot slot) throws IOException, ApduException, BadResponseException {
        if (slot == Slot.AUTHENTICATION) {
            return mAuthentication;
        } else if (slot == Slot.SIGNATURE) {
            return mSignature;
        } else if (slot == Slot.KEY_MANAGEMENT) {
            return mKeyManagement;
        } else if (slot == Slot.CARD_AUTH) {
            return mCardAuth;
        } else {
            return null;
        }
    }
}
