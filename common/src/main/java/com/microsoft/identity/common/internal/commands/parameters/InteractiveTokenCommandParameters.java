package com.microsoft.identity.common.internal.commands.parameters;

import android.app.Activity;

import androidx.fragment.app.Fragment;

import com.google.gson.annotations.Expose;
import com.microsoft.identity.common.internal.providers.oauth2.OpenIdConnectPromptParameter;
import com.microsoft.identity.common.internal.ui.AuthorizationAgent;
import com.microsoft.identity.common.internal.ui.browser.BrowserDescriptor;

import java.util.HashMap;
import java.util.List;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
public class InteractiveTokenCommandParameters extends TokenCommandParameters {

    @EqualsAndHashCode.Exclude
    private transient Activity activity;

    @EqualsAndHashCode.Exclude
    private transient Fragment fragment;

    private transient List<BrowserDescriptor> browserSafeList;

    private transient HashMap<String, String> requestHeaders;

    private boolean brokerBrowserSupportEnabled;

    private String loginHint;

    @Expose()
    private OpenIdConnectPromptParameter prompt;

    @Expose()
    private AuthorizationAgent authorizationAgent;

    @Expose()
    private boolean isWebViewZoomEnabled;

    @Expose()
    private boolean isWebViewZoomControlsEnabled;

//    @Builder(builderMethodName = "interactiveTokenCommandParametersBuilder")
//    InteractiveTokenCommandParameters(String correlationId, String applicationName, String applicationVersion, String requiredBrokerProtocolVersion, SdkType sdkType, String sdkVersion, Context androidApplicationContext, OAuth2TokenCache oAuth2TokenCache, boolean isSharedDevice, String clientId, String redirectUri, @NonNull Set<String> scopes, Authority authority, String claimsRequestJson, List<Pair<String, String>> extraQueryStringParameters, List<String> extraScopesToConsent, IAccountRecord account, AbstractAuthenticationScheme authenticationScheme, Activity activity, Fragment fragment, String loginHint, OpenIdConnectPromptParameter prompt, HashMap<String, String> requestHeaders, AuthorizationAgent authorizationAgent, boolean brokerBrowserSupportEnabled, boolean isWebViewZoomEnabled, boolean isWebViewZoomControlsEnabled, List<BrowserDescriptor> browserSafeList) {
//        super(correlationId, applicationName, applicationVersion, requiredBrokerProtocolVersion, sdkType, sdkVersion, androidApplicationContext, oAuth2TokenCache, isSharedDevice, clientId, redirectUri, scopes, authority, claimsRequestJson, extraQueryStringParameters, extraScopesToConsent, account, authenticationScheme);
//        this.activity = activity;
//        this.fragment = fragment;
//        this.loginHint = loginHint;
//        this.prompt = prompt;
//        this.requestHeaders = requestHeaders;
//        this.authorizationAgent = authorizationAgent;
//        this.brokerBrowserSupportEnabled = brokerBrowserSupportEnabled;
//        this.isWebViewZoomEnabled = isWebViewZoomEnabled;
//        this.isWebViewZoomControlsEnabled = isWebViewZoomControlsEnabled;
//        this.browserSafeList = browserSafeList;
//    }
}
