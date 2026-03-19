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
package com.microsoft.identity.common.java.authscheme;

import com.microsoft.identity.common.java.base64.Base64Util;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class WebAppsPopAuthenticationSchemeInternalTest {

    // Base64url-encoded req_cnf with kid: {"kid":"test-kid-1"}
    private static final String REQ_CNF_ONE = Base64Util.encodeUrlSafeString("{\"kid\":\"test-kid-1\"}".getBytes());

    // Base64url-encoded req_cnf with kid: {"kid":"test-kid-1"} (same as ONE)
    private static final String REQ_CNF_ONE_CLONE = Base64Util.encodeUrlSafeString("{\"kid\":\"test-kid-1\"}".getBytes());

    // Base64url-encoded req_cnf with kid: {"kid":"test-kid-2"}
    private static final String REQ_CNF_TWO = Base64Util.encodeUrlSafeString("{\"kid\":\"test-kid-2\"}".getBytes());

    private static final WebAppsPopAuthenticationSchemeInternal AUTHSCHEME_ONE =
            new WebAppsPopAuthenticationSchemeInternal(REQ_CNF_ONE);

    private static final WebAppsPopAuthenticationSchemeInternal AUTHSCHEME_ONE_CLONE =
            new WebAppsPopAuthenticationSchemeInternal(REQ_CNF_ONE_CLONE);

    private static final WebAppsPopAuthenticationSchemeInternal AUTHSCHEME_TWO =
            new WebAppsPopAuthenticationSchemeInternal(REQ_CNF_TWO);

    @Test
    public void testConstructor_validRequestConfirmation() {
        final String reqCnf = Base64Util.encodeUrlSafeString("{\"kid\":\"my-key-id\"}".getBytes());
        final WebAppsPopAuthenticationSchemeInternal scheme = new WebAppsPopAuthenticationSchemeInternal(reqCnf);

        Assert.assertNotNull(scheme);
        Assert.assertEquals(reqCnf, scheme.getRequestConfirmation());
        Assert.assertEquals("my-key-id", scheme.getKid());
        Assert.assertEquals(WebAppsPopAuthenticationSchemeInternal.SCHEME_POP_PREGENERATED, scheme.getName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_emptyRequestConfirmation() {
        new WebAppsPopAuthenticationSchemeInternal("");
    }

    @Test
    public void testConstructor_invalidBase64() {
        // Invalid base64 should not throw, but kid will be null
        final WebAppsPopAuthenticationSchemeInternal scheme =
                new WebAppsPopAuthenticationSchemeInternal("not-valid-base64!@#$");

        Assert.assertNotNull(scheme);
        Assert.assertEquals("not-valid-base64!@#$", scheme.getRequestConfirmation());
        Assert.assertNull(scheme.getKid());
    }

    @Test
    public void testConstructor_invalidJson() {
        // Invalid JSON should not throw, but kid will be null
        final String invalidJson = Base64Util.encodeUrlSafeString("{invalid json}".getBytes());
        final WebAppsPopAuthenticationSchemeInternal scheme =
                new WebAppsPopAuthenticationSchemeInternal(invalidJson);

        Assert.assertNotNull(scheme);
        Assert.assertEquals(invalidJson, scheme.getRequestConfirmation());
        Assert.assertNull(scheme.getKid());
    }

    @Test
    public void testConstructor_reqCnfWithoutKid() {
        final String reqCnfWithoutKid = Base64Util.encodeUrlSafeString("{\"other\":\"value\"}".getBytes());
        final WebAppsPopAuthenticationSchemeInternal scheme = new WebAppsPopAuthenticationSchemeInternal(reqCnfWithoutKid);

        Assert.assertNotNull(scheme);
        Assert.assertEquals(reqCnfWithoutKid, scheme.getRequestConfirmation());
        Assert.assertNull(scheme.getKid());
    }

    @Test
    public void testGetAccessTokenForScheme() {
        final String accessToken = "test-access-token";
        final String result = AUTHSCHEME_ONE.getAccessTokenForScheme(accessToken);

        Assert.assertEquals("Access token should be returned as-is", accessToken, result);
    }

    @Test
    public void testMappability() {
        Map<WebAppsPopAuthenticationSchemeInternal, Boolean> testMap = new HashMap<>();

        testMap.put(AUTHSCHEME_ONE, true);
        Assert.assertEquals(1, testMap.size());
        testMap.put(AUTHSCHEME_TWO, true);
        Assert.assertEquals(2, testMap.size());
    }

    @Test
    public void testHashCode_equals() {
        Assert.assertEquals(AUTHSCHEME_ONE.hashCode(), AUTHSCHEME_ONE_CLONE.hashCode());
    }

    @Test
    public void testHashCode_notEquals() {
        Assert.assertNotEquals(AUTHSCHEME_ONE.hashCode(), AUTHSCHEME_TWO.hashCode());
    }

    @Test
    public void testEquals_equals() {
        Assert.assertEquals(AUTHSCHEME_ONE, AUTHSCHEME_ONE_CLONE);
    }

    @Test
    public void testEquals_notEqualNull() {
        Assert.assertNotEquals(AUTHSCHEME_ONE, null);
    }

    @Test
    public void testEquals_equalsSame() {
        Assert.assertEquals(AUTHSCHEME_ONE, AUTHSCHEME_ONE);
    }

    @Test
    public void testEquals_notEqualDifferenceInKid() {
        Assert.assertNotEquals(AUTHSCHEME_ONE, AUTHSCHEME_TWO);
    }

    @Test
    public void testEquals_notEqualDifferentType() {
        Assert.assertNotEquals(AUTHSCHEME_ONE, new BearerAuthenticationSchemeInternal());
    }

    @Test
    public void testSchemeName() {
        Assert.assertEquals(WebAppsPopAuthenticationSchemeInternal.SCHEME_POP_PREGENERATED,
                AUTHSCHEME_ONE.getName());
    }

    @Test
    public void testBase64UrlDecoding() {
        // Test with various req_cnf values including special characters
        final String jsonWithSpecialChars = "{\"kid\":\"test+kid/with=special\"}";
        final String reqCnf = Base64Util.encodeUrlSafeString(jsonWithSpecialChars.getBytes());
        final WebAppsPopAuthenticationSchemeInternal scheme = new WebAppsPopAuthenticationSchemeInternal(reqCnf);

        Assert.assertNotNull(scheme);
        Assert.assertEquals("test+kid/with=special", scheme.getKid());
    }
}
