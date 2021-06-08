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
package com.microsoft.identity.common.unit;

import com.microsoft.identity.common.internal.util.ThreadUtils;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Tests for classes in the ThreadUtils package.
 */
public class ThreadUtilsTests {
    @Test
    public void basicPoolTest() throws Exception {
        final ExecutorService s = ThreadUtils.getNamedThreadPoolExecutor(1, 10, -1, 5, TimeUnit.SECONDS, "testPool");
        final Future<String> result = s.submit(new Callable<String>() {
            @Override
            public String call() {
                return Thread.currentThread().getName();
            }
        });
        Assert.assertTrue(result.get().startsWith("testPool"));
        s.shutdownNow();
    }

    @Test
    public void exceptionPropagationTest() throws Exception {
        final ExecutorService s = ThreadUtils.getNamedThreadPoolExecutor(1, 10, -1, 5, TimeUnit.SECONDS, "testPool");
        final Future<String> result = s.submit(new Callable<String>() {
            @Override
            public String call() {
                throw new RuntimeException("Boom!");
            }
        });
        try {
            result.get();
            throw new AssertionError("Should have gotten an exception here");
        } catch (ExecutionException e) {
            Assert.assertEquals("Boom!", e.getCause().getMessage());
        }
        s.shutdownNow();
    }

