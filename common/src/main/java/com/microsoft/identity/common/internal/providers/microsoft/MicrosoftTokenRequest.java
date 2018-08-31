package com.microsoft.identity.common.internal.providers.microsoft;

import com.google.gson.annotations.SerializedName;
import com.microsoft.identity.common.internal.providers.oauth2.TokenRequest;

public class MicrosoftTokenRequest extends TokenRequest {

    public static final String CODE_VERIFIER = "code_verifier";
    public static final String CLIENT_INFO = "client_info";

    public MicrosoftTokenRequest(){
        mClientInfoEnabled = "1";
    }

    @SerializedName(CODE_VERIFIER)
    private String mCodeVerifier;

    @SerializedName(CLIENT_INFO)
    private String mClientInfoEnabled;

    public String getCodeVerifier(){
        return this.mCodeVerifier;
    }

    public void setCodeVerifier(String codeVerifier){
        this.mCodeVerifier = codeVerifier;
    }

    public String getClientInfoEnabled(){
        return this.mClientInfoEnabled;
    }


}
