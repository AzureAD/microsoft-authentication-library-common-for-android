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

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.microsoft.identity.common.components.AndroidPlatformComponentsFactory;
import com.microsoft.identity.common.java.cache.NameValueStorageBrokerApplicationMetadataCache;
import com.microsoft.identity.common.java.cache.BrokerApplicationMetadata;
import com.microsoft.identity.common.java.cache.IBrokerApplicationMetadataCache;
import com.microsoft.identity.common.java.interfaces.IPlatformComponents;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@RunWith(AndroidJUnit4.class)
public class SharedPreferencesBrokerApplicationMetadataCacheTest {

    private IBrokerApplicationMetadataCache mMetadataCache;

    @Before
    public void setUp() {
        final Context context = ApplicationProvider.getApplicationContext();
        final IPlatformComponents platformComponents = AndroidPlatformComponentsFactory.createFromContext(
                context
        );
        mMetadataCache = new NameValueStorageBrokerApplicationMetadataCache(
                platformComponents
        );
    }

    @After
    public void tearDown() {
        mMetadataCache.clear();
    }

    @Test
    public void testEmptyCacheReturnsList() {
        final List<BrokerApplicationMetadata> applicationMetadata = mMetadataCache.getAll();
        assertNotNull(applicationMetadata);
        assertTrue(applicationMetadata.isEmpty());
    }

    @Test
    public void testInsert() {
        final BrokerApplicationMetadata randomMetadata = generateRandomMetadata();

        mMetadataCache.insert(randomMetadata);

        assertEquals(
                1,
                mMetadataCache.getAll().size()
        );

        assertEquals(
                randomMetadata,
                mMetadataCache
                        .getAll()
                        .get(0)
        );
    }

    @Test
    public void testInsertMultiple() {
        final int expected = 10;
        final List<BrokerApplicationMetadata> metadataList = new ArrayList<>();

        for (int ii = 0; ii < expected; ii++) {
            metadataList.add(generateRandomMetadata());
        }

        for (final BrokerApplicationMetadata metadata : metadataList) {
            mMetadataCache.insert(metadata);
        }

        final List<BrokerApplicationMetadata> cacheContents = mMetadataCache.getAll();

        for (final BrokerApplicationMetadata metadata : cacheContents) {
            assertTrue(
                    metadataList.contains(metadata)
            );
        }
    }

    @Test
    public void testRemove() {
        final BrokerApplicationMetadata randomMetadata = generateRandomMetadata();

        mMetadataCache.insert(randomMetadata);

        assertEquals(
                1,
                mMetadataCache.getAll().size()
        );

        assertEquals(
                randomMetadata,
                mMetadataCache
                        .getAll()
                        .get(0)
        );

        assertTrue(mMetadataCache.remove(randomMetadata));
    }

    @Test
    public void testRemoveMultiple() {
        final int expected = 10;
        final List<BrokerApplicationMetadata> metadataList = new ArrayList<>();

        for (int ii = 0; ii < expected; ii++) {
            metadataList.add(generateRandomMetadata());
        }

        for (final BrokerApplicationMetadata metadata : metadataList) {
            mMetadataCache.insert(metadata);
        }

        final List<BrokerApplicationMetadata> cacheContents = mMetadataCache.getAll();

        for (final BrokerApplicationMetadata metadata : cacheContents) {
            assertTrue(
                    mMetadataCache.remove(metadata)
            );
        }

        assertTrue(mMetadataCache.getAll().isEmpty());
    }

    @Test
    public void testRemoveNone() {
        assertTrue(
                mMetadataCache.remove(
                        generateRandomMetadata()
                )
        );
    }

    @Test
    public void testClear() {
        final int expected = 10;

        for (int ii = 0; ii < expected; ii++) {
            mMetadataCache.insert(generateRandomMetadata());
        }

        assertEquals(
                expected,
                mMetadataCache.getAll().size()
        );

        assertTrue(
                mMetadataCache.clear()
        );

        assertEquals(
                0,
                mMetadataCache.getAll().size()
        );
    }

    @Test
    public void testDuplicateDataCannotExist() {
        // Create a random app
        final BrokerApplicationMetadata metadata = generateRandomMetadata();

        // Mark that app as non-foci
        metadata.setFoci(null);

        // Insert that record into the cache...
        final boolean inserted = mMetadataCache.insert(metadata);

        // Assert it was correctly inserted
        assertTrue(inserted);

        // Take the original app, and make it now foci
        metadata.setFoci("1");

        // Insert that record into the cache...
        final boolean insertedAgain = mMetadataCache.insert(metadata);

        // Assert it was correctly inserted
        assertTrue(insertedAgain);

        // Because those were the 'same app', assert that our cache only contains 1 entry
        // AND that that entry contains the updated foci state...
        assertEquals(1, mMetadataCache.getAll().size());

        assertTrue(mMetadataCache.getAllNonFociClientIds().isEmpty());
        assertEquals(1, mMetadataCache.getAllFociClientIds().size());
    }

    private static BrokerApplicationMetadata generateRandomMetadata() {
        final BrokerApplicationMetadata randomMetadata = new BrokerApplicationMetadata();

        randomMetadata.setClientId(UUID.randomUUID().toString());
        randomMetadata.setEnvironment(UUID.randomUUID().toString());
        randomMetadata.setFoci(UUID.randomUUID().toString());
        randomMetadata.setUid(new Random().nextInt());

        return randomMetadata;
    }
}
