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
package com.microsoft.identity.common.internal.controllers;

import android.app.Service;
import android.text.TextUtils;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.JsonSyntaxException;
import com.microsoft.identity.common.WarningType;
import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.exception.BaseException;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.exception.DeviceRegistrationRequiredException;
import com.microsoft.identity.common.exception.IntuneAppProtectionPolicyRequiredException;
import com.microsoft.identity.common.exception.TerminalException;
import com.microsoft.identity.common.exception.ServiceException;
import com.microsoft.identity.common.exception.UiRequiredException;
import com.microsoft.identity.common.exception.UserCancelException;
import com.microsoft.identity.common.internal.authorities.Authority;
import com.microsoft.identity.common.internal.authorities.AzureActiveDirectoryAuthority;
import com.microsoft.identity.common.internal.commands.parameters.BrokerSilentTokenCommandParameters;
import com.microsoft.identity.common.internal.commands.parameters.CommandParameters;
import com.microsoft.identity.common.internal.commands.parameters.TokenCommandParameters;
import com.microsoft.identity.common.internal.net.HttpResponse;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftAuthorizationErrorResponse;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationErrorResponse;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResult;
import com.microsoft.identity.common.internal.providers.oauth2.TokenErrorResponse;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResult;
import com.microsoft.identity.common.internal.result.AcquireTokenResult;
import com.microsoft.identity.common.internal.telemetry.CliTelemInfo;
import com.microsoft.identity.common.internal.util.HeaderSerializationUtil;
import com.microsoft.identity.common.internal.util.StringUtil;
import com.microsoft.identity.common.logging.Logger;

import org.json.JSONException;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

public class ExceptionAdapter {

    private static final String TAG = ExceptionAdapter.class.getSimpleName();

    @Nullable
    public static BaseException exceptionFromAcquireTokenResult(final AcquireTokenResult result, final CommandParameters commandParameters) {
        final String methodName = ":exceptionFromAcquireTokenResult";

        @SuppressWarnings(WarningType.rawtype_warning)
        final AuthorizationResult authorizationResult = result.getAuthorizationResult();

        if (null != authorizationResult) {
            final AuthorizationErrorResponse authorizationErrorResponse = authorizationResult.getAuthorizationErrorResponse();
            if (!authorizationResult.getSuccess()) {
                //THERE ARE CURRENTLY NO USAGES of INVALID_REQUEST
                switch (result.getAuthorizationResult().getAuthorizationStatus()) {
                    case FAIL:
                        // Check if the error is to register device and throw DEVICE_REGISTRATION_NEEDED exception
                        if (authorizationErrorResponse instanceof MicrosoftAuthorizationErrorResponse) {
                            MicrosoftAuthorizationErrorResponse microsoftAuthorizationErrorResponse =
                                    (MicrosoftAuthorizationErrorResponse) authorizationErrorResponse;

                            if (microsoftAuthorizationErrorResponse.getError().equals(
                                    MicrosoftAuthorizationErrorResponse.DEVICE_REGISTRATION_NEEDED)) {

                                return new DeviceRegistrationRequiredException(
                                        microsoftAuthorizationErrorResponse.getError(),
                                        microsoftAuthorizationErrorResponse.getErrorDescription(),
                                        microsoftAuthorizationErrorResponse.getUserName()
                                );
                            }
                        }

                        return new ServiceException(
                                authorizationErrorResponse.getError(),
                                authorizationErrorResponse.getErrorDescription(),
                                ServiceException.DEFAULT_STATUS_CODE,
                                null
                        );

                    case SDK_CANCEL:
                        return new ClientException(
                                authorizationErrorResponse.getError(),
                                authorizationErrorResponse.getErrorDescription()
                        );

                    case USER_CANCEL:
                        return new UserCancelException();

                }
            }
        } else {
            Logger.warn(
                    TAG + methodName,
                    "AuthorizationResult was null -- expected for ATS cases."
            );
        }

        return exceptionFromTokenResult(result.getTokenResult(), commandParameters);
    }

    /**
     * Get an exception out of a TokenResult object.
     *
     * @param tokenResult
     * @return ServiceException, UiRequiredException
     * */
    public static ServiceException exceptionFromTokenResult(final TokenResult tokenResult, final CommandParameters commandParameters) {
        final String methodName = ":exceptionFromTokenResult";

        ServiceException outErr;

        if (tokenResult != null &&
                !tokenResult.getSuccess() &&
                tokenResult.getErrorResponse() != null &&
                !StringUtil.isEmpty(tokenResult.getErrorResponse().getError())) {

            outErr = getExceptionFromTokenErrorResponse(commandParameters, tokenResult.getErrorResponse());
            applyCliTelemInfo(tokenResult.getCliTelemInfo(), outErr);
        }else {
            Logger.warn(
                    TAG + methodName,
                    "Unknown error, Token result is null [" + (tokenResult == null) + "]"
            );
            outErr = new ServiceException(
                    ServiceException.UNKNOWN_ERROR,
                    "Request failed, but no error returned back from service.",
                    null
            );
        }

        return outErr;
    }

