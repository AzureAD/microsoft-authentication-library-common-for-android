// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.common.internal.request;

import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.ACCOUNT_CLIENTID_KEY;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.ACCOUNT_CORRELATIONID;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.ACCOUNT_HOME_ACCOUNT_ID;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.ACCOUNT_REDIRECT;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.AUTH_SCHEME_PARAMS_POP;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.BROKER_REQUEST_V2;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.BROKER_REQUEST_V2_COMPRESSED;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.CALLER_INFO_UID;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.CLIENT_ADVERTISED_MAXIMUM_BP_VERSION_KEY;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.CLIENT_CONFIGURED_MINIMUM_BP_VERSION_KEY;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.ENVIRONMENT;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.CAN_FOCI_APPS_CONSTRUCT_ACCOUNTS_FROM_PRT_ID_TOKEN_KEY;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.MSAL_TO_BROKER_PROTOCOL_VERSION_CODE;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.NEGOTIATED_BP_VERSION_KEY;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.REQUEST_AUTHORITY;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.SHOULD_SEND_PKEYAUTH_HEADER_TO_THE_TOKEN_ENDPOINT;
import static com.microsoft.identity.common.internal.util.GzipUtil.compressString;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.internal.broker.BrokerRequest;
import com.microsoft.identity.common.java.commands.parameters.AcquirePrtSsoTokenCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.GenerateShrCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.RemoveAccountCommandParameters;
import com.microsoft.identity.common.java.opentelemetry.OTelUtility;
import com.microsoft.identity.common.java.opentelemetry.SerializableSpanContext;
import com.microsoft.identity.common.java.opentelemetry.SpanExtension;
import com.microsoft.identity.common.java.ui.BrowserDescriptor;
import com.microsoft.identity.common.java.util.BrokerProtocolVersionUtil;
import com.microsoft.identity.common.java.util.QueryParamsAdapter;
import com.microsoft.identity.common.java.authorities.AzureActiveDirectoryAuthority;
import com.microsoft.identity.common.java.authscheme.AuthenticationSchemeFactory;
import com.microsoft.identity.common.java.authscheme.INameable;
import com.microsoft.identity.common.java.authscheme.PopAuthenticationSchemeInternal;
import com.microsoft.identity.common.java.commands.parameters.CommandParameters;
import com.microsoft.identity.common.java.commands.parameters.InteractiveTokenCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.SilentTokenCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.TokenCommandParameters;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.AzureActiveDirectory;
import com.microsoft.identity.common.java.providers.oauth2.OpenIdConnectPromptParameter;
import com.microsoft.identity.common.java.ui.AuthorizationAgent;
import com.microsoft.identity.common.java.util.StringUtil;
import com.microsoft.identity.common.logging.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import io.opentelemetry.api.trace.Span;

public class MsalBrokerRequestAdapter implements IBrokerRequestAdapter {

    private static final String TAG = MsalBrokerRequestAdapter.class.getName();

