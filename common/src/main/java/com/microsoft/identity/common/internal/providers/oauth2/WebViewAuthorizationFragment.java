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

import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.AUTH_INTENT;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.POST_PAGE_LOADED_URL;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.REDIRECT_URI;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.REQUEST_HEADERS;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.REQUEST_URL;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.WEB_VIEW_ZOOM_CONTROLS_ENABLED;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.WEB_VIEW_ZOOM_ENABLED;
import static com.microsoft.identity.common.java.AuthenticationConstants.SdkPlatformFields.PRODUCT;
import static com.microsoft.identity.common.java.AuthenticationConstants.SdkPlatformFields.VERSION;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ProgressBar;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.FragmentActivity;

import com.microsoft.identity.common.R;
import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.internal.fido.LegacyFido2ApiObject;
import com.microsoft.identity.common.internal.fido.LegacyFidoActivityResultContract;
import com.microsoft.identity.common.internal.ui.webview.AzureActiveDirectoryWebViewClient;
import com.microsoft.identity.common.internal.ui.webview.ISendResultCallback;
import com.microsoft.identity.common.internal.ui.webview.OnPageLoadedCallback;
import com.microsoft.identity.common.internal.ui.webview.ProcessUtil;
import com.microsoft.identity.common.internal.ui.webview.WebViewUtil;
import com.microsoft.identity.common.internal.ui.webview.switchbrowser.SwitchBrowserProtocolCoordinator;
import com.microsoft.identity.common.java.WarningType;
import com.microsoft.identity.common.java.constants.FidoConstants;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.flighting.CommonFlight;
import com.microsoft.identity.common.java.flighting.CommonFlightsManager;
import com.microsoft.identity.common.java.providers.RawAuthorizationResult;
import com.microsoft.identity.common.java.ui.webview.authorization.IAuthorizationCompletionCallback;
import com.microsoft.identity.common.java.util.ClientExtraSku;
import com.microsoft.identity.common.java.util.StringUtil;
import com.microsoft.identity.common.logging.Logger;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;

import static com.microsoft.identity.common.java.AuthenticationConstants.OAuth2.UTID;


import io.opentelemetry.api.trace.SpanContext;

/**
 * Authorization fragment with embedded webview.
 */
public class WebViewAuthorizationFragment extends AuthorizationFragment {

    private static final String TAG = WebViewAuthorizationFragment.class.getSimpleName();

    @VisibleForTesting
    private static final String PKEYAUTH_STATUS = "pkeyAuthStatus";

    private WebView mWebView;

    private AzureActiveDirectoryWebViewClient mAADWebViewClient;

    private ProgressBar mProgressBar;

    private Intent mAuthIntent;

    private boolean mPkeyAuthStatus = false;

    private String mAuthorizationRequestUrl;

    private String mRedirectUri;

    private HashMap<String, String> mRequestHeaders;

    // For MSAL CPP test cases only
    private String mPostPageLoadedJavascript;

    private boolean webViewZoomControlsEnabled;

    private boolean webViewZoomEnabled;

    private String mUtid;

    private final CameraPermissionRequestHandler mCameraPermissionRequestHandler = new CameraPermissionRequestHandler(this);

    // This is used by LegacyFido2ApiManager to launch a PendingIntent received by the legacy API.
    private ActivityResultLauncher<LegacyFido2ApiObject> mFidoLauncher;
    // This is used by the switch browser protocol to handle the resume of the flow.
    private SwitchBrowserProtocolCoordinator mSwitchBrowserProtocolCoordinator = null;

    private boolean isBrokerRequest = false;

    private static Bundle switchBrowserBundle;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final String methodTag = TAG + ":onCreate";
        Logger.verbose(methodTag, "WebViewAuthorizationFragment onCreate");
        final FragmentActivity activity = getActivity();
        if (activity != null) {
            WebViewUtil.setDataDirectorySuffix(activity.getApplicationContext());
        }
        if (CommonFlightsManager.INSTANCE.getFlightsProvider().isFlightEnabled(CommonFlight.ENABLE_LEGACY_FIDO_SECURITY_KEY_LOGIC)
                && Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            mFidoLauncher = registerForActivityResult(
                    new LegacyFidoActivityResultContract(),
                    result -> {
                        Logger.info(methodTag, "Legacy FIDO2 API result received.");
                    }
            );
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Logger.verbose(TAG + ":onResume", "WebViewAuthorizationFragment onResume");
        if (getSwitchBrowserCoordinator().isExpectingSwitchBrowserResume()) {
            resumeSwitchBrowser();
        } else {
            setSwitchBrowserBundle(null);
        }
    }

