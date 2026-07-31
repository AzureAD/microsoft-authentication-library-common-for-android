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
package com.microsoft.identity.common.java.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

public class StringUtilTest {

    @Test
    public void testIsUuidForValidAndInvalidInput() {
        assertTrue(StringUtil.isUuid("12345678-1234-1234-1234-123456789012"));
        assertFalse(StringUtil.isUuid("not-a-uuid"));
    }

    @Test
    public void testJoin() {
        assertEquals("", StringUtil.join(",", Collections.<String>emptyList()));
        assertEquals("only", StringUtil.join(",", Collections.singletonList("only")));
        assertEquals("a,b,c", StringUtil.join(",", Arrays.asList("a", "b", "c")));
    }

    @Test
    public void testGetTenantInfoWithValidHomeAccountId() {
        final Map.Entry<String, String> info = StringUtil.getTenantInfo("uid-value.utid-value");
        assertEquals("uid-value", info.getKey());
        assertEquals("utid-value", info.getValue());
    }

    @Test
    public void testGetTenantInfoWithMalformedHomeAccountIdReturnsNullParts() {
        final Map.Entry<String, String> info = StringUtil.getTenantInfo("no-delimiter");
        assertNull(info.getKey());
        assertNull(info.getValue());
    }

    @Test
    public void testGetUIdFromHomeAccountId() {
        // V2 tenanted format [uid].[utid] -> uid
        assertEquals("uid-value", StringUtil.getUIdFromHomeAccountId("uid-value.utid-value"));
        // V1 format (uid only) -> uid
        assertEquals("uid-only", StringUtil.getUIdFromHomeAccountId("uid-only"));
        // null input -> null
        assertNull(StringUtil.getUIdFromHomeAccountId(null));
        // Unexpected number of parts -> null
        assertNull(StringUtil.getUIdFromHomeAccountId("a.b.c"));
    }

    @Test
    public void testRfc3339DateRoundTrips() throws ParseException {
        // Milliseconds are a multiple of 1000 so the value round-trips exactly.
        final Date original = new Date(1600000000000L);
        final String asString = StringUtil.RFC3339DateToString(original);
        assertEquals(original, StringUtil.RFC3339StringToDate(asString));
    }

    @Test
    public void testThrowIfArgumentIsNullOrEmptyThrowsForNullAndEmpty() {
        try {
            StringUtil.throwIfArgumentIsNullOrEmpty(null, "arg", "tag");
            fail("Expected NullPointerException for null argument");
        } catch (final NullPointerException expected) {
            // expected
        }

        try {
            StringUtil.throwIfArgumentIsNullOrEmpty("", "arg", "tag");
            fail("Expected NullPointerException for empty argument");
        } catch (final NullPointerException expected) {
            // expected
        }
    }

    @Test
    public void testThrowIfArgumentIsNullOrEmptyPassesForNonEmpty() {
        // Should not throw.
        StringUtil.throwIfArgumentIsNullOrEmpty("value", "arg", "tag");
    }
}
