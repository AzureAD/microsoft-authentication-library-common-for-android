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

import android.accounts.Account;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.microsoft.identity.common.java.WarningType;
import com.microsoft.identity.common.internal.authorities.Authority;
import com.microsoft.identity.common.internal.authorities.AzureActiveDirectoryAuthority;
import com.microsoft.identity.common.internal.authorities.Environment;
import com.microsoft.identity.common.internal.authscheme.AbstractAuthenticationScheme;
import com.microsoft.identity.common.internal.authscheme.AuthenticationSchemeFactory;
import com.microsoft.identity.common.internal.authscheme.BearerAuthenticationSchemeInternal;
import com.microsoft.identity.common.internal.authscheme.INameable;
import com.microsoft.identity.common.internal.authscheme.PopAuthenticationSchemeInternal;
import com.microsoft.identity.common.internal.broker.BrokerRequest;
import com.microsoft.identity.common.internal.broker.BrokerValidator;
import com.microsoft.identity.common.internal.commands.parameters.BrokerInteractiveTokenCommandParameters;
import com.microsoft.identity.common.internal.commands.parameters.BrokerSilentTokenCommandParameters;
import com.microsoft.identity.common.internal.commands.parameters.CommandParameters;
import com.microsoft.identity.common.internal.commands.parameters.GenerateShrCommandParameters;
import com.microsoft.identity.common.internal.commands.parameters.InteractiveTokenCommandParameters;
import com.microsoft.identity.common.internal.commands.parameters.RemoveAccountCommandParameters;
import com.microsoft.identity.common.internal.commands.parameters.SilentTokenCommandParameters;
import com.microsoft.identity.common.internal.commands.parameters.TokenCommandParameters;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectory;
import com.microsoft.identity.common.internal.providers.oauth2.OpenIdConnectPromptParameter;
import com.microsoft.identity.common.internal.ui.AuthorizationAgent;
import com.microsoft.identity.common.internal.ui.browser.BrowserDescriptor;
import com.microsoft.identity.common.internal.util.BrokerProtocolVersionUtil;
import com.microsoft.identity.common.internal.util.ClockSkewManager;
import com.microsoft.identity.common.internal.util.QueryParamsAdapter;
import com.microsoft.identity.common.internal.util.StringUtil;
import com.microsoft.identity.common.java.util.IClockSkewManager;
import com.microsoft.identity.common.logging.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.ACCOUNT_CLIENTID_KEY;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.ACCOUNT_HOME_ACCOUNT_ID;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.ACCOUNT_REDIRECT;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.AUTH_SCHEME_PARAMS_POP;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.BROKER_ACTIVITY_NAME;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.BROKER_PACKAGE_NAME;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.BROKER_REQUEST_V2;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.BROKER_REQUEST_V2_COMPRESSED;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.CALLER_INFO_UID;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.CLIENT_ADVERTISED_MAXIMUM_BP_VERSION_KEY;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.CLIENT_CONFIGURED_MINIMUM_BP_VERSION_KEY;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.ENVIRONMENT;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.MSAL_TO_BROKER_PROTOCOL_VERSION_CODE;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.NEGOTIATED_BP_VERSION_KEY;
import static com.microsoft.identity.common.internal.util.GzipUtil.compressString;
import static com.microsoft.identity.common.internal.util.GzipUtil.decompressBytesToString;

public class MsalBrokerRequestAdapter implements IBrokerRequestAdapter {

    private static final String TAG = MsalBrokerRequestAdapter.class.getName();

    // TODO: provide this from a factory
    public static Gson sRequestAdapterGsonInstance = new GsonBuilder()
                      .registerTypeAdapter(
                            AbstractAuthenticationScheme.class,
                            new AuthenticationSchemeTypeAdapter()
                      ).create();

    @Override
    public BrokerRequest brokerRequestFromAcquireTokenParameters(@NonNull final InteractiveTokenCommandParameters parameters) {
        Logger.info(TAG, "Constructing result bundle from AcquireTokenOperationParameters.");

        final String extraQueryStringParameter = parameters.getExtraQueryStringParameters() != null ?
                QueryParamsAdapter._toJson(parameters.getExtraQueryStringParameters())
                : null;
        final String extraOptions = parameters.getExtraOptions() != null ?
                QueryParamsAdapter._toJson(parameters.getExtraOptions()) : null;
        final BrokerRequest brokerRequest = BrokerRequest.builder()
                .authority(parameters.getAuthority().getAuthorityURL().toString())
                .scope(TextUtils.join(" ", parameters.getScopes()))
                .redirect(getRedirectUri(parameters))
                .clientId(parameters.getClientId())
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
                .build();

        return brokerRequest;
    }

