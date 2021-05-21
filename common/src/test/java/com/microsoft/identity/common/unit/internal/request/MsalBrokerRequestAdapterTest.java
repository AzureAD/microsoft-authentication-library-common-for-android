// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.common.unit.internal.request;

import android.app.Activity;
import android.util.Pair;

import androidx.fragment.app.Fragment;

import com.microsoft.identity.common.internal.authorities.AccountsInOneOrganization;
import com.microsoft.identity.common.internal.authorities.Authority;
import com.microsoft.identity.common.internal.authorities.AzureActiveDirectoryAudience;
import com.microsoft.identity.common.internal.authorities.AzureActiveDirectoryAuthority;
import com.microsoft.identity.common.internal.authscheme.AbstractAuthenticationScheme;
import com.microsoft.identity.common.internal.authscheme.BearerAuthenticationSchemeInternal;
import com.microsoft.identity.common.internal.broker.BrokerRequest;
import com.microsoft.identity.common.internal.commands.parameters.BrokerInteractiveTokenCommandParameters;
import com.microsoft.identity.common.internal.commands.parameters.InteractiveTokenCommandParameters;
import com.microsoft.identity.common.internal.dto.IAccountRecord;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectoryCloud;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectorySlice;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2TokenCache;
import com.microsoft.identity.common.internal.providers.oauth2.OpenIdConnectPromptParameter;
import com.microsoft.identity.common.internal.request.MsalBrokerRequestAdapter;
import com.microsoft.identity.common.internal.request.SdkType;
import com.microsoft.identity.common.internal.ui.AuthorizationAgent;
import com.microsoft.identity.common.internal.ui.browser.BrowserDescriptor;
import com.microsoft.identity.common.internal.util.StringUtil;
import com.microsoft.identity.common.unit.MockAccountRecord;
import com.microsoft.identity.common.unit.MockOauth2TokenCache;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.ParameterizedRobolectricTestRunner;
import org.robolectric.Robolectric;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@RunWith(ParameterizedRobolectricTestRunner.class)
public class MsalBrokerRequestAdapterTest {

    public static final String TEST_APPLICATION_NAME = "application";
    public static final String TEST_APPLICATION_VERSION = "version";
    public static final boolean CLOUD_IS_VALIDATES = true;
    public static final String CACHE_HOST_NAME = "cacheHostName";
    public static final String NETWORK_HOST_NAME = "networkHostName";
    public static final String TEST_TENANT_ID = "aTenantId";
    public static final String TEST_CLOUD_URL = "https://login.fabrikam.com";
    public static final AzureActiveDirectoryAudience TEST_AUDIENCE = AccountsInOneOrganization.builder().tenantId(TEST_TENANT_ID).cloudUrl(TEST_CLOUD_URL)
            .build();
    public static final AzureActiveDirectorySlice TEST_SLICE_WITH_SLICE_DC = AzureActiveDirectorySlice.builder()
            .slice("aSlice")
            .dataCenter("dataCenter")
            .build();
    public static final AzureActiveDirectorySlice TEST_SLICE_WITH_SLICE = AzureActiveDirectorySlice.builder()
            .slice("aSlice")
            .build();
    public static final AzureActiveDirectorySlice TEST_SLICE_WITH_DC = AzureActiveDirectorySlice.builder()
            .dataCenter("dataCenter")
            .build();
    public static final AzureActiveDirectorySlice TEST_SLICE_WITH_NOTHING = AzureActiveDirectorySlice.builder()
            .build();
    public static final IAccountRecord TEST_ACCOUNT_RECORD = new MockAccountRecord();

