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
package com.microsoft.identity.common.java.providers.microsoft.microsoftsts;

import static com.microsoft.identity.common.java.AuthenticationConstants.AAD.APP_PACKAGE_NAME;
import static com.microsoft.identity.common.java.AuthenticationConstants.AAD.APP_VERSION;
import static com.microsoft.identity.common.java.AuthenticationConstants.Broker.CHALLENGE_REQUEST_HEADER;
import static com.microsoft.identity.common.java.AuthenticationConstants.OAuth2Scopes.CLAIMS_UPDATE_RESOURCE;
import static com.microsoft.identity.common.java.AuthenticationConstants.SdkPlatformFields.PRODUCT;
import static com.microsoft.identity.common.java.AuthenticationConstants.SdkPlatformFields.VERSION;
import static com.microsoft.identity.common.java.net.HttpConstants.HeaderField.XMS_CCS_REQUEST_ID;
import static com.microsoft.identity.common.java.net.HttpConstants.HeaderField.XMS_CCS_REQUEST_SEQUENCE;
import static com.microsoft.identity.common.java.net.HttpConstants.HeaderField.X_MS_CLITELEM;
import static com.microsoft.identity.common.java.providers.oauth2.TokenRequest.GrantTypes.CLIENT_CREDENTIALS;

import com.google.gson.JsonParseException;
import com.microsoft.identity.common.java.WarningType;
import com.microsoft.identity.common.java.authscheme.AbstractAuthenticationScheme;
import com.microsoft.identity.common.java.authscheme.AuthenticationSchemeFactory;
import com.microsoft.identity.common.java.authscheme.PopAuthenticationSchemeInternal;
import com.microsoft.identity.common.java.authscheme.PopAuthenticationSchemeWithClientKeyInternal;
import com.microsoft.identity.common.java.cache.ICacheRecord;
import com.microsoft.identity.common.java.challengehandlers.PKeyAuthChallenge;
import com.microsoft.identity.common.java.challengehandlers.PKeyAuthChallengeFactory;
import com.microsoft.identity.common.java.commands.parameters.RopcTokenCommandParameters;
import com.microsoft.identity.common.java.crypto.IDevicePopManager;
import com.microsoft.identity.common.java.dto.IAccountRecord;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.exception.ErrorStrings;
import com.microsoft.identity.common.java.exception.ServiceException;
import com.microsoft.identity.common.java.flighting.CommonFlight;
import com.microsoft.identity.common.java.flighting.CommonFlightsManager;
import com.microsoft.identity.common.java.logging.DiagnosticContext;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.logging.LibraryInfoHelper;
import com.microsoft.identity.common.java.net.HttpClient;
import com.microsoft.identity.common.java.net.HttpConstants;
import com.microsoft.identity.common.java.net.HttpResponse;
import com.microsoft.identity.common.java.net.UrlConnectionHttpClient;
import com.microsoft.identity.common.java.opentelemetry.AttributeName;
import com.microsoft.identity.common.java.opentelemetry.SpanExtension;
import com.microsoft.identity.common.java.platform.Device;
import com.microsoft.identity.common.java.providers.microsoft.MicrosoftAuthorizationResponse;
import com.microsoft.identity.common.java.providers.microsoft.MicrosoftTokenErrorResponse;
import com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.AzureActiveDirectory;
import com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.AzureActiveDirectoryCloud;
import com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.AzureActiveDirectorySlice;
import com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.ClientInfo;
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationResult;
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationResultFactory;
import com.microsoft.identity.common.java.providers.oauth2.IAuthorizationStrategy;
import com.microsoft.identity.common.java.providers.oauth2.IDToken;
import com.microsoft.identity.common.java.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.java.providers.oauth2.OAuth2StrategyParameters;
import com.microsoft.identity.common.java.providers.oauth2.OpenIdProviderConfiguration;
import com.microsoft.identity.common.java.providers.oauth2.OpenIdProviderConfigurationClient;
import com.microsoft.identity.common.java.providers.oauth2.TokenErrorResponse;
import com.microsoft.identity.common.java.providers.oauth2.TokenRequest;
import com.microsoft.identity.common.java.providers.oauth2.TokenResult;
import com.microsoft.identity.common.java.telemetry.CliTelemInfo;
import com.microsoft.identity.common.java.util.CommonURIBuilder;
import com.microsoft.identity.common.java.util.HeaderSerializationUtil;
import com.microsoft.identity.common.java.util.ObjectMapper;
import com.microsoft.identity.common.java.util.ResultUtil;
import com.microsoft.identity.common.java.util.StringUtil;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.NonNull;

