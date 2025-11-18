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
package com.microsoft.identity.common.internal.controllers;

import static com.microsoft.identity.common.java.exception.ErrorStrings.UI_NOT_ALLOWED;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.gson.Gson;
import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.components.MockPlatformComponentsFactory;
import com.microsoft.identity.common.internal.broker.ipc.BrokerOperationBundle;
import com.microsoft.identity.common.internal.broker.ipc.IIpcStrategy;
import com.microsoft.identity.common.internal.broker.ipc.WebAppsAdditionalRequiredParameters;
import com.microsoft.identity.common.internal.result.MsalBrokerResultAdapter;
import com.microsoft.identity.common.java.authorities.Authority;
import com.microsoft.identity.common.java.cache.CacheRecord;
import com.microsoft.identity.common.java.cache.ICacheRecord;
import com.microsoft.identity.common.java.commands.AcquirePrtSsoTokenResult;
import com.microsoft.identity.common.java.commands.parameters.AcquirePrtSsoTokenCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.ResourceAccountCommandParameters;
import com.microsoft.identity.common.java.commands.webapps.WebAppsGetTokenSubOperationEnvelope;
import com.microsoft.identity.common.java.commands.webapps.WebAppsGetTokenSubOperationRequest;
import com.microsoft.identity.common.java.dto.AccountRecord;
import com.microsoft.identity.common.java.interfaces.IPlatformComponents;
import com.microsoft.identity.common.java.request.SdkType;
import com.microsoft.identity.common.java.util.ObjectMapper;
import com.microsoft.identity.common.shadows.ShadowAcquireTokenInternalBrokerMsalController;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Collections;
import java.util.Locale;

import lombok.SneakyThrows;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Build.VERSION_CODES.N}, shadows = {})
public class BrokerMsalControllerTest {

    private static final String NEGOTIATED_VERSION = "19.0";
    private static final String EXPECTED_RESULT = "{\"status\":\"ok\",\"payload\":\"test\"}";
    private static final String EXPECTED_ERROR_RESULT = "Error from the broker side";

    /**
     * This test simulates a result calling the PrtSsoToken Api where everything goes well talking
     * to the broker.
     */
    @Test
    public void testPrtSsoToken() throws Exception {
        final String anAccountName = "anAccountName";
        final String aHomeAccountId = "aHomeAccountId";
        final String aLocalAccountId = "aLocalAccountId";
        final String aClientId = "aClientId";
        final String aCorrelationId = "aCorrelationId";
        final String accountAuthority = "https://login.microsoft.com/anAuthority";
        final String ssoUrl = "https://a.url.that.we.need/that/has/a/path?one_useless_param&sso_nonce=aNonceToUse&anotherUselessParam=foo";
        final String aCookie = "aCookie";
        final IPlatformComponents components = MockPlatformComponentsFactory.getNonFunctionalBuilder().build();
        final IIpcStrategy strategy = new IIpcStrategy() {
            @Override
            public Bundle communicateToBroker(@NonNull BrokerOperationBundle bundle) {
                Bundle retBundle = new Bundle();
                if (bundle.getOperation().equals(BrokerOperationBundle.Operation.MSAL_HELLO)) {
                    retBundle.putString(AuthenticationConstants.Broker.NEGOTIATED_BP_VERSION_KEY, "7.0");
                } else if (bundle.getOperation().equals(BrokerOperationBundle.Operation.MSAL_SSO_TOKEN)) {
                    AcquirePrtSsoTokenResult result = AcquirePrtSsoTokenResult.builder()
                            .accountName(anAccountName)
                            .localAccountId(aLocalAccountId)
                            .homeAccountId(aHomeAccountId)
                            .accountAuthority(accountAuthority)
                            .cookieName("x-ms-RefreshTokenCredential")
                            .cookieContent(aCookie)
                            .telemetry(Collections.<String, Object>emptyMap())
                            .build();

                    retBundle.putString(AuthenticationConstants.Broker.BROKER_GENERATE_SSO_TOKEN_RESULT, new Gson().toJson(result));
                }
                return retBundle;
            }

            @Override
            public boolean isSupportedByTargetedBroker(@NonNull final String targetedBrokerPackageName) {
                return true;
            }

            @Override
            @NonNull
            public Type getType() {
                return Type.CONTENT_PROVIDER;
            }
        };

        final BrokerMsalController controller = new BrokerMsalController(
                InstrumentationRegistry.getInstrumentation().getContext(),
                components,
                "aBrokerPackage",
                Collections.singletonList(strategy));

        final AcquirePrtSsoTokenCommandParameters params = AcquirePrtSsoTokenCommandParameters.builder()
                .platformComponents(components)
                .correlationId(aCorrelationId)
                .accountName(anAccountName)
                .clientId(aClientId)
                .requestAuthority(accountAuthority)
                .ssoUrl(ssoUrl)
                .build();

        final AcquirePrtSsoTokenResult ssoTokenResult = controller.getSsoToken(params);
        Assert.assertEquals(accountAuthority, ssoTokenResult.getAccountAuthority());
        Assert.assertEquals(anAccountName, ssoTokenResult.getAccountName());
        Assert.assertEquals(aHomeAccountId, ssoTokenResult.getHomeAccountId());
        Assert.assertEquals(aLocalAccountId, ssoTokenResult.getLocalAccountId());
        Assert.assertEquals(aCookie, ssoTokenResult.getCookieContent());
        Assert.assertEquals("x-ms-RefreshTokenCredential", ssoTokenResult.getCookieName());
    }