    public static final List<BrowserDescriptor> TEST_BROWSER_SAFE_LIST = Arrays.asList(
            BrowserDescriptor.builder()
                    .packageName("aBrowser")
                    .signatureHashes(Collections.singleton("browserHash"))
                    .versionLowerBound("1")
                    .versionUpperBound("2")
                    .build());
    public static final String TEST_CLAIMS_JSON = "{ \"claims\": \"something\"";
    public static final String TEST_CLIENT_ID = "aClientId";
    public static final String TEST_CORRELATION_ID = "aCorrelationId";
    public static final List<Pair<String, String>> TEST_EXTRA_OPTIONS = Arrays.asList(new Pair<String, String>("one", "two"));
    public static final List<Pair<String, String>> TEST_EXTRA_QUERY_STRING_PARAMETERS = Arrays.asList(new Pair<String, String>("QPone", "QPtwo"));
    public static final List<Pair<String, String>> TEST_EXTRA_QUERY_STRING_PARAMETERS_SLICE = Arrays.asList(new Pair<String, String>("QPone", "QPtwo"), new Pair<>("slice", "aDifferentSlice"));
    public static final List<String> TEST_EXTRA_SCOPE = Arrays.asList("extraScope");
    public static final String TEST_LOGIN_HINT = "aLoginHint";
    public static final OAuth2TokenCache TEST_OAUTH_2_TOKEN_CACHE = new MockOauth2TokenCache();
    public static final String TEST_REDIRECT_URI = "aRedirectUri";
    public static final HashMap<String, String> TEST_REQUEST_HEADERS = new HashMap<>(Collections.singletonMap("aHeader", "aValue"));
    public static final String REQUIRED_BROKER_PROTOCOL_VERSION = "3.0";
    public static final String SDK_VERSION = "sdkVersion";
    public static final Set<String> TEST_SCOPES = Collections.singleton("aScope");
    public static final List<String> CLOUD_ALIASES = Arrays.asList("common");
    public static final String TEST_AUTHORITY_TYPE = "AAD";
    public static final AzureActiveDirectoryCloud TEST_CLOUD = AzureActiveDirectoryCloud.builder()
            .cloudHostAliases(CLOUD_ALIASES)
            .isValidated(CLOUD_IS_VALIDATES)
            .preferredCacheHostName(CACHE_HOST_NAME)
            .preferredNetworkHostName(NETWORK_HOST_NAME)
            .build();
    public static final AzureActiveDirectoryAuthority TEST_AUTHORITY_WITH_SLICE_DC = AzureActiveDirectoryAuthority.builder()
            .audience(TEST_AUDIENCE)
            .slice(TEST_SLICE_WITH_SLICE)
            .authorityTypeString(TEST_AUTHORITY_TYPE)
            .azureActiveDirectoryCloud(TEST_CLOUD)
            .authorityUrl("https://an.authority.url/")
            .build();
    public static final AzureActiveDirectoryAuthority TEST_AUTHORITY_WITH_SLICE = AzureActiveDirectoryAuthority.builder()
            .audience(TEST_AUDIENCE)
            .slice(TEST_SLICE_WITH_SLICE)
            .authorityTypeString(TEST_AUTHORITY_TYPE)
            .azureActiveDirectoryCloud(TEST_CLOUD)
            .authorityUrl("https://an.authority.url/")
            .build();
    public static final AzureActiveDirectoryAuthority TEST_AUTHORITY_WITH_DC = AzureActiveDirectoryAuthority.builder()
            .audience(TEST_AUDIENCE)
            .slice(TEST_SLICE_WITH_DC)
            .authorityTypeString(TEST_AUTHORITY_TYPE)
            .azureActiveDirectoryCloud(TEST_CLOUD)
            .authorityUrl("https://an.authority.url/")
            .build();
    public static final AzureActiveDirectoryAuthority TEST_AUTHORITY_WITH_NONE_SLICE = AzureActiveDirectoryAuthority.builder()
            .audience(TEST_AUDIENCE)
            .slice(TEST_SLICE_WITH_NOTHING)
            .authorityTypeString(TEST_AUTHORITY_TYPE)
            .azureActiveDirectoryCloud(TEST_CLOUD)
            .authorityUrl("https://an.authority.url/")
            .build();
    public static final AzureActiveDirectoryAuthority TEST_AUTHORITY_WITH_NULL_SLICE = AzureActiveDirectoryAuthority.builder()
            .audience(TEST_AUDIENCE)
            .slice(null)
            .authorityTypeString(TEST_AUTHORITY_TYPE)
            .azureActiveDirectoryCloud(TEST_CLOUD)
            .authorityUrl("https://an.authority.url/")
            .build();
    public static final AzureActiveDirectoryAuthority TEST_AUTHORITY_WITHOUT_SLICE = AzureActiveDirectoryAuthority.builder()
            .audience(TEST_AUDIENCE)
            .authorityTypeString(TEST_AUTHORITY_TYPE)
            .azureActiveDirectoryCloud(TEST_CLOUD)
            .authorityUrl("https://an.authority.url/")
            .build();
    public static final Fragment TEST_FRAGMENT = new Fragment();
    private final List<AzureActiveDirectorySlice> slices = Arrays.asList(TEST_SLICE_WITH_DC,
            TEST_SLICE_WITH_SLICE, TEST_SLICE_WITH_SLICE_DC, TEST_SLICE_WITH_NOTHING, null);
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();
    MsalBrokerRequestAdapter adapter = new MsalBrokerRequestAdapter();
    boolean forceRefresh = true;
    boolean handleNullTaskAffinity = true;
    boolean isSharedDevice = true;
    boolean isWebViewZoomControlsEnabled = true;
    boolean isWebViewZoomEnabled = true;
    boolean powerOptCheckEnabled = true;
    OpenIdConnectPromptParameter promptParameter = OpenIdConnectPromptParameter.LOGIN;
    SdkType sdkType = SdkType.ADAL;
    Authority testAuthority = TEST_AUTHORITY_WITH_SLICE;
    String testApplicationName = TEST_APPLICATION_NAME;
    String testApplicationVersion = TEST_APPLICATION_VERSION;
    AbstractAuthenticationScheme authenticationScheme = BearerAuthenticationSchemeInternal.builder().build();
    AuthorizationAgent authorizationAgent = AuthorizationAgent.BROWSER;
    boolean brokerBrowserSupportEnabled = false;
    List<BrowserDescriptor> testBrowserSafeList = TEST_BROWSER_SAFE_LIST;
    IAccountRecord testAccountRecord = TEST_ACCOUNT_RECORD;
    String testClaimsJson = TEST_CLAIMS_JSON;
    String testClientId = TEST_CLIENT_ID;
    String testCorrelationId = TEST_CORRELATION_ID;
    List<Pair<String, String>> testExtraOptions = TEST_EXTRA_OPTIONS;
    List<Pair<String, String>> testExtraQueryStringParameters = TEST_EXTRA_QUERY_STRING_PARAMETERS;
    List<String> testExtraScope = TEST_EXTRA_SCOPE;
    Fragment testFragment = TEST_FRAGMENT;
    String testLoginHint = TEST_LOGIN_HINT;
    OAuth2TokenCache testOauth2TokenCache = TEST_OAUTH_2_TOKEN_CACHE;
    String testRedirectUri = TEST_REDIRECT_URI;
    HashMap<String, String> testRequestHeaders = TEST_REQUEST_HEADERS;
    String requiredBrokerProtocolVersion = REQUIRED_BROKER_PROTOCOL_VERSION;
    String sdkVersion = SDK_VERSION;
    Set<String> testScopes = TEST_SCOPES;

