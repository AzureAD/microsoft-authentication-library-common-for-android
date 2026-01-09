// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

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
