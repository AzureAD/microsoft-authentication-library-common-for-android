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

    private RefreshTokenRequestParameters(RefreshTokenRequestParameters.Builder builder) {
        mClientId = builder.mClientId;
        mScopes = builder.mScopes;
        mRefreshToken = builder.mRefreshToken;
        mClaims = builder.mClaims;
        mIdTokenVersion = builder.mIdTokenVersion;
        mCorrelationId = builder.mCorrelationId;
    }

    public static class Builder {
        private String mClientId;
        private String mScopes;
        private String mRefreshToken;
        private String mClaims;
        private String mIdTokenVersion;
        private UUID mCorrelationId;

        public RefreshTokenRequestParameters.Builder clientId(@NonNull final String clientId) {
            this.mClientId = clientId;
            return this;
        }

        public RefreshTokenRequestParameters.Builder scopes(@NonNull final String scopes) {
            this.mScopes = scopes;
            return this;
        }

        public RefreshTokenRequestParameters.Builder refreshToken(@NonNull final String refreshToken) {
            this.mRefreshToken = refreshToken;
            return this;
        }

        public RefreshTokenRequestParameters.Builder claims(@Nullable final String claims) {
            this.mClaims = claims;
            return this;
        }

        public RefreshTokenRequestParameters.Builder idTokenVersion(@Nullable final String idTokenVersion) {
            this.mIdTokenVersion = idTokenVersion;
            return this;
        }

        public RefreshTokenRequestParameters.Builder correlationId(@Nullable final UUID correlationId) {
            this.mCorrelationId = correlationId;
            return this;
        }

        public RefreshTokenRequestParameters.Builder fromParameters(@NonNull final AcquireTokenSilentOperationParameters parameters) {
            this.mClientId = parameters.getClientId();
            this.mScopes = TextUtils.join(" ", parameters.getScopes());
            this.mRefreshToken = parameters.getRefreshToken().getSecret();
            this.mClaims = parameters.getClaimsRequestJson();
            this.mIdTokenVersion = parameters.getSdkType() == SdkType.ADAL ? "1" : null;
            this.mCorrelationId = null;
            return this;
        }

        public RefreshTokenRequestParameters build() {
            return new RefreshTokenRequestParameters(this);
        }
    }
}
