package com.microsoft.identity.common;

import android.content.Context;

import androidx.annotation.Nullable;

import com.microsoft.identity.common.crypto.AndroidBrokerStorageEncryptionManager;
import com.microsoft.identity.common.crypto.AndroidSdkStorageEncryptionManager;
import com.microsoft.identity.common.internal.net.cache.HttpCache;
import com.microsoft.identity.common.internal.util.ProcessUtil;
import com.microsoft.identity.common.java.crypto.IStorageEncryptionManager;
import com.microsoft.identity.common.java.interfaces.ICommonComponents;
import com.microsoft.identity.common.java.telemetry.ITelemetryCallback;

import lombok.NonNull;

public class AndroidCommonComponents implements ICommonComponents {

    protected final Context mContext;

    public AndroidCommonComponents(@NonNull final Context context){
        mContext = context;
    }

    @Override
    public void flushHttpCache() {
        HttpCache.flush();
    }

    // TODO: The caller of this base 'common' class is unclear whether it's in Broker or ADAL/MSAL.
    //       Once we wired this e2e, we should be able to supply the right object,
    //       and shouldn't need process to decide which one to return.
    @Override
    public IStorageEncryptionManager getStorageEncryptionManager(@Nullable final ITelemetryCallback telemetryCallback) {
        if (ProcessUtil.isBrokerProcess(mContext)) {
            return new AndroidBrokerStorageEncryptionManager(mContext, telemetryCallback);
        }

        return new AndroidSdkStorageEncryptionManager(mContext, telemetryCallback);
    }
}