    @Override
    public BrokerRequest brokerRequestFromSilentOperationParameters(@NonNull final SilentTokenCommandParameters parameters) {

        Logger.info(TAG, "Constructing result bundle from AcquireTokenSilentOperationParameters.");
        final String extraOptions = parameters.getExtraOptions() != null ?
                QueryParamsAdapter._toJson(parameters.getExtraOptions()) : null;

        final BrokerRequest brokerRequest = BrokerRequest.builder()
                .authority(parameters.getAuthority().getAuthorityURL().toString())
                .scope(TextUtils.join(" ", parameters.getScopes()))
                .redirect(getRedirectUri(parameters))
                .extraOptions(extraOptions)
                .clientId(parameters.getClientId())
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
                .build();

        return brokerRequest;
    }

    @NonNull
    private static AbstractAuthenticationScheme getAuthenticationScheme(
            @NonNull final Context context,
            @NonNull final BrokerRequest request) {
        final AbstractAuthenticationScheme requestScheme = request.getAuthenticationScheme();

        if (null == requestScheme) {
            // Default assumes the scheme is Bearer
            return new BearerAuthenticationSchemeInternal();
        } else {
            if (requestScheme instanceof PopAuthenticationSchemeInternal) {
                final IClockSkewManager clockSkewManager = new ClockSkewManager(context);
                ((PopAuthenticationSchemeInternal) requestScheme).setClockSkewManager(clockSkewManager);
            }

            return requestScheme;
        }
    }

    @Override
    public BrokerInteractiveTokenCommandParameters brokerInteractiveParametersFromActivity(
            @NonNull final Activity callingActivity) {

        Logger.info(TAG, "Constructing BrokerAcquireTokenOperationParameters from calling activity");

        final Intent intent = callingActivity.getIntent();

        final BrokerRequest brokerRequest = brokerRequestFromBundle(intent.getExtras());

        if (brokerRequest == null) {
            Logger.error(TAG, "Broker Result is null, returning empty parameters, " +
                    "validation is expected to fail", null
            );
            return BrokerInteractiveTokenCommandParameters.builder().build();
        }

        int callingAppUid = intent.getIntExtra(CALLER_INFO_UID, 0);

        List<Map.Entry<String, String>> extraQP = QueryParamsAdapter._fromJson(brokerRequest.getExtraQueryStringParameter());
        List<Map.Entry<String, String>> extraOptions = QueryParamsAdapter._fromJson(brokerRequest.getExtraOptions());;

        final AzureActiveDirectoryAuthority authority = AdalBrokerRequestAdapter.getRequestAuthorityWithExtraQP(
                brokerRequest.getAuthority(),
                extraQP
        );

        if (authority != null) {
            authority.setMultipleCloudsSupported(brokerRequest.isMultipleCloudsSupported());
        }

        String correlationIdString = brokerRequest.getCorrelationId();

        if (TextUtils.isEmpty(correlationIdString)) {
            UUID correlationId = UUID.randomUUID();
            correlationIdString = correlationId.toString();
        }
        final String negotiatedBrokerProtocolVersion = intent.getStringExtra(NEGOTIATED_BP_VERSION_KEY);

        Logger.info(TAG, "Authorization agent passed in by MSAL: " + brokerRequest.getAuthorizationAgent());

        @SuppressWarnings("rawtypes") final BrokerInteractiveTokenCommandParameters.BrokerInteractiveTokenCommandParametersBuilder
                commandParametersBuilder = BrokerInteractiveTokenCommandParameters.builder()
                .authenticationScheme(getAuthenticationScheme(callingActivity, brokerRequest))
                .activity(callingActivity)
                .androidApplicationContext(callingActivity.getApplicationContext())
                .sdkType(brokerRequest.getSdkType() == null ? SdkType.MSAL : brokerRequest.getSdkType())
                .sdkVersion(brokerRequest.getMsalVersion())
                .callerUid(callingAppUid)
                .applicationName(brokerRequest.getApplicationName())
                .applicationVersion(brokerRequest.getApplicationVersion())
                .callerPackageName(brokerRequest.getApplicationName())
                .callerAppVersion(brokerRequest.getApplicationVersion())
                .extraQueryStringParameters(extraQP)
                .authority(authority)
                .extraOptions(extraOptions)
                .scopes(getScopesAsSet(brokerRequest.getScope()))
                .clientId(brokerRequest.getClientId())
                .redirectUri(brokerRequest.getRedirect())
                .loginHint(brokerRequest.getUserName())
                .correlationId(correlationIdString)
                .claimsRequestJson(brokerRequest.getClaims())
                .prompt(brokerRequest.getPrompt() != null ?
                        OpenIdConnectPromptParameter.valueOf(brokerRequest.getPrompt()) :
                        OpenIdConnectPromptParameter.UNSET)
                .negotiatedBrokerProtocolVersion(negotiatedBrokerProtocolVersion)
                .powerOptCheckEnabled(brokerRequest.isPowerOptCheckEnabled());

        if (AuthorizationAgent.BROWSER.name().equalsIgnoreCase(brokerRequest.getAuthorizationAgent())
                && isCallingPackageIntune(brokerRequest.getApplicationName())) { // TODO : Remove this whenever we enable System Browser support in Broker for apps.
            Logger.info(TAG, "Setting Authorization Agent to Browser for Intune app");
            buildCommandParameterBuilder(commandParametersBuilder);
        } else {
            commandParametersBuilder.authorizationAgent(AuthorizationAgent.WEBVIEW);
        }

        // Set Global environment variable for instance discovery if present
        if (!TextUtils.isEmpty(brokerRequest.getEnvironment())) {
            AzureActiveDirectory.setEnvironment(
                    Environment.valueOf(brokerRequest.getEnvironment())
            );
        }

        return commandParametersBuilder.build();
    }

