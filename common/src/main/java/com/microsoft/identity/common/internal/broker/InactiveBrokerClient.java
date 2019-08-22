package com.microsoft.identity.common.internal.broker;

import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.internal.logging.Logger;

/**
 * Use for communicating with inactive broker only.
 * This is NOT being used in ADAL. If you plan to do so, make sure it works properly.
 **/
public class InactiveBrokerClient {
    private static final String TAG = InactiveBrokerClient.class.getSimpleName();

    private static final String BROKER_ACCOUNT_SERVICE_INTENT_FILTER = "com.microsoft.workaccount.BrokerAccount";
    private static final String BROKER_ACCOUNT_SERVICE_CLASS_NAME = "com.microsoft.aad.adal.BrokerAccountService";

    private Context mContext;
    private String mInactiveBrokerPackageName;
    private BrokerAccountServiceConnection mBrokerAccountServiceConnection;
    private Boolean mBound = false;

    /**
     * Constructor for the BrokerAccountServiceClient
     *
     * @param context
     * @param inactiveBrokerData
     */
    public InactiveBrokerClient(@NonNull final Context context,
                                @NonNull final BrokerData inactiveBrokerData) {
        mContext = context;
        mInactiveBrokerPackageName = inactiveBrokerData.packageName;
    }

    /**
     * Binds to the service and returns a future that provides the proxy for the calling the BrokerAccountService
     *
     * @return BrokerAccountServiceFuture
     */
    @NonNull
    public BrokerAccountServiceFuture connect() throws ClientException {
        final BrokerAccountServiceFuture future = new BrokerAccountServiceFuture();
        mBrokerAccountServiceConnection = new BrokerAccountServiceConnection(future);

        mBound = mContext.bindService(getIntentForBrokerAccountService(), mBrokerAccountServiceConnection, Context.BIND_AUTO_CREATE);
        Logger.verbose(TAG + "connect", "The status for BrokerAccountService bindService call is: " + Boolean.valueOf(mBound));

        if (!mBound) {
            throw new ClientException("Service is unavailable or does not support binding. BrokerAccountService.");
        }

        return future;
    }

    /**
     * Disconnects (unbinds) from the bound BrokerAccountService
     */
    public void disconnect() {
        if (mBound) {
            mContext.unbindService(mBrokerAccountServiceConnection);
            mBound = false;
        }
    }

    /**
     * Gets the intent that points to the bound service on the device... if available
     * You shouldn't get this far if it's not available
     *
     * @return Intent
     */
    @Nullable
    private Intent getIntentForBrokerAccountService() {
        final Intent authServiceToBind = new Intent(BROKER_ACCOUNT_SERVICE_INTENT_FILTER);
        authServiceToBind.setPackage(mInactiveBrokerPackageName);
        authServiceToBind.setClassName(mInactiveBrokerPackageName, BROKER_ACCOUNT_SERVICE_CLASS_NAME);

        return authServiceToBind;
    }
}