    @Test
    public void capacityOneTest() throws Exception {
        final ExecutorService s = ThreadUtils.getNamedThreadPoolExecutor(1, 1, 1, 5, TimeUnit.SECONDS, "testPool");
        final Future<?> result = s.submit(hangThread());
        final Future<?> result2 = s.submit(hangThread());
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
    public void capacityFiveUnboundedLooped() throws Exception {
        for (int i = 0; i < 10000; i++) {
            capacityFiveTestUnbounded();
        }
    }

    @Test
    public void capacityFiveTestUnbounded() throws Exception {
        final ExecutorService s = ThreadUtils.getNamedThreadPoolExecutor(1, 5, -1, 5, TimeUnit.SECONDS, "testPool");
        final CountDownLatch latch = new CountDownLatch(5);
        final CountDownLatch goLatch = new CountDownLatch(1);
        final CountDownLatch stopLatch = new CountDownLatch(5);
        final Future<?> result = s.submit(hangThreadLatch(latch, goLatch, stopLatch));
        final Future<?> result2 = s.submit(hangThreadLatch(latch, goLatch, stopLatch));
        final Future<?> result3 = s.submit(hangThreadLatch(latch, goLatch, stopLatch));
        final Future<?> result4 = s.submit(hangThreadLatch(latch, goLatch, stopLatch));
        final Future<?> result5 = s.submit(hangThreadLatch(latch, goLatch, stopLatch));

        goLatch.countDown();
        Assert.assertTrue(stopLatch.await(5, TimeUnit.MINUTES));

        for (int i = 0; i < 10; i++) {
            CountDownLatch newLatch = new CountDownLatch(1);
            CountDownLatch newGoLatch = new CountDownLatch(1);
            CountDownLatch newStopLatch = new CountDownLatch(1);
            for (int j = 0; j < 1000; j++) {
                s.submit(hangThreadLatch(newLatch, newGoLatch, newStopLatch));
            }
            Assert.assertTrue(newLatch.await(60, TimeUnit.SECONDS));
            newGoLatch.countDown();
            Assert.assertTrue(newStopLatch.await(60, TimeUnit.SECONDS));
        }

        s.shutdownNow();
    }

    @Test
    public void capacityFiveTestUnboundedExceptionsAndCancellations() throws Exception {
        final ExecutorService s = ThreadUtils.getNamedThreadPoolExecutor(1, 5, -1, 5, TimeUnit.SECONDS, "testPool");
        final CountDownLatch latch = new CountDownLatch(5);
        final CountDownLatch goLatch = new CountDownLatch(1);
        final CountDownLatch stopLatch = new CountDownLatch(5);
        final Future<?> result = s.submit(hangThreadLatch(latch, goLatch, stopLatch));
        final Future<?> result2 = s.submit(hangThreadLatch(latch, goLatch, stopLatch));
        final Future<?> result3 = s.submit(hangThreadLatch(latch, goLatch, stopLatch));
        final Future<?> result4 = s.submit(hangThreadLatch(latch, goLatch, stopLatch));
        final Future<?> result5 = s.submit(hangThreadLatch(latch, goLatch, stopLatch));

        goLatch.countDown();
        Assert.assertTrue(stopLatch.await(5, TimeUnit.MINUTES));

        for (int i = 0; i < 10; i++) {
            CountDownLatch newLatch = new CountDownLatch(1);
            CountDownLatch newGoLatch = new CountDownLatch(1);
            CountDownLatch newStopLatch = new CountDownLatch(1);
            List<Future> futures = new ArrayList<>();
            for (int j = 0; j < 1000; j++) {
                if (j%3 == 0) {
                    s.submit(hangThreadLatch(newLatch, newGoLatch, newStopLatch));
                } else if (j%3 == 1) {
                    s.submit(exceptionTaskLatched(newLatch, newGoLatch, newStopLatch));
                } else {
                    Future f = s.submit(hangThread());
                    futures.add(f);
                }
            }
            Assert.assertTrue(newLatch.await(60, TimeUnit.SECONDS));
            for (Future f: futures) {
                f.cancel(true);
            }
            newGoLatch.countDown();
            Assert.assertTrue(newStopLatch.await(60, TimeUnit.SECONDS));
        }

        s.shutdownNow();
    }

    @Test
    public void capacityFiveTestUnboundedExceptions() throws Exception {
        final ExecutorService s = ThreadUtils.getNamedThreadPoolExecutor(1, 5, -1, 5, TimeUnit.SECONDS, "testPool");
        final CountDownLatch latch = new CountDownLatch(5);
        final CountDownLatch goLatch = new CountDownLatch(1);
        final CountDownLatch stopLatch = new CountDownLatch(5);
        final Future<?> result = s.submit(hangThreadLatch(latch, goLatch, stopLatch));
        final Future<?> result2 = s.submit(hangThreadLatch(latch, goLatch, stopLatch));
        final Future<?> result3 = s.submit(hangThreadLatch(latch, goLatch, stopLatch));
        final Future<?> result4 = s.submit(hangThreadLatch(latch, goLatch, stopLatch));
        final Future<?> result5 = s.submit(hangThreadLatch(latch, goLatch, stopLatch));

        goLatch.countDown();
        Assert.assertTrue(stopLatch.await(5, TimeUnit.MINUTES));

        for (int i = 0; i < 10; i++) {
            CountDownLatch newLatch = new CountDownLatch(1);
            CountDownLatch newGoLatch = new CountDownLatch(1);
            CountDownLatch newStopLatch = new CountDownLatch(1);
            for (int j = 0; j < 1000; j++) {
                if (j%2 == 0) {
                    s.submit(hangThreadLatch(newLatch, newGoLatch, newStopLatch));
                } else {
                    s.submit(exceptionTaskLatched(newLatch, newGoLatch, newStopLatch));
                }
            }
            Assert.assertTrue(newLatch.await(60, TimeUnit.SECONDS));
            newGoLatch.countDown();
            Assert.assertTrue(newStopLatch.await(60, TimeUnit.SECONDS));
        }

        s.shutdownNow();
    }

    @Test
    public void capacityFiveTest() throws Exception {
        final ExecutorService s = ThreadUtils.getNamedThreadPoolExecutor(1, 5, 0, 5, TimeUnit.SECONDS, "testPool");
        final CountDownLatch latch = new CountDownLatch(5);
        final CountDownLatch goLatch = new CountDownLatch(1);
        final CountDownLatch stopLatch = new CountDownLatch(5);
        final Future<?> result = s.submit(hangThreadLatch(latch, goLatch, stopLatch));
        final Future<?> result2 = s.submit(hangThreadLatch(latch, goLatch, stopLatch));
        final Future<?> result3 = s.submit(hangThreadLatch(latch, goLatch, stopLatch));
        final Future<?> result4 = s.submit(hangThreadLatch(latch, goLatch, stopLatch));
        final Future<?> result5 = s.submit(hangThreadLatch(latch, goLatch, stopLatch));
        Assert.assertTrue(latch.await(10, TimeUnit.SECONDS));
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
        goLatch.countDown();
        Assert.assertTrue(stopLatch.await(5, TimeUnit.MINUTES));
        ((ThreadPoolExecutor)s).purge();
        int k = 0;
        // As far as I can tell, there's no reliable method to wait on this.
        // This is probably a good reason not to do this thing.
        while (((ThreadPoolExecutor)s).getActiveCount() > 0 && k < 10) {
            k++;
            Thread.sleep(0, 50);
        }
        Assert.assertEquals(0, ((ThreadPoolExecutor)s).getActiveCount());
        Assert.assertEquals(0, ((ThreadPoolExecutor)s).getQueue().size());
        CountDownLatch newLatch = new CountDownLatch(1);
        CountDownLatch newGoLatch = new CountDownLatch(1);
        CountDownLatch newStopLatch = new CountDownLatch(1);
        s.submit(hangThreadLatch(newLatch, newGoLatch, newStopLatch));
        Assert.assertTrue(newLatch.await(60, TimeUnit.SECONDS));
        newGoLatch.countDown();
        Assert.assertTrue(newStopLatch.await(60, TimeUnit.SECONDS));
        ((ThreadPoolExecutor)s).purge();
        s.shutdownNow();
    }

    @Test
    public void capacityFiveTestExceptions() throws Exception {
        final ExecutorService s = ThreadUtils.getNamedThreadPoolExecutor(1, 5, 0, 5, TimeUnit.SECONDS, "testPool");
        final CountDownLatch latch = new CountDownLatch(5);
        final CountDownLatch goLatch = new CountDownLatch(1);
        final CountDownLatch stopLatch = new CountDownLatch(5);
        final Future<?> result = s.submit(hangThreadLatch(latch, goLatch, stopLatch));
        final Future<?> result2 = s.submit(exceptionTaskLatched(latch, goLatch, stopLatch));
        final Future<?> result3 = s.submit(hangThreadLatch(latch, goLatch, stopLatch));
        final Future<?> result4 = s.submit(exceptionTaskLatched(latch, goLatch, stopLatch));
        final Future<?> result5 = s.submit(hangThreadLatch(latch, goLatch, stopLatch));
        Assert.assertTrue(latch.await(10, TimeUnit.SECONDS));
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
        goLatch.countDown();
        Assert.assertTrue(stopLatch.await(5, TimeUnit.MINUTES));
        ((ThreadPoolExecutor)s).purge();
        int k = 0;
        // As far as I can tell, there's no reliable method to wait on this.
        // This is probably a good reason not to do this thing.
        while (((ThreadPoolExecutor)s).getActiveCount() > 0 && k < 10) {
            k++;
            Thread.sleep(0, 100);
        }
        Assert.assertEquals(0, ((ThreadPoolExecutor)s).getActiveCount());
        Assert.assertEquals(0, ((ThreadPoolExecutor)s).getQueue().size());
        CountDownLatch newLatch = new CountDownLatch(1);
        CountDownLatch newGoLatch = new CountDownLatch(1);
        CountDownLatch newStopLatch = new CountDownLatch(1);
        s.submit(hangThreadLatch(newLatch, newGoLatch, newStopLatch));
        Assert.assertTrue(newLatch.await(60, TimeUnit.SECONDS));
        newGoLatch.countDown();
        Assert.assertTrue(newStopLatch.await(60, TimeUnit.SECONDS));
        ((ThreadPoolExecutor)s).purge();
        s.shutdownNow();
    }

    @Test
    public void capacityZeroTest() throws Exception {
        final ExecutorService s = ThreadUtils.getNamedThreadPoolExecutor(1, 1, 0, 5, TimeUnit.SECONDS, "testPool");
        final Future<?> result = s.submit(hangThread());
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
    private Runnable exceptionTaskLatched(CountDownLatch latch, CountDownLatch goLatch, CountDownLatch stopLatch) {
        return new Runnable() {
            @Override
            public void run() {
                latch.countDown();
                try {
                    goLatch.await();
                    throw new RuntimeException("BOOM!");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    stopLatch.countDown();
                }
            }
        };
    }

    private Runnable hangThreadLatch(CountDownLatch latch, CountDownLatch goLatch, CountDownLatch stopLatch) {
        return new Runnable() {
            @Override
            public void run() {
                latch.countDown();
                try {
                    goLatch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                stopLatch.countDown();
            }
        };
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
