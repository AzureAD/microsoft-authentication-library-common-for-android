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

    private static AuthorizationStrategyFactory sInstance = null;

    public static AuthorizationStrategyFactory getInstance() {
        if (sInstance == null) {
            sInstance = new AuthorizationStrategyFactory();
        }
        return sInstance;
    }

    public GenericAuthorizationStrategy getAuthorizationStrategy(Activity activity, @NonNull AuthorizationConfiguration configuration) {
        if (configuration.getAuthorizationAgent() == AuthorizationAgent.WEBVIEW) {
            Logger.info(TAG, "Use webView for authorization.");
            return (GenericAuthorizationStrategy)(new EmbeddedWebViewAuthorizationStrategy(activity, configuration));
        }

        // Use device browser auth flow as default.
        Logger.info(TAG, "Use browser for authorization.");
        return (GenericAuthorizationStrategy)(new BrowserAuthorizationStrategy(activity, configuration));
    }
}
