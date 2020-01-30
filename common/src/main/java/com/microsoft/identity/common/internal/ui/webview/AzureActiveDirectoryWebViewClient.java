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

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.text.TextUtils;
import android.webkit.ClientCertRequest;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.exception.ErrorStrings;
import com.microsoft.identity.common.internal.broker.PackageHelper;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.ui.webview.challengehandlers.ClientCertAuthChallengeHandler;
import com.microsoft.identity.common.internal.ui.webview.challengehandlers.IAuthorizationCompletionCallback;
import com.microsoft.identity.common.internal.ui.webview.challengehandlers.PKeyAuthChallenge;
import com.microsoft.identity.common.internal.ui.webview.challengehandlers.PKeyAuthChallengeFactory;
import com.microsoft.identity.common.internal.ui.webview.challengehandlers.PKeyAuthChallengeHandler;
import com.microsoft.identity.common.internal.util.StringUtil;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.AUTHORIZATION_FINAL_URL;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Browser.SUB_ERROR_UI_CANCEL;

/**
 * For web view client, we do not distinguish V1 from V2.
 * Thus we name V1 and V2 webview client as AADWebViewClient, synced with the naming in the iOS common library.
 * <p>
 * The only differences between V1 and V2 is
 * 1. on the start url construction, which is handled in the Authorization request classes.
 * 2. the auth result is handled in the Authorization result classes.
 */
public class AzureActiveDirectoryWebViewClient extends OAuth2WebViewClient {
    private static final String TAG = AzureActiveDirectoryWebViewClient.class.getSimpleName();

    public static final String ERROR = "error";
    public static final String ERROR_SUBCODE = "error_subcode";
    public static final String ERROR_DESCRIPTION = "error_description";
    private final String mRedirectUrl;

    public AzureActiveDirectoryWebViewClient(@NonNull final Activity activity,
                                             @NonNull final IAuthorizationCompletionCallback completionCallback,
                                             @NonNull final OnPageLoadedCallback pageLoadedCallback,
                                             @NonNull final String redirectUrl) {
        super(activity, completionCallback, pageLoadedCallback);
        mRedirectUrl = redirectUrl;
    }

    /**
     * Give the host application a chance to take over the control when a new url is about to be loaded in the current WebView.
     * This method was deprecated in API level 24.
     *
     * @param view The WebView that is initiating the callback.
     * @param url  The url to be loaded.
     * @return return true means the host application handles the url, while return false means the current WebView handles the url.
     */
    @Override
    public boolean shouldOverrideUrlLoading(final WebView view, final String url) {
        if (StringUtil.isEmpty(url)) {
            throw new IllegalArgumentException("Redirect to empty url in web view.");
        }
        return handleUrl(view, url);
    }

    /**
     * Give the host application a chance to take over the control when a new url is about to be loaded in the current WebView.
     * This method is added in API level 24.
     *
     * @param view    The WebView that is initiating the callback.
     * @param request Object containing the details of the request.
     * @return return true means the host application handles the url, while return false means the current WebView handles the url.
     */
    @Override
    @TargetApi(Build.VERSION_CODES.N)
    @RequiresApi(Build.VERSION_CODES.N)
    public boolean shouldOverrideUrlLoading(final WebView view, final WebResourceRequest request) {
        final Uri requestUrl = request.getUrl();
        return handleUrl(view, requestUrl.toString());
    }

    private boolean handleUrl(final WebView view, final String url) {
        final String formattedURL = url.toLowerCase(Locale.US);

        if (isPkeyAuthUrl(formattedURL)) {
            Logger.info(TAG, "WebView detected request for pkeyauth challenge.");
            try {
                final PKeyAuthChallengeFactory factory = new PKeyAuthChallengeFactory();
                final PKeyAuthChallenge pKeyAuthChallenge = factory.getPKeyAuthChallenge(url);
                final PKeyAuthChallengeHandler pKeyAuthChallengeHandler = new PKeyAuthChallengeHandler(view, getCompletionCallback());
                pKeyAuthChallengeHandler.processChallenge(pKeyAuthChallenge);
            } catch (final ClientException exception) {
                Logger.error(TAG, exception.getErrorCode(), null);
                Logger.errorPII(TAG, exception.getMessage(), exception);
                returnError(exception.getErrorCode(), exception.getMessage());
                view.stopLoading();
            }
            return true;
        } else if (isRedirectUrl(formattedURL)) {
            Logger.info(TAG, "Navigation starts with the redirect uri.");
            return processRedirectUrl(view, url);
        } else if (isWebsiteRequestUrl(formattedURL)) {
            Logger.info(TAG, "It is an external website request");
            return processWebsiteRequest(view, url);
        } else if (isInstallRequestUrl(formattedURL)) {
            Logger.info(TAG, "It is an install request");
            return processInstallRequest(view, url);
        } else {
            Logger.info(TAG, "It is an invalid redirect uri.");
            return processInvalidUrl(view, url);
        }
    }

