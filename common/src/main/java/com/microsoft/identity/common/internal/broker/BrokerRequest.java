//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.common.internal.broker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;
import com.microsoft.identity.common.java.authscheme.AbstractAuthenticationScheme;
import com.microsoft.identity.common.java.opentelemetry.SerializableSpanContext;
import com.microsoft.identity.common.java.providers.oauth2.OpenIdConnectPromptParameter;
import com.microsoft.identity.common.java.request.SdkType;
import com.microsoft.identity.common.java.ui.BrowserDescriptor;

import java.io.Serializable;

import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * Represents the broker request
 */
@Builder
@Accessors(prefix = "m")
@Getter
public class BrokerRequest implements Serializable {

    private static final long serialVersionUID = -543392127065130474L;

    private static final class SerializedNames {
        final static String EXTRA_OPTIONS = "extra_options";
        final static String AUTHORITY = "authority";
        final static String SCOPE = "scopes";
        final static String REDIRECT = "redirect_uri";
        final static String CLIENT_ID = "client_id";
        final static String HOME_ACCOUNT_ID = "home_account_id";
        final static String LOCAL_ACCOUNT_ID = "local_account_id";
        final static String USERNAME = "username";
        final static String EXTRA_QUERY_STRING_PARAMETER = "extra_query_param";
        final static String CORRELATION_ID = "correlation_id";
        final static String PROMPT = "prompt";
        final static String CLAIMS = "claims";
        final static String FORCE_REFRESH = "force_refresh";
        final static String CLIENT_APP_NAME = "client_app_name";
        final static String CLIENT_APP_VERSION = "client_app_version";
        final static String CLIENT_VERSION = "client_version";
        final static String CLIENT_SDK_TYPE = "client_sdk_type";
        final static String ENVIRONMENT = "environment";
        final static String MULTIPLE_CLOUDS_SUPPORTED = "multiple_clouds_supported";
        final static String AUTHORIZATION_AGENT = "authorization_agent";
        final static String AUTHENTICATION_SCHEME = "authentication_scheme";
        final static String POWER_OPT_CHECK_ENABLED = "power_opt_check_enabled";
        final static String SPAN_CONTEXT = "span_context";
        final static String PREFERRED_BROWSER = "preferred_browser";
    }

    /**
     * Authority for the request
     */
    @SerializedName(SerializedNames.AUTHORITY)
    @NonNull
    private String mAuthority;

    /**
     * Scopes for the request. This is expected to be of the format
     * "scope 1 scope2 scope3" with space as a delimiter
     */
    @NonNull
    @SerializedName(SerializedNames.SCOPE)
    private String mScope;

    /**
     * The redirect uri for the request.
     */
    @NonNull
    @SerializedName(SerializedNames.REDIRECT)
    private String mRedirect;

    /**
     * The client id of the application.
     */
    @NonNull
    @SerializedName(SerializedNames.CLIENT_ID)
    private String mClientId;

    /**
     * The username for the request.
     */
    @Nullable
    @SerializedName(SerializedNames.USERNAME)
    private String mUserName;

    /**
     * Home account id of the user. Needs to be set for silent request
     */
    @Nullable
    @SerializedName(SerializedNames.HOME_ACCOUNT_ID)
    private String mHomeAccountId;

    /**
     * Local account id of the user. Needs to be set for silent request
     */
    @SerializedName(SerializedNames.LOCAL_ACCOUNT_ID)
    private String mLocalAccountId;

    /**
     * Extra query parameters set for the request.
     */
    @Nullable
    @SerializedName(SerializedNames.EXTRA_QUERY_STRING_PARAMETER)
    private String mExtraQueryStringParameter;

    /**
     * Extra options flags for the request.
     */
    @Nullable
    @SerializedName(SerializedNames.EXTRA_OPTIONS)
    private String mExtraOptions;

    /**
     * Correlation id for the request, it should be a unique GUID.
     */
    @NonNull
    @SerializedName(SerializedNames.CORRELATION_ID)
    private String mCorrelationId;

    /**
     * Prompt for the request.
     * {@link OpenIdConnectPromptParameter}
     * <p>
     * Default value : {@link OpenIdConnectPromptParameter#SELECT_ACCOUNT}
     */
    @Nullable
    @SerializedName(SerializedNames.PROMPT)
    private String mPrompt;

    /**
     * Claims for the request. This needs to be a valid json string.
     */
    @Nullable
    @SerializedName(SerializedNames.CLAIMS)
    private String mClaims;

    /**
     * Boolean if set, will try to refresh the token instead of using it from cache.
     */
    @Nullable
    @SerializedName(SerializedNames.FORCE_REFRESH)
    private boolean mForceRefresh;

    /**
     * Application package name.
     */
    @NonNull
    @SerializedName(SerializedNames.CLIENT_APP_NAME)
    private String mApplicationName;

    /**
     * Application version.
     */
    @NonNull
    @SerializedName((SerializedNames.CLIENT_APP_VERSION))
    private String mApplicationVersion;

    /**
     * Msal version.
     */
    @NonNull
    @SerializedName(SerializedNames.CLIENT_VERSION)
    private String mMsalVersion;

    /**
     * Sdk Type.
     */
    @NonNull
    @SerializedName(SerializedNames.CLIENT_SDK_TYPE)
    private SdkType mSdkType;

    /**
     * AAD Environment.
     */
    @NonNull
    @SerializedName(SerializedNames.ENVIRONMENT)
    private String mEnvironment;

    /**
     * Boolean indicated whether app supports multiple clouds.
     */
    @NonNull
    @SerializedName(SerializedNames.MULTIPLE_CLOUDS_SUPPORTED)
    private boolean mMultipleCloudsSupported;

    @Nullable
    @SerializedName(SerializedNames.AUTHORIZATION_AGENT)
    private String mAuthorizationAgent;

    @Nullable
    @SerializedName(SerializedNames.AUTHENTICATION_SCHEME)
    private AbstractAuthenticationScheme mAuthenticationScheme;

    @Nullable
    @SerializedName(SerializedNames.POWER_OPT_CHECK_ENABLED)
    private boolean mPowerOptCheckEnabled;

    @Nullable
    @SerializedName(SerializedNames.SPAN_CONTEXT)
    private SerializableSpanContext mSpanContext;

    @Nullable
    @SerializedName(SerializedNames.PREFERRED_BROWSER)
    private BrowserDescriptor mPreferredBrowser;

}
