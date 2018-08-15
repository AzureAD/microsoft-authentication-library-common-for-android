package com.microsoft.identity.common.internal.providers.oauth2;

import com.microsoft.identity.common.internal.ui.AuthorizationAgent;

public class AuthorizationConfiguration {
    private static AuthorizationConfiguration sInstance = null;

    private final AuthorizationAgent mWebViewType;

    private AuthorizationConfiguration() {
        mWebViewType = AuthorizationAgent.BROWSER;
    }

    public static AuthorizationConfiguration getInstance() {
        if (sInstance == null) {
            sInstance = new AuthorizationConfiguration();
        }

        return sInstance;
    }

    public AuthorizationAgent getWebViewType() {
        return mWebViewType;
    }
}