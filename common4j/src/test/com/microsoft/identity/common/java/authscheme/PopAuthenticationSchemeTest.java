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

import com.microsoft.identity.common.java.util.UrlUtil;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class PopAuthenticationSchemeTest {

    public static final PopAuthenticationSchemeInternal AUTHSCHEME_ONE =
            PopAuthenticationSchemeInternal.builder()
                    .httpMethod("GET")
                    .nonce("one")
                    .url(UrlUtil.makeUrlSilent("http://url"))
                    .build();
    public static final PopAuthenticationSchemeInternal AUTHSCHEME_ONE_CLONE =
            PopAuthenticationSchemeInternal.builder()
                    .httpMethod("GET")
                    .nonce("one")
                    .url(UrlUtil.makeUrlSilent("http://url"))
                    .build();
    public static final PopAuthenticationSchemeInternal AUTHSCHEME_TWO =
            PopAuthenticationSchemeInternal.builder()
                    .httpMethod("GET")
                    .url(UrlUtil.makeUrlSilent("http://url"))
                    .nonce("two")
                    .build();

    @Test
    public void testMappability() throws Exception {
        Map<PopAuthenticationSchemeInternal, Boolean> testMap = new HashMap<>();

        testMap.put(AUTHSCHEME_ONE, true);
        Assert.assertEquals(1, testMap.size());
        testMap.put(AUTHSCHEME_TWO, true);
        Assert.assertEquals(2, testMap.size());
    }

    @Test
    public void testHashCode_equals() throws Exception {
        Assert.assertEquals(AUTHSCHEME_ONE.hashCode(), AUTHSCHEME_ONE_CLONE.hashCode());
    }

    @Test
    public void testHashCode_notEquals() throws Exception {
        Assert.assertNotEquals(AUTHSCHEME_ONE.hashCode(), AUTHSCHEME_TWO.hashCode());
    }

    @Test
    public void testEquals_equals() throws Exception {
        Assert.assertEquals(AUTHSCHEME_ONE, AUTHSCHEME_ONE_CLONE);
    }

    @Test
    public void testEquals_notEqualNull() throws Exception {
        Assert.assertNotEquals(AUTHSCHEME_ONE, null);
    }

    @Test
    public void testEquals_equalsSame() throws Exception {
        Assert.assertEquals(AUTHSCHEME_ONE, AUTHSCHEME_ONE);
    }

    @Test
    public void testEquals_notEqualDifferenceInNonce() {
        Assert.assertNotEquals(AUTHSCHEME_ONE, AUTHSCHEME_TWO);
    }
}
