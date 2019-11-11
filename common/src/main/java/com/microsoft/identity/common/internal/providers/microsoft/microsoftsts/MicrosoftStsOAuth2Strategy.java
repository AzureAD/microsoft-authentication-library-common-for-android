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
package com.microsoft.identity.common.internal.providers.microsoft.microsoftsts;

import android.net.Uri;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.exception.ErrorStrings;
import com.microsoft.identity.common.exception.ServiceException;
import com.microsoft.identity.common.internal.dto.IAccountRecord;
import com.microsoft.identity.common.internal.eststelemetry.EstsTelemetry;
import com.microsoft.identity.common.internal.logging.DiagnosticContext;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.net.HttpRequest;
import com.microsoft.identity.common.internal.net.HttpResponse;
import com.microsoft.identity.common.internal.net.ObjectMapper;
import com.microsoft.identity.common.internal.platform.Device;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftAuthorizationRequest;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftAuthorizationResponse;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftTokenErrorResponse;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectory;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectoryCloud;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.ClientInfo;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResult;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResultFactory;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationStrategy;
import com.microsoft.identity.common.internal.providers.oauth2.IDToken;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.OpenIdProviderConfiguration;
import com.microsoft.identity.common.internal.providers.oauth2.TokenErrorResponse;
import com.microsoft.identity.common.internal.providers.oauth2.TokenRequest;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResult;
import com.microsoft.identity.common.internal.telemetry.CliTelemInfo;
import com.microsoft.identity.common.internal.ui.webview.challengehandlers.PKeyAuthChallenge;
import com.microsoft.identity.common.internal.ui.webview.challengehandlers.PKeyAuthChallengeFactory;
import com.microsoft.identity.common.internal.ui.webview.challengehandlers.PKeyAuthChallengeHandler;
import com.microsoft.identity.common.internal.util.HeaderSerializationUtil;
import com.microsoft.identity.common.internal.util.StringUtil;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.CHALLENGE_REQUEST_HEADER;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.HeaderField.X_MS_CLITELEM;
import static com.microsoft.identity.common.internal.controllers.BaseController.logResult;

