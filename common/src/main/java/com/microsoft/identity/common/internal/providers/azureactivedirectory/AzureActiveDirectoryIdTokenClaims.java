package com.microsoft.identity.common.internal.providers.azureactivedirectory;

/**
 * Created by shoatman on 11/21/2017.
 */

public class AzureActiveDirectoryIdTokenClaims {
    public static final String ISSUER = "iss";
    public static final String OJBECT_ID = "oid";
    public static final String TENANT_ID = "tid";

    static final String OBJECT_ID = "oid";
    //static final String HOME_OBJECT_ID = "home_oid"; Leaving this out for now... not sure if AAD or B2C returns this....
    //static final String VERSION = "ver"; ... again not sure who is returning this.... are we versioning id tokens in either B2C or AAD?
}
