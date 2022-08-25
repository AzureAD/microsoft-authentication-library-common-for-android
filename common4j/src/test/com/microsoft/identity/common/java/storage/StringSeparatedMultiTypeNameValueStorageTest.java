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
package com.microsoft.identity.common.java.storage;

import com.microsoft.identity.common.components.SettablePlatformComponents;
import com.microsoft.identity.common.java.interfaces.IPerSeparatorMultiTypeNameValueStorage;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

/**
 * Tests for {@link StringSeparatedMultiTypeNameValueStorage}.
 */
public class StringSeparatedMultiTypeNameValueStorageTest {
    private static final String SEPARATOR_1 = "separator-1";
    private static final String SEPARATOR_2 = "separator-2";
    private static final String TEST_KEY_1 = "test_key_1";
    private static final String TEST_KEY_2 = "test_key_2";
    private static final String TEST_VALUE_1 = "test_value_1";
    private static final String TEST_VALUE_2 = "test_value_2";

    private IPerSeparatorMultiTypeNameValueStorage<String> mStringSeparatedMultiTypeNameValueStorage;

    @Before
    public void setup() throws IOException {
        final SettablePlatformComponents components = SettablePlatformComponents.builder()
                .build();
        mStringSeparatedMultiTypeNameValueStorage = new StringSeparatedMultiTypeNameValueStorage(
                components, false
        );

        // clean state
        mStringSeparatedMultiTypeNameValueStorage.clear(SEPARATOR_1);
        mStringSeparatedMultiTypeNameValueStorage.clear(SEPARATOR_2);
    }

    @Test
    public void testCanReadAndWriteAString() {
        mStringSeparatedMultiTypeNameValueStorage.putString(SEPARATOR_1, TEST_KEY_1, TEST_VALUE_1);

        final String val = mStringSeparatedMultiTypeNameValueStorage.getString(SEPARATOR_1, TEST_KEY_1);
        Assert.assertEquals(TEST_VALUE_1, val);
    }

    @Test
    public void testCanReadAndWriteAStringToMultipleSeparators() {
        mStringSeparatedMultiTypeNameValueStorage.putString(SEPARATOR_1, TEST_KEY_1, TEST_VALUE_1);

        final String val = mStringSeparatedMultiTypeNameValueStorage.getString(SEPARATOR_1, TEST_KEY_1);
        Assert.assertEquals(TEST_VALUE_1, val);

        mStringSeparatedMultiTypeNameValueStorage.putString(SEPARATOR_2, TEST_KEY_2, TEST_VALUE_2);

        final String val2 = mStringSeparatedMultiTypeNameValueStorage.getString(SEPARATOR_2, TEST_KEY_2);
        Assert.assertEquals(TEST_VALUE_2, val2);
    }


    @Test
    public void testCanUpdateValue() {
        mStringSeparatedMultiTypeNameValueStorage.putString(SEPARATOR_1, TEST_KEY_1, TEST_VALUE_1);
        final String val = mStringSeparatedMultiTypeNameValueStorage.getString(SEPARATOR_1, TEST_KEY_1);
        Assert.assertEquals(TEST_VALUE_1, val);

        mStringSeparatedMultiTypeNameValueStorage.putString(SEPARATOR_1, TEST_KEY_1, TEST_VALUE_2);
        final String updatedVal = mStringSeparatedMultiTypeNameValueStorage.getString(SEPARATOR_1, TEST_KEY_1);
        Assert.assertEquals(TEST_VALUE_2, updatedVal);
    }

    @Test
    public void testCanPutNullValueAndUpdateAsWell() {
        // put null
        mStringSeparatedMultiTypeNameValueStorage.putString(SEPARATOR_1, TEST_KEY_1, null);

        // we should get back null
        Assert.assertNull(mStringSeparatedMultiTypeNameValueStorage.getString(SEPARATOR_1, TEST_KEY_1));

        // put actual value
        mStringSeparatedMultiTypeNameValueStorage.putString(SEPARATOR_1, TEST_KEY_1, TEST_VALUE_1);

        // we should get back the value we put
        Assert.assertEquals(TEST_VALUE_1, mStringSeparatedMultiTypeNameValueStorage.getString(SEPARATOR_1, TEST_KEY_1));

        // put null again
        mStringSeparatedMultiTypeNameValueStorage.putString(SEPARATOR_1, TEST_KEY_1, null);

        // we should get back null
        Assert.assertNull(mStringSeparatedMultiTypeNameValueStorage.getString(SEPARATOR_1, TEST_KEY_1));
    }

