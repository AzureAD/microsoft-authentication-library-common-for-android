//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.common.internal.result;

import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.BROKER_ACCOUNTS;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.BROKER_ACCOUNTS_COMPRESSED;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.BROKER_ACTIVITY_NAME;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.BROKER_DEVICE_MODE;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.BROKER_GENERATE_SHR_RESULT;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.BROKER_GENERATE_SSO_TOKEN_RESULT;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.BROKER_PACKAGE_NAME;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.BROKER_RESULT_V2_COMPRESSED;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.HELLO_ERROR_CODE;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.HELLO_ERROR_MESSAGE;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.NEGOTIATED_BP_VERSION_KEY;
import static com.microsoft.identity.common.internal.util.GzipUtil.compressString;
import static com.microsoft.identity.common.java.exception.ClientException.INVALID_BROKER_BUNDLE;

import android.accounts.AccountManager;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.adal.internal.util.HashMapExtensions;
import com.microsoft.identity.common.adal.internal.util.JsonExtensions;
import com.microsoft.identity.common.internal.broker.BrokerResult;
import com.microsoft.identity.common.internal.request.AuthenticationSchemeTypeAdapter;
import com.microsoft.identity.common.internal.util.GzipUtil;
import com.microsoft.identity.common.java.cache.ICacheRecord;
import com.microsoft.identity.common.java.commands.AcquirePrtSsoTokenResult;
import com.microsoft.identity.common.java.constants.OAuth2ErrorCode;
import com.microsoft.identity.common.java.constants.OAuth2SubErrorCode;
import com.microsoft.identity.common.java.dto.AccessTokenRecord;
import com.microsoft.identity.common.java.dto.IAccountRecord;
import com.microsoft.identity.common.java.exception.ArgumentException;
import com.microsoft.identity.common.java.exception.BaseException;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.exception.ErrorStrings;
import com.microsoft.identity.common.java.exception.IntuneAppProtectionPolicyRequiredException;
import com.microsoft.identity.common.java.exception.ServiceException;
import com.microsoft.identity.common.java.exception.UiRequiredException;
import com.microsoft.identity.common.java.exception.UnsupportedBrokerException;
import com.microsoft.identity.common.java.exception.UserCancelException;
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationResult;
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationResult;
import com.microsoft.identity.common.java.request.SdkType;
import com.microsoft.identity.common.java.result.AcquireTokenResult;
import com.microsoft.identity.common.java.result.GenerateShrResult;
import com.microsoft.identity.common.java.result.ILocalAuthenticationResult;
import com.microsoft.identity.common.java.result.LocalAuthenticationResult;
import com.microsoft.identity.common.java.util.BrokerProtocolVersionUtil;
import com.microsoft.identity.common.java.util.HeaderSerializationUtil;
import com.microsoft.identity.common.java.util.StringUtil;
import com.microsoft.identity.common.java.util.ObjectMapper;
import com.microsoft.identity.common.logging.Logger;

import org.json.JSONException;

import java.io.IOException;
import java.util.List;

/**
 * For Broker: constructs result bundle.
 * For MSAL: unpack result bundle.
 */
public class MsalBrokerResultAdapter implements IBrokerResultAdapter {

    private static final String TAG = MsalBrokerResultAdapter.class.getSimpleName();
    public static final Gson GSON = new Gson();

