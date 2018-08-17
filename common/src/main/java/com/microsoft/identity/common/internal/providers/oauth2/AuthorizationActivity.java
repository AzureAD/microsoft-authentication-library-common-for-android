package com.microsoft.identity.common.internal.providers.oauth2;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.VisibleForTesting;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.microsoft.identity.common.R;
import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.ui.webview.OAuth2WebViewClient;
import com.microsoft.identity.common.internal.ui.webview.WebViewClientFactory;
import com.microsoft.identity.common.internal.ui.webview.challengehandlers.IChallengeCompletionCallback;

public final class AuthorizationActivity <GenericOAuth2WebViewClient extends OAuth2WebViewClient> extends Activity {
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

    private WebView mWebView;

    private Intent mAuthIntent;

    private boolean mPkeyAuthStatus = false; //NOPMD //TODO Will finish the implementation in Phase 1 (broker is ready).

    private String mAuthorizationRequestUrl;
    private AuthorizationConfiguration mAuthorizationConfiguration;

    public static Intent createStartIntent(final Intent authIntent,
                                           final String requestUrl,
                                           final AuthorizationConfiguration configuration) {
        final Intent intent = new Intent(configuration.getContext(), AuthorizationActivity.class);
        intent.putExtra(KEY_AUTH_INTENT, authIntent);
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

        mAuthIntent = state.getParcelable(KEY_AUTH_INTENT);
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

        if(mAuthorizationConfiguration.getIdpType().equals("Microsoft") && !mAuthorizationStarted) {
            GenericOAuth2WebViewClient webViewClient
                    = (GenericOAuth2WebViewClient)WebViewClientFactory.getInstance().getWebViewClient(
                            mAuthorizationConfiguration,
                    this,
                            new ChallengeCompletionCallback());
            setUpWebView(webViewClient);
            mAuthorizationStarted = true;
            mWebView.post(new Runnable() {
                @Override
                public void run() {
                    // load blank first to avoid error for not loading webview
                    mWebView.loadUrl("about:blank");
                    Logger.verbose(TAG, "Launching embedded WebView for acquiring auth code.");
                    Logger.verbosePII(TAG, "The start url is" + mAuthorizationRequestUrl);
                    mWebView.loadUrl(mAuthorizationRequestUrl);
                }
            });
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
            startActivity(mAuthIntent);
            return;
        }

        if(getIntent().getData()!= null) {
            Logger.info(TAG, null, "onNewIntent is called, received redirect from system webview.");
            final String url = getIntent().getStringExtra(AuthorizationStrategy.CUSTOM_TAB_REDIRECT);

            final Intent resultIntent = new Intent();
            resultIntent.putExtra(AuthorizationStrategy.AUTHORIZATION_FINAL_URL, url);
            setResult(AuthorizationStrategy.UIResponse.AUTH_CODE_COMPLETE,
                    resultIntent);
            finish();
        } else {
            setResult(AuthorizationStrategy.UIResponse.CANCEL, new Intent());
            finish();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        //mAuthorizationStrategy.dispose(); //TODO to unbind the custom tabs service if needed
    }

    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_AUTHORIZATION_STARTED, mAuthorizationStarted);
        outState.putBoolean(KEY_PKEYAUTH_STATUS, mPkeyAuthStatus);
        outState.putSerializable(KEY_AUTH_CONFIGURATION, mAuthorizationConfiguration);
        outState.putString(KEY_AUTH_REQUEST_URL, mAuthorizationRequestUrl);
    }

    /**
     * Set up the web view configurations.
     * @param webViewClient AzureActiveDirectoryWebViewClient
     */
    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    private void setUpWebView(final GenericOAuth2WebViewClient webViewClient) {
        // Create the Web View to show the page
        mWebView =  this.findViewById(R.id.webview);
        WebSettings userAgentSetting = mWebView.getSettings();
        final String userAgent = userAgentSetting.getUserAgentString();
        mWebView.getSettings().setUserAgentString(
                userAgent + AuthenticationConstants.Broker.CLIENT_TLS_NOT_SUPPORTED);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.requestFocus(View.FOCUS_DOWN);

        // Set focus to the view for touch event
        mWebView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(final View view, final MotionEvent event) {
                int action = event.getAction();
                if ((action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_UP) && !view.hasFocus()) {
                    view.requestFocus();
                }
                return false;
            }
        });

        mWebView.getSettings().setLoadWithOverviewMode(true);
        mWebView.getSettings().setDomStorageEnabled(true);
        mWebView.getSettings().setUseWideViewPort(true);
        mWebView.getSettings().setBuiltInZoomControls(true);
        mWebView.setVisibility(View.INVISIBLE);
        mWebView.setWebViewClient(webViewClient);
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