public class MicrosoftStsOAuth2Strategy
        extends OAuth2Strategy
        <MicrosoftStsAccessToken,
                MicrosoftStsAccount,
                MicrosoftStsAuthorizationRequest,
                MicrosoftStsAuthorizationRequest.Builder,
                AuthorizationStrategy,
                MicrosoftStsOAuth2Configuration,
                MicrosoftStsAuthorizationResponse,
                MicrosoftStsRefreshToken,
                MicrosoftStsTokenRequest,
                MicrosoftStsTokenResponse,
                TokenResult,
                AuthorizationResult> {

    private static final String TAG = MicrosoftStsOAuth2Strategy.class.getSimpleName();

    /**
     * Constructor of MicrosoftStsOAuth2Strategy.
     *
     * @param config MicrosoftStsOAuth2Configuration
     */
    public MicrosoftStsOAuth2Strategy(@NonNull final MicrosoftStsOAuth2Configuration config) {
        super(config);
        setTokenEndpoint(config.getTokenEndpoint().toString());
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

        final Map<String, String> platformParameters = Device.getPlatformIdParameters();
        builder.setLibraryName(platformParameters.get(
                Device.PlatformIdParameters.PRODUCT)
        );
        builder.setLibraryVersion(platformParameters.get(
                Device.PlatformIdParameters.VERSION)
        );

        builder.setFlightParameters(mConfig.getFlightParameters());
        builder.setMultipleCloudAware(mConfig.getMultipleCloudsSupported());

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

            final Pair<String, String> uidUtidPair = StringUtil.getTenantInfo(homeAccountId);

            if (!StringExtensions.isNullOrBlank(uidUtidPair.first)
                    && !StringExtensions.isNullOrBlank(uidUtidPair.second)) {
                builder.setUid(uidUtidPair.first);
                builder.setUtid(uidUtidPair.second);

                Logger.infoPII(
                        TAG + methodName,
                        "Builder w/ uid: [" + uidUtidPair.first + "]"
                );

                Logger.infoPII(
                        TAG + methodName,
                        "Builder w/ utid: [" + uidUtidPair.second + "]"
                );
            }
        }

        return builder;
    }

    @Override
    public MicrosoftStsTokenRequest createTokenRequest(@NonNull final MicrosoftStsAuthorizationRequest request,
                                                       @NonNull final MicrosoftStsAuthorizationResponse response) {
        final String methodName = ":createTokenRequest";
        Logger.verbose(
                TAG + methodName,
                "Creating TokenRequest..."
        );

        if (mConfig.getMultipleCloudsSupported() || request.getMultipleCloudAware()) {
            Logger.verbose(TAG, "get cloud specific authority based on authorization response.");
            setTokenEndpoint(getCloudSpecificTokenEndpoint(request, response));
        }

        final MicrosoftStsTokenRequest tokenRequest = new MicrosoftStsTokenRequest();
        tokenRequest.setCodeVerifier(request.getPkceChallenge().getCodeVerifier());
        tokenRequest.setCode(response.getCode());
        tokenRequest.setRedirectUri(request.getRedirectUri());
        tokenRequest.setClientId(request.getClientId());
        tokenRequest.setScope(request.getTokenScope());

        tokenRequest.setGrantType(TokenRequest.GrantTypes.AUTHORIZATION_CODE);

        try {
            tokenRequest.setCorrelationId(
                    UUID.fromString(
                            DiagnosticContext
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

        return tokenRequest;
    }

    @Override
    public MicrosoftStsTokenRequest createRefreshTokenRequest() {
        final String methodName = ":createRefreshTokenRequest";
        Logger.verbose(
                TAG + methodName,
                "Creating refresh token request"
        );

        final MicrosoftStsTokenRequest request = new MicrosoftStsTokenRequest();
        request.setGrantType(TokenRequest.GrantTypes.REFRESH_TOKEN);
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
        headers.put("client-request-id", DiagnosticContext.getRequestContext().get(DiagnosticContext.CORRELATION_ID));
        headers.putAll(Device.getPlatformIdParameters());

        final String challengeHeader = response.getHeaders().get(CHALLENGE_REQUEST_HEADER).get(0);
        Logger.info(TAG + methodName, "Device certificate challenge request. ");
        Logger.infoPII(TAG + methodName, "Challenge header: " + challengeHeader);

        try {
            final PKeyAuthChallengeFactory factory = new PKeyAuthChallengeFactory();
            final URL authority = StringExtensions.getUrl(mTokenEndpoint);
            final PKeyAuthChallenge pkeyAuthChallenge = factory.getPKeyAuthChallenge(
                    challengeHeader,
                    authority.toString()
            );
            headers.putAll(PKeyAuthChallengeHandler.getChallengeHeader(pkeyAuthChallenge));
            headers.putAll(EstsTelemetry.getInstance().getTelemetryHeaders());

            final HttpResponse pkeyAuthResponse = HttpRequest.sendPost(
                    authority,
                    headers,
                    requestBody.getBytes(ObjectMapper.ENCODING_SCHEME),
                    TOKEN_REQUEST_CONTENT_TYPE
            );

            return pkeyAuthResponse;
        } catch (final UnsupportedEncodingException exception) {
            throw new ClientException(ErrorStrings.UNSUPPORTED_ENCODING,
                    "Unsupported encoding", exception);
        }
    }

    @Override
    @NonNull
    protected TokenResult getTokenResultFromHttpResponse(@NonNull final HttpResponse response) {
        final String methodName = ":getTokenResultFromHttpResponse";

        Logger.verbose(
                TAG + methodName,
                "Getting TokenResult from HttpResponse..."
        );

        MicrosoftStsTokenResponse tokenResponse = null;
        TokenErrorResponse tokenErrorResponse = null;

        if (response.getStatusCode() >= HttpURLConnection.HTTP_BAD_REQUEST) {
            //An error occurred
            tokenErrorResponse = ObjectMapper.deserializeJsonStringToObject(
                    response.getBody(),
                    MicrosoftTokenErrorResponse.class
            );
            tokenErrorResponse.setStatusCode(response.getStatusCode());

            if (null != response.getHeaders()) {
                tokenErrorResponse.setResponseHeadersJson(
                        HeaderSerializationUtil.toJson(response.getHeaders())
                );
            }

            tokenErrorResponse.setResponseBody(response.getBody());
        } else {
            tokenResponse = ObjectMapper.deserializeJsonStringToObject(
                    response.getBody(),
                    MicrosoftStsTokenResponse.class
            );
        }

        final TokenResult result = new TokenResult(tokenResponse, tokenErrorResponse);

        logResult(TAG, result);

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
        }

        return result;
    }

    private String buildCloudSpecificTokenEndpoint(
            @NonNull final MicrosoftStsAuthorizationResponse response) {
        if (!StringUtil.isEmpty(response.getCloudInstanceHostName())) {
            final String updatedTokenEndpoint =
                    Uri.parse(mTokenEndpoint)
                            .buildUpon()
                            .authority(response.getCloudInstanceHostName())
                            .build()
                            .toString();

            return updatedTokenEndpoint;
        }

        return mTokenEndpoint;
    }

    private String getCloudSpecificTokenEndpoint(MicrosoftAuthorizationRequest request,
                                                 MicrosoftAuthorizationResponse response) {
        final String methodName = ":getCloudSpecificTokenEndpoint";
        String tokenEndpoint;

        if (StringUtil.isEmpty(response.getCloudInstanceHostName())) {
            return mTokenEndpoint;
        }

        final OpenIdProviderConfiguration openIdConfig = mConfig.getOpenIdWellKnownConfig(
                response.getCloudInstanceHostName(),
                request.getAuthority().getPath()
        );

        if (openIdConfig != null && openIdConfig.getTokenEndpoint() != null) {
            tokenEndpoint = openIdConfig.getTokenEndpoint();
        } else {
            Logger.verbose(TAG + methodName,
                    "Token Endpoint not obtained from well known config. Building token endpoint manually.");
            // otherwise build it manually
            tokenEndpoint = buildCloudSpecificTokenEndpoint((MicrosoftStsAuthorizationResponse) response);
        }

        return tokenEndpoint;
    }

}
