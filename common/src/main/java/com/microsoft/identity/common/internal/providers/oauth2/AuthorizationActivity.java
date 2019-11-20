package com.microsoft.identity.common.internal.providers.oauth2;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.microsoft.identity.common.R;
import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.exception.ErrorStrings;
import com.microsoft.identity.common.internal.controllers.CommandDispatcher;
import com.microsoft.identity.common.internal.logging.DiagnosticContext;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.telemetry.Telemetry;
import com.microsoft.identity.common.internal.telemetry.events.UiEndEvent;
import com.microsoft.identity.common.internal.telemetry.events.UiStartEvent;
import com.microsoft.identity.common.internal.ui.AuthorizationAgent;
import com.microsoft.identity.common.internal.ui.webview.AzureActiveDirectoryWebViewClient;
import com.microsoft.identity.common.internal.ui.webview.WebViewUtil;
import com.microsoft.identity.common.internal.ui.webview.challengehandlers.IAuthorizationCompletionCallback;
import com.microsoft.identity.common.internal.util.StringUtil;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.DEVICE_REGISTRATION_REDIRECT_URI_HOSTNAME;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.INSTALL_URL_KEY;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.REDIRECT_PREFIX;
import static com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResultFactory.ERROR;
import static com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResultFactory.ERROR_DESCRIPTION;
import static com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResultFactory.ERROR_SUBCODE;

public final class AuthorizationActivity extends Activity {
    @VisibleForTesting
    static final String KEY_AUTH_INTENT = "authIntent";

    @VisibleForTesting
    static final String KEY_BROWSER_FLOW_STARTED = "browserFlowStarted";

    @VisibleForTesting
    static final String KEY_PKEYAUTH_STATUS = "pkeyAuthStatus";

    @VisibleForTesting
    static final String KEY_AUTH_REQUEST_URL = "authRequestUrl";

    @VisibleForTesting
    static final String KEY_AUTH_REDIRECT_URI = "authRedirectUri";

    @VisibleForTesting
    static final String KEY_AUTH_AUTHORIZATION_AGENT = "authorizationAgent";

    @VisibleForTesting
    static final String KEY_REQUEST_HEADERS = "requestHeaders";


    public static final String CANCEL_INTERACTIVE_REQUEST_ACTION = "cancel_interactive_request_action";

    private static final String TAG = AuthorizationActivity.class.getSimpleName();

    private boolean mBrowserFlowStarted = false;

    private WebView mWebView;

    private Intent mAuthIntent;

    private boolean mPkeyAuthStatus = false; //NOPMD //TODO Will finish the implementation in Phase 1 (broker is ready).

    private String mAuthorizationRequestUrl;

    private String mRedirectUri;

    private HashMap<String, String> mRequestHeaders;

    private AuthorizationAgent mAuthorizationAgent;

    private boolean mAuthResultSent = false;

