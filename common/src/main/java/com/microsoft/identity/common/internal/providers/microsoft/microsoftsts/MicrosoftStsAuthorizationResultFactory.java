package com.microsoft.identity.common.internal.providers.microsoft.microsoftsts;

import android.content.Intent;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.exception.ErrorStrings;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftAuthorizationErrorResponse;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationErrorResponse;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResult;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResultFactory;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationStatus;
import com.microsoft.identity.common.internal.util.StringUtil;

import java.util.HashMap;

public class MicrosoftStsAuthorizationResultFactory extends AuthorizationResultFactory {

    private static final String TAG = MicrosoftStsAuthorizationResultFactory.class.getSimpleName();

    public static final String MSSTS_AUTHORIZATION_FINAL_URL = "com.microsoft.identity.client.finalUrl";

    @Override
    public AuthorizationResult createAuthorizationResult(final int resultCode, final Intent data) {
        if (data == null) {
            return createAuthorizationResultWithErrorResponse(AuthorizationStatus.FAIL,
                    MicrosoftAuthorizationErrorResponse.AUTHORIZATION_FAILED, MicrosoftAuthorizationErrorResponse.NULL_INTENT);
        }
        switch (resultCode) {
            case AuthenticationConstants.UIResponse.BROWSER_CODE_CANCEL:
                Logger.verbose(TAG, null, "User cancel the request in webview.");
                return createAuthorizationResultWithErrorResponse(AuthorizationStatus.USER_CANCEL,
                        MicrosoftAuthorizationErrorResponse.USER_CANCEL, MicrosoftAuthorizationErrorResponse.USER_CANCELLED_FLOW);

            case AuthenticationConstants.UIResponse.BROWSER_CODE_COMPLETE:
                final String url = data.getStringExtra(MSSTS_AUTHORIZATION_FINAL_URL);
                return parseUrlAndCreateAuthorizationResponse(url);

            case AuthenticationConstants.UIResponse.BROWSER_CODE_ERROR:
                // This is purely client side error, possible return could be chrome_not_installed or the request intent is
                // not resolvable
                final String error = data.getStringExtra(ERROR_CODE);
                final String errorDescription = data.getStringExtra(ERROR_DESCRIPTION);
                return createAuthorizationResultWithErrorResponse(AuthorizationStatus.FAIL, error, errorDescription);

            default:
                return createAuthorizationResultWithErrorResponse(AuthorizationStatus.FAIL,
                        MicrosoftAuthorizationErrorResponse.UNKNOWN_ERROR, MicrosoftAuthorizationErrorResponse.UNKNOWN_RESULT_CODE);
        }
    }

    private MicrosoftStsAuthorizationResult createAuthorizationResultWithErrorResponse(final AuthorizationStatus authStatus,
                                                                                       final String error,
                                                                                       final String errorDescription) {
        Logger.info(TAG, null, "Error is returned from webview redirect");
        Logger.infoPII(TAG, null, "error: " + error + " errorDescription: " + errorDescription);
        AuthorizationErrorResponse errorResponse = new MicrosoftStsAuthorizationErrorResponse(error, errorDescription);
        return new MicrosoftStsAuthorizationResult(authStatus, errorResponse);
    }

    private MicrosoftStsAuthorizationResult parseUrlAndCreateAuthorizationResponse(final String url) {
        HashMap<String, String> urlParameters = StringUtil.isEmpty(url) ? null : StringExtensions.getUrlParameters(url);
        if (urlParameters == null || urlParameters.isEmpty()) {
            Logger.warn(TAG, null, "Invalid server response, empty query string from the webview redirect.");
            return createAuthorizationResultWithErrorResponse(AuthorizationStatus.FAIL,
                    MicrosoftAuthorizationErrorResponse.AUTHORIZATION_FAILED,
                    MicrosoftAuthorizationErrorResponse.AUTHORIZATION_SERVER_INVALID_RESPONSE);
        } else if (urlParameters.containsKey(CODE)) {
            return validateAndCreateAuthorizationResult(urlParameters.get(CODE), urlParameters.get(STATE));
        } else if (urlParameters.containsKey(ERROR)) {
            return createAuthorizationResultWithErrorResponse(AuthorizationStatus.FAIL,
                    urlParameters.get(ERROR), urlParameters.get(ERROR_DESCRIPTION));
        }
        return createAuthorizationResultWithErrorResponse(AuthorizationStatus.FAIL,
                MicrosoftAuthorizationErrorResponse.AUTHORIZATION_FAILED,
                MicrosoftAuthorizationErrorResponse.AUTHORIZATION_SERVER_INVALID_RESPONSE);

    }

    private MicrosoftStsAuthorizationResult validateAndCreateAuthorizationResult(final String code, final String state) {
        if (StringUtil.isEmpty(state)) {
            Logger.warn(TAG, "State parameter is not returned from the webview redirect.");
            return createAuthorizationResultWithErrorResponse(AuthorizationStatus.FAIL,
                    ErrorStrings.STATE_MISMATCH,
                    MicrosoftAuthorizationErrorResponse.STATE_NOT_RETURNED);
        } else {
            //TODO : validate state
            Logger.info(TAG, "Auth code is successfully returned from webview redirect.");
            MicrosoftStsAuthorizationResponse authResponse = new MicrosoftStsAuthorizationResponse(code, state);
            return new MicrosoftStsAuthorizationResult(AuthorizationStatus.SUCCESS, authResponse);
        }
    }

}