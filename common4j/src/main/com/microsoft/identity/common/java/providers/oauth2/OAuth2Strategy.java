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
package com.microsoft.identity.common.java.providers.oauth2;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.microsoft.identity.common.java.AuthenticationConstants;
import com.microsoft.identity.common.java.BaseAccount;
import com.microsoft.identity.common.java.WarningType;
import com.microsoft.identity.common.java.authscheme.AbstractAuthenticationScheme;
import com.microsoft.identity.common.java.cache.ICacheRecord;
import com.microsoft.identity.common.java.commands.parameters.RopcTokenCommandParameters;
import com.microsoft.identity.common.java.dto.IAccountRecord;
import com.microsoft.identity.common.java.eststelemetry.EstsTelemetry;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.logging.DiagnosticContext;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.logging.ProductHelper;
import com.microsoft.identity.common.java.net.HttpClient;
import com.microsoft.identity.common.java.net.HttpConstants;
import com.microsoft.identity.common.java.net.HttpResponse;
import com.microsoft.identity.common.java.net.UrlConnectionHttpClient;
import com.microsoft.identity.common.java.opentelemetry.AttributeName;
import com.microsoft.identity.common.java.opentelemetry.SpanExtension;
import com.microsoft.identity.common.java.platform.Device;
import com.microsoft.identity.common.java.providers.microsoft.MicrosoftTokenRequest;
import com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.AzureActiveDirectorySlice;
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationErrorResponse;
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationRequest;
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationResponse;
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationResult;
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Configuration;
import com.microsoft.identity.common.java.telemetry.Telemetry;
import com.microsoft.identity.common.java.telemetry.TelemetryEventStrings;
import com.microsoft.identity.common.java.telemetry.events.UiShownEvent;
import com.microsoft.identity.common.java.util.ClientExtraSku;
import com.microsoft.identity.common.java.util.CommonURIBuilder;
import com.microsoft.identity.common.java.util.IClockSkewManager;
import com.microsoft.identity.common.java.util.ObjectMapper;
import com.microsoft.identity.common.java.util.StringUtil;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Future;

import javax.net.ssl.HttpsURLConnection;

import lombok.NonNull;

import static com.microsoft.identity.common.java.AuthenticationConstants.AAD.CLIENT_REQUEST_ID;
import static com.microsoft.identity.common.java.AuthenticationConstants.Broker.PKEYAUTH_HEADER;
import static com.microsoft.identity.common.java.AuthenticationConstants.Broker.PKEYAUTH_VERSION;
import static com.microsoft.identity.common.java.AuthenticationConstants.OAuth2.ERROR;
import static com.microsoft.identity.common.java.AuthenticationConstants.OAuth2.ERROR_DESCRIPTION;

/**
 * Serves as the abstract base class for an oAuth2 client implementation; The base class should be extended
 * by Identity Provider specific implementations; For example: Azure Active Directory, ADFS, Microsoft STS, Etc...
 */
