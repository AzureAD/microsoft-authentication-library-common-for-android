package com.microsoft.identity.common.internal.commands.parameters;

import com.google.gson.annotations.Expose;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
public class SilentTokenCommandParameters extends TokenCommandParameters {

    @Expose()
    private boolean forceRefresh;

//    @Builder(builderMethodName = "silentTokenCommandParametersBuilder")
//    SilentTokenCommandParameters(String correlationId, String applicationName, String applicationVersion, String requiredBrokerProtocolVersion, SdkType sdkType, String sdkVersion, Context androidApplicationContext, OAuth2TokenCache oAuth2TokenCache, boolean isSharedDevice, String clientId, String redirectUri, @NonNull Set<String> scopes, Authority authority, String claimsRequestJson, List<Pair<String, String>> extraQueryStringParameters, List<String> extraScopesToConsent, IAccountRecord account, AbstractAuthenticationScheme authenticationScheme, boolean forceRefresh) {
//        super(correlationId, applicationName, applicationVersion, requiredBrokerProtocolVersion, sdkType, sdkVersion, androidApplicationContext, oAuth2TokenCache, isSharedDevice, clientId, redirectUri, scopes, authority, claimsRequestJson, extraQueryStringParameters, extraScopesToConsent, account, authenticationScheme);
//        this.forceRefresh = forceRefresh;
//    }
}
