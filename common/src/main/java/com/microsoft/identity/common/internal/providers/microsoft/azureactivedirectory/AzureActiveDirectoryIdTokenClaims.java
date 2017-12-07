package com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory;


import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftIdTokenClaims;

/**
 * Represents additional id token claims issued by AAD (V1 Endpoint)
 * https://docs.microsoft.com/en-us/azure/active-directory/develop/active-directory-token-and-claims
 */
public class AzureActiveDirectoryIdTokenClaims extends MicrosoftIdTokenClaims {

    /**
     * Stores the user name of the user principal.
     */
    public static final String UPN = "upn";

    /**
     * Provides a human readable value that identifies the subject of the token. This value is not
     * guaranteed to be unique within a tenant and is designed to be used only for display purposes.
     */
    public static final String UNIQUE_NAME = "unique_name";
    public static final String PASSWORD_EXPIRATION = "pwd_exp";
    public static final String PASSWORD_CHANGE_URL = "pwd_url";
}