    private boolean isPkeyAuthUrl(@NonNull final String url) {
        return url.startsWith(AuthenticationConstants.Broker.PKEYAUTH_REDIRECT.toLowerCase());
    }

    private boolean isRedirectUrl(@NonNull final String url) {
        return url.startsWith(mRedirectUrl.toLowerCase(Locale.US));
    }

    private boolean isWebsiteRequestUrl(@NonNull final String url) {
        return url.startsWith(AuthenticationConstants.Broker.BROWSER_EXT_PREFIX);
    }

    private boolean isInstallRequestUrl(@NonNull final String url) {
        return url.startsWith(AuthenticationConstants.Broker.BROWSER_EXT_INSTALL_PREFIX);
    }

    private boolean isBrokerRequest(final Intent callingIntent) {
        // Intent should have a flag and activity is hosted inside broker
        return callingIntent != null
                && !StringExtensions.isNullOrBlank(callingIntent
                .getStringExtra(AuthenticationConstants.Broker.BROKER_REQUEST));
    }

    // This function is only called when the client received a redirect that starts with the apps
    // redirect uri.
    protected boolean processRedirectUrl(@NonNull final WebView view, @NonNull final String url) {
        final Map<String, String> parameters = StringExtensions.getUrlParameters(url);
        if (!StringExtensions.isNullOrBlank(parameters.get(ERROR))) {
            Logger.info(TAG, "Sending intent to cancel authentication activity");
            Intent resultIntent = new Intent();
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

            //If user clicked the "Back" button in the webview
            if (!StringUtil.isEmpty(parameters.get(ERROR_SUBCODE)) && parameters.get(ERROR_SUBCODE).equalsIgnoreCase(SUB_ERROR_UI_CANCEL)) {
                getCompletionCallback().onChallengeResponseReceived(AuthenticationConstants.UIResponse.BROWSER_CODE_CANCEL, resultIntent);
            } else {
                getCompletionCallback().onChallengeResponseReceived(AuthenticationConstants.UIResponse.BROWSER_CODE_ERROR, resultIntent);
            }

            view.stopLoading();
        } else {
            Logger.info(TAG, "It is pointing to redirect. Final url can be processed to get the code or error.");
            Intent resultIntent = new Intent();
            resultIntent.putExtra(AUTHORIZATION_FINAL_URL, url);
            //TODO log request info
            getCompletionCallback().onChallengeResponseReceived(
                    AuthenticationConstants.UIResponse.BROWSER_CODE_COMPLETE,
                    resultIntent);
            view.stopLoading();
            //the TokenTask should be processed at after the authorization process in the upper calling layer.
        }

        return true;
    }

    private boolean processWebsiteRequest(@NonNull final WebView view, @NonNull final String url) {
        final String methodName = "#processWebsiteRequest";
        final PackageHelper packageHelper = new PackageHelper(getActivity().getPackageManager());
        final Context applicationContext = getActivity().getApplicationContext();
        if (url.startsWith(AuthenticationConstants.Broker.BROWSER_DEVICE_CA_URL)
                && packageHelper.isPackageInstalledAndEnabled(applicationContext, AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME)
                && packageHelper.isPackageInstalledAndEnabled(applicationContext, AuthenticationConstants.Broker.IPPHONE_APP_PACKAGE_NAME)
                && AuthenticationConstants.Broker.IPPHONE_APP_SIGNATURE.equals(
                packageHelper.getCurrentSignatureForPackage(AuthenticationConstants.Broker.IPPHONE_APP_PACKAGE_NAME))) {
            // TODO: This flow should really check if the Microsoft Intune or the Company Portal apps are installed,
            // which is the correct client app to launch depending on the enrollment, and launch that app, to permanently skip the browser.
            // Also it must handle 3rd party MDMs as appropriate, which will depend on the browser flow determining the authority.
            Logger.info(TAG + methodName, "It is a device CA request on IPPhone. Company Portal is installed.");
            try {
                Logger.verbose(TAG + methodName, "Sending intent to launch the CompanyPortal.");
                final Intent intent = new Intent();
                intent.setComponent(new ComponentName(AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME,
                        AuthenticationConstants.Broker.COMPANY_PORTAL_APP_LAUNCH_ACTIVITY_NAME));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                getActivity().startActivity(intent);
            } catch (final SecurityException ex) {
                Logger.warn(TAG + methodName, "Failed to launch Company Portal, falling back to browser.");
                openLinkInBrowser(url);
            }
        } else {
            openLinkInBrowser(url);
        }

        view.stopLoading();
        Intent resultIntent = new Intent();
        getCompletionCallback().onChallengeResponseReceived(AuthenticationConstants.UIResponse.BROWSER_CODE_CANCEL, resultIntent);
        return true;
    }

