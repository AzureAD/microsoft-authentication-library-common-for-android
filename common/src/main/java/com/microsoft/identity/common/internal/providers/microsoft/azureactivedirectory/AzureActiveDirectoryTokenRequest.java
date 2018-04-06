package com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory;

import com.google.gson.annotations.SerializedName;
import com.microsoft.identity.common.internal.providers.oauth2.TokenRequest;

/**
 * Created by shoatman on 4/6/2018.
 */

public class AzureActiveDirectoryTokenRequest extends TokenRequest {

    @SerializedName("resource")
    protected String mResourceId;

    public String getResourceId(){
        return this.mResourceId;
    }

    public void setResourceId(String resourceId){
        this.mResourceId = resourceId;
    }
}
