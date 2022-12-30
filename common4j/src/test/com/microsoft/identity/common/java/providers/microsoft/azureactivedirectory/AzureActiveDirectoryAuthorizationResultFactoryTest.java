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
package com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory;

import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.exception.ErrorStrings;
import com.microsoft.identity.common.java.providers.RawAuthorizationResult;
import com.microsoft.identity.common.java.providers.microsoft.MicrosoftAuthorizationErrorResponse;
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationErrorResponse;
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationResult;
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationResultFactory;
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationStatus;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import lombok.NonNull;

import static com.microsoft.identity.common.java.AuthenticationConstants.AAD.CORRELATION_ID;
import static com.microsoft.identity.common.java.providers.Constants.BROKER_INSTALLATION_REQUIRED_WEBVIEW_REDIRECT_URI;
import static com.microsoft.identity.common.java.providers.Constants.MOCK_AUTH_CODE;
import static com.microsoft.identity.common.java.providers.Constants.MOCK_AUTH_CODE_AND_STATE;
import static com.microsoft.identity.common.java.providers.Constants.MOCK_CORRELATION_ID;
import static com.microsoft.identity.common.java.providers.Constants.MOCK_ERROR_CODE;
import static com.microsoft.identity.common.java.providers.Constants.MOCK_ERROR_DESCRIPTION;
import static com.microsoft.identity.common.java.providers.Constants.MOCK_ERROR_MESSAGE;
import static com.microsoft.identity.common.java.providers.Constants.MOCK_REDIRECT_URI;
import static com.microsoft.identity.common.java.providers.Constants.MOCK_STATE;
import static com.microsoft.identity.common.java.providers.Constants.MOCK_STATE_ENCODED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(JUnit4.class)
public class AzureActiveDirectoryAuthorizationResultFactoryTest {

    private AuthorizationResultFactory<AzureActiveDirectoryAuthorizationResult, AzureActiveDirectoryAuthorizationRequest> mAuthorizationResultFactory;

    @Before
    public void setUp() {
        mAuthorizationResultFactory = new AzureActiveDirectoryAuthorizationResultFactory();
    }

    private AzureActiveDirectoryAuthorizationRequest getAADRequest() {
        return new AzureActiveDirectoryAuthorizationRequest.Builder().setState(MOCK_STATE).build();
    }

    private AzureActiveDirectoryAuthorizationRequest getAADRequestWithState(@NonNull final String state) {
        return new AzureActiveDirectoryAuthorizationRequest.Builder().setState(state).build();
    }

