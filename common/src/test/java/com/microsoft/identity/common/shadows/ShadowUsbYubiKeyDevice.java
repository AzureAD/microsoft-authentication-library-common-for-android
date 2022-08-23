package com.microsoft.identity.common.shadows;

import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;

import com.yubico.yubikit.android.transport.usb.UsbYubiKeyDevice;
import com.yubico.yubikit.android.transport.usb.connection.UsbSmartCardConnection;
import com.yubico.yubikit.core.YubiKeyConnection;
import com.yubico.yubikit.core.otp.OtpConnection;
import com.yubico.yubikit.core.smartcard.SmartCardConnection;
import com.yubico.yubikit.core.util.Callback;
import com.yubico.yubikit.core.util.Result;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.io.IOException;
import java.lang.reflect.Constructor;

@Implements(UsbYubiKeyDevice.class)
public class ShadowUsbYubiKeyDevice {

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
            @SuppressWarnings("unchecked") T usbSmartCardConnection = (T) constructor.newInstance(null, null, null, null); //new UsbSmartCardConnection();

            callback.invoke(Result.success(usbSmartCardConnection));
        } catch (Exception e) {
            //do nothing for now
        }
    }
}
