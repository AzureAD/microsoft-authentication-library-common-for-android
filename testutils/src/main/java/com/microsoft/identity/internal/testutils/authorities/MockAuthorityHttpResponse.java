package com.microsoft.identity.internal.testutils.authorities;

import androidx.annotation.NonNull;

import com.microsoft.identity.common.internal.authorities.AzureActiveDirectoryAudience;
import com.microsoft.identity.common.internal.authorities.AzureActiveDirectoryAuthority;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Configuration;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2StrategyParameters;
import com.microsoft.identity.internal.testutils.strategies.MockStrategyWithMockedHttpResponse;

public class MockAuthorityHttpResponse extends AzureActiveDirectoryAuthority {

    public MockAuthorityHttpResponse(final AzureActiveDirectoryAudience azureActiveDirectoryAudience) {
        super(azureActiveDirectoryAudience);
    }

    @Override
    public OAuth2Strategy createOAuth2Strategy(@NonNull final OAuth2StrategyParameters parameters) {
        final MicrosoftStsOAuth2Configuration config = createOAuth2Configuration();
        return new MockStrategyWithMockedHttpResponse(config);
    }

}