// Suppressing rawtype warnings due to generic types AuthorizationRequest, AuthorizationRequest.Builder, AuthorizationStrategy, AuthorizationResult and AuthorizationResultFactory
@SuppressWarnings(WarningType.rawtype_warning)
public abstract class OAuth2Strategy
        <GenericAccessToken extends AccessToken,
                GenericAccount extends BaseAccount,
                GenericAuthorizationRequest extends AuthorizationRequest,
                GenericAuthorizationRequestBuilder extends AuthorizationRequest.Builder,
                GenericAuthorizationStrategy extends IAuthorizationStrategy,
                GenericOAuth2Configuration extends OAuth2Configuration,
                GenericOAuth2StrategyParameters extends OAuth2StrategyParameters,
                GenericAuthorizationResponse extends AuthorizationResponse,
                GenericRefreshToken extends RefreshToken,
                GenericTokenRequest extends TokenRequest,
                GenericTokenResponse extends TokenResponse,
                GenericTokenResult extends TokenResult,
                GenericAuthorizationResult extends AuthorizationResult> {

    private static final String TAG = OAuth2Strategy.class.getSimpleName();

    protected static final String TOKEN_REQUEST_CONTENT_TYPE = "application/x-www-form-urlencoded";
    protected static final String DEVICE_CODE_CONTENT_TYPE = TOKEN_REQUEST_CONTENT_TYPE;

    protected final HttpClient httpClient = UrlConnectionHttpClient.getDefaultInstance();

    protected final GenericOAuth2Configuration mConfig;
    protected final GenericOAuth2StrategyParameters mStrategyParameters;
    protected final IClockSkewManager mClockSkewManager;
    protected String mTokenEndpoint;
    protected String mAuthorizationEndpoint;
    private URI mIssuer;

    /**
     * Constructor of OAuth2Strategy.
     *
     * @param config generic OAuth2 configuration
     */
    public OAuth2Strategy(GenericOAuth2Configuration config,
                          GenericOAuth2StrategyParameters strategyParameters) {
        mConfig = config;
        mStrategyParameters = strategyParameters;

        if (null != mStrategyParameters.getPlatformComponents()) {
            mClockSkewManager = mStrategyParameters.getPlatformComponents().getClockSkewManager();
        } else {
            Logger.info(TAG, "No valid platform component to initialize ClockSkewManager with!");
            mClockSkewManager = null;
        }
    }

    /**
     * Template method for executing an OAuth2 authorization request.
     *
     * @param request               generic authorization request.
     * @param authorizationStrategy generic authorization strategy.
     * @return GenericAuthorizationResponse
     */
    public Future<AuthorizationResult> requestAuthorization(
            final GenericAuthorizationRequest request,
            final GenericAuthorizationStrategy authorizationStrategy)
            throws ClientException {
        validateAuthorizationRequest(request);

        // Suppressing unchecked warnings due to casting an object in reference of current class to the child class GenericOAuth2Strategy while calling method requestAuthorization()
        @SuppressWarnings(WarningType.unchecked_warning) final Future<AuthorizationResult> authorizationFuture =
                authorizationStrategy.requestAuthorization(request, this);
        Telemetry.emit(new UiShownEvent().putVisible(TelemetryEventStrings.Value.TRUE));

        return authorizationFuture;
    }

    public abstract AuthorizationResultFactory getAuthorizationResultFactory();

    /**
     * @param request generic token request.
     * @return GenericTokenResult
     * @throws IOException thrown when failed or interrupted I/O operations occur.
     */
    public GenericTokenResult requestToken(final GenericTokenRequest request) throws IOException, ClientException {
        final String methodName = ":requestToken";

        Logger.verbose(
                TAG + methodName,
                "Requesting token..."
        );

        validateTokenRequest(request);

        final HttpResponse response = performTokenRequest(request);
        final GenericTokenResult result = getTokenResultFromHttpResponse(response);
        if (result.getTokenResponse() != null) {
            result.getTokenResponse().setAuthority(mTokenEndpoint);
        }
        if (result.getSuccess()) {
            validateTokenResponse(request, result);
        }
        return result;
    }

    // Suppressing unchecked warnings due to casting from TokenResponse to GenericTokenResponse in arguments in the call to method validateTokenResponse()
    @SuppressWarnings(WarningType.unchecked_warning)
    private void validateTokenResponse(GenericTokenRequest request, GenericTokenResult result) throws ClientException {
        validateTokenResponse(
                request,
                (GenericTokenResponse) result.getSuccessResponse()
        );
    }

    protected HttpResponse performTokenRequest(final GenericTokenRequest request) throws IOException, ClientException {
        final String methodName = ":performTokenRequest";

        Logger.verbose(
                TAG + methodName,
                "Performing token request..."
        );

        final String requestBody = getRequestBody(request);
        final Map<String, String> headers = new TreeMap<>();
        headers.put(CLIENT_REQUEST_ID, DiagnosticContext.INSTANCE.getRequestContext().get(DiagnosticContext.CORRELATION_ID));

        final String product = ProductHelper.getProduct();
        final String productVersion = ProductHelper.getProductVersion();

        if (request instanceof MicrosoftTokenRequest &&
                !StringUtil.isNullOrEmpty(((MicrosoftTokenRequest) request).getBrokerVersion())) {
            headers.put(
                    Device.PlatformIdParameters.BROKER_VERSION,
                    ((MicrosoftTokenRequest) request).getBrokerVersion()
            );

            // Attach client extras header for ESTS telemetry. Only done for broker requests
            final ClientExtraSku clientExtraSku = ClientExtraSku.builder()
                    .srcSku(product)
                    .srcSkuVer(productVersion)
                    .build();
            headers.put(AuthenticationConstants.SdkPlatformFields.CLIENT_EXTRA_SKU, clientExtraSku.toString());
        }
        headers.putAll(Device.getPlatformIdParameters());
        headers.put(AuthenticationConstants.SdkPlatformFields.PRODUCT, product);
        headers.put(AuthenticationConstants.SdkPlatformFields.VERSION, productVersion);
        headers.putAll(EstsTelemetry.getInstance().getTelemetryHeaders());
        headers.put(HttpConstants.HeaderField.CONTENT_TYPE, TOKEN_REQUEST_CONTENT_TYPE);

        if (request instanceof MicrosoftTokenRequest) {
            final MicrosoftTokenRequest microsoftTokenRequest = (MicrosoftTokenRequest) request;
            headers.put(
                    AuthenticationConstants.AAD.APP_PACKAGE_NAME,
                    microsoftTokenRequest.getClientAppName()
            );
            headers.put(
                    AuthenticationConstants.AAD.APP_VERSION,
                    microsoftTokenRequest.getClientAppVersion()
            );
            if (microsoftTokenRequest.isPKeyAuthHeaderAllowed()) {
                headers.put(PKEYAUTH_HEADER, PKEYAUTH_VERSION);
            }
        }

        final URL requestUrl = new URL(getTokenEndpoint());
        final long networkStartTime = System.currentTimeMillis();
        final HttpResponse response = httpClient.post(
                requestUrl,
                headers,
                requestBody.getBytes(ObjectMapper.ENCODING_SCHEME)
        );
        final long networkEndTime = System.currentTimeMillis();
        final long networkTime = networkEndTime - networkStartTime;
        SpanExtension.current().setAttribute(AttributeName.elapsed_time_network_acquire_at.name(), networkTime);


        // Record the clock skew between *this device* and EVO...
        if (null != response.getDate()) {
            recordClockSkew(response.getDate().getTime());
        }
        return response;
    }

    protected String getTokenEndpoint() {
        return mTokenEndpoint;
    }

    protected String getRequestBody(final GenericTokenRequest request) throws UnsupportedEncodingException, ClientException {
        return ObjectMapper.serializeObjectToFormUrlEncoded(request);
    }

    private void recordClockSkew(final long referenceTimeMillis) {
        if (null != mClockSkewManager) {
            mClockSkewManager.onTimestampReceived(referenceTimeMillis);
        }
    }

    protected final void setTokenEndpoint(final String tokenEndpoint) throws ClientException {
        mTokenEndpoint = tokenEndpoint;

        if (mConfig != null && mConfig instanceof MicrosoftStsOAuth2Configuration) {

            final MicrosoftStsOAuth2Configuration oauth2Config =
                    (MicrosoftStsOAuth2Configuration) mConfig;

            final AzureActiveDirectorySlice slice = oauth2Config.getSlice();

            if (slice != null) {
                try {
                    final CommonURIBuilder commonUriBuilder = new CommonURIBuilder(mTokenEndpoint);
                    if (!StringUtil.isNullOrEmpty(slice.getSlice())) {
                        commonUriBuilder.setParameter(AzureActiveDirectorySlice.SLICE_PARAMETER, slice.getSlice());
                    }
                    if (!StringUtil.isNullOrEmpty(slice.getDataCenter())) {
                        commonUriBuilder.setParameter(AzureActiveDirectorySlice.DC_PARAMETER, slice.getDataCenter());
                    }

                    mTokenEndpoint = commonUriBuilder.build().toString();
                } catch (final URISyntaxException e) {
                    throw new ClientException(ClientException.MALFORMED_URL, e.getMessage(), e);
                }
            }
        }
    }

    public String getAuthorityFromTokenEndpoint() {
        return mTokenEndpoint.toLowerCase(Locale.ROOT).replace("oauth2/v2.0/token", "");
    }

    protected final void setAuthorizationEndpoint(final String authorizationEndpoint) {
        mAuthorizationEndpoint = authorizationEndpoint;
    }

    public AuthorizationResult getDeviceCode(@NonNull final MicrosoftStsAuthorizationRequest authorizationRequest) throws IOException {
        final String methodName = ":getDeviceCode";

        // Set up headers and request body
        final String requestBody = ObjectMapper.serializeObjectToFormUrlEncoded(authorizationRequest);
        final Map<String, String> headers = new TreeMap<>();
        headers.put(CLIENT_REQUEST_ID, DiagnosticContext.INSTANCE.getRequestContext().get(DiagnosticContext.CORRELATION_ID));
        headers.putAll(EstsTelemetry.getInstance().getTelemetryHeaders());
        headers.put(HttpConstants.HeaderField.CONTENT_TYPE, DEVICE_CODE_CONTENT_TYPE);

        final HttpResponse response = httpClient.post(
                ((MicrosoftStsOAuth2Configuration) mConfig).getDeviceAuthorizationEndpoint(),
                headers,
                requestBody.getBytes(ObjectMapper.ENCODING_SCHEME)
        );

        // Create the authorization result

        // Check if the request was successful
        // Any code below 300 (HTTP_MULT_CHOICE) is considered a success
        if (response.getStatusCode() < HttpsURLConnection.HTTP_MULT_CHOICE) {
            // Get and parse response body
            final HashMap<String, String> parsedResponseBody = new Gson().fromJson(
                    response.getBody(),
                    TypeToken.getParameterized(HashMap.class, String.class, String.class)
                            .getType()
            );

            // Create response and result objects
            // "code" can be left null since it's DCF
            final MicrosoftStsAuthorizationResponse authorizationResponse =
                    new MicrosoftStsAuthorizationResponse(null, authorizationRequest.getState(), parsedResponseBody);

            // MicrosoftSTAuthorizationResultFactory not used since no Intent is being created
            final AuthorizationResult authorizationResult = new MicrosoftStsAuthorizationResult(AuthorizationStatus.SUCCESS, authorizationResponse);

            Logger.verbose(
                    TAG + methodName,
                    "Device Code Flow authorization successful..."
            );

            return authorizationResult;
        }

        // Request failed
        else {
            // Get and parse response body
            final HashMap<String, Object> parsedResponseBody = new Gson().fromJson(
                    response.getBody(),
                    TypeToken.getParameterized(HashMap.class, String.class, Object.class)
                            .getType()
            );

            // Create response and result objects
            final MicrosoftStsAuthorizationErrorResponse authorizationErrorResponse =
                    new MicrosoftStsAuthorizationErrorResponse(
                            (String) parsedResponseBody.get(ERROR),
                            (String) parsedResponseBody.get(ERROR_DESCRIPTION));

            // MicrosoftSTAuthorizationResultFactory not used since no Intent is being created
            final AuthorizationResult authorizationResult = new MicrosoftStsAuthorizationResult(AuthorizationStatus.FAIL, authorizationErrorResponse);

            Logger.verbose(
                    TAG + methodName,
                    "Device Code Flow authorization failure..."
            );

            return authorizationResult;
        }
    }

    protected GenericOAuth2Configuration getOAuth2Configuration() {
        return mConfig;
    }

    protected URI getIssuer() {
        return mIssuer;
    }

    protected final void setIssuer(final URI issuer) {
        mIssuer = issuer;
    }

    /**
     * An abstract method for returning the issuer identifier to be used when caching a token response.
     *
     * @param request generic token request.
     * @return String of issuer cache identifier.
     */
    public abstract String getIssuerCacheIdentifier(GenericAuthorizationRequest request) throws ClientException;

    /**
     * @param response generic token response.
     * @return generic access token.
     */
    public abstract GenericAccessToken getAccessTokenFromResponse(GenericTokenResponse response);

    /**
     * @param response generic token response.
     * @return generic refresh token.
     */
    public abstract GenericRefreshToken getRefreshTokenFromResponse(GenericTokenResponse response);

    /**
     * An abstract method for returning the user associated with a request.
     * This could be based on the contents of the ID Token or it could be returned based on making a call
     * to the user_info or profile endpoint associated with a userr: For example: graph.microsoft.com/me
     * This allows IDPs that do not support OIDC to still be able to return a user to us
     * This method should take the TokenResponse as a parameter
     *
     * @param response Generic token response.
     * @return GenericAccount
     */
    public abstract GenericAccount createAccount(GenericTokenResponse response);

    /**
     * Abstract method for creating the authorization request.
     *
     * @return AuthorizationRequest.
     */
    public abstract GenericAuthorizationRequestBuilder createAuthorizationRequestBuilder();

    /**
     * Abstract method for creating the authorization request.
     *
     * @param account The IAccount available to this strategy.
     * @return AuthorizationRequest.
     */
    public abstract GenericAuthorizationRequestBuilder createAuthorizationRequestBuilder(IAccountRecord account);

    /**
     * Abstract method for creating the token request.  In the case of AAD this is the method
     *
     * @return TokenRequest.
     */
    public abstract GenericTokenRequest createTokenRequest(GenericAuthorizationRequest request,
                                                           GenericAuthorizationResponse response,
                                                           AbstractAuthenticationScheme authScheme)
            throws ClientException;

    /**
     * Abstract method for creating the refresh token request.
     *
     * @return TokenRequest.
     */
    public abstract GenericTokenRequest createRefreshTokenRequest(AbstractAuthenticationScheme authScheme) throws ClientException;

    /**
     * Abstract method for creating the ropc token request.
     *
     * @return TokenRequest.
     */
    public abstract GenericTokenRequest createRopcTokenRequest(RopcTokenCommandParameters tokenCommandParameters) throws ClientException;

    /**
     * Abstract method for validating the authorization request.  In the case of AAD this is the method
     * from which the details of the authorization request including authority validation would occur (preferred network and preferred cache)
     *
     * @param request generic authorization request.
     */
    protected abstract void validateAuthorizationRequest(GenericAuthorizationRequest request);

    /**
     * Abstract method for validating the token request.  Generally speaking I expect this just to be validating
     * that all of the information was provided in the Token Request in order to successfully complete it.
     *
     * @param request Generic token request.
     */
    protected abstract void validateTokenRequest(GenericTokenRequest request);

    /**
     * Abstract method for translating the HttpResponse to a TokenResponse.
     *
     * @param response Http response.
     */
    protected abstract GenericTokenResult getTokenResultFromHttpResponse(HttpResponse response) throws ClientException;

    // TODO
//    protected abstract void validateAuthorizationResponse(GenericAuthorizationResponse response);

    protected abstract void validateTokenResponse(GenericTokenRequest request, GenericTokenResponse response) throws ClientException;

    /**
     * Validate the result yielded from the cache prior to returning it to the caller.
     *
     * @param cacheRecord The {@link ICacheRecord} to validate.
     * @return True if the CacheRecord checks out. False otherwise.
     */
    public boolean validateCachedResult(@NonNull final AbstractAuthenticationScheme authScheme,
                                        @NonNull final ICacheRecord cacheRecord) {
        // TODO Perform validations on the CacheRecord prior to returning result...

        // TODO Ideally, we would check the expiry, completeness of record, etc here...
        // TODO This requires refactoring the controllers to relocate logic to here.
        // TODO Broker looks to need a bit extra refactoring as its less integrated with the strategy model
        return true;
    }
}
