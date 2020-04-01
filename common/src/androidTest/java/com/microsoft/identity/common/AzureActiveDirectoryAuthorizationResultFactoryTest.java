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
package com.microsoft.identity.common;

import android.content.Intent;
import android.os.Bundle;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.exception.ErrorStrings;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftAuthorizationErrorResponse;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftAuthorizationResult;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectoryAuthorizationErrorResponse;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectoryAuthorizationRequest;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectoryAuthorizationResponse;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectoryAuthorizationResult;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectoryAuthorizationResultFactory;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationErrorResponse;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResult;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResultFactory;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationStatus;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;


@RunWith(AndroidJUnit4.class)
public class AzureActiveDirectoryAuthorizationResultFactoryTest {

    private static final String REDIRECT_URI = "aad-clientid://packagename/";
    private static final String AUTH_CODE_AND_STATE = "code=authorization_code&state=state";
    private static final String AUTH_CODE = "authorization_code";
    private static final String STATE = "state";
    private static final String ERROR_MESSAGE = "access_denied";
    private static final String ERROR_DESCRIPTION = "access denied error description";
    private static final String CORRELATION_ID = "correlationId";
    private static final String ERROR_CODES = "access_denied_error_code";

    private AuthorizationResultFactory<AzureActiveDirectoryAuthorizationResult, AzureActiveDirectoryAuthorizationRequest> mAuthorizationResultFactory;

    @Before
    public void setUp() {
        mAuthorizationResultFactory = new AzureActiveDirectoryAuthorizationResultFactory();
    }

    private AzureActiveDirectoryAuthorizationRequest getAADRequest() {
        return new AzureActiveDirectoryAuthorizationRequest.Builder().build();
    }

    @Test
    public void testBrowserCodeCancel() {
        Intent intent = new Intent();
        intent.putExtras(new Bundle());
        AuthorizationResult result = mAuthorizationResultFactory.createAuthorizationResult(
                AuthenticationConstants.UIResponse.BROWSER_CODE_CANCEL, intent, getAADRequest());
        assertNotNull(result);
        assertNull(result.getAuthorizationResponse());
        assertEquals(AuthorizationStatus.USER_CANCEL, result.getAuthorizationStatus());
        AuthorizationErrorResponse errorResponse = result.getAuthorizationErrorResponse();
        assertNotNull(errorResponse);
        assertEquals(MicrosoftAuthorizationErrorResponse.USER_CANCEL, errorResponse.getError());
        assertEquals(MicrosoftAuthorizationErrorResponse.USER_CANCELLED_FLOW, errorResponse.getErrorDescription());
    }

    @Test
    public void testBrowserCodeError() {
        Intent intent = new Intent();
        AuthorizationResult result = mAuthorizationResultFactory.createAuthorizationResult(
                AuthenticationConstants.UIResponse.BROWSER_CODE_ERROR, intent, getAADRequest());
        assertNotNull(result);
        assertNull(result.getAuthorizationResponse());
        assertEquals(AuthorizationStatus.FAIL, result.getAuthorizationStatus());
        assertNotNull(result.getAuthorizationErrorResponse());
    }

    @Test
    public void testBrowserCodeRequestResume() {
        Intent intent = new Intent();
        intent.putExtras(new Bundle());
        AuthorizationResult result = mAuthorizationResultFactory.createAuthorizationResult(
                AuthenticationConstants.UIResponse.BROKER_REQUEST_RESUME, intent, getAADRequest());
        assertNotNull(result);
        assertNull(result.getAuthorizationResponse());
        assertEquals(AuthorizationStatus.FAIL, result.getAuthorizationStatus());
        AuthorizationErrorResponse errorResponse = result.getAuthorizationErrorResponse();
        assertNotNull(errorResponse);
        assertEquals(MicrosoftAuthorizationErrorResponse.AUTHORIZATION_FAILED, errorResponse.getError());
        assertEquals(MicrosoftAuthorizationErrorResponse.BROKER_NEEDS_TO_BE_INSTALLED, errorResponse.getErrorDescription());
    }

    @Test
    public void testBrowserCodeAuthenticationException() {
        Intent intent = new Intent();
        Bundle bundle = new Bundle();
        String mockError = "mockError";
        String mockErrorDescription = "mockErrorDescription";
        ClientException exception = new ClientException(mockError, mockErrorDescription);
        bundle.putSerializable(AuthenticationConstants.Browser.RESPONSE_AUTHENTICATION_EXCEPTION, exception);
        intent.putExtras(bundle);
        AuthorizationResult result = mAuthorizationResultFactory.createAuthorizationResult(
                AuthenticationConstants.UIResponse.BROWSER_CODE_AUTHENTICATION_EXCEPTION, intent, getAADRequest());
        assertNotNull(result);
        assertNull(result.getAuthorizationResponse());
        assertEquals(AuthorizationStatus.FAIL, result.getAuthorizationStatus());
        AuthorizationErrorResponse errorResponse = result.getAuthorizationErrorResponse();
        assertNotNull(errorResponse);
        assertEquals(mockError, errorResponse.getError());
        assertEquals(mockErrorDescription, errorResponse.getErrorDescription());
    }

