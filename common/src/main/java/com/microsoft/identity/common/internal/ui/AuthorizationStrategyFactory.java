package com.microsoft.identity.common.internal.ui;

import android.app.Activity;
import android.support.annotation.NonNull;

import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationConfiguration;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationStrategy;
import com.microsoft.identity.common.internal.ui.embeddedwebview.EmbeddedWebViewAuthorizationStrategy;
import com.microsoft.identity.common.internal.ui.embeddedwebview.challengehandlers.IChallengeCompletionCallback;
import com.microsoft.identity.common.internal.ui.systembrowser.SystemBrowserAuthorizationStrategy;

import java.io.UnsupportedEncodingException;

public class AuthorizationStrategyFactory <GenericAuthorizationStrategy extends AuthorizationStrategy,
        GenericAuthorizationRequest extends AuthorizationRequest> {
    private static final String TAG = AuthorizationStrategyFactory.class.getSimpleName();

    public GenericAuthorizationStrategy getAuthorizationStrategy(@NonNull final Activity activity,
                                                                 @NonNull GenericAuthorizationRequest authorizationRequest,
                                                                 @NonNull AuthorizationConfiguration configuration,
                                                                 IChallengeCompletionCallback callback) throws UnsupportedEncodingException, ClientException {
        if (configuration.getWebViewType() == WebViewType.EMBEDDED_WEBVIEW) {
            return (GenericAuthorizationStrategy)(new EmbeddedWebViewAuthorizationStrategy(activity, authorizationRequest, callback));
        }

        if (configuration.getWebViewType() == WebViewType.SYSTEM_BROWSER) {
            return (GenericAuthorizationStrategy)(new SystemBrowserAuthorizationStrategy(activity, authorizationRequest, configuration));
        }

        Logger.error(TAG, "Invalid webView type.", null);
        return null;
    }
}