    private void openLinkInBrowser(final String url) {
        final String methodName = "#openLinkInBrowser";
        Logger.info(TAG + methodName, "Try to open url link in browser");
        final String link = url
                .replace(AuthenticationConstants.Broker.BROWSER_EXT_PREFIX, "https://");
        final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));

        if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
            getActivity().startActivity(intent);
        } else {
            Logger.warn(TAG + methodName, "Unable to find an app to resolve the activity.");
        }
    }

    private boolean processInstallRequest(@NonNull final WebView view, @NonNull final String url) {
        final Intent resultIntent = new Intent();
        final HashMap<String, String> parameters = StringExtensions.getUrlParameters(url);
        final String installLink = parameters.get(AuthenticationConstants.Broker.INSTALL_URL_KEY);
        final String userName = parameters.get(AuthenticationConstants.Broker.INSTALL_UPN_KEY);
        if (TextUtils.isEmpty(installLink)) {
            Logger.info(TAG, "Install link is null or empty, " +
                    "Return to caller with BROWSER_CODE_DEVICE_REGISTER"
            );
            resultIntent.putExtra(AuthenticationConstants.Broker.INSTALL_UPN_KEY, userName);
            getCompletionCallback().onChallengeResponseReceived(
                    AuthenticationConstants.UIResponse.BROWSER_CODE_DEVICE_REGISTER,
                    resultIntent
            );
            view.stopLoading();
            return true;
        }

        Logger.info(TAG, "Return to caller with BROKER_REQUEST_RESUME, and waiting for result.");
        getCompletionCallback().onChallengeResponseReceived(
                AuthenticationConstants.UIResponse.BROKER_REQUEST_RESUME,
                resultIntent
        );

        // Having thread sleep for 1 second for calling activity to receive the result from
        // prepareForBrokerResumeRequest, thus the receiver for listening broker result return
        // can be registered. openLinkInBrowser will launch activity for going to
        // play store and broker app download page which brought the calling activity down
        // in the activity stack.

        final Handler handler = new Handler();
        final int threadSleepForCallingActivity = 1000;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                String link = installLink
                        .replace(AuthenticationConstants.Broker.BROWSER_EXT_PREFIX, "https://");
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                getActivity().startActivity(intent);
                view.stopLoading();
            }
        }, threadSleepForCallingActivity);

        return true;
    }

    private boolean processInvalidUrl(@NonNull final WebView view, @NonNull final String url) {
        if (isBrokerRequest(getActivity().getIntent())
                && url.startsWith(AuthenticationConstants.Broker.REDIRECT_PREFIX)) {
            Logger.error(TAG, "The RedirectUri is not as expected.", null);
            Logger.errorPII(TAG, String.format("Received %s and expected %s", url, mRedirectUrl), null);
            returnError(ErrorStrings.DEVELOPER_REDIRECTURI_INVALID,
                    String.format("The RedirectUri is not as expected. Received %s and expected %s", url,
                            mRedirectUrl));
            view.stopLoading();
            return true;
        }

        if (url.toLowerCase(Locale.US).equals("about:blank")) {
            Logger.verbose(TAG, "It is an blank page request");
            return true;
        }

        if (!url.toLowerCase(Locale.US).startsWith(AuthenticationConstants.Broker.REDIRECT_SSL_PREFIX)) {
            String redactedUrl = "redacted";
            try {
                redactedUrl = StringExtensions.removeQueryParameterFromUrl(url);
            } catch (final URISyntaxException e) {
                // Best effort.
            }

            Logger.error(TAG, "The webView was redirected to an unsafe URL: " + redactedUrl, null);
            returnError(ErrorStrings.WEBVIEW_REDIRECTURL_NOT_SSL_PROTECTED, "The webView was redirected to an unsafe URL.");
            view.stopLoading();
            return true;
        }

        return false;
    }

    private void returnError(final String errorCode, final String errorMessage) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(AuthenticationConstants.Browser.RESPONSE_ERROR_CODE, errorCode);
        resultIntent.putExtra(AuthenticationConstants.Browser.RESPONSE_ERROR_MESSAGE, errorMessage);
        //TODO log request info
        getCompletionCallback().onChallengeResponseReceived(AuthenticationConstants.UIResponse.BROWSER_CODE_ERROR, resultIntent);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onReceivedClientCertRequest(WebView view, final ClientCertRequest clientCertRequest) {
        final ClientCertAuthChallengeHandler clientCertAuthChallengeHandler = new ClientCertAuthChallengeHandler(getActivity());
        clientCertAuthChallengeHandler.processChallenge(clientCertRequest);
    }
}