    @SuppressWarnings(WarningType.unchecked_warning)
    private void buildCommandParameterBuilder(@SuppressWarnings(WarningType.rawtype_warning) BrokerInteractiveTokenCommandParameters.BrokerInteractiveTokenCommandParametersBuilder commandParametersBuilder) {
        commandParametersBuilder
                .authorizationAgent(AuthorizationAgent.BROWSER)
                .brokerBrowserSupportEnabled(true)
                .browserSafeList(getBrowserSafeListForBroker());
    }

    @Override
    public BrokerSilentTokenCommandParameters brokerSilentParametersFromBundle(
            @NonNull final Bundle bundle,
            @NonNull final Context context,
            @NonNull final Account account) {

        Logger.info(TAG, "Constructing BrokerAcquireTokenSilentOperationParameters from result bundle");

        final BrokerRequest brokerRequest = brokerRequestFromBundle(bundle);

        if (brokerRequest == null) {
            Logger.error(TAG, "Broker Result is null, returning empty parameters, " +
                    "validation is expected to fail", null
            );

            return BrokerSilentTokenCommandParameters.builder().build();
        }

        int callingAppUid = bundle.getInt(CALLER_INFO_UID);

        final Authority authority = Authority.getAuthorityFromAuthorityUrl(
                brokerRequest.getAuthority()
        );

        if (authority instanceof AzureActiveDirectoryAuthority) {
            ((AzureActiveDirectoryAuthority) authority).setMultipleCloudsSupported(
                    brokerRequest.isMultipleCloudsSupported()
            );
        }

        String correlationIdString = bundle.getString(
                brokerRequest.getCorrelationId()
        );
        if (TextUtils.isEmpty(correlationIdString)) {
            UUID correlationId = UUID.randomUUID();
            correlationIdString = correlationId.toString();
        }

        final String negotiatedBrokerProtocolVersion = bundle.getString(NEGOTIATED_BP_VERSION_KEY);
        List<Map.Entry<String, String>> extraOptions = QueryParamsAdapter._fromJson(brokerRequest.getExtraOptions());

        final BrokerSilentTokenCommandParameters commandParameters = BrokerSilentTokenCommandParameters
                .builder()
                .authenticationScheme(getAuthenticationScheme(context, brokerRequest))
                .androidApplicationContext(context)
                .accountManagerAccount(account)
                .sdkType(brokerRequest.getSdkType() == null ? SdkType.MSAL : brokerRequest.getSdkType())
                .sdkVersion(brokerRequest.getMsalVersion())
                .callerUid(callingAppUid)
                .applicationName(brokerRequest.getApplicationName())
                .applicationVersion(brokerRequest.getApplicationVersion())
                .callerPackageName(brokerRequest.getApplicationName())
                .callerAppVersion(brokerRequest.getApplicationVersion())
                .authority(authority)
                .correlationId(correlationIdString)
                .scopes(getScopesAsSet(brokerRequest.getScope()))
                .redirectUri(brokerRequest.getRedirect())
                .extraOptions(extraOptions)
                .clientId(brokerRequest.getClientId())
                .forceRefresh(brokerRequest.isForceRefresh())
                .claimsRequestJson(brokerRequest.getClaims())
                .loginHint(brokerRequest.getUserName())
                .homeAccountId(brokerRequest.getHomeAccountId())
                .localAccountId(brokerRequest.getLocalAccountId())
                .negotiatedBrokerProtocolVersion(negotiatedBrokerProtocolVersion)
                .powerOptCheckEnabled(brokerRequest.isPowerOptCheckEnabled())
                .build();


        // Set Global environment variable for instance discovery if present
        if (!TextUtils.isEmpty(brokerRequest.getEnvironment())) {
            AzureActiveDirectory.setEnvironment(
                    Environment.valueOf(brokerRequest.getEnvironment())
            );
        }

        return commandParameters;
    }

