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

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class StringUtilTest {

    @Test
    public void isNullOrEmpty_nullBlankAndNonBlank() {
        Assert.assertTrue(StringUtil.isNullOrEmpty(null));
        Assert.assertTrue(StringUtil.isNullOrEmpty(""));
        Assert.assertTrue(StringUtil.isNullOrEmpty("   "));
        Assert.assertFalse(StringUtil.isNullOrEmpty("abc"));
    }

    @Test
    public void containsSubString_nullEmptyAndMatching() {
        Assert.assertFalse(StringUtil.containsSubString(null, "a"));
        Assert.assertFalse(StringUtil.containsSubString("", "a"));
        Assert.assertTrue(StringUtil.containsSubString("hello world", "world"));
        Assert.assertFalse(StringUtil.containsSubString("hello", "xyz"));
    }

    @Test
    public void equalsIgnoreCase_variants() {
        Assert.assertTrue(StringUtil.equalsIgnoreCase(null, null));
        Assert.assertTrue(StringUtil.equalsIgnoreCase("ABC", "abc"));
        Assert.assertFalse(StringUtil.equalsIgnoreCase("ABC", null));
        Assert.assertFalse(StringUtil.equalsIgnoreCase(null, "abc"));
        Assert.assertFalse(StringUtil.equalsIgnoreCase("abc", "abd"));
    }

    @Test
    public void getTenantInfo_validSplit() {
        final Map.Entry<String, String> result = StringUtil.getTenantInfo("uid.utid");
        Assert.assertEquals("uid", result.getKey());
        Assert.assertEquals("utid", result.getValue());
    }

    @Test
    public void getTenantInfo_unsplittableReturnsNullPair() {
        final Map.Entry<String, String> result = StringUtil.getTenantInfo("nodot");
        Assert.assertNull(result.getKey());
        Assert.assertNull(result.getValue());
    }

    @Test
    public void getUIdFromHomeAccountId_tenanted() {
        Assert.assertEquals("uid", StringUtil.getUIdFromHomeAccountId("uid.utid"));
    }

    @Test
    public void getUIdFromHomeAccountId_v1SinglePart() {
        Assert.assertEquals("uid", StringUtil.getUIdFromHomeAccountId("uid"));
    }

    @Test
    public void getUIdFromHomeAccountId_nullAndMalformed() {
        Assert.assertNull(StringUtil.getUIdFromHomeAccountId(null));
        Assert.assertNull(StringUtil.getUIdFromHomeAccountId("a.b.c"));
    }

    @Test
    public void hasPrefixInHeader_trueAndFalse() {
        Assert.assertTrue(StringUtil.hasPrefixInHeader("Bearer token", "Bearer"));
        Assert.assertFalse(StringUtil.hasPrefixInHeader("Bearer", "Bearer"));
    }

    @Test
    public void getStringTokens_skipsEmpty() {
        final List<String> tokens = StringUtil.getStringTokens("a,b,,c", ",");
        Assert.assertEquals(Arrays.asList("a", "b", "c"), tokens);
    }

    @Test
    public void splitWithQuotes_respectsQuotedSegments() {
        final ArrayList<String> items = StringUtil.splitWithQuotes("a,\"b,c\",d", ',');
        Assert.assertEquals(3, items.size());
        Assert.assertEquals("a", items.get(0));
        Assert.assertEquals("\"b,c\"", items.get(1));
        Assert.assertEquals("d", items.get(2));
    }

    @Test
    public void removeQuoteInHeaderValue_stripsQuotesAndHandlesNull() {
        Assert.assertEquals("abc", StringUtil.removeQuoteInHeaderValue("\"abc\""));
        Assert.assertNull(StringUtil.removeQuoteInHeaderValue(null));
    }

    @Test
    public void urlFormEncodeDecode_roundTrip() throws Exception {
        final String raw = "a b&c=d";
        final String encoded = StringUtil.urlFormEncode(raw);
        Assert.assertEquals(raw, StringUtil.urlFormDecode(encoded));
        Assert.assertEquals("", StringUtil.urlFormDecode(""));
    }

    @Test
    public void byteArrayRoundTrip() {
        final byte[] bytes = StringUtil.toByteArray("hello");
        Assert.assertEquals("hello", StringUtil.fromByteArray(bytes));
    }

    @Test
    public void rfc3339DateRoundTrip() throws Exception {
        final Date original = new Date(0L);
        final String asString = StringUtil.RFC3339DateToString(original);
        Assert.assertEquals("1970-01-01T00:00:00Z", asString);
        Assert.assertEquals(original, StringUtil.RFC3339StringToDate(asString));
    }

    @Test
    public void isUuid_validAndInvalid() {
        Assert.assertTrue(StringUtil.isUuid("00000000-0000-0000-0000-000000000000"));
        Assert.assertFalse(StringUtil.isUuid("not-a-uuid"));
    }

    @Test
    public void createHash_nonEmptyAndPassthrough() throws Exception {
        Assert.assertNotNull(StringUtil.createHash("message"));
        Assert.assertEquals("", StringUtil.createHash(""));
    }

    @Test
    public void equalsIgnoreCaseTrim_variants() {
        Assert.assertTrue(StringUtil.equalsIgnoreCaseTrim("abc", "  abc  "));
        Assert.assertTrue(StringUtil.equalsIgnoreCaseTrim(null, null));
        Assert.assertFalse(StringUtil.equalsIgnoreCaseTrim("abc", null));
    }

    @Test
    public void equalsIgnoreCaseTrimBoth_trimsLeftInput() {
        Assert.assertTrue(StringUtil.equalsIgnoreCaseTrimBoth("  ABC  ", "abc"));
    }

    @Test
    public void sanitizeNull_returnsEmptyForNull() {
        Assert.assertEquals("", StringUtil.sanitizeNull(null));
        Assert.assertEquals("x", StringUtil.sanitizeNull("x"));
    }

    @Test
    public void sanitizeNullAndLowercaseAndTrim_normalizes() {
        Assert.assertEquals("", StringUtil.sanitizeNullAndLowercaseAndTrim(null));
        Assert.assertEquals("abc", StringUtil.sanitizeNullAndLowercaseAndTrim("  ABC  "));
    }

    @Test
    public void join_emptySingleAndMultiple() {
        Assert.assertEquals("", StringUtil.join(",", new ArrayList<String>()));
        Assert.assertEquals("only", StringUtil.join(",", Arrays.asList("only")));
        Assert.assertEquals("a,b,c", StringUtil.join(",", Arrays.asList("a", "b", "c")));
    }

    @Test(expected = NullPointerException.class)
    public void throwIfArgumentIsNullOrEmpty_throwsOnEmpty() {
        StringUtil.throwIfArgumentIsNullOrEmpty("", "arg", "tag");
    }

    @Test
    public void throwIfArgumentIsNullOrEmpty_passesOnNonEmpty() {
        StringUtil.throwIfArgumentIsNullOrEmpty("value", "arg", "tag");
    }

    @Test
    public void overwriteWithNull_zeroesOutAndHandlesNull() {
        final char[] chars = {'a', 'b', 'c'};
        StringUtil.overwriteWithNull(chars);
        Assert.assertArrayEquals(new char[]{'\0', '\0', '\0'}, chars);
        StringUtil.overwriteWithNull(null);
    }

    @Test
    public void getStacktraceAsStringFromElementArray_joinsWithNewline() {
        final StackTraceElement[] elements = new Throwable().getStackTrace();
        final String result = StringUtil.getStacktraceAsStringFromElementArray(elements);
        Assert.assertNotNull(result);
        Assert.assertTrue(result.contains(elements[0].toString()));
    }
}
