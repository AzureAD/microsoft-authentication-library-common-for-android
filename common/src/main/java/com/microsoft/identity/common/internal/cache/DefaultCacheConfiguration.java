package com.microsoft.identity.common.internal.cache;

import com.microsoft.identity.common.internal.dto.AccessToken;
import com.microsoft.identity.common.internal.dto.Account;
import com.microsoft.identity.common.internal.dto.RefreshToken;

public class DefaultCacheConfiguration implements ICacheConfiguration {

    @Override
    public ICacheHelper<Account> getAccountCacheHelper() {
        return new DefaultAccountCacheHelper();
    }

    @Override
    public ICacheHelper<AccessToken> getAccessTokenCacheHelper() {
        return new DefaultAccessTokenCacheHelper();
    }

    @Override
    public ICacheHelper<RefreshToken> getRefreshTokenCacheHelper() {
        return new DefaultRefreshTokenCacheHelper();
    }

    @Override
    public IAccountFactory getAccountFactory() {
        return new DefaultAccountFactory();
    }

    @Override
    public ICredentialFactory getCredentialFactory() {
        return new DefaultCredentialFactory();
    }
}
