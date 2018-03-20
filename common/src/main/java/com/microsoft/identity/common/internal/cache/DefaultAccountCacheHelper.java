package com.microsoft.identity.common.internal.cache;

import com.microsoft.identity.common.internal.dto.Account;

import java.util.ArrayList;
import java.util.List;

/**
 * Default class for creating cache keys and values for {@link Account} objects.
 */
public class DefaultAccountCacheHelper extends AbstractCacheHelper<Account> {

    @Override
    public String createCacheKey(final Account account) {
        final List<String> keyComponents = new ArrayList<>();
        keyComponents.add(account.getUniqueId());
        keyComponents.add(account.getEnvironment());
        keyComponents.add(account.getRealm());

        return collapseKeyComponents(keyComponents);
    }

}
