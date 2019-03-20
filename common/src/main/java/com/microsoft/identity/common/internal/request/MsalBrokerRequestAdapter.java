package com.microsoft.identity.common.internal.request;

import android.accounts.Account;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

public class MsalBrokerRequestAdapter implements IBrokerRequestAdapter {

    @Override
    public Bundle bundleFromAcquireTokenParameters(AcquireTokenOperationParameters parameters) {
        return null;
    }

    @Override
    public Bundle bundleFromSilentOperationParameters(AcquireTokenSilentOperationParameters parameters) {
        return null;
    }

    @Override
    public BrokerAcquireTokenOperationParameters brokerParametersFromActivity(Activity callingActivity) {
        return null;
    }

    @Override
    public BrokerAcquireTokenSilentOperationParameters brokerSilentParametersFromBundle(Bundle bundle, Context context, Account account) {
        return null;
    }
}
