package com.microsoft.identity.common.internal.providers.azureactivedirectory;

import com.microsoft.identity.common.Account;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;

/**
 * Created by shoatman on 11/16/2017.
 */

public class AzureActiveDirectoryOAuth2Strategy extends OAuth2Strategy {

    protected void validateAuthoriztionRequest(AuthorizationRequest request){

    }
    protected void validateTokenRequest(AuthorizationRequest request){

    }
    protected Account createAccount(){
        Account a = new AzureActiveDirectoryAccount();
        return a;
    }
}
