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

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.microsoft.identity.common.components.AndroidPlatformComponentsFactory;
import com.microsoft.identity.common.java.util.BrokerProtocolVersionUtil;
import com.microsoft.identity.common.java.authorities.Authority;
import com.microsoft.identity.common.java.authscheme.BearerAuthenticationSchemeInternal;
import com.microsoft.identity.common.java.cache.MsalOAuth2TokenCache;
import com.microsoft.identity.common.java.commands.parameters.RopcTokenCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.SilentTokenCommandParameters;
import com.microsoft.identity.common.java.controllers.BaseController;
import com.microsoft.identity.common.java.dto.IAccountRecord;
import com.microsoft.identity.common.java.interfaces.IPlatformComponents;
import com.microsoft.identity.common.java.providers.oauth2.IDToken;
import com.microsoft.identity.common.java.request.SdkType;
import com.microsoft.identity.common.java.result.AcquireTokenResult;
import com.microsoft.identity.common.java.result.ILocalAuthenticationResult;
import com.microsoft.identity.labapi.utilities.BuildConfig;
import com.microsoft.identity.labapi.utilities.authentication.LabApiAuthenticationClient;
import com.microsoft.identity.labapi.utilities.client.ILabAccount;
import com.microsoft.identity.labapi.utilities.client.ILabClient;
import com.microsoft.identity.labapi.utilities.client.LabClient;
import com.microsoft.identity.labapi.utilities.client.LabQuery;
import com.microsoft.identity.labapi.utilities.exception.LabApiException;
import com.microsoft.identity.labapi.utilities.jwt.IJWTParser;
import com.microsoft.identity.labapi.utilities.jwt.JWTParserFactory;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import lombok.NonNull;

/**
 * Tests for {@link LocalMSALController}.
 */
@RunWith(RobolectricTestRunner.class)
public class LocalMsalControllerTest {
    private static final ILabClient sLabClient = new LabClient(
            new LabApiAuthenticationClient(BuildConfig.LAB_CLIENT_SECRET)
    );

    private static ILabAccount sTestAccount;

    private static final String AUTHORITY_URL = "https://login.microsoftonline.com/organizations";

    private static final String CLIENT_ID = "4b0db8c2-9f26-4417-8bde-3f0e3656f8e0";

    private static final String APPLICATION_IDENTIFIER = "unset/unset";

    private static final String REDIRECT_URI = "msauth://com.msft.identity.client.sample.local/1wIqXSqBj7w%2Bh11ZifsnqwgyKrY%3D";

    private static final String SCOPE = "User.read";

    private IPlatformComponents mPlatformComponents;
    private final BaseController mController = new LocalMSALController();
    private final IJWTParser mJwtParser = JWTParserFactory.INSTANCE.getJwtParser();

    @BeforeClass
    public static void setupClass() throws LabApiException {
        final LabQuery query = LabQuery.builder().build();
        sTestAccount = sLabClient.getLabAccount(query);
    }

    @Before
    public void setup() {
        final Context context = ApplicationProvider.getApplicationContext();
        mPlatformComponents = AndroidPlatformComponentsFactory.createFromContext(
                context
        );
    }

    @Ignore("Ignoring as the maven pipeline doesn't lab cert/secret enabled currently.")
    @Test
    public void testCanGetTokenViaRopc() throws Exception {
        acquireTokenUsingRopc();
    }

    @Ignore("Ignoring as the maven pipeline doesn't lab cert/secret enabled currently.")
    @Test
    public void testCanGetTokenSilentlyAfterPerformingRopc() throws Exception {
        final ILocalAuthenticationResult result1 = acquireTokenUsingRopc();
        final ILocalAuthenticationResult result2 = acquireTokenSilently(result1.getAccountRecord());

        // token acquired silently should be same as one acquired via ROPC
        // because we should be getting it out of cache
        Assert.assertTrue(result2.isServicedFromCache());
        Assert.assertEquals(result1.getAccessToken(), result2.getAccessToken());
        Assert.assertEquals(result1.getIdToken(), result2.getIdToken());
    }