    /**
     * This test simulates a result calling the ProvisionResourceAccount Api.
     */
    @SneakyThrows
    @Test
    public void testProvisionResourceAccount() {
        final String mockHomeAccountId = "mockHomeAccountId";
        final String mockCorrelationId = "mockCorrelationId";
        final String mockAuthorityStr = "https://login.microsoft.com/mockAuthority";
        final Authority mockAuthority = Authority.getAuthorityFromAuthorityUrl(mockAuthorityStr);
        final String mockNegotiatedBrokerVersion = "18.0";
        final String mockAccountName = "mockAccountName";
        final String mockClientId = "mockClientId";
        final String mockRedirectUri = "mockRedirectUri";
        final AccountRecord mockAccountRecord = new AccountRecord();
        mockAccountRecord.setHomeAccountId(mockHomeAccountId);
        mockAccountRecord.setUsername(mockAccountName);
        final CacheRecord mockCacheRecord = CacheRecord.builder()
                .account(mockAccountRecord)
                .build();
        final MsalBrokerResultAdapter resultAdapter = new MsalBrokerResultAdapter();
        final IIpcStrategy strategy = new IIpcStrategy() {
            @Override
            public Bundle communicateToBroker(@NonNull BrokerOperationBundle bundle) {
                Bundle retBundle = new Bundle();
                if (bundle.getOperation().equals(BrokerOperationBundle.Operation.MSAL_HELLO)) {
                    retBundle.putString(AuthenticationConstants.Broker.NEGOTIATED_BP_VERSION_KEY, mockNegotiatedBrokerVersion);
                } else if (bundle.getOperation().equals(BrokerOperationBundle.Operation.PROVISION_RESOURCE_ACCOUNT)) {
                    retBundle = resultAdapter.bundleFromAccounts(Collections.singletonList(mockCacheRecord), mockNegotiatedBrokerVersion);
                }
                return retBundle;
            }

            @Override
            public boolean isSupportedByTargetedBroker(@NonNull final String targetedBrokerPackageName) {
                return true;
            }

            @Override
            @NonNull
            public Type getType() {
                return Type.CONTENT_PROVIDER;
            }
        };

        final IPlatformComponents components = MockPlatformComponentsFactory.getNonFunctionalBuilder().build();
        final ResourceAccountCommandParameters parameters = ResourceAccountCommandParameters.builder()
                .platformComponents(components)
                .homeAccountId(mockHomeAccountId)
                .authority(mockAuthority)
                .correlationId(mockCorrelationId)
                .applicationName("mockApplicationName")
                .applicationVersion("mockApplicationVersion")
                .sdkVersion("mockSdkVersion")
                .sdkType(SdkType.MSAL)
                .clientId(mockClientId)
                .redirectUri(mockRedirectUri)
                .requiredBrokerProtocolVersion(mockNegotiatedBrokerVersion)
                .build();

        final BrokerMsalController controller = new BrokerMsalController(
                InstrumentationRegistry.getInstrumentation().getContext(),
                components,
                "aBrokerPackage",
                Collections.singletonList(strategy));

        final ICacheRecord cacheRecord = controller.provisionResourceAccount(parameters);

        // verify the cache record
        Assert.assertEquals(mockHomeAccountId, cacheRecord.getAccount().getHomeAccountId());
        Assert.assertEquals(mockAccountName, cacheRecord.getAccount().getUsername());
    }

