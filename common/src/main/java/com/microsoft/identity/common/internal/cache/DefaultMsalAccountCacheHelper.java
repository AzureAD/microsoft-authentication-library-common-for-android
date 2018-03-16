package com.microsoft.identity.common.internal.cache;

import com.microsoft.identity.common.internal.dto.Account;

/**
 * Default class for creating cache keys and values for {@link Account} objects.
 */
public class DefaultMsalAccountCacheHelper extends AbstractCacheHelper<Account> {

    @Override
    public String createCacheKey(Account account) {
        // TODO
        return null;
    }
    
}
