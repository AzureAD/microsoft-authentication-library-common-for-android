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

package com.microsoft.identity.common.java.commands;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import com.microsoft.identity.common.java.commands.parameters.InteractiveTokenCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.SilentTokenCommandParameters;
import com.microsoft.identity.common.java.interfaces.IPlatformComponents;

import org.junit.Test;

import java.util.Collections;
import java.util.Set;

/**
 * Unit tests for {@link BrokerInstallResumeParamsFactory} — the interactive-&gt;silent parameter
 * projection used to replay a parked request through the broker with {@code login_hint = UPN} (PBI-4).
 */
public class BrokerInstallResumeParamsFactoryTest {

    private static final String UPN = "upn@contoso.com";

    private static InteractiveTokenCommandParameters interactiveParams(final String loginHint) {
        final Set<String> scopes = Collections.singleton("User.Read");
        return InteractiveTokenCommandParameters.builder()
                .platformComponents(mock(IPlatformComponents.class))
                .clientId("client-123")
                .redirectUri("msauth://com.contoso.app/hash")
                .correlationId("cid-abc")
                .applicationName("Outlook")
                .applicationVersion("4.0")
                .forceRefresh(true)
                .scopes(scopes)
                .loginHint(loginHint)
                .build();
    }

    @Test
    public void projectsSharedFields_andSetsLoginHintToUpn() {
        final InteractiveTokenCommandParameters interactive = interactiveParams("old-hint@contoso.com");

        final SilentTokenCommandParameters silent =
                BrokerInstallResumeParamsFactory.toSilentParameters(interactive, UPN);

        assertEquals("client-123", silent.getClientId());
        assertEquals("msauth://com.contoso.app/hash", silent.getRedirectUri());
        assertEquals("cid-abc", silent.getCorrelationId());
        assertEquals("Outlook", silent.getApplicationName());
        assertEquals("4.0", silent.getApplicationVersion());
        assertTrue(silent.isForceRefresh());
        assertTrue(silent.getScopes().contains("User.Read"));
        assertSame(interactive.getPlatformComponents(), silent.getPlatformComponents());
        assertEquals("login_hint must be set to the UPN", UPN, silent.getLoginHint());
    }

    @Test
    public void preservesExistingLoginHint_whenUpnIsNullOrBlank() {
        final InteractiveTokenCommandParameters interactive = interactiveParams("old-hint@contoso.com");

        assertEquals("old-hint@contoso.com",
                BrokerInstallResumeParamsFactory.toSilentParameters(interactive, null).getLoginHint());
        assertEquals("old-hint@contoso.com",
                BrokerInstallResumeParamsFactory.toSilentParameters(interactive, "  ").getLoginHint());
    }

    @Test
    public void producesASilentTokenCommandParameters() {
        final SilentTokenCommandParameters silent =
                BrokerInstallResumeParamsFactory.toSilentParameters(interactiveParams(null), UPN);
        assertTrue(silent instanceof SilentTokenCommandParameters);
    }
}
