package com.microsoft.identity.common.internal.providers.microsoft.microsoftsts;

import com.microsoft.identity.common.internal.providers.oauth2.TokenRequest;

/**
 * {@link TokenRequest} subclass for the Microsoft STS (V2).
 * Includes support for client assertions per the specs:
 * https://tools.ietf.org/html/rfc7521
 * https://docs.microsoft.com/en-us/azure/active-directory/develop/active-directory-v2-protocols-oauth-client-creds
 */
public class MicrosoftSTSTokenRequest extends TokenRequest {

    private String mClientAssertionType;
    private String mClientAssertion;



}
