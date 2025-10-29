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
package com.microsoft.identity.common.internal.ui.webview;

import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Browser.SSL_HELP_URL;

import android.app.Activity;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.view.View;
import android.webkit.HttpAuthHandler;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.internal.broker.AuthUxJavaScriptInterface;
import com.microsoft.identity.common.internal.ui.webview.challengehandlers.ChallengeFactory;
import com.microsoft.identity.common.internal.ui.webview.challengehandlers.IChallengeHandler;
import com.microsoft.identity.common.internal.ui.webview.challengehandlers.NtlmChallenge;
import com.microsoft.identity.common.internal.ui.webview.challengehandlers.NtlmChallengeHandler;
import com.microsoft.identity.common.internal.util.StringUtil;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.flighting.CommonFlight;
import com.microsoft.identity.common.java.flighting.CommonFlightsManager;
import com.microsoft.identity.common.java.opentelemetry.AttributeName;
import com.microsoft.identity.common.java.opentelemetry.OTelUtility;
import com.microsoft.identity.common.java.providers.RawAuthorizationResult;
import com.microsoft.identity.common.java.ui.webview.authorization.IAuthorizationCompletionCallback;
import com.microsoft.identity.common.logging.Logger;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;

public abstract class OAuth2WebViewClient extends WebViewClient {
    /* constants */
    private static final String TAG = OAuth2WebViewClient.class.getSimpleName();

    private static final LongCounter sWebViewSslErrorCount = OTelUtility.createLongCounter(
            "web_view_ssl_error_count",
            "Number of SSL errors received in onReceivedSslError"
    );

    private final IAuthorizationCompletionCallback mCompletionCallback;
    private final OnPageLoadedCallback mPageLoadedCallback;
    private final Activity mActivity;

    @SuppressFBWarnings(value = "MS_SHOULD_BE_FINAL", justification = "This is only exposed in testing")
    @VisibleForTesting
    public static ExpectedPage mExpectedPage = null;

    protected boolean mAuthUxJavaScriptInterfaceAdded = false;

    /**
     * @return context
     */
    public Activity getActivity() {
        return mActivity;
    }

    /**
     * @return handler completion callback
     */
    IAuthorizationCompletionCallback getCompletionCallback() {
        return mCompletionCallback;
    }

    /**
     * Constructor for the OAuth2 basic web view client.
     *
     * @param activity           app Context
     * @param completionCallback Challenge completion callback
     * @param pageLoadedCallback callback to be triggered on page load. For UI purposes.
     */
    OAuth2WebViewClient(@NonNull final Activity activity,
                        @NonNull final IAuthorizationCompletionCallback completionCallback,
                        @NonNull final OnPageLoadedCallback pageLoadedCallback
    ) {
        // the validation of redirect url and authorization request should be in upper level before launching the webview.
        mActivity = activity;
        mCompletionCallback = completionCallback;
        mPageLoadedCallback = pageLoadedCallback;
    }

