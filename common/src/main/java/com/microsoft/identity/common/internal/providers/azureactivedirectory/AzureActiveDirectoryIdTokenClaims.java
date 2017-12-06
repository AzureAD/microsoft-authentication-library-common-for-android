package com.microsoft.identity.common.internal.providers.azureactivedirectory;


/**
 * Represents additional id token claims issued by AAD (V1 Endpoint)
 * https://docs.microsoft.com/en-us/azure/active-directory/develop/active-directory-token-and-claims
 */
public class AzureActiveDirectoryIdTokenClaims {
    public static final String ISSUER = "iss";
    public static final String OJBECT_ID = "oid";
    public static final String TENANT_ID = "tid";
    public static final String UPN = "upn";
    public static final String AUDIENCE = "aud";
    public static final String ISSUED_AT = "iat";
    public static final String NOT_BEFORE = "nbf";
    public static final String VERSION = "ver";
    public static final String UNIQUE_NAME = "unique_name";
    public static final String PASSWORD_EXPIRATION = "pwd_exp";
    public static final String PASSWORD_CHANGE_URL = "pwd_url";

}