    @Test
    public void testBrowserCodeCancel() {
        AuthorizationResult result = mAuthorizationResultFactory.createAuthorizationResult(
                RawAuthorizationResult.fromResultCode(RawAuthorizationResult.ResultCode.CANCELLED), getAADRequest());
        assertNotNull(result);
        assertNull(result.getAuthorizationResponse());
        assertEquals(AuthorizationStatus.USER_CANCEL, result.getAuthorizationStatus());
        AuthorizationErrorResponse errorResponse = result.getAuthorizationErrorResponse();
        assertNotNull(errorResponse);
        assertEquals(MicrosoftAuthorizationErrorResponse.USER_CANCEL, errorResponse.getError());
        assertEquals(MicrosoftAuthorizationErrorResponse.USER_CANCELLED_FLOW, errorResponse.getErrorDescription());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBrowserCodeError() {
        mAuthorizationResultFactory.createAuthorizationResult(
                RawAuthorizationResult.fromResultCode(RawAuthorizationResult.ResultCode.NON_OAUTH_ERROR), getAADRequest());
        Assert.fail();
    }

    @Test
    public void testBrowserCodeRequestResume() {
        AuthorizationResult result = mAuthorizationResultFactory.createAuthorizationResult(
                RawAuthorizationResult.fromRedirectUri(BROKER_INSTALLATION_REQUIRED_WEBVIEW_REDIRECT_URI), getAADRequest());
        assertNotNull(result);
        assertNull(result.getAuthorizationResponse());
        assertEquals(AuthorizationStatus.FAIL, result.getAuthorizationStatus());
        AuthorizationErrorResponse errorResponse = result.getAuthorizationErrorResponse();
        assertNotNull(errorResponse);
        assertEquals(MicrosoftAuthorizationErrorResponse.BROKER_NEEDS_TO_BE_INSTALLED, errorResponse.getError());
        assertEquals(MicrosoftAuthorizationErrorResponse.BROKER_NEEDS_TO_BE_INSTALLED_ERROR_DESCRIPTION, errorResponse.getErrorDescription());
    }

    @Test
    public void testBrowserCodeWithException() {
        String mockError = "mockError";
        String mockErrorDescription = "mockErrorDescription";
        ClientException exception = new ClientException(mockError, mockErrorDescription);
        AuthorizationResult result = mAuthorizationResultFactory.createAuthorizationResult(
                RawAuthorizationResult.fromException(exception), getAADRequest());
        assertNotNull(result);
        assertNull(result.getAuthorizationResponse());
        assertEquals(AuthorizationStatus.FAIL, result.getAuthorizationStatus());
        AuthorizationErrorResponse errorResponse = result.getAuthorizationErrorResponse();
        assertNotNull(errorResponse);
        assertEquals(mockError, errorResponse.getError());
        assertEquals(mockErrorDescription, errorResponse.getErrorDescription());
    }

    @Test
    public void testUnknownResultCode() {
        AuthorizationResult result = mAuthorizationResultFactory.createAuthorizationResult(
                RawAuthorizationResult.fromResultCode(RawAuthorizationResult.ResultCode.UNKNOWN), getAADRequest());
        assertNotNull(result);
        assertNull(result.getAuthorizationResponse());
        assertEquals(AuthorizationStatus.FAIL, result.getAuthorizationStatus());
        AuthorizationErrorResponse errorResponse = result.getAuthorizationErrorResponse();
        assertNotNull(errorResponse);
        assertEquals(MicrosoftAuthorizationErrorResponse.UNKNOWN_ERROR, errorResponse.getError());
        assertNotNull(errorResponse.getErrorDescription());
    }

    @Test
    public void testEmptyUrl() {
        AuthorizationResult result = mAuthorizationResultFactory.createAuthorizationResult(
                RawAuthorizationResult.fromRedirectUri(""), getAADRequest());
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
        AuthorizationResult result = mAuthorizationResultFactory.createAuthorizationResult(
                RawAuthorizationResult.fromRedirectUri(MOCK_REDIRECT_URI), getAADRequest());
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
        AuthorizationResult result = mAuthorizationResultFactory.createAuthorizationResult(
                RawAuthorizationResult.fromRedirectUri(MOCK_REDIRECT_URI + "?some_random_error=accessdenied"), getAADRequest());
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
        String responseUrl = MOCK_REDIRECT_URI + "?" + MOCK_AUTH_CODE_AND_STATE + "&"
                + CORRELATION_ID + "=" + MOCK_CORRELATION_ID;
        AzureActiveDirectoryAuthorizationResult result = mAuthorizationResultFactory.createAuthorizationResult(
                RawAuthorizationResult.fromRedirectUri(responseUrl), getAADRequest());
        assertNotNull(result);
        assertNull(result.getAuthorizationErrorResponse());
        Assert.assertEquals(AuthorizationStatus.SUCCESS, result.getAuthorizationStatus());
        AzureActiveDirectoryAuthorizationResponse response = result.getAuthorizationResponse();
        assertNotNull(response);
        Assert.assertEquals(MOCK_AUTH_CODE, response.getCode());
        Assert.assertEquals(MOCK_STATE_ENCODED, response.getState());
        Assert.assertEquals(MOCK_CORRELATION_ID, response.getCorrelationId());
    }

    @Test
    public void testUrlWithIncorrectState() {
        String responseUrl = MOCK_REDIRECT_URI + "?" + MOCK_AUTH_CODE_AND_STATE + "&"
                + CORRELATION_ID + "=" + MOCK_CORRELATION_ID;
        AuthorizationResult result = mAuthorizationResultFactory.createAuthorizationResult(
                RawAuthorizationResult.fromRedirectUri(responseUrl), getAADRequestWithState("some_random_state"));
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
        String responseUrl = MOCK_REDIRECT_URI + "?error=" + MOCK_ERROR_MESSAGE + "&error_description="
                + MOCK_ERROR_DESCRIPTION + "&error_codes=" + MOCK_ERROR_CODE;
        AzureActiveDirectoryAuthorizationResult result = mAuthorizationResultFactory.createAuthorizationResult(
                RawAuthorizationResult.fromRedirectUri(responseUrl), getAADRequest());
        assertNotNull(result);
        assertNull(result.getAuthorizationResponse());
        Assert.assertEquals(AuthorizationStatus.FAIL, result.getAuthorizationStatus());
        AzureActiveDirectoryAuthorizationErrorResponse errorResponse = result.getAuthorizationErrorResponse();
        assertNotNull(errorResponse);
        Assert.assertEquals(MOCK_ERROR_MESSAGE, errorResponse.getError());
        Assert.assertEquals(MOCK_ERROR_DESCRIPTION, errorResponse.getErrorDescription());
        Assert.assertEquals(MOCK_ERROR_CODE, errorResponse.getErrorCodes());
    }
}