    @NonNull
    @Override
    public Bundle bundleFromAuthenticationResult(@NonNull final ILocalAuthenticationResult authenticationResult,
                                                 @Nullable final String negotiatedBrokerProtocolVersion) {
        final String methodTag = TAG + ":bundleFromAuthenticationResult";
        Logger.info(methodTag, "Constructing result bundle from ILocalAuthenticationResult");

        final IAccountRecord accountRecord = authenticationResult.getAccountRecord();

        final AccessTokenRecord accessTokenRecord = authenticationResult.getAccessTokenRecord();

        final long expiresOn = Long.parseLong(accessTokenRecord.getExpiresOn());

        // eSTS doesn't return Extended Expires On for MSA accounts (ext_expires_on is an optional
        // field). So using same value here as expires on since we need to send something back to
        // MSAL. It seems we have historically passed this optional field from broker to MSAL,
        // however it seems MSAL just ignores ext_expires_on when creating its own version of
        // Authentication Result.
        final long extendedExpiresOn = accessTokenRecord.getExtendedExpiresOn() == null
                ? expiresOn
                : Long.parseLong(accessTokenRecord.getExtendedExpiresOn());

        final BrokerResult brokerResult = new BrokerResult.Builder()
                .tenantProfileRecords(authenticationResult.getCacheRecordWithTenantProfileData())
                .accessToken(authenticationResult.getAccessToken())
                .idToken(authenticationResult.getIdToken())
                .refreshToken(authenticationResult.getRefreshToken())
                .homeAccountId(accountRecord.getHomeAccountId())
                .localAccountId(accountRecord.getLocalAccountId())
                .userName(accountRecord.getUsername())
                .tokenType(accessTokenRecord.getAccessTokenType())
                .clientId(accessTokenRecord.getClientId())
                .familyId(authenticationResult.getFamilyId())
                .scope(accessTokenRecord.getTarget())
                .clientInfo(accountRecord.getClientInfo())
                .authority(accessTokenRecord.getAuthority())
                .environment(accessTokenRecord.getEnvironment())
                .tenantId(authenticationResult.getTenantId())
                .expiresOn(expiresOn)
                .extendedExpiresOn(extendedExpiresOn)
                .cachedAt(Long.parseLong(accessTokenRecord.getCachedAt()))
                .speRing(authenticationResult.getSpeRing())
                .refreshTokenAge(authenticationResult.getRefreshTokenAge())
                .success(true)
                .servicedFromCache(authenticationResult.isServicedFromCache())
                .build();

        final Bundle resultBundle = bundleFromBrokerResult(brokerResult, negotiatedBrokerProtocolVersion);
        resultBundle.putBoolean(AuthenticationConstants.Broker.BROKER_REQUEST_V2_SUCCESS, true);

        return resultBundle;
    }

    @NonNull
    @Override
    public Bundle bundleFromBaseException(@NonNull final BaseException exception,
                                          @Nullable final String negotiatedBrokerProtocolVersion) {
        final String methodTag = TAG + ":bundleFromBaseException";
        Logger.info(methodTag, "Constructing result bundle from ClientException");

        final BrokerResult.Builder builder = new BrokerResult.Builder()
                .success(false)
                .errorCode(exception.getErrorCode())
                .errorMessage(exception.getMessage())
                .exceptionType(exception.getExceptionName())
                .correlationId(exception.getCorrelationId())
                .cliTelemErrorCode(exception.getCliTelemErrorCode())
                .cliTelemSubErrorCode(exception.getCliTelemSubErrorCode())
                .speRing(exception.getSpeRing())
                .refreshTokenAge(exception.getRefreshTokenAge());

        if (exception instanceof ServiceException) {
            final ServiceException serviceException = (ServiceException) exception;
            builder.oauthSubErrorCode(serviceException.getOAuthSubErrorCode())
                    .httpStatusCode(serviceException.getHttpStatusCode())
                    .httpResponseBody(AuthenticationSchemeTypeAdapter.getGsonInstance().toJson(
                            serviceException.getHttpResponseBody()));

            if (serviceException.getHttpResponseHeaders() != null) {
                builder.httpResponseHeaders(
                        HeaderSerializationUtil.toJson((
                                (ServiceException) exception).getHttpResponseHeaders()
                        ));
            }

            if (serviceException.getHttpResponseBody() != null) {
                builder.httpResponseBody(
                        AuthenticationSchemeTypeAdapter.getGsonInstance().toJson(
                                serviceException.getHttpResponseBody()));
            }
        }

        if (exception instanceof IntuneAppProtectionPolicyRequiredException) {
            builder.userName(((IntuneAppProtectionPolicyRequiredException) exception).getAccountUpn())
                    .localAccountId(((IntuneAppProtectionPolicyRequiredException) exception).getAccountUserId())
                    .authority(((IntuneAppProtectionPolicyRequiredException) exception).getAuthorityUrl())
                    .tenantId(((IntuneAppProtectionPolicyRequiredException) exception).getTenantId());
        }

        final Bundle resultBundle = bundleFromBrokerResult(builder.build(), negotiatedBrokerProtocolVersion);
        resultBundle.putBoolean(AuthenticationConstants.Broker.BROKER_REQUEST_V2_SUCCESS, false);

        return resultBundle;
    }