    @Test
    public void testCanGetAll() {
        mStringSeparatedMultiTypeNameValueStorage.putString(SEPARATOR_1, TEST_KEY_1, TEST_VALUE_1);
        mStringSeparatedMultiTypeNameValueStorage.putString(SEPARATOR_1, TEST_KEY_2, TEST_VALUE_2);

        final String val1 = mStringSeparatedMultiTypeNameValueStorage.getString(SEPARATOR_1, TEST_KEY_1);
        Assert.assertEquals(TEST_VALUE_1, val1);

        final String val2 = mStringSeparatedMultiTypeNameValueStorage.getString(SEPARATOR_1, TEST_KEY_2);
        Assert.assertEquals(TEST_VALUE_2, val2);

        final Map<String, String> map = mStringSeparatedMultiTypeNameValueStorage.getAll(SEPARATOR_1);
        Assert.assertNotNull(map);
        Assert.assertEquals(2, map.size());
        Assert.assertEquals(TEST_VALUE_1, map.get(TEST_KEY_1));
        Assert.assertEquals(TEST_VALUE_2, map.get(TEST_KEY_2));
    }

    @Test
    public void testCanGetAllFromEachSeparator() {
        mStringSeparatedMultiTypeNameValueStorage.putString(SEPARATOR_1, TEST_KEY_1, TEST_VALUE_1);
        mStringSeparatedMultiTypeNameValueStorage.putString(SEPARATOR_1, TEST_KEY_2, TEST_VALUE_2);

        mStringSeparatedMultiTypeNameValueStorage.putString(SEPARATOR_2, TEST_KEY_1, TEST_VALUE_1);

        final String val1 = mStringSeparatedMultiTypeNameValueStorage.getString(SEPARATOR_1, TEST_KEY_1);
        Assert.assertEquals(TEST_VALUE_1, val1);

        final String val2 = mStringSeparatedMultiTypeNameValueStorage.getString(SEPARATOR_1, TEST_KEY_2);
        Assert.assertEquals(TEST_VALUE_2, val2);

        final String val3 = mStringSeparatedMultiTypeNameValueStorage.getString(SEPARATOR_2, TEST_KEY_1);
        Assert.assertEquals(TEST_VALUE_1, val3);

        final Map<String, String> map = mStringSeparatedMultiTypeNameValueStorage.getAll(SEPARATOR_1);
        Assert.assertNotNull(map);
        Assert.assertEquals(2, map.size());
        Assert.assertEquals(TEST_VALUE_1, map.get(TEST_KEY_1));
        Assert.assertEquals(TEST_VALUE_2, map.get(TEST_KEY_2));

        final Map<String, String> map2 = mStringSeparatedMultiTypeNameValueStorage.getAll(SEPARATOR_2);
        Assert.assertNotNull(map2);
        Assert.assertEquals(1, map2.size());
        Assert.assertEquals(TEST_VALUE_1, map2.get(TEST_KEY_1));
    }

    @Test
    public void testCanDeleteAKey() {
        mStringSeparatedMultiTypeNameValueStorage.putString(SEPARATOR_1, TEST_KEY_1, TEST_VALUE_1);

        final String val = mStringSeparatedMultiTypeNameValueStorage.getString(SEPARATOR_1, TEST_KEY_1);
        Assert.assertEquals(TEST_VALUE_1, val);

        mStringSeparatedMultiTypeNameValueStorage.remove(SEPARATOR_1, TEST_KEY_1);

        final String valAfterDeleted = mStringSeparatedMultiTypeNameValueStorage.getString(SEPARATOR_1, TEST_KEY_1);
        Assert.assertNull(valAfterDeleted);
    }

    @Test
    public void testDeletingOneKeyDoesNotDeleteOtherKey() {
        mStringSeparatedMultiTypeNameValueStorage.putString(SEPARATOR_1, TEST_KEY_1, TEST_VALUE_1);
        mStringSeparatedMultiTypeNameValueStorage.putString(SEPARATOR_1, TEST_KEY_2, TEST_VALUE_2);

        final String val1 = mStringSeparatedMultiTypeNameValueStorage.getString(SEPARATOR_1, TEST_KEY_1);
        Assert.assertEquals(TEST_VALUE_1, val1);

        final String val2 = mStringSeparatedMultiTypeNameValueStorage.getString(SEPARATOR_1, TEST_KEY_2);
        Assert.assertEquals(TEST_VALUE_2, val2);

        mStringSeparatedMultiTypeNameValueStorage.remove(SEPARATOR_1, TEST_KEY_1);

        final String val1AfterDeleted = mStringSeparatedMultiTypeNameValueStorage.getString(SEPARATOR_1, TEST_KEY_1);
        Assert.assertNull(val1AfterDeleted);

        final String val2Again = mStringSeparatedMultiTypeNameValueStorage.getString(SEPARATOR_1, TEST_KEY_2);
        Assert.assertEquals(TEST_VALUE_2, val2Again);
    }