    @Override
    public BrokerRequest brokerRequestFromAcquireTokenParameters(@NonNull final InteractiveTokenCommandParameters parameters) {
        final String methodTag = TAG + ":brokerRequestFromAcquireTokenParameters";
        Logger.info(methodTag, "Constructing result bundle from AcquireTokenOperationParameters.");

        final String extraQueryStringParameter = parameters.getExtraQueryStringParameters() != null ?
                QueryParamsAdapter._toJson(parameters.getExtraQueryStringParameters())
                : null;
        final String extraOptions = parameters.getExtraOptions() != null ?
                QueryParamsAdapter._toJson(parameters.getExtraOptions()) : null;
        final BrokerRequest brokerRequest = BrokerRequest.builder()
                .authority(parameters.getAuthority().getAuthorityURL().toString())
                .scope(TextUtils.join(" ", parameters.getScopes()))
                .redirect(parameters.getRedirectUri())
                .clientId(parameters.getClientId())
                .brkRedirect(parameters.getBrkRedirectUri())
                .brkClientId(parameters.getBrkClientId())
                .userName(parameters.getLoginHint())
                .extraQueryStringParameter(extraQueryStringParameter)
                .extraOptions(extraOptions)
                .prompt((OpenIdConnectPromptParameter.UNSET.name().equals(parameters.getPrompt().name())) ? null : parameters.getPrompt().name())
                .claims(parameters.getClaimsRequestJson())
                .forceRefresh(parameters.isForceRefresh())
                .correlationId(parameters.getCorrelationId())
                .applicationName(parameters.getApplicationName())
                .applicationVersion(parameters.getApplicationVersion())
                .msalVersion(parameters.getSdkVersion())
                .sdkType(parameters.getSdkType())
                .environment(AzureActiveDirectory.getEnvironment().name())
                .multipleCloudsSupported(getMultipleCloudsSupported(parameters))
                .authorizationAgent(
                        parameters.isBrokerBrowserSupportEnabled() ?
                                AuthorizationAgent.BROWSER.name() :
                                AuthorizationAgent.WEBVIEW.name()
                ).authenticationScheme(parameters.getAuthenticationScheme())
                .powerOptCheckEnabled(parameters.isPowerOptCheckEnabled())
                .spanContext(SerializableSpanContext.builder()
                        .traceId(SpanExtension.current().getSpanContext().getTraceId())
                        .spanId(SpanExtension.current().getSpanContext().getSpanId())
                        .traceFlags(SpanExtension.current().getSpanContext().getTraceFlags().asByte())
                        .parentSpanName(OTelUtility.getCurrentSpanName())
                        .build()
                )
                .build();

        return brokerRequest;
    }

    @Override
    public BrokerRequest brokerRequestFromSilentOperationParameters(@NonNull final SilentTokenCommandParameters parameters) {
        final String methodTag = TAG + ":brokerRequestFromSilentOperationParameters";

        Logger.info(methodTag, "Constructing result bundle from AcquireTokenSilentOperationParameters.");
        final String extraOptions = parameters.getExtraOptions() != null ?
                QueryParamsAdapter._toJson(parameters.getExtraOptions()) : null;

        final BrokerRequest brokerRequest = BrokerRequest.builder()
                .authority(parameters.getAuthority().getAuthorityURL().toString())
                .scope(TextUtils.join(" ", parameters.getScopes()))
                .redirect(parameters.getRedirectUri())
                .extraOptions(extraOptions)
                .clientId(parameters.getClientId())
                .brkRedirect(parameters.getBrkRedirectUri())
                .brkClientId(parameters.getBrkClientId())
                .homeAccountId(parameters.getAccount().getHomeAccountId())
                .localAccountId(parameters.getAccount().getLocalAccountId())
                .userName(parameters.getAccount().getUsername())
                .claims(parameters.getClaimsRequestJson())
                .forceRefresh(parameters.isForceRefresh())
                .correlationId(parameters.getCorrelationId())
                .applicationName(parameters.getApplicationName())
                .applicationVersion(parameters.getApplicationVersion())
                .msalVersion(parameters.getSdkVersion())
                .sdkType(parameters.getSdkType())
                .environment(AzureActiveDirectory.getEnvironment().name())
                .multipleCloudsSupported(getMultipleCloudsSupported(parameters))
                .authenticationScheme(parameters.getAuthenticationScheme())
                .powerOptCheckEnabled(parameters.isPowerOptCheckEnabled())
                .spanContext(SerializableSpanContext.builder()
                        .traceId(SpanExtension.current().getSpanContext().getTraceId())
                        .spanId(SpanExtension.current().getSpanContext().getSpanId())
                        .traceFlags(SpanExtension.current().getSpanContext().getTraceFlags().asByte())
                        .parentSpanName(OTelUtility.getCurrentSpanName())
                        .build()
                )
                .build();

        return brokerRequest;
    }

