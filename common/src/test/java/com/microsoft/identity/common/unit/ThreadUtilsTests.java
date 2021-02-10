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
