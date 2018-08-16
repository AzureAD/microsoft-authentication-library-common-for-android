package com.microsoft.identity.common.internal.providers.oauth2;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
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
    static final String KEY_AUTHORIZATION_STARTED = "authStarted";

    @VisibleForTesting
    static final String KEY_PKEYAUTH_STATUS = "pkeyAuthStatus";

    @VisibleForTesting
    static final String KEY_AUTH_REQUEST_URL = "authRequestUrl";

    @VisibleForTesting
    static final String KEY_AUTH_CONFIGURATION = "authConfiguration";

    static final String CUSTOM_TAB_REDIRECT = "com.microsoft.identity.customtab.redirect";

    private static final String TAG = AuthorizationActivity.class.getSimpleName();

    private boolean mAuthorizationStarted = false;

    private boolean mPkeyAuthStatus = false; //NOPMD //TODO Will finish the implementation in Phase 1 (broker is ready).

    private String mAuthorizationRequestUrl;
    private GenericAuthorizationStrategy mAuthorizationStrategy;
    private AuthorizationConfiguration mAuthorizationConfiguration;

    public static Intent createStartIntent(final Context context,
                                           final String requestUrl,
                                           final AuthorizationConfiguration configuration) {
        final Intent intent = new Intent(context, AuthorizationActivity.class);
        intent.putExtra(KEY_AUTH_REQUEST_URL, requestUrl);
        intent.putExtra(KEY_AUTH_CONFIGURATION, configuration);
        return intent;
    }

    private void extractState(final Bundle state) {
        if (state == null) {
            Logger.warn(TAG,"No stored state. Unable to handle response");
            finish();
            return;
        }

        mAuthorizationStarted = state.getBoolean(KEY_AUTHORIZATION_STARTED, false);
        mPkeyAuthStatus = state.getBoolean(KEY_PKEYAUTH_STATUS, false);
        mAuthorizationRequestUrl = state.getString(KEY_AUTH_REQUEST_URL);
        mAuthorizationConfiguration = (AuthorizationConfiguration)state.getSerializable(KEY_AUTH_CONFIGURATION);
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(savedInstanceState == null) {
            extractState(getIntent().getExtras());
        } else {
            extractState(savedInstanceState);
        }

        //Initialize the authorization strategy if the auth have not started
        if(!mAuthorizationStarted) {
            mAuthorizationStrategy
                    = (GenericAuthorizationStrategy) new AuthorizationStrategyFactory().getAuthorizationStrategy(
                    this,
                    mAuthorizationConfiguration,
                    new ChallengeCompletionCallback());
        }
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

        /*
         * If this is the first run of the activity, start the authorization request.
         */
        if(!mAuthorizationStarted) {
            mAuthorizationStarted = true;
            try {
                mAuthorizationStrategy.requestAuthorization(mAuthorizationRequestUrl);
            } catch (ClientException exception) {
                //TODO
            }
            return;
        }

        if(getIntent().getData()!= null) {
            /*Logger.info(TAG, null, "onNewIntent is called, received redirect from system webview.");
            final String url = intent.getStringExtra(CUSTOM_TAB_REDIRECT);

            final Intent resultIntent = new Intent();
            resultIntent.putExtra(Constants.AUTHORIZATION_FINAL_URL, url);
            returnToCaller(Constants.UIResponse.AUTH_CODE_COMPLETE,
                    resultIntent);*/
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
        outState.putBoolean(KEY_AUTHORIZATION_STARTED, mAuthorizationStarted);
        outState.putBoolean(KEY_PKEYAUTH_STATUS, mPkeyAuthStatus);
        outState.putSerializable(KEY_AUTH_CONFIGURATION, mAuthorizationConfiguration);
        outState.putString(KEY_AUTH_REQUEST_URL, mAuthorizationRequestUrl);
    }

    class ChallengeCompletionCallback implements IChallengeCompletionCallback {
        @Override
        public void onChallengeResponseReceived(final int returnCode, final Intent responseIntent) {
            Logger.verbose(TAG, null, "onChallengeResponseReceived:" + returnCode);
            setResult(returnCode, responseIntent);
            finish();
        }

        @Override
        public void setPKeyAuthStatus(final boolean status) {
            mPkeyAuthStatus = status;
            Logger.verbose(TAG, null, "setPKeyAuthStatus:" + status);
        }
    }
}