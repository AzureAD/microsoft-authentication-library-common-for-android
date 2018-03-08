package com.microsoft.identity.common.internal.providers.microsoft.activedirectoryfederationservices;

import com.microsoft.identity.common.model_old.Account;

import java.util.List;


/**
 * The ADFS 2016 Account Object
 */
public class ActiveDirectoryFederationServices2016Account extends Account {
    @Override
    public String getUniqueIdentifier() {
        return null;
    }

    @Override
    public List<String> getCacheIdentifiers() {
        return null;
    }
}
