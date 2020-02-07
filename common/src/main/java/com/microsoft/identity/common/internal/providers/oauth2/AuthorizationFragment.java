// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.common.internal.providers.oauth2;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

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
import com.microsoft.identity.common.internal.ui.webview.OnPageLoadedCallback;
import com.microsoft.identity.common.internal.ui.webview.WebViewUtil;
import com.microsoft.identity.common.internal.ui.webview.challengehandlers.IAuthorizationCompletionCallback;
import com.microsoft.identity.common.internal.util.StringUtil;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.DEVICE_REGISTRATION_REDIRECT_URI_HOSTNAME;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.INSTALL_URL_KEY;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.REDIRECT_PREFIX;
import static com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResultFactory.ERROR;
import static com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResultFactory.ERROR_DESCRIPTION;
import static com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResultFactory.ERROR_SUBCODE;

public class AuthorizationFragment extends Fragment {
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

    static final String WEB_VIEW_ZOOM_CONTROLS_ENABLED = "com.microsoft.identity.web.view.zoom.controls.enabled";

    static final String WEB_VIEW_ZOOM_ENABLED = "com.microsoft.identity.web.view.zoom.enabled";

    public static final String CANCEL_INTERACTIVE_REQUEST_ACTION = "cancel_interactive_request_action";

    private static final String TAG = AuthorizationFragment.class.getSimpleName();

    /**
     * The bundle containing values for initializing this fragment.
     */
    private Bundle mInstanceState;

    /**
     * Class of the Activity that hosts this fragment.
     * This needs to be static - see createCustomTabResponseIntent() for more details.
     */
    private static Class<?> sCallingActivityClass;

    /**
     * Response URI of the browser flow.
     * As we might not have any control over the calling Activity,
     * we can't rely on the content of the launching intent to provide us this value.
     */
    private static String sCustomTabResponseUri;

    private boolean mBrowserFlowStarted = false;

    private WebView mWebView;

    private ProgressBar mProgressBar;

    private Intent mAuthIntent;

    private boolean mPkeyAuthStatus = false; //NOPMD //TODO Will finish the implementation in Phase 1 (broker is ready).

    private String mAuthorizationRequestUrl;

    private String mRedirectUri;

    private HashMap<String, String> mRequestHeaders;

    private AuthorizationAgent mAuthorizationAgent;

    private boolean mAuthResultSent = false;

    private boolean webViewZoomControlsEnabled;

    private boolean webViewZoomEnabled;