    @Test
    public void testExecuteWebAppRequest_SilentSuccess_MSALJS() throws Exception {
        final BrokerMsalController controller = createController(buildStrategyForSilentSuccess());
        final String requestJson = buildStrictlySilentGetTokenRequestJson(false);
        final WebAppsAdditionalRequiredParameters addParams = buildAdditionalParams(false);

        final String result = controller.executeWebAppRequest(
                requestJson,
                "19.0",
                addParams
        );

        Assert.assertEquals(EXPECTED_RESULT, result);
    }

    @Test
    public void testExecuteWebAppRequest_SilentSuccess_ESTS() throws Exception {
        final BrokerMsalController controller = createController(buildStrategyForSilentSuccess());
        final String requestJson = buildStrictlySilentGetTokenRequestJson(true);
        final WebAppsAdditionalRequiredParameters addParams = buildAdditionalParams(false);

        final String result = controller.executeWebAppRequest(
                requestJson,
                "19.0",
                addParams
        );

        Assert.assertEquals(EXPECTED_RESULT, result);
    }

    @Test
    public void testExecuteWebAppRequest_SilentError_FromBroker() throws Exception {
        final BrokerMsalController controller = createController(buildStrategyForSilentErrorFromBroker());
        final String requestJson = buildStrictlySilentGetTokenRequestJson(false);
        final WebAppsAdditionalRequiredParameters addParams = buildAdditionalParams(false);

        final String result = controller.executeWebAppRequest(
                requestJson,
                "19.0",
                addParams
        );

        Assert.assertTrue(result.contains(EXPECTED_ERROR_RESULT));
    }

    @Test
    public void testExecuteWebAppRequest_SilentError_FromController() throws Exception {
        final BrokerMsalController controller = createController(buildStrategyForSilentErrorFromBroker());
        final WebAppsAdditionalRequiredParameters addParams = buildAdditionalParams(false);
        final String malformedRequestJson = new JSONObject()
                .put("request", "malformed_request")
                .toString();
        final String result = controller.executeWebAppRequest(
                malformedRequestJson,
                "19.0",
                addParams
        );

        Assert.assertTrue(result.contains("Error occurred during request parsing"));
    }

    @Test
    public void testExecuteWebAppRequest_SilentError_UiNotAllowed() throws Exception {
        final BrokerMsalController controller = createController(buildStrategyForSilentErrorFromBroker());
        final WebAppsGetTokenSubOperationRequest request = new WebAppsGetTokenSubOperationRequest(
                "account-id",
                "clientId",
                "https://login.microsoftonline.com/common",
                "User.Read",
                "https://redirect",
                "corr-id",
                "login", // Setting to login
                false,
                null,
                null,
                null,
                false,
                null
        );

        WebAppsGetTokenSubOperationEnvelope envelope =
                new WebAppsGetTokenSubOperationEnvelope(
                        "GetToken",
                        request,
                        "https://login.microsoftonline.com" // sender
                );

        final String requestJson = ObjectMapper.serializeObjectToJsonString(envelope);
        final WebAppsAdditionalRequiredParameters addParams = buildAdditionalParams(false);

        final String result = controller.executeWebAppRequest(
                requestJson,
                "19.0",
                addParams
        );

        Assert.assertTrue(result.contains(UI_NOT_ALLOWED.toUpperCase(Locale.ROOT)));
    }

