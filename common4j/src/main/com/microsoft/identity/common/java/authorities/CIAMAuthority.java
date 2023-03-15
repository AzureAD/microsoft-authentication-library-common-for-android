package com.microsoft.identity.common.java.authorities;

import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.java.providers.oauth2.OAuth2StrategyParameters;

public class CIAMAuthority extends Authority{
    @Override
    public OAuth2Strategy createOAuth2Strategy(OAuth2StrategyParameters parameters) throws ClientException {
        return null;
    }
}
