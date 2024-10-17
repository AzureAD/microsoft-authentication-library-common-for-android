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
package com.microsoft.identity.common.java.controllers;

import com.google.gson.JsonSyntaxException;
import com.microsoft.identity.common.java.WarningType;
import com.microsoft.identity.common.java.commands.parameters.BrokerInteractiveTokenCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.BrokerSilentTokenCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.CommandParameters;
import com.microsoft.identity.common.java.constants.OAuth2ErrorCode;
import com.microsoft.identity.common.java.constants.OAuth2SubErrorCode;
import com.microsoft.identity.common.java.exception.BaseException;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.exception.DeviceRegistrationRequiredException;
import com.microsoft.identity.common.java.exception.InsufficientDeviceRegistrationException;
import com.microsoft.identity.common.java.exception.IntuneAppProtectionPolicyRequiredException;
import com.microsoft.identity.common.java.exception.ServiceException;
import com.microsoft.identity.common.java.exception.StrongDeviceRegistrationRequiredException;
import com.microsoft.identity.common.java.exception.TerminalException;
import com.microsoft.identity.common.java.exception.UiRequiredException;
import com.microsoft.identity.common.java.exception.UserCancelException;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.net.HttpResponse;
import com.microsoft.identity.common.java.providers.microsoft.MicrosoftAuthorizationErrorResponse;
import com.microsoft.identity.common.java.providers.microsoft.MicrosoftTokenErrorResponse;
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationErrorResponse;
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationResult;
import com.microsoft.identity.common.java.providers.oauth2.TokenErrorResponse;
import com.microsoft.identity.common.java.providers.oauth2.TokenResult;
import com.microsoft.identity.common.java.result.AcquireTokenResult;
import com.microsoft.identity.common.java.telemetry.CliTelemInfo;
import com.microsoft.identity.common.java.util.HeaderSerializationUtil;
import com.microsoft.identity.common.java.util.StringUtil;

import org.json.JSONException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.NonNull;

public class ExceptionAdapter {

    private static final String TAG = ExceptionAdapter.class.getSimpleName();

    @Nullable
    public static BaseException exceptionFromAcquireTokenResult(final AcquireTokenResult result, final CommandParameters commandParameters) {
        final String methodName = ":exceptionFromAcquireTokenResult";

        @SuppressWarnings(WarningType.rawtype_warning)
        final AuthorizationResult authorizationResult = result.getAuthorizationResult();

        if (null != authorizationResult) {
            if (!authorizationResult.getSuccess()) {
                return exceptionFromAuthorizationResult(authorizationResult, commandParameters);
            }
        } else {
            Logger.warn(
                    TAG + methodName,
                    "AuthorizationResult was null -- expected for ATS cases."
            );
        }

        return exceptionFromTokenResult(result.getTokenResult(), commandParameters);
    }

    public static BaseException exceptionFromAuthorizationResult(@NonNull final AuthorizationResult authorizationResult, @Nullable final CommandParameters commandParameters) {
        final String methodTag = TAG + ":exceptionFromAuthorizationResult";
        final AuthorizationErrorResponse authorizationErrorResponse = authorizationResult.getAuthorizationErrorResponse();
        if (authorizationErrorResponse == null) {
            Logger.warn(
                    methodTag,
                    "AuthorizationErrorResponse is not set"
            );
            return new ClientException(ClientException.AUTHORIZATION_RESULT_NULL_ERROR_RESPONSE, "Authorization error response is null. Authorization Status: " +authorizationResult.getAuthorizationStatus());
        }

        //THERE ARE CURRENTLY NO USAGES of INVALID_REQUEST
        switch (authorizationResult.getAuthorizationStatus()) {
            case FAIL:
                // Check if the error is to register device and throw DEVICE_REGISTRATION_NEEDED exception
                if (authorizationErrorResponse instanceof MicrosoftAuthorizationErrorResponse) {
                    MicrosoftAuthorizationErrorResponse microsoftAuthorizationErrorResponse =
                            (MicrosoftAuthorizationErrorResponse) authorizationErrorResponse;

                    if (MicrosoftAuthorizationErrorResponse.DEVICE_REGISTRATION_NEEDED.equals(
                            microsoftAuthorizationErrorResponse.getError())) {

                        if (microsoftAuthorizationErrorResponse.isTokenProtectionRequired()) {
                            return new StrongDeviceRegistrationRequiredException(
                                    microsoftAuthorizationErrorResponse.getError(),
                                    microsoftAuthorizationErrorResponse.getErrorDescription(),
                                    microsoftAuthorizationErrorResponse.getUpnToWpj()
                            );
                        } else {
                            return new DeviceRegistrationRequiredException(
                                    microsoftAuthorizationErrorResponse.getError(),
                                    microsoftAuthorizationErrorResponse.getErrorDescription(),
                                    microsoftAuthorizationErrorResponse.getUpnToWpj()
                            );
                        }
                    } else if (MicrosoftAuthorizationErrorResponse.INSUFFICIENT_DEVICE_REGISTRATION.equals(
                            microsoftAuthorizationErrorResponse.getError())) {
                        return new InsufficientDeviceRegistrationException(
                                microsoftAuthorizationErrorResponse.getError(),
                                microsoftAuthorizationErrorResponse.getErrorDescription(),
                                microsoftAuthorizationErrorResponse.getUpnToWpj()
                        );
                    }
                }
                // Do i need to create a new exception i do not know
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
            default:
                Logger.warn(
                        methodTag,
                        "No AuthorizationResult status set"
                );
                return new ClientException(authorizationErrorResponse.getError(), authorizationErrorResponse.getErrorDescription());
        }
    }

