package com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectoryb2c;

import com.microsoft.identity.common.model_old.Account;

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
