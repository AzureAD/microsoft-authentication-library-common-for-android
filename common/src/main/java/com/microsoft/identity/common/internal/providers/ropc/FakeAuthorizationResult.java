package com.microsoft.identity.common.internal.providers.ropc;

import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResult;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationStatus;

public class FakeAuthorizationResult extends AuthorizationResult {

    public FakeAuthorizationResult() {
        this.setAuthorizationStatus(AuthorizationStatus.SUCCESS);
    }
}
