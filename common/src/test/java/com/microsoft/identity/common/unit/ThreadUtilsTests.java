package com.microsoft.identity.common.unit;

import com.microsoft.identity.common.internal.util.ThreadUtils;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Tests for classes in the ThreadUtils package.
 */
public class ThreadUtilsTests {
    @Test
    public void basicPoolTest() throws Exception {
        ExecutorService s = ThreadUtils.getNamedThreadPoolExecutor(1, 10, -1, 5, TimeUnit.SECONDS, "testPool");
        Future<String> result = s.submit(new Callable<String>() {
            @Override
            public String call() {
                return Thread.currentThread().getName();
            }
        });
        Assert.assertTrue(result.get().startsWith("testPool"));
        s.shutdownNow();
    }

    @Test
    public void capacityOneTest() throws Exception {
        ExecutorService s = ThreadUtils.getNamedThreadPoolExecutor(1, 1, 1, 5, TimeUnit.SECONDS, "testPool");
        Future<?> result = s.submit(hangThread());
        Future<?> result2 = s.submit(hangThread());
        boolean caught = false;
        try {
            s.submit(new Runnable() {
                @Override
                public void run() {

                }
            });
        } catch (RejectedExecutionException e) {
            caught = true;
        }
        Assert.assertTrue("Execution should have been rejected", caught);
        result.cancel(true);
        result2.cancel(true);
        s.shutdownNow();
    }
    @Test
    public void capacityZeroTest() throws Exception {
        ExecutorService s = ThreadUtils.getNamedThreadPoolExecutor(1, 1, 0, 5, TimeUnit.SECONDS, "testPool");
        Future<?> result = s.submit(hangThread());
        boolean caught = false;
        try {
            s.submit(new Runnable() {
                @Override
                public void run() {

                }
            });
        } catch (RejectedExecutionException e) {
            caught = true;
        }
        Assert.assertTrue("Execution should have been rejected", caught);
        result.cancel(true);
        s.shutdownNow();
    }

    private Runnable hangThread() {
        return new Runnable() {
            @Override
            public void run() {
                ThreadUtils.sleepSafely(10_000_000, "foo", "Pretty much expecting it.");
            }
        };
    }
}