    /**
     * Determine if an exception owning the given error codes should be converted into UiRequiredException.
     *
     * @param oAuthError
     * @return boolean
     * */
    @SuppressWarnings("deprecation")
    private static boolean shouldBeConvertedToUiRequiredException(final String oAuthError){
        // Invalid_grant doesn't necessarily requires UI protocol-wise.
        // We simplify our logic because this layer is also used by MSAL.

        //Interaction required has been deprecated... hence suppressing warning.
        return AuthenticationConstants.OAuth2ErrorCode.INVALID_GRANT.equalsIgnoreCase(oAuthError) ||
                AuthenticationConstants.OAuth2ErrorCode.INTERACTION_REQUIRED.equalsIgnoreCase(oAuthError);

    }


    /**
     * Get an exception object from the given oAuth values.
     *
     * @param errorResponse
     * @return ServiceException, UiRequiredException
     * */
    public static ServiceException getExceptionFromTokenErrorResponse(@NonNull final TokenErrorResponse errorResponse) {

        final ServiceException outErr;

        if (shouldBeConvertedToUiRequiredException(errorResponse.getError())) {

            outErr = new UiRequiredException(
                    errorResponse.getError(),
                    errorResponse.getErrorDescription());
        } else {
            outErr = new ServiceException(
                    errorResponse.getError(),
                    errorResponse.getErrorDescription(),
                    null);
        }

        outErr.setOauthSubErrorCode(errorResponse.getSubError());
        setHttpResponseUsingTokenErrorResponse(outErr, errorResponse);
        return outErr;
    }


    public static ServiceException getExceptionFromTokenErrorResponse(@Nullable final CommandParameters commandParameters,
                                                                      @NonNull final TokenErrorResponse errorResponse) {

        if(isIntunePolicyRequiredError(errorResponse)){
            if(commandParameters == null || !(commandParameters instanceof BrokerSilentTokenCommandParameters)){
                Logger.warn(TAG, "In order to properly construct the IntuneAppProtectionPolicyRequiredException we need the command parameters to be supplied.  Returning as service exception instead.");
                return getExceptionFromTokenErrorResponse(errorResponse);
            }
            IntuneAppProtectionPolicyRequiredException policyRequiredException = new IntuneAppProtectionPolicyRequiredException(
                    errorResponse.getError(),
                    errorResponse.getErrorDescription()
            );
            policyRequiredException.setOauthSubErrorCode(errorResponse.getSubError());
            setHttpResponseUsingTokenErrorResponse(policyRequiredException, errorResponse);

            setIntuneExceptionProperties(
                    policyRequiredException,
                    (BrokerSilentTokenCommandParameters) commandParameters
            );
            return policyRequiredException;
        }else{
            return getExceptionFromTokenErrorResponse(errorResponse);
        }


    }

    /**
     * Name: setHttpResponseUsingTokenErrorResponse
     * @param exception ServiceException to which we will append an HttpResponse
     * @param errorResponse A TokenErrorResponse from which we will recontruct an HttpResponse
     */

    private static void setHttpResponseUsingTokenErrorResponse(@NonNull final ServiceException exception,
            @NonNull final TokenErrorResponse errorResponse){

        try {
            exception.setHttpResponse(
                    synthesizeHttpResponse(
                    errorResponse.getStatusCode(),
                    errorResponse.getResponseHeadersJson(),
                    errorResponse.getResponseBody()));
        } catch (JSONException e) {
            Logger.warn(
                    TAG,
                    "Failed to deserialize error data: status, headers, response body."
            );
        }

    }

    /**
     * Helper method to get uid from home account id
     * V2 home account format : <uid>.<utid>
     * V1 : it's stored as <uid>
     *
     * @param homeAccountId
     * @return valid uid or null if it's not in either of the format.
     */
    @Nullable
    public static String getUIdFromHomeAccountId(@Nullable String homeAccountId) {
        //TODO: This method is from BrokerOperationParameterUtils...
        // seems like this is not broker specific per se and should move to somewhere better
        final String methodName = ":getUIdFromHomeAccountId";
        final String DELIMITER_TENANTED_USER_ID = ".";
        final int EXPECTED_ARGS_LEN = 2;
        final int INDEX_USER_ID = 0;

        if (!TextUtils.isEmpty(homeAccountId)) {
            final String[] homeAccountIdSplit = homeAccountId.split(
                    Pattern.quote(DELIMITER_TENANTED_USER_ID)
            );

            if (homeAccountIdSplit.length == EXPECTED_ARGS_LEN) {
                com.microsoft.identity.common.internal.logging.Logger.info(TAG + methodName,
                        "Home account id is tenanted, returning uid "
                );
                return homeAccountIdSplit[INDEX_USER_ID];
            } else if (homeAccountIdSplit.length == 1) {
                com.microsoft.identity.common.internal.logging.Logger.info(TAG + methodName,
                        "Home account id not tenanted, it's the uid added by v1 broker "
                );
                return homeAccountIdSplit[INDEX_USER_ID];
            }
        }

        com.microsoft.identity.common.internal.logging.Logger.warn(TAG + methodName,
                "Home Account id doesn't have uid or tenant id information, returning null "
        );

        return null;
    }

