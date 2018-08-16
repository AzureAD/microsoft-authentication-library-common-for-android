package com.microsoft.identity.common.internal.providers.oauth2;

import com.microsoft.identity.common.internal.ui.AuthorizationAgent;

import java.io.Serializable;

public class AuthorizationConfiguration implements Serializable {
    /**
     * Serial version id.
     */
    private static final long serialVersionUID = -8547851138779764480L;

    private static AuthorizationConfiguration sInstance = null;

    private final AuthorizationAgent mWebViewType;

    private String mRedirectUrl;

    private AuthorizationConfiguration() {
        mWebViewType = AuthorizationAgent.BROWSER;
    }

    public static AuthorizationConfiguration getInstance() {
        if (sInstance == null) {
            sInstance = new AuthorizationConfiguration();
        }

        return sInstance;
    }

    public void setRedirectUrl(final String redirectUrl) {
        mRedirectUrl = redirectUrl;
    }
    public String getRedirectUrl() {
        return mRedirectUrl;
    }

    public AuthorizationAgent getWebViewType() {
        return mWebViewType;
    }
}