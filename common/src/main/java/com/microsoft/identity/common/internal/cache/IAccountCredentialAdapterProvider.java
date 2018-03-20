package com.microsoft.identity.common.internal.cache;

import com.microsoft.identity.common.internal.dto.Account;
import com.microsoft.identity.common.internal.dto.Credential;

/**
 * Provides Adapters to the UnnamedClassThatDelegatesToTheActualCache.
 */
public interface IAccountCredentialAdapterProvider {

    /**
     * Gets the {@link Account} adapter.
     *
     * @return The Account adapter to get.
     */
    IAccountAdapter getAccountAdapter();

    /**
     * Gets the {@link Credential} adapter.
     *
     * @return The Credential adapter to get.
     */
    ICredentialAdapter getCredentialAdapter();

}
