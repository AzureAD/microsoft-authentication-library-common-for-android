/**
 * // Copyright (c) Microsoft Corporation.
 * // All rights reserved.
 * //
 * // This code is licensed under the MIT License.
 * //
 * // Permission is hereby granted, free of charge, to any person obtaining a copy
 * // of this software and associated documentation files(the "Software"), to deal
 * // in the Software without restriction, including without limitation the rights
 * // to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
 * // copies of the Software, and to permit persons to whom the Software is
 * // furnished to do so, subject to the following conditions :
 * //
 * // The above copyright notice and this permission notice shall be included in
 * // all copies or substantial portions of the Software.
 * //
 * // THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * // IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * // FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * // AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * // LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * // OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * // THE SOFTWARE.
 * */

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
package com.microsoft.identity.common.adal.internal.util;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.microsoft.identity.common.adal.internal.AndroidTestHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;

import static com.microsoft.identity.common.adal.internal.util.HashMapExtensions.urlFormDecode;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class HashMapExtensionTests extends AndroidTestHelper {
    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testURLFormDecodeNegative() {
        HashMap<String, String> result = urlFormDecode("");
        assertNotNull(result);
        assertTrue(result.isEmpty());

        result = urlFormDecode("&&&");
        assertNotNull(result);
        assertTrue(result.isEmpty());

        result = urlFormDecode("=&=");
        assertNotNull(result);
        assertTrue(result.isEmpty());

        result = urlFormDecode("=&");
        assertNotNull(result);
        assertTrue(result.isEmpty());

        result = urlFormDecode("&=");
        assertNotNull(result);
        assertTrue(result.isEmpty());

        result = urlFormDecode("&=b");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testURLFormDecodePositive() {
        HashMap<String, String> result = urlFormDecode("a=b&c=2");
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.containsKey("a"));
        assertTrue(result.containsKey("c"));
        assertTrue(result.containsValue("b"));
        assertTrue(result.containsValue("2"));

        assertTrue(result.containsKey("a"));
        assertTrue(result.containsKey("c"));
        assertTrue(result.containsValue("b"));
        assertTrue(result.containsValue("2"));

        result = urlFormDecode("a=v");
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.containsKey("a"));
        assertTrue(result.containsValue("v"));

        result = urlFormDecode("d=f&");
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.containsKey("d"));
        assertTrue(result.containsValue("f"));
        assertEquals(1, result.size());

        result = urlFormDecode("=b&c=");
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.containsKey("c"));
        assertFalse(result.containsValue("b"));
        assertEquals(1, result.size());
    }
}