    private BroadcastReceiver mCancelRequestReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Logger.info(TAG, "Received Authorization flow cancel request from SDK");
            Telemetry.emit(new UiEndEvent().isUserCancelled());
            sendResult(AuthenticationConstants.UIResponse.BROWSER_CODE_SDK_CANCEL, new Intent());
            finish();
        }
    };

    private Intent createResultIntent(@NonNull final String url) {
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
     * Creates an intent to handle the completion of an authorization flow with browser.
     * This restores the activity that hosts AuthorizationFragment that was created at the start of the flow.
     *
     * @param context     the package context for the app.
     * @param responseUri the response URI, which carries the parameters describing the response.
     */
    public static Intent createCustomTabResponseIntent(final Context context,
                                                       final String responseUri) {
        // We cannot pass this as part of a new intent, because we might not have any control over the calling activity.
        sCustomTabResponseUri = responseUri;

        final Intent intent = new Intent(context, sCallingActivityClass);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return intent;
    }

    public void setInstanceState(@NonNull final Bundle instanceStateBundle) {
        mInstanceState = instanceStateBundle;
    }

    private void finish(){
        final FragmentActivity activity = getActivity();
        if (activity instanceof AuthorizationActivity) {
            activity.finish();
        } else {
            // The calling activity is not owned by MSAL/Broker.
            // Just remove this fragment.
            getFragmentManager()
                    .beginTransaction()
                    .setTransitionStyle(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .remove(this)
                    .commit();
        }
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
        webViewZoomEnabled = state.getBoolean(WEB_VIEW_ZOOM_ENABLED, true);
        webViewZoomControlsEnabled = state.getBoolean(WEB_VIEW_ZOOM_CONTROLS_ENABLED, true);
    }

    /**
     * Extracts request headers from the given bundle object.
     */
    private HashMap<String, String> getRequestHeaders(final Bundle state) {
        try {
            return (HashMap<String, String>) state.getSerializable(KEY_REQUEST_HEADERS);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        final String methodName = "#onCreate";
        super.onCreate(savedInstanceState);

        sCallingActivityClass = this.getActivity().getClass();

        WebViewUtil.setDataDirectorySuffix(getActivity().getApplicationContext());

        // Register Broadcast receiver to cancel the auth request
        // if another incoming request is launched by the app
        getActivity().getApplicationContext().registerReceiver(mCancelRequestReceiver,
                new IntentFilter(CANCEL_INTERACTIVE_REQUEST_ACTION));

        if (savedInstanceState == null) {
            Logger.verbose(TAG + methodName, "Extract state from the intent bundle.");
            extractState(mInstanceState);
        } else {
            // If activity is killed by the os, savedInstance will be the saved bundle.
            Logger.verbose(TAG + methodName, "Extract state from the saved bundle.");
            extractState(savedInstanceState);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final String methodName = "#onCreateView";
        final View view = inflater.inflate(R.layout.common_activity_authentication, container, false);
        mProgressBar = view.findViewById(R.id.common_auth_webview_progressbar);

        Telemetry.emit(new UiStartEvent().putUserAgent(mAuthorizationAgent));
        if (mAuthorizationAgent == AuthorizationAgent.WEBVIEW) {
            AzureActiveDirectoryWebViewClient webViewClient = new AzureActiveDirectoryWebViewClient(
                    getActivity(),
                    new AuthorizationCompletionCallback(),
                    new OnPageLoadedCallback() {
                        @Override
                        public void onPageLoaded() {
                            mProgressBar.setVisibility(View.INVISIBLE);
                        }
                    },
                    mRedirectUri);
            setUpWebView(view, webViewClient);
        }

        if (mAuthorizationAgent == AuthorizationAgent.WEBVIEW) {
            mWebView.post(new Runnable() {
                @Override
                public void run() {
                    // load blank first to avoid error for not loading webView
                    mWebView.loadUrl("about:blank");
                    Logger.info(TAG + methodName, "Launching embedded WebView for acquiring auth code.");
                    Logger.infoPII(TAG + methodName, "The start url is " + mAuthorizationRequestUrl);
                    mWebView.loadUrl(mAuthorizationRequestUrl, mRequestHeaders);

                    // The first page load could take time, and we do not want to just show a blank page.
                    // Therefore, we'll show a spinner here, and hides it when mAuthorizationRequestUrl is successfully loaded.
                    // After that, progress bar will be displayed by MSA/AAD.
                    mProgressBar.setVisibility(View.VISIBLE);
                }
            });
        }

        return view;
    }

    /**
     * When authorization fragment is launched.  It will be launched on a new thread. (TODO: verify this)
     * Initialize based on value provided in intent.
     */
    private static String setDiagnosticContextForNewThread(String correlationId) {
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

    @Override
    public void onResume() {
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
                if (!StringUtil.isEmpty(sCustomTabResponseUri)) {
                    completeAuthorizationInBrowserFlow(sCustomTabResponseUri);
                } else {
                    cancelAuthorization();
                }
            }
        }
    }

    @Override
    public void onStop() {
        final String methodName = ":onStop";
        if(!mAuthResultSent && getActivity().isFinishing()){
            Logger.info(TAG + methodName,
                    "Hosting Activity is destroyed before Auth request is completed, sending request cancel"
            );
            Telemetry.emit(new UiEndEvent().isUserCancelled());
            sendResult(AuthenticationConstants.UIResponse.BROWSER_CODE_SDK_CANCEL, new Intent());
        }
        super.onStop();
    }

    @Override
    public void onDestroy() {
        final String methodName = "#onDestroy";
        Logger.info(TAG + methodName, "");
        if(!mAuthResultSent){
            Logger.info(TAG + methodName,
                    "Hosting Activity is destroyed before Auth request is completed, sending request cancel"
            );
            Telemetry.emit(new UiEndEvent().isUserCancelled());
            sendResult(AuthenticationConstants.UIResponse.BROWSER_CODE_SDK_CANCEL, new Intent());
        }
        getActivity().getApplicationContext().unregisterReceiver(mCancelRequestReceiver);
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

    private void completeAuthorizationInBrowserFlow(@NonNull final String customTabResponseUri) {
        Logger.info(TAG, null, "Received redirect from customTab/browser.");
        final Intent resultIntent = createResultIntent(customTabResponseUri);
        final Map<String, String> urlQueryParameters = StringExtensions.getUrlParameters(customTabResponseUri);
        final String userName = urlQueryParameters.get(AuthenticationConstants.Broker.INSTALL_UPN_KEY);

        if (isDeviceRegisterRedirect(customTabResponseUri) && !TextUtils.isEmpty(userName)) {
            Logger.info(TAG, " Device needs to be registered, sending BROWSER_CODE_DEVICE_REGISTER");
            Logger.infoPII(TAG, "Device Registration triggered for user: " + userName);
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
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_AUTH_INTENT, mAuthIntent);
        outState.putBoolean(KEY_BROWSER_FLOW_STARTED, mBrowserFlowStarted);
        outState.putBoolean(KEY_PKEYAUTH_STATUS, mPkeyAuthStatus);
        outState.putSerializable(KEY_AUTH_AUTHORIZATION_AGENT, mAuthorizationAgent);
        outState.putString(KEY_AUTH_REDIRECT_URI, mRedirectUri);
        outState.putString(KEY_AUTH_REQUEST_URL, mAuthorizationRequestUrl);
        outState.putBoolean(WEB_VIEW_ZOOM_CONTROLS_ENABLED, webViewZoomControlsEnabled);
        outState.putBoolean(WEB_VIEW_ZOOM_ENABLED, webViewZoomEnabled);
    }

    /**
     * NOTE: Fragment-only mode will not support this, as we don't own the activity.
     *       This must be invoked by AuthorizationActivity.onBackPressed().
     */
    public boolean onBackPressed() {
        Logger.info(TAG, "Back button is pressed");
        if (null != mWebView && mWebView.canGoBack()) {
            // User should be able to click back button to cancel. Counting blank page as well.
            final int BACK_PRESSED_STEPS = -2;
            if (!mWebView.canGoBackOrForward(BACK_PRESSED_STEPS)) {
                cancelAuthorization();
            } else {
                mWebView.goBack();
            }
            return true;
        }

        return false;
    }

    /**
     * Set up the web view configurations.
     *
     * @param view          View
     * @param webViewClient AzureActiveDirectoryWebViewClient
     */
    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    private void setUpWebView(@NonNull final View view,
                              @NonNull final AzureActiveDirectoryWebViewClient webViewClient) {
        // Create the Web View to show the page
        mWebView = view.findViewById(R.id.common_auth_webview);
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
        mWebView.getSettings().setBuiltInZoomControls(webViewZoomControlsEnabled);
        mWebView.getSettings().setSupportZoom(webViewZoomEnabled);
        mWebView.setVisibility(View.INVISIBLE);
        mWebView.setWebViewClient(webViewClient);
    }

    private boolean isDeviceRegisterRedirect(@NonNull final String redirectUrl) {
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
