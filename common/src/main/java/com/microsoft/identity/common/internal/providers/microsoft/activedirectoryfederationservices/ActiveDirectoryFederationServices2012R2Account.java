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
        throw new UnsupportedOperationException("Method stub!");
    }

    @Override
    public List<String> getCacheIdentifiers() {
        throw new UnsupportedOperationException("Method stub!");
    }

    @Override
    public String getUniqueUserId() {
        throw new UnsupportedOperationException("Method stub!");
    }

    @Override
    public String getEnvironment() {
        throw new UnsupportedOperationException("Method stub!");
    }

    @Override
    public String getRealm() {
        throw new UnsupportedOperationException("Method stub!");
    }

    @Override
    public String getAuthorityAccountId() {
        throw new UnsupportedOperationException("Method stub!");
    }

    @Override
    public String getUsername() {
        throw new UnsupportedOperationException("Method stub!");
    }

    @Override
    public String getAuthorityType() {
        throw new UnsupportedOperationException("Method stub!");
    }

    @Override
    public String getGuestId() {
        throw new UnsupportedOperationException("Method stub!");
    }

    @Override
    public String getFirstName() {
        throw new UnsupportedOperationException("Method stub!");
    }

    @Override
    public String getLastName() {
        throw new UnsupportedOperationException("Method stub!");
    }

    @Override
    public String getAvatarUrl() {
        throw new UnsupportedOperationException("Method stub!");
    }
}
