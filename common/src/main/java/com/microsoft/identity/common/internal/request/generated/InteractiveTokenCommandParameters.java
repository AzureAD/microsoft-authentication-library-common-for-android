package com.microsoft.identity.common.internal.request.generated;

import android.util.Pair;

import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.microsoft.identity.common.internal.authorities.Authority;
import com.microsoft.identity.common.internal.dto.AccountRecord;
import com.microsoft.identity.common.internal.dto.IAccountRecord;
import com.microsoft.identity.common.internal.providers.oauth2.OpenIdConnectPromptParameter;
import com.microsoft.identity.common.internal.ui.AuthorizationAgent;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

@AutoValue
@AutoValue.CopyAnnotations
public abstract class InteractiveTokenCommandParameters
        extends CommandParameters
        implements IScopesAddable<InteractiveTokenCommandParameters>, ITokenRequestParameters{

    public abstract Set<String> scopes();
    public abstract String clientId();
    public abstract String redirectUri();
    public abstract Authority authority();
    @Nullable
    public abstract IAccountRecord accountRecord();
    @Nullable
    public abstract String claimsRequestJson();
    @Nullable
    public abstract Boolean forceRefresh();
    @Nullable
    public abstract String loginHint();
    @Nullable
    public abstract List<Pair<String, String>> extraQueryStringParameters();
    @Nullable
    public abstract List<String> extraScopesToConsent();
    public abstract OpenIdConnectPromptParameter prompt();
    @Nullable
    public abstract HashMap<String, String> requestHeaders();
    public abstract AuthorizationAgent authorizationAgent();

    public static InteractiveTokenCommandParameters.Builder builder() {
        return new AutoValue_InteractiveTokenCommandParameters.Builder();
    }

    public abstract Builder toBuilder();

    public InteractiveTokenCommandParameters addDefaultScopes(List<String> defaultScopes){
        InteractiveTokenCommandParameters.Builder builder = this.toBuilder();
        Set<String> requestedScopes = this.scopes();
        requestedScopes.addAll(defaultScopes);
        // sanitize empty and null scopes
        requestedScopes.removeAll(Arrays.asList("", null));
        builder.setScopes(requestedScopes);
        return builder.build();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder setScopes(Set<String> value);
        public abstract Builder setClientId(String value);
        public abstract Builder setRedirectUri(String value);
        public abstract Builder setAuthority(Authority value);
        public abstract Builder setClaimsRequestJson(String value);
        public abstract Builder setForceRefresh(Boolean value);
        public abstract Builder setLoginHint(String value);
        public abstract Builder setExtraQueryStringParameters(List<Pair<String, String>> value);
        public abstract Builder setExtraScopesToConsent(List<String> value);
        public abstract Builder setPrompt(OpenIdConnectPromptParameter value);
        public abstract Builder setRequestHeaders(HashMap<String, String> value);
        public abstract Builder setAuthorizationAgent(AuthorizationAgent value);
        public abstract Builder setAccountRecord(IAccountRecord value);
        public abstract InteractiveTokenCommandParameters build();
    }

}
