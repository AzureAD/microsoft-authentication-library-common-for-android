package com.microsoft.identity.common.internal.cache;

import com.microsoft.identity.common.Account;

/**
 * Created by shoatman on 11/22/2017.
 */

public interface IShareSingleSignOnState {

    void setSingleSignOnState(Account account, String refreshToken);

    String getSingleSignOnState(Account account);

}
