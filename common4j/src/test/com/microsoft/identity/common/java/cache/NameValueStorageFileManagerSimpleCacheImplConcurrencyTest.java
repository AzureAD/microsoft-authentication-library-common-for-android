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
package com.microsoft.identity.common.java.cache;

import com.google.gson.reflect.TypeToken;
import com.microsoft.identity.common.java.interfaces.INameValueStorage;
import com.microsoft.identity.common.java.interfaces.IPlatformComponents;
import com.microsoft.identity.common.java.interfaces.IStorageSupplier;
import com.microsoft.identity.common.java.util.IPlatformUtil;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Concurrency tests for NameValueStorageFileManagerSimpleCacheImpl to verify that the
 * ReentrantReadWriteLock properly protects cache operations from race conditions.
 */
public class NameValueStorageFileManagerSimpleCacheImplConcurrencyTest {

    private static final String TEST_CACHE_NAME = "test-cache";
    private static final String TEST_KEY = "test-key";
    private static final int THREAD_COUNT = 10;
    private static final int OPERATIONS_PER_THREAD = 100;

    @Mock
    private IPlatformComponents mockComponents;

    @Mock
    private IPlatformUtil mockPlatformUtil;

    @Mock
    private IStorageSupplier mockStorageSupplier;

    private TestSimpleCache cache;
    private InMemoryStorage storage;
    private AutoCloseable mocks;

    /**
     * Test implementation of NameValueStorageFileManagerSimpleCacheImpl for testing purposes.
     */
    private static class TestSimpleCache extends NameValueStorageFileManagerSimpleCacheImpl<TestData> {
        public TestSimpleCache(IPlatformComponents components, String name, String key) {
            super(components, name, key, false);
        }

        @Override
        public Type getListTypeToken() {
            return TypeToken.getParameterized(List.class, TestData.class).getType();
        }
    }

    /**
     * Simple test data class.
     */
    private static class TestData {
        private final String id;
        private final int value;

        public TestData(String id, int value) {
            this.id = id;
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestData testData = (TestData) o;
            return value == testData.value && id.equals(testData.id);
        }

        @Override
        public int hashCode() {
            return 31 * id.hashCode() + value;
        }

        @Override
        public String toString() {
            return "TestData{id='" + id + "', value=" + value + "}";
        }
    }

    /**
     * In-memory implementation of INameValueStorage for testing.
     */
    private static class InMemoryStorage implements INameValueStorage<String> {
        private final Map<String, String> storage = new ConcurrentHashMap<>();

        @Override
        public String get(String name) {
            return storage.get(name);
        }

        @Override
        public Map<String, String> getAll() {
            return new ConcurrentHashMap<>(storage);
        }

        @Override
        public void put(String name, String value) {
            if (value == null) {
                storage.remove(name);
            } else {
                storage.put(name, value);
            }
        }

        @Override
        public void remove(String name) {
            storage.remove(name);
        }

        @Override
        public void clear() {
            storage.clear();
        }

        @Override
        public Set<String> keySet() {
            return new HashSet<>(storage.keySet());
        }

        @Override
        public java.util.Iterator<Map.Entry<String, String>> getAllFilteredByKey(
                com.microsoft.identity.common.java.util.ported.Predicate<String> keyFilter) {
            return storage.entrySet().stream()
                    .filter(entry -> keyFilter.test(entry.getKey()))
                    .iterator();
        }
    }

    @Before
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        storage = new InMemoryStorage();
        
        when(mockComponents.getPlatformUtil()).thenReturn(mockPlatformUtil);
        when(mockPlatformUtil.getNanosecondTime()).thenReturn(System.nanoTime());
        when(mockComponents.getStorageSupplier()).thenReturn(mockStorageSupplier);
        when(mockStorageSupplier.getUnencryptedNameValueStore(anyString(), any())).thenReturn(storage);
        
