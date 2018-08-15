package com.microsoft.identity.common.internal.ui;

import android.app.Activity;
import android.support.annotation.NonNull;

import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationConfiguration;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationStrategy;
import com.microsoft.identity.common.internal.ui.webview.EmbeddedWebViewAuthorizationStrategy;
import com.microsoft.identity.common.internal.ui.webview.challengehandlers.IChallengeCompletionCallback;
import com.microsoft.identity.common.internal.ui.browser.BrowserAuthorizationStrategy;

import java.io.UnsupportedEncodingException;

public class AuthorizationStrategyFactory <GenericAuthorizationStrategy extends AuthorizationStrategy> {
    private static final String TAG = AuthorizationStrategyFactory.class.getSimpleName();

    public GenericAuthorizationStrategy getAuthorizationStrategy(@NonNull final Activity activity,
                                                                 @NonNull AuthorizationConfiguration configuration,
                                                                 IChallengeCompletionCallback callback) throws UnsupportedEncodingException, ClientException {
        if (configuration.getWebViewType() == AuthorizationAgent.WEBVIEW) {
            Logger.info(TAG, "Use webView for authorization.");
            return (GenericAuthorizationStrategy)(new EmbeddedWebViewAuthorizationStrategy(activity, callback));
        }

        /*if (configuration.getWebViewType() == WebViewType.SYSTEM_BROWSER) {
            return (GenericAuthorizationStrategy)(new SystemBrowserAuthorizationStrategy(activity, authorizationRequest, configuration));
        }

        Logger.error(TAG, "Invalid webView type.", null);
        throw new ClientException(TAG, "Invalid webView type. " + "WebViewType = " + configuration.getWebViewType());*/

        // SystemBrowserAuthorizationStrategy as default.
        Logger.info(TAG, "Use browser for authorization.");
        return (GenericAuthorizationStrategy)(new BrowserAuthorizationStrategy(activity, configuration));
    }
}