    @Test
    public void testExecuteWebAppRequest_SilentError_UiNotAllowed_ESTS() throws Exception {
        final BrokerMsalController controller = createController(buildStrategyForSilentErrorFromBroker());
        final WebAppsGetTokenSubOperationRequest request = new WebAppsGetTokenSubOperationRequest(
                "account-id",
                "clientId",
                "https://login.microsoftonline.com/common",
                "User.Read",
                "https://redirect",
                "corr-id",
                "login", // Setting to login
                true,
                null,
                null,
                null,
                false,
                null
        );

        WebAppsGetTokenSubOperationEnvelope envelope =
                new WebAppsGetTokenSubOperationEnvelope(
                        "GetToken",
                        request,
                        "https://login.microsoftonline.com" // sender
                );

        final String requestJson = ObjectMapper.serializeObjectToJsonString(envelope);
        final WebAppsAdditionalRequiredParameters addParams = buildAdditionalParams(false);

        final String result = controller.executeWebAppRequest(
                requestJson,
                "19.0",
                addParams
        );

        Assert.assertTrue(result.contains(UI_NOT_ALLOWED.toUpperCase(Locale.ROOT)));
    }


    @Test
    @Config(sdk = {Build.VERSION_CODES.N}, shadows = {ShadowAcquireTokenInternalBrokerMsalController.class})
    public void testExecuteWebAppRequest_ForceInteractive_Success() throws Exception {

        final BrokerMsalController controller = createController(buildStrategyForInteractiveSuccessFromBroker());
        final String requestJson = buildInteractiveGetTokenRequestJson(true);

        WebAppsAdditionalRequiredParameters addParams = new WebAppsAdditionalRequiredParameters(
                true,               // canShowUi
                "test.app.package",
                "Mock App",
                "1.2.3",
                SdkType.MSAL_CPP,
                "1.2.3"
        );

        // Queue interactive result for shadowed acquireTokenInternal.
        Bundle interactiveBundle = new Bundle();
        interactiveBundle.putString(AuthenticationConstants.Broker.BROKER_WEB_APPS_SUCCESSFUL_RESULT, EXPECTED_RESULT);
        ShadowAcquireTokenInternalBrokerMsalController.enqueueResult(interactiveBundle);

        String result = controller.executeWebAppRequest(requestJson, "19.0", addParams);

        Assert.assertEquals(EXPECTED_RESULT, result);
    }

    @Test
    @Config(sdk = {Build.VERSION_CODES.N}, shadows = {ShadowAcquireTokenInternalBrokerMsalController.class})
    public void testExecuteWebAppRequest_SilentFallbackToInteractive_MSALJS() throws Exception {

        final BrokerMsalController controller = createController(buildStrategyForSilentErrorFromBroker());
        final String requestJson = buildFallbackSilentGetTokenRequestJson(false);

        WebAppsAdditionalRequiredParameters addParams = new WebAppsAdditionalRequiredParameters(
                true,               // canShowUi
                "test.app.package",
                "Mock App",
                "1.2.3",
                SdkType.MSAL_CPP,
                "1.2.3"
        );

        // Queue interactive result for shadowed acquireTokenInternal.
        Bundle interactiveBundle = new Bundle();
        interactiveBundle.putString(AuthenticationConstants.Broker.BROKER_WEB_APPS_SUCCESSFUL_RESULT, EXPECTED_RESULT);
        ShadowAcquireTokenInternalBrokerMsalController.enqueueResult(interactiveBundle);

        String result = controller.executeWebAppRequest(requestJson, "19.0", addParams);

        Assert.assertEquals(EXPECTED_RESULT, result);
    }