    private ILocalAuthenticationResult acquireTokenUsingRopc() throws Exception {
        final RopcTokenCommandParameters tokenCommandParameters = createRopcCommandParameters();
        final AcquireTokenResult acquireTokenResult = mController.acquireTokenWithPassword(tokenCommandParameters);
        performAssertionsOnTokenResult(acquireTokenResult);
        return acquireTokenResult.getLocalAuthenticationResult();
    }

    private ILocalAuthenticationResult acquireTokenSilently(@NonNull final IAccountRecord accountRecord) throws Exception {
        final SilentTokenCommandParameters tokenCommandParameters = createSilentTokenCommandParameters(accountRecord);
        final AcquireTokenResult acquireTokenResult = mController.acquireTokenSilent(tokenCommandParameters);
        performAssertionsOnTokenResult(acquireTokenResult);
        return acquireTokenResult.getLocalAuthenticationResult();
    }

    private void performAssertionsOnTokenResult(@NonNull final AcquireTokenResult acquireTokenResult) {
        Assert.assertNotNull(acquireTokenResult);
        Assert.assertTrue(acquireTokenResult.getSucceeded());

        final ILocalAuthenticationResult localAuthenticationResult = acquireTokenResult.getLocalAuthenticationResult();

        Assert.assertNotNull(localAuthenticationResult);
        Assert.assertNotNull(localAuthenticationResult.getAccessToken());
        Assert.assertNotNull(localAuthenticationResult.getIdToken());

        final Map<String, ?> idTokenClaims = mJwtParser.parseJWT(localAuthenticationResult.getIdToken());

        Assert.assertNotNull(idTokenClaims);

        Assert.assertEquals(sTestAccount.getUsername(), idTokenClaims.get(IDToken.PREFERRED_USERNAME));
    }

    private RopcTokenCommandParameters createRopcCommandParameters() {
        return RopcTokenCommandParameters.builder()
                .username(sTestAccount.getUsername())
                .password(sTestAccount.getPassword())
                .authority(Authority.getAuthorityFromAuthorityUrl(AUTHORITY_URL))
                .clientId(CLIENT_ID)
                .applicationIdentifier(APPLICATION_IDENTIFIER)
                .correlationId(UUID.randomUUID().toString())
                .redirectUri(REDIRECT_URI)
                .platformComponents(mPlatformComponents)
                .oAuth2TokenCache(MsalOAuth2TokenCache.create(mPlatformComponents))
                .authenticationScheme(new BearerAuthenticationSchemeInternal())
                .sdkType(SdkType.MSAL)
                .requiredBrokerProtocolVersion(BrokerProtocolVersionUtil.MSAL_TO_BROKER_PROTOCOL_COMPRESSION_CHANGES_MINIMUM_VERSION)
                .scopes(Collections.singleton(SCOPE))
                .build();
    }

    private SilentTokenCommandParameters createSilentTokenCommandParameters(@NonNull final IAccountRecord accountRecord) {
        return SilentTokenCommandParameters.builder()
                .authority(Authority.getAuthorityFromAuthorityUrl(AUTHORITY_URL))
                .clientId(CLIENT_ID)
                .correlationId(UUID.randomUUID().toString())
                .redirectUri(REDIRECT_URI)
                .platformComponents(mPlatformComponents)
                .oAuth2TokenCache(MsalOAuth2TokenCache.create(mPlatformComponents))
                .authenticationScheme(new BearerAuthenticationSchemeInternal())
                .sdkType(SdkType.MSAL)
                .requiredBrokerProtocolVersion(BrokerProtocolVersionUtil.MSAL_TO_BROKER_PROTOCOL_COMPRESSION_CHANGES_MINIMUM_VERSION)
                .scopes(Collections.singleton(SCOPE))
                .account(accountRecord)
                .build();
    }
}
