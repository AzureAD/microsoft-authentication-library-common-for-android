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
}
