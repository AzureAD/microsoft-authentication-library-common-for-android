package com.microsoft.identity.common.internal.request;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.UUID;

public class RefreshTokenRequestParameters {

    @NonNull
    private String mClientId;

    @NonNull
    private String mScopes;

    @NonNull
    private String mRefreshToken;

    @Nullable
    private String mClaims;

    @Nullable
    private String mIdTokenVersion;

    @Nullable
    private UUID mCorrelationId;

    @Nullable
    public UUID getCorrelationId() {
        return mCorrelationId;
    }

    @NonNull
    public String getClientId() {
        return mClientId;
    }

    @NonNull
    public String getScopes() {
        return mScopes;
    }

    @NonNull
    public String getRefreshToken() {
        return mRefreshToken;
    }

    @Nullable
    public String getClaims() {
        return mClaims;
    }

    @Nullable
    public String getIdTokenVersion() {
        return mIdTokenVersion;
    }

    private RefreshTokenRequestParameters(@NonNull final String clientId,
                                          @NonNull final String scopes,
                                          @NonNull final String refreshToken,
                                          @Nullable final String claims,
                                          @Nullable final String idTokenVersion,
                                          @Nullable final UUID correlationId) {
        this.mClientId = clientId;
        this.mScopes = scopes;
        this.mRefreshToken = refreshToken;
        this.mClaims = claims;
        this.mIdTokenVersion = idTokenVersion;
        this.mCorrelationId = correlationId;
    }

    public RefreshTokenRequestParameters(@NonNull final String clientId,
                                          @NonNull final String scopes,
                                          @NonNull final String refreshToken,
                                          @Nullable final String idTokenVersion,
                                          @Nullable final UUID correlationId) {
        this(
                clientId,
                scopes,
                refreshToken,
                null,
                idTokenVersion,
                correlationId
        );
    }

    public RefreshTokenRequestParameters(@NonNull final AcquireTokenSilentOperationParameters parameters) {
        this(
                parameters.getClientId(),
                TextUtils.join(" ", parameters.getScopes()),
                parameters.getRefreshToken().getSecret(),
                parameters.getClaimsRequestJson(),
                parameters.getSdkType() == SdkType.ADAL ? "1" : null,
                null
        );
    }
}
