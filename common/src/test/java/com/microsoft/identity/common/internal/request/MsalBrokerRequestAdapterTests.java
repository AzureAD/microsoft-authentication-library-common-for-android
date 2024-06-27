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
package com.microsoft.identity.common.internal.request;

import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.ACCOUNT_CORRELATIONID;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.REQUEST_AUTHORITY;

import android.os.Bundle;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.components.MockPlatformComponentsFactory;
import com.microsoft.identity.common.java.commands.parameters.AcquirePrtSsoTokenCommandParameters;
import com.microsoft.identity.common.java.interfaces.IPlatformComponents;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class MsalBrokerRequestAdapterTests {

    @Test
    public void test_getRequestBundleForSsoToken() {
        final String anAccountName = "anAccountName";
        final String aHomeAccountId = "aHomeAccountId";
        final String aLocalAccountId = "aLocalAccountId";
        final String aClientId = "aClientId";
        final String aCorrelationId = "aCorrelationId";
        final String accountAuthority = "https://login.microsoft.com/anAuthority";
        final String ssoUrl = "https://a.url.that.we.need/that/has/a/path?one_useless_param&sso_nonce=aNonceToUse&anotherUselessParam=foo";
        final String negotiatedBrokerProtocolVersion = "1.0";

        final IPlatformComponents components = MockPlatformComponentsFactory.getNonFunctionalBuilder().build();
        final AcquirePrtSsoTokenCommandParameters params = AcquirePrtSsoTokenCommandParameters.builder()
                .platformComponents(components)
                .correlationId(aCorrelationId)
                .accountName(anAccountName)
                .homeAccountId(aHomeAccountId)
                .localAccountId(aLocalAccountId)
                .clientId(aClientId)
                .requestAuthority(accountAuthority)
                .ssoUrl(ssoUrl)
                .build();

        MsalBrokerRequestAdapter msalBrokerRequestAdapter = new MsalBrokerRequestAdapter();
        Bundle requestBundle = msalBrokerRequestAdapter.getRequestBundleForSsoToken(params, negotiatedBrokerProtocolVersion);

        Assert.assertEquals(anAccountName, requestBundle.getString(AuthenticationConstants.Broker.ACCOUNT_NAME));
        Assert.assertEquals(aHomeAccountId, requestBundle.getString(AuthenticationConstants.Broker.ACCOUNT_HOME_ACCOUNT_ID));
        Assert.assertEquals(aLocalAccountId, requestBundle.getString(AuthenticationConstants.Broker.ACCOUNT_LOCAL_ACCOUNT_ID));
        Assert.assertEquals(aClientId, requestBundle.getString(AuthenticationConstants.Broker.SSO_TOKEN_CLIENT_ID));
        Assert.assertEquals(accountAuthority, requestBundle.getString(REQUEST_AUTHORITY));
        Assert.assertEquals(ssoUrl, requestBundle.getString(AuthenticationConstants.Broker.BROKER_SSO_URL_KEY));
        Assert.assertEquals(negotiatedBrokerProtocolVersion, requestBundle.getString(AuthenticationConstants.Broker.NEGOTIATED_BP_VERSION_KEY));
        Assert.assertEquals(aCorrelationId, requestBundle.getString(ACCOUNT_CORRELATIONID));
    }
}
