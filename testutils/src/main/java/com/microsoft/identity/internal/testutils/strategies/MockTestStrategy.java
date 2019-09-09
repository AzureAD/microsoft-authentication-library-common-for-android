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
package com.microsoft.identity.internal.testutils.strategies;

import androidx.annotation.NonNull;

import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.internal.logging.DiagnosticContext;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.net.HttpResponse;
import com.microsoft.identity.common.internal.net.ObjectMapper;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationRequest;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationResponse;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Configuration;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Strategy;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsTokenRequest;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResult;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationStrategy;
import com.microsoft.identity.common.internal.providers.oauth2.TokenRequest;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResponse;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResult;
import com.microsoft.identity.internal.testutils.FakeAuthorizationResult;
import com.microsoft.identity.common.internal.result.ResultFuture;
import com.microsoft.identity.internal.testutils.MockTokenResponse;
import com.microsoft.identity.common.internal.util.StringUtil;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.Future;

public class MockTestStrategy extends ResourceOwnerPasswordCredentialsTestStrategy {

    /**
     * Constructor of MockTestStrategy.
     *
     * @param config Microsoft Sts OAuth2 configuration
     */
    public MockTestStrategy(MicrosoftStsOAuth2Configuration config) {
        super(config);
    }

    @Override
    String getPasswordForUser(String username) {
        return "fake-password";
    }

    public TokenResult getTokenResult() {
        final TokenResponse tokenResponse = MockTokenResponse.getTokenResponse();
        final TokenResult tokenResult = new TokenResult(tokenResponse);
        return tokenResult;
    }

    @Override
    protected HttpResponse performTokenRequest(final MicrosoftStsTokenRequest tokenRequest) {
        final TokenResult tokenResult = getTokenResult();
        final TokenResponse tokenResponse = tokenResult.getTokenResponse();
        final HttpResponse httpResponse = makeHttpResponseFromResponseObject(tokenResponse);
        return httpResponse;
    }

    public HttpResponse makeHttpResponseFromResponseObject(final Object obj) {
        final String httpResponseBody = ObjectMapper.serializeObjectToJsonString(obj);
        final HttpResponse httpResponse = new HttpResponse(200, httpResponseBody, null);
        return httpResponse;
    }

}