    @Override
    public void onReceivedHttpAuthRequest(WebView view, final HttpAuthHandler handler,
                                          String host, String realm) {
        final String methodTag = TAG + ":onReceivedHttpAuthRequest";
        // Create a dialog to ask for credentials and post it to the handler.
        Logger.info(methodTag,"Receive the http auth request. Start the dialog to ask for creds. ");
        Logger.infoPII(methodTag,"Host:" + host);

        //TODO TelemetryEvent.setNTLM(true); after the Telemetry is finished in common.
        // Use ChallengeFactory to produce a NtlmChallenge
        final NtlmChallenge ntlmChallenge = ChallengeFactory.getNtlmChallenge(view, handler, host, realm);

        // Init the NtlmChallengeHandler
        final IChallengeHandler<NtlmChallenge, Void> challengeHandler = new NtlmChallengeHandler(mActivity, mCompletionCallback);

        //Process the challenge through the NtlmChallengeHandler created
        challengeHandler.processChallenge(ntlmChallenge);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onReceivedError(final WebView view,
                                final int errorCode,
                                final String description,
                                final String failingUrl) {
        sendErrorToCallback(view, errorCode, description);
    }

    /**
     * API 23+ overload of {@link #onReceivedError(WebView, int, String, String)} - unlike the pre-23
     * impl, this overload will trigger pageload errors for subframes of the page. As these may not
     * necessarily affect the sign-in experience (such as failed scripts in an iframe), we are going
     * to ignore errors for the non-main-frame such that the pre-API 23 behavior is preserved.
     * <p>
     * More info:
     * https://stackoverflow.com/questions/44068123/how-to-detect-errors-only-from-the-main-page-in-new-onreceivederror-from-webview
     * https://developer.android.com/reference/android/webkit/WebViewClient#onReceivedError(android.webkit.WebView,%20android.webkit.WebResourceRequest,%20android.webkit.WebResourceError)
     *
     * @param view    The WebView which triggered the error.
     * @param request The request which failed within the page.
     * @param error   The error yielded by the failing request.
     * @see #onReceivedError(WebView, int, String, String)
     */
    @Override
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void onReceivedError(@NonNull final WebView view,
                                @NonNull final WebResourceRequest request,
                                @NonNull final WebResourceError error) {
        final String methodTag = TAG + ":onReceivedError";
        final boolean isForMainFrame = request.isForMainFrame();

        Logger.warn(methodTag, "WebResourceError - isForMainFrame? " + isForMainFrame);
        Logger.warnPII(methodTag, "Failing url: " + request.getUrl());

        if (request.isForMainFrame()) {
            sendErrorToCallback(view, error.getErrorCode(), error.getDescription().toString());
        }
    }

    private void sendErrorToCallback(@NonNull final WebView view,
                                     final int errorCode,
                                     @NonNull final String description) {
        view.stopLoading();

        // Send the result back to the calling activity
        mCompletionCallback.onChallengeResponseReceived(
                RawAuthorizationResult.fromException(
                        new ClientException("Code:" + errorCode, description)));
    }

    @Override
    public void onReceivedSslError(final WebView view,
                                   final SslErrorHandler handler,
                                   final SslError error) {
        // Developer does not have option to control this for now
        final String methodTag = TAG + ":onReceivedSslError";
        super.onReceivedSslError(view, handler, error);
        final String errMsg = "Received SSL Error during request. For more info see: " + SSL_HELP_URL + ". Error: " + error.toString();

        Logger.warn(methodTag, errMsg);
        final Attributes attributes = Attributes.builder()
                .put(AttributeName.web_view_ssl_primary_error_code.name(), error.getPrimaryError())
                .build();
        sWebViewSslErrorCount.add(1, attributes);
        if (!CommonFlightsManager.INSTANCE.getFlightsProvider().isFlightEnabled(CommonFlight.SHOULD_PRESERVE_WEBVIEW_FLOW_ON_SSL_ERROR)) {
            // Send the result back to the calling activity
            mCompletionCallback.onChallengeResponseReceived(
                    RawAuthorizationResult.fromException(
                            new ClientException("Code:" + ERROR_FAILED_SSL_HANDSHAKE, error.toString())));
        }
    }

    @Override
    public void onPageFinished(final WebView view,
                               final String url) {
        super.onPageFinished(view, url);
        mPageLoadedCallback.onPageLoaded(url);

        //Supports UI Automation... informing that the webview resource is now idle
        if (mExpectedPage != null && url.startsWith(mExpectedPage.mExpectedPageUrlStartsWith)) {
            mExpectedPage.mCallback.onPageLoaded(url);
        }

        // Once web view is fully loaded,set to visible
        view.setVisibility(View.VISIBLE);
    }

    /**
     * We received an ANR bug report from LTW, where webview crashing would cause the calling application to crash as well,
     * because Broker was not handling the crash and packaging it into an exception
     *
     * Overriding this method allows us to do that and send an error gracefully to the callback object when the render process is gone.
     * @param view webview in question
     * @param detail minor details about the render process being lost
     * @return whether or not we handled the crash
     */
    @Override
    public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
        // Handle render process crash
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            sendErrorToCallback(view, -1, "WebView render process gone, crashed? : " + detail.didCrash());
        } else {
            sendErrorToCallback(view, -1, "WebView render process gone.");
        }

        return true; // Indicate we handled the crash
    }

    @Override
    public void onPageStarted(final WebView view,
                              final String url,
                              final Bitmap favicon) {
        final String methodTag = TAG + ":onPageStarted";
        checkStartUrl(url);

        // Re-evaluate adding AuthUx JavaScript Interface
        if (shouldExposeJavaScriptInterface(url)) {
            // If broker request, and a valid url, expose JavaScript API
            Logger.info(methodTag, "Adding AuthUx JavaScript Interface");
            view.addJavascriptInterface(new AuthUxJavaScriptInterface(), AuthUxJavaScriptInterface.Companion.getInterfaceName());
            mAuthUxJavaScriptInterfaceAdded = true;
        } else if (mAuthUxJavaScriptInterfaceAdded) {
            // Remove AuthUx JavaScript Interface
            Logger.info(methodTag, "Removing AuthUx JavaScript Interface");
            view.removeJavascriptInterface(AuthUxJavaScriptInterface.Companion.getInterfaceName());
            mAuthUxJavaScriptInterfaceAdded = false;
        }

        Logger.info(methodTag,"WebView starts loading.");
        super.onPageStarted(view, url, favicon);
    }

    private void checkStartUrl(final String url) {
        final String methodTag = TAG + ":checkStartUrl";
        if (StringUtil.isEmpty(url)) {
            Logger.info(methodTag,"onPageStarted: Null url for page to load.");
            return;
        }

        final Uri uri = Uri.parse(url);
        if (uri.isOpaque()) {
            Logger.info(methodTag,"onPageStarted: Non-hierarchical loading uri.");
            Logger.infoPII(methodTag,"start url: " + url);
        } else if (StringUtil.isEmpty(uri.getQueryParameter(AuthenticationConstants.OAuth2.CODE))) {
            Logger.info(methodTag,"onPageStarted: URI has no auth code ('"
                    + AuthenticationConstants.OAuth2.CODE + "') query parameter.");
            Logger.infoPII(methodTag,"Scheme:" + uri.getScheme() + " Host: " + uri.getHost()
                    + " Path: " + uri.getPath());
        } else {
            Logger.info(methodTag,"Auth code is returned for the loading url.");
            Logger.infoPII(methodTag,"Scheme:" + uri.getScheme() + " Host: " + uri.getHost()
                    + " Path: " + uri.getPath());
        }
    }

    protected boolean shouldExposeJavaScriptInterface(final String url) {
        return ProcessUtil.isRunningOnAuthService(getActivity().getApplicationContext())
                && AuthUxJavaScriptInterface.Companion.isValidUriForInterface(url)
                && CommonFlightsManager.INSTANCE.getFlightsProvider().isFlightEnabled(CommonFlight.ENABLE_JS_API_FOR_AUTHUX);
    }
}
