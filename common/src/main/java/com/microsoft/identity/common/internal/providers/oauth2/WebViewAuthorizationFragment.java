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

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.microsoft.identity.common.R;
import com.microsoft.identity.common.internal.ui.webview.ISendResultCallback;
import com.microsoft.identity.common.java.WarningType;
import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.internal.ui.webview.AzureActiveDirectoryWebViewClient;
import com.microsoft.identity.common.internal.ui.webview.OnPageLoadedCallback;
import com.microsoft.identity.common.internal.ui.webview.WebViewUtil;
import com.microsoft.identity.common.java.ui.webview.authorization.IAuthorizationCompletionCallback;
import com.microsoft.identity.common.java.providers.RawAuthorizationResult;
import com.microsoft.identity.common.logging.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.AUTH_INTENT;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.POST_PAGE_LOADED_URL;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.REDIRECT_URI;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.REQUEST_HEADERS;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.REQUEST_URL;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.WEB_VIEW_ZOOM_CONTROLS_ENABLED;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.WEB_VIEW_ZOOM_ENABLED;

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

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final FragmentActivity activity = getActivity();
        if (activity != null) {
            WebViewUtil.setDataDirectorySuffix(activity.getApplicationContext());
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
    }

    @Override
    void extractState(@NonNull final Bundle state) {
        super.extractState(state);
        mAuthIntent = state.getParcelable(AUTH_INTENT);
        mPkeyAuthStatus = state.getBoolean(PKEYAUTH_STATUS, false);
        mAuthorizationRequestUrl = state.getString(REQUEST_URL);
        mRedirectUri = state.getString(REDIRECT_URI);
        mRequestHeaders = getRequestHeaders(state);
        mPostPageLoadedJavascript = state.getString(POST_PAGE_LOADED_URL);
        webViewZoomEnabled = state.getBoolean(WEB_VIEW_ZOOM_ENABLED, true);
        webViewZoomControlsEnabled = state.getBoolean(WEB_VIEW_ZOOM_CONTROLS_ENABLED, true);
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
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                mWebView.evaluateJavascript(javascriptToExecute[0], null);
                            } else {
                                // On earlier versions of Android, javascript has to be loaded with a custom scheme.
                                // In these cases, Android will helpfully unescape any octects it finds. Unfortunately,
                                // our javascript may contain the '%' character, so we escape it again, to undo that.
                                mWebView.loadUrl("javascript:" + javascriptToExecute[0].replace("%", "%25"));
                            }
                        }
                    }



                },
                mRedirectUri);
        setUpWebView(view, mAADWebViewClient);
        checkPermissionsAndLaunchWebView(runTimeRequiredPermissions());
        return view;
    }


    private void checkPermissionsAndLaunchWebView(@NonNull final List<String> requiredPermissions) {
        if (allRequiredPermissionsGranted(requiredPermissions)) {
            launchWebView();
        } else if (shouldShowRequestPermissionRationale(requiredPermissions)) {
            showRequestPermissionRationale(requiredPermissions);
        } else {
            requestPermissionLauncher.launch(requiredPermissions.toArray(new String[0]));
        }
    }


    private void showRequestPermissionRationale(@NonNull final List<String> permissions) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setMessage("This app requires the following permissions to continue:" + permissions.toString())
                .setTitle("Permission required")
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, id) -> {
                   if (shouldShowRequestPermissionRationale(permissions))
                   {
                       requestPermissionLauncher.launch(permissions.toArray(new String[0]));
                   } else {
                       // User has permanently denied the permission, take them to settings
                       final Intent intent = new Intent(
                               android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                               Uri.fromParts("package", getContext().getPackageName(), null)
                       );
                       someActivityResultLauncher.launch(intent);
                   }
                })
                .setNegativeButton("Cancel", (dialog, id) -> {
                    // User cancelled the dialog
                    dialog.dismiss();
                    cancelAuthorization(true);
                });
        builder.show();
    }

    private Boolean shouldShowRequestPermissionRationale(@NonNull final List<String> permissions) {
        for (final String permission : permissions) {
            if(shouldShowRequestPermissionRationale(permission)) {
                return true;
            }
        }
        return false;
    }

    private Boolean allRequiredPermissionsGranted(@NonNull final List<String> permissions) {
        for (final String permission : permissions) {
            if (ContextCompat.checkSelfPermission(getContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
    private List<String> runTimeRequiredPermissions() {
        final List<String> permissions = new ArrayList();
        if (isQRCodePlusPin()) {
            permissions.add(Manifest.permission.CAMERA);
        }
        return permissions;
    }

    private Boolean isQRCodePlusPin() {
        return true;
        //mAuthorizationRequestUrl.contains("login");
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
        mWebView.getSettings().setPluginState(WebSettings.PluginState.ON);

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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    request.grant(request.getResources());
                }
            }


        });


    }

    /**
     * Loads starting authorization request url into WebView.
     */
    private void launchWebView() {
        final String methodTag = TAG + ":launchWebView";
        mWebView.post(new Runnable() {
            @Override
            public void run() {
                Logger.info(methodTag, "Launching embedded WebView for acquiring auth code.");
                Logger.infoPII(methodTag, "The start url is " + mAuthorizationRequestUrl);

                mAADWebViewClient.setRequestHeaders(mRequestHeaders);

                mWebView.loadUrl("https://gentle-pebble-09da6fb1e.3.azurestaticapps.net/");

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
    }

    private ActivityResultLauncher<String[]> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            permissions   -> {
                final List<String> rejectedPermissions = new ArrayList();
                for (final String permission : permissions.keySet()) {
                    if (permissions.get(permission) == false) {
                        rejectedPermissions.add(permission);
                        //Toast.makeText(getContext(), "We cannot proceed without the permission:" + permission, Toast.LENGTH_SHORT).show();
                    }
                }
                if (rejectedPermissions.isEmpty()) {
                    // ALL PERMISSIONS GRANTED
                    launchWebView();
                } else {
                    showRequestPermissionRationale(rejectedPermissions);
                }
            }
    );


    // You can do the assignment inside onAttach or onCreate, i.e, before the activity is displayed
    private ActivityResultLauncher<Intent> someActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_CANCELED) {
                    checkPermissionsAndLaunchWebView(runTimeRequiredPermissions());
                }
            });


    /**
     * Extracts request headers from the given bundle object.
     */
    private HashMap<String, String> getRequestHeaders(final Bundle state) {
        try {
            // Suppressing unchecked warnings due to casting of serializable String to HashMap<String, String>
            @SuppressWarnings(WarningType.unchecked_warning)
            HashMap<String, String> requestHeaders = (HashMap<String, String>) state.getSerializable(REQUEST_HEADERS);

            return requestHeaders;
        } catch (Exception e) {
            return null;
        }
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
}
