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
package com.microsoft.identity.common;

import androidx.test.InstrumentationRegistry;

import com.microsoft.identity.common.adal.internal.AndroidSecretKeyEnabledHelper;
import com.microsoft.identity.common.adal.internal.cache.StorageHelper;
import com.microsoft.identity.common.internal.cache.ISharedPreferencesFileManager;
import com.microsoft.identity.common.internal.cache.SharedPreferencesFileManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

@RunWith(Parameterized.class)
public class SharedPreferencesFileManagerTests extends AndroidSecretKeyEnabledHelper {

    private static final String sTEST_SHARED_PREFS_NAME = "com.microsoft.test.preferences";
    private static final String sTEST_KEY = "test_key";
    private static final String sTEST_VALUE = "test_value";

    private ISharedPreferencesFileManager mSharedPreferencesFileManager;

    @Parameterized.Parameters
    public static Iterable<ISharedPreferencesFileManager> testParams() {
        return Arrays.asList(new ISharedPreferencesFileManager[]{
                new SharedPreferencesFileManager(
                        InstrumentationRegistry.getTargetContext(),
                        sTEST_SHARED_PREFS_NAME
                ),
                new SharedPreferencesFileManager(
                        InstrumentationRegistry.getTargetContext(),
                        sTEST_SHARED_PREFS_NAME,
                        new StorageHelper(InstrumentationRegistry.getTargetContext())
                )
        });
    }

    public SharedPreferencesFileManagerTests(final ISharedPreferencesFileManager sharedPreferencesFileManager) {
        mSharedPreferencesFileManager = sharedPreferencesFileManager;
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @After
    public void tearDown() {
        mSharedPreferencesFileManager.clear();
        mSharedPreferencesFileManager = null;
    }

    @Test
    public void testPutString() {
        mSharedPreferencesFileManager.putString(sTEST_KEY, sTEST_VALUE);
        assertTrue(mSharedPreferencesFileManager.contains(sTEST_KEY));
    }

    @Test
    public void testGetString() {
        mSharedPreferencesFileManager.putString(sTEST_KEY, sTEST_VALUE);
        assertEquals(sTEST_VALUE, mSharedPreferencesFileManager.getString(sTEST_KEY));
    }

    @Test
    public void testGetSharedPreferencesFileName() {
        assertEquals(
                sTEST_SHARED_PREFS_NAME,
                mSharedPreferencesFileManager.getSharedPreferencesFileName()
        );
    }

    @Test
    public void testGetAll() {
        String[] testKeys = {"1", "2", "3"};
        String[] testValues = {"a", "b", "c"};

        for (int ii = 0; ii < testKeys.length; ii++) {
            mSharedPreferencesFileManager.putString(testKeys[ii], testValues[ii]);
        }

        final int expectedSize = 3;
        assertEquals(expectedSize, mSharedPreferencesFileManager.getAll().size());
    }

    @Test
    public void testContainsTrue() {
        mSharedPreferencesFileManager.putString(sTEST_KEY, sTEST_VALUE);
        assertTrue(mSharedPreferencesFileManager.contains(sTEST_KEY));
    }

    @Test
    public void testContainsFalse() {
        assertFalse(mSharedPreferencesFileManager.contains(sTEST_KEY));
    }

    @Test
    public void testClear() {
        // Insert a value
        mSharedPreferencesFileManager.putString(sTEST_KEY, sTEST_VALUE);
        // Verify the size() is 1
        assertEquals(1, mSharedPreferencesFileManager.getAll().size());

        // Clear the SharedPreferences
        mSharedPreferencesFileManager.clear();
        // Verify that it is now empty
        assertEquals(0, mSharedPreferencesFileManager.getAll().size());
    }

    @Test
    public void testRemove() {
        // Insert a value
        mSharedPreferencesFileManager.putString(sTEST_KEY, sTEST_VALUE);
        // Verify the size() is 1
        assertEquals(1, mSharedPreferencesFileManager.getAll().size());

        // Call remove()
        mSharedPreferencesFileManager.remove(sTEST_KEY);
        // Verify that it is now empty
        assertEquals(0, mSharedPreferencesFileManager.getAll().size());
    }
}
