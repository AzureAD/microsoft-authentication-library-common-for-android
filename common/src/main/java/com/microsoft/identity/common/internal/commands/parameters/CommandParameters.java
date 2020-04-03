package com.microsoft.identity.common.internal.commands.parameters;

import android.content.Context;

import com.microsoft.identity.common.internal.providers.oauth2.OAuth2TokenCache;
import com.microsoft.identity.common.internal.request.SdkType;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@EqualsAndHashCode
@SuperBuilder(toBuilder = true)
public class CommandParameters {

    @Setter
    @EqualsAndHashCode.Exclude
    private String correlationId;

    @EqualsAndHashCode.Exclude
    private String applicationName;

    @EqualsAndHashCode.Exclude
    private String applicationVersion;

    private String requiredBrokerProtocolVersion;

    @Builder.Default
    private SdkType sdkType = SdkType.MSAL;

    private String sdkVersion;

    @EqualsAndHashCode.Exclude
    private Context androidApplicationContext;

    @EqualsAndHashCode.Exclude
    private OAuth2TokenCache oAuth2TokenCache;

    @EqualsAndHashCode.Exclude
    private boolean isSharedDevice;

    private String clientId;

    private String redirectUri;

//    CommandParameters(String correlationId, String applicationName, String applicationVersion, String requiredBrokerProtocolVersion, SdkType sdkType, String sdkVersion, Context androidApplicationContext, OAuth2TokenCache oAuth2TokenCache, boolean isSharedDevice, String clientId, String redirectUri) {
//        this.correlationId = correlationId;
//        this.applicationName = applicationName;
//        this.applicationVersion = applicationVersion;
//        this.requiredBrokerProtocolVersion = requiredBrokerProtocolVersion;
//        this.sdkType = sdkType;
//        this.sdkVersion = sdkVersion;
//        this.androidApplicationContext = androidApplicationContext;
//        this.oAuth2TokenCache = oAuth2TokenCache;
//        this.isSharedDevice = isSharedDevice;
//        this.clientId = clientId;
//        this.redirectUri = redirectUri;
//    }
}
