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
package com.microsoft.identity.common.java.crypto;

import com.microsoft.identity.common.java.crypto.key.KeyUtil;

import org.junit.Assert;
import org.junit.Test;

import javax.crypto.SecretKey;

import static com.microsoft.identity.common.java.crypto.MockData.PREDEFINED_KEY;
import static com.microsoft.identity.common.java.crypto.MockData.PREDEFINED_KEY_SLIGHTLY_MODIFIED;
import static com.microsoft.identity.common.java.crypto.MockData.ANDROID_WRAPPED_KEY;

/**
 * Tests for {@link KeyUtil}
 */
public class KeyUtilTest {

    @Test
    public void testThumbprintShouldBeSame() {
        final MockAES256KeyLoader firstKeyLoader = new MockAES256KeyLoader(PREDEFINED_KEY, "KEY_1");
        final SecretKey firstKey = firstKeyLoader.getKey();

        final String firstThumbPrint = KeyUtil.getKeyThumbPrint(firstKey);
        final String secondThumbPrint = KeyUtil.getKeyThumbPrint(firstKey);
        final String thumbPrintFromLoader = KeyUtil.getKeyThumbPrint(firstKeyLoader);

        // No randomness should occur. Same input should yield the same output.
        Assert.assertEquals(firstThumbPrint, secondThumbPrint);
        Assert.assertEquals(firstThumbPrint, thumbPrintFromLoader);
    }

    @Test
    public void testThumbprintShouldDifferForDifferentKeys() {
        final MockAES256KeyLoader firstKeyLoader = new MockAES256KeyLoader(PREDEFINED_KEY, "KEY_1");
        final SecretKey firstKey = firstKeyLoader.getKey();

        final MockAES256KeyLoader secondKeyLoader = new MockAES256KeyLoader(ANDROID_WRAPPED_KEY, "KEY_2");
        final SecretKey secondKey = secondKeyLoader.getKey();

        final String firstKeyThumbPrint = KeyUtil.getKeyThumbPrint(firstKey);
        final String secondKeyThumbPrint = KeyUtil.getKeyThumbPrint(secondKey);

        // Keys from different set of rawbytes should not yield the same result.
        Assert.assertNotEquals(firstKeyThumbPrint, secondKeyThumbPrint);
    }

    @Test
    public void testThumbprintShouldDifferSlightlyDifferentKeys() {
        final MockAES256KeyLoader firstKeyLoader = new MockAES256KeyLoader(PREDEFINED_KEY, "KEY_1");
        final SecretKey firstKey = firstKeyLoader.getKey();

        final MockAES256KeyLoader secondKeyLoader = new MockAES256KeyLoader(PREDEFINED_KEY_SLIGHTLY_MODIFIED, "KEY_2");
        final SecretKey secondKey = secondKeyLoader.getKey();

        final String firstKeyThumbPrint = KeyUtil.getKeyThumbPrint(firstKey);
        final String secondKeyThumbPrint = KeyUtil.getKeyThumbPrint(secondKey);

        // Load with a slightly modified version of the first key. thumbprint should differs.
        Assert.assertNotEquals(firstKeyThumbPrint, secondKeyThumbPrint);
    }


    @Test
    public void testThumbprintForSameRawKeyButDifferentKeyObjectShouldBeSame() {
        final MockAES256KeyLoader keyLoader = new MockAES256KeyLoader(PREDEFINED_KEY, "KEY_1");
        final SecretKey key = keyLoader.getKey();

        final MockAES256KeyLoader anotherKeyLoader = new MockAES256KeyLoader(PREDEFINED_KEY, "KEY_1");
        final SecretKey anotherKey = anotherKeyLoader.getKey();

        final String thumbPrint = KeyUtil.getKeyThumbPrint(key);
        final String anotherThumbPrint = KeyUtil.getKeyThumbPrint(anotherKey);
        final String thumbPrintFromLoader = KeyUtil.getKeyThumbPrint(keyLoader);
        final String anotherThumbPrintFromLoader = KeyUtil.getKeyThumbPrint(anotherKeyLoader);

        // Since They're coming from the same rawbytes, they should yield the same thumbprint.
        Assert.assertEquals(thumbPrint, anotherThumbPrint);
        Assert.assertEquals(thumbPrint, thumbPrintFromLoader);
        Assert.assertEquals(thumbPrint, anotherThumbPrintFromLoader);
    }
}
