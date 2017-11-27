package com.microsoft.identity.common;

import java.util.List;

/**
 * In MSAL we have user... in ADAL we have userinfo
 * Users are human or software agents.... humans and software agents have accounts
 * UserInfo shouldn't be used in common since it collides with the OIDC spec
 * This class contains information about the user/account associated with the authenticated subject/principal
 */
public abstract class Account {

    /**
     * Not all IDPs will have the same unique identifier for a user
     * Per the OIDC spec the unique identifier is subject... or the sub claim; however AAD and other
     * IDPs have their own unique identifiers for users
     *
     * Let the IDP give us the representation of the user/account based on the token response
     * @return
     */
    public abstract String getUniqueIdentifier();

    public abstract List<String> getCacheIdentifiers();

}
