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

package com.microsoft.identity.common.java.controllers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.microsoft.identity.common.java.TestUtils;
import com.microsoft.identity.common.java.authorities.Authority;
import com.microsoft.identity.common.java.commands.parameters.BrokerInteractiveTokenCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.BrokerSilentTokenCommandParameters;
import com.microsoft.identity.common.java.constants.OAuth2ErrorCode;
import com.microsoft.identity.common.java.constants.OAuth2SubErrorCode;
import com.microsoft.identity.common.java.exception.BaseException;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.exception.IntuneAppProtectionPolicyRequiredException;
import com.microsoft.identity.common.java.exception.ServiceException;
import com.microsoft.identity.common.java.exception.TerminalException;
import com.microsoft.identity.common.java.exception.UiRequiredException;
import com.microsoft.identity.common.java.providers.microsoft.MicrosoftTokenErrorResponse;
import com.microsoft.identity.common.java.providers.oauth2.TokenErrorResponse;
import com.microsoft.identity.common.java.providers.oauth2.TokenResult;
import com.microsoft.identity.common.java.telemetry.ClientDataInfo;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;

import lombok.SneakyThrows;

@RunWith(JUnit4.class)
public class ExceptionAdapterTests {

    @Test
    public void testBaseExceptionFromException_TerminalException() throws Exception{
        final TerminalException t = new TerminalException("errorMsg", ClientException.KEY_RING_WRITE_FAILURE);
        final BaseException e = ExceptionAdapter.baseExceptionFromException(t);
        assertEquals(t.getErrorCode(), e.getErrorCode());
        assertEquals(t, e.getCause());
    }

    @Test
    public void testMFATokenErrorResponse_IsTranslatedToUIRequiredException() {
        final MicrosoftTokenErrorResponse tokenErrorResponse = new MicrosoftTokenErrorResponse();
        tokenErrorResponse.setError("invalid_grant");
        tokenErrorResponse.setErrorDescription("AADSTS50076: Due to a configuration change made by your administrator, or because you moved to a new location, you must use multi-factor authentication to access '7ae46e1'. Trace ID: 01276277-3a30020d900900 Correlation ID: 6209e18a-f89b-4f14-a05e-0371c6757adb Timestamp: 2024-11-14 13:09:18Z");
        tokenErrorResponse.setErrorCodes(new ArrayList<Long>(Arrays.asList(50076L)));
        tokenErrorResponse.setSubError("basic_action");

        BaseException e = ExceptionAdapter.getExceptionFromTokenErrorResponse(tokenErrorResponse);
        assertTrue("Expected exception of UiRequiredException type", e instanceof UiRequiredException);
        assertEquals(tokenErrorResponse.getError(), e.getErrorCode());
        assertEquals(tokenErrorResponse.getErrorDescription(), e.getMessage());
    }

    @Test
    public void testMFATokenErrorResponse_allowUiRequiredException_True() {
        final MicrosoftTokenErrorResponse tokenErrorResponse = new MicrosoftTokenErrorResponse();
        tokenErrorResponse.setError("invalid_grant");
        tokenErrorResponse.setErrorDescription("AADSTS50076: Due to a configuration change made by your administrator, or because you moved to a new location, you must use multi-factor authentication to access '7ae46e1'. Trace ID: 01276277-3a30020d900900 Correlation ID: 6209e18a-f89b-4f14-a05e-0371c6757adb Timestamp: 2024-11-14 13:09:18Z");
        tokenErrorResponse.setErrorCodes(new ArrayList<Long>(Arrays.asList(50076L)));
        tokenErrorResponse.setSubError("basic_action");

        BaseException e = ExceptionAdapter.getExceptionFromTokenErrorResponse(tokenErrorResponse, true);
        assertTrue("Expected exception of UiRequiredException type", e instanceof UiRequiredException);
        assertEquals(tokenErrorResponse.getError(), e.getErrorCode());
        assertEquals(tokenErrorResponse.getErrorDescription(), e.getMessage());
    }

