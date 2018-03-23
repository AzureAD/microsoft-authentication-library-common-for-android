package com.microsoft.identity.common.internal.cache;

import android.support.annotation.Nullable;

import com.microsoft.identity.common.internal.dto.Account;
import com.microsoft.identity.common.internal.dto.Credential;
import com.microsoft.identity.common.internal.dto.CredentialType;

import java.util.List;

public interface IAccountCredentialCache {

    void saveAccount(final Account account);

    void saveCredential(final Credential credential);

    Account getAccount(final String cacheKey);

    Credential getCredential(final String cacheKey);

    List<Account> getAccounts();

    List<Account> getAccounts(
            @Nullable final String uniqueId,
            final String environment,
            @Nullable final String realm
    );

    List<Credential> getCredentials();

    List<Credential> getCredentials(
            @Nullable final String uniqueId,
            final String environment,
            final CredentialType credentialType,
            final String clientId,
            @Nullable final String realm,
            @Nullable final String target
    );

    boolean removeAccount(final String uniqueId, final String environment);

    boolean removeCredential(final Credential credentialToClear);

    int removeAll(final String uniqueId, final String environment);

    void clearAll();

}
