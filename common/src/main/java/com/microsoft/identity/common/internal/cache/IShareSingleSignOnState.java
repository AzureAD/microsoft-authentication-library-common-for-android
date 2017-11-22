package com.microsoft.identity.common.internal.cache;

import com.microsoft.identity.common.Account;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.TokenRequest;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResponse;

/**
 * Created by shoatman on 11/22/2017.
 */

public interface IShareSingleSignOnState {

    void setSingleSignOnState(Account account, String refreshToken);
    String getSingleSignOnState(Account account);

}
