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
package com.microsoft.identity.common.java.providers.microsoft.microsoftsts;

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

import static com.microsoft.identity.common.java.providers.Constants.BROKER_INSTALLATION_REQUIRED_WEBVIEW_REDIRECT_URI;
import static com.microsoft.identity.common.java.providers.Constants.CANCEL_RESPONSE_REDIRECT_URI;
import static com.microsoft.identity.common.java.providers.Constants.MOCK_AUTH_CODE_AND_STATE;
import static com.microsoft.identity.common.java.providers.Constants.MOCK_ERROR_CODE;
import static com.microsoft.identity.common.java.providers.Constants.MOCK_ERROR_MESSAGE;
import static com.microsoft.identity.common.java.providers.Constants.MOCK_FRAGMENT_STRING;
import static com.microsoft.identity.common.java.providers.Constants.MOCK_REDIRECT_URI;
import static com.microsoft.identity.common.java.providers.Constants.MOCK_STATE;
import static com.microsoft.identity.common.java.providers.Constants.MOCK_STATE_ENCODED;
import static com.microsoft.identity.common.java.providers.Constants.MOCK_WPJ_USERNAME;
import static com.microsoft.identity.common.java.providers.Constants.WPJ_REQUIRED_REDIRECT_URI;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(JUnit4.class)
public class MicrosoftStsAuthorizationResultFactoryTest {

    private AuthorizationResultFactory<MicrosoftStsAuthorizationResult, MicrosoftStsAuthorizationRequest> mAuthorizationResultFactory;

    @Before
    public void setUp() {
        mAuthorizationResultFactory = new MicrosoftStsAuthorizationResultFactory();
    }

    private MicrosoftStsAuthorizationRequest getMstsAuthorizationRequest() {
        return new MicrosoftStsAuthorizationRequest.Builder().setState(MOCK_STATE).build();
    }

    private MicrosoftStsAuthorizationRequest getMstsAuthorizationRequestWithState(@NonNull final String state) {
        return new MicrosoftStsAuthorizationRequest.Builder().setState(state).build();
    }

    @Test
    public void testClientError() {
        final AuthorizationResult result = mAuthorizationResultFactory.createAuthorizationResult(
                RawAuthorizationResult.fromException(new ClientException(MOCK_ERROR_CODE, MOCK_ERROR_MESSAGE)), getMstsAuthorizationRequest());
        assertNotNull(result);
        assertNull(result.getAuthorizationResponse());
        assertEquals(AuthorizationStatus.FAIL, result.getAuthorizationStatus());
        assertNotNull(result.getAuthorizationErrorResponse());
        assertNotNull(MOCK_ERROR_CODE, result.getAuthorizationErrorResponse().getError());
        assertNotNull(MOCK_ERROR_MESSAGE, result.getAuthorizationErrorResponse().getErrorDescription());
    }

    // BROWSER_CODE_COMPLETE should be set only via RawAuthorizationResult.fromRedirectUrl()
    @Test(expected = IllegalArgumentException.class)
    public void testHandleCompleteResultWithOnlyCode() {
        mAuthorizationResultFactory.createAuthorizationResult(
                RawAuthorizationResult.fromResultCode(RawAuthorizationResult.ResultCode.COMPLETED), getMstsAuthorizationRequest());
        Assert.fail();
    }

    @Test
    public void testHandleInlineWpj(){
        final AuthorizationResult result = mAuthorizationResultFactory.createAuthorizationResult(
                RawAuthorizationResult.fromRedirectUri(WPJ_REQUIRED_REDIRECT_URI), getMstsAuthorizationRequest());
        assertNotNull(result);
        assertNull(result.getAuthorizationResponse());
        assertEquals(AuthorizationStatus.FAIL, result.getAuthorizationStatus());
        AuthorizationErrorResponse errorResponse = result.getAuthorizationErrorResponse();
        assertNotNull(errorResponse);
        assertEquals(MicrosoftAuthorizationErrorResponse.DEVICE_REGISTRATION_NEEDED, errorResponse.getError());
        assertEquals(MicrosoftAuthorizationErrorResponse.DEVICE_REGISTRATION_NEEDED_ERROR_DESCRIPTION, errorResponse.getErrorDescription());
        assertEquals(MOCK_WPJ_USERNAME, errorResponse.getUpnToWpj());
    }

