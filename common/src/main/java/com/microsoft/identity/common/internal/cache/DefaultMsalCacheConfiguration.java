package com.microsoft.identity.common.internal.cache;

import com.microsoft.identity.common.internal.dto.AccessToken;
import com.microsoft.identity.common.internal.dto.Account;
import com.microsoft.identity.common.internal.dto.RefreshToken;

public class DefaultMsalCacheConfiguration implements IMsalCacheConfiguration {

    @Override
    public ICacheHelper<Account> getAccountCacheHelper() {
        return new DefaultMsalAccountCacheHelper();
    }

    @Override
    public ICacheHelper<AccessToken> getAccessTokenCacheHelper() {
        return new DefaultMsalAccessTokenCacheHelper();
    }

    @Override
    public ICacheHelper<RefreshToken> getRefreshTokenCacheHelper() {
        return new DefaultRefreshTokenCacheHelper();
    }

    @Override
    public IMsalAccountFactory getAccountFactory() {
        return new DefaultMsalAccountFactory();
    }

    @Override
    public IMsalCredentialFactory getCredentialFactory() {
        return new DefaultMsalCredentialFactory();
    }
}
