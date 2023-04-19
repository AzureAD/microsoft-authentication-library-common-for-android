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
import android.webkit.ClientCertRequest;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.internal.broker.PackageHelper;
import com.microsoft.identity.common.internal.ui.webview.certbasedauth.AbstractSmartcardCertBasedAuthChallengeHandler;
import com.microsoft.identity.common.internal.ui.webview.certbasedauth.AbstractCertBasedAuthChallengeHandler;
import com.microsoft.identity.common.internal.ui.webview.certbasedauth.CertBasedAuthFactory;
import com.microsoft.identity.common.java.ui.webview.authorization.IAuthorizationCompletionCallback;
import com.microsoft.identity.common.java.challengehandlers.PKeyAuthChallenge;
import com.microsoft.identity.common.java.challengehandlers.PKeyAuthChallengeFactory;
import com.microsoft.identity.common.internal.ui.webview.challengehandlers.PKeyAuthChallengeHandler;
import com.microsoft.identity.common.java.WarningType;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.exception.ErrorStrings;
import com.microsoft.identity.common.java.providers.RawAuthorizationResult;
import com.microsoft.identity.common.java.util.StringUtil;
import com.microsoft.identity.common.logging.Logger;

import java.net.URISyntaxException;
import java.security.Principal;
import java.util.Locale;
import java.util.Map;

