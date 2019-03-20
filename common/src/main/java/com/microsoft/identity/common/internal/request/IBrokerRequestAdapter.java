package com.microsoft.identity.common.internal.request;

import android.accounts.Account;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

public interface IBrokerRequestAdapter {

    Bundle bundleFromAcquireTokenParameters(AcquireTokenOperationParameters parameters);

    Bundle bundleFromSilentOperationParameters(AcquireTokenSilentOperationParameters parameters);

    BrokerAcquireTokenOperationParameters brokerParametersFromActivity(Activity callingActivity);

    BrokerAcquireTokenSilentOperationParameters brokerSilentParametersFromBundle(Bundle bundle,
                                                                                 Context context,
                                                                                 Account account);


}