    public MsalBrokerRequestAdapterTest(String name, boolean forceRefresh, boolean handleNullTaskAffinity, boolean isSharedDevice,
                                        boolean isWebViewZoomControlsEnabled, boolean isWebViewZoomEnabled,
                                        boolean powerOptCheckEnabled, OpenIdConnectPromptParameter promptParameter,
                                        SdkType sdkType, Authority testAuthority, String testApplicationName,
                                        String testApplicationVersion,
                                        AbstractAuthenticationScheme authenticationScheme,
                                        AuthorizationAgent authorizationAgent, boolean brokerBrowserSupportEnabled,
                                        List<BrowserDescriptor> testBrowserSafeList,
                                        IAccountRecord testAccountRecord, String testClaimsJson, String testClientId,
                                        String testCorrelationId, List<Pair<String, String>> testExtraOptions,
                                        List<Pair<String, String>> testExtraQueryStringParameters,
                                        List<String> testExtraScope, Fragment testFragment, String testLoginHint,
                                        OAuth2TokenCache testOauth2TokenCache, String testRedirectUri,
                                        HashMap<String, String> testRequestHeaders, String requiredBrokerProtocolVersion,
                                        String sdkVersion, Set<String> testScopes) {
        this.forceRefresh = forceRefresh;
        this.handleNullTaskAffinity = handleNullTaskAffinity;
        this.isSharedDevice = isSharedDevice;
        this.isWebViewZoomControlsEnabled = isWebViewZoomControlsEnabled;
        this.isWebViewZoomEnabled = isWebViewZoomEnabled;
        this.powerOptCheckEnabled = powerOptCheckEnabled;
        this.promptParameter = promptParameter;
        this.sdkType = sdkType;
        this.testAuthority = testAuthority;
        this.testApplicationName = testApplicationName;
        this.testApplicationVersion = testApplicationVersion;
        this.authenticationScheme = authenticationScheme;
        this.authorizationAgent = authorizationAgent;
        this.brokerBrowserSupportEnabled = brokerBrowserSupportEnabled;
        this.testBrowserSafeList = testBrowserSafeList;
        this.testAccountRecord = testAccountRecord;
        this.testClaimsJson = testClaimsJson;
        this.testClientId = testClientId;
        this.testCorrelationId = testCorrelationId;
        this.testExtraOptions = testExtraOptions;
        this.testExtraQueryStringParameters = testExtraQueryStringParameters;
        this.testExtraScope = testExtraScope;
        this.testFragment = testFragment;
        this.testLoginHint = testLoginHint;
        this.testOauth2TokenCache = testOauth2TokenCache;
        this.testRedirectUri = testRedirectUri;
        this.testRequestHeaders = testRequestHeaders;
        this.requiredBrokerProtocolVersion = requiredBrokerProtocolVersion;
        this.sdkVersion = sdkVersion;
        this.testScopes = testScopes;
    }

