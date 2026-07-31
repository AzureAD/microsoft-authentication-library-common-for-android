//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.

package com.microsoft.identity.common.java.exception;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for {@link BrokerInstallationRequiredException} — carries the WPJ username (UPN) and the
 * install link for the MAM broker-install request-resume engine.
 */
public class BrokerInstallationRequiredExceptionTest {

    private static final String ERROR = "broker_needs_to_be_installed";
    private static final String DESCRIPTION = "Device needs to have broker installed";
    private static final String UPN = "idlab1@msidlab4.onmicrosoft.com";
    private static final String INSTALL_LINK =
            "https://play.google.com/store/apps/details?id=com.microsoft.windowsintune.companyportal";

    @Test
    public void carriesErrorCodeDescriptionUpnAndInstallLink() {
        final BrokerInstallationRequiredException ex =
                new BrokerInstallationRequiredException(ERROR, DESCRIPTION, UPN, INSTALL_LINK);

        assertEquals(ERROR, ex.getErrorCode());
        assertEquals(DESCRIPTION, ex.getMessage());
        assertEquals("UPN is carried via BaseException.username for use as login_hint on resume",
                UPN, ex.getUsername());
        assertEquals(INSTALL_LINK, ex.getInstallLink());
    }

    @Test
    public void isABaseException() {
        final BaseException ex =
                new BrokerInstallationRequiredException(ERROR, DESCRIPTION, UPN, INSTALL_LINK);
        assertTrue(ex instanceof BrokerInstallationRequiredException);
    }

    @Test
    public void allowsNullUpnAndInstallLink() {
        final BrokerInstallationRequiredException ex =
                new BrokerInstallationRequiredException(ERROR, DESCRIPTION, null, null);
        assertNull(ex.getUsername());
        assertNull(ex.getInstallLink());
        assertEquals(ERROR, ex.getErrorCode());
    }

    @Test
    public void exceptionName_isStableForSerialization() {
        final BrokerInstallationRequiredException ex =
                new BrokerInstallationRequiredException(ERROR, DESCRIPTION, UPN, INSTALL_LINK);
        assertEquals(BrokerInstallationRequiredException.sName, ex.getExceptionName());
        assertEquals("com.microsoft.identity.common.exception.BrokerInstallationRequiredException",
                ex.getExceptionName());
    }
}
