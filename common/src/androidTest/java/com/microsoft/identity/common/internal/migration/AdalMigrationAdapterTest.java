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
package com.microsoft.identity.common.internal.migration;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.gson.Gson;
import com.microsoft.identity.common.internal.cache.ADALTokenCacheItem;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class AdalMigrationAdapterTest {

    private static final String AUTHORITY = "https://login.microsoftonline.com/common";
    private static final String CLIENT_ID = "a_client_id";
    private static final String TENANT_ID = "a_tenant_id";
    public static final String CACHE_KEY = "a_cache_key";

    private AdalMigrationAdapter mMigrationAdapter;

    @Before
    public void setUp() {
        final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        mMigrationAdapter = new AdalMigrationAdapter(
                context,
                null,
                true
        );
    }

    @After
    public void tearDown() {
        mMigrationAdapter.setMigrationStatus(false);
    }

    @Test
    public void testDeserialization() {
        final Map<String, String> inputCacheItems = new HashMap<>();
        inputCacheItems.put(CACHE_KEY, getAdalTokenCacheItemJson());
        final Map<String, ADALTokenCacheItem> outputCacheItems =
                mMigrationAdapter.deserialize(inputCacheItems);
        assertEquals(1, outputCacheItems.size());
        final ADALTokenCacheItem deserializedCacheItem = outputCacheItems.get(CACHE_KEY);
        assertEquals(AUTHORITY, deserializedCacheItem.getAuthority());
        assertEquals(CLIENT_ID, deserializedCacheItem.getClientId());
        assertEquals(TENANT_ID, deserializedCacheItem.getTenantId());
    }

    @Test
    public void testDeserializationWithMalformedInput() {
        final Map<String, String> inputCacheItems = new HashMap<>();
        inputCacheItems.put(CACHE_KEY, getAdalTokenCacheItemJson());

        // Insert malformed json
        inputCacheItems.put("another_key", "{[}");

        final Map<String, ADALTokenCacheItem> outputCacheItems =
                mMigrationAdapter.deserialize(inputCacheItems);
        assertEquals(1, outputCacheItems.size());
        final ADALTokenCacheItem deserializedCacheItem = outputCacheItems.get(CACHE_KEY);
        assertEquals(AUTHORITY, deserializedCacheItem.getAuthority());
        assertEquals(CLIENT_ID, deserializedCacheItem.getClientId());
        assertEquals(TENANT_ID, deserializedCacheItem.getTenantId());
    }

    private String getAdalTokenCacheItemJson() {
        return new Gson().toJson(getAdalTokenCacheItem());
    }

    private ADALTokenCacheItem getAdalTokenCacheItem() {
        final ADALTokenCacheItem cacheItem = new ADALTokenCacheItem();
        cacheItem.setAuthority(AUTHORITY);
        cacheItem.setClientId(CLIENT_ID);
        cacheItem.setIsMultiResourceRefreshToken(true);
        cacheItem.setTenantId(TENANT_ID);

        return cacheItem;
    }
}