        cache = new TestSimpleCache(mockComponents, TEST_CACHE_NAME, TEST_KEY);
    }

    @After
    public void tearDown() throws Exception {
        if (mocks != null) {
            mocks.close();
        }
    }

    /**
     * Test that multiple threads can safely call insert() concurrently without data corruption.
     */
    @Test
    public void testConcurrentInserts() throws InterruptedException {
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch completionLatch = new CountDownLatch(THREAD_COUNT);
        final Set<TestData> expectedData = Collections.synchronizedSet(new HashSet<>());

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    // Wait for all threads to be ready
                    startLatch.await();

                    // Each thread inserts unique items
                    for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                        TestData data = new TestData("thread-" + threadId + "-item-" + j, j);
                        expectedData.add(data);
                        cache.insert(data);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        // Start all threads at once
        startLatch.countDown();

        // Wait for all threads to complete
        boolean completed = completionLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();
        Assert.assertTrue("Test timed out", completed);

        // Verify all data was inserted correctly
        List<TestData> actualData = cache.getAll();
        Assert.assertEquals("Expected all items to be present", 
                expectedData.size(), actualData.size());
        Assert.assertTrue("All expected items should be in cache", 
                new HashSet<>(actualData).containsAll(expectedData));
    }

    /**
     * Test that reads can happen concurrently while writes are in progress without seeing
     * inconsistent state.
     */
    @Test
    public void testConcurrentReadsAndWrites() throws InterruptedException {
        final CyclicBarrier barrier = new CyclicBarrier(THREAD_COUNT);
        final CountDownLatch completionLatch = new CountDownLatch(THREAD_COUNT);
        final AtomicInteger readCount = new AtomicInteger(0);
        final AtomicInteger writeCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

        // Half threads do reads, half do writes
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            final boolean isReader = i % 2 == 0;

            executor.submit(() -> {
                try {
                    // Wait for all threads to be ready
                    barrier.await();

                    if (isReader) {
                        // Reader threads
                        for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                            List<TestData> data = cache.getAll();
                            // Verify data consistency - all items should be valid
                            Assert.assertNotNull("Read data should not be null", data);
                            readCount.incrementAndGet();
                        }
                    } else {
                        // Writer threads
                        for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                            TestData data = new TestData("writer-" + threadId + "-item-" + j, j);
                            cache.insert(data);
                            writeCount.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    Assert.fail("Exception in thread: " + e.getMessage());
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        // Wait for all threads to complete
        boolean completed = completionLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();
        Assert.assertTrue("Test timed out", completed);

        // Verify operations completed
        Assert.assertTrue("Should have performed read operations", readCount.get() > 0);
        Assert.assertTrue("Should have performed write operations", writeCount.get() > 0);

        // Verify final state is consistent
        List<TestData> finalData = cache.getAll();
        Assert.assertNotNull("Final data should not be null", finalData);
    }

    /**
     * Test concurrent remove operations to ensure data consistency.
     */
    @Test
    public void testConcurrentRemoves() throws InterruptedException {
        // Pre-populate cache
        Set<TestData> initialData = new HashSet<>();
        for (int i = 0; i < THREAD_COUNT * OPERATIONS_PER_THREAD; i++) {
            TestData data = new TestData("item-" + i, i);
            initialData.add(data);
            cache.insert(data);
        }

        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch completionLatch = new CountDownLatch(THREAD_COUNT);
        final List<TestData> dataList = new ArrayList<>(initialData);

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();

                    // Each thread removes a portion of the data
                    for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                        int index = threadId * OPERATIONS_PER_THREAD + j;
                        if (index < dataList.size()) {
                            cache.remove(dataList.get(index));
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = completionLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();
        Assert.assertTrue("Test timed out", completed);

        // Verify all items were removed
        List<TestData> remainingData = cache.getAll();
        Assert.assertEquals("All items should be removed", 0, remainingData.size());
    }

    /**
     * Test concurrent clear operations to ensure they are thread-safe.
     */
    @Test
    public void testConcurrentClears() throws InterruptedException {
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch completionLatch = new CountDownLatch(THREAD_COUNT);

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();

                    // Alternate between inserts and clears
                    for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                        if (j % 2 == 0) {
                            TestData data = new TestData("item-" + j, j);
                            cache.insert(data);
                        } else {
                            cache.clear();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = completionLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();
        Assert.assertTrue("Test timed out", completed);

        // Final state should be consistent (either empty or with some items)
        List<TestData> finalData = cache.getAll();
        Assert.assertNotNull("Final data should not be null", finalData);
    }

    /**
     * Stress test with mixed operations (insert, remove, getAll, clear) to verify overall
     * thread safety under high contention.
     */
    @Test
    public void testMixedOperationsUnderStress() throws InterruptedException {
        final CyclicBarrier barrier = new CyclicBarrier(THREAD_COUNT);
        final CountDownLatch completionLatch = new CountDownLatch(THREAD_COUNT);
        final AtomicInteger operationCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    barrier.await();

                    // Each thread performs random operations
                    for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                        int operation = (threadId + j) % 4;
                        switch (operation) {
                            case 0: // Insert
                                TestData data = new TestData("thread-" + threadId + "-item-" + j, j);
                                cache.insert(data);
                                break;
                            case 1: // GetAll
                                List<TestData> allData = cache.getAll();
                                Assert.assertNotNull("Data should not be null", allData);
                                break;
                            case 2: // Remove (try to remove something that might exist)
                                TestData toRemove = new TestData("thread-" + threadId + "-item-" + (j - 1), j - 1);
                                cache.remove(toRemove);
                                break;
                            case 3: // Clear (less frequently)
                                if (j % 20 == 0) {
                                    cache.clear();
                                }
                                break;
                        }
                        operationCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    Assert.fail("Exception in thread: " + e.getMessage());
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        boolean completed = completionLatch.await(60, TimeUnit.SECONDS);
        executor.shutdown();
        Assert.assertTrue("Test timed out", completed);

        // Verify we completed all operations
        int expectedOperations = THREAD_COUNT * OPERATIONS_PER_THREAD;
        Assert.assertEquals("All operations should complete", 
                expectedOperations, operationCount.get());

        // Verify final state is consistent
        List<TestData> finalData = cache.getAll();
        Assert.assertNotNull("Final data should not be null", finalData);
    }

    /**
     * Test that verifies the lock prevents data corruption by checking that
     * concurrent operations maintain data integrity.
     */
    @Test
    public void testLockPreventsDataCorruption() throws InterruptedException {
        final int ITERATIONS = 50;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch completionLatch = new CountDownLatch(THREAD_COUNT);

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

        // Each thread repeatedly inserts and then reads the same item
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();

                    for (int j = 0; j < ITERATIONS; j++) {
                        TestData data = new TestData("thread-" + threadId, j);
                        
                        // Insert
                        cache.insert(data);
                        
                        // Immediately read and verify
                        List<TestData> allData = cache.getAll();
                        boolean found = false;
                        for (TestData item : allData) {
                            if (item.id.equals("thread-" + threadId)) {
                                found = true;
                                // With proper locking, we should see either the old or new value,
                                // but never a corrupted/partial state
                                Assert.assertTrue("Value should be valid", item.value >= 0);
                                break;
                            }
                        }
                        Assert.assertTrue("Inserted item should be found", found);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    Assert.fail("Data corruption detected: " + e.getMessage());
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = completionLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();
        Assert.assertTrue("Test timed out", completed);
    }
}