    @Test
    public void testMFATokenErrorResponse_allowUiRequiredException_False() {
        final MicrosoftTokenErrorResponse tokenErrorResponse = new MicrosoftTokenErrorResponse();
        tokenErrorResponse.setError("invalid_grant");
        tokenErrorResponse.setErrorDescription("AADSTS50076: Due to a configuration change made by your administrator, or because you moved to a new location, you must use multi-factor authentication to access '7ae46e1'. Trace ID: 01276277-3a30020d900900 Correlation ID: 6209e18a-f89b-4f14-a05e-0371c6757adb Timestamp: 2024-11-14 13:09:18Z");
        tokenErrorResponse.setErrorCodes(new ArrayList<Long>(Arrays.asList(50076L)));
        tokenErrorResponse.setSubError("basic_action");

        BaseException e = ExceptionAdapter.getExceptionFromTokenErrorResponse(tokenErrorResponse, false);
        assertFalse("Expected exception of UiRequiredException type", e instanceof UiRequiredException);
        assertTrue("Expected exception of UiRequiredException type", e instanceof ServiceException);
        assertEquals(tokenErrorResponse.getError(), e.getErrorCode());
        assertEquals(tokenErrorResponse.getErrorDescription(), e.getMessage());
    }

    @Test
    public void testNativeAuthMFAException_ContainsCorrectDescription() {
        String description = "description";
        ServiceException outErr = new ServiceException("errorCode", description, null);
        outErr.setCliTelemErrorCode("50076");
        ServiceException result = ExceptionAdapter.convertToNativeAuthException(outErr);
        assertEquals("Multi-factor authentication is required, which can't be fulfilled as part of this flow. Please sign out and perform a new sign in operation. Please see exception details for more information." + description, result.getMessage());
    }

    @Test
    public void testNativeAuthResetPasswordRequiredException_ContainsCorrectDescription() {
        String description = "description";
        ServiceException outErr = new ServiceException("errorCode", description, null);
        outErr.setCliTelemErrorCode("50142");
        ServiceException result = ExceptionAdapter.convertToNativeAuthException(outErr);
        assertEquals("User password change is required, which can't be fulfilled as part of this flow.Please reset the password and perform a new sign in operation. Please see exception details for more information." + description, result.getMessage());
    }

    @Test
    public void testClientExceptionFromException_TimeoutException() {
        final TimeoutException t = new TimeoutException();
        assertEquals(ClientException.TIMED_OUT, ExceptionAdapter.clientExceptionFromException(t).getErrorCode());
    }

    @SneakyThrows
    @Test
    public void testGetExceptionFromTokenErrorResponse_WithBrokerSilentTokenCommandParameters_PolicyProtectionRequired() {
        final BrokerSilentTokenCommandParameters commandParameters = mock(BrokerSilentTokenCommandParameters.class);
        final Authority authority = mock(Authority.class);
        when(authority.getAuthorityURL()).thenReturn(new URL("https://login.microsoftonline.com/organizations"));
        when(commandParameters.getAuthority()).thenReturn(authority);
        when(commandParameters.isRequestForResourceAccount()).thenReturn(false);
        final TokenErrorResponse errorResponse = new TokenErrorResponse();
        errorResponse.setError(OAuth2ErrorCode.UNAUTHORIZED_CLIENT);
        errorResponse.setSubError(OAuth2SubErrorCode.PROTECTION_POLICY_REQUIRED);
        errorResponse.setErrorDescription("Intune policy required.");

        ServiceException exception = ExceptionAdapter.getExceptionFromTokenErrorResponse(commandParameters, errorResponse);

        assertTrue(exception instanceof IntuneAppProtectionPolicyRequiredException);
        assertEquals(OAuth2SubErrorCode.PROTECTION_POLICY_REQUIRED, exception.getSubErrorCode());
        assertEquals("Intune policy required.", exception.getMessage());
    }

    @Test
    public void testGetExceptionFromTokenErrorResponse_NullCommandParameters_PolicyProtectionRequired() {
        final TokenErrorResponse errorResponse = new TokenErrorResponse();
        errorResponse.setError(OAuth2ErrorCode.UNAUTHORIZED_CLIENT);
        errorResponse.setSubError(OAuth2SubErrorCode.PROTECTION_POLICY_REQUIRED);
        errorResponse.setErrorDescription("Intune policy required.");

        ServiceException exception = ExceptionAdapter.getExceptionFromTokenErrorResponse(null, errorResponse);

        assertFalse(exception instanceof UiRequiredException);
        assertEquals(OAuth2ErrorCode.UNAUTHORIZED_CLIENT, exception.getErrorCode());
        assertEquals("Intune policy required.", exception.getMessage());
    }