// Suppressing rawtype warnings due to the generic type AuthorizationStrategy, AuthorizationResult, AuthorizationResultFactory and MicrosoftAuthorizationRequest
@SuppressWarnings(WarningType.rawtype_warning)
public class MicrosoftStsOAuth2Strategy
        extends OAuth2Strategy
        <MicrosoftStsAccessToken,
                MicrosoftStsAccount,
                MicrosoftStsAuthorizationRequest,
                MicrosoftStsAuthorizationRequest.Builder,
                IAuthorizationStrategy,
                MicrosoftStsOAuth2Configuration,
                OAuth2StrategyParameters,
                MicrosoftStsAuthorizationResponse,
                MicrosoftStsRefreshToken,
                MicrosoftStsTokenRequest,
                MicrosoftStsTokenResponse,
                TokenResult,
                AuthorizationResult> {

    private static final String TAG = MicrosoftStsOAuth2Strategy.class.getSimpleName();
    /**
     * The default scope.  This effects of usint this are captured in documentation here
     * https://docs.microsoft.com/en-us/azure/active-directory/develop/v2-permissions-and-consent#the-default-scope
     * <p>
     * What this does is important, from the documentation it requests permission for every scope
     * that has been selected for the client application in the registration portal.
     */
    private static final String RESOURCE_DEFAULT_SCOPE = "/.default";

    private final HttpClient httpClient = UrlConnectionHttpClient.getDefaultInstance();
    private OpenIdProviderConfiguration mOpenIdProviderConfiguration;

    /**
     * Constructor of MicrosoftStsOAuth2Strategy.
     *
     * @param config     MicrosoftStsOAuth2Configuration
     * @param parameters OAuth2StrategyParameters
     */
    @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE")
    public MicrosoftStsOAuth2Strategy(@NonNull final MicrosoftStsOAuth2Configuration config,
                                      @NonNull final OAuth2StrategyParameters parameters) throws ClientException {
        super(config, parameters);

        if (parameters.isUsingOpenIdConfiguration()) {
            try {
                if (config.getSlice() != null && config.getSlice().getDataCenter() != null) {
                    String extraParams = "?" + AzureActiveDirectorySlice.DC_PARAMETER + "=" + config.getSlice().getDataCenter();
                    loadOpenIdProviderConfiguration(extraParams);
                } else {
                    loadOpenIdProviderConfiguration();
                }
                String openIdConnectTokenEndpoint = mOpenIdProviderConfiguration.getTokenEndpoint();
                if (!StringUtil.isNullOrEmpty(openIdConnectTokenEndpoint)) {
                    setTokenEndpoint(openIdConnectTokenEndpoint);
                } else {
                    setTokenEndpoint(config.getTokenEndpoint().toString());
                }
            } catch (final ServiceException e) {
                Logger.error(
                        TAG,
                        "There was a problem with loading the openIdConfiguration",
                        e
                );
                setTokenEndpoint(config.getTokenEndpoint().toString());
            }
        } else {
            setTokenEndpoint(config.getTokenEndpoint().toString());
        }
    }

    /**
     * Given a v1 resource uri, append '/.default' to convert it to a v2 scope.
     * This is making use of the default scope as documented here:
     * https://docs.microsoft.com/en-us/azure/active-directory/develop/v2-permissions-and-consent#the-default-scope
     *
     * @param resource The v1 resource uri.
     * @return The v1 resource uri as a scope.
     */
    @NonNull
    public static String getScopeFromResource(@NonNull final String resource) {
        return resource + RESOURCE_DEFAULT_SCOPE;
    }

    @Override
    public AuthorizationResultFactory getAuthorizationResultFactory() {
        return new MicrosoftStsAuthorizationResultFactory();
    }

    @Override
    public String getIssuerCacheIdentifier(@NonNull final MicrosoftStsAuthorizationRequest request) {
        final String methodName = ":getIssuerCacheIdentifier";

        final URL authority = request.getAuthority();
        final AzureActiveDirectoryCloud cloudEnv = AzureActiveDirectory.getAzureActiveDirectoryCloud(authority);

        // This map can only be consulted if the authority (cloud really) is known to Microsoft
        // If the host has a hardcoded trust, we can just use the hostname.
        if (null != cloudEnv) {
            final String preferredCacheHostName = cloudEnv.getPreferredCacheHostName();
            Logger.verbose(
                    TAG + methodName,
                    "Using preferred cache host name..."
            );
            Logger.verbose(
                    TAG + methodName,
                    "Preferred cache hostname: [" + preferredCacheHostName + "]"
            );

            return preferredCacheHostName;
        }

        return authority.getHost();
    }

    private String getIssuerCacheIdentifierFromAuthority(final URL authority) {
        final String methodName = ":getIssuerCacheIdentifierFromAuthority";

        final AzureActiveDirectoryCloud cloudEnv = AzureActiveDirectory.getAzureActiveDirectoryCloud(authority);

        // This map can only be consulted if the cloud is known to Microsoft
        // If the host has a hardcoded trust, we can just use the hostname.
        if (null != cloudEnv) {
            final String preferredCacheHostName = cloudEnv.getPreferredCacheHostName();
            Logger.info(
                    TAG + methodName,
                    "Using preferred cache host name..."
            );
            Logger.infoPII(
                    TAG + methodName,
                    "Preferred cache hostname: [" + preferredCacheHostName + "]"
            );

            return preferredCacheHostName;
        }

        return authority.getHost();
    }

    public String getIssuerCacheIdentifierFromTokenEndpoint() {
        final String methodName = ":getIssuerCacheIdentifierFromTokenEndpoint";
        URL authority = null;
        String cacheIdentifier = null;

        try {
            authority = new URL(mTokenEndpoint);
        } catch (MalformedURLException e) {
            Logger.error(
                    TAG + methodName,
                    "Getting issuer cache identifier from token endpoint failed due to malformed URL (mTokenEndpoint)...",
                    e
            );
        }

        if (authority != null) {
            cacheIdentifier = getIssuerCacheIdentifierFromAuthority(authority);
        }

        return cacheIdentifier;
    }

    @Override
    public MicrosoftStsAccessToken getAccessTokenFromResponse(
            @NonNull final MicrosoftStsTokenResponse response) {
        final String methodName = ":getAccessTokenFromResponse";

        Logger.verbose(
                TAG + methodName,
                "Getting AT from TokenResponse..."
        );

        return new MicrosoftStsAccessToken(response);
    }

    @Override
    public MicrosoftStsRefreshToken getRefreshTokenFromResponse(
            @NonNull final MicrosoftStsTokenResponse response) {
        final String methodName = ":getRefreshTokenFromResponse";

        Logger.verbose(
                TAG + methodName,
                "Getting RT from TokenResponse..."
        );

        return new MicrosoftStsRefreshToken(response);
    }

    @Override
    public MicrosoftStsAccount createAccount(
            @NonNull final MicrosoftStsTokenResponse response) {
        final String methodName = ":createAccount";
        Logger.verbose(
                TAG + methodName,
                "Creating account from TokenResponse..."
        );
        IDToken idToken = null;
        ClientInfo clientInfo = null;

        try {
            idToken = new IDToken(response.getIdToken());
            clientInfo = new ClientInfo(response.getClientInfo());
        } catch (ServiceException ccse) {
            Logger.error(
                    TAG + methodName,
                    "Failed to construct IDToken or ClientInfo",
                    null
            );
            Logger.errorPII(
                    TAG + methodName,
                    "Failed with Exception",
                    ccse
            );

            throw new RuntimeException();
        }

        MicrosoftStsAccount account = new MicrosoftStsAccount(idToken, clientInfo);

        account.setEnvironment(getIssuerCacheIdentifierFromTokenEndpoint());

        return account;
    }

    @Override
    public MicrosoftStsAuthorizationRequest.Builder createAuthorizationRequestBuilder() {
        final String methodName = ":createAuthorizationRequestBuilder";

        Logger.info(
                TAG + methodName,
                "Creating AuthorizationRequestBuilder..."
        );

        final MicrosoftStsAuthorizationRequest.Builder builder = new MicrosoftStsAuthorizationRequest.Builder();
        builder.setAuthority(mConfig.getAuthorityUrl());

        if (mConfig.getSlice() != null) {
            Logger.info(
                    TAG + methodName,
                    "Setting slice params..."
            );
            builder.setSlice(mConfig.getSlice());
        }

        builder.setLibraryName(LibraryInfoHelper.getLibraryName());
        builder.setLibraryVersion(LibraryInfoHelper.getLibraryVersion());
        builder.setFlightParameters(mConfig.getFlightParameters());
        builder.setMultipleCloudAware(mConfig.getMultipleCloudsSupported());
        builder.setOpenIdProviderConfiguration(mOpenIdProviderConfiguration);

        return builder;
    }

    @Override
    public MicrosoftStsAuthorizationRequest.Builder createAuthorizationRequestBuilder(
            @Nullable final IAccountRecord account) {
        final String methodName = ":createAuthorizationRequestBuilder";
        Logger.info(
                TAG + methodName,
                "Creating AuthorizationRequestBuilder"
        );
        final MicrosoftStsAuthorizationRequest.Builder builder = createAuthorizationRequestBuilder();

        if (null != account) {
            final String homeAccountId = account.getHomeAccountId();

            final Map.Entry<String, String> uidUtidKeyValuePair = StringUtil.getTenantInfo(homeAccountId);

            if (!StringUtil.isNullOrEmpty(uidUtidKeyValuePair.getKey())
                    && !StringUtil.isNullOrEmpty(uidUtidKeyValuePair.getValue())) {
                builder.setUid(uidUtidKeyValuePair.getKey());
                builder.setUtid(uidUtidKeyValuePair.getValue());

                Logger.infoPII(
                        TAG + methodName,
                        "Builder w/ uid: [" + uidUtidKeyValuePair.getKey() + "]"
                );

                Logger.infoPII(
                        TAG + methodName,
                        "Builder w/ utid: [" + uidUtidKeyValuePair.getValue() + "]"
                );
            }
        }

        return builder;
    }

    @Override
    public MicrosoftStsTokenRequest createTokenRequest(@NonNull final MicrosoftStsAuthorizationRequest request,
                                                       @NonNull final MicrosoftStsAuthorizationResponse response,
                                                       @NonNull final AbstractAuthenticationScheme authScheme)
            throws ClientException {
        final String methodName = ":createTokenRequest";
        Logger.verbose(
                TAG + methodName,
                "Creating TokenRequest..."
        );

        if (mConfig.getMultipleCloudsSupported() || request.getMultipleCloudAware()) {
            Logger.verbose(TAG, "get cloud specific authority based on authorization response.");
            setTokenEndpoint(getCloudSpecificTokenEndpoint(response));
        }

        final MicrosoftStsTokenRequest tokenRequest = new MicrosoftStsTokenRequest();
        tokenRequest.setCodeVerifier(request.getPkceCodeVerifier());
        tokenRequest.setCode(response.getCode());
        tokenRequest.setRedirectUri(request.getRedirectUri());
        tokenRequest.setClientId(request.getClientId());
        tokenRequest.setScope(request.getTokenScope());
        tokenRequest.setClaims(request.getClaims());
        setTokenRequestCorrelationId(tokenRequest);

        // Existence of a device code inside of the response object implies Device Code Flow is being used
        if (response.getDeviceCode() != null) {
            tokenRequest.setGrantType(TokenRequest.GrantTypes.DEVICE_CODE);
            tokenRequest.setDeviceCode(response.getDeviceCode());
        } else { // If device code doesn't exist, continue with auth_code configuration
            tokenRequest.setGrantType(TokenRequest.GrantTypes.AUTHORIZATION_CODE);
        }

        if (authScheme instanceof PopAuthenticationSchemeInternal) {
            // Add a token_type
            tokenRequest.setTokenType(TokenRequest.TokenType.POP);

            final IDevicePopManager devicePopManager =
                    mStrategyParameters.getPlatformComponents().getDefaultDevicePopManager();

            // Generate keys if they don't already exist...
            if (!devicePopManager.asymmetricKeyExists()) {
                final String thumbprint = devicePopManager.generateAsymmetricKey();

                Logger.verbosePII(
                        TAG,
                        "Generated new PoP asymmetric key with thumbprint: "
                                + thumbprint
                );
            }

            final String reqCnf = devicePopManager.getRequestConfirmation();
            // Set the req_cnf
            tokenRequest.setRequestConfirmation(reqCnf);
        } else if (authScheme instanceof PopAuthenticationSchemeWithClientKeyInternal) {
            tokenRequest.setTokenType(TokenRequest.TokenType.POP);
            tokenRequest.setRequestConfirmation(((PopAuthenticationSchemeWithClientKeyInternal) authScheme).getRequestConfirmation());
        }

        return tokenRequest;
    }

    private void setTokenRequestCorrelationId(@NonNull final MicrosoftStsTokenRequest tokenRequest) {
        try {
            tokenRequest.setCorrelationId(
                    UUID.fromString(
                            DiagnosticContext.INSTANCE
                                    .getRequestContext()
                                    .get(DiagnosticContext.CORRELATION_ID)
                    )
            );
        } catch (IllegalArgumentException ex) {
            //We're not setting the correlation id if we can't parse it from the diagnostic context
            Logger.error(
                    "MicrosoftSTSOAuth2Strategy",
                    "Correlation id on diagnostic context is not a UUID.",
                    ex
            );
        }
    }

    @Override
    public MicrosoftStsTokenRequest createRefreshTokenRequest(@NonNull final AbstractAuthenticationScheme authScheme) throws ClientException {
        final String methodName = ":createRefreshTokenRequest";
        Logger.verbose(
                TAG + methodName,
                "Creating refresh token request"
        );

        final MicrosoftStsTokenRequest request = new MicrosoftStsTokenRequest();
        request.setGrantType(TokenRequest.GrantTypes.REFRESH_TOKEN);

        if (authScheme instanceof PopAuthenticationSchemeInternal) {
            request.setTokenType(TokenRequest.TokenType.POP);

            final IDevicePopManager devicePopManager =
                    mStrategyParameters.getPlatformComponents().getDefaultDevicePopManager();

            if (!devicePopManager.asymmetricKeyExists()) {
                devicePopManager.generateAsymmetricKey();
            }

            request.setRequestConfirmation(devicePopManager.getRequestConfirmation());
        } else if (authScheme instanceof PopAuthenticationSchemeWithClientKeyInternal) {
            request.setTokenType(TokenRequest.TokenType.POP);
            request.setRequestConfirmation(((PopAuthenticationSchemeWithClientKeyInternal) authScheme).getRequestConfirmation());
        }

        return request;
    }

    @Override
    public MicrosoftStsTokenRequest createRopcTokenRequest(@NonNull final RopcTokenCommandParameters parameters) throws ClientException {
        final String methodName = ":createPasswordTokenRequest";
        Logger.verbose(
                TAG + methodName,
                "Creating password token request"
        );

        final MicrosoftStsRopcTokenRequest request = new MicrosoftStsRopcTokenRequest();
        request.setGrantType(TokenRequest.GrantTypes.PASSWORD);

        request.setUsername(parameters.getUsername());
        request.setPassword(parameters.getPassword());
        request.setClaims(parameters.getClaimsRequestJson());
        request.setClientId(parameters.getClientId());
        request.setRedirectUri(parameters.getRedirectUri());
        request.setScope(StringUtil.join(" ", parameters.getScopes()));
        setTokenRequestCorrelationId(request);

        if (AuthenticationSchemeFactory.isPopAuthenticationScheme(parameters.getAuthenticationScheme())) {
            throw new UnsupportedOperationException("MSAL Android supports ROPC on Bearer flows only for testing purposes.");
        }

        return request;
    }

    @Override
    protected void validateAuthorizationRequest(final MicrosoftStsAuthorizationRequest request) {
        // TODO implement

    }

    @Override
    protected void validateTokenRequest(MicrosoftStsTokenRequest request) {
        //TODO implement
    }

    @Override
    protected HttpResponse performTokenRequest(final MicrosoftStsTokenRequest request)
            throws IOException, ClientException {
        final String methodName = ":performTokenRequest";
        final HttpResponse response = super.performTokenRequest(request);

        if (response.getStatusCode() == HttpURLConnection.HTTP_UNAUTHORIZED
                && response.getHeaders() != null
                && response.getHeaders().containsKey(CHALLENGE_REQUEST_HEADER)) {
            // Received the device certificate challenge request. It is sent in 401 header.
            // This is triggered whenever we're trying to redeem an AT with an RT without deviceid claim,
            // and the AT requires device auth.
            Logger.info(TAG + methodName, "Receiving device certificate challenge request. ");

            return performPKeyAuthRequest(response, request);
        }

        return response;
    }

    private HttpResponse performPKeyAuthRequest(
            @NonNull final HttpResponse response,
            @NonNull final MicrosoftStsTokenRequest request)
            throws IOException, ClientException {
        final String methodName = "#performPkeyAuthRequest";
        final String requestBody = ObjectMapper.serializeObjectToFormUrlEncoded(request);
        final Map<String, String> headers = new TreeMap<>();
        headers.put("client-request-id", DiagnosticContext.INSTANCE.getRequestContext().get(DiagnosticContext.CORRELATION_ID));
        headers.putAll(Device.getPlatformIdParameters());
        headers.put(PRODUCT, LibraryInfoHelper.getLibraryName());
        headers.put(VERSION, LibraryInfoHelper.getLibraryVersion());

        headers.put(APP_PACKAGE_NAME, request.getClientAppName());
        headers.put(APP_VERSION, request.getClientAppVersion());

        final String challengeHeader = response.getHeaders().get(CHALLENGE_REQUEST_HEADER).get(0);
        Logger.info(TAG + methodName, "Device certificate challenge request. ");
        Logger.infoPII(TAG + methodName, "Challenge header: " + challengeHeader);

        try {
            final PKeyAuthChallengeFactory factory = new PKeyAuthChallengeFactory();
            final URL authority = new URL(mTokenEndpoint);
            final PKeyAuthChallenge pkeyAuthChallenge = factory.getPKeyAuthChallengeFromTokenEndpointResponse(
                    challengeHeader,
                    authority.toString()
            );
            headers.putAll(pkeyAuthChallenge.getChallengeHeader());
            headers.put(HttpConstants.HeaderField.CONTENT_TYPE, TOKEN_REQUEST_CONTENT_TYPE);

            return httpClient.post(
                    authority,
                    headers,
                    requestBody.getBytes(ObjectMapper.ENCODING_SCHEME)
            );
        } catch (final UnsupportedEncodingException exception) {
            throw new ClientException(ErrorStrings.UNSUPPORTED_ENCODING,
                    "Unsupported encoding", exception);
        }
    }

    @Override
    @NonNull
    protected TokenResult getTokenResultFromHttpResponse(@NonNull final HttpResponse response)
            throws ClientException {
        final String methodName = ":getTokenResultFromHttpResponse";

        Logger.verbose(
                TAG + methodName,
                "Getting TokenResult from HttpResponse..."
        );

        MicrosoftStsTokenResponse tokenResponse = null;
        TokenErrorResponse tokenErrorResponse = null;

        if (response.getStatusCode() >= HttpURLConnection.HTTP_BAD_REQUEST) {
            //An error occurred
            try {
                tokenErrorResponse = ObjectMapper.deserializeJsonStringToObject(
                        getBodyFromUnsuccessfulResponse(response.getBody()),
                        MicrosoftTokenErrorResponse.class
                );
            } catch (final JsonParseException ex) {
                tokenErrorResponse = new MicrosoftTokenErrorResponse();
                final String statusCode = String.valueOf(response.getStatusCode());
                tokenErrorResponse.setError(statusCode);
                tokenErrorResponse.setErrorDescription("Received " + statusCode + " status code from Server ");
            }
            tokenErrorResponse.setStatusCode(response.getStatusCode());

            if (null != response.getHeaders()) {
                tokenErrorResponse.setResponseHeadersJson(
                        HeaderSerializationUtil.toJson(response.getHeaders())
                );
            }
            tokenErrorResponse.setResponseBody(response.getBody());
        } else {
            tokenResponse = ObjectMapper.deserializeJsonStringToObject(
                    getBodyFromSuccessfulResponse(response.getBody()),
                    MicrosoftStsTokenResponse.class
            );
        }

        final TokenResult result = new TokenResult(tokenResponse, tokenErrorResponse);

        ResultUtil.logResult(TAG, result);

        if (null != response.getHeaders()) {
            final Map<String, List<String>> responseHeaders = response.getHeaders();

            final List<String> cliTelemValues;
            if (null != (cliTelemValues = responseHeaders.get(X_MS_CLITELEM))
                    && !cliTelemValues.isEmpty()) {
                // Element should only contain 1 value...
                final String cliTelemHeader = cliTelemValues.get(0);
                final CliTelemInfo cliTelemInfo = CliTelemInfo.fromXMsCliTelemHeader(
                        cliTelemHeader
                );
                // Parse and set the result...
                result.setCliTelemInfo(cliTelemInfo);

                if (null != tokenResponse && null != cliTelemInfo) {
                    tokenResponse.setSpeRing(cliTelemInfo.getSpeRing());
                    tokenResponse.setRefreshTokenAge(cliTelemInfo.getRefreshTokenAge());
                    tokenResponse.setCliTelemErrorCode(cliTelemInfo.getServerErrorCode());
                    tokenResponse.setCliTelemSubErrorCode(cliTelemInfo.getServerSubErrorCode());
                }
            }

            final Map<String, String> mapWithAdditionalEntry = new HashMap<String, String>();

            final String ccsRequestId = response.getHeaderValue(XMS_CCS_REQUEST_ID, 0);
            if (null != ccsRequestId) {
                SpanExtension.current().setAttribute(AttributeName.ccs_request_id.name(), ccsRequestId);
                if (CommonFlightsManager.INSTANCE.getFlightsProvider().isFlightEnabled(CommonFlight.EXPOSE_CCS_REQUEST_ID_IN_TOKENRESPONSE)){
                    mapWithAdditionalEntry.put(XMS_CCS_REQUEST_ID, ccsRequestId);
                }
            }

            final String ccsRequestSequence = response.getHeaderValue(XMS_CCS_REQUEST_SEQUENCE, 0);
            if (null != ccsRequestSequence) {
                SpanExtension.current().setAttribute(AttributeName.ccs_request_sequence.name(), ccsRequestSequence);
                if (CommonFlightsManager.INSTANCE.getFlightsProvider().isFlightEnabled(CommonFlight.EXPOSE_CCS_REQUEST_SEQUENCE_IN_TOKENRESPONSE)){
                    mapWithAdditionalEntry.put(XMS_CCS_REQUEST_SEQUENCE, ccsRequestSequence);
                }
            }

            if (null != tokenResponse) {
                if (null != tokenResponse.getExtraParameters()) {
                    for (final Map.Entry<String, String> entry : tokenResponse.getExtraParameters()) {
                        mapWithAdditionalEntry.put(entry.getKey(), entry.getValue());
                    }
                }

                tokenResponse.setExtraParameters(mapWithAdditionalEntry.entrySet());
            }
        }

        return result;
    }

    protected String getBodyFromSuccessfulResponse(@NonNull final String responseBody) throws ClientException {
        return responseBody;
    }

    protected String getBodyFromUnsuccessfulResponse(@NonNull final String responseBody) throws ClientException {
        final String EMPTY_JSON_OBJECT = "{}";
        return responseBody.isEmpty() ? EMPTY_JSON_OBJECT : responseBody;
    }

    @Override
    protected void validateTokenResponse(@NonNull final MicrosoftStsTokenRequest request,
                                         @NonNull final MicrosoftStsTokenResponse response)
            throws ClientException {
        validateAuthScheme(request, response);
        validateTokensAreInResponse(request, response);
    }

    /**
     * Validates that the auth scheme in the TokenRequest matches the auth scheme in the TokenResponse.
     *
     * @param request  The TokenRequest with the IdP's response.
     * @param response The idp response.
     * @throws ClientException
     */
    private void validateAuthScheme(@NonNull final MicrosoftStsTokenRequest request,
                                    @NonNull final MicrosoftStsTokenResponse response)
            throws ClientException {
        final String requestTokenType = request.getTokenType();
        final String responseAuthScheme = response.getTokenType();

        // if the request token type is null, the response value is assumed Bearer
        if (requestTokenType != null && !requestTokenType.equalsIgnoreCase(responseAuthScheme)) {
            throw new ClientException(
                    ClientException.AUTH_SCHEME_MISMATCH,
                    "Expected: [" + requestTokenType + "]"
                            + "\n"
                            + "Actual: [" + responseAuthScheme + "]"
            );
        }
    }

    /**
     * Validates that the token response contains an access token, id_token and refresh token
     *
     * @param response The idp response.
     * @throws ClientException
     */
    private void validateTokensAreInResponse(@NonNull final MicrosoftStsTokenRequest request,
                                             @NonNull final MicrosoftStsTokenResponse response)
            throws ClientException {

        String clientException = null;
        String tokens = "";
        final String tokensMissingMessage = "Missing required tokens of type: %s";

        // PRT interrupt flow do not return AT.
        if (!StringUtil.containsSubString(request.getScope(), CLAIMS_UPDATE_RESOURCE) &&
                StringUtil.isNullOrEmpty(response.getAccessToken())) {
            clientException = ClientException.TOKENS_MISSING;
            tokens = tokens.concat("access_token");
        }

        if (!CLIENT_CREDENTIALS.equalsIgnoreCase(request.getGrantType()) &&
                StringUtil.isNullOrEmpty(response.getIdToken())) {
            clientException = ClientException.TOKENS_MISSING;
            tokens = tokens.concat(" id_token");
        }

        if (!CLIENT_CREDENTIALS.equalsIgnoreCase(request.getGrantType()) &&
                StringUtil.isNullOrEmpty(response.getRefreshToken())) {
            clientException = ClientException.TOKENS_MISSING;
            tokens = tokens.concat(" refresh_token");
        }

        if (clientException != null) {
            throw new ClientException(clientException, String.format(tokensMissingMessage, tokens));
        }
    }

    private String buildCloudSpecificTokenEndpoint(
            @NonNull final MicrosoftStsAuthorizationResponse response) throws ClientException {
        if (!StringUtil.isNullOrEmpty(response.getCloudInstanceHostName())) {
            try {
                return new CommonURIBuilder(mTokenEndpoint)
                        .setHost(response.getCloudInstanceHostName())
                        .build()
                        .toString();
            } catch (final URISyntaxException e) {
                throw new ClientException(ClientException.MALFORMED_URL,
                        "Failed to construct token endpoint from getCloudInstanceHostName()", e);
            }
        }

        return mTokenEndpoint;
    }

    private String getCloudSpecificTokenEndpoint(final MicrosoftAuthorizationResponse response) throws ClientException {
        if (StringUtil.isNullOrEmpty(response.getCloudInstanceHostName())) {
            return mTokenEndpoint;
        }
        return buildCloudSpecificTokenEndpoint((MicrosoftStsAuthorizationResponse) response);
    }

    /**
     * Gets the at/pop device credential's thumbprint.
     *
     * @return The at/pop device credential thumbprint.
     */
    @Nullable
    public String getDeviceAtPopThumbprint() {
        if (mStrategyParameters.getAuthenticationScheme() instanceof PopAuthenticationSchemeWithClientKeyInternal) {
            return ((PopAuthenticationSchemeWithClientKeyInternal) mStrategyParameters.getAuthenticationScheme()).getKid();
        }

        String atPoPKid = null;

        IDevicePopManager devicePopManager = null;
        try {
            devicePopManager = mStrategyParameters.getPlatformComponents().getDefaultDevicePopManager();
        } catch (final ClientException e) {
            Logger.error(
                    TAG,
                    e.getMessage(),
                    e
            );
        }

        if (null != devicePopManager) {
            if (devicePopManager.asymmetricKeyExists()) {
                try {
                    atPoPKid = devicePopManager.getAsymmetricKeyThumbprint();
                } catch (final ClientException e) {
                    Logger.error(
                            TAG,
                            "Key exists. But failed to load thumbprint.",
                            e
                    );

                    throw new RuntimeException(e);
                }
            } else {
                // something has gone seriously wrong.
                throw new RuntimeException("Symmetric keys do not exist.");
            }
        } else {
            Logger.warn(
                    TAG,
                    "DevicePopManager does not exist."
            );
        }

        return atPoPKid;
    }

    @Override
    public boolean validateCachedResult(@NonNull final AbstractAuthenticationScheme authScheme,
                                        @NonNull final ICacheRecord cacheRecord) {
        super.validateCachedResult(authScheme, cacheRecord);

        if (authScheme instanceof PopAuthenticationSchemeInternal) {
            return cachedAccessTokenKidMatchesKeystoreKid(cacheRecord.getAccessToken().getKid());
        } else if (authScheme instanceof PopAuthenticationSchemeWithClientKeyInternal) {
            return ((PopAuthenticationSchemeWithClientKeyInternal) authScheme).getKid()
                    .equalsIgnoreCase(cacheRecord.getAccessToken().getKid());
        }

        return true;
    }

    private boolean cachedAccessTokenKidMatchesKeystoreKid(@Nullable final String atKid) {
        final String deviceKid = getDeviceAtPopThumbprint();

        // If the value known to the strategy is null, something's wrong. Discard the current token
        // and generate keys anew. Once those keys are generated we will use the cache RT to acquire
        // a new PoP/AT.
        if (StringUtil.isNullOrEmpty(deviceKid)) {
            return false;
        }

        return deviceKid.equals(atKid);
    }

    /**
     * Using this method to load the {@link OpenIdProviderConfiguration}
     * This will cause the strategy to fetch the authorization endpoint from OpenId Configuration rather
     * than generating one with the default authorization endpoint
     */
    private void loadOpenIdProviderConfiguration() throws ServiceException {
        final OpenIdProviderConfigurationClient client =
                new OpenIdProviderConfigurationClient();
        mOpenIdProviderConfiguration = client.loadOpenIdProviderConfigurationFromAuthority(mConfig.getAuthorityUrl().toString());

    }

    /**
     * Using this method to load the {@link OpenIdProviderConfiguration} with extra parameters
     * This will cause the strategy to fetch the authorization endpoint from OpenId Configuration rather
     * than generating one with the default authorization endpoint
     */
    @SuppressFBWarnings
    private void loadOpenIdProviderConfiguration(@NonNull final String extraParams) throws ServiceException {
        final OpenIdProviderConfigurationClient client =
                new OpenIdProviderConfigurationClient();
        mOpenIdProviderConfiguration = client.loadOpenIdProviderConfigurationFromAuthorityWithExtraParams(mConfig.getAuthorityUrl().toString(), extraParams);
    }
}