    @NonNull
    @Override
    public ILocalAuthenticationResult authenticationResultFromBundle(@NonNull final Bundle resultBundle) throws ClientException {
        final String methodTag = TAG + ":authenticationResultFromBundle";
        final BrokerResult brokerResult = brokerResultFromBundle(resultBundle);

        Logger.info(methodTag, "Broker Result returned from Bundle, constructing authentication result");

        final List<ICacheRecord> tenantProfileCacheRecords = brokerResult.getTenantProfileData();
        if (tenantProfileCacheRecords == null) {
            Logger.error(methodTag, "getTenantProfileData is null", null);
            throw new ClientException(INVALID_BROKER_BUNDLE, "getTenantProfileData is null.");
        }

        return new LocalAuthenticationResult(
                tenantProfileCacheRecords.get(0),
                tenantProfileCacheRecords,
                SdkType.MSAL,
                brokerResult.isServicedFromCache()
        );
    }

    @NonNull
    @Override
    public BaseException getBaseExceptionFromBundle(@NonNull final Bundle resultBundle) {
        final String methodTag = TAG + ":getBaseExceptionFromBundle";
        Logger.info(methodTag, "Constructing exception from result bundle");

        final BrokerResult brokerResult;
        try {
            brokerResult = brokerResultFromBundle(resultBundle);
        } catch (final ClientException e) {
            return e;
        }

        final String exceptionType = brokerResult.getExceptionType();

        if (!StringUtil.isNullOrEmpty(exceptionType)) {
            return getBaseExceptionFromExceptionType(exceptionType, brokerResult);
        } else {
            // This code is here for legacy purposes where old versions of broker (3.1.8 or below)
            // wouldn't return exception type in the result.
            Logger.info(methodTag, "Exception type is not returned from the broker, " +
                    "using error codes to transform to the right exception");
            return getBaseExceptionFromErrorCodes(brokerResult);
        }
    }

    @NonNull
    @Override
    public AcquirePrtSsoTokenResult getAcquirePrtSsoTokenResultFromBundle(Bundle resultBundle) {
        return GSON.fromJson(resultBundle.getString(BROKER_GENERATE_SSO_TOKEN_RESULT), AcquirePrtSsoTokenResult.class);
    }

    @NonNull
    public Bundle bundleFromBrokerResult(@NonNull final BrokerResult brokerResult,
                                         @Nullable final String negotiatedBrokerProtocolVersion) {
        final String methodTag = TAG + ":bundleFromBrokerResult";
        final Bundle resultBundle = new Bundle();
        final String brokerResultString = AuthenticationSchemeTypeAdapter.getGsonInstance().toJson(
                brokerResult,
                BrokerResult.class
        );
        if (BrokerProtocolVersionUtil.canCompressBrokerPayloads(negotiatedBrokerProtocolVersion)) {
            try {
                byte[] compressedBytes = compressString(brokerResultString);
                Logger.info(methodTag, "Broker Result, raw payload size:"
                        + brokerResultString.getBytes(AuthenticationConstants.CHARSET_UTF8).length + " ,compressed bytes " + compressedBytes.length
                );
                resultBundle.putByteArray(
                        BROKER_RESULT_V2_COMPRESSED,
                        compressedBytes
                );
            } catch (IOException e) {
                Logger.error(methodTag, "Failed to compress Broker Result, sending as jsonString ", e);
                resultBundle.putString(
                        AuthenticationConstants.Broker.BROKER_RESULT_V2,
                        brokerResultString
                );
            }
        } else {
            Logger.info(methodTag, "Broker protocol version: " + negotiatedBrokerProtocolVersion +
                    " lower than compression changes, sending as string"
            );
            resultBundle.putString(
                    AuthenticationConstants.Broker.BROKER_RESULT_V2,
                    brokerResultString
            );
        }
        return resultBundle;
    }

    @NonNull
    public BrokerResult brokerResultFromBundle(@NonNull final Bundle resultBundle) throws ClientException {
        final String methodTag = TAG + ":brokerResultFromBundle";

        String brokerResultString;

        byte[] compressedBytes = resultBundle.getByteArray(BROKER_RESULT_V2_COMPRESSED);
        if (compressedBytes != null) {
            try {
                brokerResultString = GzipUtil.decompressBytesToString(compressedBytes);
            } catch (IOException e) {
                // We should never hit this ideally unless the string/bytes are malformed for some unknown reason.
                // The caller should handle the null broker result
                Logger.error(methodTag, "Failed to decompress broker result :", e);
                throw new ClientException(INVALID_BROKER_BUNDLE, "Failed to decompress broker result", e);
            }
        } else {
            brokerResultString = resultBundle.getString(AuthenticationConstants.Broker.BROKER_RESULT_V2);
        }

        if (StringUtil.isNullOrEmpty(brokerResultString)) {
            Logger.error(methodTag, "Broker Result not returned from Broker", null);
            throw new ClientException(INVALID_BROKER_BUNDLE, "Broker Result not returned from Broker", null);
        }

        return JsonExtensions.getBrokerResultFromJsonString(brokerResultString);
    }

