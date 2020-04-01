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
package com.microsoft.identity.internal.testutils.mocks;

import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftAuthorizationResponse;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationResponse;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResult;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationStatus;
import com.microsoft.identity.internal.testutils.labutils.LabConfig;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

/**
 * A class to provide a Fake Authorization Result object to be used in ROPC flow
 */
public class MockSuccessAuthorizationResultNetworkTests extends AuthorizationResult {

    @Override
    public boolean getSuccess() {
        return true;
    }

    public MockSuccessAuthorizationResultNetworkTests() {
        try {
            // get cloud instance host name from the authority url provided by lab info
            // and set in the mock authorization response so that we can test multiple cloud support
            final URL authorityURL = new URL(LabConfig.getCurrentLabConfig().getAuthority());
            final HashMap<String, String> authorizationParams = new HashMap<>();
            authorizationParams.put(MicrosoftAuthorizationResponse.CLOUD_INSTANCE_HOST_NAME, authorityURL.getHost());
            MicrosoftStsAuthorizationResponse response = new MicrosoftStsAuthorizationResponse("", "", authorizationParams);
            this.setAuthorizationResponse(response);
            // assume that we have auth code and auth request was successful
            this.setAuthorizationStatus(AuthorizationStatus.SUCCESS);
        } catch (MalformedURLException e) {
            this.setAuthorizationStatus(AuthorizationStatus.FAIL);
            e.printStackTrace();
        }
    }
}