    /**
     * Resume the switch browser protocol flow.
     */
    private void resumeSwitchBrowser() {
        final String methodTag = TAG + ":resumeSwitchBrowser";
        try {
            if (switchBrowserBundle == null) {
                throw new ClientException(
                        ClientException.NULL_OBJECT,
                        "No switch browser bundle found to resume the flow."
                );
            }
            Logger.info(methodTag, "Resuming switch browser flow");
            getSwitchBrowserCoordinator().processSwitchBrowserResume(
                    mAuthorizationRequestUrl,
                    switchBrowserBundle,
                    (switchBrowserResumeUri, switchBrowserResumeHeaders) -> {
                        launchWebView(switchBrowserResumeUri.toString(), switchBrowserResumeHeaders);
                        return null;
                    }
            );
            setSwitchBrowserBundle(null);
        } catch (final ClientException e) {
            Logger.error(methodTag, "Error processing switch browser resume", e);
            sendResult(RawAuthorizationResult.fromException(e));
            finish();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(AUTH_INTENT, mAuthIntent);
        outState.putBoolean(PKEYAUTH_STATUS, mPkeyAuthStatus);
        outState.putString(REDIRECT_URI, mRedirectUri);
        outState.putString(REQUEST_URL, mAuthorizationRequestUrl);
        outState.putSerializable(REQUEST_HEADERS, mRequestHeaders);
        outState.putSerializable(POST_PAGE_LOADED_URL, mPostPageLoadedJavascript);
        outState.putBoolean(WEB_VIEW_ZOOM_CONTROLS_ENABLED, webViewZoomControlsEnabled);
        outState.putBoolean(WEB_VIEW_ZOOM_ENABLED, webViewZoomEnabled);
        outState.putString(UTID, mUtid);
    }

    @Override
    void extractState(@NonNull final Bundle state) {
        super.extractState(state);
        mAuthIntent = state.getParcelable(AUTH_INTENT);
        mPkeyAuthStatus = state.getBoolean(PKEYAUTH_STATUS, false);
        mAuthorizationRequestUrl = state.getString(REQUEST_URL);
        final Context context = getContext();
        if (context != null) {
            isBrokerRequest = ProcessUtil.isRunningOnAuthService(context);
        }
        mRedirectUri = state.getString(REDIRECT_URI);
        mRequestHeaders = getRequestHeaders(state);
        mPostPageLoadedJavascript = state.getString(POST_PAGE_LOADED_URL);
        webViewZoomEnabled = state.getBoolean(WEB_VIEW_ZOOM_ENABLED, true);
        webViewZoomControlsEnabled = state.getBoolean(WEB_VIEW_ZOOM_CONTROLS_ENABLED, true);
        mUtid = state.getString(UTID);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final String methodTag = TAG + ":onCreateView";
        final View view = inflater.inflate(R.layout.common_activity_authentication, container, false);
        mProgressBar = view.findViewById(R.id.common_auth_webview_progressbar);

        final FragmentActivity activity = getActivity();
        if (activity == null) {
            return null;
        }
        mAADWebViewClient = new AzureActiveDirectoryWebViewClient(
                activity,
                new AuthorizationCompletionCallback(),
                new OnPageLoadedCallback() {
                    @Override
                    public void onPageLoaded(final String url) {
                        final String[] javascriptToExecute = new String[1];
                        mProgressBar.setVisibility(View.INVISIBLE);
                        try {
                            javascriptToExecute[0] = String.format("window.expectedUrl = '%s';%n%s",
                                    URLEncoder.encode(url, "UTF-8"),
                                    mPostPageLoadedJavascript);
                        } catch (final UnsupportedEncodingException e) {
                            // Encode url component failed, fallback.
                            Logger.warn(methodTag, "Inject expectedUrl failed.");
                        }
                        // Inject the javascript string from testing. This should only be evaluated if we haven't sent
                        // an auth result already.
                        if (!mAuthResultSent && !StringExtensions.isNullOrBlank(javascriptToExecute[0])) {
                            mWebView.evaluateJavascript(javascriptToExecute[0], null);
                        }
                    }
                },
                mRedirectUri,
                getSwitchBrowserCoordinator().getSwitchBrowserRequestHandler(),
                mUtid
        );
        setUpWebView(view, mAADWebViewClient);
        mAADWebViewClient.initializeAuthUxJavaScriptApi(mWebView, mAuthorizationRequestUrl);
        launchWebView(mAuthorizationRequestUrl, mRequestHeaders);
        return view;
    }

    @Override
    public void handleBackButtonPressed() {
        final String methodTag = TAG + ":handleBackButtonPressed";
        Logger.info(methodTag, "Back button is pressed");

        if (mWebView.canGoBack()) {
            mWebView.goBack();
        } else {
            cancelAuthorization(true);
        }
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
        final String methodTag = TAG + ":setUpWebView";

        // Create the Web View to show the page
        mWebView = view.findViewById(R.id.common_auth_webview);
        final WebSettings webSettings = mWebView.getSettings();
        final String userAgent = webSettings.getUserAgentString();
        webSettings.setUserAgentString(
                userAgent + AuthenticationConstants.Broker.CLIENT_TLS_NOT_SUPPORTED);
        webSettings.setJavaScriptEnabled(true);

        // Security settings to prevent unauthorized access - controlled by flight
        if (CommonFlightsManager.INSTANCE.getFlightsProvider().isFlightEnabled(CommonFlight.ENABLE_WEBVIEW_SECURITY_SETTINGS)) {
            webSettings.setAllowFileAccess(false);
            webSettings.setAllowContentAccess(false);
            webSettings.setAllowFileAccessFromFileURLs(false);
            webSettings.setAllowUniversalAccessFromFileURLs(false);
            webSettings.setGeolocationEnabled(false);
        }

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
        mWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                requireActivity().runOnUiThread(() -> {
                    // Log the permission request
                    Logger.info(methodTag,
                            "Permission requested from:" + request.getOrigin() +
                                    " for resources:" + Arrays.toString(request.getResources())
                    );
                    mCameraPermissionRequestHandler.handle(request, requireContext());
                });
            }

            @Override
            public Bitmap getDefaultVideoPoster() {
                // When not playing, video elements are represented by a 'poster' image.
                // The image to use can be specified by the poster attribute of the video tag in HTML.
                // If the attribute is absent, then a default poster will be used.
                // This method allows the ChromeClient to provide that default image.
                // We will return a 10x10 empty image, instead of the default grey playback image. #2424
                return Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888);
            }

