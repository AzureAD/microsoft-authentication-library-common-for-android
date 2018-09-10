package com.microsoft.identity.common.internal.broker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.adal.internal.AuthenticationSettings;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.exception.ErrorStrings;
import com.microsoft.identity.common.internal.RequestParameters;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.internal.util.StringUtil;


public class BrokerTokenProvider {

    private static final String TAG = BrokerTokenProvider.class.getSimpleName();

    /**
     * Acquire token interactively, will prompt user if possible.
     * See {@link MicrosoftAuthServiceHandler#getIntentForInteractiveRequest(final Context context)} for details.
     */
    public void acquireTokenInteractively(final Activity activity, RequestParameters requestParameters)
            throws ClientException {
        final String methodName = ":acquireTokenInteractively";

        if (activity == null) {
            throw new ClientException(ErrorStrings.EMPTY_ANDROID_CONTEXT);
        }

        final Bundle requestBundle = getBrokerOptions(requestParameters);
        final Intent brokerIntent = MicrosoftAuthServiceHandler.getInstance().getIntentForInteractiveRequest(activity);
        if (brokerIntent == null) {
            ClientException exception = new ClientException(ErrorStrings.BROKER_APP_NOT_RESPONDING, "Received null intent from broker interactive request.");
            Logger.error(TAG, "Received null intent from broker interactive request.", exception);
            throw exception;
        } else {
            brokerIntent.putExtras(requestBundle);
            brokerIntent.putExtra(AuthenticationConstants.Broker.BROKER_REQUEST,
                    AuthenticationConstants.Broker.BROKER_REQUEST);

            // Only the new broker with PRT support can read the new PromptBehavior force_prompt.
            // If talking to the old broker, and PromptBehavior is set as force_prompt, reset it as
            // Always.
            if (!isBrokerWithPRTSupport(brokerIntent) && PromptBehavior.FORCE_PROMPT == request.getPrompt()) {
                Logger.verbose(TAG + methodName, "FORCE_PROMPT is set for broker auth via old version of broker app, reset to ALWAYS.");
                brokerIntent.putExtra(AuthenticationConstants.Broker.ACCOUNT_PROMPT, PromptBehavior.Always.name());
            }
        }

        activity.startActivityForResult(brokerIntent, AuthenticationConstants.UIRequest.BROWSER_FLOW);
        Logger.verbose(TAG + methodName, "Calling activity. " + "Pid:" + android.os.Process.myPid()
                + " tid:" + android.os.Process.myTid() + "uid:" + android.os.Process.myUid());
    }


    private Bundle getBrokerOptions(final AuthorizationRequest authRequest) {
        Bundle brokerOptions = new Bundle();
        // request needs to be parcelable to send across process
        brokerOptions.putInt(AuthenticationConstants.Browser.REQUEST_ID, ((Integer)requestParameter.get(AuthenticationConstants.Browser.REQUEST_ID)).intValue());
        brokerOptions.putInt(AuthenticationConstants.Broker.EXPIRATION_BUFFER, ((Integer)requestParameter.get(AuthenticationConstants.Broker.EXPIRATION_BUFFER)).intValue());
        brokerOptions.putString(AuthenticationConstants.Broker.ACCOUNT_AUTHORITY, (String)requestParameter.get(AuthenticationConstants.Broker.ACCOUNT_AUTHORITY));
        brokerOptions.putString(AuthenticationConstants.Broker.ACCOUNT_RESOURCE, (String)requestParameter.get(AuthenticationConstants.Broker.ACCOUNT_RESOURCE));
        brokerOptions.putString(AuthenticationConstants.Broker.ACCOUNT_REDIRECT, (String)requestParameter.get(AuthenticationConstants.Broker.ACCOUNT_REDIRECT));
        brokerOptions.putString(AuthenticationConstants.Broker.ACCOUNT_CLIENTID_KEY, (String)requestParameter.get(AuthenticationConstants.Broker.ACCOUNT_CLIENTID_KEY));
        brokerOptions.putString(AuthenticationConstants.Broker.ADAL_VERSION_KEY, (String)requestParameter.get(AuthenticationConstants.Broker.ADAL_VERSION_KEY));
        brokerOptions.putString(AuthenticationConstants.Broker.ACCOUNT_USERINFO_USERID, (String)requestParameter.get(AuthenticationConstants.Broker.ACCOUNT_USERINFO_USERID));
        brokerOptions.putString(AuthenticationConstants.Broker.ACCOUNT_EXTRA_QUERY_PARAM, (String)requestParameter.get(AuthenticationConstants.Broker.ACCOUNT_EXTRA_QUERY_PARAM));

        String correlationId = (String)requestParameter.get(AuthenticationConstants.Broker.ACCOUNT_CORRELATIONID);
        if (correlationId != null) {
            brokerOptions.putString(AuthenticationConstants.Broker.ACCOUNT_CORRELATIONID, correlationId);
        }

        String username = (String)requestParameter.get(AuthenticationConstants.Broker.ACCOUNT_NAME);
        if (StringExtensions.isNullOrBlank(username)) {
            username = (String)requestParameter.get(AuthenticationConstants.Broker.ACCOUNT_LOGIN_HINT);
        }

        brokerOptions.putString(AuthenticationConstants.Broker.ACCOUNT_LOGIN_HINT, username);
        brokerOptions.putString(AuthenticationConstants.Broker.ACCOUNT_NAME, username);

        String prompt = (String)requestParameter.get(AuthenticationConstants.Broker.ACCOUNT_PROMPT);
        if (prompt != null) {
            brokerOptions.putString(AuthenticationConstants.Broker.ACCOUNT_PROMPT, prompt);
        }

        String claimsChallenge = (String)requestParameter.get(AuthenticationConstants.Broker.ACCOUNT_CLAIMS);
        if (!StringUtil.isEmpty(claimsChallenge)) {
            brokerOptions.putString(AuthenticationConstants.Broker.BROKER_SKIP_CACHE, Boolean.toString(true));
            brokerOptions.putString(AuthenticationConstants.Broker.ACCOUNT_CLAIMS, claimsChallenge);
        }

        Boolean forceRefresh = (Boolean)requestParameter.get(AuthenticationConstants.Broker.BROKER_FORCE_REFRESH);
        if (forceRefresh){
            brokerOptions.putString(AuthenticationConstants.Broker.BROKER_FORCE_REFRESH, Boolean.toString(true));
        }

        return brokerOptions;
    }

    /**
     * Check if the broker is the new one with PRT support by checking the version returned from intent.
     * Only new broker will send {@link AuthenticationConstants.Broker.BROKER_VERSION}, and the version number
     * will be v2.
     */
    private boolean isBrokerWithPRTSupport(final Intent intent) {
        if (intent == null) {
            throw new IllegalArgumentException("intent");
        }

        // Only new broker with PRT support will send down the value and the version will be v2
        final String brokerVersion = intent.getStringExtra(AuthenticationConstants.Broker.BROKER_VERSION);
        return AuthenticationConstants.Broker.BROKER_PROTOCOL_VERSION.equalsIgnoreCase(brokerVersion);
    }


}
