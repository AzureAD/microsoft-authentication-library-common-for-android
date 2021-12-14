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
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.dx.rop.code.Exceptions;
import com.microsoft.identity.common.internal.cache.BrokerApplicationMetadata;
import com.microsoft.identity.common.internal.cache.IBrokerApplicationMetadataCache;
import com.microsoft.identity.common.internal.cache.SharedPreferencesBrokerApplicationMetadataCache;
import com.microsoft.identity.common.logging.Logger;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4.class)
public class SharedPreferencesBrokerApplicationMetadataCacheTest {

    public static final String M_CLIENT_ID = UUID.randomUUID().toString();
    public static final String M_ENVIRONMENT = UUID.randomUUID().toString();
    public static final int M_UID = new Random().nextInt();
    public static final int fixedIteration = 1000000;
    private SharedPreferencesBrokerApplicationMetadataCache mMetadataCache;

    @Before
    public void setUp() {
        mMetadataCache = SharedPreferencesBrokerApplicationMetadataCache.getInstance(
                InstrumentationRegistry.getContext()
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
    public void testMultipleInsertOrUpdate() throws Exception {
        final ExecutorService executor = Executors.newFixedThreadPool(10);
        final CountDownLatch latch =  new CountDownLatch(1);
        final CountDownLatch executionLatch =  new CountDownLatch(10);
        final Set<BrokerApplicationMetadata> set = Collections.synchronizedSet(new HashSet<BrokerApplicationMetadata>());
        List<Future<Boolean>> list = new ArrayList<>();
        for (int i = 0; i <10; i++){
            list.add(executor.submit(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    BrokerApplicationMetadata metadata = generateDuplicateMetadata();
                    set.add(metadata);
                    return insertOrUpdateFutureHelper(metadata, latch, executionLatch);
                }
            }));
        }
        executionLatch.await();
        latch.countDown();

        for(Future<Boolean> f : list){
            Assert.assertTrue(f.get());
        }

        final int size = mMetadataCache.getAll().size();

        assertEquals("Expected 1 record, but got too many",
                1,
                size
        );

        Assert.assertTrue(set.contains(mMetadataCache.getAll().get(0)));
    }

    public boolean insertOrUpdateFutureHelper
            (BrokerApplicationMetadata metadata, CountDownLatch latch, CountDownLatch executionLatch)
            throws InterruptedException {
        executionLatch.countDown();
        latch.await();
        return mMetadataCache.insertOrUpdate(metadata);
    }

    public boolean insertOrUpdateHelper
            (BrokerApplicationMetadata metadata, CountDownLatch latch, CountDownLatch executionLatch)
            throws InterruptedException {
        executionLatch.countDown();
        latch.await();
        return mMetadataCache.insertOrUpdate(metadata);
    }

    @Test
    public void testReadsWithInsertOrUpdate() throws Exception {
        final String methodName = ":testReadsWithInsertOrUpdate";

        final ExecutorService executor = Executors.newFixedThreadPool(10);
        final CountDownLatch latch =  new CountDownLatch(1);
        final CountDownLatch executionWriteLatch =  new CountDownLatch(5);
        final CountDownLatch executionReadLatch =  new CountDownLatch(5);
        final AtomicInteger iterations = new AtomicInteger(0);
        final AtomicBoolean failCheck = new AtomicBoolean(true);
        final AtomicInteger reads = new AtomicInteger(0);
        final AtomicInteger writes = new AtomicInteger(0);

        mMetadataCache.clear();
        //inserting one entry initially
        mMetadataCache.insertOrUpdate(generateDuplicateMetadata());

        assertEquals(1, mMetadataCache.getAll().size());

        for (int i = 0; i <5; i++){
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    while (iterations.getAndIncrement() < fixedIteration) {
                        BrokerApplicationMetadata metadata = generateDuplicateMetadata();
                        try {
                            if (!insertOrUpdateHelper(metadata, latch, executionWriteLatch)){
                                failCheck.set(false);
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        writes.getAndIncrement();
                    }
                }
            });

            executor.submit(new Runnable() {
                @Override
                public void run() {
                    while (iterations.getAndIncrement() < fixedIteration) {
                        try {
                            if (!readsWithInsertOrUpdateHelper(latch, executionReadLatch)){
                                failCheck.set(false);
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        reads.getAndIncrement();
                    }
                }
            });
        }
        executionWriteLatch.await();
        executionReadLatch.await();
        latch.countDown();
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.HOURS);

        final int size = mMetadataCache.getAll().size();

        assertEquals("Expected 1 record, but we got : " + size,
                1,
                size
        );

        Assert.assertTrue(failCheck.get());
        Logger.info(methodName,"Reads to Writes Ratio : " + reads + ":" + writes);
    }

    public boolean readsWithInsertOrUpdateHelper
            (CountDownLatch latch, CountDownLatch executionLatch)
            throws InterruptedException {
        executionLatch.countDown();
        latch.await();
        return mMetadataCache.getAll().size() == 1;
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

    private static BrokerApplicationMetadata generateDuplicateMetadata() {
        final BrokerApplicationMetadata randomMetadata = new BrokerApplicationMetadata();

        randomMetadata.setClientId(M_CLIENT_ID);
        randomMetadata.setEnvironment(M_ENVIRONMENT);
        randomMetadata.setFoci(UUID.randomUUID().toString());
        randomMetadata.setUid(M_UID);

        return randomMetadata;
    }
}
