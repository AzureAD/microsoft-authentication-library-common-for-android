package com.microsoft.identity.common;

import android.content.Intent;
import android.support.test.runner.AndroidJUnit4;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftAuthorizationErrorResponse;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationResponse;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationResult;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationResultFactory;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationErrorResponse;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResponse;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResult;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationStatus;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


@RunWith(AndroidJUnit4.class)
public class MicrosoftStsAuthorizationResultFactoryTest {

    private static final String REDIRECT_URI = "msauth-clientid://packagename/";
    private static final String AUTH_CODE_AND_STATE = "code=authorization_code&state=state";
    private static final String MOCK_AUTH_CODE = "authorization_code";
    private static final String MOCK_STATE = "state";
    private static final String ERROR_MESSAGE = "access_denied";
    private static final String ERROR_DESCRIPTION = "access denied error description";

    @Test
    public void testBrowserCodeCancel() {
        Intent intent = new Intent();
        MicrosoftStsAuthorizationResultFactory factory = new MicrosoftStsAuthorizationResultFactory();
        AuthorizationResult result = factory.createAuthorizationResult(AuthenticationConstants.UIResponse.BROWSER_CODE_CANCEL, intent);
        assertTrue(result instanceof MicrosoftStsAuthorizationResult);
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
        MicrosoftStsAuthorizationResultFactory factory = new MicrosoftStsAuthorizationResultFactory();
        AuthorizationResult result = factory.createAuthorizationResult(AuthenticationConstants.UIResponse.BROWSER_CODE_ERROR, intent);
        assertTrue(result instanceof MicrosoftStsAuthorizationResult);
        assertNull(result.getAuthorizationResponse());
        assertEquals(AuthorizationStatus.FAIL, result.getAuthorizationStatus());
        assertNotNull(result.getAuthorizationErrorResponse());
    }

    @Test
    public void testNoMatchingResultCode() {
        Intent intent = new Intent();
        MicrosoftStsAuthorizationResultFactory factory = new MicrosoftStsAuthorizationResultFactory();
        AuthorizationResult result = factory.createAuthorizationResult(0, intent);
        assertTrue(result instanceof MicrosoftStsAuthorizationResult);
        assertNull(result.getAuthorizationResponse());
        assertEquals(AuthorizationStatus.FAIL, result.getAuthorizationStatus());
        AuthorizationErrorResponse errorResponse = result.getAuthorizationErrorResponse();
        assertNotNull(errorResponse);
        assertEquals(MicrosoftAuthorizationErrorResponse.UNKNOWN_ERROR, errorResponse.getError());
        assertEquals(MicrosoftAuthorizationErrorResponse.UNKNOWN_RESULT_CODE, errorResponse.getErrorDescription());
    }

    @Test
    public void testNullIntent() {
        MicrosoftStsAuthorizationResultFactory factory = new MicrosoftStsAuthorizationResultFactory();
        AuthorizationResult result = factory.createAuthorizationResult(AuthenticationConstants.UIResponse.BROWSER_CODE_COMPLETE, null);
        assertTrue(result instanceof MicrosoftStsAuthorizationResult);
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
        MicrosoftStsAuthorizationResultFactory factory = new MicrosoftStsAuthorizationResultFactory();
        AuthorizationResult result = factory.createAuthorizationResult(AuthenticationConstants.UIResponse.BROWSER_CODE_COMPLETE, intent);
        assertTrue(result instanceof MicrosoftStsAuthorizationResult);
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
        MicrosoftStsAuthorizationResultFactory factory = new MicrosoftStsAuthorizationResultFactory();
        AuthorizationResult result = factory.createAuthorizationResult(AuthenticationConstants.UIResponse.BROWSER_CODE_COMPLETE, intent);
        assertTrue(result instanceof MicrosoftStsAuthorizationResult);
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
        MicrosoftStsAuthorizationResultFactory factory = new MicrosoftStsAuthorizationResultFactory();
        AuthorizationResult result = factory.createAuthorizationResult(AuthenticationConstants.UIResponse.BROWSER_CODE_COMPLETE, intent);
        assertTrue(result instanceof MicrosoftStsAuthorizationResult);
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
        MicrosoftStsAuthorizationResultFactory factory = new MicrosoftStsAuthorizationResultFactory();
        AuthorizationResult result = factory.createAuthorizationResult(AuthenticationConstants.UIResponse.BROWSER_CODE_COMPLETE, intent);
        assertTrue(result instanceof MicrosoftStsAuthorizationResult);
        assertNull(result.getAuthorizationErrorResponse());
        assertEquals(AuthorizationStatus.SUCCESS, result.getAuthorizationStatus());
        AuthorizationResponse response = result.getAuthorizationResponse();
        assertTrue(response instanceof MicrosoftStsAuthorizationResponse);
        assertNotNull(response);
        assertEquals(response.getCode(), MOCK_AUTH_CODE);
        assertEquals(response.getState(), MOCK_STATE);
    }

    @Test
    public void testUrlWithErrorInParams() {
        Intent intent = new Intent();
        String responseUrl = REDIRECT_URI + "?error=" + ERROR_MESSAGE + "&error_description=" + ERROR_DESCRIPTION;
        intent.putExtra(MicrosoftStsAuthorizationResultFactory.MSSTS_AUTHORIZATION_FINAL_URL, responseUrl);
        MicrosoftStsAuthorizationResultFactory factory = new MicrosoftStsAuthorizationResultFactory();
        AuthorizationResult result = factory.createAuthorizationResult(AuthenticationConstants.UIResponse.BROWSER_CODE_COMPLETE, intent);
        assertTrue(result instanceof MicrosoftStsAuthorizationResult);
        assertNull(result.getAuthorizationResponse());
        assertEquals(AuthorizationStatus.FAIL, result.getAuthorizationStatus());
        AuthorizationErrorResponse errorResponse = result.getAuthorizationErrorResponse();
        assertNotNull(errorResponse);
        assertEquals(errorResponse.getError(), ERROR_MESSAGE);
        assertEquals(errorResponse.getErrorDescription(), ERROR_DESCRIPTION);
    }

    //TODO: Add tests to validate state once implemented


}
