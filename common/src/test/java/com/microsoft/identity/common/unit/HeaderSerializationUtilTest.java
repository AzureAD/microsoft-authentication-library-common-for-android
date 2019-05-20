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
package com.microsoft.identity.common.unit;

import com.microsoft.identity.common.internal.util.HeaderSerializationUtil;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.microsoft.identity.common.unit.HeaderSerializationUtilTest.HeaderNames.ACCESS_CONTROL_ALLOW_METHODS;
import static com.microsoft.identity.common.unit.HeaderSerializationUtilTest.HeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN;
import static com.microsoft.identity.common.unit.HeaderSerializationUtilTest.HeaderNames.CACHE_CONTROL;
import static com.microsoft.identity.common.unit.HeaderSerializationUtilTest.HeaderNames.CONTENT_TYPE;
import static com.microsoft.identity.common.unit.HeaderSerializationUtilTest.HeaderNames.SERVER;
import static org.junit.Assert.assertEquals;

@RunWith(JUnit4.class)
public class HeaderSerializationUtilTest {

    static class HeaderNames {
        static final String CACHE_CONTROL = "Cache-Control";
        static final String CONTENT_TYPE = "Content-Type";
        static final String SERVER = "Server";
        static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
        static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
    }

    @Test
    public void test_serialization() {
        final Map<String, List<String>> testHeaders = new HashMap<>();

        // Initialize the containers
        testHeaders.put(CACHE_CONTROL, new ArrayList<String>());
        testHeaders.put(CONTENT_TYPE, new ArrayList<String>());
        testHeaders.put(SERVER, new ArrayList<String>());
        testHeaders.put(ACCESS_CONTROL_ALLOW_ORIGIN, new ArrayList<String>());
        testHeaders.put(ACCESS_CONTROL_ALLOW_METHODS, new ArrayList<String>());

        // Populate some sample data...
        testHeaders.get(CACHE_CONTROL).add("private");
        testHeaders.get(CACHE_CONTROL).add("max-age=86400");

        testHeaders.get(CONTENT_TYPE).add("application/json");
        testHeaders.get(CONTENT_TYPE).add("charset=utf-8");

        testHeaders.get(SERVER).add("Microsoft-IIS/10.0");

        testHeaders.get(ACCESS_CONTROL_ALLOW_ORIGIN).add("*");

        testHeaders.get(ACCESS_CONTROL_ALLOW_METHODS).add("GET");
        testHeaders.get(ACCESS_CONTROL_ALLOW_METHODS).add("OPTIONS");

        final String json = HeaderSerializationUtil.toJson(testHeaders);

        final Map<String, List<String>> reserializedMap = HeaderSerializationUtil.fromJson(json);

        assertEquals(
                testHeaders.size(),
                reserializedMap.size()
        );

        assertEquals(
                testHeaders.get(CACHE_CONTROL).size(),
                reserializedMap.get(CACHE_CONTROL).size()
        );

        assertEquals(
                testHeaders.get(CONTENT_TYPE).size(),
                reserializedMap.get(CONTENT_TYPE).size()
        );

        assertEquals(
                testHeaders.get(SERVER).size(),
                reserializedMap.get(SERVER).size()
        );

        assertEquals(
                testHeaders.get(ACCESS_CONTROL_ALLOW_ORIGIN).size(),
                reserializedMap.get(ACCESS_CONTROL_ALLOW_ORIGIN).size()
        );

        assertEquals(
                testHeaders.get(ACCESS_CONTROL_ALLOW_METHODS).size(),
                reserializedMap.get(ACCESS_CONTROL_ALLOW_METHODS).size()
        );
    }
}