    @NonNull
    private BaseException getBaseExceptionFromExceptionType(@NonNull final String exceptionType,
                                                            @NonNull final BrokerResult brokerResult) {
        final String methodTag = TAG + ":getBaseExceptionFromExceptionType";
        BaseException baseException;

        Logger.warn(methodTag, "Received a " + exceptionType + " from Broker : "
                + brokerResult.getErrorCode()
        );

        if (exceptionType.equalsIgnoreCase(UiRequiredException.sName)) {

            baseException = getUiRequiredException(brokerResult);

        } else if (exceptionType.equalsIgnoreCase(ServiceException.sName)) {

            baseException = getServiceException(brokerResult);

        } else if (exceptionType.equalsIgnoreCase(IntuneAppProtectionPolicyRequiredException.sName)) {

            baseException = getIntuneProtectionRequiredException(brokerResult);

        } else if (exceptionType.equalsIgnoreCase(UserCancelException.sName)) {

            baseException = new UserCancelException();

        } else if (exceptionType.equalsIgnoreCase(ClientException.sName)) {

            baseException = new ClientException(
                    brokerResult.getErrorCode(),
                    brokerResult.getErrorMessage()
            );

        } else if (exceptionType.equalsIgnoreCase(ArgumentException.sName)) {

            baseException = new ArgumentException(
                    ArgumentException.BROKER_TOKEN_REQUEST_OPERATION_NAME,
                    brokerResult.getErrorCode(),
                    brokerResult.getErrorMessage()
            );

        } else {
            // Default to ClientException if null
            Logger.warn(methodTag, " Exception type is unknown : " + exceptionType
                    + brokerResult.getErrorCode() + ", defaulting to Client Exception "
            );
            baseException = new ClientException(
                    brokerResult.getErrorCode(),
                    brokerResult.getErrorMessage()
            );
        }

        baseException.setCliTelemErrorCode(brokerResult.getCliTelemErrorCode());
        baseException.setCliTelemSubErrorCode(brokerResult.getCliTelemSubErrorCode());
        baseException.setCorrelationId(brokerResult.getCorrelationId());
        baseException.setSpeRing(brokerResult.getSpeRing());
        baseException.setRefreshTokenAge(brokerResult.getRefreshTokenAge());

        return baseException;
    }


