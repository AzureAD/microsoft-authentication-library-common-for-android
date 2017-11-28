package com.microsoft.identity.common.internal.providers.azureactivedirectory;

import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;

import java.net.URL;


public class AzureActiveDirectoryAuthorizationRequest extends AuthorizationRequest {

    URL mAuthority;

    public URL getAuthority() {
        return mAuthority;
    }

    public void setAuthority(URL mAuthority) {
        this.mAuthority = mAuthority;
    }


}
