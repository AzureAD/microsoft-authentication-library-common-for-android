package com.microsoft.identity.common.internal.request.generated;

import android.util.Pair;

import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.microsoft.identity.common.internal.authorities.Authority;
import com.microsoft.identity.common.internal.providers.oauth2.OpenIdConnectPromptParameter;
import com.microsoft.identity.common.internal.request.BrokerAcquireTokenOperationParameters;
import com.microsoft.identity.common.internal.ui.AuthorizationAgent;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

@AutoValue
@AutoValue.CopyAnnotations
public abstract class BrokerInteractiveTokenCommandParameters
        extends CommandParameters
        implements IScopesAddable<BrokerInteractiveTokenCommandParameters>{

    abstract Set<String> scopes();
    abstract String clientId();
    abstract String redirectUri();
    abstract Authority authority();
    @Nullable
    abstract String claimsRequestJson();
    @Nullable
    abstract Boolean forceRefresh();
    @Nullable
    abstract String loginHint();
    @Nullable
    abstract List<Pair<String, String>> extraQueryStringParameters();
    @Nullable
    abstract List<String> extraScopesToConsent();
    abstract OpenIdConnectPromptParameter prompt();
    @Nullable
    abstract HashMap<String, String> requestHeaders();
    abstract AuthorizationAgent authorizationAgent();
    @Nullable
    abstract String callerPackageName();
    abstract int callerUid();
    @Nullable
    abstract String callerAppVersion();

    /** Specifying that this acquireToken operation was hit by an interrupt, which needs to be interactively resolved.*/
    @Nullable
    abstract Boolean shouldResolveInterrupt();
    abstract BrokerAcquireTokenOperationParameters.RequestType requestType();

    public static InteractiveTokenCommandParameters.Builder builder() {
        return new AutoValue_InteractiveTokenCommandParameters.Builder();
    }

    public abstract Builder toBuilder();

    public BrokerInteractiveTokenCommandParameters addDefaultScopes(List<String> defaultScopes){
        BrokerInteractiveTokenCommandParameters.Builder builder = this.toBuilder();
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
        public abstract Builder setCallerPackageName(String value);
        public abstract Builder setCallerUid(int value);
        public abstract Builder setCallerAppVersion(String value);
        public abstract Builder setShouldResolveInterrupt(Boolean value);
        public abstract Builder setRequestType(BrokerAcquireTokenOperationParameters.RequestType value);
        public abstract BrokerInteractiveTokenCommandParameters build();
    }

}