    public @NonNull Bundle getRequestBundleForSsoToken(final @NonNull AcquirePrtSsoTokenCommandParameters parameters,
                                                       final @NonNull String negotiatedBrokerProtocolVersion) {
        Bundle requestBundle = new Bundle();
        requestBundle.putString(AuthenticationConstants.Broker.ACCOUNT_NAME, parameters.getAccountName());
        requestBundle.putString(AuthenticationConstants.Broker.ACCOUNT_HOME_ACCOUNT_ID, parameters.getHomeAccountId());
        requestBundle.putString(AuthenticationConstants.Broker.ACCOUNT_LOCAL_ACCOUNT_ID, parameters.getLocalAccountId());
        if (parameters.getRequestAuthority() != null) {
            requestBundle.putString(REQUEST_AUTHORITY, parameters.getRequestAuthority());
        }
        if (parameters.getSsoUrl() != null) {
            requestBundle.putString(AuthenticationConstants.Broker.BROKER_SSO_URL_KEY, parameters.getSsoUrl());
        }
        requestBundle.putString(
                AuthenticationConstants.Broker.NEGOTIATED_BP_VERSION_KEY,
                negotiatedBrokerProtocolVersion
        );
        requestBundle.putString(ACCOUNT_CORRELATIONID, parameters.getCorrelationId());
        return requestBundle;
    }

    /**
     * Method to construct a request bundle for broker hello request.
     *
     * @param parameters input parameters.
     * @return request bundle to perform hello.
     */
    public Bundle getRequestBundleForHello(@NonNull final CommandParameters parameters) {
        final Bundle requestBundle = new Bundle();
        requestBundle.putString(
                CLIENT_ADVERTISED_MAXIMUM_BP_VERSION_KEY,
                MSAL_TO_BROKER_PROTOCOL_VERSION_CODE
        );

        if (!StringUtil.isNullOrEmpty(parameters.getRequiredBrokerProtocolVersion())) {
            requestBundle.putString(
                    CLIENT_CONFIGURED_MINIMUM_BP_VERSION_KEY,
                    parameters.getRequiredBrokerProtocolVersion()
            );
        }

        return requestBundle;
    }

    /**
     * Method to construct a request bundle for broker acquireTokenInteractive request.
     *
     * @param parameters                      input parameters
     * @param negotiatedBrokerProtocolVersion protocol version returned by broker hello.
     * @return request Bundle
     */
    public Bundle getRequestBundleForAcquireTokenInteractive(@NonNull final InteractiveTokenCommandParameters parameters,
                                                             @Nullable final String negotiatedBrokerProtocolVersion) {
        final BrokerRequest brokerRequest = brokerRequestFromAcquireTokenParameters(parameters);
        return getRequestBundleFromBrokerRequest(
                brokerRequest,
                negotiatedBrokerProtocolVersion,
                parameters.getRequiredBrokerProtocolVersion()
        );
    }

    /**
     * Method to construct a request bundle for broker acquireTokenSilent request.
     *
     * @param context                         {@link Context}
     * @param parameters                      input parameters
     * @param negotiatedBrokerProtocolVersion protocol version returned by broker hello.
     * @return request Bundle
     */
    public Bundle getRequestBundleForAcquireTokenSilent(@NonNull final Context context,
                                                        @NonNull final SilentTokenCommandParameters parameters,
                                                        @Nullable final String negotiatedBrokerProtocolVersion) {
        final MsalBrokerRequestAdapter msalBrokerRequestAdapter = new MsalBrokerRequestAdapter();

        final BrokerRequest brokerRequest = msalBrokerRequestAdapter.
                brokerRequestFromSilentOperationParameters(parameters);

        final Bundle requestBundle = getRequestBundleFromBrokerRequest(
                brokerRequest,
                negotiatedBrokerProtocolVersion,
                parameters.getRequiredBrokerProtocolVersion()
        );

        requestBundle.putInt(
                CALLER_INFO_UID,
                context.getApplicationInfo().uid
        );

        return requestBundle;
    }

