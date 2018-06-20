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
import android.support.test.runner.AndroidJUnit4;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.exception.ErrorStrings;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftAuthorizationErrorResponse;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftAuthorizationResult;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationResult;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationResultFactory;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationErrorResponse;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResponse;
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
public class MicrosoftStsAuthorizationResultFactoryTest {

    private static final String REDIRECT_URI = "msauth-clientid://packagename/";
    private static final String AUTH_CODE_AND_STATE = "code=authorization_code&state=state";
    private static final String MOCK_AUTH_CODE = "authorization_code";
    private static final String MOCK_STATE = "state";
    private static final String ERROR_MESSAGE = "access_denied";
    private static final String ERROR_DESCRIPTION = "access denied error description";

    private AuthorizationResultFactory<MicrosoftStsAuthorizationResult> mAuthorizationResultFactory;

    @Before
    public void setUp(){
        mAuthorizationResultFactory = new MicrosoftStsAuthorizationResultFactory();
    }

    @Test
    public void testBrowserCodeCancel() {
        Intent intent = new Intent();
        AuthorizationResult result = mAuthorizationResultFactory.createAuthorizationResult(
                AuthenticationConstants.UIResponse.BROWSER_CODE_CANCEL, intent);
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
                AuthenticationConstants.UIResponse.BROWSER_CODE_ERROR, intent);
        assertNotNull(result);
        assertNull(result.getAuthorizationResponse());
        assertEquals(AuthorizationStatus.FAIL, result.getAuthorizationStatus());
        assertNotNull(result.getAuthorizationErrorResponse());
    }

    @Test
    public void testNoMatchingResultCode() {
        Intent intent = new Intent();
        AuthorizationResult result = mAuthorizationResultFactory.createAuthorizationResult(0, intent);
        assertNotNull(result);
        assertNull(result.getAuthorizationResponse());
        assertEquals(AuthorizationStatus.FAIL, result.getAuthorizationStatus());
        AuthorizationErrorResponse errorResponse = result.getAuthorizationErrorResponse();
        assertNotNull(errorResponse);
        assertEquals(MicrosoftAuthorizationErrorResponse.UNKNOWN_ERROR, errorResponse.getError());
        assertEquals(MicrosoftAuthorizationErrorResponse.UNKNOWN_RESULT_CODE, errorResponse.getErrorDescription());
    }

    @Test
    public void testNullIntent() {
        AuthorizationResult result = mAuthorizationResultFactory.createAuthorizationResult(
                AuthenticationConstants.UIResponse.BROWSER_CODE_COMPLETE, null);
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
        AuthorizationResult result = mAuthorizationResultFactory.createAuthorizationResult(
                AuthenticationConstants.UIResponse.BROWSER_CODE_COMPLETE, intent);
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
        intent.putExtra(MicrosoftStsAuthorizationResultFactory.MSSTS_AUTHORIZATION_FINAL_URL, REDIRECT_URI);
        AuthorizationResult result = mAuthorizationResultFactory.createAuthorizationResult(
                AuthenticationConstants.UIResponse.BROWSER_CODE_COMPLETE, intent);
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
        intent.putExtra(MicrosoftStsAuthorizationResultFactory.MSSTS_AUTHORIZATION_FINAL_URL, REDIRECT_URI + "?some_random_error=accessdenied");
        AuthorizationResult result = mAuthorizationResultFactory.createAuthorizationResult(
                AuthenticationConstants.UIResponse.BROWSER_CODE_COMPLETE, intent);
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
        intent.putExtra(MicrosoftStsAuthorizationResultFactory.MSSTS_AUTHORIZATION_FINAL_URL, REDIRECT_URI + "?" + AUTH_CODE_AND_STATE);
        intent.putExtra(MicrosoftAuthorizationResult.REQUEST_STATE_PARAMETER, MOCK_STATE);
        AuthorizationResult result = mAuthorizationResultFactory.createAuthorizationResult(
                AuthenticationConstants.UIResponse.BROWSER_CODE_COMPLETE, intent);
        assertNotNull(result);
        assertNull(result.getAuthorizationErrorResponse());
        assertEquals(AuthorizationStatus.SUCCESS, result.getAuthorizationStatus());
        AuthorizationResponse response = result.getAuthorizationResponse();
        assertNotNull(response);
        assertEquals(response.getCode(), MOCK_AUTH_CODE);
        assertEquals(response.getState(), MOCK_STATE);
    }

    @Test
    public void testUrlWithInCorrectState() {
        Intent intent = new Intent();
        intent.putExtra(MicrosoftStsAuthorizationResultFactory.MSSTS_AUTHORIZATION_FINAL_URL, REDIRECT_URI + "?" + AUTH_CODE_AND_STATE);
        intent.putExtra(MicrosoftAuthorizationResult.REQUEST_STATE_PARAMETER, "incorrect_state");
        AuthorizationResult result = mAuthorizationResultFactory.createAuthorizationResult(
                AuthenticationConstants.UIResponse.BROWSER_CODE_COMPLETE, intent);
        assertNotNull(result);
        assertNotNull(result.getAuthorizationErrorResponse());
        assertEquals(AuthorizationStatus.FAIL, result.getAuthorizationStatus());
        AuthorizationErrorResponse errorResponse = result.getAuthorizationErrorResponse();
        assertNotNull(errorResponse);
        assertEquals(ErrorStrings.STATE_MISMATCH, errorResponse.getError());
        assertEquals(MicrosoftAuthorizationErrorResponse.STATE_NOT_THE_SAME, errorResponse.getErrorDescription());
    }

    @Test
    public void testUrlWithErrorInParams() {
        Intent intent = new Intent();
        String responseUrl = REDIRECT_URI + "?error=" + ERROR_MESSAGE + "&error_description=" + ERROR_DESCRIPTION;
        intent.putExtra(MicrosoftStsAuthorizationResultFactory.MSSTS_AUTHORIZATION_FINAL_URL, responseUrl);
        AuthorizationResult result = mAuthorizationResultFactory.createAuthorizationResult(
                AuthenticationConstants.UIResponse.BROWSER_CODE_COMPLETE, intent);
        assertNotNull(result);
        assertNull(result.getAuthorizationResponse());
        assertEquals(AuthorizationStatus.FAIL, result.getAuthorizationStatus());
        AuthorizationErrorResponse errorResponse = result.getAuthorizationErrorResponse();
        assertNotNull(errorResponse);
        assertEquals(errorResponse.getError(), ERROR_MESSAGE);
        assertEquals(errorResponse.getErrorDescription(), ERROR_DESCRIPTION);
    }

    //TODO: Add tests to validate state once implemented


}