    @Nullable
    public BrokerRequest brokerRequestFromBundle(@NonNull final Bundle requestBundle) {
        BrokerRequest brokerRequest = null;

        if (requestBundle.containsKey(BROKER_REQUEST_V2_COMPRESSED)) {
            try {
                final String deCompressedString = decompressBytesToString(
                        requestBundle.getByteArray(BROKER_REQUEST_V2_COMPRESSED)
                );
                brokerRequest = sRequestAdapterGsonInstance.fromJson(
                        deCompressedString, BrokerRequest.class
                );
            } catch (final IOException e) {
                // We would ideally never run into this case as compression would always work as expected.
                // The caller should handle the null value of broker request.
                Logger.error(TAG, "Decompression of Broker Request failed," +
                        " unable to continue with Broker Request", e
                );
            }

        } else {
            brokerRequest = sRequestAdapterGsonInstance.fromJson(
                    requestBundle.getString(BROKER_REQUEST_V2),
                    BrokerRequest.class
            );
        }
        return brokerRequest;
    }

    /**
     * Helper method to transforn scopes string to Set
     */
    private Set<String> getScopesAsSet(@Nullable final String scopeString) {
        if (TextUtils.isEmpty(scopeString)) {
            return new HashSet<>();
        }
        final String[] scopes = scopeString.split(" ");
        return new HashSet<>(Arrays.asList(scopes));
    }

    /**
     * Helper method to get redirect uri from parameters, calculates from package signature if not available.
     */
    private String getRedirectUri(@NonNull TokenCommandParameters parameters) {
        if (TextUtils.isEmpty(parameters.getRedirectUri())) {
            return BrokerValidator.getBrokerRedirectUri(
                    parameters.getAndroidApplicationContext(),
                    parameters.getApplicationName()
            );
        }
        return parameters.getRedirectUri();
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

        if (!StringUtil.isEmpty(parameters.getRequiredBrokerProtocolVersion())) {
            requestBundle.putString(
                    CLIENT_CONFIGURED_MINIMUM_BP_VERSION_KEY,
                    parameters.getRequiredBrokerProtocolVersion()
            );
        }

        return requestBundle;
    }

    /**
     * Method to construct a request intent for broker acquireTokenInteractive request.
     * Only used in case of BrokerContentProvider
     *
     * @param resultBundle                    result Bundle returned by broker.
     * @param parameters                      input parameters
     * @param negotiatedBrokerProtocolVersion protocol version returned by broker hello.
     * @return request Intent
     */
    public Intent getRequestIntentForAcquireTokenInteractive(@NonNull final Bundle resultBundle,
                                                             @NonNull final InteractiveTokenCommandParameters parameters,
                                                             @Nullable final String negotiatedBrokerProtocolVersion) {
        final Bundle requestBundle = getRequestBundleForAcquireTokenInteractive(
                parameters,
                negotiatedBrokerProtocolVersion
        );
        Intent interactiveRequestIntent = new Intent();
        interactiveRequestIntent.putExtras(requestBundle);
        interactiveRequestIntent.putExtras(resultBundle);
        interactiveRequestIntent.setPackage(resultBundle.getString(BROKER_PACKAGE_NAME));
        interactiveRequestIntent.setClassName(
                resultBundle.getString(BROKER_PACKAGE_NAME, ""),
                resultBundle.getString(BROKER_ACTIVITY_NAME, "")
        );
        interactiveRequestIntent.putExtra(NEGOTIATED_BP_VERSION_KEY, negotiatedBrokerProtocolVersion);
        return interactiveRequestIntent;
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
        return getRequestBundleFromBrokerRequest(brokerRequest, negotiatedBrokerProtocolVersion);
    }

