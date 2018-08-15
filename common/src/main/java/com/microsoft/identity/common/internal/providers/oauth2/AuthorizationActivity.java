package com.microsoft.identity.common.internal.providers.oauth2;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.VisibleForTesting;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftAuthorizationRequest;
import com.microsoft.identity.common.internal.ui.AuthorizationStrategyFactory;
import com.microsoft.identity.common.internal.ui.webview.challengehandlers.IChallengeCompletionCallback;

import java.io.UnsupportedEncodingException;


public final class AuthorizationActivity <GenericAuthorizationStrategy extends AuthorizationStrategy,
        GenericAuthorizationRequest extends MicrosoftAuthorizationRequest>
        extends Activity {
    @VisibleForTesting
    static final String KEY_AUTH_INTENT = "authIntent";

    @VisibleForTesting
    static final String KEY_AUTH_REQUEST = "authRequest";

    @VisibleForTesting
    static final String KEY_AUTHORIZATION_STARTED = "authStarted";

    private static final String TAG = AuthorizationActivity.class.getSimpleName();

    private boolean mAuthorizationStarted = false;

    private GenericAuthorizationRequest mAuthorizationRequest;
    private GenericAuthorizationStrategy mAuthorizationStrategy;
    private AuthorizationConfiguration mAuthorizationConfiguration;
    private ChallengeCompletionCallback mChallengeCompletionCallback;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //0. Set telemetry if savedInstanceState.

        //0.5 check getIntent null
        // If activity is killed by the os, savedInstance will be the saved bundle.
        if (savedInstanceState != null) {
            Logger.verbose(TAG, null, "AuthenticationActivity is re-created after killed by the os.");
            //extractState(savedInstanceState)
            mAuthorizationStarted = true;
            return;
        }

        //1. Get the auth request from the bundle.

        //2. Launch webView
    }

    /**
     * OnNewIntent will be called before onResume.
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(!mAuthorizationStarted) {
            //request auth code by calling authorizationStrategy.requestAuthCode();
            try {
                mAuthorizationStrategy
                        = (GenericAuthorizationStrategy) new AuthorizationStrategyFactory().getAuthorizationStrategy(
                        this,
                        mAuthorizationConfiguration,
                        mChallengeCompletionCallback);
                //mAuthorizationStrategy.requestAuthorization();
            } catch (final UnsupportedEncodingException | ClientException exception) {
                //TODO
            }
            mAuthorizationStarted = true;
            return;
        }

        if(getIntent().getData()!= null) {
            //received redirect from system browser
        } else {
            //cancel request
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mAuthorizationStrategy.dispose(); //TODO to unbind the custom tabs service if needed
    }

    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        /*
        outState.putBoolean(KEY_AUTHORIZATION_STARTED, mAuthorizationStarted);
        outState.putParcelable(KEY_AUTH_INTENT, mAuthIntent);
        outState.putString(KEY_AUTH_REQUEST, mAuthRequest.jsonSerializeString());
         */
    }

    class ChallengeCompletionCallback implements IChallengeCompletionCallback {
        @Override
        public void onChallengeResponseReceived(final int returnCode, final Intent responseIntent) {
            Logger.verbose(TAG, null, "onChallengeResponseReceived:" + returnCode);

            if (mAuthorizationRequest == null) {
                Logger.warn(TAG, null, "Request object is null");
            } else {
                // set request id related to this response to send the delegateId
                //Logger.verbose(TAG, null,
                //        "Set request id related to response. "
                //                + "REQUEST_ID for caller returned to:" + mAuthorizationRequest.getCorrelationId());
                //responseIntent.putExtra(AuthenticationConstants.Browser.REQUEST_ID, mAuthorizationRequest.getCorrelationId());
                //TODO
            }

            setResult(returnCode, responseIntent);
            finish();
        }

        @Override
        public void setPKeyAuthStatus(final boolean status) {
            Logger.verbose(TAG, null, "setPKeyAuthStatus:" + status);
        }
    }
}