package com.microsoft.identity.common.internal.broker;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.microsoft.aad.adal.IBrokerAccountService;
import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.exception.BaseException;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.internal.logging.Logger;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Use for communicating with inactive broker only.
 * This is NOT being used in ADAL. If you plan to do so, make sure it works properly.
 **/
public class InactiveBrokerClient {
    private static final String TAG = InactiveBrokerClient.class.getSimpleName();

    private static final String BROKER_ACCOUNT_SERVICE_INTENT_FILTER = "com.microsoft.workaccount.BrokerAccount";
    private static final String BROKER_ACCOUNT_SERVICE_CLASS_NAME = "com.microsoft.aad.adal.BrokerAccountService";

    // Time out for the encryption key retrieval bind service.
    private static final int KEY_BIND_TIMEOUT_IN_SECONDS = 5;

    private Context mContext;
    private String mActiveBrokerPackageName;
    private BrokerAccountServiceConnection mBrokerAccountServiceConnection;
    private Intent mBrokerAccountServiceIntent;
    private Boolean mBound = false;

    /**
     * IMPORTANT:
     *      1.) This operation might perform an IPC call, which requires the main thread to be free.
     *      (onServiceConnected will only be resumed on the main thread).
     *
     *      2.) The caller has to be certain that there are no other operation blocking the main thread.
     *      Otherwise, deadlock will occur.
     *
     *      2.) BrokerAccountServiceFuture.get() blocks the calling thread, waiting for the IPC connection to be established.
     *      Therefore, THIS OPERATION MUST NOT BE CALLED FROM THE MAIN THREAD.
     * */
    public static String getSerializedSymmetricKeyFromInactiveBroker(@NonNull Context context,
                                                                     @NonNull final String activeBrokerPackageName) {
        final String methodName = ":getSerializedSymmetricKeyFromInactiveBroker";

        if (Looper.myLooper() == Looper.getMainLooper()) {
            final String errorMessage =  methodName + " can't be called from main thread.";
            Logger.error(TAG + methodName, errorMessage, null);
            throw new IllegalStateException(errorMessage);
        }

        final InactiveBrokerClient client = new InactiveBrokerClient(context, activeBrokerPackageName);
        try {
            final BrokerAccountServiceFuture brokerAccountServiceFuture = client.connect();
            final IBrokerAccountService service = brokerAccountServiceFuture.get(KEY_BIND_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);

            final Bundle requestBundle = new Bundle();
            requestBundle.putString(AuthenticationConstants.Broker.CALLER_INFO_PACKAGE, context.getPackageName());

            final Bundle resultBundle = service.getInactiveBrokerKey(requestBundle);

            // This means that the inactive broker doesn't support getInactiveBrokerKey().
            if (resultBundle == null){
                Logger.verbose(TAG + methodName, "resultBundle is null");
                return null;
            }
            else if (resultBundle.getString(AuthenticationConstants.OAuth2.ERROR) != null) {
                // NOTE: BrokerAccountService's createErrorBundle is still using OAuth2.ERROR. so i'm keeping the pattern here.
                Logger.error(
                        TAG + methodName,
                        "Receiving error result from inactive broker: "
                                + resultBundle.getString(AuthenticationConstants.OAuth2.ERROR_DESCRIPTION),
                        null);
                return null;
            }

            return resultBundle.getString(AuthenticationConstants.Broker.BROKER_KEYSTORE_SYMMETRIC_KEY);

        } catch (final BaseException | InterruptedException | ExecutionException | RemoteException | TimeoutException e) {
            Logger.error(
                    TAG + methodName,
                    "Exception is thrown when trying to get key from inactive broker:"
                            + e.getMessage(),
                    e);
        } finally {
            client.disconnect();
        }

        return null;
    }

    /**
     * Constructor for the BrokerAccountServiceClient
     *
     * @param context
     * @param activeBrokerPackageName package name of the calling active broker.
     */
    private InactiveBrokerClient(@NonNull final Context context,
                                 @NonNull final String activeBrokerPackageName) {
        mContext = context;
        mActiveBrokerPackageName = activeBrokerPackageName;
        mBrokerAccountServiceIntent = getIntentForBrokerAccountService(mContext);
    }

    /**
     * Binds to the service and returns a future that provides the proxy for the calling the BrokerAccountService
     *
     * @return BrokerAccountServiceFuture
     */
    @NonNull
    private BrokerAccountServiceFuture connect() throws ClientException {

        if (mBrokerAccountServiceIntent == null) {
            throw new ClientException("mBrokerAccountServiceIntent is null. BrokerAccountService.");
        }

        final BrokerAccountServiceFuture future = new BrokerAccountServiceFuture();
        mBrokerAccountServiceConnection = new BrokerAccountServiceConnection(future);

        mBound = mContext.bindService(mBrokerAccountServiceIntent, mBrokerAccountServiceConnection, Context.BIND_AUTO_CREATE);
        Logger.verbose(TAG + "connect", "The status for BrokerAccountService bindService call is: " + Boolean.valueOf(mBound));

        if (!mBound) {
            throw new ClientException("Service is unavailable or does not support binding. BrokerAccountService.");
        }

        return future;
    }

    /**
     * Disconnects (unbinds) from the bound BrokerAccountService
     */
    private void disconnect() {
        if (mBound) {
            mContext.unbindService(mBrokerAccountServiceConnection);
            mBound = false;
        }
    }

    /**
     * Gets the intent that points to the bound service on the device... if available
     * You shouldn't get this far if it's not available
     *
     * @param context
     * @return Intent
     */
    @Nullable
    private Intent getIntentForBrokerAccountService(final Context context) {
        final String inactiveBrokerPackageName = getInactiveBrokerPackageName(context);

        if (TextUtils.isEmpty(inactiveBrokerPackageName)) {
            return null;
        }

        final Intent authServiceToBind = new Intent(BROKER_ACCOUNT_SERVICE_INTENT_FILTER);
        authServiceToBind.setPackage(inactiveBrokerPackageName);
        authServiceToBind.setClassName(inactiveBrokerPackageName, BROKER_ACCOUNT_SERVICE_CLASS_NAME);

        return authServiceToBind;
    }

    /**
     * Returns the inactive installed authenticator.
     * - If CP is the broker, this will return authenticator.
     * - If authenticator is the broker, this will return CP.
     *
     * @param context
     * @return package name of the installed inactive broker app, if there's any.
     */
    @Nullable
    private String getInactiveBrokerPackageName(@NonNull final Context context) {
        final String methodName = ":getInactiveBrokerPackageName";

        final String inactiveBrokerPackageName;
        if (AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_PACKAGE_NAME.equalsIgnoreCase(mActiveBrokerPackageName)) {
            inactiveBrokerPackageName = AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME;
        } else if (AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME.equalsIgnoreCase(mActiveBrokerPackageName)) {
            inactiveBrokerPackageName = AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_PACKAGE_NAME;
        } else {
            Logger.error(TAG + methodName, "Unknown active broker package name:" + mActiveBrokerPackageName, null);
            return null;
        }

        // Verify the signature to make sure that we're not binding to malicious apps.
        final BrokerValidator validator = new BrokerValidator(context);
        if (validator.verifySignature(inactiveBrokerPackageName)) {
            Logger.verbose(TAG + methodName, "inactive broker package found:" + inactiveBrokerPackageName);
            return inactiveBrokerPackageName;
        }

        Logger.verbose(TAG + methodName, "Inactive broker package not found.");
        return null;
    }
}