    /**
     * Method to get the right base exception based on error codes.
     * Note : In newer versions of Broker, exception type will be sent and is used to determine the right exception.
     * <p>
     * This method is to support legacy broker versions (3.1.8 or below)
     *
     * @return {@link BaseException}
     */
    @NonNull
    private BaseException getBaseExceptionFromErrorCodes(@NonNull final BrokerResult brokerResult) {
        final String methodTag = TAG + ":getBaseExceptionFromErrorCodes";
        final String errorCode = brokerResult.getErrorCode();
        final BaseException baseException;

        //INTERACTION_REQUIRED is marked as deprecated
        if (OAuth2ErrorCode.INTERACTION_REQUIRED.equalsIgnoreCase(errorCode) ||
                OAuth2ErrorCode.INVALID_GRANT.equalsIgnoreCase(errorCode) ||
                ErrorStrings.INVALID_BROKER_REFRESH_TOKEN.equalsIgnoreCase(errorCode) ||
                ErrorStrings.NO_ACCOUNT_FOUND.equalsIgnoreCase(errorCode) ||
                ErrorStrings.NO_TOKENS_FOUND.equalsIgnoreCase(errorCode)) {

            Logger.warn(methodTag, "Received a UIRequired exception from Broker : " + errorCode);
            baseException = getUiRequiredException(brokerResult);

        } else if (OAuth2ErrorCode.UNAUTHORIZED_CLIENT.equalsIgnoreCase(errorCode) &&
                OAuth2SubErrorCode.PROTECTION_POLICY_REQUIRED.
                        equalsIgnoreCase(brokerResult.getSubErrorCode())) {

            Logger.warn(
                    methodTag,
                    "Received a IntuneAppProtectionPolicyRequiredException exception from Broker : "
                            + errorCode);
            baseException = getIntuneProtectionRequiredException(brokerResult);

        } else if (ErrorStrings.USER_CANCELLED.equalsIgnoreCase(errorCode)) {

            Logger.warn(methodTag, "Received a User cancelled exception from Broker : " + errorCode);
            baseException = new UserCancelException();

        } else if (ArgumentException.ILLEGAL_ARGUMENT_ERROR_CODE.equalsIgnoreCase(errorCode)) {

            Logger.warn(methodTag, "Received a Argument exception from Broker : " + errorCode);
            baseException = new ArgumentException(
                    ArgumentException.BROKER_TOKEN_REQUEST_OPERATION_NAME,
                    errorCode,
                    brokerResult.getErrorMessage()
            );

        } else if (!StringUtil.isNullOrEmpty(brokerResult.getHttpResponseHeaders()) ||
                !StringUtil.isNullOrEmpty(brokerResult.getHttpResponseBody())) {

            Logger.warn(methodTag, "Received a Service exception from Broker : " + errorCode);
            baseException = getServiceException(brokerResult);

        } else {

            Logger.warn(methodTag, "Received a Client exception from Broker : " + errorCode);
            baseException = new ClientException(
                    brokerResult.getErrorCode(),
                    brokerResult.getErrorMessage()
            );
        }

        baseException.setCliTelemErrorCode(brokerResult.getCliTelemErrorCode());
        baseException.setCliTelemSubErrorCode(brokerResult.getCliTelemSubErrorCode());
        baseException.setCorrelationId(brokerResult.getCorrelationId());
        baseException.setSpeRing(brokerResult.getSpeRing());
        baseException.setRefreshTokenAge(brokerResult.getRefreshTokenAge());

        return baseException;
    }

    /**
     * Helper method to retrieve IntuneAppProtectionPolicyRequiredException from BrokerResult
     */
    @NonNull
    private IntuneAppProtectionPolicyRequiredException getIntuneProtectionRequiredException(
            @NonNull final BrokerResult brokerResult) {
        final String methodTag = TAG + ":getIntuneProtectionRequiredException";
        final IntuneAppProtectionPolicyRequiredException exception =
                new IntuneAppProtectionPolicyRequiredException(
                        brokerResult.getErrorCode(),
                        brokerResult.getErrorMessage()
                );
        exception.setTenantId(brokerResult.getTenantId());
        exception.setAuthorityUrl(brokerResult.getAuthority());
        exception.setAccountUserId(brokerResult.getLocalAccountId());
        exception.setAccountUpn(brokerResult.getUserName());
        exception.setOauthSubErrorCode(brokerResult.getSubErrorCode());
        try {
            exception.setHttpResponseBody(HashMapExtensions.jsonStringAsMap(
                    brokerResult.getHttpResponseBody())
            );
            if (brokerResult.getHttpResponseHeaders() != null) {
                exception.setHttpResponseHeaders(
                        HeaderSerializationUtil.fromJson(
                                brokerResult.getHttpResponseHeaders())
                );
            }
        } catch (JSONException e) {
            Logger.warn(methodTag, "Unable to parse json");
        }
        return exception;
    }

    /**
     * Helper method to retrieve ServiceException from BrokerResult
     */
    @NonNull
    private ServiceException getServiceException(@NonNull final BrokerResult brokerResult) {
        final String methodTag = TAG + ":getServiceException";
        final ServiceException serviceException = new ServiceException(
                brokerResult.getErrorCode(),
                brokerResult.getErrorMessage(),
                null
        );

        serviceException.setOauthSubErrorCode(brokerResult.getSubErrorCode());

        try {
            serviceException.setHttpResponseBody(
                    brokerResult.getHttpResponseBody() != null ?
                            HashMapExtensions.jsonStringAsMap(
                                    brokerResult.getHttpResponseBody()) :
                            null
            );
            serviceException.setHttpResponseHeaders(
                    brokerResult.getHttpResponseHeaders() != null ?
                            HeaderSerializationUtil.fromJson(
                                    brokerResult.getHttpResponseHeaders()) :
                            null
            );

        } catch (JSONException e) {
            Logger.warn(methodTag, "Unable to parse json");
        }
        return serviceException;

    }

