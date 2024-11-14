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

import com.microsoft.identity.common.java.exception.BaseException;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.exception.ServiceException;
import com.microsoft.identity.common.java.exception.TerminalException;
import com.microsoft.identity.common.java.exception.UiRequiredException;
import com.microsoft.identity.common.java.providers.microsoft.MicrosoftTokenErrorResponse;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;

@RunWith(JUnit4.class)
public class ExceptionAdapterTests {

    @Test
    public void testBaseExceptionFromException_TerminalException() throws Exception{
        TerminalException t = new TerminalException("errorMsg", ClientException.KEY_RING_WRITE_FAILURE);
        BaseException e = ExceptionAdapter.baseExceptionFromException(t);
        Assert.assertEquals(e.getErrorCode(), t.getErrorCode());
        Assert.assertEquals(e.getCause(), t);
    }

    @Test
    public void testMFATokenErrorResponse_IsTranslatedToUIRequiredException() {
        final MicrosoftTokenErrorResponse tokenErrorResponse = new MicrosoftTokenErrorResponse();
        tokenErrorResponse.setError("invalid_grant");
        tokenErrorResponse.setErrorDescription("AADSTS50076: Due to a configuration change made by your administrator, or because you moved to a new location, you must use multi-factor authentication to access '7ae46e1'. Trace ID: 01276277-3a30020d900900 Correlation ID: 6209e18a-f89b-4f14-a05e-0371c6757adb Timestamp: 2024-11-14 13:09:18Z");
        tokenErrorResponse.setErrorCodes(new ArrayList<Long>(Arrays.asList(50076L)));
        tokenErrorResponse.setSubError("basic_action");

        BaseException e = ExceptionAdapter.getExceptionFromTokenErrorResponse(tokenErrorResponse);
        Assert.assertTrue("Expected exception of UiRequiredException type", e instanceof UiRequiredException);
        Assert.assertEquals(e.getErrorCode(), tokenErrorResponse.getError());
        Assert.assertEquals(e.getMessage(), tokenErrorResponse.getErrorDescription());
    }

    @Test
    public void testNativeAuthMFAException_ContainsCorrectDescription() {
        String description = "description";
        ServiceException outErr = new ServiceException("errorCode", description, null);
        outErr.setCliTelemErrorCode("50076");
        ServiceException result = ExceptionAdapter.convertToNativeAuthException(outErr);
        Assert.assertEquals("Multi-factor authentication is required, which can't be fulfilled as part of this flow. Please sign out and perform a new sign in operation. Please see exception details for more information." + description, result.getMessage());
    }

    @Test
    public void testNativeAuthResetPasswordRequiredException_ContainsCorrectDescription() {
        String description = "description";
        ServiceException outErr = new ServiceException("errorCode", description, null);
        outErr.setCliTelemErrorCode("50142");
        ServiceException result = ExceptionAdapter.convertToNativeAuthException(outErr);
        Assert.assertEquals("User password change is required, which can't be fulfilled as part of this flow.Please reset the password and perform a new sign in operation. Please see exception details for more information." + description, result.getMessage());
    }

    @Test
    public void testClientExceptionFromException_TimeoutException() {
        final TimeoutException t = new TimeoutException();
        Assert.assertEquals(ClientException.TIMED_OUT, ExceptionAdapter.clientExceptionFromException(t).getErrorCode());
    }
}