    @Test
    public void testNoMatchingResultCode() {
        Intent intent = new Intent();
        intent.putExtras(new Bundle());
        AuthorizationResult result = mAuthorizationResultFactory.createAuthorizationResult(0, intent, getAADRequest());
        assertNotNull(result);
        assertNull(result.getAuthorizationResponse());
        assertEquals(AuthorizationStatus.FAIL, result.getAuthorizationStatus());
        AuthorizationErrorResponse errorResponse = result.getAuthorizationErrorResponse();
        assertNotNull(errorResponse);
        assertEquals(MicrosoftAuthorizationErrorResponse.UNKNOWN_ERROR, errorResponse.getError());
        assertNotNull(errorResponse.getErrorDescription());
    }

    @Test
    public void testNullIntent() {
        AuthorizationResult result = mAuthorizationResultFactory.createAuthorizationResult(
                AuthenticationConstants.UIResponse.BROWSER_CODE_COMPLETE, null, getAADRequest());
        assertNotNull(result);
        assertNull(result.getAuthorizationResponse());
        assertEquals(AuthorizationStatus.FAIL, result.getAuthorizationStatus());
        AuthorizationErrorResponse errorResponse = result.getAuthorizationErrorResponse();
        assertNotNull(errorResponse);
        assertEquals(MicrosoftAuthorizationErrorResponse.AUTHORIZATION_FAILED, errorResponse.getError());
        assertEquals(MicrosoftAuthorizationErrorResponse.NULL_INTENT, errorResponse.getErrorDescription());
    }

    @Test
    public void testNullBundle() {
        Intent intent = new Intent();
        AuthorizationResult result = mAuthorizationResultFactory.createAuthorizationResult(
                AuthenticationConstants.UIResponse.BROWSER_CODE_COMPLETE, intent, getAADRequest());
        assertNotNull(result);
        assertNull(result.getAuthorizationResponse());
        assertEquals(AuthorizationStatus.FAIL, result.getAuthorizationStatus());
        AuthorizationErrorResponse errorResponse = result.getAuthorizationErrorResponse();
        assertNotNull(errorResponse);
        assertEquals(MicrosoftAuthorizationErrorResponse.AUTHORIZATION_FAILED, errorResponse.getError());
        assertEquals(MicrosoftAuthorizationErrorResponse.NULL_INTENT, errorResponse.getErrorDescription());
    }

    @Test
    public void testNullUrl() {
        Intent intent = new Intent();
        intent.putExtras(new Bundle());
        AuthorizationResult result = mAuthorizationResultFactory.createAuthorizationResult(
                AuthenticationConstants.UIResponse.BROWSER_CODE_COMPLETE, intent, getAADRequest());
        assertNotNull(result);
        assertNull(result.getAuthorizationResponse());
        assertEquals(AuthorizationStatus.FAIL, result.getAuthorizationStatus());
        AuthorizationErrorResponse errorResponse = result.getAuthorizationErrorResponse();
        assertNotNull(errorResponse);
        assertEquals(MicrosoftAuthorizationErrorResponse.AUTHORIZATION_FAILED, errorResponse.getError());
        assertEquals(MicrosoftAuthorizationErrorResponse.AUTHORIZATION_SERVER_INVALID_RESPONSE, errorResponse.getErrorDescription());
    }

    @Test
    public void testUrlWithEmptyParams() {
        Intent intent = new Intent();
        Bundle bundle = new Bundle();
        bundle.putString(AuthenticationConstants.Browser.RESPONSE_FINAL_URL, REDIRECT_URI);
        intent.putExtras(bundle);
        AuthorizationResult result = mAuthorizationResultFactory.createAuthorizationResult(
                AuthenticationConstants.UIResponse.BROWSER_CODE_COMPLETE, intent, getAADRequest());
        assertNotNull(result);
        assertNull(result.getAuthorizationResponse());
        assertEquals(AuthorizationStatus.FAIL, result.getAuthorizationStatus());
        AuthorizationErrorResponse errorResponse = result.getAuthorizationErrorResponse();
        assertNotNull(errorResponse);
        assertEquals(MicrosoftAuthorizationErrorResponse.AUTHORIZATION_FAILED, errorResponse.getError());
        assertEquals(MicrosoftAuthorizationErrorResponse.AUTHORIZATION_SERVER_INVALID_RESPONSE, errorResponse.getErrorDescription());
    }