    @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
    public static Collection arguments() {
        return Arrays.asList(new Object[][]{
                {"login_adal", true, true, true, true, true, true, OpenIdConnectPromptParameter.LOGIN, SdkType.ADAL, TEST_AUTHORITY_WITH_DC,
                        TEST_APPLICATION_NAME, TEST_APPLICATION_VERSION, BearerAuthenticationSchemeInternal.builder().build(),
                        AuthorizationAgent.BROWSER,
                        true, TEST_BROWSER_SAFE_LIST, TEST_ACCOUNT_RECORD, TEST_CLAIMS_JSON, TEST_CLIENT_ID,
                        TEST_CORRELATION_ID, TEST_EXTRA_OPTIONS, TEST_EXTRA_QUERY_STRING_PARAMETERS,
                        TEST_EXTRA_SCOPE, TEST_FRAGMENT, TEST_LOGIN_HINT,
                        TEST_OAUTH_2_TOKEN_CACHE, TEST_REDIRECT_URI, TEST_REQUEST_HEADERS, "3.0", "anSdkVersion",
                        TEST_SCOPES},
                {"consent_adal", true, true, true, true, true, true, OpenIdConnectPromptParameter.CONSENT, SdkType.ADAL, TEST_AUTHORITY_WITH_DC,
                        TEST_APPLICATION_NAME, TEST_APPLICATION_VERSION, BearerAuthenticationSchemeInternal.builder().build(),
                        AuthorizationAgent.BROWSER,
                        true, TEST_BROWSER_SAFE_LIST, TEST_ACCOUNT_RECORD, TEST_CLAIMS_JSON, TEST_CLIENT_ID,
                        TEST_CORRELATION_ID, TEST_EXTRA_OPTIONS, TEST_EXTRA_QUERY_STRING_PARAMETERS,
                        TEST_EXTRA_SCOPE, TEST_FRAGMENT, TEST_LOGIN_HINT,
                        TEST_OAUTH_2_TOKEN_CACHE, TEST_REDIRECT_URI, TEST_REQUEST_HEADERS, "3.0", "anSdkVersion",
                        TEST_SCOPES},
                {"select_account_msal", true, true, true, true, true, true, OpenIdConnectPromptParameter.SELECT_ACCOUNT, SdkType.MSAL, TEST_AUTHORITY_WITH_DC,
                        TEST_APPLICATION_NAME, TEST_APPLICATION_VERSION, BearerAuthenticationSchemeInternal.builder().build(),
                        AuthorizationAgent.BROWSER,
                        true, TEST_BROWSER_SAFE_LIST, TEST_ACCOUNT_RECORD, TEST_CLAIMS_JSON, TEST_CLIENT_ID,
                        TEST_CORRELATION_ID, TEST_EXTRA_OPTIONS, TEST_EXTRA_QUERY_STRING_PARAMETERS,
                        TEST_EXTRA_SCOPE, TEST_FRAGMENT, TEST_LOGIN_HINT,
                        TEST_OAUTH_2_TOKEN_CACHE, TEST_REDIRECT_URI, TEST_REQUEST_HEADERS, "3.0", "anSdkVersion",
                        TEST_SCOPES},
                {"null_query_params", true, true, true, true, true, true, OpenIdConnectPromptParameter.SELECT_ACCOUNT, SdkType.MSAL, TEST_AUTHORITY_WITH_DC,
                        TEST_APPLICATION_NAME, TEST_APPLICATION_VERSION, BearerAuthenticationSchemeInternal.builder().build(),
                        AuthorizationAgent.BROWSER,
                        true, TEST_BROWSER_SAFE_LIST, TEST_ACCOUNT_RECORD, TEST_CLAIMS_JSON, TEST_CLIENT_ID,
                        TEST_CORRELATION_ID, TEST_EXTRA_OPTIONS, null,
                        TEST_EXTRA_SCOPE, TEST_FRAGMENT, TEST_LOGIN_HINT,
                        TEST_OAUTH_2_TOKEN_CACHE, TEST_REDIRECT_URI, TEST_REQUEST_HEADERS, "3.0", "anSdkVersion",
                        TEST_SCOPES},
                {"null_options", true, true, true, true, true, true, OpenIdConnectPromptParameter.SELECT_ACCOUNT, SdkType.MSAL, TEST_AUTHORITY_WITH_DC,
                        TEST_APPLICATION_NAME, TEST_APPLICATION_VERSION, BearerAuthenticationSchemeInternal.builder().build(),
                        AuthorizationAgent.BROWSER,
                        true, TEST_BROWSER_SAFE_LIST, TEST_ACCOUNT_RECORD, TEST_CLAIMS_JSON, TEST_CLIENT_ID,
                        TEST_CORRELATION_ID, null, TEST_EXTRA_QUERY_STRING_PARAMETERS,
                        TEST_EXTRA_SCOPE, TEST_FRAGMENT, TEST_LOGIN_HINT,
                        TEST_OAUTH_2_TOKEN_CACHE, TEST_REDIRECT_URI, TEST_REQUEST_HEADERS, "3.0", "anSdkVersion",
                        TEST_SCOPES},
                {"null_params_and_options", true, true, true, true, true, true, OpenIdConnectPromptParameter.SELECT_ACCOUNT, SdkType.MSAL, TEST_AUTHORITY_WITH_DC,
                        TEST_APPLICATION_NAME, TEST_APPLICATION_VERSION, BearerAuthenticationSchemeInternal.builder().build(),
                        AuthorizationAgent.BROWSER,
                        true, TEST_BROWSER_SAFE_LIST, TEST_ACCOUNT_RECORD, TEST_CLAIMS_JSON, TEST_CLIENT_ID,
                        TEST_CORRELATION_ID, null, null,
                        TEST_EXTRA_SCOPE, TEST_FRAGMENT, TEST_LOGIN_HINT,
                        TEST_OAUTH_2_TOKEN_CACHE, TEST_REDIRECT_URI, TEST_REQUEST_HEADERS, "3.0", "anSdkVersion",
                        TEST_SCOPES},
                {"null_params_and_options_and_slice", true, true, true, true, true, true, OpenIdConnectPromptParameter.SELECT_ACCOUNT, SdkType.MSAL, TEST_AUTHORITY_WITH_NULL_SLICE,
                        TEST_APPLICATION_NAME, TEST_APPLICATION_VERSION, BearerAuthenticationSchemeInternal.builder().build(),
                        AuthorizationAgent.BROWSER,
                        true, TEST_BROWSER_SAFE_LIST, TEST_ACCOUNT_RECORD, TEST_CLAIMS_JSON, TEST_CLIENT_ID,
                        TEST_CORRELATION_ID, null, null,
                        TEST_EXTRA_SCOPE, TEST_FRAGMENT, TEST_LOGIN_HINT,
                        TEST_OAUTH_2_TOKEN_CACHE, TEST_REDIRECT_URI, TEST_REQUEST_HEADERS, "3.0", "anSdkVersion",
                        TEST_SCOPES},
                {"null_params_and_options", true, true, true, true, true, true, OpenIdConnectPromptParameter.SELECT_ACCOUNT, SdkType.MSAL, TEST_AUTHORITY_WITH_DC,
                        TEST_APPLICATION_NAME, TEST_APPLICATION_VERSION, BearerAuthenticationSchemeInternal.builder().build(),
                        AuthorizationAgent.BROWSER,
                        true, TEST_BROWSER_SAFE_LIST, TEST_ACCOUNT_RECORD, TEST_CLAIMS_JSON, TEST_CLIENT_ID,
                        TEST_CORRELATION_ID, null, null,
                        TEST_EXTRA_SCOPE, TEST_FRAGMENT, TEST_LOGIN_HINT,
                        TEST_OAUTH_2_TOKEN_CACHE, TEST_REDIRECT_URI, TEST_REQUEST_HEADERS, "3.0", "anSdkVersion",
                        TEST_SCOPES},
                {"false_force_refresh", false, true, true, true, true, true, OpenIdConnectPromptParameter.LOGIN, SdkType.ADAL, TEST_AUTHORITY_WITH_DC,
                        TEST_APPLICATION_NAME, TEST_APPLICATION_VERSION, BearerAuthenticationSchemeInternal.builder().build(),
                        AuthorizationAgent.BROWSER,
                        true, TEST_BROWSER_SAFE_LIST, TEST_ACCOUNT_RECORD, TEST_CLAIMS_JSON, TEST_CLIENT_ID,
                        TEST_CORRELATION_ID, TEST_EXTRA_OPTIONS, TEST_EXTRA_QUERY_STRING_PARAMETERS,
                        TEST_EXTRA_SCOPE, TEST_FRAGMENT, TEST_LOGIN_HINT,
                        TEST_OAUTH_2_TOKEN_CACHE, TEST_REDIRECT_URI, TEST_REQUEST_HEADERS, "3.0", "anSdkVersion",
                        TEST_SCOPES},
                {"false_null_task_affinity", true, false, true, true, true, true, OpenIdConnectPromptParameter.LOGIN, SdkType.ADAL, TEST_AUTHORITY_WITH_DC,
                        TEST_APPLICATION_NAME, TEST_APPLICATION_VERSION, BearerAuthenticationSchemeInternal.builder().build(),
                        AuthorizationAgent.BROWSER,
                        true, TEST_BROWSER_SAFE_LIST, TEST_ACCOUNT_RECORD, TEST_CLAIMS_JSON, TEST_CLIENT_ID,
                        TEST_CORRELATION_ID, TEST_EXTRA_OPTIONS, TEST_EXTRA_QUERY_STRING_PARAMETERS,
                        TEST_EXTRA_SCOPE, TEST_FRAGMENT, TEST_LOGIN_HINT,
                        TEST_OAUTH_2_TOKEN_CACHE, TEST_REDIRECT_URI, TEST_REQUEST_HEADERS, "3.0", "anSdkVersion",
                        TEST_SCOPES},
                {"false_is_shared", true, true, false, true, true, true, OpenIdConnectPromptParameter.LOGIN, SdkType.ADAL, TEST_AUTHORITY_WITH_DC,
                        TEST_APPLICATION_NAME, TEST_APPLICATION_VERSION, BearerAuthenticationSchemeInternal.builder().build(),
                        AuthorizationAgent.BROWSER,
                        true, TEST_BROWSER_SAFE_LIST, TEST_ACCOUNT_RECORD, TEST_CLAIMS_JSON, TEST_CLIENT_ID,
                        TEST_CORRELATION_ID, TEST_EXTRA_OPTIONS, TEST_EXTRA_QUERY_STRING_PARAMETERS,
                        TEST_EXTRA_SCOPE, TEST_FRAGMENT, TEST_LOGIN_HINT,
                        TEST_OAUTH_2_TOKEN_CACHE, TEST_REDIRECT_URI, TEST_REQUEST_HEADERS, "3.0", "anSdkVersion",
                        TEST_SCOPES},
                {"false_webview_zoom_control", true, true, true, false, true, true, OpenIdConnectPromptParameter.LOGIN, SdkType.ADAL, TEST_AUTHORITY_WITH_DC,
                        TEST_APPLICATION_NAME, TEST_APPLICATION_VERSION, BearerAuthenticationSchemeInternal.builder().build(),
                        AuthorizationAgent.BROWSER,
                        true, TEST_BROWSER_SAFE_LIST, TEST_ACCOUNT_RECORD, TEST_CLAIMS_JSON, TEST_CLIENT_ID,
                        TEST_CORRELATION_ID, TEST_EXTRA_OPTIONS, TEST_EXTRA_QUERY_STRING_PARAMETERS,
                        TEST_EXTRA_SCOPE, TEST_FRAGMENT, TEST_LOGIN_HINT,
                        TEST_OAUTH_2_TOKEN_CACHE, TEST_REDIRECT_URI, TEST_REQUEST_HEADERS, "3.0", "anSdkVersion",
                        TEST_SCOPES},
                {"false_webvire_zoom_enabled", true, true, true, true, false, true, OpenIdConnectPromptParameter.LOGIN, SdkType.ADAL, TEST_AUTHORITY_WITH_DC,
                        TEST_APPLICATION_NAME, TEST_APPLICATION_VERSION, BearerAuthenticationSchemeInternal.builder().build(),
                        AuthorizationAgent.BROWSER,
                        true, TEST_BROWSER_SAFE_LIST, TEST_ACCOUNT_RECORD, TEST_CLAIMS_JSON, TEST_CLIENT_ID,
                        TEST_CORRELATION_ID, TEST_EXTRA_OPTIONS, TEST_EXTRA_QUERY_STRING_PARAMETERS,
                        TEST_EXTRA_SCOPE, TEST_FRAGMENT, TEST_LOGIN_HINT,
                        TEST_OAUTH_2_TOKEN_CACHE, TEST_REDIRECT_URI, TEST_REQUEST_HEADERS, "3.0", "anSdkVersion",
                        TEST_SCOPES},
                {"false_is_power_opt_check", true, true, true, true, true, false, OpenIdConnectPromptParameter.LOGIN, SdkType.ADAL, TEST_AUTHORITY_WITH_DC,
                        TEST_APPLICATION_NAME, TEST_APPLICATION_VERSION, BearerAuthenticationSchemeInternal.builder().build(),
                        AuthorizationAgent.BROWSER,
                        true, TEST_BROWSER_SAFE_LIST, TEST_ACCOUNT_RECORD, TEST_CLAIMS_JSON, TEST_CLIENT_ID,
                        TEST_CORRELATION_ID, TEST_EXTRA_OPTIONS, TEST_EXTRA_QUERY_STRING_PARAMETERS,
                        TEST_EXTRA_SCOPE, TEST_FRAGMENT, TEST_LOGIN_HINT,
                        TEST_OAUTH_2_TOKEN_CACHE, TEST_REDIRECT_URI, TEST_REQUEST_HEADERS, "3.0", "anSdkVersion",
                        TEST_SCOPES},
                {"different_sdk_version", true, true, true, true, true, true, OpenIdConnectPromptParameter.LOGIN, SdkType.MSAL, TEST_AUTHORITY_WITH_DC,
                        TEST_APPLICATION_NAME, TEST_APPLICATION_VERSION, BearerAuthenticationSchemeInternal.builder().build(),
                        AuthorizationAgent.BROWSER,
                        true, TEST_BROWSER_SAFE_LIST, TEST_ACCOUNT_RECORD, TEST_CLAIMS_JSON, TEST_CLIENT_ID,
                        TEST_CORRELATION_ID, TEST_EXTRA_OPTIONS, TEST_EXTRA_QUERY_STRING_PARAMETERS,
                        TEST_EXTRA_SCOPE, TEST_FRAGMENT, TEST_LOGIN_HINT,
                        TEST_OAUTH_2_TOKEN_CACHE, TEST_REDIRECT_URI, TEST_REQUEST_HEADERS, "3.0", "anSdkVersion2",
                        TEST_SCOPES},
                {"slice_no_qp", true, true, true, true, true, true, OpenIdConnectPromptParameter.LOGIN, SdkType.MSAL, TEST_AUTHORITY_WITH_SLICE,
                        TEST_APPLICATION_NAME, TEST_APPLICATION_VERSION, BearerAuthenticationSchemeInternal.builder().build(),
                        AuthorizationAgent.BROWSER,
                        true, TEST_BROWSER_SAFE_LIST, TEST_ACCOUNT_RECORD, TEST_CLAIMS_JSON, TEST_CLIENT_ID,
                        TEST_CORRELATION_ID, TEST_EXTRA_OPTIONS, TEST_EXTRA_QUERY_STRING_PARAMETERS,
                        TEST_EXTRA_SCOPE, TEST_FRAGMENT, TEST_LOGIN_HINT,
                        TEST_OAUTH_2_TOKEN_CACHE, TEST_REDIRECT_URI, TEST_REQUEST_HEADERS, "3.0", "anSdkVersion",
                        TEST_SCOPES},
                {"slice_and_dc_no_qp", true, true, true, true, true, true, OpenIdConnectPromptParameter.LOGIN, SdkType.MSAL, TEST_AUTHORITY_WITH_SLICE_DC,
                        TEST_APPLICATION_NAME, TEST_APPLICATION_VERSION, BearerAuthenticationSchemeInternal.builder().build(),
                        AuthorizationAgent.BROWSER,
                        true, TEST_BROWSER_SAFE_LIST, TEST_ACCOUNT_RECORD, TEST_CLAIMS_JSON, TEST_CLIENT_ID,
                        TEST_CORRELATION_ID, TEST_EXTRA_OPTIONS, TEST_EXTRA_QUERY_STRING_PARAMETERS,
                        TEST_EXTRA_SCOPE, TEST_FRAGMENT, TEST_LOGIN_HINT,
                        TEST_OAUTH_2_TOKEN_CACHE, TEST_REDIRECT_URI, TEST_REQUEST_HEADERS, "3.0", "anSdkVersion",
                        TEST_SCOPES},
                {"none_specified_slice_no_qp", true, true, true, true, true, true, OpenIdConnectPromptParameter.LOGIN, SdkType.MSAL, TEST_AUTHORITY_WITH_NONE_SLICE,
                        TEST_APPLICATION_NAME, TEST_APPLICATION_VERSION, BearerAuthenticationSchemeInternal.builder().build(),
                        AuthorizationAgent.BROWSER,
                        true, TEST_BROWSER_SAFE_LIST, TEST_ACCOUNT_RECORD, TEST_CLAIMS_JSON, TEST_CLIENT_ID,
                        TEST_CORRELATION_ID, TEST_EXTRA_OPTIONS, TEST_EXTRA_QUERY_STRING_PARAMETERS,
                        TEST_EXTRA_SCOPE, TEST_FRAGMENT, TEST_LOGIN_HINT,
                        TEST_OAUTH_2_TOKEN_CACHE, TEST_REDIRECT_URI, TEST_REQUEST_HEADERS, "3.0", "anSdkVersion",
                        TEST_SCOPES},
                {"no_slice_provided_no_qp", true, true, true, true, true, true, OpenIdConnectPromptParameter.LOGIN, SdkType.MSAL, TEST_AUTHORITY_WITHOUT_SLICE,
                        TEST_APPLICATION_NAME, TEST_APPLICATION_VERSION, BearerAuthenticationSchemeInternal.builder().build(),
                        AuthorizationAgent.BROWSER,
                        true, TEST_BROWSER_SAFE_LIST, TEST_ACCOUNT_RECORD, TEST_CLAIMS_JSON, TEST_CLIENT_ID,
                        TEST_CORRELATION_ID, TEST_EXTRA_OPTIONS, TEST_EXTRA_QUERY_STRING_PARAMETERS,
                        TEST_EXTRA_SCOPE, TEST_FRAGMENT, TEST_LOGIN_HINT,
                        TEST_OAUTH_2_TOKEN_CACHE, TEST_REDIRECT_URI, TEST_REQUEST_HEADERS, "3.0", "anSdkVersion",
                        TEST_SCOPES},
                {"null_slice_provided_no_qp", true, true, true, true, true, true, OpenIdConnectPromptParameter.LOGIN, SdkType.MSAL, TEST_AUTHORITY_WITH_NULL_SLICE,
                        TEST_APPLICATION_NAME, TEST_APPLICATION_VERSION, BearerAuthenticationSchemeInternal.builder().build(),
                        AuthorizationAgent.BROWSER,
                        true, TEST_BROWSER_SAFE_LIST, TEST_ACCOUNT_RECORD, TEST_CLAIMS_JSON, TEST_CLIENT_ID,
                        TEST_CORRELATION_ID, TEST_EXTRA_OPTIONS, TEST_EXTRA_QUERY_STRING_PARAMETERS,
                        TEST_EXTRA_SCOPE, TEST_FRAGMENT, TEST_LOGIN_HINT,
                        TEST_OAUTH_2_TOKEN_CACHE, TEST_REDIRECT_URI, TEST_REQUEST_HEADERS, "3.0", "anSdkVersion",
                        TEST_SCOPES},
                {"no_slice_provided_has_qp", true, true, true, true, true, true, OpenIdConnectPromptParameter.LOGIN, SdkType.MSAL, TEST_AUTHORITY_WITHOUT_SLICE,
                        TEST_APPLICATION_NAME, TEST_APPLICATION_VERSION, BearerAuthenticationSchemeInternal.builder().build(),
                        AuthorizationAgent.BROWSER,
                        true, TEST_BROWSER_SAFE_LIST, TEST_ACCOUNT_RECORD, TEST_CLAIMS_JSON, TEST_CLIENT_ID,
                        TEST_CORRELATION_ID, TEST_EXTRA_OPTIONS, TEST_EXTRA_QUERY_STRING_PARAMETERS_SLICE,
                        TEST_EXTRA_SCOPE, TEST_FRAGMENT, TEST_LOGIN_HINT,
                        TEST_OAUTH_2_TOKEN_CACHE, TEST_REDIRECT_URI, TEST_REQUEST_HEADERS, "3.0", "anSdkVersion",
                        TEST_SCOPES},
        });
    }

