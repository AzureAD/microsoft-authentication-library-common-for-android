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
package com.microsoft.identity.common.java.util;

import lombok.NonNull;

import com.microsoft.identity.common.java.logging.Logger;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A set of utility classes for thread operations to avoid repetition of common patterns.  The idea here is that the barrier
 * to entry to this class should be high, and the pattern should be in use in multiple locations.  Some
 * of these may duplicate functionality present in the JVM in various iterations - once we get support
 * for that we should remove them.
 */
public class ThreadUtils {
    /**
     * A method to sleep safely without needing to explicitly handle InterruptedException.
     *
     * @param sleepTimeInMs the number of milliseconds to sleep.
     * @param tag           the tag for logging a message.
     * @param message       the message to log.
     */
    public static void sleepSafely(final int sleepTimeInMs, @NonNull final String tag, @NonNull final String message) {
        if (sleepTimeInMs > 0) {
            try {
                Thread.sleep(sleepTimeInMs);
            } catch (final InterruptedException e) {
                Logger.info(tag, message);
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Construct a thread pool with specified name and optionally bounded size.
     *
     * @param corePool      The smallest number of threads to keep alive in the pool.
     * @param maxPool       The maximum number of threads to allow in the thread pool, after which RejectedExecutionException will occur.
     * @param queueSize     The number of items to keep in the queue.  If this is less than 0, the queue is unbounded.
     * @param keepAliveTime The amount of time to keep excess (greater than corePool size) threads alive before terminating them.
     * @param keepAliveUnit The time unit on that time.
     * @param poolName      The name of the thread pool in use.
     * @return An executor service with the specified properties.
     */
    public static ExecutorService getNamedThreadPoolExecutor(final int corePool, final int maxPool,
                                                             final int queueSize, final long keepAliveTime,
                                                             @NonNull final TimeUnit keepAliveUnit,
                                                             @NonNull final String poolName) {
        if (queueSize > 0) {
            return new ThreadPoolExecutor(corePool, maxPool, keepAliveTime, keepAliveUnit,
                                          new ArrayBlockingQueue<Runnable>(queueSize),
                                          getNamedThreadFactory(poolName, System.getSecurityManager()));
        } else if (queueSize == 0) {
            return new ThreadPoolExecutor(corePool, maxPool, keepAliveTime, keepAliveUnit,
                                          new SynchronousQueue<Runnable>(),
                                          getNamedThreadFactory(poolName, System.getSecurityManager()));
        } else { // (queueSize < 0)
            return new ThreadPoolExecutor(corePool, maxPool, keepAliveTime, keepAliveUnit,
                                          new LinkedBlockingQueue<Runnable>(),
                                          getNamedThreadFactory(poolName, System.getSecurityManager()));
        }
    }

    //Nice thought, but if you're using executors, you're using ThreadGroup whether you want to or not.
    @SuppressWarnings("PMD.AvoidThreadGroup")
    private static ThreadFactory getNamedThreadFactory(@NonNull final String poolName, final SecurityManager securityManager) {
        return new ThreadFactory() {
            private final String poolPrefix = poolName + "-";
            private final AtomicLong threadNumber = new AtomicLong(1);
            private final ThreadGroup group = securityManager == null ? Thread.currentThread().getThreadGroup()
                                                                      : securityManager.getThreadGroup();

            @Override
            public Thread newThread(Runnable r) {
                final Thread thread = new Thread(group, r, poolPrefix + threadNumber.getAndIncrement(), 0);
                thread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                    @Override
                    public void uncaughtException(@NonNull final Thread t, @NonNull final Throwable e) {
                        if (e instanceof ThreadDeath) {
                            Logger.info("ThreadPool[" + poolName + "]", null,
                                    "Thread Death Exception in thread pool " + poolName);
                        } else {
                            Logger.error("ThreadPool[" + poolName + "]", null,
                                    "Uncaught Exception in thread pool " + poolName, e);
                        }
                    }
                });
                return thread;
            }
        };
    }
}