    /**
     * Helper method to retrieve UiRequiredException from BrokerResult
     *
     * @return {@link com.microsoft.identity.common.java.exception.UiRequiredException}
     */
    @NonNull
    private UiRequiredException getUiRequiredException(@NonNull final BrokerResult brokerResult) {
        final String errorCode = brokerResult.getErrorCode();
        final UiRequiredException exception = new UiRequiredException(
                errorCode,
                brokerResult.getErrorMessage()
        );
        if (OAuth2ErrorCode.INTERACTION_REQUIRED.equalsIgnoreCase(errorCode) ||
                OAuth2ErrorCode.INVALID_GRANT.equalsIgnoreCase(errorCode)) {
            exception.setOauthSubErrorCode(brokerResult.getSubErrorCode());
        }
        return exception;
    }

    @NonNull
    public String verifyHelloFromResultBundle(@NonNull final String activeBrokerPackageName,
                                              @Nullable final Bundle bundle) throws BaseException {
        final String methodTag = TAG + ":verifyHelloFromResultBundle";

        // This means that the Broker doesn't support hello().
        if (bundle == null) {
            Logger.warn(methodTag, "The hello result bundle is null.");
            throw new UnsupportedBrokerException(activeBrokerPackageName);
        }

        final String negotiatedBrokerProtocolVersion = bundle.getString(AuthenticationConstants.Broker.NEGOTIATED_BP_VERSION_KEY);
        if (!StringUtil.isNullOrEmpty(negotiatedBrokerProtocolVersion)) {
            Logger.info(methodTag,
                    "Able to establish the connect, " +
                            "the broker protocol version in common is ["
                            + negotiatedBrokerProtocolVersion + "]");
            return negotiatedBrokerProtocolVersion;
        }

        // Actual handshake error.
        if (!StringUtil.isNullOrEmpty(bundle.getString(HELLO_ERROR_CODE))) {
            final String errorCode = bundle.getString(HELLO_ERROR_CODE);
            final String errorMessage = bundle.getString(HELLO_ERROR_MESSAGE);
            throw new UnsupportedBrokerException(activeBrokerPackageName, errorCode, errorMessage);
        }

        final Object resultObject = bundle.get(AuthenticationConstants.Broker.BROKER_RESULT_V2);
        if (resultObject instanceof BrokerResult) {
            // for the back compatibility purpose to version 3.0.4 and 3.0.6.
            final BrokerResult brokerResult = (BrokerResult) resultObject;
            throw new ClientException(brokerResult.getErrorCode(), brokerResult.getErrorMessage());
        }

        // This means that the Broker doesn't support hello().
        Logger.warn(methodTag, "The result bundle is not in a recognizable format.");
        throw new UnsupportedBrokerException(activeBrokerPackageName);
    }

    @NonNull
    public Intent getIntentForInteractiveRequestFromResultBundle(@NonNull final Bundle resultBundle,
                                                                 @NonNull final String negotiatedBrokerProtocolVersion) throws ClientException {
        final Bundle interactiveRequestBundle = extractInteractiveRequestBundleFromResultBundle(resultBundle);

        final String packageName = interactiveRequestBundle.getString(BROKER_PACKAGE_NAME);
        final String className = interactiveRequestBundle.getString(BROKER_ACTIVITY_NAME);
        if (StringUtil.isNullOrEmpty(packageName) ||
                StringUtil.isNullOrEmpty(className)) {
            throw new ClientException(INVALID_BROKER_BUNDLE, "Content of Authorization Intent's package and class name should not be null.");
        }

        final Intent intent = new Intent();
        intent.setPackage(packageName);
        intent.setClassName(
                packageName,
                className
        );
        intent.putExtras(interactiveRequestBundle);
        intent.putExtra(NEGOTIATED_BP_VERSION_KEY, negotiatedBrokerProtocolVersion);
        return intent;
    }

    /**
     * AccountManager strategy is currently returning result in a different format compared with the other strategies.
     * We should make sure this does NOT happen going forward.
     */
    public Bundle extractInteractiveRequestBundleFromResultBundle(@NonNull final Bundle resultBundle) {
        final Intent interactiveRequestIntent = resultBundle.getParcelable(AccountManager.KEY_INTENT);
        if (interactiveRequestIntent != null) {
            return interactiveRequestIntent.getExtras();
        }

        return resultBundle;
    }