    @Test
    public void testGetExceptionFromTokenErrorResponse_WithBrokerSilentTokenCommandParameters_ResourceAccount() {
        final BrokerSilentTokenCommandParameters commandParameters = mock(BrokerSilentTokenCommandParameters.class);
        when(commandParameters.isRequestForResourceAccount()).thenReturn(true);

        final TokenErrorResponse errorResponse = new TokenErrorResponse();
        errorResponse.setError(OAuth2ErrorCode.INVALID_GRANT);
        errorResponse.setErrorDescription("UI required.");

        ServiceException exception = ExceptionAdapter.getExceptionFromTokenErrorResponse(commandParameters, errorResponse);

        assertFalse(exception instanceof UiRequiredException);
        assertEquals(OAuth2ErrorCode.INVALID_GRANT, exception.getErrorCode());
        assertEquals("UI required.", exception.getMessage());
    }

    @Test
    public void testGetExceptionFromTokenErrorResponse_WithBrokerSilentTokenCommandParameters() {
        final BrokerSilentTokenCommandParameters commandParameters = mock(BrokerSilentTokenCommandParameters.class);
        when(commandParameters.isRequestForResourceAccount()).thenReturn(false);

        final TokenErrorResponse errorResponse = new TokenErrorResponse();
        errorResponse.setError(OAuth2ErrorCode.INVALID_GRANT);
        errorResponse.setErrorDescription("UI required.");

        ServiceException exception = ExceptionAdapter.getExceptionFromTokenErrorResponse(commandParameters, errorResponse);

        assertTrue(exception instanceof UiRequiredException);
        assertEquals(OAuth2ErrorCode.INVALID_GRANT, exception.getErrorCode());
        assertEquals("UI required.", exception.getMessage());
    }

    @Test
    public void testGetExceptionFromTokenErrorResponse_NullCommandParameters() {
        final TokenErrorResponse errorResponse = new TokenErrorResponse();
        errorResponse.setError(OAuth2ErrorCode.INVALID_GRANT);
        errorResponse.setErrorDescription("UI required.");

        ServiceException exception = ExceptionAdapter.getExceptionFromTokenErrorResponse(null, errorResponse);

        assertTrue(exception instanceof UiRequiredException);
        assertEquals(OAuth2ErrorCode.INVALID_GRANT, exception.getErrorCode());
        assertEquals("UI required.", exception.getMessage());
    }

    // -----------------------------------------------------------------------
    // ClientDataInfo wiring tests (PR #3109)
    // -----------------------------------------------------------------------

    @Test
    public void testExceptionFromTokenResult_attachesClientDataInfo() {
        final TokenErrorResponse errorResponse = new TokenErrorResponse();
        errorResponse.setError(OAuth2ErrorCode.INVALID_GRANT);
        errorResponse.setErrorDescription("token failure");

        final ClientDataInfo clientDataInfo = ClientDataInfo.fromPipeDelimited("m|AADSTS50058|login_required|us|public");
        Assert.assertNotNull(clientDataInfo);

        final TokenResult tokenResult = new TokenResult(null, errorResponse);
        tokenResult.setClientDataInfo(clientDataInfo);

        final ServiceException exception = ExceptionAdapter.exceptionFromTokenResult(tokenResult, null);

        Assert.assertNotNull("ClientDataInfo should be attached to the exception", exception.getClientDataInfo());
        assertEquals("AADSTS50058", exception.getClientDataInfo().getError());
        assertEquals("login_required", exception.getClientDataInfo().getSubError());
        assertEquals("m|AADSTS50058|login_required|us|public", exception.getClientDataInfo().getRaw());
    }

    @Test
    public void testExceptionFromTokenResult_nullClientDataInfo_doesNotThrow() {
        final TokenErrorResponse errorResponse = new TokenErrorResponse();
        errorResponse.setError(OAuth2ErrorCode.INVALID_GRANT);
        errorResponse.setErrorDescription("token failure");

        final TokenResult tokenResult = new TokenResult(null, errorResponse);
        // No ClientDataInfo set

        final ServiceException exception = ExceptionAdapter.exceptionFromTokenResult(tokenResult, null);
        Assert.assertNull(exception.getClientDataInfo());
    }
}