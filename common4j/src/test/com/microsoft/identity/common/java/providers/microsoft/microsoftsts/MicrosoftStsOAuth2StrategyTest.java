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
package com.microsoft.identity.common.java.providers.microsoft.microsoftsts;

import com.microsoft.identity.common.components.MockPlatformComponentsFactory;
import com.microsoft.identity.common.java.authscheme.BearerAuthenticationSchemeInternal;
import com.microsoft.identity.common.java.interfaces.IPlatformComponents;
import com.microsoft.identity.common.java.net.HttpResponse;
import com.microsoft.identity.common.java.providers.oauth2.OAuth2StrategyParameters;
import com.microsoft.identity.common.java.providers.oauth2.TokenResult;
import com.microsoft.identity.common.java.util.ObjectMapper;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.net.URL;

import lombok.NonNull;
import lombok.SneakyThrows;

/**
 * Tests for {@link MicrosoftStsOAuth2Strategy}
 */
@RunWith(JUnit4.class)
public class MicrosoftStsOAuth2StrategyTest {
    private static final String MOCK_TOKEN_SUCCESS_RESPONSE = "{\n" +
            "\t\"token_type\": \"Bearer\",\n" +
            "\t\"scope\": \"mock_scope_1\",\n" +
            "\t\"expires_in\": 3599,\n" +
            "\t\"ext_expires_in\": 3599,\n" +
            "\t\"access_token\": \"b06d0810-12ff-4a4e-850b-4bda1540d895\",\n" +
            "\t\"refresh_token\": \"6b80f5b5-d53c-4c46-992d-66c5dcd4cfb1\",\n" +
            "\t\"id_token\": \"95608142-3a7a-4643-a543-6db44e403e97\",\n" +
            "\t\"client_info\": \"2245f73e-287a-41c4-ba87-560809ad06b9\"\n" +
            "}";

    @SneakyThrows
    @Test
    public void testGetTokenResultFromHttpResponse() {
        final IPlatformComponents mockPlatformComponents = MockPlatformComponentsFactory.getNonFunctionalBuilder().build();
        final MicrosoftStsOAuth2Configuration mockConfig = new MicrosoftStsOAuth2Configuration();
        mockConfig.setAuthorityUrl(new URL("https://login.microsoftonline.com/common"));
        final OAuth2StrategyParameters parameters = OAuth2StrategyParameters.builder()
                .platformComponents(mockPlatformComponents)
                .usingOpenIdConfiguration(false)
                .build();
        final MicrosoftStsOAuth2Strategy microsoftStsOAuth2Strategy = new MicrosoftStsOAuth2Strategy(mockConfig, parameters);
        final HttpResponse mockHttpResponse = new HttpResponse(200, MOCK_TOKEN_SUCCESS_RESPONSE, null);
        final TokenResult tokenResult = microsoftStsOAuth2Strategy.getTokenResultFromHttpResponse(mockHttpResponse);
        Assert.assertNotNull(tokenResult);
        Assert.assertNotNull(tokenResult.getSuccessResponse());
        Assert.assertTrue(tokenResult.getSuccess());
    }

    @SneakyThrows
    @Test
    public void testRequestToken_WithResponseHandler() {
        final IPlatformComponents mockPlatformComponents = MockPlatformComponentsFactory.getNonFunctionalBuilder().build();
        final MicrosoftStsOAuth2Configuration mockConfig = new MicrosoftStsOAuth2Configuration();
        mockConfig.setAuthorityUrl(new URL("https://login.microsoftonline.com/common"));
        final OAuth2StrategyParameters parameters = OAuth2StrategyParameters.builder()
                .platformComponents(mockPlatformComponents)
                .usingOpenIdConfiguration(false)
                .build();

        final HttpResponse mockHttpResponse = new HttpResponse(200, "response_body", null);
        final MicrosoftStsOAuth2Strategy microsoftStsOAuth2Strategy = new MicrosoftStsOAuth2Strategy(mockConfig, parameters) {
            @Override
            public HttpResponse performTokenRequest(@NonNull MicrosoftStsTokenRequest requestContext) {
                return mockHttpResponse;
            }
        };
        final AbstractMicrosoftStsTokenResponseHandler mockTokenResponseHandler = new AbstractMicrosoftStsTokenResponseHandler() {
            @Override
            protected MicrosoftStsTokenResponse getSuccessfulResponse(@NonNull HttpResponse httpResponse) {
                return ObjectMapper.deserializeJsonStringToObject(MOCK_TOKEN_SUCCESS_RESPONSE, MicrosoftStsTokenResponse.class);
            }
        };
        final MicrosoftStsTokenRequest mockTokenRequest = new MicrosoftStsTokenRequest();
        final TokenResult tokenResult = microsoftStsOAuth2Strategy.requestToken(mockTokenRequest, mockTokenResponseHandler);
        Assert.assertNotNull(tokenResult);
        Assert.assertNotNull(tokenResult.getSuccessResponse());
        Assert.assertTrue(tokenResult.getSuccess());
        Assert.assertNotNull(tokenResult.getSuccessResponse().getAuthority());
    }


    @SneakyThrows
    @Test
    public void testRequestToken() {
        final IPlatformComponents mockPlatformComponents = MockPlatformComponentsFactory.getNonFunctionalBuilder().build();
        final MicrosoftStsOAuth2Configuration mockConfig = new MicrosoftStsOAuth2Configuration();
        mockConfig.setAuthorityUrl(new URL("https://login.microsoftonline.com/common"));
        final OAuth2StrategyParameters parameters = OAuth2StrategyParameters.builder()
                .platformComponents(mockPlatformComponents)
                .usingOpenIdConfiguration(false)
                .build();

        final HttpResponse mockHttpResponse = new HttpResponse(200, MOCK_TOKEN_SUCCESS_RESPONSE, null);
        final MicrosoftStsOAuth2Strategy microsoftStsOAuth2Strategy = new MicrosoftStsOAuth2Strategy(mockConfig, parameters) {
            @Override
            public HttpResponse performTokenRequest(@NonNull MicrosoftStsTokenRequest requestContext) {
                return mockHttpResponse;
            }
        };

        final MicrosoftStsTokenRequest mockTokenRequest = new MicrosoftStsTokenRequest();
        final TokenResult tokenResult = microsoftStsOAuth2Strategy.requestToken(mockTokenRequest);
        Assert.assertNotNull(tokenResult);
        Assert.assertNotNull(tokenResult.getSuccessResponse());
        Assert.assertTrue(tokenResult.getSuccess());
        Assert.assertNotNull(tokenResult.getSuccessResponse().getAuthority());
    }
}