    private Bundle getRequestBundleFromBrokerRequest(@NonNull BrokerRequest brokerRequest,
                                                     @Nullable String negotiatedBrokerProtocolVersion,
                                                     @Nullable String requiredBrokerProtocolVersion) {
        final String methodTag = TAG + ":getRequestBundleFromBrokerRequest";
        final Bundle requestBundle = new Bundle();

        if (BrokerProtocolVersionUtil.canCompressBrokerPayloads(negotiatedBrokerProtocolVersion)) {
            try {
                final String jsonString = AuthenticationSchemeTypeAdapter.getGsonInstance().toJson(brokerRequest, BrokerRequest.class);
                byte[] compressedBytes = compressString(jsonString);
                Logger.info(methodTag, "Broker Result, raw payload size:"
                        + jsonString.getBytes(AuthenticationConstants.CHARSET_UTF8).length + " ,compressed bytes size: " + compressedBytes.length
                );
                requestBundle.putByteArray(BROKER_REQUEST_V2_COMPRESSED, compressedBytes);
            } catch (IOException e) {
                Logger.error(methodTag, "Compression to bytes failed, sending broker request as json String", e);
                requestBundle.putString(
                        BROKER_REQUEST_V2,
                        AuthenticationSchemeTypeAdapter.getGsonInstance().toJson(brokerRequest, BrokerRequest.class)
                );
            }
        } else {
            Logger.info(methodTag, "Broker protocol version: " + negotiatedBrokerProtocolVersion +
                    " lower than compression changes, sending as string"
            );
            requestBundle.putString(
                    BROKER_REQUEST_V2,
                    AuthenticationSchemeTypeAdapter.getGsonInstance().toJson(brokerRequest, BrokerRequest.class)
            );
        }
        requestBundle.putString(NEGOTIATED_BP_VERSION_KEY, negotiatedBrokerProtocolVersion);
        requestBundle.putBoolean(
                SHOULD_SEND_PKEYAUTH_HEADER_TO_THE_TOKEN_ENDPOINT,
                BrokerProtocolVersionUtil.canSendPKeyAuthHeaderToTheTokenEndpoint(requiredBrokerProtocolVersion)
        );
        return requestBundle;
    }

    /**
     * Method to construct a request bundle for broker getAccounts request.
     *
     * @param parameters                      input parameters
     * @param negotiatedBrokerProtocolVersion protocol version returned by broker hello.
     * @return request Bundle
     */
    public Bundle getRequestBundleForGetAccounts(@NonNull final CommandParameters parameters,
                                                 @Nullable final String negotiatedBrokerProtocolVersion) {
        final Bundle requestBundle = new Bundle();
        requestBundle.putString(ACCOUNT_CLIENTID_KEY, parameters.getClientId());
        requestBundle.putString(ACCOUNT_REDIRECT, parameters.getRedirectUri());
        requestBundle.putString(NEGOTIATED_BP_VERSION_KEY, negotiatedBrokerProtocolVersion);
        requestBundle.putBoolean(
                CAN_FOCI_APPS_CONSTRUCT_ACCOUNTS_FROM_PRT_ID_TOKEN_KEY,
                BrokerProtocolVersionUtil.canFociAppsConstructAccountsFromPrtIdTokens(parameters.getRequiredBrokerProtocolVersion())
        );
        //Disable the environment and tenantID. Just return all accounts belong to this clientID.
        return requestBundle;
    }

