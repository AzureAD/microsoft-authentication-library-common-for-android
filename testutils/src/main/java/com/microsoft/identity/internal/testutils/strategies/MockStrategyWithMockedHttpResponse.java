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
package com.microsoft.identity.internal.testutils.strategies;

import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationRequest;
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Configuration;
import com.microsoft.identity.common.java.providers.oauth2.IAuthorizationStrategy;
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationResult;
import com.microsoft.identity.common.java.util.ResultFuture;
import com.microsoft.identity.internal.testutils.mocks.MockSuccessAuthorizationResultMockedTests;

public class MockStrategyWithMockedHttpResponse extends ResourceOwnerPasswordCredentialsTestStrategy {

    /**
     * Constructor of ResourceOwnerPasswordCredentialsTestStrategy.
     *
     * @param config Microsoft Sts OAuth2 configuration
     */
    public MockStrategyWithMockedHttpResponse(MicrosoftStsOAuth2Configuration config) throws ClientException {
        super(config);
    }

    /**
     * Template method for executing an OAuth2 authorization request.
     *
     * @param request                microsoft sts authorization request.
     * @param IAuthorizationStrategy authorization strategy.
     * @return GenericAuthorizationResponse
     */
    @Override
    public ResultFuture<AuthorizationResult> requestAuthorization(
            final MicrosoftStsAuthorizationRequest request,
            final IAuthorizationStrategy IAuthorizationStrategy) {
        final MockSuccessAuthorizationResultMockedTests authorizationResult = new MockSuccessAuthorizationResultMockedTests();
        final ResultFuture<AuthorizationResult> future = new ResultFuture<>();
        future.setResult(authorizationResult);
        return future;
    }

    @Override
    String getPasswordForUser(String username) {
        return "fake-password";
    }

}
