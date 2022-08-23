package com.microsoft.identity.common.internal.broker.ipc;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.common.exception.BrokerCommunicationException;

public class WpjLegacyAccountAuthenticatorStrategy implements IIpcStrategy{

    private final Context mContext;

    public WpjLegacyAccountAuthenticatorStrategy(@NonNull Context context) {
        mContext = context;
    }

    @Nullable
    @Override
    public Bundle communicateToBroker(@NonNull final BrokerOperationBundle bundle) throws BrokerCommunicationException {
        return null;
    }

    @Override
    public Type getType() {
        return Type.LEGACY_ACCOUNT_AUTHENTICATOR_FOR_WPJ_API;
    }
}
