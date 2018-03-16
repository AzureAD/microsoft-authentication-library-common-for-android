package com.microsoft.identity.common.internal.cache;

import com.microsoft.identity.common.internal.dto.Account;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResponse;

public class DefaultMsalAccountFactory implements IMsalAccountFactory {

    @Override
    public Account createAccount(OAuth2Strategy strategy, AuthorizationRequest request, TokenResponse response) {
        final Account account = new Account();
        // TODO initialize
        return account;
    }
}
