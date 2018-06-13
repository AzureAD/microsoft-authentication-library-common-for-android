package com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory;

import android.content.Intent;
import android.os.Bundle;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.exception.ErrorStrings;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftAuthorizationErrorResponse;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationErrorResponse;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResult;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResultFactory;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationStatus;
import com.microsoft.identity.common.internal.util.StringUtil;

import java.io.Serializable;
import java.util.HashMap;

public class AzureActiveDirectoryAuthorizationResultFactory extends AuthorizationResultFactory {

    private static final String TAG = AzureActiveDirectoryAuthorizationResultFactory.class.getSimpleName();

    private static final String ERROR_CODES = "error_codes";

    @Override
    public AuthorizationResult createAuthorizationResult(final int resultCode, final Intent data) {
        if (data == null || data.getExtras() == null) {
            return createAuthorizationResultWithErrorResponse(AuthorizationStatus.FAIL,
                    MicrosoftAuthorizationErrorResponse.AUTHORIZATION_FAILED,
                    MicrosoftAuthorizationErrorResponse.NULL_INTENT);
        }
        final Bundle extras = data.getExtras();
        final int requestId = extras.getInt(AuthenticationConstants.Browser.REQUEST_ID);
        switch (resultCode) {
            case AuthenticationConstants.UIResponse.BROWSER_CODE_CANCEL:
                Logger.verbose(TAG, "User cancel the request in webview: " + requestId);
                return createAuthorizationResultWithErrorResponse(AuthorizationStatus.USER_CANCEL,
                        MicrosoftAuthorizationErrorResponse.USER_CANCEL,
                        MicrosoftAuthorizationErrorResponse.USER_CANCELLED_FLOW);

            case AuthenticationConstants.UIResponse.BROWSER_CODE_COMPLETE:
                final String url = extras.getString(AuthenticationConstants.Browser.RESPONSE_FINAL_URL, "");
                return parseUrlAndCreateAuthorizationResult(url);

            case AuthenticationConstants.UIResponse.BROWSER_CODE_ERROR:
                // This is purely client side error, possible return could be chrome_not_installed or the request intent is
                // not resolvable
                final String error = extras.getString(AuthenticationConstants.Browser.RESPONSE_ERROR_CODE);
                final String errorDescription = extras.getString(AuthenticationConstants.Browser.RESPONSE_ERROR_MESSAGE);
                return createAuthorizationResultWithErrorResponse(AuthorizationStatus.FAIL, error, errorDescription);

            case AuthenticationConstants.UIResponse.BROWSER_CODE_AUTHENTICATION_EXCEPTION:
                //TODO : Verify that a ClientException is serialized here after Broker Implementation
                Serializable clientException = extras.getSerializable(AuthenticationConstants.Browser.RESPONSE_AUTHENTICATION_EXCEPTION);
                if (clientException != null && clientException instanceof ClientException) {
                    ClientException exception = (ClientException) clientException;
                    return createAuthorizationResultWithErrorResponse(AuthorizationStatus.FAIL,
                            exception.getErrorCode(), exception.getMessage());
                }

            case AuthenticationConstants.UIResponse.BROKER_REQUEST_RESUME:
                Logger.verbose(TAG, "Device needs to have broker installed, we expect the apps to call us"
                        + "back when the broker is installed");
                return createAuthorizationResultWithErrorResponse(AuthorizationStatus.FAIL,
                        MicrosoftAuthorizationErrorResponse.AUTHORIZATION_FAILED,
                        MicrosoftAuthorizationErrorResponse.BROKER_NEEDS_TO_BE_INSTALLED);
        }
        return createAuthorizationResultWithErrorResponse(AuthorizationStatus.FAIL,
                MicrosoftAuthorizationErrorResponse.UNKNOWN_ERROR,
                MicrosoftAuthorizationErrorResponse.UNKNOWN_RESULT_CODE + "[" + resultCode + "]");
    }

    private AzureActiveDirectoryAuthorizationResult createAuthorizationResultWithErrorResponse(final AuthorizationStatus authStatus,
                                                                                               final String error,
                                                                                               final String errorDescription) {
        AuthorizationErrorResponse errorResponse = new AzureActiveDirectoryAuthorizationErrorResponse(error, errorDescription);
        return new AzureActiveDirectoryAuthorizationResult(authStatus, errorResponse);
    }

    private AzureActiveDirectoryAuthorizationResult createAuthorizationResultWithErrorResponse(final AuthorizationStatus authStatus,
                                                                                               final String error,
                                                                                               final String errorDescription,
                                                                                               final String errorCodes,
                                                                                               final String correlationId) {
        Logger.info(TAG, correlationId, "Error is returned from webview redirect");
        Logger.infoPII(TAG, correlationId, "error: " + error + " errorDescription: " + errorDescription);
        AzureActiveDirectoryAuthorizationErrorResponse errorResponse =
                new AzureActiveDirectoryAuthorizationErrorResponse(error, errorDescription);
        errorResponse.setErrorCodes(errorCodes);
        return new AzureActiveDirectoryAuthorizationResult(authStatus, errorResponse);
    }

    private AzureActiveDirectoryAuthorizationResult parseUrlAndCreateAuthorizationResult(final String url) {
        HashMap<String, String> urlParameters = StringExtensions.getUrlParameters(url);
        if (urlParameters == null || urlParameters.isEmpty()) {
            Logger.warn(TAG, null, "Invalid server response, empty query string from the webview redirect.");
            return createAuthorizationResultWithErrorResponse(AuthorizationStatus.FAIL,
                    MicrosoftAuthorizationErrorResponse.AUTHORIZATION_FAILED,
                    MicrosoftAuthorizationErrorResponse.AUTHORIZATION_SERVER_INVALID_RESPONSE);
        }
        String correlationInResponse = urlParameters.get(AuthenticationConstants.AAD.CORRELATION_ID);
        if (urlParameters.containsKey(CODE)) {
            return validateAndCreateAuthorizationResult(urlParameters.get(CODE), urlParameters.get(STATE), correlationInResponse);
        } else if (urlParameters.containsKey(ERROR)) {
            return createAuthorizationResultWithErrorResponse(AuthorizationStatus.FAIL, urlParameters.get(ERROR),
                    urlParameters.get(ERROR_DESCRIPTION), urlParameters.get(ERROR_CODES), correlationInResponse);
        } else {
            return createAuthorizationResultWithErrorResponse(AuthorizationStatus.FAIL,
                    MicrosoftAuthorizationErrorResponse.AUTHORIZATION_FAILED,
                    MicrosoftAuthorizationErrorResponse.AUTHORIZATION_SERVER_INVALID_RESPONSE);
        }
    }

    private AzureActiveDirectoryAuthorizationResult validateAndCreateAuthorizationResult(final String code,
                                                                                         final String state,
                                                                                         final String correlationId) {
        if (StringUtil.isEmpty(state)) {
            Logger.warn(TAG, correlationId, "State parameter is not returned from the webview redirect.");
            return createAuthorizationResultWithErrorResponse(AuthorizationStatus.FAIL,
                    ErrorStrings.STATE_MISMATCH, MicrosoftAuthorizationErrorResponse.STATE_NOT_RETURNED);
        } else {
            //TODO : validate state that it matches the one from request
            Logger.info(TAG, correlationId, "Auth code is successfully returned from webview redirect.");
            AzureActiveDirectoryAuthorizationResponse response = new AzureActiveDirectoryAuthorizationResponse(code, state);
            response.setCorrelationId(correlationId);
            return new AzureActiveDirectoryAuthorizationResult(AuthorizationStatus.SUCCESS, response);
        }
    }

}