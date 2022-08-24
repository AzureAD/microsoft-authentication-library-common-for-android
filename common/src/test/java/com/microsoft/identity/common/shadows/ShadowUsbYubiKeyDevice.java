package com.microsoft.identity.common.shadows;

import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.util.Log;

import androidx.annotation.Nullable;

import com.yubico.yubikit.android.transport.usb.UsbYubiKeyDevice;
import com.yubico.yubikit.android.transport.usb.connection.UsbSmartCardConnection;
import com.yubico.yubikit.core.YubiKeyConnection;
import com.yubico.yubikit.core.util.Callback;
import com.yubico.yubikit.core.util.Result;
import com.yubico.yubikit.piv.Slot;

import org.robolectric.Shadows;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadow.api.Shadow;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.security.cert.X509Certificate;

import lombok.NonNull;

@Implements(UsbYubiKeyDevice.class)
public class ShadowUsbYubiKeyDevice {

    private X509Certificate mAuthentication;
    private X509Certificate mSignature;
    private X509Certificate mKeyManagement;
    private X509Certificate mCardAuth;

    @Implementation
    public <T extends YubiKeyConnection> void requestConnection(Class<T> connectionType, Callback<Result<T, IOException>> callback) {
        try {
            Class<?>[] parameterType = new Class[4];
            parameterType[0] = UsbDeviceConnection.class;
            parameterType[1] = UsbInterface.class;
            parameterType[2] = UsbEndpoint.class;
            parameterType[3] = UsbEndpoint.class;
            Constructor<UsbSmartCardConnection> constructor = UsbSmartCardConnection.class.getDeclaredConstructor(parameterType);
            constructor.setAccessible(true);

            UsbSmartCardConnection usbSmartCardConnection = constructor.newInstance(null, null, null, null);
            ShadowUsbSmartCardConnection shadowUsbSmartCardConnection = Shadow.extract(usbSmartCardConnection);
            loadCertsIntoConnection(shadowUsbSmartCardConnection);
            //try passing back
            @SuppressWarnings("unchecked") T connection = (T) usbSmartCardConnection; //new UsbSmartCardConnection();
            callback.invoke(Result.success(connection));
        } catch (Exception e) {
            //do nothing for now
            Log.e("tag", "Message", e);
        }
    }

    public void loadCertsIntoConnection(ShadowUsbSmartCardConnection connection) {
        connection.putCertificate(Slot.AUTHENTICATION, mAuthentication);
        connection.putCertificate(Slot.SIGNATURE, mSignature);
        connection.putCertificate(Slot.KEY_MANAGEMENT, mKeyManagement);
        connection.putCertificate(Slot.CARD_AUTH, mCardAuth);
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

    //Method to clear certs
    public void clearCerts() {
        mAuthentication = null;
        mSignature = null;
        mKeyManagement = null;
        mCardAuth = null;
    }

}
