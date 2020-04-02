package com.microsoft.identity.common.internal.commands.parameters;

import com.microsoft.identity.common.internal.request.BrokerAcquireTokenOperationParameters;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder(toBuilder = true)
public class BrokerInteractiveTokenCommandParameters extends InteractiveTokenCommandParameters {

    private String callerPackageName;
    private int callerUid;
    private String callerAppVersion;
    private String brokerVersion;

    private boolean shouldResolveInterrupt;
    private BrokerAcquireTokenOperationParameters.RequestType requestType;

//    @Builder(builderMethodName = "brokerInteractiveTokenCommandParametersBuilder", toBuilder = true)
//    BrokerInteractiveTokenCommandParameters(String correlationId, String applicationName, String applicationVersion, String requiredBrokerProtocolVersion, SdkType sdkType, String sdkVersion, Context androidApplicationContext, OAuth2TokenCache oAuth2TokenCache, boolean isSharedDevice, String clientId, String redirectUri, @NonNull Set<String> scopes, Authority authority, String claimsRequestJson, List<Pair<String, String>> extraQueryStringParameters, List<String> extraScopesToConsent, IAccountRecord account, AbstractAuthenticationScheme authenticationScheme, Activity activity, Fragment fragment, String loginHint, OpenIdConnectPromptParameter prompt, HashMap<String, String> requestHeaders, AuthorizationAgent authorizationAgent, boolean brokerBrowserSupportEnabled, boolean isWebViewZoomEnabled, boolean isWebViewZoomControlsEnabled, List<BrowserDescriptor> browserSafeList, String callerPackageName, int callerUid, String callerAppVersion, String brokerVersion, boolean shouldResolveInterrupt, BrokerAcquireTokenOperationParameters.RequestType requestType) {
//        super(correlationId, applicationName, applicationVersion, requiredBrokerProtocolVersion, sdkType, sdkVersion, androidApplicationContext, oAuth2TokenCache, isSharedDevice, clientId, redirectUri, scopes, authority, claimsRequestJson, extraQueryStringParameters, extraScopesToConsent, account, authenticationScheme, activity, fragment, loginHint, prompt, requestHeaders, authorizationAgent, brokerBrowserSupportEnabled, isWebViewZoomEnabled, isWebViewZoomControlsEnabled, browserSafeList);
//        this.callerPackageName = callerPackageName;
//        this.callerUid = callerUid;
//        this.callerAppVersion = callerAppVersion;
//        this.brokerVersion = brokerVersion;
//        this.shouldResolveInterrupt = shouldResolveInterrupt;
//        this.requestType = requestType;
//    }

    /**
     * Helper method to identify if the request originated from Broker itself or from client libraries.
     *
     * @return : true if request is the request is originated from Broker, false otherwise
     */
    public boolean isRequestFromBroker() {
        return requestType == BrokerAcquireTokenOperationParameters.RequestType.BROKER_RT_REQUEST ||
                requestType == BrokerAcquireTokenOperationParameters.RequestType.RESOLVE_INTERRUPT;
    }
}
