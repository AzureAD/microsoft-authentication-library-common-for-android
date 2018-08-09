package com.microsoft.identity.common.internal.ui;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.webkit.WebView;

import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftAuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationConfiguration;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationStrategy;
import com.microsoft.identity.common.internal.ui.embeddedwebview.AzureActiveDirectoryWebViewClient;
import com.microsoft.identity.common.internal.ui.embeddedwebview.challengehandlers.IChallengeCompletionCallback;

public class AuthorizationStrategyFactory {
    public AuthorizationStrategy getAuthorizationStrategy(@NonNull final Activity activity,
                                                          AuthorizationRequest authorizationRequest,
                                                          IChallengeCompletionCallback callback,
                                                          AuthorizationConfiguration configuration) {
        if (configuration.getWebViewType() == WebViewType.EMBEDDED_WEBVIEW) {
            if (authorizationRequest instanceof MicrosoftAuthorizationRequest) {
                AzureActiveDirectoryWebViewClient webViewClient = new AzureActiveDirectoryWebViewClient(
                        activity,
                        (MicrosoftAuthorizationRequest) authorizationRequest,
                        callback);
            }

        }

        if (configuration.getWebViewType() == WebViewType.SYSTEM_BROWSER) {

        }

        return null;
    }
}
