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
package com.microsoft.identity.common.internal.providers.oauth2;

import static com.microsoft.identity.common.internal.providers.oauth2.WebViewAuthorizationFragment.getAuthorityHostFromRequestUrl;
import static com.microsoft.identity.common.internal.providers.oauth2.WebViewAuthorizationFragment.getClientIdFromRequestUrl;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/**
 * Tests for the two request-URL readers on {@link WebViewAuthorizationFragment}. They supply the
 * client id and authority host that scope and bind state captured mid-flow, so they have to fail
 * soft: a URL they cannot make sense of must yield null rather than throw, because nothing here is
 * worth failing a sign-in over.
 */
@RunWith(RobolectricTestRunner.class)
public class WebViewAuthorizationFragmentUrlHelpersTest {

    private static final String CLIENT_ID = "4b0db8c2-9f26-4417-8bde-3f0e3656f8e0";
    private static final String AUTHORIZE_URL =
            "https://login.microsoftonline.com/common/oauth2/v2.0/authorize"
                    + "?client_id=" + CLIENT_ID
                    + "&response_type=code"
                    + "&redirect_uri=msauth%3A%2F%2Fcom.contoso.app%2Fabc%253D";

    @Test
    public void testGetClientId_readsItOffTheRequestUrl() {
        assertEquals(CLIENT_ID, getClientIdFromRequestUrl(AUTHORIZE_URL));
    }

    @Test
    public void testGetClientId_urlWithoutTheParameter_isNull() {
        assertNull(getClientIdFromRequestUrl(
                "https://login.microsoftonline.com/common/oauth2/v2.0/authorize?response_type=code"));
    }

    @Test
    public void testGetClientId_urlWithNoQueryAtAll_isNull() {
        assertNull(getClientIdFromRequestUrl("https://login.microsoftonline.com/common"));
    }

    @Test
    public void testGetClientId_nullOrBlankUrl_isNull() {
        assertNull(getClientIdFromRequestUrl(null));
        assertNull(getClientIdFromRequestUrl(""));
        assertNull(getClientIdFromRequestUrl("   "));
    }

    /**
     * {@code Uri.getQueryParameter} throws on an opaque URI (one with no {@code //} authority).
     * That must be swallowed - a request we cannot classify is not a reason to fail sign-in.
     */
    @Test
    public void testGetClientId_opaqueUri_isNullAndDoesNotThrow() {
        assertNull(getClientIdFromRequestUrl("mailto:someone@contoso.com?client_id=" + CLIENT_ID));
    }

    @Test
    public void testGetAuthorityHost_readsItOffTheRequestUrl() {
        assertEquals("login.microsoftonline.com", getAuthorityHostFromRequestUrl(AUTHORIZE_URL));
    }

    /**
     * The host is what binds a captured hint to its cloud, so a sovereign-cloud authority has to
     * read back as that cloud's host and not the commercial one.
     */
    @Test
    public void testGetAuthorityHost_sovereignCloud_readsThatCloudsHost() {
        assertEquals("login.microsoftonline.us", getAuthorityHostFromRequestUrl(
                "https://login.microsoftonline.us/common/oauth2/v2.0/authorize?client_id=" + CLIENT_ID));
    }

    @Test
    public void testGetAuthorityHost_nullOrBlankUrl_isNull() {
        assertNull(getAuthorityHostFromRequestUrl(null));
        assertNull(getAuthorityHostFromRequestUrl(""));
        assertNull(getAuthorityHostFromRequestUrl("   "));
    }

    @Test
    public void testGetAuthorityHost_uriWithNoHost_isNull() {
        assertNull(getAuthorityHostFromRequestUrl("mailto:someone@contoso.com"));
    }

    @Test
    public void testGetAuthorityHost_notAUrl_isNullAndDoesNotThrow() {
        assertNull(getAuthorityHostFromRequestUrl("not a url at all"));
    }
}
