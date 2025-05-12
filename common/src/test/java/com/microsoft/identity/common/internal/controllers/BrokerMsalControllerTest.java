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

import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.gson.Gson;
import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.components.MockPlatformComponentsFactory;
import com.microsoft.identity.common.internal.broker.ipc.BrokerOperationBundle;
import com.microsoft.identity.common.internal.broker.ipc.IIpcStrategy;
import com.microsoft.identity.common.internal.result.MsalBrokerResultAdapter;
import com.microsoft.identity.common.java.authorities.Authority;
import com.microsoft.identity.common.java.cache.CacheRecord;
import com.microsoft.identity.common.java.cache.ICacheRecord;
import com.microsoft.identity.common.java.commands.AcquirePrtSsoTokenResult;
import com.microsoft.identity.common.java.commands.parameters.AcquirePrtSsoTokenCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.ResourceAccountCommandParameters;
import com.microsoft.identity.common.java.dto.AccountRecord;
import com.microsoft.identity.common.java.interfaces.IPlatformComponents;
import com.microsoft.identity.common.java.request.SdkType;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Collections;

import lombok.SneakyThrows;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Build.VERSION_CODES.N}, shadows = {})
public class BrokerMsalControllerTest {
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
}
