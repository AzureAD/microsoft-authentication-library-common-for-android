package com.microsoft.identity.common.internal.providers.ropc;

import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResult;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationStatus;

/**
 * A class to provide a Fake Authorization Result object to be used in ROPC flow
 */
public class FakeAuthorizationResult extends AuthorizationResult {

    public FakeAuthorizationResult() {
        // assume that we have auth code and auth request was successful
        this.setAuthorizationStatus(AuthorizationStatus.SUCCESS);
    }
}
