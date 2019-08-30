package com.microsoft.identity.common.internal.providers.ropc;

import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Configuration;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsTokenRequest;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResult;

import java.io.IOException;

public class B2CRopcTestStrategy extends ResourceOwnerPasswordCredentialsTestStrategy {

    private static final String TAG = B2CRopcTestStrategy.class.getName();

    /**
     * Constructor of B2CRopcTestStrategy.
     *
     * @param config Microsoft Sts OAuth2 configuration
     */
    public B2CRopcTestStrategy(MicrosoftStsOAuth2Configuration config) {
        super(config);
    }

    /**
     * @param request microsoft sts token request.
     * @return TokenResult
     * @throws IOException thrown when failed or interrupted I/O operations occur.
     */
    @Override
    public TokenResult requestToken(final MicrosoftStsTokenRequest request) throws IOException, ClientException {
        setTokenEndpointForRopc();
        return super.requestToken(request);
    }

    private void setTokenEndpointForRopc() {
        String ropcTokenEndpoint = mConfig.getTokenEndpoint().toString() + "?p=b2c_1_ropc_auth";
        setTokenEndpoint(ropcTokenEndpoint);
    }
}