    private static void setIntuneExceptionProperties(
            @NonNull final IntuneAppProtectionPolicyRequiredException exception,
            @NonNull final BrokerSilentTokenCommandParameters requestParameters) {

        com.microsoft.identity.common.internal.logging.Logger.info(TAG, "Setting properties to IntuneAppProtectionPolicyRequiredException ");

        final String upn = (requestParameters.getAccountManagerAccount() != null) ?
                requestParameters.getAccountManagerAccount().name :
                requestParameters.getLoginHint();
        exception.setAccountUpn(upn);

        String uId = requestParameters.getLocalAccountId();

        if (TextUtils.isEmpty(uId)) {
            com.microsoft.identity.common.internal.logging.Logger.info(TAG, "Local account id is empty, attempting get user id from home account id");
            uId = getUIdFromHomeAccountId(
                    requestParameters.getHomeAccountId()
            );
        }

        exception.setAccountUserId(uId);

        final Authority authority = requestParameters.getAuthority();
        exception.setAuthorityUrl(authority.getAuthorityURL().toString());

        final String homeAccountId = requestParameters.getHomeAccountId();
        String tenantId = null;

        if (homeAccountId != null) {
            final Pair<String, String> tenantInfo = StringUtil.getTenantInfo(homeAccountId);
            tenantId = tenantInfo.second;
        }

        if (TextUtils.isEmpty(tenantId) && authority instanceof AzureActiveDirectoryAuthority) {
            tenantId = ((AzureActiveDirectoryAuthority) authority).mAudience.getTenantId();

        }
        exception.setTenantId(tenantId);

    }

    public static void applyCliTelemInfo(@Nullable final CliTelemInfo cliTelemInfo,
                                         @NonNull final BaseException outErr) {
        if (null != cliTelemInfo) {
            outErr.setSpeRing(cliTelemInfo.getSpeRing());
            outErr.setRefreshTokenAge(cliTelemInfo.getRefreshTokenAge());
            outErr.setCliTelemErrorCode(cliTelemInfo.getServerErrorCode());
            outErr.setCliTelemSubErrorCode(cliTelemInfo.getServerSubErrorCode());
        }
    }

    private static HttpResponse synthesizeHttpResponse(final int statusCode,
                                                       @Nullable String responseHeadersJson,
                                                       @Nullable String responseBody) {
        final String methodName = ":applyHttpErrorResponseData";

        if (null != responseHeadersJson && null != responseBody) {
            try {
                return new HttpResponse(
                        statusCode,
                        responseBody,
                        HeaderSerializationUtil.fromJson(responseHeadersJson)
                );
            } catch (JsonSyntaxException e) {
                Logger.warn(
                        TAG + methodName,
                        "Failed to deserialize error data: status, headers, response body."
                );
            }
        }

        return null;
    }

    public static BaseException baseExceptionFromException(final Throwable exception) {
        Throwable e = exception;
        if (exception instanceof ExecutionException){
            e = exception.getCause();
        }

        if (e instanceof TerminalException) {
            final String errorCode = ((TerminalException) e).getErrorCode();
            e = e.getCause();
            return new ClientException(
                    errorCode,
                    "An unhandled exception occurred with message: " + e.getMessage(),
                    e
            );
        }

        if (e instanceof IOException) {
            return new ClientException(
                    ClientException.IO_ERROR,
                    "An IO error occurred with message: " + e.getMessage(),
                    e
            );
        }

        if (e instanceof BaseException) {
            return (BaseException) e;
        }

        return new ClientException(
                ClientException.UNKNOWN_ERROR,
                e.getMessage(),
                e);
    }

    private static boolean isIntunePolicyRequiredError(
            @NonNull final TokenErrorResponse errorResponse) {

        return !TextUtils.isEmpty(errorResponse.getError()) &&
                !TextUtils.isEmpty(errorResponse.getSubError()) &&
                errorResponse.getError().equalsIgnoreCase(AuthenticationConstants.OAuth2ErrorCode.UNAUTHORIZED_CLIENT) &&
                errorResponse.getSubError().equalsIgnoreCase(AuthenticationConstants.OAuth2SubErrorCode.PROTECTION_POLICY_REQUIRED);
    }
}