    @Test
    public void testRequestGeneration() {
        parameterizedTransmissionTest(forceRefresh, handleNullTaskAffinity, isSharedDevice, isWebViewZoomControlsEnabled, isWebViewZoomEnabled, powerOptCheckEnabled, promptParameter, sdkType, testAuthority, testApplicationName, testApplicationVersion, authenticationScheme, authorizationAgent, brokerBrowserSupportEnabled, testBrowserSafeList, testAccountRecord, testClaimsJson, testClientId, testCorrelationId, testExtraOptions, testExtraQueryStringParameters, testExtraScope, testFragment, testLoginHint, testOauth2TokenCache, testRedirectUri, testRequestHeaders, requiredBrokerProtocolVersion, sdkVersion, testScopes);
    }

    private void parameterizedTransmissionTest(boolean forceRefresh, boolean handleNullTaskAffinity, boolean isSharedDevice, boolean isWebViewZoomControlsEnabled, boolean isWebViewZoomEnabled, boolean powerOptCheckEnabled, OpenIdConnectPromptParameter promptParameter, SdkType sdkType, Authority testAuthority, String testApplicationName, String testApplicationVersion, AbstractAuthenticationScheme authenticationScheme, AuthorizationAgent authorizationAgent, boolean brokerBrowserSupportEnabled, List<BrowserDescriptor> testBrowserSafeList, IAccountRecord testAccountRecord, String testClaimsJson, String testClientId, String testCorrelationId, List<Pair<String, String>> testExtraOptions, List<Pair<String, String>> testExtraQueryStringParameters, List<String> testExtraScope, Fragment testFragment, String testLoginHint, OAuth2TokenCache testOauth2TokenCache, String testRedirectUri, HashMap<String, String> testRequestHeaders, String requiredBrokerProtocolVersion, String sdkVersion, Set<String> testScopes) {
        MsalBrokerRequestAdapter adapter = new MsalBrokerRequestAdapter();
        final List<Pair<String, String>> testExtraQueryStringParametersCopy = testExtraQueryStringParameters == null ? null : new ArrayList<>(testExtraQueryStringParameters);
        InteractiveTokenCommandParameters params = InteractiveTokenCommandParameters.builder()
                .authority(testAuthority)
                .activity(new Activity())
                .applicationName(testApplicationName)
                .applicationVersion(testApplicationVersion)
                .authenticationScheme(authenticationScheme)
                .authorizationAgent(authorizationAgent)
                .brokerBrowserSupportEnabled(brokerBrowserSupportEnabled)
                .browserSafeList(testBrowserSafeList)
                .account(testAccountRecord)
                .claimsRequestJson(testClaimsJson)
                .clientId(testClientId)
                .correlationId(testCorrelationId)
                .extraOptions(testExtraOptions)
                .extraQueryStringParameters(testExtraQueryStringParametersCopy)
                .extraScopesToConsent(testExtraScope)
                .forceRefresh(forceRefresh)
                .fragment(testFragment)
                .handleNullTaskAffinity(handleNullTaskAffinity)
                .isSharedDevice(isSharedDevice)
                .isWebViewZoomControlsEnabled(isWebViewZoomControlsEnabled)
                .isWebViewZoomEnabled(isWebViewZoomEnabled)
                .loginHint(testLoginHint)
                .oAuth2TokenCache(testOauth2TokenCache)
                .powerOptCheckEnabled(powerOptCheckEnabled)
                .prompt(promptParameter)
                .redirectUri(testRedirectUri)
                .requestHeaders(testRequestHeaders)
                .requiredBrokerProtocolVersion(requiredBrokerProtocolVersion)
                .sdkType(sdkType)
                .sdkVersion(sdkVersion)
                .scopes(testScopes)
                .build();
        BrokerRequest brokerRequest = adapter.brokerRequestFromAcquireTokenParameters(params);
        Activity mockActivity = Robolectric.buildActivity(Activity.class).get();
        BrokerInteractiveTokenCommandParameters out = adapter.brokerInteactiveParametersFromBrokerRequest(mockActivity, 0, "3.0",
                brokerRequest);
        Assert.assertEquals(testScopes, out.getScopes());
        Assert.assertEquals(null, out.getFragment());
        Assert.assertEquals(sdkType, out.getSdkType());
        final List<Pair<String, String>> slices = Optional.ofNullable(testExtraQueryStringParametersCopy).orElse(Collections.<Pair<String,String>>emptyList()).stream().filter(new Predicate<Pair<String, String>>() {
            public boolean test(Pair<String, String> p1) {
                return "slice".equals(p1.first);
            }
        }).collect(Collectors.<Pair<String,String>>toList());
        if (!slices.isEmpty()) {

        }
        final List<Pair<String, String>> dcs = Optional.ofNullable(testExtraQueryStringParametersCopy).orElse(Collections.<Pair<String,String>>emptyList()).stream().filter(new Predicate<Pair<String, String>>() {
            public boolean test(Pair<String, String> p1) {
                return "dc".equals(p1.first);
            }
        }).collect(Collectors.<Pair<String,String>>toList());
        if (!dcs.isEmpty()) {

        }
        if (slices.isEmpty() && dcs.isEmpty()) {
            if (testAuthority.getSlice() == null && out.getAuthority().getSlice() != null) {
                if (!((StringUtil.isEmpty(out.getAuthority().getSlice().getSlice()) && StringUtil.isEmpty(out.getAuthority().getSlice().getDC())))) {
                    throw new AssertionError("Slice mismatch");
                }
            } else {
                Assert.assertEquals(testAuthority, out.getAuthority());
                Assert.assertEquals(testAuthority.getSlice(), out.getAuthority().getSlice());
            }
        } else {
            testExtraQueryStringParametersCopy.removeAll(slices);
            testExtraQueryStringParametersCopy.removeAll(dcs);
            if (!slices.isEmpty()) {
                Assert.assertEquals(slices.get(slices.size() - 1).second, out.getAuthority().getSlice().getSlice());
            }
            if (!dcs.isEmpty()) {
                Assert.assertEquals(dcs.get(dcs.size() - 1).second, out.getAuthority().getSlice().getDC());
            }
        }
        Assert.assertEquals(testAuthority, out.getAuthority());
        Assert.assertEquals(testExtraQueryStringParametersCopy, out.getExtraQueryStringParameters());
        Assert.assertEquals(sdkVersion, out.getSdkVersion());
        Assert.assertEquals(testApplicationName, out.getApplicationName());
        Assert.assertEquals(testApplicationVersion, out.getApplicationVersion());
        Assert.assertEquals(testExtraOptions == null ? Collections.emptyList() : testExtraOptions, out.getExtraOptions());
        Assert.assertEquals(null, out.getBrowserSafeList());
        Assert.assertEquals(testClaimsJson, out.getClaimsRequestJson());
        Assert.assertEquals(testClientId, out.getClientId());
        Assert.assertEquals(testCorrelationId, out.getCorrelationId());
        Assert.assertEquals(testLoginHint, out.getLoginHint());
        Assert.assertEquals(null, out.getAccount());
    }

}
