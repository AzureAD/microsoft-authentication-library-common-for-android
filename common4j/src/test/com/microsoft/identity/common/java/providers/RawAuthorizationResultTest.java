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
package com.microsoft.identity.common.java.providers;

import com.microsoft.identity.common.java.exception.ClientException;

import org.junit.Assert;
import org.junit.Test;

import static com.microsoft.identity.common.java.providers.Constants.BROKER_INSTALLATION_REQUIRED_BROWSER_REDIRECT_URI;
import static com.microsoft.identity.common.java.providers.Constants.BROKER_INSTALLATION_REQUIRED_WEBVIEW_REDIRECT_URI;
import static com.microsoft.identity.common.java.providers.Constants.CANCEL_RESPONSE_REDIRECT_URI;
import static com.microsoft.identity.common.java.providers.Constants.ERROR_RESPONSE_REDIRECT_URI;
import static com.microsoft.identity.common.java.providers.Constants.MALFORMED_REDIRECT_URI;
import static com.microsoft.identity.common.java.providers.Constants.MOCK_ERROR_CODE;
import static com.microsoft.identity.common.java.providers.Constants.MOCK_ERROR_MESSAGE;
import static com.microsoft.identity.common.java.providers.Constants.SUCCEED_REDIRECT_URI;
import static com.microsoft.identity.common.java.providers.Constants.WPJ_REQUIRED_REDIRECT_URI;
import static com.microsoft.identity.common.java.providers.RawAuthorizationResult.ResultCode.BROKER_INSTALLATION_TRIGGERED;
import static com.microsoft.identity.common.java.providers.RawAuthorizationResult.ResultCode.CANCELLED;
import static com.microsoft.identity.common.java.providers.RawAuthorizationResult.ResultCode.NON_OAUTH_ERROR;
import static com.microsoft.identity.common.java.providers.RawAuthorizationResult.ResultCode.COMPLETED;
import static com.microsoft.identity.common.java.providers.RawAuthorizationResult.ResultCode.DEVICE_REGISTRATION_REQUIRED;
import static com.microsoft.identity.common.java.providers.RawAuthorizationResult.ResultCode.MDM_FLOW;
import static com.microsoft.identity.common.java.providers.RawAuthorizationResult.ResultCode.SDK_CANCELLED;
import static com.microsoft.identity.common.java.providers.RawAuthorizationResult.ResultCode.UNKNOWN;

public class RawAuthorizationResultTest {

    @Test
    public void testWithUnknownResultCode() {
        testFromResultCode(UNKNOWN);
    }

    @Test
    public void testWithCancelledResultCode() {
        testFromResultCode(CANCELLED);
    }

    // Should be set via RawAuthorizationResult.fromException() only.
    @Test(expected = IllegalArgumentException.class)
    public void testWithClientErrorResultCode() {
        testFromResultCode(NON_OAUTH_ERROR);
    }

    // Should be set via RawAuthorizationResult.fromRedirectUri() only.
    @Test(expected = IllegalArgumentException.class)
    public void testWithCompletedResultCode() {
        testFromResultCode(COMPLETED);
    }

    // Should be set via RawAuthorizationResult.fromRedirectUri() only.
    @Test(expected = IllegalArgumentException.class)
    public void testWithBrokerInstallationResultCode() {
        testFromResultCode(BROKER_INSTALLATION_TRIGGERED);
    }

    @Test
    public void testWithSdkCancelledResultCode() {
        testFromResultCode(SDK_CANCELLED);
    }

    @Test
    public void testWithMdmFlowResultCode() {
        testFromResultCode(MDM_FLOW);
    }

    // Should be set via RawAuthorizationResult.fromRedirectUri() only.
    @Test(expected = IllegalArgumentException.class)
    public void testWithWpjRequiredErrorCode() {
        testFromResultCode(DEVICE_REGISTRATION_REQUIRED);
    }

    public void testFromResultCode(final RawAuthorizationResult.ResultCode resultCode) {
        final RawAuthorizationResult result = RawAuthorizationResult.fromResultCode(resultCode);
        Assert.assertEquals(resultCode, result.getResultCode());
        Assert.assertNull(result.getException());
        Assert.assertNull(result.getAuthorizationFinalUri());
    }

