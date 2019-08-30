package com.microsoft.identity.common.internal.testutils;

import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsTokenResponse;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResponse;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResult;

public class MockTokenResponse {

    private static final long ACCESS_TOKEN_AGE = 3599;
    private static final String TOKEN_TYPE = "Bearer";

    public static TokenResponse getTokenResponse() {
        String fakeAccessToken = "aaaa.BBBB.123";
        String fakeRefreshToken = "abcDeFGhijkl";
        String fakeIdToken = TokenCreator.createIdToken();
        String fakeClientInfo = TokenCreator.createRawClientInfo();

        MicrosoftStsTokenResponse tokenResponse = new MicrosoftStsTokenResponse();

        tokenResponse.setExpiresIn(ACCESS_TOKEN_AGE);
        tokenResponse.setExtExpiresIn(ACCESS_TOKEN_AGE);
        tokenResponse.setAccessToken(fakeAccessToken);
        tokenResponse.setTokenType(TOKEN_TYPE);
        tokenResponse.setRefreshToken(fakeRefreshToken);
        tokenResponse.setScope("User.Read");
        tokenResponse.setIdToken(fakeIdToken);
        tokenResponse.setClientInfo(fakeClientInfo);
        tokenResponse.setRefreshTokenAge("");
        tokenResponse.setCliTelemErrorCode("0");
        tokenResponse.setCliTelemSubErrorCode("0");
        tokenResponse.setResponseReceivedTime(Long.valueOf(0));

        return tokenResponse;
    }

    public static TokenResponse getTokenResponseWithExpiredAccessToken() {
        MicrosoftStsTokenResponse tokenResponse = (MicrosoftStsTokenResponse) getTokenResponse();
        tokenResponse.setExpiresIn(Long.valueOf(0));
        tokenResponse.setExtExpiresIn(Long.valueOf(0));
        return tokenResponse;
    }

    public static TokenResponse getTokenResponseWithoutAccessToken() {
        MicrosoftStsTokenResponse tokenResponse = (MicrosoftStsTokenResponse) getTokenResponse();
        tokenResponse.setExpiresIn(null);
        tokenResponse.setExtExpiresIn(null);
        tokenResponse.setAccessToken(null);
        return tokenResponse;
    }

}
