package com.microsoft.identity.common.shadows;

import static com.yubico.yubikit.core.smartcard.SW.FILE_NOT_FOUND;

import android.util.Log;

import com.yubico.yubikit.core.application.ApplicationNotAvailableException;
import com.yubico.yubikit.core.application.BadResponseException;
import com.yubico.yubikit.core.smartcard.ApduException;
import com.yubico.yubikit.core.smartcard.SmartCardConnection;
import com.yubico.yubikit.piv.PivSession;
import com.yubico.yubikit.piv.Slot;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadow.api.Shadow;

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
        ShadowUsbSmartCardConnection shadowConnection = Shadow.extract(connection);
        mAuthentication = shadowConnection.getCertificate(Slot.AUTHENTICATION);
        mSignature = shadowConnection.getCertificate(Slot.SIGNATURE);
        mKeyManagement = shadowConnection.getCertificate(Slot.KEY_MANAGEMENT);
        mCardAuth = shadowConnection.getCertificate(Slot.CARD_AUTH);
    }

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

    @Implementation
    public X509Certificate getCertificate(Slot slot) throws IOException, ApduException, BadResponseException {
        if (slot == Slot.AUTHENTICATION) {
            if (mAuthentication == null) {
                throw new ApduException(FILE_NOT_FOUND);
            }
            return mAuthentication;
        } else if (slot == Slot.SIGNATURE) {
            if (mSignature == null) {
                throw new ApduException(FILE_NOT_FOUND);
            }
            return mSignature;
        } else if (slot == Slot.KEY_MANAGEMENT) {
            if (mKeyManagement == null) {
                throw new ApduException(FILE_NOT_FOUND);
            }
            return mKeyManagement;
        } else if (slot == Slot.CARD_AUTH) {
            if (mCardAuth == null) {
                throw new ApduException(FILE_NOT_FOUND);
            }
            return mCardAuth;
        } else {
            return null;
        }
    }

    @Implementation
    public int getPinAttempts() throws IOException, ApduException {
        return 3;
    }
}