    @NonNull
    private static String buildInteractiveGetTokenRequestJson(final boolean isSts) {
        WebAppsGetTokenSubOperationRequest req = new WebAppsGetTokenSubOperationRequest(
                null,                                  // homeAccountId (null -> STS flow)
                "clientId",
                "https://login.microsoftonline.com/common",
                "User.Read",
                "https://demoapp.com/",
                "corr-id",
                "login",                               // prompt forces interactive
                isSts,                                  // isSecurityTokenService
                null,
                null,
                null,
                false,
                null
        );

        WebAppsGetTokenSubOperationEnvelope envelope = new WebAppsGetTokenSubOperationEnvelope(
                "GetToken",
                req,
                isSts ? "https://login.microsoftonline.com" : "https://demoapp.com"
        );
        return ObjectMapper.serializeObjectToJsonString(envelope);
    }

    private String buildStrictlySilentGetTokenRequestJson(final boolean isSts) throws Exception {
        WebAppsGetTokenSubOperationRequest request = new WebAppsGetTokenSubOperationRequest(
                "account-id",                // homeAccountId
                "clientId",          // clientId (required)
                "https://login.microsoftonline.com/common", // authority
                "User.Read",         // scopes
                "https://demoapp.com/",  // redirectUri (required)
                "corr-id",           // correlationId (optional)
                "none",              // prompt ("none" for silent)
                isSts,               // isSecurityTokenService
                null,                // nonce
                null,                // state
                null,                // loginHint
                false,               // instanceAware
                null                 // extraParameters
        );

        WebAppsGetTokenSubOperationEnvelope envelope =
                new WebAppsGetTokenSubOperationEnvelope(
                        "GetToken",
                        request,
                        isSts ? "https://login.microsoftonline.com" : "https://demoapp.com" // sender
                );

        return ObjectMapper.serializeObjectToJsonString(envelope);
    }

    private String buildFallbackSilentGetTokenRequestJson(final boolean isSts) throws Exception {
        WebAppsGetTokenSubOperationRequest request = new WebAppsGetTokenSubOperationRequest(
                "account-id",                // homeAccountId
                "clientId",          // clientId (required)
                "https://login.microsoftonline.com/common", // authority
                "User.Read",         // scopes
                "https://demoapp.com/",  // redirectUri (required)
                "corr-id",           // correlationId (optional)
                "select_account",    // prompt
                isSts,               // isSecurityTokenService
                null,                // nonce
                null,                // state
                null,                // loginHint
                false,               // instanceAware
                null                 // extraParameters
        );

        WebAppsGetTokenSubOperationEnvelope envelope =
                new WebAppsGetTokenSubOperationEnvelope(
                        "GetToken",
                        request,
                        isSts ? "https://login.microsoftonline.com" : "https://demoapp.com" // sender
                );

        return ObjectMapper.serializeObjectToJsonString(envelope);
    }

    private WebAppsAdditionalRequiredParameters buildAdditionalParams(boolean canShowUi) {
        // Replace with real builder/factory as needed
        return new WebAppsAdditionalRequiredParameters(
                canShowUi,
                "test.app.package",
                "Mock App",
                "1.2.3",
                SdkType.MSAL_CPP,
                "1.2.3"
        );
    }

    private BrokerMsalController createController(IIpcStrategy strategy) {
        IPlatformComponents components = MockPlatformComponentsFactory.getNonFunctionalBuilder().build();
        return new BrokerMsalController(
                InstrumentationRegistry.getInstrumentation().getContext(),
                components,
                "test.app.package",
                java.util.Collections.singletonList(strategy)
        );
    }