    @Test
    public void testHandleBrokerAppInstallationRequired(){
        final AuthorizationResult result = mAuthorizationResultFactory.createAuthorizationResult(
                RawAuthorizationResult.fromRedirectUri(BROKER_INSTALLATION_REQUIRED_WEBVIEW_REDIRECT_URI), getMstsAuthorizationRequest());
        assertNotNull(result);
        assertNull(result.getAuthorizationResponse());
        assertEquals(AuthorizationStatus.FAIL, result.getAuthorizationStatus());
        AuthorizationErrorResponse errorResponse = result.getAuthorizationErrorResponse();
        assertNotNull(errorResponse);
        assertEquals(MicrosoftAuthorizationErrorResponse.BROKER_NEEDS_TO_BE_INSTALLED, errorResponse.getError());
        assertEquals(MicrosoftAuthorizationErrorResponse.BROKER_NEEDS_TO_BE_INSTALLED_ERROR_DESCRIPTION, errorResponse.getErrorDescription());
        assertEquals(MOCK_WPJ_USERNAME, errorResponse.getUpnToWpj());
    }

    @Test
    public void testHandleSDKCancel(){
        final AuthorizationResult result = mAuthorizationResultFactory.createAuthorizationResult(
                RawAuthorizationResult.fromResultCode(RawAuthorizationResult.ResultCode.SDK_CANCELLED), getMstsAuthorizationRequest());
        assertNotNull(result);
        assertNull(result.getAuthorizationResponse());
        assertEquals(AuthorizationStatus.SDK_CANCEL, result.getAuthorizationStatus());
        AuthorizationErrorResponse errorResponse = result.getAuthorizationErrorResponse();
        assertNotNull(errorResponse);
        assertEquals(MicrosoftAuthorizationErrorResponse.SDK_AUTH_CANCEL, errorResponse.getError());
        assertEquals(MicrosoftAuthorizationErrorResponse.SDK_CANCELLED_FLOW, errorResponse.getErrorDescription());
    }

    @Test
    public void testBrowserCodeCancel() {
        final AuthorizationResult result = mAuthorizationResultFactory.createAuthorizationResult(
                RawAuthorizationResult.fromResultCode(RawAuthorizationResult.ResultCode.CANCELLED), getMstsAuthorizationRequest());
        assertNotNull(result);
        assertNull(result.getAuthorizationResponse());
        assertEquals(AuthorizationStatus.USER_CANCEL, result.getAuthorizationStatus());
        AuthorizationErrorResponse errorResponse = result.getAuthorizationErrorResponse();
        assertNotNull(errorResponse);
        assertEquals(MicrosoftAuthorizationErrorResponse.USER_CANCEL, errorResponse.getError());
        assertEquals(MicrosoftAuthorizationErrorResponse.USER_CANCELLED_FLOW, errorResponse.getErrorDescription());
    }

    @Test
    public void testHandleCancelViaUrl(){
        final AuthorizationResult result = mAuthorizationResultFactory.createAuthorizationResult(
                RawAuthorizationResult.fromRedirectUri(CANCEL_RESPONSE_REDIRECT_URI), getMstsAuthorizationRequest());
        assertNotNull(result);
        assertNull(result.getAuthorizationResponse());
        assertEquals(AuthorizationStatus.USER_CANCEL, result.getAuthorizationStatus());
        AuthorizationErrorResponse errorResponse = result.getAuthorizationErrorResponse();
        assertNotNull(errorResponse);
        assertEquals(MicrosoftAuthorizationErrorResponse.USER_CANCEL, errorResponse.getError());
        assertEquals(MicrosoftAuthorizationErrorResponse.USER_CANCELLED_FLOW, errorResponse.getErrorDescription());
    }

    @Test
    public void testHandleMDMFlow(){
        final AuthorizationResult result = mAuthorizationResultFactory.createAuthorizationResult(
                RawAuthorizationResult.fromResultCode(RawAuthorizationResult.ResultCode.MDM_FLOW), getMstsAuthorizationRequest());
        assertNotNull(result);
        assertNull(result.getAuthorizationResponse());
        assertEquals(AuthorizationStatus.FAIL, result.getAuthorizationStatus());
        AuthorizationErrorResponse errorResponse = result.getAuthorizationErrorResponse();
        assertNotNull(errorResponse);
        assertEquals(MicrosoftAuthorizationErrorResponse.DEVICE_NEEDS_TO_BE_MANAGED, errorResponse.getError());
        assertEquals(MicrosoftAuthorizationErrorResponse.DEVICE_NEEDS_TO_BE_MANAGED_ERROR_DESCRIPTION, errorResponse.getErrorDescription());
    }

