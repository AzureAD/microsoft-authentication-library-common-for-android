package com.microsoft.identity.common.internal.commands.parameters;

import android.content.Context;

import com.google.gson.annotations.Expose;
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

    @EqualsAndHashCode.Exclude
    private transient Context androidApplicationContext;

    @EqualsAndHashCode.Exclude
    private transient OAuth2TokenCache oAuth2TokenCache;

    @EqualsAndHashCode.Exclude
    private transient boolean isSharedDevice;

    @EqualsAndHashCode.Exclude
    @Expose()
    private String applicationName;

    @EqualsAndHashCode.Exclude
    @Expose()
    private String applicationVersion;

    @Expose()
    private String requiredBrokerProtocolVersion;

    @Builder.Default
    @Expose()
    private SdkType sdkType = SdkType.MSAL;

    @Expose()
    private String sdkVersion;

    @Expose()
    private String clientId;

    @Expose()
    private String redirectUri;

    @Setter
    @EqualsAndHashCode.Exclude
    @Expose()
    private String correlationId;
}