    /**
     * Get an exception out of a TokenResult object.
     *
     * @param tokenResult
     * @return ServiceException, UiRequiredException
     */
    public static ServiceException exceptionFromTokenResult(final TokenResult tokenResult, final CommandParameters commandParameters) {
        final String methodName = ":exceptionFromTokenResult";

        ServiceException outErr;

        if (tokenResult != null &&
                !tokenResult.getSuccess() &&
                tokenResult.getErrorResponse() != null &&
                !StringUtil.isNullOrEmpty(tokenResult.getErrorResponse().getError())) {

            outErr = getExceptionFromTokenErrorResponse(commandParameters, tokenResult.getErrorResponse());
            applyCliTelemInfo(tokenResult.getCliTelemInfo(), outErr);
        } else {
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
     */
    @SuppressWarnings("deprecation")
    private static boolean shouldBeConvertedToUiRequiredException(final String oAuthError) {
        // Invalid_grant doesn't necessarily requires UI protocol-wise.
        // We simplify our logic because this layer is also used by MSAL.

        //Interaction required has been deprecated... hence suppressing warning.
        return OAuth2ErrorCode.INVALID_GRANT.equalsIgnoreCase(oAuthError) ||
                OAuth2ErrorCode.INTERACTION_REQUIRED.equalsIgnoreCase(oAuthError);

    }


    /**
     * Get an exception object from the given oAuth values.
     *
     * @param errorResponse
     * @return ServiceException, UiRequiredException
     */
    public static ServiceException getExceptionFromTokenErrorResponse(@NonNull final TokenErrorResponse errorResponse) {

        final ServiceException outErr;

        if (isNativeAuthenticationMFAError(errorResponse)) {
            ServiceException apiError = new ServiceException(
                    errorResponse.getError(),
                    errorResponse.getErrorDescription(),
                    null);

            String developerDescription = "Multi-factor authentication is required, which can't be fulfilled as part of this flow. Please sign out and perform a new sign in operation. Please see exception details for more information.";
            outErr = new ServiceException(
                    errorResponse.getError(),
                    developerDescription,
                    apiError
            );
        }
        else if (shouldBeConvertedToUiRequiredException(errorResponse.getError())) {
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

    /**
     * Converts any child of ServiceException (e.g. UiRequiredException) to generic ServiceException
     * Additional instructions are set to inform the developer about the next steps.
     *
     * @param exception the ServiceException to convert
     * @return ServiceException
     */
    public static ServiceException convertToNativeAuthException(@NonNull final ServiceException exception) {
        final ServiceException outErr;

        outErr = new ServiceException(
                exception.getErrorCode(),
                exception.getMessage(),
                exception.getHttpStatusCode(),
                exception
        );
        outErr.setOauthSubErrorCode(exception.getOAuthSubErrorCode());
        outErr.setHttpResponseHeaders(exception.getHttpResponseHeaders());
        outErr.setHttpResponseBody(exception.getHttpResponseBody());
        return outErr;
    }

    @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
    public static ServiceException getExceptionFromTokenErrorResponse(@Nullable final CommandParameters commandParameters,
                                                                      @NonNull final TokenErrorResponse errorResponse) {

        if (isIntunePolicyRequiredError(errorResponse)) {
            if (commandParameters == null || !(isBrokerTokenCommandParameters(commandParameters))) {
                Logger.warn(TAG, "In order to properly construct the IntuneAppProtectionPolicyRequiredException we need the command parameters to be supplied.  Returning as service exception instead.");
                return getExceptionFromTokenErrorResponse(errorResponse);
            }
            IntuneAppProtectionPolicyRequiredException policyRequiredException;
            if (commandParameters instanceof BrokerInteractiveTokenCommandParameters) {
                policyRequiredException = new IntuneAppProtectionPolicyRequiredException(
                        errorResponse.getError(),
                        errorResponse.getErrorDescription(),
                        (BrokerInteractiveTokenCommandParameters) commandParameters
                );
            } else {
                policyRequiredException = new IntuneAppProtectionPolicyRequiredException(
                        errorResponse.getError(),
                        errorResponse.getErrorDescription(),
                        (BrokerSilentTokenCommandParameters) commandParameters
                );
            }
            policyRequiredException.setOauthSubErrorCode(errorResponse.getSubError());
            setHttpResponseUsingTokenErrorResponse(policyRequiredException, errorResponse);

            return policyRequiredException;
        } else {
            return getExceptionFromTokenErrorResponse(errorResponse);
        }


    }

    /**
     * Name: setHttpResponseUsingTokenErrorResponse
     *
     * @param exception     ServiceException to which we will append an HttpResponse
     * @param errorResponse A TokenErrorResponse from which we will recontruct an HttpResponse
     */

    private static void setHttpResponseUsingTokenErrorResponse(@NonNull final ServiceException exception,
                                                               @NonNull final TokenErrorResponse errorResponse) {

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

    private static boolean isBrokerTokenCommandParameters(CommandParameters commandParameters) {
        return ((commandParameters instanceof BrokerSilentTokenCommandParameters) || (commandParameters instanceof BrokerInteractiveTokenCommandParameters));
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
        if (exception instanceof ExecutionException
                && exception.getCause() != null) {
            e = exception.getCause();
        }
        if (e instanceof BaseException) {
            return (BaseException) e;
        }

        return clientExceptionFromException(e);
    }

    @NonNull
    public static ClientException clientExceptionFromException(@NonNull final Throwable exception) {
        if (exception instanceof ClientException){
            return (ClientException) exception;
        }

        Throwable e = exception;
        if (exception instanceof ExecutionException
                && exception.getCause() != null) {
            e = exception.getCause();
        }

        if (e instanceof TerminalException) {
            final String errorCode = ((TerminalException) e).getErrorCode();
            final Throwable cause = exception.getCause();
            if (cause != null) {
                e = cause;
            }
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

        if (e instanceof InterruptedException) {
            return new ClientException(
                    ClientException.INTERRUPTED_OPERATION,
                    "SDK cancelled operation, the thread execution was interrupted",
                    e
            );
        }

        if (e instanceof TimeoutException) {
            return new ClientException(
                    ClientException.TIMED_OUT,
                    "A blocking operation has timed out: " + e.getMessage(),
                    e
            );
        }

        if (e instanceof NullPointerException) {
            return new ClientException(
                    ClientException.NULL_POINTER_ERROR,
                    e.getMessage(),
                    e
            );
        }


        if (e instanceof OutOfMemoryError) {
            return new ClientException(
                    ClientException.OUT_OF_MEMORY,
                    e.getMessage(),
                    e
            );
        }

        if (e instanceof GeneralSecurityException) {
            if (e instanceof CertificateException) {
                return new ClientException(
                        ClientException.CERTIFICATE_LOAD_FAILURE,
                        e.getMessage(),
                        e);
            } else if (e instanceof KeyStoreException) {
                return new ClientException(
                        ClientException.KEYSTORE_NOT_INITIALIZED,
                        e.getMessage(),
                        e);
            } else if (e instanceof NoSuchAlgorithmException) {
                return new ClientException(
                        ClientException.NO_SUCH_ALGORITHM,
                        e.getMessage(),
                        e);
            } else if (e instanceof InvalidAlgorithmParameterException) {
                return new ClientException(
                        ClientException.INVALID_ALG_PARAMETER,
                        e.getMessage(),
                        e);
            } else if (e instanceof UnrecoverableEntryException) {
                return new ClientException(
                        ClientException.INVALID_PROTECTION_PARAMS,
                        e.getMessage(),
                        e);
            } else if (e instanceof InvalidKeyException) {
                return new ClientException(
                        ClientException.INVALID_KEY,
                        e.getMessage(),
                        e);
            }
        }

        return new ClientException(
                ClientException.UNKNOWN_ERROR,
                e.getMessage(),
                e);
    }

    private static boolean isIntunePolicyRequiredError(
            @NonNull final TokenErrorResponse errorResponse) {

        return !StringUtil.isNullOrEmpty(errorResponse.getError()) &&
                !StringUtil.isNullOrEmpty(errorResponse.getSubError()) &&
                errorResponse.getError().equalsIgnoreCase(OAuth2ErrorCode.UNAUTHORIZED_CLIENT) &&
                errorResponse.getSubError().equalsIgnoreCase(OAuth2SubErrorCode.PROTECTION_POLICY_REQUIRED);
    }

    /**
     * Identifies whether an error is specific to native authentication MFA scenarios.
     * @param errorResponse
     * @return true if errorResponse is a native authentication MFA error
     */
    private static boolean isNativeAuthenticationMFAError(
            @NonNull final TokenErrorResponse errorResponse) {
        if (!(errorResponse instanceof MicrosoftTokenErrorResponse)) {
            return false;
        }

        MicrosoftTokenErrorResponse microsoftTokenErrorResponse = ((MicrosoftTokenErrorResponse) errorResponse);
        return microsoftTokenErrorResponse.getErrorCodes() != null &&
                !microsoftTokenErrorResponse.getErrorCodes().isEmpty() &&
                microsoftTokenErrorResponse.getErrorCodes().contains((long) 50076);
    }
}
