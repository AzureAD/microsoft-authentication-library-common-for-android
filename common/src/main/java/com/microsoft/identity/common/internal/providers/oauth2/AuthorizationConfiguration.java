package com.microsoft.identity.common.internal.providers.oauth2;

import com.microsoft.identity.common.internal.ui.WebViewType;

public class AuthorizationConfiguration {
    private static AuthorizationConfiguration sInstance = null;

    private final WebViewType mWebViewType;

    private AuthorizationConfiguration() {
        mWebViewType = WebViewType.SYSTEM_BROWSER;
    }

    public static AuthorizationConfiguration getInstance() {
        if (sInstance == null) {
            sInstance = new AuthorizationConfiguration();
        }

        return sInstance;
    }

    public WebViewType getWebViewType() {
        return mWebViewType;
    }
}