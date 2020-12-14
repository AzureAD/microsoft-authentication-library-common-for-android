package com.microsoft.identity.client.ui.automation.sdk;

import android.app.Activity;
import lombok.Getter;

/**
 * A wrapper class for all the parameters that are required to acquire token
 * either interactively or silently.
 */
@Getter
public class AuthTestParams {

    protected String loginHint;
    protected String resource;
    protected String clientId;
    protected String redirectUri;
    protected String authority;
    protected Activity activity;
}
