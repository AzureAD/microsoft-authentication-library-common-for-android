package com.microsoft.identity.common.internal.broker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.exception.ErrorStrings;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftAuthorizationRequest;


public class BrokerTokenProvider {

    private static final String TAG = BrokerTokenProvider.class.getSimpleName();

    /**
     * Acquire token interactively, will prompt user if possible.
     * See {@link MicrosoftAuthServiceHandler#getIntentForInteractiveRequest(final Context context)} for details.
     */
    public void acquireTokenInteractively(final Activity activity, MicrosoftAuthorizationRequest request)
            throws ClientException {
        final String methodName = ":acquireTokenInteractively";

        if (activity == null) {
            throw new ClientException(ErrorStrings.ANDROID_CONTEXT_IS_NULL);
        }

        final Bundle requestBundle = getBrokerOptions(request, activity);
        final Intent brokerIntent = MicrosoftAuthServiceHandler.getInstance().getIntentForInteractiveRequest(activity);

        //TODO the following code needs an update if "getIntentForInteractiveRequest" returns an error bundle instead of null someday.
        if (brokerIntent == null) {
            ClientException exception = new ClientException(ErrorStrings.BROKER_APP_NOT_RESPONDING, "Received null intent from broker interactive request.");
            Logger.error(TAG, "Received null intent from broker interactive request.", exception);
            throw exception;
        }

        brokerIntent.putExtras(requestBundle);
        brokerIntent.putExtra(AuthenticationConstants.Broker.BROKER_REQUEST, AuthenticationConstants.Broker.BROKER_REQUEST);

        Logger.verbose(TAG + methodName, "Calling activity. " + "Pid:" + android.os.Process.myPid()
                + " tid:" + android.os.Process.myTid() + "uid:" + android.os.Process.myUid());
        activity.startActivityForResult(brokerIntent, AuthenticationConstants.UIRequest.BROWSER_FLOW);
    }


    private Bundle getBrokerOptions(final MicrosoftAuthorizationRequest request, final Activity activity) {
        Bundle brokerOptions = new Bundle();
        // request needs to be parcelable to send across process
        brokerOptions.putString(AuthenticationConstants.Broker.ACCOUNT_AUTHORITY, request.getAuthority().toString());
        brokerOptions.putString(AuthenticationConstants.Broker.ACCOUNT_REDIRECT, request.getRedirectUri());
        brokerOptions.putString(AuthenticationConstants.Broker.ACCOUNT_CLIENTID_KEY, request.getClientId());
        brokerOptions.putString(AuthenticationConstants.Broker.ACCOUNT_USERINFO_USERID, request.getLoginHint());
        brokerOptions.putString(AuthenticationConstants.Broker.ACCOUNT_LOGIN_HINT, request.getLoginHint());
        brokerOptions.putString(AuthenticationConstants.Broker.ACCOUNT_NAME, request.getLoginHint());
        // TODO uncomment the line below when broker support is added...
        //brokerOptions.putString(AuthenticationConstants.Broker.ACCOUNT_EXTRA_QUERY_PARAM, request.getExtraQueryParam());
        brokerOptions.putString(AuthenticationConstants.Broker.AUTH_RESPONSE_TYPE, request.getResponseType());
        brokerOptions.putString(AuthenticationConstants.Broker.AUTH_STATE, request.getState());
        brokerOptions.putString(AuthenticationConstants.Broker.AUTH_SCOPE, request.getScope());
        brokerOptions.putString(AuthenticationConstants.Broker.LIB_NAME, request.getLibraryName());
        brokerOptions.putString(AuthenticationConstants.Broker.LIB_VERSION, request.getLibraryVersion());
        brokerOptions.putString(AuthenticationConstants.Broker.CLIENT_APP_PACKAGE_NAME, activity.getPackageName());

        if (request.getCorrelationId() != null) {
            brokerOptions.putString(AuthenticationConstants.Broker.ACCOUNT_CORRELATIONID,
                    request.getCorrelationId().toString());
        }

        if (request.getPkceChallenge() != null) {
            brokerOptions.putSerializable(AuthenticationConstants.Broker.PKCE_CHALLENGE, request.getPkceChallenge());
        }

        return brokerOptions;
    }
}
