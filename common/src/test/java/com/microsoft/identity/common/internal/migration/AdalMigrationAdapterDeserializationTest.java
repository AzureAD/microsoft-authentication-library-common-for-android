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

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;

import com.google.gson.Gson;
import com.microsoft.identity.common.adal.internal.cache.ADALTokenCacheItem;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.ParameterizedRobolectricTestRunner;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(ParameterizedRobolectricTestRunner.class)
public class AdalMigrationAdapterDeserializationTest {

    private static final String AUTHORITY = "https://login.microsoftonline.com/common";
    private static final String CLIENT_ID = "a_client_id";
    private static final String TENANT_ID = "a_tenant_id";
    public static final String CACHE_KEY = "a_cache_key";

    private interface DeserializationValidator {
        void onDeserializationFinished(Map<String, ADALTokenCacheItem> result);
    }

    private AdalMigrationAdapter mMigrationAdapter;

    private Map<String, String> mDeserializationInput;
    private DeserializationValidator mValidator;

    @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
    public static Iterable<Object[]> testParams() {
        return Arrays.asList(
                new Object[]{
                        "Single Item Valid Input Test",
                        getAdalCacheItemInput(),
                        getDeserializationValidator()
                },
                new Object[]{
                        "Multi-Item Input Test with Malformed JSON",
                        getMalformedCacheItemInput(),
                        getDeserializationValidator()
                }
        );
    }

    @SuppressWarnings("unused")
    public AdalMigrationAdapterDeserializationTest(@NonNull final String testName,
                                                   @NonNull final Map<String, String> deserializationInput,
                                                   @NonNull final DeserializationValidator validator) {
        mDeserializationInput = deserializationInput;
        mValidator = validator;
    }

    @Before
    public void setUp() {
        final Context context = ApplicationProvider.getApplicationContext();
        mMigrationAdapter = new AdalMigrationAdapter(context, null, true);
    }

    @After
    public void tearDown() {
        mMigrationAdapter.setMigrationStatus(false);
    }

    @Test
    public void executeTest() {
        mValidator.onDeserializationFinished(
                mMigrationAdapter.deserialize(mDeserializationInput)
        );
    }

    private static DeserializationValidator getDeserializationValidator() {
        return new DeserializationValidator() {
            @Override
            public void onDeserializationFinished(Map<String, ADALTokenCacheItem> result) {
                assertEquals(1, result.size());
                final ADALTokenCacheItem deserializedCacheItem = result.get(CACHE_KEY);
                assertEquals(AUTHORITY, deserializedCacheItem.getAuthority());
                assertEquals(CLIENT_ID, deserializedCacheItem.getClientId());
                assertEquals(TENANT_ID, deserializedCacheItem.getTenantId());
            }
        };
    }

    private static Map<String, String> getMalformedCacheItemInput() {
        final Map<String, String> inputCacheItems = getAdalCacheItemInput();
        // Insert malformed json
        inputCacheItems.put("another_key", "{[}");
        return inputCacheItems;
    }

    private static Map<String, String> getAdalCacheItemInput() {
        final Map<String, String> inputCacheItems = new HashMap<>();
        inputCacheItems.put(CACHE_KEY, getAdalTokenCacheItemJson());
        return inputCacheItems;
    }

    private static String getAdalTokenCacheItemJson() {
        return new Gson().toJson(getAdalTokenCacheItem());
    }

    private static ADALTokenCacheItem getAdalTokenCacheItem() {
        final ADALTokenCacheItem cacheItem = new ADALTokenCacheItem();
        cacheItem.setAuthority(AUTHORITY);
        cacheItem.setClientId(CLIENT_ID);
        cacheItem.setIsMultiResourceRefreshToken(true);
        cacheItem.setTenantId(TENANT_ID);

        return cacheItem;
    }
}
