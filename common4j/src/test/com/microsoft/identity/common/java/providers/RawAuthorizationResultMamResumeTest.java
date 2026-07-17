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

package com.microsoft.identity.common.java.providers;

import static com.microsoft.identity.common.java.providers.RawAuthorizationResult.ResultCode.BROKER_INSTALLATION_TRIGGERED;
import static com.microsoft.identity.common.java.providers.RawAuthorizationResult.ResultCode.BROKER_INSTALL_RESUME;
import static com.microsoft.identity.common.java.providers.RawAuthorizationResult.ResultCode.COMPLETED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * Unit tests for the MAM broker-install resume detection in {@link RawAuthorizationResult} (PBI-3):
 * classifying a {@code mam_resume=<cid>} redirect and extracting the parked correlation id.
 */
public class RawAuthorizationResultMamResumeTest {

    private static final String CID = "3f2504e0-4f89-11d3-9a0c-0305e82c3301";
    private static final String APP_LINK_ENCODED =
            "https%3a%2f%2fplay.google.com%2fstore%2fapps%2fdetails%3fid%3dcom.microsoft.windowsintune.companyportal";

    @Test
    public void mamResumeRedirect_isClassifiedAsBrokerInstallResume() {
        final String redirect = "msauth://com.contoso.app/signaturehash?mam_resume=" + CID;

        final RawAuthorizationResult result = RawAuthorizationResult.fromRedirectUri(redirect);

        assertEquals(BROKER_INSTALL_RESUME, result.getResultCode());
    }

    @Test
    public void getMamResumeCorrelationId_returnsTheCid() {
        final String redirect = "msauth://com.contoso.app/signaturehash?mam_resume=" + CID;

        final RawAuthorizationResult result = RawAuthorizationResult.fromRedirectUri(redirect);

        assertEquals(CID, result.getMamResumeCorrelationId());
    }

    @Test
    public void mamResume_takesPrecedenceOverAppLink() {
        // Defensive: if a redirect ever carried both, the resume branch wins.
        final String redirect = "msauth://com.contoso.app/signaturehash?mam_resume=" + CID
                + "&app_link=" + APP_LINK_ENCODED;

        final RawAuthorizationResult result = RawAuthorizationResult.fromRedirectUri(redirect);

        assertEquals(BROKER_INSTALL_RESUME, result.getResultCode());
        assertEquals(CID, result.getMamResumeCorrelationId());
    }

    @Test
    public void installRequiredRedirect_withoutMamResume_stillClassifiedAsInstallTriggered() {
        // Regression: the pre-existing broker-install-required classification is unchanged.
        final String redirect = "msauth://com.contoso.app/signaturehash?username=idlab1%40msidlab4.onmicrosoft.com"
                + "&app_link=" + APP_LINK_ENCODED;

        final RawAuthorizationResult result = RawAuthorizationResult.fromRedirectUri(redirect);

        assertEquals(BROKER_INSTALLATION_TRIGGERED, result.getResultCode());
        assertNull("a non-resume redirect has no mam_resume cid", result.getMamResumeCorrelationId());
    }

    @Test
    public void mamResumeParam_onNonMsauthScheme_isNotClassifiedAsResume() {
        // The discriminator is scoped to the msauth redirect scheme.
        final String redirect = "https://contoso.com/auth?mam_resume=" + CID;

        final RawAuthorizationResult result = RawAuthorizationResult.fromRedirectUri(redirect);

        assertEquals(COMPLETED, result.getResultCode());
    }

    @Test
    public void completedRedirect_hasNoMamResumeCid() {
        final RawAuthorizationResult result =
                RawAuthorizationResult.fromRedirectUri("msauth://com.contoso.app/signaturehash?code=abc");

        assertEquals(COMPLETED, result.getResultCode());
        assertNull(result.getMamResumeCorrelationId());
    }
}
