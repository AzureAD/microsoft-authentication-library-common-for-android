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
package com.microsoft.identity.common.java.util;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

public class RequestHeaderSerializationUtilTest {

    @Test
    public void testSerializeHeaders_returnsValidString() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer token");
        headers.put("Content-Type", "application/json");

        String serialized = RequestHeaderSerializationUtil.toJson(headers);

        assertNotNull(serialized);
        assertTrue(serialized.contains("Authorization"));
        assertTrue(serialized.contains("Bearer token"));
        assertTrue(serialized.contains("Content-Type"));
    }

    @Test
    public void testDeserializeHeaders_returnsValidMap() {
        String input = "{\"Authorization\":\"Bearer token\",\"Content-Type\":\"application/json\"}";
        Map<String, String> headers = RequestHeaderSerializationUtil.fromJson(input);

        assertNotNull(headers);
        assertEquals("Bearer token", headers.get("Authorization"));
        assertEquals("application/json", headers.get("Content-Type"));
    }

    @Test
    public void testSerializeEmptyHeaders_returnsEmptyString() {
        Map<String, String> headers = new HashMap<>();
        String serialized = RequestHeaderSerializationUtil.toJson(headers);
        assertEquals("{}", serialized);
    }

    @Test
    public void testDeserializeEmptyString_returnsEmptyMap() {
        Map<String, String> headers = RequestHeaderSerializationUtil.fromJson("{}");
        assertTrue(headers.isEmpty());
    }
}
