package com.microsoft.identity.common.internal.cache;

import com.microsoft.identity.common.internal.dto.Account;
import com.microsoft.identity.common.internal.dto.Credential;

import java.util.List;

public interface IAccountCredentialCache {

    void saveAccount(final Account account);

    void saveCredential(final Credential credential);

    Account getAccount(final String cacheKey);

    Credential getCredential(final String cacheKey);

    List<Account> getAccounts();

    List<Credential> getCredentials();

}
