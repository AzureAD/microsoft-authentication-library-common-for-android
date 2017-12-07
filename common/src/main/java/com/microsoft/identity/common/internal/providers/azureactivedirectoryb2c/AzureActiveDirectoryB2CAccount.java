package com.microsoft.identity.common.internal.providers.azureactivedirectoryb2c;

import com.microsoft.identity.common.Account;

import java.util.List;

/**
 * Represents the Azure AD B2C Account
 */
public class AzureActiveDirectoryB2CAccount extends Account {
    @Override
    public String getUniqueIdentifier() {
        return null;
    }

    @Override
    public List<String> getCacheIdentifiers() {
        return null;
    }
}