    /**
     * Get authorizationResult from resultBundle for Device Code Flow
     * @param resultBundle The bundle to interpret
     * @return authorizationResult {@link AuthorizationResult}
     * @throws BaseException
     * @throws ClientException
     */
    @NonNull
    public AuthorizationResult getDeviceCodeFlowAuthResultFromResultBundle(@NonNull final Bundle resultBundle) throws BaseException, ClientException {
        String serializedDCFAuthResult = resultBundle.getString(AuthenticationConstants.Broker.BROKER_DCF_AUTH_RESULT);
        if (serializedDCFAuthResult != null) {
            AuthorizationResult authorizationResult = ObjectMapper.deserializeJsonStringToObject(serializedDCFAuthResult, MicrosoftStsAuthorizationResult.class);
            return authorizationResult;
        }

        // DCF not supported - thrown when BrokerFlight.ENABLE_DCF_IN_BROKER is false
        BrokerResult brokerResult = brokerResultFromBundle(resultBundle);
        if (brokerResult.getErrorCode() != null && brokerResult.getErrorCode().equals(ErrorStrings.DEVICE_CODE_FLOW_NOT_SUPPORTED)) {
            Logger.error(TAG, "deviceCodeFlowAuthRequest() not supported in BrokerMsalController", new ClientException(ErrorStrings.DEVICE_CODE_FLOW_NOT_SUPPORTED, "deviceCodeFlowAuthRequest() not supported in BrokerMsalController"));
            throw new ClientException(ErrorStrings.DEVICE_CODE_FLOW_NOT_SUPPORTED, "deviceCodeFlowAuthRequest() not supported in BrokerMsalController");
        }

        throw getBaseExceptionFromBundle(resultBundle);
    }

    /**
     * Get acquireTokenResult from resultBundle for Device Code Flow
     * @param resultBundle The bundle to interpret
     * @return acquireTokenResult {@link AcquireTokenResult}
     * @throws BaseException
     * @throws ClientException
     */
    @NonNull
    public AcquireTokenResult getDeviceCodeFlowTokenResultFromResultBundle(@NonNull final Bundle resultBundle) throws BaseException, ClientException {
        // DCF not supported - thrown when BrokerFlight.ENABLE_DCF_IN_BROKER is false
        BrokerResult brokerResult = brokerResultFromBundle(resultBundle);
        if (brokerResult.getErrorCode() != null && brokerResult.getErrorCode().equals(ErrorStrings.DEVICE_CODE_FLOW_NOT_SUPPORTED)) {
            Logger.error(TAG, "acquireDeviceCodeFlowToken() not supported in BrokerMsalController", new ClientException(ErrorStrings.DEVICE_CODE_FLOW_NOT_SUPPORTED, "deviceCodeFlowAuthRequest() not supported in BrokerMsalController"));
            throw new ClientException(ErrorStrings.DEVICE_CODE_FLOW_NOT_SUPPORTED, "acquireDeviceCodeFlowToken() not supported in BrokerMsalController");
        }

        if (resultBundle.getBoolean(AuthenticationConstants.Broker.BROKER_REQUEST_V2_SUCCESS)) {
            final AcquireTokenResult acquireTokenResult = new AcquireTokenResult();
            acquireTokenResult.setLocalAuthenticationResult(authenticationResultFromBundle(resultBundle));
            return acquireTokenResult;
        } else if (brokerResult.getErrorCode().equals(ErrorStrings.DEVICE_CODE_FLOW_AUTHORIZATION_PENDING_ERROR_CODE)) {
            return null;
        }

        throw getBaseExceptionFromBundle(resultBundle);
    }

    public @NonNull
    AcquireTokenResult getAcquireTokenResultFromResultBundle(@NonNull final Bundle resultBundle) throws BaseException {
        final MsalBrokerResultAdapter resultAdapter = new MsalBrokerResultAdapter();
        if (resultBundle.getBoolean(AuthenticationConstants.Broker.BROKER_REQUEST_V2_SUCCESS)) {
            final AcquireTokenResult acquireTokenResult = new AcquireTokenResult();
            acquireTokenResult.setLocalAuthenticationResult(
                    resultAdapter.authenticationResultFromBundle(resultBundle)
            );

            return acquireTokenResult;
        }

        throw getBaseExceptionFromBundle(resultBundle);
    }

