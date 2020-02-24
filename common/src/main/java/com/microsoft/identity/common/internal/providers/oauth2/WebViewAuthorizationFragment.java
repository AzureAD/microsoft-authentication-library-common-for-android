package com.microsoft.identity.common.internal.providers.oauth2;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
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

import com.microsoft.identity.common.R;
import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.ui.webview.AzureActiveDirectoryWebViewClient;
import com.microsoft.identity.common.internal.ui.webview.OnPageLoadedCallback;
import com.microsoft.identity.common.internal.ui.webview.WebViewUtil;
import com.microsoft.identity.common.internal.ui.webview.challengehandlers.IAuthorizationCompletionCallback;

import java.util.HashMap;

import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.AUTH_INTENT;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.POST_PAGE_LOADED_URL;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.REDIRECT_URI;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.REQUEST_URL;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.REQUEST_HEADERS;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.WEB_VIEW_ZOOM_CONTROLS_ENABLED;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.WEB_VIEW_ZOOM_ENABLED;

/**
 * Authorization fragment with embedded webview.
 * */
public class WebViewAuthorizationFragment extends AuthorizationFragment {

    private static final String TAG = WebViewAuthorizationFragment.class.getSimpleName();

    @VisibleForTesting
    private static final String PKEYAUTH_STATUS = "pkeyAuthStatus";

    private WebView mWebView;

    private ProgressBar mProgressBar;

    private Intent mAuthIntent;

    private boolean mPkeyAuthStatus = false;

    private String mAuthorizationRequestUrl;

    private String mRedirectUri;

    private HashMap<String, String> mRequestHeaders;

    // For MSAL CPP test cases only
    private String mPostPageLoadedUrl;

    private boolean webViewZoomControlsEnabled;

    private boolean webViewZoomEnabled;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WebViewUtil.setDataDirectorySuffix(getActivity().getApplicationContext());
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(AUTH_INTENT, mAuthIntent);
        outState.putBoolean(PKEYAUTH_STATUS, mPkeyAuthStatus);
        outState.putString(REDIRECT_URI, mRedirectUri);
        outState.putString(REQUEST_URL, mAuthorizationRequestUrl);
        outState.putSerializable(REQUEST_HEADERS, mRequestHeaders);
        outState.putSerializable(POST_PAGE_LOADED_URL, mPostPageLoadedUrl);
        outState.putSerializable(POST_PAGE_LOADED_URL, mPostPageLoadedUrl);
        outState.putBoolean(WEB_VIEW_ZOOM_CONTROLS_ENABLED, webViewZoomControlsEnabled);
        outState.putBoolean(WEB_VIEW_ZOOM_ENABLED, webViewZoomEnabled);
    }

    @Override
    void extractState(final Bundle state) {
        super.extractState(state);
        mAuthIntent = state.getParcelable(AUTH_INTENT);
        mPkeyAuthStatus = state.getBoolean(PKEYAUTH_STATUS, false);
        mAuthorizationRequestUrl = state.getString(REQUEST_URL);
        mRedirectUri = state.getString(REDIRECT_URI);
        mRequestHeaders = getRequestHeaders(state);
        mPostPageLoadedUrl = state.getString(POST_PAGE_LOADED_URL);
        webViewZoomEnabled = state.getBoolean(WEB_VIEW_ZOOM_ENABLED, true);
        webViewZoomControlsEnabled = state.getBoolean(WEB_VIEW_ZOOM_CONTROLS_ENABLED, true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final String methodName = "#onCreateView";
        final View view = inflater.inflate(R.layout.common_activity_authentication, container, false);
        mProgressBar = view.findViewById(R.id.common_auth_webview_progressbar);

        final AzureActiveDirectoryWebViewClient webViewClient = new AzureActiveDirectoryWebViewClient(
                getActivity(),
                new AuthorizationCompletionCallback(),
                new OnPageLoadedCallback() {
                    @Override
                    public void onPageLoaded() {
                        mProgressBar.setVisibility(View.INVISIBLE);

                        // Inject string from test suites.
                        if (!StringExtensions.isNullOrBlank(mPostPageLoadedUrl)) {
                            mWebView.loadUrl(mPostPageLoadedUrl);
                        }
                    }
                },
                mRedirectUri);
        setUpWebView(view, webViewClient);
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
        return view;
    }

    /**
     * NOTE: Fragment-only mode will not support this, as we don't own the activity.
     *       This must be invoked by AuthorizationActivity.onBackPressed().
     */
    @Override
    public boolean onBackPressed() {
        Logger.info(TAG, "Back button is pressed");
        if (null != mWebView && mWebView.canGoBack()) {
            // User should be able to click back button to cancel. Counting blank page as well.
            final int BACK_PRESSED_STEPS = -2;
            if (!mWebView.canGoBackOrForward(BACK_PRESSED_STEPS)) {
                cancelAuthorization(true);
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

    /**
     * Extracts request headers from the given bundle object.
     */
    private HashMap<String, String> getRequestHeaders(final Bundle state) {
        try {
            return (HashMap<String, String>) state.getSerializable(REQUEST_HEADERS);
        } catch (Exception e) {
            return null;
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
