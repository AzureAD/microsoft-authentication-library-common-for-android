package com.microsoft.identity.common.shadows;

import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;

import com.yubico.yubikit.android.transport.usb.connection.UsbSmartCardConnection;
import com.yubico.yubikit.core.smartcard.SmartCardConnection;

import org.robolectric.annotation.Implements;

import java.io.IOException;

@Implements(UsbSmartCardConnection.class)
public class ShadowUsbSmartCardConnection {

    protected void __constructor__(UsbDeviceConnection connection, UsbInterface ccidInterface, UsbEndpoint endpointIn, UsbEndpoint endpointOut) throws IOException {
        //do nothing
    }
}