    @NonNull
    public Bundle bundleFromAccounts(@NonNull final List<ICacheRecord> cacheRecords,
                                     @Nullable final String negotiatedProtocolVersion) {
        final String methodTag = TAG + ":bundleFromAccounts";
        final Bundle resultBundle = new Bundle();

        final String jsonString = JsonExtensions.getJsonStringFromICacheRecordList(cacheRecords);
        if (BrokerProtocolVersionUtil.canCompressBrokerPayloads(negotiatedProtocolVersion)) {
            try {
                byte[] bytes = GzipUtil.compressString(jsonString);
                Logger.info(methodTag, "Get accounts, raw payload size :"
                        + jsonString.getBytes(AuthenticationConstants.CHARSET_UTF8).length + " compressed size " + bytes.length
                );
                resultBundle.putByteArray(BROKER_ACCOUNTS_COMPRESSED, bytes);
            } catch (IOException e) {
                Logger.error(methodTag, " Failed to compress account list to bytes, sending as jsonString", e);
                resultBundle.putString(BROKER_ACCOUNTS, jsonString);
            }
        } else {
            Logger.info(methodTag, "Broker protocol version: " + negotiatedProtocolVersion +
                    " lower than compression changes, sending as string"
            );
            resultBundle.putString(BROKER_ACCOUNTS, jsonString);
        }

        return resultBundle;
    }

    @NonNull
    public List<ICacheRecord> getAccountsFromResultBundle(@NonNull final Bundle bundle) throws BaseException {
        final String methodTag = TAG + ":getAccountsFromResultBundle";
        String accountJson;

        final byte[] compressedData = bundle.getByteArray(BROKER_ACCOUNTS_COMPRESSED);
        if (compressedData != null) {
            try {
                accountJson = GzipUtil.decompressBytesToString(compressedData);
            } catch (IOException e) {
                Logger.error(methodTag, " Failed to decompress account list to bytes", e);
                throw new ClientException(INVALID_BROKER_BUNDLE, " Failed to decompress account list to bytes.");
            }
        } else {
            accountJson = bundle.getString(BROKER_ACCOUNTS);
        }

        if (StringUtil.isNullOrEmpty(accountJson)) {
            throw new MsalBrokerResultAdapter().getBaseExceptionFromBundle(bundle);
        }

        return JsonExtensions.getICacheRecordListFromJsonString(accountJson);
    }

    public void verifyRemoveAccountResultFromBundle(@Nullable final Bundle bundle) throws BaseException {
        final String methodTag = TAG + ":verifyRemoveAccountResultFromBundle";
        if (bundle == null) {
            // Backward compatibility. We treated null = success.
            return;
        }

        final String brokerResultString = bundle.getString(AuthenticationConstants.Broker.BROKER_RESULT_V2);
        if (StringUtil.isNullOrEmpty(brokerResultString)) {
            throw getBaseExceptionFromBundle(bundle);
        }

        final BrokerResult brokerResult = JsonExtensions.getBrokerResultFromJsonString(brokerResultString);
        if (brokerResult != null && brokerResult.isSuccess()) {
            return;
        }

        Logger.warn(methodTag, "Failed to remove account.");
        throw getBaseExceptionFromBundle(bundle);
    }

    @NonNull
    public Bundle bundleFromDeviceMode(final boolean isSharedDevice) {
        final Bundle resultBundle = new Bundle();
        resultBundle.putBoolean(BROKER_DEVICE_MODE, isSharedDevice);
        return resultBundle;
    }

    public boolean getDeviceModeFromResultBundle(@NonNull final Bundle bundle) throws BaseException {
        if (!bundle.containsKey(BROKER_DEVICE_MODE)) {
            throw new MsalBrokerResultAdapter().getBaseExceptionFromBundle(bundle);
        }

        return bundle.getBoolean(BROKER_DEVICE_MODE);
    }

    @NonNull
    public ClientException getExceptionForEmptyResultBundle() {
        return new ClientException(INVALID_BROKER_BUNDLE, "Broker Result not returned from Broker.");
    }

    /**
     * Deserializes the {@link GenerateShrResult} object from the broker response {@link Bundle}.
     *
     * @param resultBundle The result Bundle produced by the broker.
     * @return The deserialized GenerateShrResult object containing the result (or corresponding
     * error).
     */
    public GenerateShrResult getGenerateShrResultFromResultBundle(@NonNull final Bundle resultBundle) {
        final String resultJson = resultBundle.getString(BROKER_GENERATE_SHR_RESULT);
        final GenerateShrResult shrResult = AuthenticationSchemeTypeAdapter.getGsonInstance().fromJson(
                resultJson,
                GenerateShrResult.class
        );

        return shrResult;
    }
}
