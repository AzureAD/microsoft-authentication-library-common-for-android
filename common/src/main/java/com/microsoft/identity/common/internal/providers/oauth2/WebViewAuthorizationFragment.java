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
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.WEB_VIEW_WEB_CP_ENABLED;
import static com.microsoft.identity.common.java.AuthenticationConstants.SdkPlatformFields.PRODUCT;
import static com.microsoft.identity.common.java.AuthenticationConstants.SdkPlatformFields.VERSION;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import com.microsoft.identity.common.internal.ui.webview.IUrlLoadTracker;
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

import com.microsoft.identity.common.java.opentelemetry.AttributeName;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static com.microsoft.identity.common.java.AuthenticationConstants.OAuth2.UTID;

import com.microsoft.identity.common.java.opentelemetry.OTelUtility;
import com.microsoft.identity.common.java.opentelemetry.SpanExtension;
import com.microsoft.identity.common.java.opentelemetry.SpanName;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Scope;

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

    private boolean isWebViewWebcpEnabledInBrokerlessCase;

    private String mUtid;

    private final CameraPermissionRequestHandler mCameraPermissionRequestHandler = new CameraPermissionRequestHandler(this);

    /**
     * Callback for file chooser requests from the WebView.
     * This is set when {@link WebChromeClient#onShowFileChooser} is invoked and
     * must be called back with the selected file URI(s) or null if cancelled.
     */
    private ValueCallback<Uri[]> mFileUploadCallback;

    /**
     * Launcher for the file chooser activity, registered in {@link #onCreate}.
     * Handles the result of the file selection and passes it back to the WebView.
     */
    private ActivityResultLauncher<Intent> mFileChooserLauncher;

    // This is used by LegacyFido2ApiManager to launch a PendingIntent received by the legacy API.
    private ActivityResultLauncher<LegacyFido2ApiObject> mFidoLauncher;
    // This is used by the switch browser protocol to handle the resume of the flow.
    private SwitchBrowserProtocolCoordinator mSwitchBrowserProtocolCoordinator = null;

    private boolean isBrokerRequest = false;

    private static final AtomicReference<Bundle> sSwitchBrowserBundle = new AtomicReference<>();

    public static void setSwitchBrowserBundle(@Nullable final Bundle bundle) {
        sSwitchBrowserBundle.set(bundle);
    }

    public static void clearSwitchBrowserBundle() {
        sSwitchBrowserBundle.set(null);
    }

    private static @Nullable Bundle consumeSwitchBrowserBundle() {
        return sSwitchBrowserBundle.getAndSet(null);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final String methodTag = TAG + ":onCreate";
        Logger.verbose(methodTag, "WebViewAuthorizationFragment onCreate");
        final FragmentActivity activity = getActivity();
        if (activity != null) {
            WebViewUtil.setDataDirectorySuffix(activity.getApplicationContext());
        }

        // Register file chooser launcher for WebView file upload support.
        if (CommonFlightsManager.INSTANCE.getFlightsProvider().isFlightEnabled(CommonFlight.ENABLE_WEBVIEW_FILE_UPLOAD)) {
            mFileChooserLauncher = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (mFileUploadCallback == null) {
                            Logger.warn(methodTag, "File upload callback is null, ignoring result.");
                            return;
                        }
                        Uri[] resultUris = null;
                        if (result.getResultCode() == FragmentActivity.RESULT_OK && result.getData() != null) {
                            final Intent data = result.getData();
                            if (data.getClipData() != null) {
                                // Multiple files selected
                                final int count = data.getClipData().getItemCount();
                                resultUris = new Uri[count];
                                for (int i = 0; i < count; i++) {
                                    resultUris[i] = data.getClipData().getItemAt(i).getUri();
                                }
                            } else if (data.getData() != null) {
                                // Single file selected
                                resultUris = new Uri[]{data.getData()};
                            }
                            Logger.info(methodTag, "File chooser returned "
                                    + (resultUris != null ? resultUris.length : 0) + " file(s).");
                        } else {
                            Logger.info(methodTag, "File chooser cancelled or returned no data.");
                        }
                        mFileUploadCallback.onReceiveValue(resultUris);
                        mFileUploadCallback = null;
                    }
            );
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
            clearSwitchBrowserBundle();
        }
    }

    /**
     * Resume the switch browser protocol flow.
     */
    private void resumeSwitchBrowser() {
        final String methodTag = TAG + ":resumeSwitchBrowser";
        try {
            final Bundle switchBrowserBundle = consumeSwitchBrowserBundle();
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
        outState.putBoolean(WEB_VIEW_WEB_CP_ENABLED, isWebViewWebcpEnabledInBrokerlessCase);
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
        isWebViewWebcpEnabledInBrokerlessCase = state.getBoolean(WEB_VIEW_WEB_CP_ENABLED, false);
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

                        // Dynamically toggle multiple-windows support so that target="_blank"
                        // interception is active ONLY on the TLR start page. On all other
                        // pages the WebView behaves exactly as before.
                        if (CommonFlightsManager.INSTANCE.getFlightsProvider()
                                .isFlightEnabled(CommonFlight.ENABLE_WEBVIEW_MULTIPLE_WINDOWS)) {
                            mWebView.getSettings().setSupportMultipleWindows(isTlrUrl(url));
                        }
                    }
                },
                mRedirectUri,
                getSwitchBrowserCoordinator().getSwitchBrowserRequestHandler(),
                mUtid,
                isWebViewWebcpEnabledInBrokerlessCase,
                new IUrlLoadTracker() {
                    @Override
                    public void trackNewUrlStatus(final String url, final String loadingError, final String authError) {
                        WebViewAuthorizationFragment.this.trackUrlStatus(url, loadingError, authError);
                    }

                    @Override
                    public void updateLatestUrlStatus(final String loadingError, final String authError) {
                        WebViewAuthorizationFragment.this.updateLatestUrlStatus(loadingError, authError);
                    }

                    @Override
                    public Map<Integer, UrlStatus> getUrlStatusMap() {
                        return WebViewAuthorizationFragment.this.getUrlLoadTracker();
                    }
                }
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
            public boolean onShowFileChooser(
                    final WebView webView,
                    final ValueCallback<Uri[]> filePathCallback,
                    final FileChooserParams fileChooserParams) {
                final FragmentActivity host = getActivity();
                final SpanContext parentSpanContext = host instanceof AuthorizationActivity
                        ? ((AuthorizationActivity) host).getSpanContext() : null;
                return handleFileUploadRequest(filePathCallback, fileChooserParams, parentSpanContext);
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

            @Override
            public boolean onCreateWindow(final WebView view, boolean isDialog,
                                          boolean isUserGesture, final Message resultMsg) {
                if (resultMsg.obj == null) {
                    Logger.error(methodTag, "onCreateWindow: resultMsg.obj is null, cannot set up transport.", null);
                    return false;
                }

                final SpanContext parentSpanContext = requireActivity() instanceof AuthorizationActivity
                        ? ((AuthorizationActivity) requireActivity()).getSpanContext() : null;
                final Span span = OTelUtility.createSpanFromParent(
                        SpanName.WebViewTargetBlankNavigation.name(), parentSpanContext);
                boolean windowHandled = false;
                try (final Scope scope = SpanExtension.makeCurrentSpan(span)) {
                    Logger.info(methodTag, "onCreateWindow: intercepting target=_blank navigation.");
                    final WebView interceptorWebView = new WebView(view.getContext());
                    interceptorWebView.setWebViewClient(new WebViewClient() {
                        @Override
                        public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest request) {
                            handleInterceptedUrlFromNewWindow(view, v, request, span, isUserGesture);
                            return true;
                        }
                    });
                    final WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                    transport.setWebView(interceptorWebView);
                    resultMsg.sendToTarget();
                    // Span status and end are handled in handleInterceptedUrlFromNewWindow,
                    // which fires asynchronously when shouldOverrideUrlLoading is called.
                    windowHandled = true;
                } catch (@NonNull final Exception e) {
                    Logger.error(methodTag, "Error handling target=_blank navigation.", e);
                    span.recordException(e);
                    span.setStatus(StatusCode.ERROR);
                    span.end();
                }
                return windowHandled;
            }
        });
        setupPasskeyWebListener(mWebView, webViewClient);
    }

    /**
     * Handles the URL intercepted from a target=_blank navigation (onCreateWindow).
     * Routes the URL based on whether the main WebView is currently on a TLR page:
     * - TLR page: opens the URL in an external browser.
     * - Non-TLR page: loads the URL inline in the main WebView.
     *
     * @param mainWebView        The main authentication WebView.
     * @param interceptorWebView The temporary interceptor WebView (will be destroyed after handling).
     * @param request            The intercepted URL request.
     * @param span               The telemetry span to record which routing path is taken.
     * @param isUserGesture      Whether the popup was initiated by a user gesture (e.g. a click).
     */
    @VisibleForTesting
    void handleInterceptedUrlFromNewWindow(@NonNull final WebView mainWebView,
                                                   @NonNull final WebView interceptorWebView,
                                                   @NonNull final WebResourceRequest request,
                                                   @NonNull final Span span,
                                                   final boolean isUserGesture) {
        final String methodTag = TAG + ":handleInterceptedUrlFromNewWindow";
        try {
            final String targetUrl = request.getUrl().toString();
            final String currentPageUrl = mainWebView.getUrl();

            if (targetUrl == null) {
                span.setAttribute(AttributeName.target_blank_navigation_route.name(), AuthenticationConstants.Broker.WEBVIEW_TARGET_BLANK_ROUTE_NULL_URL);
                Logger.warn(methodTag, "onCreateWindow: target URL is null, ignoring.");
            } else if (!isUserGesture) {
                // Not initiated by user gesture: load inline as a safe fallback instead of
                // opening an external browser, to prevent programmatic/scripted popups.
                span.setAttribute(AttributeName.target_blank_navigation_route.name(), AuthenticationConstants.Broker.WEBVIEW_TARGET_BLANK_ROUTE_NO_USER_GESTURE);
                Logger.warn(methodTag, "onCreateWindow: popup not initiated by user gesture, loading URL inline.");
                mainWebView.loadUrl(targetUrl);
            } else if (!targetUrl.toLowerCase().startsWith(AuthenticationConstants.Broker.REDIRECT_SSL_PREFIX)) {
                // Non-SSL URL: refuse to open, matching AzureActiveDirectoryWebViewClient behavior.
                span.setAttribute(AttributeName.target_blank_navigation_route.name(), AuthenticationConstants.Broker.WEBVIEW_TARGET_BLANK_ROUTE_NON_SSL);
                Logger.error(methodTag, "onCreateWindow: URL is not SSL protected, refusing to open.", null);
            } else if (!isTlrUrl(currentPageUrl)) {
                // Non-TLR page: load inline, same as WebView default behavior.
                span.setAttribute(AttributeName.target_blank_navigation_route.name(), AuthenticationConstants.Broker.WEBVIEW_TARGET_BLANK_ROUTE_NON_TLR);
                Logger.warn(methodTag, "onCreateWindow: non-TLR page, loading URL inline as fallback.");
                mainWebView.loadUrl(targetUrl);
            } else {
                // TLR page: delegate to system browser so user can view terms externally.
                span.setAttribute(AttributeName.target_blank_navigation_route.name(), AuthenticationConstants.Broker.WEBVIEW_TARGET_BLANK_ROUTE_TLR);
                Logger.info(methodTag, "onCreateWindow: TLR page, delegating URL to system browser.");
                final Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl));
                mainWebView.getContext().startActivity(browserIntent);
            }
            span.setStatus(StatusCode.OK);
        } catch (final Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR);
            Logger.error(methodTag, "Error handling target=_blank URL.", e);
        } finally {
            span.end();
            // Destroy the interceptor WebView after it has served its purpose
            interceptorWebView.post(interceptorWebView::destroy);
        }
    }

    /**
     * Checks whether the given URL corresponds to a TLR (Terms, License, and Restrictions)
     * start page.
     *
     * @param url The URL to check.
     * @return {@code true} if the URL is a TLR start page, {@code false} otherwise.
     */
    @VisibleForTesting
    boolean isTlrUrl(@Nullable final String url) {
        if (url == null) {
            return false;
        }
        final String lowerUrl = url.toLowerCase();
        return lowerUrl.startsWith(AuthenticationConstants.Broker.REDIRECT_SSL_PREFIX)
                && lowerUrl.contains(AuthenticationConstants.Broker.TLR_START_PATH);
    }

    /**
     * Handles a file chooser request from the WebView. Creates a telemetry span,
     * manages the file upload callback, and launches the system file picker.
     *
     * @param filePathCallback  The callback to deliver file selection results to the WebView.
     * @param fileChooserParams Parameters describing the file chooser request.
     * @param parentSpanContext The parent span context for telemetry, or null.
     * @return {@code true} if the file chooser was launched, {@code false} otherwise.
     */
    @VisibleForTesting
    boolean handleFileUploadRequest(
            @NonNull final ValueCallback<Uri[]> filePathCallback,
            @NonNull final WebChromeClient.FileChooserParams fileChooserParams,
            @Nullable final SpanContext parentSpanContext) {
        final String methodTag = TAG + ":handleFileUploadRequest";

        if (!CommonFlightsManager.INSTANCE.getFlightsProvider()
                .isFlightEnabled(CommonFlight.ENABLE_WEBVIEW_FILE_UPLOAD)) {
            Logger.info(methodTag, "ENABLE_WEBVIEW_FILE_UPLOAD flight is disabled.");
            return false;
        }

        final Span span = OTelUtility.createSpanFromParent(
                SpanName.WebViewFileUpload.name(), parentSpanContext);

        try (final Scope scope = SpanExtension.makeCurrentSpan(span)) {
            // Cancel any existing callback to avoid a dangling reference.
            if (mFileUploadCallback != null) {
                mFileUploadCallback.onReceiveValue(null);
            }
            // Clear any previous callback reference before handling the new request.
            mFileUploadCallback = null;

            // Ensure the file chooser launcher is initialized before attempting to launch.
            if (mFileChooserLauncher == null) {
                Logger.error(methodTag,
                        "File chooser launcher is not initialized. Cannot handle file upload request.",
                        null);
                // Notify the caller that no file was selected/returned.
                filePathCallback.onReceiveValue(null);
                span.setStatus(StatusCode.ERROR);
                return false;
            }

            // At this point we have a valid launcher; store the callback for the result.
            mFileUploadCallback = filePathCallback;

            final Intent intent = fileChooserParams.createIntent();
            Logger.info(methodTag, "Launching file chooser for WebView file upload.");
            mFileChooserLauncher.launch(intent);
            span.setStatus(StatusCode.OK);
            return true;
        } catch (final Exception e) {
            Logger.error(methodTag, "Failed to launch file chooser.", e);
            span.recordException(e);
            span.setStatus(StatusCode.ERROR);
            if (mFileUploadCallback != null) {
                mFileUploadCallback.onReceiveValue(null);
                mFileUploadCallback = null;
            }
            return false;
        } finally {
            span.end();
        }
    }

    @VisibleForTesting
    void setFileUploadCallback(@Nullable final ValueCallback<Uri[]> callback) {
        mFileUploadCallback = callback;
    }

    @VisibleForTesting
    ValueCallback<Uri[]> getFileUploadCallback() {
        return mFileUploadCallback;
    }

    @VisibleForTesting
    void setFileChooserLauncher(@Nullable final ActivityResultLauncher<Intent> launcher) {
        mFileChooserLauncher = launcher;
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
        // Clean up file upload callback to prevent memory leaks.
        if (mFileUploadCallback != null) {
            mFileUploadCallback.onReceiveValue(null);
            mFileUploadCallback = null;
        }
        if (mFileChooserLauncher != null) {
            mFileChooserLauncher.unregister();
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

        if (StringUtil.isNullOrEmpty(webAuthNQueryParameter)) {
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
            } else {
                Logger.verbose(methodTag, "Injecting Passkey protocol header for auth only.");
                requestHeaders.put(FidoConstants.PASSKEY_PROTOCOL_HEADER_NAME, FidoConstants.PASSKEY_PROTOCOL_HEADER_AUTH_ONLY);
            }
        }

    }
}