    @Test
    public void testUrlWithInvalidParams() {
        Intent intent = new Intent();
        Bundle bundle = new Bundle();
        bundle.putString(AuthenticationConstants.Browser.RESPONSE_FINAL_URL, REDIRECT_URI + "?some_random_error=accessdenied");
        intent.putExtras(bundle);
        AuthorizationResult result = mAuthorizationResultFactory.createAuthorizationResult(
                AuthenticationConstants.UIResponse.BROWSER_CODE_COMPLETE, intent, getAADRequest());
        assertNotNull(result);
        assertNull(result.getAuthorizationResponse());
        assertEquals(AuthorizationStatus.FAIL, result.getAuthorizationStatus());
        AuthorizationErrorResponse errorResponse = result.getAuthorizationErrorResponse();
        assertNotNull(errorResponse);
        assertEquals(MicrosoftAuthorizationErrorResponse.AUTHORIZATION_FAILED, errorResponse.getError());
        assertEquals(MicrosoftAuthorizationErrorResponse.AUTHORIZATION_SERVER_INVALID_RESPONSE, errorResponse.getErrorDescription());
    }

    @Test
    public void testUrlWithCorrectCodeAndState() {
        Intent intent = new Intent();
        Bundle bundle = new Bundle();
        String responseUrl = REDIRECT_URI + "?" + AUTH_CODE_AND_STATE + "&"
                + AuthenticationConstants.AAD.CORRELATION_ID + "=" + CORRELATION_ID;
        bundle.putString(AuthenticationConstants.Browser.RESPONSE_FINAL_URL, responseUrl);
        bundle.putString(AuthenticationConstants.AAD.CORRELATION_ID, CORRELATION_ID);
        intent.putExtras(bundle);
        intent.putExtra(MicrosoftAuthorizationResult.REQUEST_STATE_PARAMETER, STATE);
        AzureActiveDirectoryAuthorizationResult result = mAuthorizationResultFactory.createAuthorizationResult(
                AuthenticationConstants.UIResponse.BROWSER_CODE_COMPLETE, intent, getAADRequest());
        assertNotNull(result);
        assertNull(result.getAuthorizationErrorResponse());
        assertEquals(AuthorizationStatus.SUCCESS, result.getAuthorizationStatus());
        AzureActiveDirectoryAuthorizationResponse response = result.getAuthorizationResponse();
        assertNotNull(response);
        assertEquals(AUTH_CODE, response.getCode());
        assertEquals(STATE, response.getState());
        assertEquals(CORRELATION_ID, response.getCorrelationId());
    }

    public void testUrlWithIncorrectState() {
        Intent intent = new Intent();
        Bundle bundle = new Bundle();
        String responseUrl = REDIRECT_URI + "?" + AUTH_CODE_AND_STATE + "&"
                + AuthenticationConstants.AAD.CORRELATION_ID + "=" + CORRELATION_ID;
        bundle.putString(AuthenticationConstants.Browser.RESPONSE_FINAL_URL, responseUrl);
        bundle.putString(AuthenticationConstants.AAD.CORRELATION_ID, CORRELATION_ID);
        intent.putExtras(bundle);
        intent.putExtra(MicrosoftAuthorizationResult.REQUEST_STATE_PARAMETER, STATE);
        AuthorizationResult result = mAuthorizationResultFactory.createAuthorizationResult(
                AuthenticationConstants.UIResponse.BROWSER_CODE_COMPLETE, intent, getAADRequest());
        assertNotNull(result);
        assertNull(result.getAuthorizationResponse());
        assertEquals(AuthorizationStatus.FAIL, result.getAuthorizationStatus());
        AuthorizationErrorResponse errorResponse = result.getAuthorizationErrorResponse();
        assertNotNull(errorResponse);
        assertEquals(ErrorStrings.STATE_MISMATCH, errorResponse.getError());
        assertEquals(MicrosoftAuthorizationErrorResponse.STATE_NOT_THE_SAME, errorResponse.getErrorDescription());
    }

    @Test
    public void testUrlWithError() {
        Intent intent = new Intent();
        Bundle bundle = new Bundle();
        String responseUrl = REDIRECT_URI + "?error=" + ERROR_MESSAGE + "&error_description="
                + ERROR_DESCRIPTION + "&error_codes=" + ERROR_CODES;
        bundle.putString(AuthenticationConstants.Browser.RESPONSE_FINAL_URL, responseUrl);
        intent.putExtras(bundle);
        AzureActiveDirectoryAuthorizationResult result = mAuthorizationResultFactory.createAuthorizationResult(
                AuthenticationConstants.UIResponse.BROWSER_CODE_COMPLETE, intent, getAADRequest());
        assertNotNull(result);
        assertNull(result.getAuthorizationResponse());
        assertEquals(AuthorizationStatus.FAIL, result.getAuthorizationStatus());
        AzureActiveDirectoryAuthorizationErrorResponse errorResponse = result.getAuthorizationErrorResponse();
        assertNotNull(errorResponse);
        assertEquals(ERROR_MESSAGE, errorResponse.getError());
        assertEquals(ERROR_DESCRIPTION, errorResponse.getErrorDescription());
        assertEquals(ERROR_CODES, errorResponse.getErrorCodes());
    }


}
