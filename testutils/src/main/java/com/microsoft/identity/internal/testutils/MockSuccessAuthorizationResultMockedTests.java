package com.microsoft.identity.internal.testutils;

import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationResponse;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResult;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationStatus;

import java.util.HashMap;

public class MockSuccessAuthorizationResultMockedTests extends AuthorizationResult {

    public MockSuccessAuthorizationResultMockedTests() {
        MicrosoftStsAuthorizationResponse response = new MicrosoftStsAuthorizationResponse("", "", new HashMap<String, String>());
        this.setAuthorizationResponse(response);
        // assume that we have auth code and auth request was successful
        this.setAuthorizationStatus(AuthorizationStatus.SUCCESS);
    }
}
