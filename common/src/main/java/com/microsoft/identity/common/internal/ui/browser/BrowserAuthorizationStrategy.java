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
package com.microsoft.identity.common.internal.ui.browser;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.internal.providers.oauth2.AndroidAuthorizationStrategy;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationActivityFactory;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationActivityParameters;
import com.microsoft.identity.common.internal.providers.oauth2.BrowserRedirectValidator;
import com.microsoft.identity.common.internal.ui.webview.ProcessUtil;
import com.microsoft.identity.common.java.WarningType;
import com.microsoft.identity.common.java.browser.Browser;
import com.microsoft.identity.common.java.configuration.LibraryConfiguration;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.flighting.CommonFlight;
import com.microsoft.identity.common.java.flighting.CommonFlightsManager;
import com.microsoft.identity.common.java.providers.RawAuthorizationResult;
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationResult;
import com.microsoft.identity.common.java.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.java.ui.AuthorizationAgent;
import com.microsoft.identity.common.java.util.ResultFuture;
import com.microsoft.identity.common.logging.Logger;

import java.net.URI;
import java.util.concurrent.Future;

import static com.microsoft.identity.common.java.AuthenticationConstants.UIRequest.BROWSER_FLOW;

// Suppressing rawtype warnings due to the generic types OAuth2Strategy, AuthorizationRequest and AuthorizationResult
@SuppressWarnings(WarningType.rawtype_warning)
public abstract class BrowserAuthorizationStrategy<
        GenericOAuth2Strategy extends OAuth2Strategy,
        GenericAuthorizationRequest extends AuthorizationRequest>
        extends AndroidAuthorizationStrategy<GenericOAuth2Strategy, GenericAuthorizationRequest> {

    private final static String TAG = BrowserAuthorizationStrategy.class.getSimpleName();

    private CustomTabsManager mCustomTabManager;
    private ResultFuture<AuthorizationResult> mAuthorizationResultFuture;
    private boolean mDisposed;
    private GenericOAuth2Strategy mOAuth2Strategy; //NOPMD
    private GenericAuthorizationRequest mAuthorizationRequest; //NOPMD

    private final Browser mBrowser;

    public BrowserAuthorizationStrategy(@NonNull Context applicationContext,
                                        @NonNull Activity activity,
                                        @Nullable Fragment fragment,
                                        @NonNull Browser browser) {
        super(applicationContext, activity, fragment);
        mBrowser = browser;
    }

    @Override
    public Future<AuthorizationResult> requestAuthorization(
            GenericAuthorizationRequest authorizationRequest,
            GenericOAuth2Strategy oAuth2Strategy)
            throws ClientException {
        final String methodTag = TAG + ":requestAuthorization";
        checkNotDisposed();
        final Context context = getApplicationContext();
        mOAuth2Strategy = oAuth2Strategy;
        mAuthorizationRequest = authorizationRequest;
        mAuthorizationResultFuture = new ResultFuture<>();

        //ClientException will be thrown if no browser found.
        Intent authIntent;
        if (mBrowser.isCustomTabsServiceSupported()) {
            Logger.info(
                    methodTag,
                    "CustomTabsService is supported."
            );
            //create customTabsIntent
            mCustomTabManager = new CustomTabsManager(context);
            if (!mCustomTabManager.bind(context, mBrowser.getPackageName())) {
                //create browser auth intent
                authIntent = new Intent(Intent.ACTION_VIEW);
                authIntent.setPackage(mBrowser.getPackageName());
            } else {
                authIntent = mCustomTabManager.getCustomTabsIntent().intent;
            }
        } else {
            Logger.warn(
                    methodTag,
                    "CustomTabsService is NOT supported"
            );
            //create browser auth intent
            authIntent = new Intent(Intent.ACTION_VIEW);
            authIntent.setPackage(mBrowser.getPackageName());
        }

        final URI requestUrl = authorizationRequest.getAuthorizationRequestAsHttpRequest();

        authIntent.setData(Uri.parse(requestUrl.toString()));

        final Intent intent = buildAuthorizationActivityStartIntent(authIntent, requestUrl);
        launchIntent(intent);

        return mAuthorizationResultFuture;
    }

    // Suppressing unchecked warnings during casting to HashMap<String,String> due to no generic type with mAuthorizationRequest
    @SuppressWarnings(WarningType.unchecked_warning)
    private Intent buildAuthorizationActivityStartIntent(Intent authIntent, URI requestUrl) {
         // RedirectURI used to get the auth code in nested app auth is that of a hub app (brkRedirectURI)   
        final String redirectUri = mAuthorizationRequest.getBrkRedirectUri() != null ? mAuthorizationRequest.getBrkRedirectUri() : mAuthorizationRequest.getRedirectUri();
        final AuthorizationActivityParameters authorizationActivityParameters = new AuthorizationActivityParameters(
                getApplicationContext(),
                authIntent,
                requestUrl.toString(),
                redirectUri,
                mAuthorizationRequest.getRequestHeaders(),
                AuthorizationAgent.BROWSER
        );
        final Intent intent = AuthorizationActivityFactory.getAuthorizationActivityIntent(authorizationActivityParameters);
        setIntentFlag(intent);
        return intent;
    }

    /**
     * Validates that no other application is registered for the same custom URL scheme before
     * delegating to the base {@code launchIntent} to start the authorization UI.
     * <p>
     * A {@link ClientException} with error code
     * {@link com.microsoft.identity.common.java.exception.ErrorStrings#MULTIPLE_APPS_LISTENING_CUSTOM_URL_SCHEME}
     * is thrown if a conflicting app is detected, ensuring it propagates correctly through the
     * command pipeline to MSAL's callback adapter layer.
     */
    @Override
    protected void launchIntent(@NonNull final Intent intent) throws ClientException {
        final String methodTag = TAG + ":launchIntent";
        final Context appContext = getApplicationContext();
        if (appContext == null) {
            Logger.info(methodTag,
                    "Application context is null; skipping multiple-app URL scheme validation.");
        } else {
            final String redirectUri = intent.getStringExtra(
                    AuthenticationConstants.AuthorizationIntentKey.REDIRECT_URI);
            if (redirectUri == null) {
                Logger.info(methodTag,
                        "Redirect URI is null in the intent; skipping multiple-app URL scheme validation.");
            } else {
                // Conditions to skip this check (brokered flows only):
                // 1. Flight SKIP_MULTIPLE_APP_VALIDATION_IN_AUTH_SERVICE is enabled (default: true)
                // 2. Request is running in the auth process (Broker request)
                // 3. Request has a redirect uri that is a valid app link or msauth
                // These conditions are met exclusively in brokered flows (e.g. COBO/COPE/AM API),
                // since only those have a browser authorization agent under broker requests
                // (Check MsalAndroidBrokerCommandParameterAdapter).
                if (CommonFlightsManager.INSTANCE.getFlightsProvider().isFlightEnabled(CommonFlight.SKIP_MULTIPLE_APP_VALIDATION_IN_AUTH_SERVICE) &&
                        ProcessUtil.isRunningOnAuthService(appContext) && checkIfRedirectUriIsValidAppLinkOrMsauth(redirectUri)) {
                    Logger.info(methodTag,
                            "Running in broker auth process; skipping multiple-app URL scheme validation.");
                } else {
                    BrowserRedirectValidator.validateNoMultipleAppsListening(
                            appContext,
                            redirectUri,
                            LibraryConfiguration.getInstance().isAuthorizationInCurrentTask()
                    );
                }
            }
        }
        super.launchIntent(intent);
    }

    private boolean checkIfRedirectUriIsValidAppLinkOrMsauth(@NonNull final String redirectUri) {
        Logger.info(TAG, "Checking if redirect URI is a valid app link or msauth: " + redirectUri);

        return redirectUri.startsWith(AuthenticationConstants.Broker.APP_LINK_PREFIX + "/" + AuthenticationConstants.Broker.LTW_APP_PACKAGE_NAME) ||
                redirectUri.startsWith(AuthenticationConstants.Broker.APP_LINK_PREFIX + "/" + AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_PACKAGE_NAME) ||
                redirectUri.startsWith(AuthenticationConstants.Broker.APP_LINK_PREFIX + "/" + AuthenticationConstants.Broker.BROKER_HOST_APP_PACKAGE_NAME) ||
                redirectUri.startsWith(AuthenticationConstants.Broker.APP_LINK_PREFIX + "/" + AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME) ||
                redirectUri.startsWith(AuthenticationConstants.Broker.APP_LINK_PREFIX + "/" + AuthenticationConstants.Broker.INTUNE_APP_PACKAGE_NAME) ||
                redirectUri.startsWith(AuthenticationConstants.Broker.REDIRECT_PREFIX);
    }

    protected abstract void setIntentFlag(@NonNull final Intent intent);

    private void checkNotDisposed() {
        if (mDisposed) {
            throw new IllegalStateException("Service has been disposed and rendered inoperable");
        }
    }


    @Override
    public void completeAuthorization(int requestCode, @NonNull final RawAuthorizationResult data) {
        final String methodTag = TAG + ":completeAuthorization";
        if (requestCode == BROWSER_FLOW) {
            dispose();

            //Suppressing unchecked warnings due to method createAuthorizationResult being a member of the raw type AuthorizationResultFactory
            @SuppressWarnings(WarningType.unchecked_warning) final AuthorizationResult result = mOAuth2Strategy
                    .getAuthorizationResultFactory().createAuthorizationResult(
                            data,
                            mAuthorizationRequest
                    );
            mAuthorizationResultFuture.setResult(result);
        } else {
            Logger.warnPII(methodTag, "Unknown request code " + requestCode);
        }
    }

    /**
     * Disposes state that will not normally be handled by garbage collection. This should be
     * called when the authorization service is no longer required, including when any owning
     * activity is paused or destroyed (i.e. in Activity#onStop()).
     */
    public void dispose() {
        if (mDisposed) {
            return;
        }
        if (mCustomTabManager != null) {
            mCustomTabManager.unbind();
        }
        mDisposed = true;
    }
}