    @Test
    public void testFromInteger() {
        for (RawAuthorizationResult.ResultCode c : RawAuthorizationResult.ResultCode.values()) {
            Assert.assertSame(c, RawAuthorizationResult.ResultCode.fromInteger(c.getCode()));
        }
        Assert.assertSame(UNKNOWN, RawAuthorizationResult.ResultCode.fromInteger(null));
        Assert.assertSame(UNKNOWN, RawAuthorizationResult.ResultCode.fromInteger(Integer.MIN_VALUE));
    }

    @Test
    public void testFromException() {
        final RawAuthorizationResult result = RawAuthorizationResult.fromException(
                new ClientException(MOCK_ERROR_CODE, MOCK_ERROR_MESSAGE)
        );

        Assert.assertEquals(NON_OAUTH_ERROR, result.getResultCode());
        Assert.assertNull(result.getAuthorizationFinalUri());
        Assert.assertNotNull(result.getException());
        Assert.assertEquals(MOCK_ERROR_CODE, result.getException().getErrorCode());
        Assert.assertEquals(MOCK_ERROR_MESSAGE, result.getException().getMessage());
    }

    @Test
    public void testFromSuccessRedirectUri() {
        final RawAuthorizationResult result = RawAuthorizationResult.fromRedirectUri(SUCCEED_REDIRECT_URI);

        Assert.assertEquals(COMPLETED, result.getResultCode());
        Assert.assertEquals(SUCCEED_REDIRECT_URI, result.getAuthorizationFinalUri().toString());
    }

    // It's a 'valid' redirect uri, so it's treated as 'succeeded'.
    // AuthorizationResponseFactory will handle the rest.
    @Test
    public void testFromErrorRedirectUri() {
        final RawAuthorizationResult result = RawAuthorizationResult.fromRedirectUri(ERROR_RESPONSE_REDIRECT_URI);

        Assert.assertEquals(COMPLETED, result.getResultCode());
        Assert.assertEquals(ERROR_RESPONSE_REDIRECT_URI, result.getAuthorizationFinalUri().toString());
    }

    @Test
    public void testFromCancelRedirectUri() {
        final RawAuthorizationResult result = RawAuthorizationResult.fromRedirectUri(CANCEL_RESPONSE_REDIRECT_URI);

        Assert.assertEquals(CANCELLED, result.getResultCode());
        Assert.assertEquals(CANCEL_RESPONSE_REDIRECT_URI, result.getAuthorizationFinalUri().toString());
    }

    @Test
    public void testFromMalformedRedirectUri() {
        final RawAuthorizationResult result = RawAuthorizationResult.fromRedirectUri(MALFORMED_REDIRECT_URI);

        Assert.assertEquals(NON_OAUTH_ERROR, result.getResultCode());
        Assert.assertNull(result.getAuthorizationFinalUri());
        Assert.assertNotNull(result.getException());
        Assert.assertEquals(ClientException.MALFORMED_URL, result.getException().getErrorCode());
    }

    @Test
    public void testFromWpjRequiredRedirectUri() {
        final RawAuthorizationResult result = RawAuthorizationResult.fromRedirectUri(WPJ_REQUIRED_REDIRECT_URI);

        Assert.assertEquals(DEVICE_REGISTRATION_REQUIRED, result.getResultCode());
        Assert.assertEquals(WPJ_REQUIRED_REDIRECT_URI, result.getAuthorizationFinalUri().toString());
    }

    @Test
    public void testFromBrowserBrokerInstallationRedirectUri() {
        final RawAuthorizationResult result = RawAuthorizationResult.fromRedirectUri(BROKER_INSTALLATION_REQUIRED_BROWSER_REDIRECT_URI);

        Assert.assertEquals(BROKER_INSTALLATION_TRIGGERED, result.getResultCode());
        Assert.assertEquals(BROKER_INSTALLATION_REQUIRED_BROWSER_REDIRECT_URI, result.getAuthorizationFinalUri().toString());
    }

    @Test
    public void testFromWebViewBrokerInstallationRedirectUri() {
        final RawAuthorizationResult result = RawAuthorizationResult.fromRedirectUri(BROKER_INSTALLATION_REQUIRED_WEBVIEW_REDIRECT_URI);

        Assert.assertEquals(BROKER_INSTALLATION_TRIGGERED, result.getResultCode());
        Assert.assertEquals(BROKER_INSTALLATION_REQUIRED_WEBVIEW_REDIRECT_URI, result.getAuthorizationFinalUri().toString());
    }
}
