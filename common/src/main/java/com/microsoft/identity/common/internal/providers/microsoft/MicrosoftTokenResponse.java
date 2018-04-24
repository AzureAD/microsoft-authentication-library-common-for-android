package com.microsoft.identity.common.internal.providers.microsoft;

import com.google.gson.annotations.SerializedName;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResponse;

public class MicrosoftTokenResponse extends TokenResponse {

    @SerializedName("ext_expires_in")
    protected Long mExtendedExpiresIn;

}
