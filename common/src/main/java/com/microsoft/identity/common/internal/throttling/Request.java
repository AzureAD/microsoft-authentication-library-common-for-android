package com.microsoft.identity.common.internal.throttling;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.Set;

import lombok.Getter;

@Getter
public class Request {

    private class SerializedNames {
        public static final String CLIENT_ID = "client_id";
        public static final String AUTHORITY = "authority";
        public static final String SCOPES = "scopes";
        public static final String HOME_ACCOUNT_ID = "home_account_id";
    }

    @SerializedName(SerializedNames.CLIENT_ID)
    private final String clientId;

    @SerializedName(SerializedNames.AUTHORITY)
    private final String authority;

    @SerializedName(SerializedNames.SCOPES)
    private final Set<String> scopes;

    @SerializedName(SerializedNames.HOME_ACCOUNT_ID)
    private final String homeAccountId;

    public Request(@NonNull final String clientId,
                   @NonNull final String authority,
                   @NonNull final Set<String> scopes,
                   @Nullable final String homeAccountId) {
        this.clientId = clientId;
        this.authority = authority;
        this.scopes = scopes;
        this.homeAccountId = homeAccountId;
    }

}
