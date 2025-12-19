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
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.webkit.ClientCertRequest;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.ViewTreeLifecycleOwner;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.internal.broker.BrokerData;
import com.microsoft.identity.common.internal.broker.AuthUxJavaScriptInterface;
import com.microsoft.identity.common.internal.broker.PackageHelper;
import com.microsoft.identity.common.internal.fido.CredManFidoManager;
import com.microsoft.identity.common.internal.fido.FidoChallenge;
import com.microsoft.identity.common.internal.fido.AuthFidoChallengeHandler;
import com.microsoft.identity.common.internal.fido.IFidoManager;
import com.microsoft.identity.common.internal.fido.LegacyFido2ApiManager;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationActivity;
import com.microsoft.identity.common.internal.providers.oauth2.PasskeyOriginRulesManager;
import com.microsoft.identity.common.internal.providers.oauth2.WebViewAuthorizationFragment;
import com.microsoft.identity.common.internal.ui.webview.certbasedauth.AbstractSmartcardCertBasedAuthChallengeHandler;
import com.microsoft.identity.common.internal.ui.webview.certbasedauth.AbstractCertBasedAuthChallengeHandler;
import com.microsoft.identity.common.internal.ui.webview.certbasedauth.CertBasedAuthFactory;
import com.microsoft.identity.common.internal.ui.webview.challengehandlers.ReAttachPrtHeaderHandler;
import com.microsoft.identity.common.internal.ui.webview.challengehandlers.SwitchBrowserChallenge;
import com.microsoft.identity.common.internal.ui.webview.challengehandlers.SwitchBrowserRequestHandler;
import com.microsoft.identity.common.internal.ui.webview.challengehandlers.NonceRedirectHandler;
import com.microsoft.identity.common.java.authorities.Authority;
import com.microsoft.identity.common.java.broker.CommonTenantInfoProvider;
import com.microsoft.identity.common.java.constants.FidoConstants;
import com.microsoft.identity.common.java.exception.IErrorInformation;
import com.microsoft.identity.common.java.flighting.CommonFlight;
import com.microsoft.identity.common.java.flighting.CommonFlightsManager;
import com.microsoft.identity.common.java.opentelemetry.AttributeName;
import com.microsoft.identity.common.java.opentelemetry.BaggageExtension;
import com.microsoft.identity.common.java.opentelemetry.OTelUtility;
import com.microsoft.identity.common.java.opentelemetry.SpanExtension;
import com.microsoft.identity.common.java.opentelemetry.SpanName;
import com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.AzureActiveDirectory;
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

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.Principal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.AMAZON_APP_REDIRECT_PREFIX;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_PACKAGE_NAME;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.IPPHONE_APP_PACKAGE_NAME;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.IPPHONE_APP_SHA512_RELEASE_SIGNATURE;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.PLAY_STORE_INSTALL_APP_PREFIX;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.PLAY_STORE_INSTALL_PREFIX;
import static com.microsoft.identity.common.java.AuthenticationConstants.AAD.APP_LINK_KEY;
import static com.microsoft.identity.common.java.exception.ClientException.UNKNOWN_ERROR;
import static com.microsoft.identity.common.java.flighting.CommonFlight.ENABLE_PLAYSTORE_URL_LAUNCH;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;

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
    public static final String ERROR_DESCRIPTION = "error_description";
    private static final String DEVICE_CERT_ISSUER = "CN=MS-Organization-Access";
    // 3 secs wait for the intent to be launched and the current flow is killed for smooth transition.
    private static final int THREAD_SLEEP_FOR_INTENT_LAUNCH_MS = 3;
    private final String mRedirectUrl;
    private final CertBasedAuthFactory mCertBasedAuthFactory;
    private AbstractCertBasedAuthChallengeHandler mCertBasedAuthChallengeHandler;
    private final SwitchBrowserRequestHandler mSwitchBrowserRequestHandler;
    private HashMap<String, String> mRequestHeaders;
    private String mRequestUrl;
    private boolean mInWebCpFlow = false;
    private boolean mAuthUxJavaScriptInterfaceAdded = false;
    // Determines whether to handle WebCP requests in the WebView in brokerless scenarios.
    private final boolean mIsWebViewWebCpEnabledInBrokerlessCase;
    private final SpanContext mSpanContext;
    private final String mUtid;

    private String mPasskeyRegistrationScript;

    public AzureActiveDirectoryWebViewClient(@NonNull final Activity activity,
                                             @NonNull final IAuthorizationCompletionCallback completionCallback,
                                             @NonNull final OnPageLoadedCallback pageLoadedCallback,
                                             @NonNull final String redirectUrl,
                                             @NonNull final SwitchBrowserRequestHandler switchBrowserRequestHandler,
                                             @Nullable final String utid,
                                             final boolean isWebViewWebCpEnabledInBrokerlessCase) {
        super(activity, completionCallback, pageLoadedCallback);
        mRedirectUrl = redirectUrl;
        mCertBasedAuthFactory = new CertBasedAuthFactory(activity);
        mSwitchBrowserRequestHandler = switchBrowserRequestHandler;
        mUtid = utid;
        mSpanContext = activity instanceof AuthorizationActivity ? ((AuthorizationActivity) getActivity()).getSpanContext() : null;
        mIsWebViewWebCpEnabledInBrokerlessCase = isWebViewWebCpEnabledInBrokerlessCase;
    }

    /**
     * This method is used to initialize the JavaScript API for the AuthUx JavaScript interface.
     * It checks if the current process is running on the AuthService and if the URL is valid for the interface.
     * If both conditions are met, it adds the JavaScript interface to the WebView.
     */
    public void initializeAuthUxJavaScriptApi(@NonNull final WebView view, final String url) {
        if (shouldExposeJavaScriptInterface(url)) {
            // If broker request, and a valid url, expose JavaScript API
            Logger.info(TAG, "Adding AuthUx JavaScript Interface");
            view.addJavascriptInterface(new AuthUxJavaScriptInterface(), AuthUxJavaScriptInterface.Companion.getInterfaceName());
            mAuthUxJavaScriptInterfaceAdded = true;
        }
    }

    @Override
    public void onPageFinished(final WebView view,
                               final String url) {
        super.onPageFinished(view, url);

        if (mAuthUxJavaScriptInterfaceAdded) {
            // Add a function to the api. Must do this to first stringify the dict object, as Android @JavaScriptInterface does not support
            // passing dict objects through Javascript APIs, only Strings and primitive types. Server side will be sending message in a dict
            String jsScript = "window." + AuthUxJavaScriptInterface.Companion.getInterfaceName() + ".postMessageToBroker = function(message) { " +
                    "    window." + AuthUxJavaScriptInterface.Companion.getInterfaceName() + ".receiveAuthUxMessage(JSON.stringify(message)); " +
                    "};";

            view.evaluateJavascript(jsScript, null);
        }
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

    public void setRequestHeaders(final HashMap<String, String> requestHeaders) {
        mRequestHeaders = requestHeaders;
    }

    public void setRequestUrl(final String requestUrl) {
        mRequestUrl = requestUrl;
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
            } else if (isPasskeyUrl(formattedURL)) {
                Logger.info(methodTag,"WebView detected request for passkey protocol.");
                final FidoChallenge challenge = FidoChallenge.createFromRedirectUri(url);
                final Activity currentActivity = getActivity();
                final Context oTelContext = currentActivity instanceof AuthorizationActivity ? ((AuthorizationActivity)currentActivity).getOtelContext() : null;
                // The legacyManager should only be getting added if the device is on Android 13 or lower and the library is MSAL/OneAuth with fragment or dialog mode.
                // The legacyManager logic should be removed once a larger majority of users are on Android 14+.
                final IFidoManager legacyManager =
                        currentActivity instanceof AuthorizationActivity
                                && ((AuthorizationActivity) currentActivity).getFragment() instanceof WebViewAuthorizationFragment
                                && CommonFlightsManager.INSTANCE.getFlightsProvider().isFlightEnabled(CommonFlight.ENABLE_LEGACY_FIDO_SECURITY_KEY_LOGIC)
                                && Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                                ? new LegacyFido2ApiManager(view.getContext(), (WebViewAuthorizationFragment)((AuthorizationActivity)currentActivity).getFragment())
                                : null;
                final AuthFidoChallengeHandler challengeHandler = new AuthFidoChallengeHandler(
                        new CredManFidoManager(
                                view.getContext(),
                                legacyManager
                        ),
                        view,
                        oTelContext,
                        ViewTreeLifecycleOwner.get(view));
                challengeHandler.processChallenge(challenge);
            } else if (CommonFlightsManager.INSTANCE.getFlightsProvider().isFlightEnabled(CommonFlight.ENABLE_ATTACH_NEW_PRT_HEADER_WHEN_NONCE_EXPIRED) && isNonceRedirect(formattedURL)) {
                Logger.info(methodTag,"Navigation contains new nonce within the redirect uri.");
                processNonceAndReAttachHeaders(view, url);
             }
             else if (isRedirectUrl(formattedURL)) {
                Logger.info(methodTag,"Navigation starts with the redirect uri.");
                if (mSwitchBrowserRequestHandler.isSwitchBrowserRequest(formattedURL, mRedirectUrl)) {
                    Logger.info(methodTag,"Request to switch browser.");
                    processSwitchBrowserRequest(url);
                } else {
                    Logger.info(methodTag,"It is a redirect request.");
                    processRedirectUrl(view, url);
                }
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
            } else if(CommonFlightsManager.INSTANCE.getFlightsProvider().isFlightEnabled(ENABLE_PLAYSTORE_URL_LAUNCH) && isPlaystoreUrlToInstallBrokerApp(formattedURL)) {
                Logger.info(methodTag,"Request to open PlayStore for broker app install.");
                return processPlayStoreURLForBrokerApps(view, url);
            } else if (isAuthAppMFAUrl(formattedURL)) {
                Logger.info(methodTag,"Request to link account with Authenticator.");
                processAuthAppMFAUrl(url);
            } else if (isAmazonAppRedirect(formattedURL)) {
                Logger.info(methodTag, "It is an Amazon app request");
                processAmazonAppUri(url);
            } else if (isInvalidRedirectUri(url)) {
                Logger.info(methodTag,"Check for Redirect Uri.");
                processInvalidRedirectUri(view, url);
            } else if (isBlankPageRequest(formattedURL)) {
                Logger.info(methodTag,"It is an blank page request");
            } else if (isIntentRequestToInstallBrokerApp(formattedURL)) {
                 Logger.info(methodTag, "It is an intent request");
                // Intent URI format is case sensitive, so we need to provide the original URI.
                processIntentToInstallBrokerApp(view, url);
            } else if (!isUriSSLProtected(formattedURL)) {
                Logger.info(methodTag,"Check for SSL protection");
                processSSLProtectionCheck(view, url);
            } else if (isHeaderForwardingRequiredUri(url)) {
                processHeaderForwardingRequiredUri(view, url);
            } else if (CommonFlightsManager.INSTANCE.getFlightsProvider().isFlightEnabled(CommonFlight.ENABLE_ATTACH_PRT_HEADER_WHEN_CROSS_CLOUD) && isCrossCloudRedirect(formattedURL)) {
                Logger.info(methodTag,"Navigation contains cross cloud redirect.");
                processCrossCloudRedirect(view, url);
            } else if (mInWebCpFlow && isWebCpEnrollmentUrl(url)) {
                Logger.info(methodTag,"Navigation contains web cp enrollment url.");
                processWebCpEnrollmentUrl(view, url);
            } else if (mInWebCpFlow && isWebCpAuthorizeUrl(url)) {
                processWebCpAuthorize(view, url);
            }  else if (isDeviceCaRequest(url) && isHttpsScheme(url) && isWebCpInWebviewFeatureEnabled(url)) {
                // Special handling for device CA requests due to a corner case in eSTS for webapps/confidential clients, which should be handled by the WebView.
                Logger.info(methodTag, "Navigation contains device CA request with https scheme.");
                processDeviceCaRequest(view, url);
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

    private boolean isPlaystoreUrlToInstallBrokerApp(@NonNull final String url) {
        if (url.startsWith(PLAY_STORE_INSTALL_APP_PREFIX)) {
            // Check if the url query parameter is for a broker app.
            for (final BrokerData brokerData : BrokerData.getAllBrokers()) {
                if (url.contains("id=" + brokerData.getPackageName())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isPkeyAuthUrl(@NonNull final String url) {
        return url.startsWith(AuthenticationConstants.Broker.PKEYAUTH_REDIRECT.toLowerCase(Locale.ROOT));
    }

    private boolean isPasskeyUrl(@NonNull final String url) {
        return url.startsWith(FidoConstants.PASSKEY_PROTOCOL_REDIRECT.toLowerCase(Locale.ROOT));
    }

    private boolean isRedirectUrl(@NonNull final String url) {
        return url.startsWith(mRedirectUrl.toLowerCase(Locale.US));
    }

    private boolean isNonceRedirect(@NonNull final String url) {
        return url.contains(AuthenticationConstants.Broker.SSO_NONCE_PARAMETER);
    }

    /**
     * Determines if the provided URL is a valid request to install a broker app.
     * <p>
     * This method checks if the URL starts with the intent prefix, is targeting the Google Play Store app,
     * and is associated with a broker app. It ensures that only valid intent requests are processed.
     *
     * @param url The URL to evaluate.
     * @return {@code true} if the URL is a permitted intent request, {@code false} otherwise.
     */
    private boolean isIntentRequestToInstallBrokerApp(@NonNull final String url) {
        // Check if the URL is an intent request
        if (!url.startsWith(AuthenticationConstants.Broker.INTENT_PREFIX)) {
            return false;
        }
        // Check if the intent request is for the google play store app
        if (!url.contains(";package=com.android.vending;")) {
            return false;
        }
        // Check if the url query parameter is for a broker app.
        for (final BrokerData brokerData : BrokerData.getAllBrokers()) {
            if (url.contains("id=" + brokerData.getPackageName())) {
                return true;
            }
        }
        return false;
    }

    private boolean isCrossCloudRedirect(@NonNull final String url) {
        try {
            final URL currentUrl = new URL(url);
            final URL startUrl = new URL(mRequestUrl);
            if (AzureActiveDirectory.isValidCloudHost(currentUrl) && AzureActiveDirectory.isValidCloudHost(startUrl)) {
                final Authority startUrlAuthority = Authority.getAuthorityFromAuthorityUrl(mRequestUrl);
                final Authority currentLoadedUrlAuthority = Authority.getAuthorityFromAuthorityUrl(url);
                if (!startUrlAuthority.getAuthorityURL().getHost().equalsIgnoreCase(currentLoadedUrlAuthority.getAuthorityURL().getHost())) {
                    Logger.info(TAG, "Detected a cross cloud redirect.");
                    return true;
                }
            }
        } catch (final Throwable throwable) {
            // No op. If it fails, let it fail silently because this is not blocking the user.
            Logger.warn(TAG, "Failure in detecting if it is a cross cloud redirect url." + throwable);
        }
        return false;
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

    private boolean isAmazonAppRedirect(@NonNull final String url) {
        return url.startsWith(AMAZON_APP_REDIRECT_PREFIX);
    }

    private boolean isWebCpEnrollmentUrl(@NonNull final String url) {
        return url.startsWith(AuthenticationConstants.Broker.WEBCP_ENROLLMENT_URL);
    }

    private boolean isWebCpAuthorizeUrl(@NonNull final String url) {
        // URL should be for authorize request for webcp.
        final String methodTag = TAG + ":isWebCpAuthorizeUrl";
        try {
            final URI uri = new URI(url);
            final String host = uri.getHost();
            final String path = uri.getPath();

            if (host == null || path == null) {
                Logger.verbose(methodTag, "URL missing host or path");
                return false;
            }

            if (!AzureActiveDirectory.isValidCloudHost(new URL(url))) {
                Logger.info(methodTag, "URL host is not a valid Azure cloud host");
                return false;
            }

            if (!path.contains("/authorize")) {
                Logger.info(methodTag, "URL path does not contain /authorize");
                return false;
            }

            final Map<String, String> queryParams = StringExtensions.getUrlParameters(url);
            final String clientId = queryParams.get(AuthenticationConstants.OAuth2.CLIENT_ID);

            if (StringUtil.isNullOrEmpty(clientId)) {
                Logger.info(methodTag, "Authorize URL does not contain client_id");
                return false;
            }

            final boolean isWebCpClient = AuthenticationConstants.Broker.WEBCP_CLIENT_ID.equalsIgnoreCase(clientId);
            Logger.info(methodTag, isWebCpClient
                    ? "WebCP authorize URL contains valid WebCP client_id."
                    : "Not running WebCP flow as client_id in authorize is not webcp client_id");
            return isWebCpClient;
        } catch (final URISyntaxException | MalformedURLException e) {
            Logger.info(methodTag, "Invalid URL: " + e.getMessage());
            return false;
        }
    }

    private boolean isHeaderForwardingRequiredUri(@NonNull final String url) {
        // MSAL makes MSA requests first to login.microsoftonline.com, and then gets redirected to login.live.com.
        // This drops all the headers, which can have credentials useful for SSO and correlationIds useful for
        // investigations.
        // Old Chromium versions <88 did this behavior by default, but it was removed in more recent versions.
        // For now, reproduce this only for MSA, and consider adding more trusted ESTS endpoints in the future.
        final boolean urlIsTrustedToReceiveHeaders =  url.startsWith("https://login.live.com/");
        final boolean originalRequestHasHeaders = mRequestHeaders != null && !mRequestHeaders.isEmpty();
        return urlIsTrustedToReceiveHeaders && originalRequestHasHeaders;
    }

    // This function is only called when the client received a redirect that starts with the apps
    // redirect uri.
    private void processRedirectUrl(@NonNull final WebView view, @NonNull final String url) {
        final String methodTag = TAG + ":processRedirectUrl";

        Logger.info(methodTag, "It is pointing to redirect. Final url can be processed to get the code or error.");
        final RawAuthorizationResult data = RawAuthorizationResult.fromRedirectUri(url);
        getCompletionCallback().onChallengeResponseReceived(data);
        view.stopLoading();
        //the TokenTask should be processed at after the authorization process in the upper calling layer.
    }

    /**
     * Launch the browser with the given action URI and code.
     * <p>
     * From the query parameters, extract the action URI and code,
     * The constructs the URI with the action URI and code.
     *
     * @param url The URL to be opened in the browser.
     */
    private void processSwitchBrowserRequest(@NonNull final String url) {
        final String methodTag = TAG + ":processSwitchBrowserRequest";
        try {
            mSwitchBrowserRequestHandler.processChallenge(
                    SwitchBrowserChallenge.constructFromRedirectUrl(url, mRequestUrl)
            );
        } catch (final Throwable throwable) {
            Logger.error(methodTag, "Switch browser challenge could not be processed.", throwable);
            final String errorCode;
            if (throwable instanceof IErrorInformation) {
                errorCode = ((IErrorInformation) throwable).getErrorCode();
            } else {
                errorCode = UNKNOWN_ERROR;
            }
            returnError(errorCode, throwable.getMessage());
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    protected void processWebsiteRequest(@NonNull final WebView view, @NonNull final String url) {
        final String methodTag = TAG + ":processWebsiteRequest";
        view.stopLoading();
        final Span span = createSpanWithAttributesFromParent(SpanName.ProcessWebsiteRequest.name());
        span.setAttribute(AttributeName.is_in_web_cp_flow.name(), mInWebCpFlow);
        try (final Scope scope = SpanExtension.makeCurrentSpan(span)) {
            if (isDeviceCaRequest(url)) {
                processDeviceCaRequest(view, url);
                span.setStatus(StatusCode.OK);
                return;
            }

            if (isRedirectToPlaystoreToInstallCp(url) && mInWebCpFlow) {
                handlePlaystoreLaunchUrlFromWebCp(url);
                span.setStatus(StatusCode.OK);
                return;
            }

            // Default case: redirect to browser
            handleBrowserRedirect(methodTag, url);
            span.setStatus(StatusCode.OK);
        } catch (final Throwable throwable) {
            Logger.error(methodTag, "Failed to open link in browser.", throwable);
            span.recordException(throwable);
            span.setStatus(StatusCode.ERROR);
            returnError(ErrorStrings.UNEXPECTED_ERROR, "No browser found to open the link.");
        } finally {
            span.end();
        }
    }

    /**
     * Handles the default browser redirect case for website requests.
     * @param methodTag The method tag for logging
     * @param url The URL to open in browser
     */
    private void handleBrowserRedirect(@NonNull final String methodTag, @NonNull final String url) {
        Logger.info(methodTag, "Not a device CA request. Redirecting to browser.");
        openLinkInBrowser(url);
        final RawAuthorizationResult.ResultCode resultCode = mInWebCpFlow
                ? RawAuthorizationResult.ResultCode.MDM_FLOW
                : RawAuthorizationResult.ResultCode.CANCELLED;

        Logger.info(methodTag, "Returning result code: " + resultCode);
        returnResult(resultCode);
    }

    // Handles Playstore launch URLs originating from WebCP, specifically for installing the Company Portal app.
    private void handlePlaystoreLaunchUrlFromWebCp(@NonNull final String url) {
        final String methodTag = TAG + ":handlePlaystoreLaunchUrlFromWebCp";
        Logger.info(methodTag, "Handling playstore launch URL from WebCP.");
        SpanExtension.current().setAttribute(AttributeName.is_redirect_to_playstore_launch_from_webcp.name(), true);
        openLinkInBrowser(url);
        returnResult(RawAuthorizationResult.ResultCode.MDM_FLOW);
    }

    /**
     * Checks if it is a redirect to Playstore to install Company Portal app.
     * @param url The URL to check.
     * @return true if it is a redirect to Playstore to install Company Portal app.
     */
    private boolean isRedirectToPlaystoreToInstallCp(@NonNull final String url) {
       if (url.contains(PLAY_STORE_INSTALL_APP_PREFIX + COMPANY_PORTAL_APP_PACKAGE_NAME)) {
           Logger.info(TAG, "Redirect to Playstore to install Company Portal app.");
           return true;
       }
       return false;
    }

    /**
     * Processed device CA requests detected in the web flow.
     * @param view The {@link WebView} instance in which the request originated.
     * @param url  The URL representing the device CA request.
     */
    private void processDeviceCaRequest(@NonNull final WebView view, @NonNull final String url) {
        final String methodTag = TAG + ":processDeviceCaRequest";
        Logger.info(methodTag, "This is a device CA request.");

        if (shouldLaunchCompanyPortal()) {
            // If CP is installed, redirect to CP.
            // TODO: Until we get a signal from eSTS that CP is the MDM app, we cannot assume that.
            //       CP is currently working on this.
            //       Until that comes, we'll only handle this in ipphone.
            try {
                launchCompanyPortal();
                return;
            } catch (final Exception ex) {
                Logger.warn(methodTag, "Failed to launch Company Portal, falling back to browser.");
            }
        }

        loadDeviceCaUrl(url, view);
    }

    private boolean isDeviceCaRequest(@NonNull final String url) {
        return url.contains(AuthenticationConstants.Broker.BROWSER_DEVICE_CA_URL_QUERY_STRING_PARAMETER);
    }

    private boolean isHttpsScheme(@NonNull final String url) {
        return url.startsWith(AuthenticationConstants.Broker.HTTPS_SCHEME);
    }

    // Decides whether to launch the Company Portal app based on the presence of the IPPhone app and its signature.
    private boolean shouldLaunchCompanyPortal() {
        final PackageHelper packageHelper = new PackageHelper(getActivity().getPackageManager());
        return packageHelper.isPackageInstalledAndEnabled(IPPHONE_APP_PACKAGE_NAME)
                && IPPHONE_APP_SHA512_RELEASE_SIGNATURE.equals(packageHelper.getSha512SignatureForPackage(IPPHONE_APP_PACKAGE_NAME))
                && packageHelper.isPackageInstalledAndEnabled(COMPANY_PORTAL_APP_PACKAGE_NAME);
    }

    // Loads the device CA URL in the WebView if the flight is enabled, otherwise opens it in the browser.
    @VisibleForTesting
    protected void loadDeviceCaUrl(@NonNull final String originalUrl, @NonNull final WebView view) {
        final String methodTag = TAG + ":loadDeviceCaUrl";
        final Span span = createSpanWithAttributesFromParent(SpanName.ProcessWebCpRedirects.name());
        try (final Scope scope = SpanExtension.makeCurrentSpan(span)) {
            if (isWebCpInWebviewFeatureEnabled(originalUrl)) {
                Logger.info(methodTag, "Loading device CA request in WebView.");
                span.setAttribute(AttributeName.is_webcp_in_webview_enabled.name(), true);
                String httpsUrl = originalUrl.replace(AuthenticationConstants.Broker.BROWSER_EXT_PREFIX, "https://");
                view.loadUrl(httpsUrl, mRequestHeaders);
            } else {
                Logger.info(methodTag, "Loading device CA request in browser.");
                span.setAttribute(AttributeName.is_webcp_in_webview_enabled.name(), false);
                openLinkInBrowser(originalUrl);
                returnResult(RawAuthorizationResult.ResultCode.MDM_FLOW);
            }
            span.setStatus(StatusCode.OK);
        } catch (final Throwable throwable) {
            Logger.error(methodTag, "Failed to load device CA URL in WebView.", throwable);
            span.recordException(throwable);
            span.setStatus(StatusCode.ERROR);
            returnError(UNKNOWN_ERROR, throwable.getMessage());
        } finally {
            span.end();
        }
    }

    // Method to decide if the WebView should load the WebCP URL based on the flights.
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    protected boolean isWebCpInWebviewFeatureEnabled(@NonNull final String originalUrl) {
        final String methodTag = TAG + ":isWebCpInWebviewFeatureEnabled";
        try {
            if (!ProcessUtil.isRunningOnAuthService(getActivity().getApplicationContext())) {
                mInWebCpFlow = mIsWebViewWebCpEnabledInBrokerlessCase;
                Logger.info(methodTag, "Not running on AuthService, WebCP in WebView feature enabled? "+ mIsWebViewWebCpEnabledInBrokerlessCase);
                return mInWebCpFlow;
            }

            final String homeTenantId = !StringUtil.isNullOrEmpty(mUtid)? mUtid : getHomeTenantIdFromUrl(originalUrl);
            if (StringUtil.isNullOrEmpty(homeTenantId)) {
                Logger.info(methodTag, "Home tenantId is empty");
                return false;
            }

            final long webCpGetFlightStartTime = System.currentTimeMillis();
            final int waitForFlightsTimeOut = CommonFlightsManager.INSTANCE.getFlightsProvider().getIntValue(CommonFlight.WEB_CP_WAIT_TIMEOUT_FOR_FLIGHTS);
            final boolean isWebCpFlightEnabled = CommonFlightsManager.INSTANCE.getFlightsProviderForTenant(homeTenantId, waitForFlightsTimeOut).isFlightEnabled(CommonFlight.ENABLE_WEB_CP_IN_WEBVIEW);
            SpanExtension.current().setAttribute(AttributeName.web_cp_flight_get_time.name(), (System.currentTimeMillis() - webCpGetFlightStartTime));
            SpanExtension.current().setAttribute(AttributeName.tenant_id.name(), homeTenantId);
            if (isWebCpFlightEnabled) {
                // Directly enabled via flight rollout.
                Logger.info(methodTag, "WebCP in WebView feature is enabled.");
                mInWebCpFlow = true;
                return true;
            }

            return false;
        } catch (final Throwable throwable) {
            // Catching any unexpected exceptions to avoid breaking the flow. We will anyway remove this block once the feature is fully rolled out.
            Logger.error(methodTag, "Failed to check if WebCP in WebView feature is enabled.", throwable);
            return false;
        }
    }

    private String getHomeTenantIdFromUrl(@NonNull final String url) {
        final String username = StringExtensions.getUrlParameters(url).get(AuthenticationConstants.AAD.LOGIN_HINT);
        if (StringUtil.isNullOrEmpty(username)) {
            return null;
        }
        return CommonTenantInfoProvider.INSTANCE.getHomeTenantId(username);
    }

    // WebCP enrollment URL is a guided enrollment page showing instructions on how to enroll within the productivity app.
    // This is a special case where the enrollment is not done in the WebView, but rather in the browser.
    private void processWebCpEnrollmentUrl(@NonNull final WebView view, @NonNull final String url) {
        final String methodTag = TAG + ":processWebCpEnrollmentUrl";
        final Span span = createSpanWithAttributesFromParent(SpanName.ProcessWebCpEnrollmentRedirect.name());
        try (final Scope scope = SpanExtension.makeCurrentSpan(span)) {
            view.stopLoading();
            Logger.info(methodTag, "Loading WebCP enrollment url in browser.");
            // This is a WebCP enrollment URL, so we need to open it in the browser (it does not work in WebView as google enrollment is enforced to be done in browser).
            openGoogleEnrollmentUrl(url);
            // We need to return MDM_FLOW result code as the enrollment is done in browser. But this may sometimes take a few seconds to launch the intent due to slowness on the device.
            // So we will wait for a few seconds before returning the result so that the current page in webview does not get closed immediately.
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    returnResult(RawAuthorizationResult.ResultCode.MDM_FLOW);
                }
            }, TimeUnit.SECONDS.toMillis(THREAD_SLEEP_FOR_INTENT_LAUNCH_MS));
            span.setStatus(StatusCode.OK);
        } catch (final Throwable throwable) {
            Logger.error(methodTag, "Failed to process WebCP enrollment URL.", throwable);
            span.recordException(throwable);
            span.setStatus(StatusCode.ERROR);
            returnError(UNKNOWN_ERROR, throwable.getMessage());
        } finally {
            span.end();
        }
    }

    // Opens the Google enrollment URL in the browser or the default intent handler (like DPC)
    private void openGoogleEnrollmentUrl(@NonNull final String url) {
        final String methodTag = TAG + ":openGoogleEnrollmentUrl";
        Logger.info(methodTag, "Opening Google enrollment URL");
        try {
            final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            // Add flags to ensure the activity is launched in a new task and clears the current task stack.
            // Important for the enrollment flow to work correctly.
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            getActivity().startActivity(intent);
        } catch (final ActivityNotFoundException e) {
            Logger.error(methodTag,"Failed to open the intent for google enrollment.", e);
            throw e;
        }
    }

    private boolean processPlayStoreURL(@NonNull final WebView view, @NonNull final String url) {
        final String methodTag = TAG + ":processPlayStoreURL";

        view.stopLoading();
        if (!(url.startsWith(PLAY_STORE_INSTALL_PREFIX + COMPANY_PORTAL_APP_PACKAGE_NAME))
                && !(url.startsWith(PLAY_STORE_INSTALL_PREFIX + AZURE_AUTHENTICATOR_APP_PACKAGE_NAME))) {
            Logger.info(methodTag, "The URI is either trying to open an unknown application or contains unknown query parameters");
            return false;
        }
        final String appPackageName = getBrokerAppPackageNameFromUrl(url);
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

    // This method processes the Play Store URL for broker apps. We check if the URL is for installing the Company Portal app or the Azure Authenticator app.
    private boolean processPlayStoreURLForBrokerApps(@NonNull final WebView view, @NonNull final String url) {
        final String methodTag = TAG + ":processPlayStoreURL";

        final String appPackageName = getBrokerAppPackageNameFromUrl(url);
        Logger.info(methodTag, "Request to open PlayStore to install package : '" + appPackageName + "'");

        try {
            final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_STORE_INSTALL_APP_PREFIX + appPackageName));
            getActivity().startActivity(intent);
            view.stopLoading();
            return true;
        } catch (final ActivityNotFoundException e) {
            //if GooglePlay is not present on the device.
            Logger.error(methodTag, "PlayStore is not present on the device", e);
        } catch(final Exception e) {
            Logger.error(methodTag, "Failed to intercept install broker playstore URL and launch the intent", e);
        }

        return false;
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

    private void processAmazonAppUri(@NonNull final String url) {
        final String methodTag = TAG + ":processAmazonAppUri";

        final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        getActivity().startActivity(intent);
        Logger.info(methodTag, "Sent Intent to launch Amazon app");
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    protected void openLinkInBrowser(final String url) {
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
        Logger.errorPII(methodTag, String.format("Received %s and expected %s", url, mRedirectUrl), null);
        returnError(ErrorStrings.DEVELOPER_REDIRECTURI_INVALID,
                String.format("The RedirectUri is not as expected. Received %s and expected %s", url,
                        mRedirectUrl));
        view.stopLoading();
    }

    /**
     * This method is used to process the intent to install the broker app.
     * It parses the intent URI and starts the activity if the package name is valid.
     *
     * @param view The WebView that will be used to open the URL.
     * @param intentUrl  The URL to be opened.
     */
    private void processIntentToInstallBrokerApp(@NonNull final WebView view, @NonNull final String intentUrl) {
        final String methodTag = TAG + ":processIntentToInstallBrokerApp";
        try {
            final Intent intent = Intent.parseUri(intentUrl, Intent.URI_INTENT_SCHEME);
            if (intent != null && intent.getPackage() != null) {
                view.getContext().startActivity(intent);
                Logger.info(methodTag, "Intent request sent to launch the app: " + intent.getPackage());
            } else {
                Logger.warn(methodTag, "Unable to parse the intent URI");
            }
        } catch (final URISyntaxException e) {
            Logger.error(methodTag, "Failed to parse the intent URI due to invalid syntax.", e);
            returnError(ErrorStrings.URI_SYNTAX_ERROR, e.getMessage());
        } catch (final ActivityNotFoundException e) {
            Logger.error(methodTag, "No activity found to handle the intent.", e);
            returnError(ErrorStrings.ACTIVITY_NOT_FOUND, e.getMessage());
        } catch (final Throwable throwable) {
            Logger.error(methodTag, "An unexpected error occurred while processing the intent URI.", throwable);
            returnError(ErrorStrings.UNEXPECTED_ERROR, throwable.getMessage());
        }
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

    private void processHeaderForwardingRequiredUri(@NonNull final WebView view, @NonNull final String url) {
        final String methodTag = TAG + ":processHeaderForwardingRequiredUri";

        Logger.infoPII(methodTag,"We are loading this new URL: '"
                + removeQueryParametersOrRedact(url) + "' with original requestHeaders appended.");

        view.loadUrl(url, mRequestHeaders);
    }

    private void processNonceAndReAttachHeaders(@NonNull final WebView view, @NonNull final String url) {
        final String methodTag = TAG + ":processNonceAndReAttachHeaders";

        final HashMap<String, String> queryParams = StringExtensions.getUrlParameters(url);
        final String nonceQueryParam = queryParams.get("sso_nonce");
        SpanExtension.current().setAttribute(
                AttributeName.is_sso_nonce_found_in_ests_request.name(), nonceQueryParam != null
        );
        if (nonceQueryParam != null) {
            final Span span = OTelUtility.createSpanFromParent(SpanName.ProcessNonceFromEstsRedirect.name(), mSpanContext);
            try (final Scope scope = SpanExtension.makeCurrentSpan(span)) {
                final NonceRedirectHandler nonceRedirect = new NonceRedirectHandler(view, mRequestHeaders, span);
                nonceRedirect.processChallenge(new URL(url));
                span.setStatus(StatusCode.OK);
            } catch (MalformedURLException e) {
                // No need to throw the error as we don't want to break the original flow.
                Logger.errorPII(methodTag, "Redirect URI has invalid syntax, unable to parse", e);
                span.setStatus(StatusCode.ERROR, "Redirect URI has invalid syntax, unable to parse");
                span.recordException(e);
            } catch (final Throwable throwable) {
                // No need to throw the error as we don't want to break the original flow.
                Logger.error(methodTag, "Error processing nonce and re-attaching headers", throwable);
                span.setStatus(StatusCode.ERROR, "Error processing nonce and re-attaching headers");
                span.recordException(throwable);
                view.loadUrl(url, mRequestHeaders);
            } finally {
                span.end();
            }
        }
    }

    /**
     * This method is used to process the webcp redirect and re-attach the PRT header to the request.
     */
    private void processWebCpAuthorize(@NonNull final WebView view, @NonNull final String url) {
        final String methodTag = TAG + ":processWebCPAuthorize";
        Logger.info(methodTag, "Processing WebCP authorize request.");
        final Span span = createSpanWithAttributesFromParent(SpanName.ProcessWebCpAuthorizeUrlRedirect.name());
        final ReAttachPrtHeaderHandler reAttachPrtHeaderHandler = new ReAttachPrtHeaderHandler(view, mRequestHeaders, span);
        reAttachPrtHeader(url, reAttachPrtHeaderHandler, view, methodTag, span);
    }

    /**
     * This method is used to process the cross cloud redirect and attach the PRT header to the request.
     */
    private void processCrossCloudRedirect(@NonNull final WebView view, @NonNull final String url) {
        final String methodTag = TAG + ":processCrossCloudRedirect";

        final Span span = OTelUtility.createSpanFromParent(SpanName.ProcessCrossCloudRedirect.name(), mSpanContext);
        final ReAttachPrtHeaderHandler reAttachPrtHeaderHandler = new ReAttachPrtHeaderHandler(view, mRequestHeaders, span);
        reAttachPrtHeader(url, reAttachPrtHeaderHandler, view, methodTag, span);
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    public void reAttachPrtHeader(@NonNull final String url, @NonNull final ReAttachPrtHeaderHandler reAttachPrtHandler, @NonNull final WebView view, @NonNull final String methodTag, @NonNull final Span span) {
        try (final Scope scope = SpanExtension.makeCurrentSpan(span)) {
            reAttachPrtHandler.processChallenge(url);
            span.setStatus(StatusCode.OK);
        } catch (final Throwable e) {
            // No op if an exception happens
            Logger.warn(methodTag, "Error attaching PRT header." + e);
            span.recordException(e);
            span.setStatus(StatusCode.ERROR);
            view.loadUrl(url);
        } finally {
            span.end();
        }
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

    @Override
    public void onPageStarted(final WebView view, final String url, final Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
        // Evaluate JavaScript for Passkey Registration if script is set and origin is allowed.
        if (mPasskeyRegistrationScript != null &&  PasskeyOriginRulesManager.isAllowedOrigin(url)) {
            Logger.verbose(TAG, "Executing onPageStarted PasskeyRegistration script for URL: " + url);
            view.evaluateJavascript(mPasskeyRegistrationScript, null);
        }
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

    /**
     * Extracts the broker app package name from the given URL.
     * Supports all known broker apps. Returns the first match found.
     * If no known broker app is found, logs a warning and returns an empty string.
     *
     * @param url The URL to inspect.
     * @return The broker app package name, or empty string if not found.
     */
    private String getBrokerAppPackageNameFromUrl(@NonNull final String url) {
        for (final BrokerData brokerData : BrokerData.getAllBrokers()) {
            if (url.contains(brokerData.getPackageName())) {
                return brokerData.getPackageName();
            }
        }
        Logger.warn(TAG + ":getBrokerAppPackageNameFromUrl", "No known broker app package name found in URL: " + url);
        return "";
    }

    /**
     * Create a span with parent span context if available.
     * @param spanName Name of the span to be created.
     * @return Created {@link Span}
     */
    private Span createSpanWithAttributesFromParent(@NonNull final String spanName) {
        final Span span = OTelUtility.createSpanFromParent(spanName, mSpanContext);
        if (mUtid != null) {
            span.setAttribute(AttributeName.tenant_id.name(), mUtid);
        }
        // Populate some of the parent span's attributes to current span.
        final Context oTelContext = getActivity() instanceof AuthorizationActivity ? ((AuthorizationActivity) getActivity()).getOtelContext() : null;
        if (oTelContext != null) {
            final Baggage baggage = BaggageExtension.fromContext(oTelContext);
            final List<AttributeName> parentAttributeNames = Arrays.asList(
                    AttributeName.correlation_id,
                    AttributeName.calling_package_name
            );
            for (AttributeName attributeName : parentAttributeNames) {
                final String value = baggage.getEntryValue(attributeName.name());
                if (value != null) {
                    span.setAttribute(attributeName.name(), value);
                }
            }
        }
        return span;
    }

    /**
     * Add a JavaScript to be executed in onPageStarted.
     * The script will be executed only for URLs that are allowed by {@link PasskeyOriginRulesManager}.
     * @param script JavaScript code to be executed.
     */
    public void addPasskeyRegistrationJsScript(@NonNull final String script) {
        this.mPasskeyRegistrationScript = script;
    }
}
