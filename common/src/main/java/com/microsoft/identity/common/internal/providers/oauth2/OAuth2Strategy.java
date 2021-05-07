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
package com.microsoft.identity.common.internal.providers.oauth2;

import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.microsoft.identity.common.BaseAccount;
import com.microsoft.identity.common.WarningType;
import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.internal.authscheme.AbstractAuthenticationScheme;
import com.microsoft.identity.common.internal.cache.ICacheRecord;
import com.microsoft.identity.common.internal.dto.IAccountRecord;
import com.microsoft.identity.common.internal.eststelemetry.EstsTelemetry;
import com.microsoft.identity.common.java.util.ObjectMapper;
import com.microsoft.identity.common.logging.DiagnosticContext;
import com.microsoft.identity.common.java.net.HttpClient;
import com.microsoft.identity.common.java.net.HttpConstants;
import com.microsoft.identity.common.java.net.HttpResponse;
import com.microsoft.identity.common.java.net.UrlConnectionHttpClient;
import com.microsoft.identity.common.internal.platform.Device;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftTokenRequest;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectorySlice;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationErrorResponse;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationRequest;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationResponse;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationResult;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Configuration;
import com.microsoft.identity.common.internal.util.ClockSkewManager;
import com.microsoft.identity.common.internal.util.IClockSkewManager;
import com.microsoft.identity.common.logging.Logger;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Future;

import javax.net.ssl.HttpsURLConnection;

import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AAD.CLIENT_REQUEST_ID;

/**
 * Serves as the abstract base class for an oAuth2 client implementation; The base class should be extended
 * by Identity Provider specific implementations; For example: Azure Active Directory, ADFS, Microsoft STS, Etc...
 */
// Suppressing rawtype warnings due to generic types AuthorizationRequest, AuthorizationReuest.Builder, AuthorizationStrategy, AuthorizationResult and AuthorizationResultFactory
@SuppressWarnings(WarningType.rawtype_warning)
public abstract class OAuth2Strategy
        <GenericAccessToken extends AccessToken,
                GenericAccount extends BaseAccount,
                GenericAuthorizationRequest extends AuthorizationRequest,
                GenericAuthorizationRequestBuilder extends AuthorizationRequest.Builder,
                GenericAuthorizationStrategy extends AuthorizationStrategy,
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
    private Uri mIssuer;

    /**
     * Constructor of OAuth2Strategy.
     *
     * @param config generic OAuth2 configuration
     */
    public OAuth2Strategy(GenericOAuth2Configuration config,
                          GenericOAuth2StrategyParameters strategyParameters) {
        mConfig = config;
        mStrategyParameters = strategyParameters;

        if (null != mStrategyParameters.getContext()) {
            mClockSkewManager = new ClockSkewManager(mStrategyParameters.getContext());
        } else {
            Logger.info(TAG, "No valid context to persist clock skew with!");
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
    public @NonNull
    Future<AuthorizationResult> requestAuthorization(
            final GenericAuthorizationRequest request,
            final GenericAuthorizationStrategy authorizationStrategy) throws ClientException {
        validateAuthorizationRequest(request);

        // Suppressing unchecked warnings due to casting an object in reference of current class to the child class GenericOAuth2Strategy while calling method requestAuthorization()
        @SuppressWarnings(WarningType.unchecked_warning) final Future<AuthorizationResult> authorizationFuture = authorizationStrategy.requestAuthorization(request, this);

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
        headers.put(CLIENT_REQUEST_ID, DiagnosticContext.getRequestContext().get(DiagnosticContext.CORRELATION_ID));

        if (request instanceof MicrosoftTokenRequest &&
                !TextUtils.isEmpty(((MicrosoftTokenRequest) request).getBrokerVersion())) {
            headers.put(
                    Device.PlatformIdParameters.BROKER_VERSION,
                    ((MicrosoftTokenRequest) request).getBrokerVersion()
            );
        }
        headers.putAll(Device.getPlatformIdParameters());
        headers.put(AuthenticationConstants.SdkPlatformFields.PRODUCT, DiagnosticContext.getRequestContext().get(AuthenticationConstants.SdkPlatformFields.PRODUCT));
        headers.put(AuthenticationConstants.SdkPlatformFields.VERSION, Device.getProductVersion());
        headers.putAll(EstsTelemetry.getInstance().getTelemetryHeaders());
        headers.put(HttpConstants.HeaderField.CONTENT_TYPE, TOKEN_REQUEST_CONTENT_TYPE);

        if (request instanceof MicrosoftTokenRequest) {
            headers.put(
                    AuthenticationConstants.AAD.APP_PACKAGE_NAME,
                    ((MicrosoftTokenRequest) request).getClientAppName()
            );
            headers.put(
                    AuthenticationConstants.AAD.APP_VERSION,
                    ((MicrosoftTokenRequest) request).getClientAppVersion()
            );
        }

        final URL requestUrl = new URL(getTokenEndpoint());
        final HttpResponse response = httpClient.post(
                requestUrl,
                headers,
                requestBody.getBytes(ObjectMapper.ENCODING_SCHEME)
        );

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

    protected final void setTokenEndpoint(final String tokenEndpoint) {
        mTokenEndpoint = tokenEndpoint;

        if (mConfig != null && mConfig instanceof MicrosoftStsOAuth2Configuration) {

            final MicrosoftStsOAuth2Configuration oauth2Config =
                    (MicrosoftStsOAuth2Configuration) mConfig;

            final AzureActiveDirectorySlice slice = oauth2Config.getSlice();

            if (slice != null) {
                final Uri.Builder uriBuilder = Uri.parse(mTokenEndpoint).buildUpon();

                if (!TextUtils.isEmpty(slice.getSlice())) {
                    uriBuilder.appendQueryParameter(AzureActiveDirectorySlice.SLICE_PARAMETER, slice.getSlice());
                }

                if (!TextUtils.isEmpty(slice.getDC())) {
                    uriBuilder.appendQueryParameter(AzureActiveDirectorySlice.DC_PARAMETER, slice.getDC());
                }

                mTokenEndpoint = uriBuilder.build().toString();
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
        headers.put(CLIENT_REQUEST_ID, DiagnosticContext.getRequestContext().get(DiagnosticContext.CORRELATION_ID));
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
            final HashMap<String, String> parsedResponseBody = new Gson().fromJson(response.getBody(), new TypeToken<HashMap<String, String>>() {
            }.getType());

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
            final HashMap<String, Object> parsedResponseBody = new Gson().fromJson(response.getBody(), new TypeToken<HashMap<String, Object>>() {
            }.getType());

            // Create response and result objects
            final MicrosoftStsAuthorizationErrorResponse authorizationErrorResponse =
                    new MicrosoftStsAuthorizationErrorResponse(
                            (String) parsedResponseBody.get(AuthorizationResultFactory.ERROR),
                            (String) parsedResponseBody.get(AuthorizationResultFactory.ERROR_DESCRIPTION));

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

    protected Uri getIssuer() {
        return mIssuer;
    }

    protected final void setIssuer(final Uri issuer) {
        mIssuer = issuer;
    }

    /**
     * An abstract method for returning the issuer identifier to be used when caching a token response.
     *
     * @param request generic token request.
     * @return String of issuer cache identifier.
     */
    public abstract String getIssuerCacheIdentifier(GenericAuthorizationRequest request);

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
