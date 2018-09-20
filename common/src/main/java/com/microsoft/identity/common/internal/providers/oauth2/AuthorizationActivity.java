package com.microsoft.identity.common.internal.providers.oauth2;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
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
import com.microsoft.identity.common.internal.ui.AuthorizationAgent;
import com.microsoft.identity.common.internal.ui.webview.AzureActiveDirectoryWebViewClient;
import com.microsoft.identity.common.internal.ui.webview.challengehandlers.IChallengeCompletionCallback;
import com.microsoft.identity.common.internal.util.StringUtil;

public final class AuthorizationActivity extends Activity {
    @VisibleForTesting
    static final String KEY_AUTH_INTENT = "authIntent";

    @VisibleForTesting
    static final String KEY_AUTHORIZATION_STARTED = "authStarted";

    @VisibleForTesting
    static final String KEY_PKEYAUTH_STATUS = "pkeyAuthStatus";

    @VisibleForTesting
    static final String KEY_AUTH_REQUEST_URL = "authRequestUrl";

    @VisibleForTesting
    static final String KEY_AUTH_REDIRECT_URI = "authRedirectUri";

    @VisibleForTesting
    static final String KEY_AUTH_AUTHORIZATION_AGENT = "authorizationAgent";

    private static final String TAG = AuthorizationActivity.class.getSimpleName();

    private boolean mAuthorizationStarted = false;

    private WebView mWebView;

    private Intent mAuthIntent;

    private boolean mPkeyAuthStatus = false; //NOPMD //TODO Will finish the implementation in Phase 1 (broker is ready).

    private String mAuthorizationRequestUrl;

    private String mRedirectUri;

    private AuthorizationAgent mAuthorizationAgent;

    public static Intent createStartIntent(final Context context,
                                           final Intent authIntent,
                                           final String requestUrl,
                                           final String redirectUri,
                                           final AuthorizationAgent authorizationAgent) {
        final Intent intent = new Intent(context, AuthorizationActivity.class);
        intent.putExtra(KEY_AUTH_INTENT, authIntent);
        intent.putExtra(KEY_AUTH_REQUEST_URL, requestUrl);
        intent.putExtra(KEY_AUTH_REDIRECT_URI, redirectUri);
        intent.putExtra(KEY_AUTH_AUTHORIZATION_AGENT, authorizationAgent);
        return intent;
    }

    /**
     * Creates an intent to handle the completion of an authorization flow with browser. This restores
     * the original AuthorizationActivity that was created at the start of the flow.
     *
     * @param context     the package context for the app.
     * @param responseUri the response URI, which carries the parameters describing the response.
     */
    public static Intent createCustomTabResponseIntent(final Context context,
                                                       final String responseUri) {
        final Intent intent = new Intent(context, AuthorizationActivity.class);
        intent.putExtra(AuthorizationStrategy.CUSTOM_TAB_REDIRECT, responseUri);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return intent;
    }

    private void extractState(final Bundle state) {
        if (state == null) {
            Logger.warn(TAG, "No stored state. Unable to handle response");
            finish();
            return;
        }

        mAuthIntent = state.getParcelable(KEY_AUTH_INTENT);
        mAuthorizationStarted = state.getBoolean(KEY_AUTHORIZATION_STARTED, false);
        mPkeyAuthStatus = state.getBoolean(KEY_PKEYAUTH_STATUS, false);
        mAuthorizationRequestUrl = state.getString(KEY_AUTH_REQUEST_URL);
        mRedirectUri = state.getString(KEY_AUTH_REDIRECT_URI);
        mAuthorizationAgent = (AuthorizationAgent)state.getSerializable(KEY_AUTH_AUTHORIZATION_AGENT);
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            extractState(getIntent().getExtras());
        } else {
            extractState(savedInstanceState);
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
        if (!mAuthorizationStarted) {
            mAuthorizationStarted = true;
            if (mAuthorizationAgent == AuthorizationAgent.WEBVIEW) {
                //TODO Replace AzureActiveDirectoryWebViewClient with GenericOAuth2WebViewClient once OAuth2Strategy get integrated.
                AzureActiveDirectoryWebViewClient webViewClient = new AzureActiveDirectoryWebViewClient(this, new ChallengeCompletionCallback(), mRedirectUri);
                setUpWebView(webViewClient);
                mWebView.post(new Runnable() {
                    @Override
                    public void run() {
                        // load blank first to avoid error for not loading webview
                        mWebView.loadUrl("about:blank");
                        Logger.verbose(TAG, "Launching embedded WebView for acquiring auth code.");
                        Logger.verbosePII(TAG, "The start url is " + mAuthorizationRequestUrl);
                        mWebView.loadUrl(mAuthorizationRequestUrl);
                    }
                });
            } else {
                startActivity(mAuthIntent);
            }
            return;
        }

        if (!StringUtil.isEmpty(getIntent().getStringExtra(AuthorizationStrategy.CUSTOM_TAB_REDIRECT))) {
            Logger.info(TAG, null, "Received redirect from system webview.");
            final String url = getIntent().getExtras().getString(AuthorizationStrategy.CUSTOM_TAB_REDIRECT);
            final Intent resultIntent = new Intent();
            resultIntent.putExtra(AuthorizationStrategy.AUTHORIZATION_FINAL_URL, url);
            setResult(AuthorizationStrategy.UIResponse.AUTH_CODE_COMPLETE,
                    resultIntent);
            finish();
        } else {
            setResult(AuthorizationStrategy.UIResponse.AUTH_CODE_CANCEL, new Intent());
            finish();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_AUTHORIZATION_STARTED, mAuthorizationStarted);
        outState.putBoolean(KEY_PKEYAUTH_STATUS, mPkeyAuthStatus);
        outState.putSerializable(KEY_AUTH_AUTHORIZATION_AGENT, mAuthorizationAgent);
        outState.putString(KEY_AUTH_REDIRECT_URI, mRedirectUri);
        outState.putString(KEY_AUTH_REQUEST_URL, mAuthorizationRequestUrl);
    }

    /**
     * Set up the web view configurations.
     *
     * @param webViewClient AzureActiveDirectoryWebViewClient
     */
    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    private void setUpWebView(final AzureActiveDirectoryWebViewClient webViewClient) {
        // Create the Web View to show the page
        mWebView = this.findViewById(R.id.webview);
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