    private IIpcStrategy buildStrategyForSilentSuccess() {
        return new IIpcStrategy() {
            @Override
            public Bundle communicateToBroker(@NonNull BrokerOperationBundle bundle) {
                Bundle out = new Bundle();
                if (bundle.getOperation() == BrokerOperationBundle.Operation.MSAL_HELLO) {
                    out.putString(AuthenticationConstants.Broker.NEGOTIATED_BP_VERSION_KEY, NEGOTIATED_VERSION);
                } else if (bundle.getOperation() == BrokerOperationBundle.Operation.BROKER_WEBAPPS_API_EXECUTE_WEB_APPS_REQUEST) {
                    // Simulate successful broker execution response
                    out.putString(AuthenticationConstants.Broker.NEGOTIATED_BP_VERSION_KEY, NEGOTIATED_VERSION);
                    // Adapter will read whatever key it expects (replace with actual key if different)
                    out.putString(AuthenticationConstants.Broker.BROKER_WEB_APPS_SUCCESSFUL_RESULT, EXPECTED_RESULT);
                }
                return out;
            }

            @Override
            public boolean isSupportedByTargetedBroker(@NonNull String targetedBrokerPackageName) {
                return true;
            }

            @NonNull
            @Override
            public Type getType() {
                return Type.CONTENT_PROVIDER;
            }
        };
    }

    private IIpcStrategy buildStrategyForSilentErrorFromBroker() {
        return new IIpcStrategy() {
            @Override
            public Bundle communicateToBroker(@NonNull BrokerOperationBundle bundle) {
                Bundle out = new Bundle();
                if (bundle.getOperation() == BrokerOperationBundle.Operation.MSAL_HELLO) {
                    out.putString(AuthenticationConstants.Broker.NEGOTIATED_BP_VERSION_KEY, NEGOTIATED_VERSION);
                } else if (bundle.getOperation() == BrokerOperationBundle.Operation.BROKER_WEBAPPS_API_EXECUTE_WEB_APPS_REQUEST) {
                    out.putString(AuthenticationConstants.Broker.NEGOTIATED_BP_VERSION_KEY, NEGOTIATED_VERSION);
                    out.putString(AuthenticationConstants.Broker.BROKER_WEB_APPS_ERROR_RESULT, EXPECTED_ERROR_RESULT);
                } else if (bundle.getOperation() == BrokerOperationBundle.Operation.MSAL_GET_INTENT_FOR_INTERACTIVE_REQUEST) {
                    out.putString(AuthenticationConstants.Broker.NEGOTIATED_BP_VERSION_KEY, NEGOTIATED_VERSION);
                    // Minimal placeholder intent
                    Intent interactive = new Intent("com.microsoft.identity.test.INTERACTIVE");
                    out.putParcelable("intent", interactive);
                }
                return out;
            }

            @Override
            public boolean isSupportedByTargetedBroker(@NonNull String targetedBrokerPackageName) {
                return true;
            }

            @NonNull
            @Override
            public Type getType() {
                return Type.CONTENT_PROVIDER;
            }
        };
    }

    private IIpcStrategy buildStrategyForInteractiveSuccessFromBroker() {
        return new IIpcStrategy() {
            @Override
            public Bundle communicateToBroker(@NonNull BrokerOperationBundle bundle) {
                Bundle out = new Bundle();
                if (bundle.getOperation() == BrokerOperationBundle.Operation.MSAL_HELLO) {
                    out.putString(AuthenticationConstants.Broker.NEGOTIATED_BP_VERSION_KEY, NEGOTIATED_VERSION);
                } else if (bundle.getOperation() == BrokerOperationBundle.Operation.MSAL_GET_INTENT_FOR_INTERACTIVE_REQUEST) {
                    out.putString(AuthenticationConstants.Broker.NEGOTIATED_BP_VERSION_KEY, NEGOTIATED_VERSION);
                    // Minimal placeholder intent
                    Intent interactive = new Intent("com.microsoft.identity.test.INTERACTIVE");
                    out.putParcelable("intent", interactive);
                }
                return out;
            }

            @Override
            public boolean isSupportedByTargetedBroker(@NonNull String targetedBrokerPackageName) {
                return true;
            }

            @NonNull
            @Override
            public Type getType() {
                return Type.CONTENT_PROVIDER;
            }
        };
    }
}