    @Test
    public void testDeletingKeyFromOneSeparatorDoesNotAffectTheOther() {
        mStringSeparatedMultiTypeNameValueStorage.putString(SEPARATOR_1, TEST_KEY_1, TEST_VALUE_1);
        mStringSeparatedMultiTypeNameValueStorage.putString(SEPARATOR_2, TEST_KEY_1, TEST_VALUE_1);

        final String val1 = mStringSeparatedMultiTypeNameValueStorage.getString(SEPARATOR_1, TEST_KEY_1);
        Assert.assertEquals(TEST_VALUE_1, val1);

        final String val2 = mStringSeparatedMultiTypeNameValueStorage.getString(SEPARATOR_2, TEST_KEY_1);
        Assert.assertEquals(TEST_VALUE_1, val2);

        mStringSeparatedMultiTypeNameValueStorage.remove(SEPARATOR_1, TEST_KEY_1);

        final String val1AfterDeleted = mStringSeparatedMultiTypeNameValueStorage.getString(SEPARATOR_1, TEST_KEY_1);
        Assert.assertNull(val1AfterDeleted);

        final String val2AfterDeleted = mStringSeparatedMultiTypeNameValueStorage.getString(SEPARATOR_2, TEST_KEY_1);
        Assert.assertEquals(TEST_VALUE_1, val2AfterDeleted);
    }

    @Test
    public void testClearDeletesAllKeys() {
        mStringSeparatedMultiTypeNameValueStorage.putString(SEPARATOR_1, TEST_KEY_1, TEST_VALUE_1);
        mStringSeparatedMultiTypeNameValueStorage.putString(SEPARATOR_1, TEST_KEY_2, TEST_VALUE_2);

        final String val1 = mStringSeparatedMultiTypeNameValueStorage.getString(SEPARATOR_1, TEST_KEY_1);
        Assert.assertEquals(TEST_VALUE_1, val1);

        final String val2 = mStringSeparatedMultiTypeNameValueStorage.getString(SEPARATOR_1, TEST_KEY_2);
        Assert.assertEquals(TEST_VALUE_2, val2);

        mStringSeparatedMultiTypeNameValueStorage.clear(SEPARATOR_1);

        final String val1AfterClear = mStringSeparatedMultiTypeNameValueStorage.getString(SEPARATOR_1, TEST_KEY_1);
        Assert.assertNull(val1AfterClear);

        final String val2AfterClear = mStringSeparatedMultiTypeNameValueStorage.getString(SEPARATOR_1, TEST_KEY_2);
        Assert.assertNull(val2AfterClear);
    }

    @Test
    public void testClearOnlyDeletesKeysFromOneSeparator() {
        mStringSeparatedMultiTypeNameValueStorage.putString(SEPARATOR_1, TEST_KEY_1, TEST_VALUE_1);
        mStringSeparatedMultiTypeNameValueStorage.putString(SEPARATOR_1, TEST_KEY_2, TEST_VALUE_2);
        mStringSeparatedMultiTypeNameValueStorage.putString(SEPARATOR_2, TEST_KEY_1, TEST_VALUE_1);

        final String val1 = mStringSeparatedMultiTypeNameValueStorage.getString(SEPARATOR_1, TEST_KEY_1);
        Assert.assertEquals(TEST_VALUE_1, val1);

        final String val2 = mStringSeparatedMultiTypeNameValueStorage.getString(SEPARATOR_1, TEST_KEY_2);
        Assert.assertEquals(TEST_VALUE_2, val2);

        mStringSeparatedMultiTypeNameValueStorage.clear(SEPARATOR_1);

        final String val1AfterClear = mStringSeparatedMultiTypeNameValueStorage.getString(SEPARATOR_1, TEST_KEY_1);
        Assert.assertNull(val1AfterClear);

        final String val2AfterClear = mStringSeparatedMultiTypeNameValueStorage.getString(SEPARATOR_1, TEST_KEY_2);
        Assert.assertNull(val2AfterClear);

        final String valInOtherSeparator = mStringSeparatedMultiTypeNameValueStorage.getString(SEPARATOR_2, TEST_KEY_1);
        Assert.assertEquals(TEST_VALUE_1, valInOtherSeparator);
    }

    @Test
    public void testCanReadAndWriteALong() {
        final String longKey = "long_key";
        final long longVal = 2;

        mStringSeparatedMultiTypeNameValueStorage.putLong(SEPARATOR_1, longKey, longVal);

        final Long val = mStringSeparatedMultiTypeNameValueStorage.getLong(SEPARATOR_1, longKey);
        Assert.assertNotNull(val);
        Assert.assertEquals(longVal, (long) val);
    }
}