import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.IPPHONE_APP_PACKAGE_NAME;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.IPPHONE_APP_SIGNATURE;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.PLAY_STORE_INSTALL_PREFIX;
import static com.microsoft.identity.common.java.AuthenticationConstants.AAD.APP_LINK_KEY;

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
    private static final String DEVICE_CERT_ISSUER = "CN=MS-Organization-Access";
    private final String mRedirectUrl;
    private final CertBasedAuthFactory mCertBasedAuthFactory;
    private AbstractCertBasedAuthChallengeHandler mCertBasedAuthChallengeHandler;

    public AzureActiveDirectoryWebViewClient(@NonNull final Activity activity,
                                             @NonNull final IAuthorizationCompletionCallback completionCallback,
                                             @NonNull final OnPageLoadedCallback pageLoadedCallback,
                                             @NonNull final String redirectUrl) {
        super(activity, completionCallback, pageLoadedCallback);
        mRedirectUrl = redirectUrl;
        mCertBasedAuthFactory = new CertBasedAuthFactory(activity);
    }

    /**
     * Give the host application a chance to take over the control when a new url is about to be loaded in the current WebView.
     * This method was deprecated in API level 24.
     *
     * @param view The WebView that is initiating the callback.
     * @param url  The url to be loaded.
     * @return return true means the host application handles the url, while return false means the current WebView handles the url.
     */
    // Suppressing deprecation warnings due to deprecated method shouldOverrideUrlLoading. There is already an existing issue for this: https://github.com/AzureAD/microsoft-authentication-library-common-for-android/issues/866
    @SuppressWarnings(WarningType.deprecation_warning)
    @Override
    public boolean shouldOverrideUrlLoading(final WebView view, final String url) {
        if (StringUtil.isNullOrEmpty(url)) {
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

    /**
     * Interpret and take action on a redirect url.
     * This function will return true in every case save 1.  That is, when the URL is none of:
     * <ul><li>A urn containing an authorization challenge (starts with "urn:http-auth:PKeyAuth")</li>
     * <li>A url that starts with the same prefix as the tenant's redirect url</li>
     * <li>An explicit request to open the browser (starts with "browser://")</li>
     * <li>A request to install the auth broker (starts with "msauth://")</li>
     * <li>A request from WebCP (starts with "companyportal://")</li>
     * <li>It is a request that has the intent of starting the broker and the url starts with "browser://"</li>
     * <li>It <strong>does not</strong> begin with "https://".</li></ul>
     *
     * @param view The WebView that is initiating the callback.
     * @param url  The string representation of the url.
     * @return false if we will not take action on the url.
     */
    private boolean handleUrl(final WebView view, final String url) {
        final String methodTag = TAG + ":handleUrl";
        final String formattedURL = url.toLowerCase(Locale.US);

        try {
            if (isPkeyAuthUrl(formattedURL)) {
                Logger.info(methodTag,"WebView detected request for pkeyauth challenge.");
                final PKeyAuthChallengeFactory factory = new PKeyAuthChallengeFactory();
                final PKeyAuthChallenge pKeyAuthChallenge = factory.getPKeyAuthChallengeFromWebViewRedirect(url);
                final PKeyAuthChallengeHandler pKeyAuthChallengeHandler = new PKeyAuthChallengeHandler(view, getCompletionCallback());
                pKeyAuthChallengeHandler.processChallenge(pKeyAuthChallenge);
            } else if (isRedirectUrl(formattedURL)) {
                Logger.info(methodTag,"Navigation starts with the redirect uri.");
                processRedirectUrl(view, url);
            } else if (isWebsiteRequestUrl(formattedURL)) {
                Logger.info(methodTag,"It is an external website request");
                processWebsiteRequest(view, url);
            } else if (isInstallRequestUrl(formattedURL)) {
                Logger.info(methodTag,"It is an install request");
                processInstallRequest(view, url);
            } else if (isWebCpUrl(formattedURL)) {
                Logger.info(methodTag,"It is a request from WebCP");
                processWebCpRequest(view, url);
            } else if (isPlayStoreUrl(formattedURL)) {
                Logger.info(methodTag,"Request to open PlayStore.");
                return processPlayStoreURL(view, url);
            } else if (isAuthAppMFAUrl(formattedURL)) {
                Logger.info(methodTag,"Request to link account with Authenticator.");
                processAuthAppMFAUrl(url);
            } else if (isInvalidRedirectUri(url)) {
                Logger.info(methodTag,"Check for Redirect Uri.");
                processInvalidRedirectUri(view, url);
            } else if (isBlankPageRequest(formattedURL)) {
                Logger.info(methodTag,"It is an blank page request");
            } else if (!isUriSSLProtected(formattedURL)) {
                Logger.info(methodTag,"Check for SSL protection");
                processSSLProtectionCheck(view, url);
            } else {
                Logger.info(methodTag,"This maybe a valid URI, but no special handling for this mentioned URI, hence deferring to WebView for loading.");
                processInvalidUrl(url);
                return false;
            }
        } catch (final ClientException exception) {
            Logger.error(methodTag,exception.getErrorCode(), null);
            Logger.errorPII(methodTag,exception.getMessage(), exception);
            returnError(exception.getErrorCode(), exception.getMessage());
            view.stopLoading();
        }
        return true;
    }

    private boolean isUriSSLProtected(@NonNull final String url) {
        return url.startsWith(AuthenticationConstants.Broker.REDIRECT_SSL_PREFIX);
    }

    private boolean isBlankPageRequest(@NonNull final String url) {
        return "about:blank".equals(url);
    }

    private boolean isInvalidRedirectUri(@NonNull final String url) {
        return isBrokerRequest(getActivity().getIntent())
                && url.startsWith(AuthenticationConstants.Broker.REDIRECT_PREFIX);
    }

    private boolean isAuthAppMFAUrl(@NonNull final String url) {
        return url.startsWith(AuthenticationConstants.Broker.AUTHENTICATOR_MFA_LINKING_PREFIX);
    }

    private boolean isPlayStoreUrl(@NonNull final String url) {
        return url.startsWith(PLAY_STORE_INSTALL_PREFIX);
    }

    private boolean isPkeyAuthUrl(@NonNull final String url) {
        return url.startsWith(AuthenticationConstants.Broker.PKEYAUTH_REDIRECT.toLowerCase(Locale.ROOT));
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

    private boolean isWebCpUrl(@NonNull final String url) {
        return url.startsWith(AuthenticationConstants.Broker.BROWSER_EXT_WEB_CP);
    }

    // This function is only called when the client received a redirect that starts with the apps
    // redirect uri.
    protected void processRedirectUrl(@NonNull final WebView view, @NonNull final String url) {
        final String methodTag = TAG + ":processRedirectUrl";

        Logger.info(methodTag, "It is pointing to redirect. Final url can be processed to get the code or error.");
        final RawAuthorizationResult data = RawAuthorizationResult.fromRedirectUri(url);
        getCompletionCallback().onChallengeResponseReceived(data);
        view.stopLoading();
        //the TokenTask should be processed at after the authorization process in the upper calling layer.
    }

    private void processWebsiteRequest(@NonNull final WebView view, @NonNull final String url) {
        final String methodTag = TAG + ":processWebsiteRequest";

        view.stopLoading();

        if (url.contains(AuthenticationConstants.Broker.BROWSER_DEVICE_CA_URL_QUERY_STRING_PARAMETER)) {
            Logger.info(methodTag, "This is a device CA request.");
            final PackageHelper packageHelper = new PackageHelper(getActivity().getPackageManager());
            final Context applicationContext = getActivity().getApplicationContext();

            // If CP is installed, redirect to CP.
            // TODO: Until we get a signal from eSTS that CP is the MDM app, we cannot assume that.
            //       CP is currently working on this.
            //       Until that comes, we'll only handle this in ipphone.
            if (packageHelper.isPackageInstalledAndEnabled(applicationContext, IPPHONE_APP_PACKAGE_NAME) &&
                    IPPHONE_APP_SIGNATURE.equals(packageHelper.getCurrentSignatureForPackage(IPPHONE_APP_PACKAGE_NAME)) &&
                    packageHelper.isPackageInstalledAndEnabled(applicationContext, COMPANY_PORTAL_APP_PACKAGE_NAME)) {
                try {
                    launchCompanyPortal();
                    return;
                } catch (final Exception ex) {
                    Logger.warn(methodTag, "Failed to launch Company Portal, falling back to browser.");
                }
            }

            // Otherwise, launch in Browser.
            openLinkInBrowser(url);
            returnResult(RawAuthorizationResult.ResultCode.MDM_FLOW);
            return;
        }

        openLinkInBrowser(url);
        returnResult(RawAuthorizationResult.ResultCode.CANCELLED);
    }

    private boolean processPlayStoreURL(@NonNull final WebView view, @NonNull final String url) {
        final String methodTag = TAG + ":processPlayStoreURL";

        view.stopLoading();
        if (!(url.startsWith(PLAY_STORE_INSTALL_PREFIX + COMPANY_PORTAL_APP_PACKAGE_NAME))
                && !(url.startsWith(PLAY_STORE_INSTALL_PREFIX + AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_PACKAGE_NAME))) {
            Logger.info(methodTag, "The URI is either trying to open an unknown application or contains unknown query parameters");
            return false;
        }
        final String appPackageName = (url.contains(COMPANY_PORTAL_APP_PACKAGE_NAME) ?
                COMPANY_PORTAL_APP_PACKAGE_NAME : AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_PACKAGE_NAME);
        Logger.info(methodTag, "Request to open PlayStore to install package : '" + appPackageName + "'");

        try {
            final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_STORE_INSTALL_PREFIX + appPackageName));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            getActivity().startActivity(intent);
        } catch (android.content.ActivityNotFoundException e) {
            //if GooglePlay is not present on the device.
            Logger.error(methodTag, "PlayStore is not present on the device", e);
        }

        return true;
    }

    private void processAuthAppMFAUrl(String url) {
        final String methodTag = TAG + ":processAuthAppMFAUrl";
        Logger.verbose(methodTag, "Linking Account in Broker for MFA.");
        try {
            final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getActivity().startActivity(intent);
        } catch (android.content.ActivityNotFoundException e) {
            Logger.error(methodTag,"Failed to open the Authenticator application.", e);
        }
    }

    private void launchCompanyPortal() {
        final String methodTag = TAG + ":launchCompanyPortal";

        Logger.verbose(methodTag, "Sending intent to launch the CompanyPortal.");
        final Intent intent = new Intent();
        intent.setComponent(new ComponentName(
                COMPANY_PORTAL_APP_PACKAGE_NAME,
                AuthenticationConstants.Broker.COMPANY_PORTAL_APP_LAUNCH_ACTIVITY_NAME));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        getActivity().startActivity(intent);

        returnResult(RawAuthorizationResult.ResultCode.MDM_FLOW);
    }

    private void openLinkInBrowser(final String url) {
        final String methodTag = TAG + ":openLinkInBrowser";
        Logger.info(methodTag, "Try to open url link in browser");
        final String link = url
                .replace(AuthenticationConstants.Broker.BROWSER_EXT_PREFIX, "https://");
        final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));

        if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
            getActivity().startActivity(intent);
        } else {
            Logger.warn(methodTag, "Unable to find an app to resolve the activity.");
        }
    }

    private void processWebCpRequest(@NonNull final WebView view, @NonNull final String url) {

        view.stopLoading();

        if (url.equalsIgnoreCase(AuthenticationConstants.Broker.WEBCP_LAUNCH_COMPANY_PORTAL_URL)) {
            launchCompanyPortal();
            return;
        }

        returnError(ErrorStrings.WEBCP_URI_INVALID,
                "Unexpected URL from WebCP: " + url);
    }

    // i.e. msauth://wpj/?username=idlab1%40msidlab4.onmicrosoft.com&app_link=https%3a%2f%2fplay.google.com%2fstore%2fapps%2fdetails%3fid%3dcom.azure.authenticator%26referrer%3dcom.msft.identity.client.sample.local
    private void processInstallRequest(@NonNull final WebView view, @NonNull final String url) {
        final String methodTag = TAG + ":processInstallRequest";

        final RawAuthorizationResult result = RawAuthorizationResult.fromRedirectUri(url);

        if (result.getResultCode() != RawAuthorizationResult.ResultCode.BROKER_INSTALLATION_TRIGGERED) {
            getCompletionCallback().onChallengeResponseReceived(result);
            view.stopLoading();
            return;
        }

        // Having thread sleep for 1 second for calling activity to receive the result from
        // prepareForBrokerResumeRequest, thus the receiver for listening broker result return
        // can be registered. openLinkInBrowser will launch activity for going to
        // play store and broker app download page which brought the calling activity down
        // in the activity stack.

        final Map<String, String> parameters = StringExtensions.getUrlParameters(url);
        final String appLink = parameters.get(APP_LINK_KEY);

        Logger.info(methodTag,"Launching the link to app:" + appLink);
        getCompletionCallback().onChallengeResponseReceived(result);

        final Handler handler = new Handler();
        final int threadSleepForCallingActivity = 1000;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                String link = appLink
                        .replace(AuthenticationConstants.Broker.BROWSER_EXT_PREFIX, "https://");
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                getActivity().startActivity(intent);
                view.stopLoading();
            }
        }, threadSleepForCallingActivity);

        view.stopLoading();
    }

    private void processInvalidRedirectUri(@NonNull final WebView view,
                                           @NonNull final String url) {
        final String methodTag = TAG + ":processInvalidRedirectUri";

        Logger.error(methodTag, "The RedirectUri is not as expected.", null);
        Logger.errorPII(methodTag,String.format("Received %s and expected %s", url, mRedirectUrl), null);
        returnError(ErrorStrings.DEVELOPER_REDIRECTURI_INVALID,
                String.format("The RedirectUri is not as expected. Received %s and expected %s", url,
                        mRedirectUrl));
        view.stopLoading();
    }

    private void processSSLProtectionCheck(@NonNull final WebView view,
                                           @NonNull final String url) {
        final String methodTag = TAG + ":processSSLProtectionCheck";
        final String redactedUrl = removeQueryParametersOrRedact(url);

        Logger.error(methodTag,"The webView was redirected to an unsafe URL: " + redactedUrl, null);
        returnError(ErrorStrings.WEBVIEW_REDIRECTURL_NOT_SSL_PROTECTED, "The webView was redirected to an unsafe URL.");
        view.stopLoading();
    }

    private void processInvalidUrl(@NonNull final String url) {
        final String methodTag = TAG + ":processInvalidUrl";

        Logger.infoPII(methodTag,"We are declining to override loading and redirect to invalid URL: '"
                + removeQueryParametersOrRedact(url) + "' the user's url pattern is '" + mRedirectUrl + "'");
    }

    private String removeQueryParametersOrRedact(@NonNull final String url) {
        final String methodTag = TAG + ":removeQueryParametersOrRedact";
        try {
            return StringExtensions.removeQueryParameterFromUrl(url);
        } catch (final URISyntaxException e) {
            Logger.errorPII(methodTag,"Redirect URI has invalid syntax, unable to parse", e);
            return "redacted";
        }
    }

    private void returnResult(final RawAuthorizationResult.ResultCode resultCode) {
        //TODO log request info
        getCompletionCallback().onChallengeResponseReceived(
                RawAuthorizationResult.fromResultCode(resultCode));
    }

    private void returnError(final String errorCode, final String errorMessage) {
        //TODO log request info
        getCompletionCallback().onChallengeResponseReceived(
                RawAuthorizationResult.fromException(new ClientException(errorCode, errorMessage)));
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onReceivedClientCertRequest(@NonNull final WebView view,
                                            @NonNull final ClientCertRequest clientCertRequest) {
        final String methodTag = TAG + ":onReceivedClientCertRequest";
        // When server sends null or empty issuers, we'll continue with CBA.
        // In the case where ADFS sends a clientTLS device auth request, we don't handle that in CBA.
        // This type of request will have a particular issuer, so if that issuer is found, we will
        //  immediately cancel the ClientCertRequest.
        final Principal[] acceptableCertIssuers = clientCertRequest.getPrincipals();
        if (acceptableCertIssuers != null) {
            for (final Principal issuer : acceptableCertIssuers) {
                if (issuer.getName().contains(DEVICE_CERT_ISSUER)) {
                    final String message = "Cancelling the TLS request, not responding to TLS challenge triggered by device authentication.";
                    Logger.info(methodTag, message);
                    clientCertRequest.cancel();
                    return;
                }
            }
        }

        if (mCertBasedAuthChallengeHandler != null) {
            mCertBasedAuthChallengeHandler.cleanUp();
        }
        mCertBasedAuthFactory.createCertBasedAuthChallengeHandler(new CertBasedAuthFactory.CertBasedAuthChallengeHandlerCallback() {
            @Override
            public void onReceived(@Nullable final AbstractCertBasedAuthChallengeHandler challengeHandler) {
                mCertBasedAuthChallengeHandler = challengeHandler;
                if (mCertBasedAuthChallengeHandler == null) {
                    //User cancelled out of CBA.
                    clientCertRequest.cancel();
                    return;
                }
                mCertBasedAuthChallengeHandler.processChallenge(clientCertRequest);
            }
        });
    }

    /**
     * Cleanup to be done when host activity is being destroyed.
     */
    public void onDestroy() {
        if (mCertBasedAuthChallengeHandler != null) {
            mCertBasedAuthChallengeHandler.cleanUp();
        }
        mCertBasedAuthFactory.onDestroy();
    }

    /**
     * Call methods to be run before sending auth results.
     * @param response {@link RawAuthorizationResult}
     * @param callback {@link ISendResultCallback}
     */
    public void finalizeBeforeSendingResult(@NonNull final RawAuthorizationResult response,
                                            @NonNull final ISendResultCallback callback) {
        if (mCertBasedAuthChallengeHandler == null) {
            callback.onResultReady();
            return;
        }
        //The challenge handler checks if CBA was proceeded with and emits telemetry.
        mCertBasedAuthChallengeHandler.emitTelemetryForCertBasedAuthResults(response);
        if (!(mCertBasedAuthChallengeHandler instanceof AbstractSmartcardCertBasedAuthChallengeHandler)) {
            callback.onResultReady();
            return;
        }
        //The challenge handler will make sure no smartcard is connected before result is sent.
        ((AbstractSmartcardCertBasedAuthChallengeHandler<?>)mCertBasedAuthChallengeHandler).promptSmartcardRemovalForResult(callback);
    }

}