    /**
     * Method to construct a request bundle for broker removeAccount request.
     *
     * @param parameters                      input parameters
     * @param negotiatedBrokerProtocolVersion protocol version returned by broker hello.
     * @return request Bundle
     */
    public Bundle getRequestBundleForRemoveAccount(@NonNull final RemoveAccountCommandParameters parameters,
                                                   @Nullable final String negotiatedBrokerProtocolVersion) {
        final Bundle requestBundle = new Bundle();
        if (null != parameters.getAccount()) {
            requestBundle.putString(ACCOUNT_CLIENTID_KEY, parameters.getClientId());
            requestBundle.putString(ENVIRONMENT, parameters.getAccount().getEnvironment());
            requestBundle.putString(ACCOUNT_HOME_ACCOUNT_ID, parameters.getAccount().getHomeAccountId());
        }
        requestBundle.putString(NEGOTIATED_BP_VERSION_KEY, negotiatedBrokerProtocolVersion);

        return requestBundle;
    }

    /**
     * Method to construct a request bundle for broker removeAccount request.
     *
     * @param parameters                      input parameters
     * @param negotiatedBrokerProtocolVersion protocol version returned by broker hello.
     * @return request Bundle
     */
    public Bundle getRequestBundleForRemoveAccountFromSharedDevice(@NonNull final RemoveAccountCommandParameters parameters,
                                                                   @Nullable final String negotiatedBrokerProtocolVersion) {
        final Bundle requestBundle = new Bundle();
        requestBundle.putString(NEGOTIATED_BP_VERSION_KEY, negotiatedBrokerProtocolVersion);

        return requestBundle;
    }

    /**
     * Method to construct a request {@link Bundle} for broker generateShr.
     *
     * @param parameters                      Input request params.
     * @param negotiatedBrokerProtocolVersion The negotiated broker protocol version in use.
     * @return The result Bundle from the Broker.
     */
    public Bundle getRequestBundleForGenerateShr(@NonNull final GenerateShrCommandParameters parameters,
                                                 @NonNull final String negotiatedBrokerProtocolVersion) throws ClientException {
        final String clientId = parameters.getClientId();
        final String homeAccountId = parameters.getHomeAccountId();

        // Convert the supplied public class to the internal representation
        final PopAuthenticationSchemeInternal popParameters =
                (PopAuthenticationSchemeInternal) AuthenticationSchemeFactory.createScheme(
                        parameters.getPlatformComponents(),
                        (INameable) parameters.getPopParameters()
                );

        final String popParamsJson = AuthenticationSchemeTypeAdapter.getGsonInstance().toJson(
                popParameters,
                PopAuthenticationSchemeInternal.class
        );

        final Bundle requestBundle = new Bundle();
        requestBundle.putString(ACCOUNT_CLIENTID_KEY, clientId);
        requestBundle.putString(ACCOUNT_HOME_ACCOUNT_ID, homeAccountId);
        requestBundle.putString(AUTH_SCHEME_PARAMS_POP, popParamsJson);
        requestBundle.putString(NEGOTIATED_BP_VERSION_KEY, negotiatedBrokerProtocolVersion);

        return requestBundle;
    }

    private boolean getMultipleCloudsSupported(@NonNull final TokenCommandParameters parameters) {
        if (parameters.getAuthority() instanceof AzureActiveDirectoryAuthority) {
            final AzureActiveDirectoryAuthority authority = (AzureActiveDirectoryAuthority) parameters.getAuthority();
            return authority.isMultipleCloudsSupported();
        } else {
            return false;
        }
    }

    /**
     * List of System Browsers which can be used from broker, currently only Chrome is supported.
     * This information here is populated from the default browser safelist in MSAL.
     *
     * @return
     */
    public static List<BrowserDescriptor> getBrowserSafeListForBroker() {
        List<BrowserDescriptor> browserDescriptors = new ArrayList<>();
        final HashSet<String> signatureHashes = new HashSet<String>();
        signatureHashes.add("7fmduHKTdHHrlMvldlEqAIlSfii1tl35bxj1OXN5Ve8c4lU6URVu4xtSHc3BVZxS6WWJnxMDhIfQN0N0K2NDJg==");
        final BrowserDescriptor chrome = new BrowserDescriptor(
                "com.android.chrome",
                signatureHashes,
                null,
                null
        );
        browserDescriptors.add(chrome);

        return browserDescriptors;
    }
}
