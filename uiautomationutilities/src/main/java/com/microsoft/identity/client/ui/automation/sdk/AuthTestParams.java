package com.microsoft.identity.client.ui.automation.sdk;

import android.app.Activity;

import com.microsoft.aad.adal.PromptBehavior;

import lombok.Builder;
import lombok.Getter;

/**
 * A wrapper class for all the parameters that are required to acquire token
 * either interactively or silently.
 */
@Builder
@Getter
public class AuthTestParams {

    private final String loginHint;
    private final String resource;
    private final String clientId;
    private final String redirectUri;
    private final String authority;
    private final PromptBehavior promptParameter;
    private final Activity activity;
    private final String extraQueryParameters;

}