    /**
     * Method to construct a request bundle for broker acquireTokenSilent request.
     *
     * @param parameters                      input parameters
     * @param negotiatedBrokerProtocolVersion protocol version returned by broker hello.
     * @return request Bundle
     */
    public Bundle getRequestBundleForAcquireTokenSilent(@NonNull final SilentTokenCommandParameters parameters,
                                                        @Nullable final String negotiatedBrokerProtocolVersion) {
        final MsalBrokerRequestAdapter msalBrokerRequestAdapter = new MsalBrokerRequestAdapter();

        final BrokerRequest brokerRequest = msalBrokerRequestAdapter.
                brokerRequestFromSilentOperationParameters(parameters);

        final Bundle requestBundle = getRequestBundleFromBrokerRequest(
                brokerRequest,
                negotiatedBrokerProtocolVersion
        );

        requestBundle.putInt(
                CALLER_INFO_UID,
                parameters.getAndroidApplicationContext().getApplicationInfo().uid
        );

        return requestBundle;
    }

    private Bundle getRequestBundleFromBrokerRequest(@NonNull BrokerRequest brokerRequest,
                                                     @Nullable String negotiatedBrokerProtocolVersion) {
        final Bundle requestBundle = new Bundle();

        if (BrokerProtocolVersionUtil.canCompressBrokerPayloads(negotiatedBrokerProtocolVersion)) {
            try {
                final String jsonString = sRequestAdapterGsonInstance.toJson(brokerRequest, BrokerRequest.class);
                byte[] compressedBytes = compressString(jsonString);
                Logger.info(TAG, "Broker Result, raw payload size:"
                        + jsonString.getBytes().length + " ,compressed bytes size: " + compressedBytes.length
                );
                requestBundle.putByteArray(BROKER_REQUEST_V2_COMPRESSED, compressedBytes);
            } catch (IOException e) {
                Logger.error(TAG, "Compression to bytes failed, sending broker request as json String", e);
                requestBundle.putString(
                        BROKER_REQUEST_V2,
                        sRequestAdapterGsonInstance.toJson(brokerRequest, BrokerRequest.class)
                );
            }
        } else {
            Logger.info(TAG, "Broker protocol version: " + negotiatedBrokerProtocolVersion +
                    " lower than compression changes, sending as string"
            );
            requestBundle.putString(
                    BROKER_REQUEST_V2,
                    sRequestAdapterGsonInstance.toJson(brokerRequest, BrokerRequest.class)
            );
        }
        requestBundle.putString(NEGOTIATED_BP_VERSION_KEY, negotiatedBrokerProtocolVersion);
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
                                                 @NonNull final String negotiatedBrokerProtocolVersion) {
        final String clientId = parameters.getClientId();
        final String homeAccountId = parameters.getHomeAccountId();

        // Convert the supplied public class to the internal representation
        final PopAuthenticationSchemeInternal popParameters =
                (PopAuthenticationSchemeInternal) AuthenticationSchemeFactory.createScheme(
                        parameters.getAndroidApplicationContext(),
                        (INameable) parameters.getPopParameters()
                );

        final String popParamsJson = sRequestAdapterGsonInstance.toJson(
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
            return authority.getMultipleCloudsSupported();
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

    /**
     * Helper method to validate in Broker that the calling package in Microsoft Intune
     * to allow System Webview Support.
     */
    private boolean isCallingPackageIntune(@NonNull final String packageName) {
        final String methodName = ":isCallingPackageIntune";
        final String intunePackageName = "com.microsoft.intune";
        Logger.info(TAG + methodName, "Calling package name : " + packageName);
        return intunePackageName.equalsIgnoreCase(packageName);
    }
}
