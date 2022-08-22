package com.microsoft.identity.common.shadows;

import android.hardware.usb.UsbDeviceConnection;

import com.yubico.yubikit.android.transport.usb.UsbYubiKeyDevice;
import com.yubico.yubikit.android.transport.usb.connection.UsbSmartCardConnection;
import com.yubico.yubikit.core.YubiKeyConnection;
import com.yubico.yubikit.core.otp.OtpConnection;
import com.yubico.yubikit.core.util.Callback;
import com.yubico.yubikit.core.util.Result;

import org.robolectric.annotation.Implements;

import java.io.IOException;

@Implements(UsbYubiKeyDevice.class)
public class ShadowUsbYubiKeyDevice {

    public <T extends YubiKeyConnection> void requestConnection(Class<T> connectionType, Callback<Result<T, IOException>> callback) {

        //Callback<Result<OtpConnection, IOException>> otpCallback = value -> callback.invoke((Result<T, IOException>) value);
        //UsbDeviceConnection usbDeviceConnection;

        //callback.invoke(Result.success(new UsbSmartCardConnection()));
    }
}
