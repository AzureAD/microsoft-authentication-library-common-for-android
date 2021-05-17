package com.microsoft.identity.common.internal.platform;

import android.os.Build;

import com.microsoft.identity.common.java.platform.IDeviceMetadata;

public class AndroidDeviceMetadata implements IDeviceMetadata {
    @Override
    public String getCpu() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            //CPU_ABI has been deprecated
            return Build.CPU_ABI;
        } else {
            final String[] supportedABIs = Build.SUPPORTED_ABIS;
            if (supportedABIs != null && supportedABIs.length > 0) {
                return supportedABIs[0];
            }
        }
        return "UNKNOWN";
    }

    @Override
    public String getOs() {
        return String.valueOf(Build.VERSION.SDK_INT);
    }

    @Override
    public String getDeviceModel() {
        return Build.MODEL;
    }

    @Override
    public String getManufacturer() {
        return Build.MANUFACTURER;
    }
}