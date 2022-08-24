package com.microsoft.identity.common.shadows;

import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;

import androidx.annotation.Nullable;

import com.yubico.yubikit.android.transport.usb.connection.UsbSmartCardConnection;
import com.yubico.yubikit.core.smartcard.SmartCardConnection;
import com.yubico.yubikit.piv.Slot;

import org.robolectric.annotation.Implements;

import java.io.IOException;
import java.security.cert.X509Certificate;

import lombok.NonNull;

@Implements(UsbSmartCardConnection.class)
public class ShadowUsbSmartCardConnection {

    private X509Certificate mAuthentication;
    private X509Certificate mSignature;
    private X509Certificate mKeyManagement;
    private X509Certificate mCardAuth;

    protected void __constructor__(UsbDeviceConnection connection, UsbInterface ccidInterface, UsbEndpoint endpointIn, UsbEndpoint endpointOut) throws IOException {
        //do nothing
    }

    //Method to add certs
    public void putCertificate(@NonNull Slot slot, @Nullable X509Certificate certificate) {
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

    //Method to get certs
    public X509Certificate getCertificate(@Nullable Slot slot) {
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
