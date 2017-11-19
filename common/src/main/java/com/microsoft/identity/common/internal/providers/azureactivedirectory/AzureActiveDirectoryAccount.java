package com.microsoft.identity.common.internal.providers.azureactivedirectory;

import com.microsoft.identity.common.Account;

/**
 * Inherits from account and implements the getUniqueIdentifier method for returning a unique identifier for an AAD User
 * UTID, UID combined as a single identifier per current MSAL implementation
 */
public class AzureActiveDirectoryAccount extends Account {

    public String getUniqueIdentifier(){
        return "";
    }
}
