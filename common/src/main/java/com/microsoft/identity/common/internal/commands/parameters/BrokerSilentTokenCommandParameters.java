package com.microsoft.identity.common.internal.commands.parameters;

import android.accounts.Account;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder(toBuilder = true)
public class BrokerSilentTokenCommandParameters extends SilentTokenCommandParameters {

    private String callerPackageName;
    private int callerUid;
    private String callerAppVersion;
    private String brokerVersion;

    private Account accountManagerAccount;
    private String homeAccountId;
    private String localAccountId;
    private int sleepTimeBeforePrtAcquisition;
    private String loginHint;

//    @Builder(builderMethodName = "brokerSilentTokenCommandParametersBuilder")
//    public BrokerSilentTokenCommandParameters(String correlationId, String applicationName, String applicationVersion, String requiredBrokerProtocolVersion, SdkType sdkType, String sdkVersion, Context androidApplicationContext, OAuth2TokenCache oAuth2TokenCache, boolean isSharedDevice, String clientId, String redirectUri, @NonNull Set<String> scopes, Authority authority, String claimsRequestJson, List<Pair<String, String>> extraQueryStringParameters, List<String> extraScopesToConsent, IAccountRecord account, AbstractAuthenticationScheme authenticationScheme, boolean forceRefresh, String callerPackageName, int callerUid, String callerAppVersion, String brokerVersion, Account accountManagerAccount, String homeAccountId, String localAccountId, int sleepTimeBeforePrtAcquisition, String loginHint) {
//        super(correlationId, applicationName, applicationVersion, requiredBrokerProtocolVersion, sdkType, sdkVersion, androidApplicationContext, oAuth2TokenCache, isSharedDevice, clientId, redirectUri, scopes, authority, claimsRequestJson, extraQueryStringParameters, extraScopesToConsent, account, authenticationScheme, forceRefresh);
//        this.callerPackageName = callerPackageName;
//        this.callerUid = callerUid;
//        this.callerAppVersion = callerAppVersion;
//        this.brokerVersion = brokerVersion;
//        this.accountManagerAccount = accountManagerAccount;
//        this.homeAccountId = homeAccountId;
//        this.localAccountId = localAccountId;
//        this.sleepTimeBeforePrtAcquisition = sleepTimeBeforePrtAcquisition;
//        this.loginHint = loginHint;
//    }
}