    @Test
    public void testUnknownResultCode() {
        final AuthorizationResult result = mAuthorizationResultFactory.createAuthorizationResult(
                RawAuthorizationResult.fromResultCode(RawAuthorizationResult.ResultCode.UNKNOWN), getMstsAuthorizationRequest());
        assertNotNull(result);
        assertNull(result.getAuthorizationResponse());
        assertEquals(AuthorizationStatus.FAIL, result.getAuthorizationStatus());
        AuthorizationErrorResponse errorResponse = result.getAuthorizationErrorResponse();
        assertNotNull(errorResponse);
        assertEquals(MicrosoftAuthorizationErrorResponse.UNKNOWN_ERROR, errorResponse.getError());
        assertEquals(MicrosoftAuthorizationErrorResponse.UNKNOWN_RESULT_CODE + "[" +
                RawAuthorizationResult.ResultCode.UNKNOWN.name() +"]", errorResponse.getErrorDescription());
    }

    @Test
    public void testEmptyUrl() {
        final AuthorizationResult result = mAuthorizationResultFactory.createAuthorizationResult(
                RawAuthorizationResult.fromRedirectUri(""), getMstsAuthorizationRequest());
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
        final AuthorizationResult result = mAuthorizationResultFactory.createAuthorizationResult(
                RawAuthorizationResult.fromRedirectUri(MOCK_REDIRECT_URI), getMstsAuthorizationRequest());
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
        final AuthorizationResult result = mAuthorizationResultFactory.createAuthorizationResult(
                RawAuthorizationResult.fromRedirectUri(MOCK_REDIRECT_URI + "?some_random_error=accessdenied"), getMstsAuthorizationRequest());
        assertNotNull(result);
        assertNull(result.getAuthorizationResponse());
        assertEquals(AuthorizationStatus.FAIL, result.getAuthorizationStatus());
        AuthorizationErrorResponse errorResponse = result.getAuthorizationErrorResponse();
        assertNotNull(errorResponse);
        assertEquals(MicrosoftAuthorizationErrorResponse.AUTHORIZATION_FAILED, errorResponse.getError());
        assertEquals(MicrosoftAuthorizationErrorResponse.AUTHORIZATION_SERVER_INVALID_RESPONSE, errorResponse.getErrorDescription());
    }


    @Test
    public void testUrlWithInCorrectState() {
        final AuthorizationResult result = mAuthorizationResultFactory.createAuthorizationResult(
                RawAuthorizationResult.fromRedirectUri(MOCK_REDIRECT_URI + "?" + MOCK_AUTH_CODE_AND_STATE),
                getMstsAuthorizationRequestWithState("incorrect_state"));
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
        final String responseUrl = MOCK_REDIRECT_URI + "?error=" + MOCK_ERROR_CODE + "&error_description=" + MOCK_ERROR_MESSAGE;
        final AuthorizationResult result = mAuthorizationResultFactory.createAuthorizationResult(
                RawAuthorizationResult.fromRedirectUri(responseUrl), getMstsAuthorizationRequest());
        assertNotNull(result);
        assertNull(result.getAuthorizationResponse());
        assertEquals(AuthorizationStatus.FAIL, result.getAuthorizationStatus());
        AuthorizationErrorResponse errorResponse = result.getAuthorizationErrorResponse();
        assertNotNull(errorResponse);
        assertEquals(errorResponse.getError(), MOCK_ERROR_CODE);
        assertEquals(errorResponse.getErrorDescription(), MOCK_ERROR_MESSAGE);
    }

    @Test
    public void testUrlWithValidAuthCodeAndFragmentParas() {
        final AuthorizationResult result = mAuthorizationResultFactory.createAuthorizationResult(
                RawAuthorizationResult.fromRedirectUri(
                        MOCK_REDIRECT_URI
                                + "?" + "code=authorization_code&state=" + MOCK_STATE_ENCODED
                                + MOCK_FRAGMENT_STRING
                ), getMstsAuthorizationRequest());
        assertNotNull(result);
        assertNotNull(result.getAuthorizationResponse());
        assertEquals(AuthorizationStatus.SUCCESS, result.getAuthorizationStatus());
        assertNotNull(result.getAuthorizationResponse().getCode());
    }

    @Test
    public void testUrlWithInvalidAuthCodeAndFragmentParas() {
        final AuthorizationResult result = mAuthorizationResultFactory.createAuthorizationResult(
                RawAuthorizationResult.fromRedirectUri(MOCK_REDIRECT_URI + "?" + MOCK_FRAGMENT_STRING), getMstsAuthorizationRequest());
        assertNotNull(result);
        assertNotNull(result.getAuthorizationErrorResponse());
        assertEquals(AuthorizationStatus.FAIL, result.getAuthorizationStatus());
        AuthorizationErrorResponse errorResponse = result.getAuthorizationErrorResponse();
        assertEquals(errorResponse.getError(), MicrosoftAuthorizationErrorResponse.AUTHORIZATION_FAILED);
        assertEquals(errorResponse.getErrorDescription(), MicrosoftAuthorizationErrorResponse.AUTHORIZATION_SERVER_INVALID_RESPONSE);
    }
}
