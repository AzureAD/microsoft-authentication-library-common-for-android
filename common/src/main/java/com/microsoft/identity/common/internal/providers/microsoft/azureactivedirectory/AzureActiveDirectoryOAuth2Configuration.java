package com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory;

import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Configuration;

/**
 * Created by shoatman on 12/1/2017.
 */

public class AzureActiveDirectoryOAuth2Configuration extends OAuth2Configuration {

    public boolean isAuthorityHostValdiationEnabled() {
        return mAuthorityHostValidationEnabled;
    }

    public void setAuthorityHostValdiationEnabled(boolean mAuthorityHostValdiationEnabled) {
        this.mAuthorityHostValidationEnabled = mAuthorityHostValdiationEnabled;
    }

    private boolean mAuthorityHostValidationEnabled = true;


}