    private BroadcastReceiver mCancelRequestReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Logger.info(TAG, "Received Authorization flow cancel request from SDK");
            Telemetry.emit(new UiEndEvent().isUserCancelled());
            sendResult(AuthenticationConstants.UIResponse.BROWSER_CODE_SDK_CANCEL, new Intent());
            finish();
        }
    };

    public static Intent createStartIntent(final Context context,
                                           final Intent authIntent,
                                           final String requestUrl,
                                           final String redirectUri,
                                           final HashMap<String, String> requestHeaders,
                                           final AuthorizationAgent authorizationAgent) {
        final Intent intent = new Intent(context, AuthorizationActivity.class);
        intent.putExtra(KEY_AUTH_INTENT, authIntent);
        intent.putExtra(KEY_AUTH_REQUEST_URL, requestUrl);
        intent.putExtra(KEY_AUTH_REDIRECT_URI, redirectUri);
        intent.putExtra(KEY_REQUEST_HEADERS, requestHeaders);
        intent.putExtra(KEY_AUTH_AUTHORIZATION_AGENT, authorizationAgent);
        intent.putExtra(DiagnosticContext.CORRELATION_ID, DiagnosticContext.getRequestContext().get(DiagnosticContext.CORRELATION_ID));
        return intent;
    }

    public static Intent createResultIntent(@NonNull final String url) {
        Intent resultIntent = new Intent();
        final Map<String, String> parameters = StringExtensions.getUrlParameters(url);
        if (!StringExtensions.isNullOrBlank(parameters.get(ERROR))) {
            Logger.info(TAG, "Sending intent to cancel authentication activity");

            resultIntent.putExtra(AuthenticationConstants.Browser.RESPONSE_ERROR_CODE, parameters.get(ERROR));
            resultIntent.putExtra(AuthenticationConstants.Browser.RESPONSE_ERROR_SUBCODE, parameters.get(ERROR_SUBCODE));

            // Fallback logic on error_subcode when error_description is not provided.
            // When error is "login_required", redirect url has error_description.
            // When error is  "access_denied", redirect url has  error_subcode.
            if (!StringUtil.isEmpty(parameters.get(ERROR_DESCRIPTION))) {
                resultIntent.putExtra(AuthenticationConstants.Browser.RESPONSE_ERROR_MESSAGE, parameters.get(ERROR_DESCRIPTION));
            } else {
                resultIntent.putExtra(AuthenticationConstants.Browser.RESPONSE_ERROR_MESSAGE, parameters.get(ERROR_SUBCODE));
            }
        } else {
            Logger.info(TAG, "It is pointing to redirect. Final url can be processed to get the code or error.");
            resultIntent.putExtra(AuthorizationStrategy.AUTHORIZATION_FINAL_URL, url);
        }

        return resultIntent;
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

        setDiagnosticContextForNewThread(state.getString(DiagnosticContext.CORRELATION_ID));
        mAuthIntent = state.getParcelable(KEY_AUTH_INTENT);
        mBrowserFlowStarted = state.getBoolean(KEY_BROWSER_FLOW_STARTED, false);
        mPkeyAuthStatus = state.getBoolean(KEY_PKEYAUTH_STATUS, false);
        mAuthorizationRequestUrl = state.getString(KEY_AUTH_REQUEST_URL);
        mRedirectUri = state.getString(KEY_AUTH_REDIRECT_URI);
        mRequestHeaders = getRequestHeaders(state);
        mAuthorizationAgent = (AuthorizationAgent) state.getSerializable(KEY_AUTH_AUTHORIZATION_AGENT);
    }

    /**
     * Extracts request headers from the given bundle object.
     */
    private HashMap<String, String> getRequestHeaders(final Bundle state) {
        try {
            return (HashMap<String,String>)state.getSerializable(KEY_REQUEST_HEADERS);
        }
        catch (Exception e) {
            return null;
        }
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        final String methodName = "#onCreate";
        super.onCreate(savedInstanceState);

        WebViewUtil.setDataDirectorySuffix(getApplicationContext());
        setContentView(R.layout.common_activity_authentication);

        // Register Broadcast receiver to cancel the auth request
        // if another incoming request is lauched by the app
        registerReceiver(mCancelRequestReceiver,
                new IntentFilter(CANCEL_INTERACTIVE_REQUEST_ACTION));

        if (savedInstanceState == null) {
            Logger.verbose(TAG + methodName, "Extract state from the intent bundle.");
            extractState(getIntent().getExtras());
        } else {
            // If activity is killed by the os, savedInstance will be the saved bundle.
            Logger.verbose(TAG + methodName, "Extract state from the saved bundle.");
            extractState(savedInstanceState);
        }
        Telemetry.emit(new UiStartEvent().putUserAgent(mAuthorizationAgent));

        if (mAuthorizationAgent == AuthorizationAgent.WEBVIEW) {
            AzureActiveDirectoryWebViewClient webViewClient = new AzureActiveDirectoryWebViewClient(this, new AuthorizationCompletionCallback(), mRedirectUri);
            setUpWebView(webViewClient);
            mWebView.post(new Runnable() {
                @Override
                public void run() {
                    // load blank first to avoid error for not loading webView
                    mWebView.loadUrl("about:blank");
                    Logger.info(TAG + methodName, "Launching embedded WebView for acquiring auth code.");
                    Logger.infoPII(TAG + methodName, "The start url is " + mAuthorizationRequestUrl);
                    mWebView.loadUrl(mAuthorizationRequestUrl, mRequestHeaders);
                }
            });
        }
    }

    /**
     * When authorization activity is launched.  It will be launched on a new thread.  Initialize based on value provided in intent.
     * @return
     */
    public static String setDiagnosticContextForNewThread(String correlationId) {
        final String methodName = ":setDiagnosticContextForAuthorizationActivity";
        final com.microsoft.identity.common.internal.logging.RequestContext rc =
                new com.microsoft.identity.common.internal.logging.RequestContext();
        rc.put(DiagnosticContext.CORRELATION_ID, correlationId);
        DiagnosticContext.setRequestContext(rc);
        Logger.verbose(
                TAG + methodName,
                "Initializing diagnostic context for AuthorizationActivity"
        );

        return correlationId;
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

       if (mAuthorizationAgent == AuthorizationAgent.DEFAULT
               || mAuthorizationAgent == AuthorizationAgent.BROWSER) {
            /*
             * If the Authorization Agent is set as Default or Browser,
             * and this is the first run of the activity, start the authorization intent with customTabs or browsers.
             *
             * When it returns back to this Activity from OAuth2 redirect, two scenarios would happen.
             * 1) The response uri is returned from BrowserTabActivity
             * 2) The authorization is cancelled by pressing the 'Back' button or the BrowserTabActivity is not launched.
             *
             * In the first case, generate the authorization result from the response uri.
             * In the second case, set the activity result intent with AUTH_CODE_CANCEL code.
             */
            //This check is needed when using customTabs or browser flow.
           if (!mBrowserFlowStarted) {
               mBrowserFlowStarted = true;
               if (mAuthIntent != null) {
                   // We cannot start browser activity inside OnCreate().
                   // Because the life cycle of the current activity will continue and onResume will be called before finishing the login in browser.
                   // This is by design of Android OS.
                   startActivity(mAuthIntent);
               } else {
                   final Intent resultIntent = new Intent();
                   resultIntent.putExtra(AuthenticationConstants.Browser.RESPONSE_AUTHENTICATION_EXCEPTION, new ClientException(ErrorStrings.AUTHORIZATION_INTENT_IS_NULL));
                   sendResult(AuthenticationConstants.UIResponse.BROWSER_CODE_AUTHENTICATION_EXCEPTION, resultIntent);
                   finish();
               }
           } else {
               if (!StringUtil.isEmpty(getIntent().getStringExtra(AuthorizationStrategy.CUSTOM_TAB_REDIRECT))) {
                   completeAuthorization();
               } else {
                   cancelAuthorization();
               }
           }
        }
    }

    @Override
    protected void onStop() {
        final String methodName = ":onStop";
        if(!mAuthResultSent && isFinishing()){
            Logger.info(TAG + methodName,
                    "Activity is destroyed before Auth request is completed, sending request cancel"
            );
            Telemetry.emit(new UiEndEvent().isUserCancelled());
            sendResult(AuthenticationConstants.UIResponse.BROWSER_CODE_SDK_CANCEL, new Intent());
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        final String methodName = "#onDestroy";
        Logger.info(TAG + methodName, "");
        if(!mAuthResultSent){
            Logger.info(TAG + methodName,
                    "Activity is destroyed before Auth request is completed, sending request cancel"
            );
            Telemetry.emit(new UiEndEvent().isUserCancelled());
            sendResult(AuthenticationConstants.UIResponse.BROWSER_CODE_SDK_CANCEL, new Intent());
        }
        unregisterReceiver(mCancelRequestReceiver);
        super.onDestroy();
    }

    private void sendResult(int resultCode, final Intent resultIntent) {
        Logger.info(TAG, "Sending result from Authorization Activity, resultCode: " + resultCode);
        CommandDispatcher.completeInteractive(
                AuthorizationStrategy.BROWSER_FLOW,
                resultCode,
                resultIntent
        );
        mAuthResultSent = true;
    }

    private void completeAuthorization() {
        Logger.info(TAG, null, "Received redirect from customTab/browser.");
        final String url = getIntent().getExtras().getString(AuthorizationStrategy.CUSTOM_TAB_REDIRECT);
        final Intent resultIntent = createResultIntent(url);
        final Map<String, String> urlQueryParameters = StringExtensions.getUrlParameters(url);
        final String userName = urlQueryParameters.get(AuthenticationConstants.Broker.INSTALL_UPN_KEY);

        if (isDeviceRegisterRedirect(url) && !TextUtils.isEmpty(userName)) {
            Logger.info(TAG, " Device needs to be registered, sending BROWSER_CODE_DEVICE_REGISTER");
            Logger.infoPII(TAG , "Device Registration triggered for user: " + userName);
            resultIntent.putExtra(AuthenticationConstants.Broker.INSTALL_UPN_KEY, userName);
            sendResult(
                    AuthenticationConstants.UIResponse.BROWSER_CODE_DEVICE_REGISTER,
                    resultIntent
            );
            return;
        }
        if (urlQueryParameters.containsKey(INSTALL_URL_KEY)) {
            final String appLink = urlQueryParameters.get(INSTALL_URL_KEY);
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(appLink));
            startActivity(browserIntent);
            Logger.info(TAG, "Return to caller with BROKER_REQUEST_RESUME, and waiting for result.");
            sendResult(AuthenticationConstants.UIResponse.BROKER_REQUEST_RESUME, resultIntent);
        } else if (!StringUtil.isEmpty(resultIntent.getStringExtra(AuthorizationStrategy.AUTHORIZATION_FINAL_URL))) {
            sendResult(AuthenticationConstants.UIResponse.BROWSER_CODE_COMPLETE, resultIntent);
            Telemetry.emit(new UiEndEvent().isUiComplete());
        } else if (!StringUtil.isEmpty(resultIntent.getStringExtra(AuthenticationConstants.Browser.RESPONSE_ERROR_SUBCODE))
                && resultIntent.getStringExtra(AuthenticationConstants.Browser.RESPONSE_ERROR_SUBCODE).equalsIgnoreCase("cancel")) {
            //when the user click the "cancel" button in the UI, server will send the the redirect uri with "cancel" error sub-code and redirects back to the calling app
            Telemetry.emit(new UiEndEvent().isUserCancelled());
            sendResult(AuthenticationConstants.UIResponse.BROWSER_CODE_SDK_CANCEL, resultIntent);
        } else {
            Telemetry.emit(new UiEndEvent().isUiCancelled());
            sendResult(AuthenticationConstants.UIResponse.BROWSER_CODE_ERROR, resultIntent);
        }

        finish();
    }

    private void cancelAuthorization() {
        Logger.info(TAG, "Authorization flow is canceled by user");
        final Intent resultIntent = new Intent();
        resultIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        sendResult(AuthenticationConstants.UIResponse.BROWSER_CODE_CANCEL, resultIntent);
        Telemetry.emit(new UiEndEvent().isUserCancelled());
        finish();
    }


    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_AUTH_INTENT, mAuthIntent);
        outState.putBoolean(KEY_BROWSER_FLOW_STARTED, mBrowserFlowStarted);
        outState.putBoolean(KEY_PKEYAUTH_STATUS, mPkeyAuthStatus);
        outState.putSerializable(KEY_AUTH_AUTHORIZATION_AGENT, mAuthorizationAgent);
        outState.putString(KEY_AUTH_REDIRECT_URI, mRedirectUri);
        outState.putString(KEY_AUTH_REQUEST_URL, mAuthorizationRequestUrl);
    }

    @Override
    public void onBackPressed() {
        Logger.info(TAG, "Back button is pressed");
        if ( null != mWebView && mWebView.canGoBack()) {
            // User should be able to click back button to cancel. Counting blank page as well.
            final int BACK_PRESSED_STEPS = -2;
            if (!mWebView.canGoBackOrForward(BACK_PRESSED_STEPS)) {
                cancelAuthorization();
            } else {
                mWebView.goBack();
            }
            return;
        } else {
            super.onBackPressed();
        }
    }

    /**
     * Set up the web view configurations.
     *
     * @param webViewClient AzureActiveDirectoryWebViewClient
     */
    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    private void setUpWebView(final AzureActiveDirectoryWebViewClient webViewClient) {
        // Create the Web View to show the page
        mWebView = findViewById(R.id.common_auth_webview);
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

    private boolean isDeviceRegisterRedirect(@NonNull final String redirectUrl){
        try {
            URI uri = new URI(redirectUrl);
            return uri.getScheme().equalsIgnoreCase(REDIRECT_PREFIX) &&
                    uri.getHost().equalsIgnoreCase(DEVICE_REGISTRATION_REDIRECT_URI_HOSTNAME);
        } catch (URISyntaxException e) {
            Logger.error(TAG, "Uri construction failed", e);
            return false;
        }

    }

    class AuthorizationCompletionCallback implements IAuthorizationCompletionCallback {
        @Override
        public void onChallengeResponseReceived(final int returnCode, final Intent responseIntent) {
            Logger.info(TAG, null, "onChallengeResponseReceived:" + returnCode);
            sendResult(returnCode, responseIntent);
            finish();
        }

        @Override
        public void setPKeyAuthStatus(final boolean status) {
            mPkeyAuthStatus = status;
            Logger.info(TAG, null, "setPKeyAuthStatus:" + status);
        }
    }
}