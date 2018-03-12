package com.microsoft.identity.common.internal.providers.microsoft.activedirectoryfederationservices;

import com.microsoft.identity.common.Account;

import java.util.List;

/**
 * The Active Directory Federation Services 2012 R2 Account Object
 * NOTE: Since ADFS 2012 R2 does not support OIDC there is no id token
 * It's unclear to me how ADFS 2012 Accounts should be identified... I believe there is a
 * need for the API to support generating a unique identifier to a user and returning that to the caller
 * OR for the caller to provide a unique identifier prior to initiating the request
 */
public class ActiveDirectoryFederationServices2012R2Account extends Account {
    @Override
    public String getUniqueIdentifier() {
        return null;
    }

    @Override
    public List<String> getCacheIdentifiers() {
        return null;
    }
}
