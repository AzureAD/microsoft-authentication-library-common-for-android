package com.microsoft.identity.common.internal.cache;

import com.microsoft.identity.common.Account;
import com.microsoft.identity.common.internal.providers.oauth2.RefreshToken;

/**
 * Interface that defines methods allowing refresh token cache state to be shared between Cache Implementations
 * The assumption being that in order for a client to avoid prompting a user to sign in they need a refresh token (effectively SSO state)
 */
public interface IShareSingleSignOnState<T extends Account, U extends RefreshToken> {

    void setSingleSignOnState(T account, U refreshToken);

    U getSingleSignOnState(T account);

}