            /**
             * Capture console messages and log them using our logger.
             *
             * @param consoleMessage The console message from the WebView
             * @return returns true if we handled the message. False will let the default handler handle it.
             */
            @Override
            public boolean onConsoleMessage(@NonNull final ConsoleMessage consoleMessage) {
                // Note: Decide what we are interested in logging and what level.
                super.onConsoleMessage(consoleMessage);
                final String methodTag = TAG + ":onConsoleMessage";
                final String errorMessage = consoleMessage.message() + " -- From line " +
                        consoleMessage.lineNumber() + " of " + consoleMessage.sourceId();
                switch (consoleMessage.messageLevel()) {
                    case ERROR:
                        Logger.error(methodTag, errorMessage, null);
                        break;
                    case WARNING:
                        Logger.warn(methodTag, errorMessage);
                        break;
                    default:
                        Logger.info(methodTag, errorMessage);
                }
                return true;
            }

        });
        setupPasskeyWebListener(mWebView, webViewClient);
    }

    /**
     * Loads starting authorization request url into WebView.
     */
    private void launchWebView(@NonNull final String authorizationRequestUrl,
                               @NonNull final HashMap<String, String> requestHeaders) {
        final String methodTag = TAG + ":launchWebView";
        mWebView.post(new Runnable() {
            @Override
            public void run() {
                Logger.info(methodTag, "Launching embedded WebView for acquiring auth code.");
                Logger.infoPII(methodTag, "The start url is " + authorizationRequestUrl);

                mAADWebViewClient.setRequestHeaders(requestHeaders);
                mAADWebViewClient.setRequestUrl(authorizationRequestUrl);
                mWebView.loadUrl(authorizationRequestUrl, requestHeaders);

                // The first page load could take time, and we do not want to just show a blank page.
                // Therefore, we'll show a spinner here, and hides it when mAuthorizationRequestUrl is successfully loaded.
                // After that, progress bar will be displayed by MSA/AAD.
                mProgressBar.setVisibility(View.VISIBLE);
            }
        });
    }

    // For CertBasedAuthChallengeHandler within AADWebViewClient,
    // the smartcard manager needs to stop discovering Usb devices upon fragment destroy.
    @Override
    public void onDestroy() {
        super.onDestroy();
        final String methodTag = TAG + ":onDestroy";
        if (mAADWebViewClient != null) {
            mAADWebViewClient.onDestroy();
        } else {
            Logger.error(methodTag, "Fragment destroyed, but smartcard usb discovery was unable to be stopped.", null);
        }
        if (CommonFlightsManager.INSTANCE.getFlightsProvider().isFlightEnabled(CommonFlight.ENABLE_LEGACY_FIDO_SECURITY_KEY_LOGIC)
                && Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                && mFidoLauncher != null) {
            // Note: mFidoLauncher shouldn't be null (based on the OS version check),
            // but we should still have a check here just to be safe.
            mFidoLauncher.unregister();
        }
    }

    /**
     * Extracts request headers from the given bundle object.
     */
    private HashMap<String, String> getRequestHeaders(final Bundle state) {
        try {
            // Suppressing unchecked warnings due to casting of serializable String to HashMap<String, String>
            @SuppressWarnings(WarningType.unchecked_warning) final HashMap<String, String> requestHeaders = (HashMap<String, String>) state.getSerializable(REQUEST_HEADERS);
            final HashMap<String, String> headers = requestHeaders != null ? requestHeaders : new HashMap<>();
            // Attach client extras header for ESTS telemetry. Only done for broker requests
            if (isBrokerRequest) {
                final ClientExtraSku clientExtraSku = ClientExtraSku.builder()
                        .srcSku(state.getString(PRODUCT))
                        .srcSkuVer(state.getString(VERSION))
                        .build();
                headers.put(com.microsoft.identity.common.java.AuthenticationConstants.SdkPlatformFields.CLIENT_EXTRA_SKU, clientExtraSku.toString());
            }
            injectPasskeyProtocolHeader(headers);
            return headers;
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    @Nullable
    public ActivityResultLauncher<LegacyFido2ApiObject> getFidoLauncher() {
        return mFidoLauncher;
    }

    class AuthorizationCompletionCallback implements IAuthorizationCompletionCallback {
        @Override
        public void onChallengeResponseReceived(@NonNull final RawAuthorizationResult response) {
            final String methodTag = TAG + ":onChallengeResponseReceived";
            Logger.info(methodTag, null, "onChallengeResponseReceived:" + response.getResultCode());
            if (mAADWebViewClient != null) {
                //Callback will be run regardless of CBA occurring.
                mAADWebViewClient.finalizeBeforeSendingResult(response, new ISendResultCallback() {
                    @Override
                    public void onResultReady() {
                        sendResult(response);
                        finish();
                    }
                });
                return;
            }
            sendResult(response);
            finish();
        }

        @Override
        public void setPKeyAuthStatus(final boolean status) {
            final String methodTag = TAG + ":setPKeyAuthStatus";
            mPkeyAuthStatus = status;
            Logger.info(methodTag, null, "setPKeyAuthStatus:" + status);
        }
    }

    private SwitchBrowserProtocolCoordinator getSwitchBrowserCoordinator() {
        if (mSwitchBrowserProtocolCoordinator == null) {
            final SpanContext spanContext = requireActivity() instanceof AuthorizationActivity ? ((AuthorizationActivity) requireActivity()).getSpanContext() : null;
            mSwitchBrowserProtocolCoordinator = new SwitchBrowserProtocolCoordinator(requireActivity(), spanContext);
        }
        return mSwitchBrowserProtocolCoordinator;
    }

    /**
     * Set the switch browser bundle to be used when resuming the flow.
     *
     * @param bundle The bundle containing the data needed to resume the flow.
     */
    public static synchronized void setSwitchBrowserBundle(@Nullable final Bundle bundle) {
        switchBrowserBundle = bundle;
    }


    /**
     * Sets up the PasskeyWebListener if the request headers indicate that both authentication and registration
     * are supported. If the hook fails, it downgrades to authentication only.
     * Called during WebView setup.
     */
    private void setupPasskeyWebListener(@NonNull final WebView webView,
                                         @NonNull final AzureActiveDirectoryWebViewClient webViewClient) {
        final String methodTag = TAG + ":setupPasskeyWebListener";
        final String passkeyProtocolHeader = mRequestHeaders.get(FidoConstants.PASSKEY_PROTOCOL_HEADER_NAME);
        if (FidoConstants.PASSKEY_PROTOCOL_HEADER_AUTH_AND_REG.equals(passkeyProtocolHeader)) {
            final boolean passkeyWebListenerHooked = PasskeyWebListener.hook(webView, requireActivity(), webViewClient);
            if (!passkeyWebListenerHooked) {
                Logger.warn(methodTag, "PasskeyWebListener hook failed, Downgrading to auth only.");
                // Downgrade to auth only
                mRequestHeaders.put(FidoConstants.PASSKEY_PROTOCOL_HEADER_NAME, FidoConstants.PASSKEY_PROTOCOL_HEADER_AUTH_ONLY);
            }
        } else {
            Logger.warn(methodTag, "Passkey protocol header not found or not for both auth and reg." +
                    " Not hooking the PasskeyWebListener.");
        }
    }

    /**
     * Injects the Passkey protocol header into the request headers if the WebAuthN query parameter is present.
     * If the header already exists, it will not be modified. If the request is from broker and the Passkey registration flight is enabled,
     * the header will indicate support for both authentication and registration.
     *
     * @param requestHeaders The request headers to modify.
     */
    private void injectPasskeyProtocolHeader(@NonNull final HashMap<String, String> requestHeaders) {
        final String methodTag = TAG + ":injectPasskeyProtocolHeader";
        final Uri authRequestUri = Uri.parse(mAuthorizationRequestUrl);
        final String webAuthNQueryParameter = authRequestUri.getQueryParameter(FidoConstants.WEBAUTHN_QUERY_PARAMETER_FIELD);
        final boolean hasWebAuthNQueryParameter = !StringUtil.isNullOrEmpty(webAuthNQueryParameter);

        if (!hasWebAuthNQueryParameter) {
            return;
        }

        if (isBrokerRequest) {
            final String passkeyProtocolHeaderValue = CommonFlightsManager.INSTANCE
                    .getFlightsProvider().isFlightEnabled(CommonFlight.ENABLE_PASSKEY_REGISTRATION)
                    ? FidoConstants.PASSKEY_PROTOCOL_HEADER_AUTH_AND_REG
                    : FidoConstants.PASSKEY_PROTOCOL_HEADER_AUTH_ONLY;
            Logger.verbose(methodTag, "Injecting Passkey protocol header for broker request: "
                    + passkeyProtocolHeaderValue);
            requestHeaders.put(FidoConstants.PASSKEY_PROTOCOL_HEADER_NAME, passkeyProtocolHeaderValue);
        } else {
            if (requestHeaders.containsKey(FidoConstants.PASSKEY_PROTOCOL_HEADER_NAME)) {
                Logger.verbose(methodTag, "Passkey protocol header already exists in request headers  "
                        + requestHeaders.get(FidoConstants.PASSKEY_PROTOCOL_HEADER_NAME));
                return;
            }
        }
        Logger.verbose(methodTag, "Injecting Passkey protocol header for auth only.");
        requestHeaders.put(FidoConstants.PASSKEY_PROTOCOL_HEADER_NAME, FidoConstants.PASSKEY_PROTOCOL_HEADER_AUTH_ONLY);
    }
}
