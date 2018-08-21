package com.microsoft.identity.common.internal.providers.oauth2;

import com.microsoft.identity.common.internal.ui.AuthorizationAgent;

import java.io.Serializable;

public class AuthorizationConfiguration implements Serializable {
    /**
     * Serial version id.
     */
    private static final long serialVersionUID = -8547851138779764480L;

    private static AuthorizationConfiguration sInstance = null;

    private AuthorizationAgent mAuthorizationAgent;

    private String mRedirectUrl;

    private AuthorizationConfiguration() {
        mAuthorizationAgent = AuthorizationAgent.BROWSER;
    }

    public static AuthorizationConfiguration getInstance() {
        if (sInstance == null) {
            sInstance = new AuthorizationConfiguration();
        }

        return sInstance;
    }

    /**
     * If the dev wants to specify the ui flow to use embedded webView.
     * you need to call AuthorizationConfiguration.getInstance().setAuthorizationAgent(AuthorizationAgent.WEBVIEW);
     * before initializing the authorization strategy. Otherwise, browser flow will be used as default.
     *
     * @param authorizationAgent AuthorizationAgent
     */
    public void setAuthorizationAgent(final AuthorizationAgent authorizationAgent) {
        mAuthorizationAgent = authorizationAgent;
    }

    public void setRedirectUrl(final String redirectUrl) {
        mRedirectUrl = redirectUrl;
    }

    public String getRedirectUrl() {
        return mRedirectUrl;
    }

    public AuthorizationAgent getAuthorizationAgent() {
        return mAuthorizationAgent;
    }
}