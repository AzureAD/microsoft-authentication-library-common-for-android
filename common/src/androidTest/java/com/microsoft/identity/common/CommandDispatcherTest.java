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

import static com.microsoft.identity.common.java.exception.ServiceException.SERVICE_NOT_AVAILABLE;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.microsoft.identity.common.components.AndroidPlatformComponentsFactory;
import com.microsoft.identity.common.internal.commands.RefreshOnCommand;
import com.microsoft.identity.common.java.cache.CacheRecord;
import com.microsoft.identity.common.java.cache.ICacheRecord;
import com.microsoft.identity.common.java.commands.BaseCommand;
import com.microsoft.identity.common.java.commands.CommandCallback;
import com.microsoft.identity.common.java.commands.EmptyCommandCallback;
import com.microsoft.identity.common.java.commands.ICommandResult;
import com.microsoft.identity.common.java.commands.parameters.CommandParameters;
import com.microsoft.identity.common.java.commands.parameters.DeviceCodeFlowCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.GenerateShrCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.InteractiveTokenCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.RemoveAccountCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.SilentTokenCommandParameters;
import com.microsoft.identity.common.java.controllers.BaseController;
import com.microsoft.identity.common.java.controllers.CommandDispatcher;
import com.microsoft.identity.common.java.controllers.CommandResult;
import com.microsoft.identity.common.java.dto.AccessTokenRecord;
import com.microsoft.identity.common.java.dto.AccountRecord;
import com.microsoft.identity.common.java.dto.IdTokenRecord;
import com.microsoft.identity.common.java.dto.RefreshTokenRecord;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.exception.ServiceException;
import com.microsoft.identity.common.java.exception.TerminalException;
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationResult;
import com.microsoft.identity.common.java.providers.oauth2.TokenResult;
import com.microsoft.identity.common.java.request.SdkType;
import com.microsoft.identity.common.java.result.AcquireTokenResult;
import com.microsoft.identity.common.java.result.FinalizableResultFuture;
import com.microsoft.identity.common.java.result.GenerateShrResult;
import com.microsoft.identity.common.java.result.ILocalAuthenticationResult;
import com.microsoft.identity.common.java.result.LocalAuthenticationResult;
import com.microsoft.identity.common.java.util.ported.PropertyBag;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;


@RunWith(AndroidJUnit4.class)
public class CommandDispatcherTest {

    private static final AtomicInteger INTEGER = new AtomicInteger(1);
    private static final String TEST_RESULT_STR = "test_result_str";
    private static final AcquireTokenResult TEST_ACQUIRE_TOKEN_REFRESH_EXPIRED_RESULT = getRefreshExpiredTokenResult();
    private static final AcquireTokenResult TEST_ACQUIRE_TOKEN_REFRESH_UNEXPIRED_RESULT = getRefreshUnexpiredTokenResult();

    @Test
    public void testSubmitSilentShouldRefresh() throws Exception {
        final CountDownLatch callbackLatch = new CountDownLatch(1);
        CountDownLatch tryLatch = new CountDownLatch(1);
        CountDownLatch executeMethodEntranceVerifierLatch = new CountDownLatch(1);

        CountDownLatch controllerLatch = new CountDownLatch(2);
        final AtomicInteger renewAccessTokenCallCount = new AtomicInteger(0);
        final AtomicInteger acquireTokenSilentCallCount = new AtomicInteger(0);
        final AtomicInteger taskCompleteCount = new AtomicInteger(0);

        final BaseCommand silentTokenCommand = new LatchedRefreshInTestCommand(TEST_ACQUIRE_TOKEN_REFRESH_EXPIRED_RESULT,
                getEmptySilentTokenParameters(),
                new CommandCallback<ILocalAuthenticationResult, Exception>() {
                    @Override
                    public void onTaskCompleted(final ILocalAuthenticationResult actual) {
                        ILocalAuthenticationResult expected = TEST_ACQUIRE_TOKEN_REFRESH_EXPIRED_RESULT.getLocalAuthenticationResult();
                        Assert.assertEquals(expected, actual);
                        taskCompleteCount.getAndIncrement();
                        callbackLatch.countDown();
                    }

                    @Override
                    public void onCancel() {
                        callbackLatch.countDown();
                        Assert.fail();
                    }

                    @Override
                    public void onError(Exception error) {
                        callbackLatch.countDown();
                        Assert.fail();
                    }

                }, 3, tryLatch, executeMethodEntranceVerifierLatch,
                renewAccessTokenCallCount, acquireTokenSilentCallCount,
                controllerLatch, true, false) {
            @Override
            public boolean isEligibleForCaching() {
                return false;
            }

        };

        FinalizableResultFuture<CommandResult> silentReturningFuture = CommandDispatcher.submitSilentReturningFuture(silentTokenCommand);
        executeMethodEntranceVerifierLatch.await();
        tryLatch.countDown();
        controllerLatch.await();
        callbackLatch.await();
        controllerLatch.await();

        Assert.assertEquals(TEST_ACQUIRE_TOKEN_REFRESH_EXPIRED_RESULT.getLocalAuthenticationResult(), silentReturningFuture.get().getResult());

        Assert.assertTrue(silentReturningFuture.isDone());
        Assert.assertEquals(1, taskCompleteCount.get());
        Assert.assertEquals(1, renewAccessTokenCallCount.get());
        Assert.assertEquals(1, acquireTokenSilentCallCount.get());

        silentReturningFuture.isCleanedUp();
        Assert.assertFalse(CommandDispatcher.isCommandOutstanding(silentTokenCommand));
    }

    @Test
    public void testSubmitSilentShouldNOTRefresh() throws Exception {
        final CountDownLatch callbackLatch = new CountDownLatch(1);
        CountDownLatch tryLatch = new CountDownLatch(1);
        CountDownLatch executeMethodEntranceVerifierLatch = new CountDownLatch(1);

        CountDownLatch controllerLatch = new CountDownLatch(1);
        final AtomicInteger renewAccessTokenCallCount = new AtomicInteger(0);
        final AtomicInteger acquireTokenSilentCallCount = new AtomicInteger(0);
        final AtomicInteger taskCompleteCount = new AtomicInteger(0);

        final BaseCommand silentTokenCommand = new LatchedRefreshInTestCommand(TEST_ACQUIRE_TOKEN_REFRESH_UNEXPIRED_RESULT,
                getEmptySilentTokenParameters(),
                new CommandCallback<ILocalAuthenticationResult, Exception>() {
                    @Override
                    public void onTaskCompleted(final ILocalAuthenticationResult actual) {
                        ILocalAuthenticationResult expected = TEST_ACQUIRE_TOKEN_REFRESH_UNEXPIRED_RESULT.getLocalAuthenticationResult();
                        Assert.assertEquals(expected, actual);
                        taskCompleteCount.getAndIncrement();
                        callbackLatch.countDown();
                    }

                    @Override
                    public void onCancel() {
                        callbackLatch.countDown();
                        Assert.fail();
                    }

                    @Override
                    public void onError(Exception error) {
                        callbackLatch.countDown();
                        Assert.fail();
                    }

                }, 5, tryLatch, executeMethodEntranceVerifierLatch, renewAccessTokenCallCount, acquireTokenSilentCallCount, controllerLatch, false, false) {
            @Override
            public boolean isEligibleForCaching() {
                return false;
            }

        };

        FinalizableResultFuture<CommandResult> silentReturningFuture = CommandDispatcher.submitSilentReturningFuture(silentTokenCommand);
        executeMethodEntranceVerifierLatch.await();
        tryLatch.countDown();
        controllerLatch.await();
        callbackLatch.await();

        Assert.assertEquals(TEST_ACQUIRE_TOKEN_REFRESH_UNEXPIRED_RESULT.getLocalAuthenticationResult(), silentReturningFuture.get().getResult());

        Assert.assertTrue(silentReturningFuture.isDone());
        Assert.assertEquals(1, taskCompleteCount.get());
        Assert.assertEquals(1, acquireTokenSilentCallCount.get());
        Assert.assertEquals(0, renewAccessTokenCallCount.get());

        silentReturningFuture.isCleanedUp();
        Assert.assertFalse(CommandDispatcher.isCommandOutstanding(silentTokenCommand));
    }

    @Test
    public void testSubmitSilentShouldRefreshButThrowsError() throws Exception {
        final CountDownLatch callbackLatch = new CountDownLatch(1);
        CountDownLatch tryLatch = new CountDownLatch(1);
        CountDownLatch executeMethodEntranceVerifierLatch = new CountDownLatch(1);

        CountDownLatch controllerLatch = new CountDownLatch(1);
        final AtomicInteger renewAccessTokenCallCount = new AtomicInteger(0);
        final AtomicInteger acquireTokenSilentCallCount = new AtomicInteger(0);
        final AtomicInteger taskCompleteCount = new AtomicInteger(0);

        final BaseCommand silentTokenCommand = new LatchedRefreshInTestCommand(TEST_ACQUIRE_TOKEN_REFRESH_UNEXPIRED_RESULT,
                getEmptySilentTokenParameters(),
                new CommandCallback<ILocalAuthenticationResult, Exception>() {
                    @Override
                    public void onTaskCompleted(final ILocalAuthenticationResult actual) {
                        ILocalAuthenticationResult expected = TEST_ACQUIRE_TOKEN_REFRESH_UNEXPIRED_RESULT.getLocalAuthenticationResult();
                        Assert.assertEquals(expected, actual);
                        taskCompleteCount.getAndIncrement();
                        callbackLatch.countDown();
                    }

                    @Override
                    public void onCancel() {
                        callbackLatch.countDown();
                        Assert.fail();
                    }

                    @Override
                    public void onError(Exception error) {
                        callbackLatch.countDown();
                        Assert.fail();
                    }

                }, 7, tryLatch,
                executeMethodEntranceVerifierLatch,
                renewAccessTokenCallCount, acquireTokenSilentCallCount, controllerLatch, true, true) {
            @Override
            public boolean isEligibleForCaching() {
                return false;
            }

        };

        FinalizableResultFuture<CommandResult> silentReturningFuture = CommandDispatcher.submitSilentReturningFuture(silentTokenCommand);
        executeMethodEntranceVerifierLatch.await();
        tryLatch.countDown();
        controllerLatch.await();
        callbackLatch.await();

        Assert.assertEquals(TEST_ACQUIRE_TOKEN_REFRESH_UNEXPIRED_RESULT.getLocalAuthenticationResult(), silentReturningFuture.get().getResult());

        Assert.assertTrue(silentReturningFuture.isDone());
        Assert.assertEquals(1, taskCompleteCount.get());
        Assert.assertEquals(1, acquireTokenSilentCallCount.get());
        Assert.assertEquals(0, renewAccessTokenCallCount.get());

        silentReturningFuture.isCleanedUp();
        Assert.assertFalse(CommandDispatcher.isCommandOutstanding(silentTokenCommand));
    }

    @Test
    public void testCanSubmitSilently() throws InterruptedException {
        final CountDownLatch testLatch = new CountDownLatch(1);

        final BaseCommand<String> testCommand = getTestCommand(testLatch);
        CommandDispatcher.submitSilent(testCommand);
        testLatch.await();
    }

    @Test
    public void testSubmitSilentCached() throws Exception {
        final CountDownLatch testLatch = new CountDownLatch(1);
        CountDownLatch submitLatch = new CountDownLatch(1);
        CountDownLatch submitLatch1 = new CountDownLatch(1);
        final AtomicInteger excutionCount = new AtomicInteger(0);

        final TestCommand testCommand = new LatchedTestCommand(
                getEmptyTestParams(),
                new CommandCallback<String, Exception>() {
                    @Override
                    public void onCancel() {
                        testLatch.countDown();
                        Assert.fail();
                    }

                    @Override
                    public void onError(Exception error) {
                        testLatch.countDown();
                        Assert.fail();
                    }

                    @Override
                    public void onTaskCompleted(String s) {
                        testLatch.countDown();
                        Assert.assertEquals(TEST_RESULT_STR, s);
                    }
                }, 1, submitLatch, submitLatch1) {
            @Override
            public boolean isEligibleForCaching() {
                return true;
            }

            @Override
            public String execute() {
                excutionCount.incrementAndGet();
                return super.execute();
            }
        };
        final TestCommand testCommand2 = new LatchedTestCommand(
                getEmptyTestParams(),
                new CommandCallback<String, Exception>() {
                    @Override
                    public void onCancel() {
                        testLatch.countDown();
                        Assert.fail();
                    }

                    @Override
                    public void onError(Exception error) {
                        testLatch.countDown();
                        Assert.fail();
                    }

                    @Override
                    public void onTaskCompleted(String s) {
                        testLatch.countDown();
                        Assert.assertEquals(TEST_RESULT_STR, s);
                    }
                }, 1, submitLatch, submitLatch1) {
            @Override
            public boolean isEligibleForCaching() {
                return true;
            }

            @Override
            public String execute() {
                excutionCount.incrementAndGet();
                return super.execute();
            }
        };
        FinalizableResultFuture<CommandResult> f = CommandDispatcher.submitSilentReturningFuture(testCommand);
        FinalizableResultFuture<CommandResult> f2 = CommandDispatcher.submitSilentReturningFuture(testCommand2);
        submitLatch1.await();
        submitLatch.countDown();
        testLatch.await();
        Assert.assertTrue(f.isDone());
        Assert.assertNotNull(f2.get(1, TimeUnit.SECONDS));
        Assert.assertEquals(TEST_RESULT_STR, f.get().getResult());
        Assert.assertEquals(TEST_RESULT_STR, f2.get().getResult());
        Assert.assertSame(f.get().getResult(), f2.get().getResult());
        Assert.assertEquals(1, excutionCount.get());
        f.isCleanedUp();
        f2.isCleanedUp();
        Assert.assertFalse(CommandDispatcher.isCommandOutstanding(testCommand));
    }

    @Test
    public void testStopSilentRequestExecutor() throws Exception {
        LongRunningTestCommand testCommand = new LongRunningTestCommand(getEmptyTestParams(), new EmptyCommandCallback());
        // schedule a long running test command
        FinalizableResultFuture<CommandResult> future1 = CommandDispatcher.submitSilentReturningFuture(testCommand);

        // Stop the silent executor
        CommandDispatcher.stopSilentRequestExecutor();

        // verify that the previous command results in error
        CommandResult result = future1.get();
        Assert.assertEquals(ICommandResult.ResultStatus.ERROR, result.getStatus());

        // try scheduling a new command
        try {
            CommandDispatcher.submitSilentReturningFuture(new LongRunningTestCommand(getEmptyTestParams(), new EmptyCommandCallback()));
            Assert.fail("Should not reach here");
        } catch (final Exception e) {
            // Should be rejected to get scheduled
            Assert.assertTrue(e instanceof RejectedExecutionException);
        }
        // Restart the silentRequestExecutor again
        CommandDispatcher.resetSilentRequestExecutor();
    }

    @Test
    public void testResetSilentRequestExecutor() throws Exception {
        LongRunningTestCommand testCommand = new LongRunningTestCommand(getEmptyTestParams(), new EmptyCommandCallback());
        // schedule a long running test command
        CommandDispatcher.submitSilentReturningFuture(testCommand);
        // Stop the silent executor
        CommandDispatcher.stopSilentRequestExecutor();
        // reset the silent executor
        CommandDispatcher.resetSilentRequestExecutor();
        // schedule a test command
        FinalizableResultFuture<CommandResult> future = CommandDispatcher.submitSilentReturningFuture(new TestCommand(getEmptyTestParams(), new EmptyCommandCallback(), 1));
        // verify command is executed and result is returned
        CommandResult result = future.get();
        Assert.assertEquals(ICommandResult.ResultStatus.COMPLETED, result.getStatus());
        Assert.assertEquals(TEST_RESULT_STR, result.getResult());
    }

    private TestCommand getTestCommand(final CountDownLatch testLatch) {
        return new TestCommand(
                getEmptyTestParams(),
                new CommandCallback<String, Exception>() {
                    @Override
                    public void onCancel() {
                        testLatch.countDown();
                        Assert.fail();
                    }

                    @Override
                    public void onError(Exception error) {
                        testLatch.countDown();
                        Assert.fail();
                    }

                    @Override
                    public void onTaskCompleted(String s) {
                        testLatch.countDown();
                        Assert.assertEquals(TEST_RESULT_STR, s);
                    }
                }, INTEGER.getAndIncrement()) {
            @Override
            public boolean isEligibleForCaching() {
                return true;
            }
        };
    }

    /**
     * This test represents the case where a command changes underneath our system
     * while we're using it as a key.  They're not immutable, so they're not safe to
     * use as keys in a map.  It won't hurt, though, unless we can't get rid of them.
     * To test this, we submit a command, block before it executes, alter it, release it,
     * and then make sure it gets cleaned up.
     *
     * @throws Exception
     */
    @Test
    public void testSubmitSilentWithParamMutation() throws Exception {
        final CountDownLatch testLatch = new CountDownLatch(1);
        CountDownLatch testStartLatch = new CountDownLatch(1);
        CountDownLatch exeutionStartLatch = new CountDownLatch(1);

        final TestCommand testCommand = new LatchedTestCommand(
                getEmptyTestParams(),
                new CommandCallback<String, Exception>() {
                    @Override
                    public void onCancel() {
                        testLatch.countDown();
                        Assert.fail();
                    }

                    @Override
                    public void onError(Exception error) {
                        testLatch.countDown();
                        Assert.fail();
                    }

                    @Override
                    public void onTaskCompleted(String s) {
                        testLatch.countDown();
                        Assert.assertEquals(TEST_RESULT_STR, s);
                    }
                }, INTEGER.getAndIncrement(), testStartLatch, exeutionStartLatch) {
            @Override
            public boolean isEligibleForCaching() {
                return true;
            }
        };
        FinalizableResultFuture<CommandResult> submitSilentFuture = CommandDispatcher.submitSilentReturningFuture(testCommand);
        exeutionStartLatch.await();
        testCommand.value = INTEGER.getAndIncrement();
        testStartLatch.countDown();
        testLatch.await();
        Assert.assertTrue(submitSilentFuture.isDone());
        Assert.assertEquals(TEST_RESULT_STR, submitSilentFuture.get().getResult());
        submitSilentFuture.isCleanedUp();
        Assert.assertFalse(CommandDispatcher.isCommandOutstanding(testCommand));
    }


    /**
     * This test represents the case where a command changes underneath our system
     * while we're using it as a key.  They're not immutable, so they're not safe to
     * use as keys in a map.  It won't hurt, though, unless we can't get rid of them.
     * To test this, we submit a command, block before it executes, alter it, release it,
     * and then make sure it gets cleaned up.
     *
     * @throws Exception
     */
    @Test
    public void testSubmitSilentWithParamMutationUncacheable() throws Exception {
        final CountDownLatch testLatch = new CountDownLatch(1);
        CountDownLatch submitLatch = new CountDownLatch(1);
        CountDownLatch submitLatch1 = new CountDownLatch(1);

        final TestCommand testCommand = new LatchedTestCommand(
                getEmptyTestParams(),
                new CommandCallback<String, Exception>() {
                    @Override
                    public void onCancel() {
                        testLatch.countDown();
                        Assert.fail();
                    }

                    @Override
                    public void onError(Exception error) {
                        testLatch.countDown();
                        Assert.fail();
                    }

                    @Override
                    public void onTaskCompleted(String s) {
                        testLatch.countDown();
                        Assert.assertEquals(TEST_RESULT_STR, s);
                    }
                }, INTEGER.getAndIncrement(), submitLatch, submitLatch1) {
            @Override
            public boolean isEligibleForCaching() {
                return false;
            }
        };
        FinalizableResultFuture<CommandResult> f = CommandDispatcher.submitSilentReturningFuture(testCommand);
        submitLatch1.await();
        testCommand.value = INTEGER.getAndIncrement();
        submitLatch.countDown();
        testLatch.await();
        Assert.assertTrue(f.isDone());
        Assert.assertEquals(TEST_RESULT_STR, f.get().getResult());
        f.isCleanedUp();
        Assert.assertFalse(CommandDispatcher.isCommandOutstanding(testCommand));
    }

    @Test
    public void testSubmitSilentWithException() {
        final CountDownLatch testLatch = new CountDownLatch(1);
        CommandDispatcher.submitSilent(new ExceptionCommand(getEmptyTestParams(),
                new CommandCallback<String, Exception>() {
                    @Override
                    public void onCancel() {
                        testLatch.countDown();
                        Assert.fail();
                    }

                    @Override
                    public void onError(Exception error) {
                        testLatch.countDown();
                    }

                    @Override
                    public void onTaskCompleted(String s) {
                        testLatch.countDown();
                        Assert.fail();
                    }
                }));
    }

    @Test
    public void testSubmitSilentWithTerminalException() {
        final String errorCode = "anError";
        final CountDownLatch testLatch = new CountDownLatch(1);
        CommandDispatcher.submitSilent(new CommandThrowingIErrorInformationException(getEmptyTestParams(),
                new CommandCallback<String, Exception>() {
                    @Override
                    public void onCancel() {
                        testLatch.countDown();
                        Assert.fail();
                    }

                    @Override
                    public void onError(Exception error) {
                        Assert.assertEquals(ClientException.class, error.getClass());
                        Assert.assertEquals(errorCode, ((ClientException) error).getErrorCode());
                        testLatch.countDown();
                    }

                    @Override
                    public void onTaskCompleted(String s) {
                        testLatch.countDown();
                        Assert.fail();
                    }
                }, errorCode));
    }
    /**
     * This test takes a while to run.  But it should always work.  Just put it here in order
     * to save anyone else from having to write it.  Effectively all of these results are non
     * cacheable, so this does not execute the deduplication logic at all.
     *
     * @throws Exception
     */
    @Test
    public void iterateTests() throws Exception {
        final int nThreads = 100;
        ExecutorService executor = Executors.newFixedThreadPool(nThreads);
        final AtomicReference<Throwable> ex = new AtomicReference<>(null);
        final int nTasks = 10_000;
        final CountDownLatch latch = new CountDownLatch(nTasks);
        final ConcurrentHashMap<Integer, Future<?>> map = new ConcurrentHashMap<>();
        for (int task = 0; task < nTasks; task++) {
            final int curTask = task;
            map.put(curTask, executor.submit(new Runnable() {
                public void run() {
                    try {
                        testSubmitSilentWithParamMutation();
                        testSubmitSilentWithParamMutationUncacheable();
                    } catch (Throwable t) {
                        ex.compareAndSet(null, t);
                    } finally {
                        latch.countDown();
                        map.remove(curTask);
                    }
                }
            }));
        }
        System.out.println("Waiting on latch");
        while (!latch.await(30, TimeUnit.SECONDS)) {
            System.out.println("Waiting, " + latch.getCount() + " outstanding");
            System.out.println("Waiting keys " + map.keySet());
        }
        executor.shutdown();
        System.out.println("Waiting, on executor");
        executor.awaitTermination(30, TimeUnit.SECONDS);
        executor.shutdownNow();
        if (ex.get() != null) {
            Assert.assertNull(ex.get());
        }
    }

    public void testSubmitSilentWithParamMutationSameCommand(final Consumer<String> c) throws Exception {
        final CountDownLatch testLatch = new CountDownLatch(1);
        CountDownLatch submitLatch = new CountDownLatch(1);
        CountDownLatch submitLatch1 = new CountDownLatch(1);

        final TestCommand testCommand = new LatchedTestCommand(
                getEmptyTestParams(),
                new CommandCallback<String, Exception>() {
                    @Override
                    public void onCancel() {
                        testLatch.countDown();
                        c.accept("FAIL");
                    }

                    @Override
                    public void onError(Exception error) {
                        testLatch.countDown();
                        error.printStackTrace();
                        c.accept("FAIL");
                    }

                    @Override
                    public void onTaskCompleted(String s) {
                        testLatch.countDown();
                        c.accept(s);
                    }
                }, 0, submitLatch, submitLatch1) {
            @Override
            public boolean isEligibleForCaching() {
                return true;
            }
        };
        FinalizableResultFuture<CommandResult> f = CommandDispatcher.submitSilentReturningFuture(testCommand);
        // We do not know if this command will execute, since it may be deduped.  We cannot await
        // the start of execution.
        testCommand.value = INTEGER.getAndIncrement();
        submitLatch.countDown();
        testLatch.await();
        Assert.assertTrue(f.isDone());
        final String result = (String) f.get().getResult();
        Assert.assertEquals(TEST_RESULT_STR, result);
        f.isCleanedUp();
        Assert.assertFalse(CommandDispatcher.isCommandOutstanding(testCommand));
    }

    /**
     * The other iteration test is all non-cacheable commands.  These are cachable.
     *
     * @throws Exception
     */
    @Test
    public void iterateTestsSame() throws Exception {
        final int nThreads = 100;
        ExecutorService executor = Executors.newFixedThreadPool(nThreads);
        final AtomicReference<Throwable> ex = new AtomicReference<>(null);
        final int nTasks = 10_000;
        final CountDownLatch latch = new CountDownLatch(nTasks);
        final ConcurrentHashMap<Integer, String> map = new ConcurrentHashMap<>();
        for (int task = 0; task < nTasks; task++) {
            final int curTask = task;
            executor.submit(new Runnable() {
                public void run() {
                    try {
                        map.put(curTask, "foo");
                        testSubmitSilentWithParamMutationSameCommand(new Consumer<String>() {
                                                                         @Override
                                                                         public void accept(String s) {
                                                                             map.remove(curTask);
                                                                             if ("FAIL".equals(s)) {
                                                                                 ex.compareAndSet(null, new Exception("WE HAD AN ERROR in " + curTask));
                                                                             }
                                                                         }
                                                                     }
                        );
                    } catch (Throwable t) {
                        ex.compareAndSet(null, t);
                    } finally {
                        latch.countDown();
                    }
                }
            });
        }
        System.out.println("Waiting on latch");
        while (!latch.await(30, TimeUnit.SECONDS)) {
            System.out.println("Waiting, " + latch.getCount() + " outstanding");
            System.out.println("Waiting keys " + map.keySet().size());
        }
        executor.shutdown();
        System.out.println("Waiting, on executor");
        executor.awaitTermination(30, TimeUnit.SECONDS);
        executor.shutdownNow();
        if (ex.get() != null) {
            // If this fails, there has been at least one error.
            Assert.assertNull(ex.get());
        }
    }

    static class ExceptionCommand extends BaseCommand<String> {

        public ExceptionCommand(@NonNull final CommandParameters parameters,
                                @NonNull final CommandCallback callback) {
            super(parameters, getTestController(), callback, "test_id");
        }

        @Override
        public String execute() {
            throw new RuntimeException("An unexpected exception!");
        }

        @Override
        public boolean isEligibleForEstsTelemetry() {
            return false;
        }
    }

    static class CommandThrowingIErrorInformationException extends BaseCommand<String> {
        final String mErrorCode;

        public CommandThrowingIErrorInformationException(@NonNull final CommandParameters parameters,
                                                         @NonNull final CommandCallback callback, String errorCode) {
            super(parameters, getTestController(), callback, "test_id");
            mErrorCode = errorCode;
        }

        @Override
        public String execute() {
            throw new TerminalException("An unexpected exception!", new Exception("Exception"), mErrorCode);
        }

        @Override
        public boolean isEligibleForEstsTelemetry() {
            return false;
        }
    }


    static class TestCommand extends BaseCommand<String> {
        public int value;

        public TestCommand(@NonNull final CommandParameters parameters,
                           @NonNull final CommandCallback callback, int value) {
            super(parameters, getTestController(), callback, "test_id");
            this.value = value;
        }

        @Override
        public String execute() {
            return new String(TEST_RESULT_STR);
        }

        @Override
        public boolean isEligibleForCaching() {
            return true;
        }

        @Override
        public boolean isEligibleForEstsTelemetry() {
            return false;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || (!(o instanceof TestCommand))) return false;
            if (!super.equals(o)) return false;
            TestCommand that = (TestCommand) o;
            return value == that.value;
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), value);
        }
    }

    public static class LatchedTestCommand extends TestCommand {
        final CountDownLatch testStartLatch;
        final CountDownLatch exeutionStartLatch;

        public LatchedTestCommand(@NonNull final CommandParameters parameters,
                                  @NonNull final CommandCallback callback,
                                  final int value,
                                  @NonNull final CountDownLatch testStartLatch,
                                  @NonNull final CountDownLatch exeutionStartLatch) {
            super(parameters, callback, value);
            this.testStartLatch = testStartLatch;
            this.exeutionStartLatch = exeutionStartLatch;
        }

        @Override
        public String execute() {
            exeutionStartLatch.countDown();
            try {
                testStartLatch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
            return super.execute();
        }
    }

    public static class LatchedRefreshInTestCommand extends BaseCommand {
        final CountDownLatch tryLatch;
        final CountDownLatch executeMethodEntranceVerifierLatch;
        final AcquireTokenResult acquireTokenResult;
        final int commandId;

        public LatchedRefreshInTestCommand(@NonNull AcquireTokenResult expectedAcquireTokenResult,
                                           @NonNull final SilentTokenCommandParameters parameters,
                                           @NonNull final CommandCallback callback,
                                           final int commandId,
                                           @NonNull final CountDownLatch tryLatch,
                                           @NonNull final CountDownLatch executeMethodEntranceVerifierLatch,
                                           @NonNull final AtomicInteger renewAccessTokenCallCount,
                                           @NonNull final AtomicInteger acquireTokenSilentCallCount,
                                           @NonNull final CountDownLatch controllerLatch,
                                           @NonNull final Boolean shouldRefresh,
                                           @NonNull final Boolean throwRenewAccessTokenError
        ) {
            super(parameters,
                    getTestRefreshInController(expectedAcquireTokenResult,
                                                        renewAccessTokenCallCount,
                                                        acquireTokenSilentCallCount,
                                                        controllerLatch,
                                                        shouldRefresh,
                                                        throwRenewAccessTokenError),
                    callback,
                    "");
            this.tryLatch = tryLatch;
            this.executeMethodEntranceVerifierLatch = executeMethodEntranceVerifierLatch;
            this.acquireTokenResult = expectedAcquireTokenResult;
            this.commandId = commandId;
        }

        @Override
        public AcquireTokenResult execute() {
            AcquireTokenResult result;
            executeMethodEntranceVerifierLatch.countDown();
            try {
                tryLatch.await();
                result = getDefaultController().acquireTokenSilent((SilentTokenCommandParameters) getParameters());
            } catch (InterruptedException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }

            return result;
        }

        @Override
        public boolean isEligibleForEstsTelemetry() {
            return false;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || (!(o instanceof TestCommand))) return false;
            if (!super.equals(o)) return false;
            LatchedRefreshInTestCommand other = (LatchedRefreshInTestCommand) o;
            return this.commandId == other.commandId;
        }

    }

    public static class LongRunningTestCommand extends BaseCommand {
        public LongRunningTestCommand(@NonNull final CommandParameters parameters,
                           @NonNull final CommandCallback callback) {
            super(parameters, getTestController(), callback, "test_id");
        }

        @Override
        public Object execute() throws Exception {
            Thread.sleep(10000);
            return null;
        }

        @Override
        public boolean isEligibleForEstsTelemetry() {
            return false;
        }
    }
    private static BaseController getTestController() {
        return new TestBaseController() {
        };
    }

    private static BaseController getTestRefreshInController(final AcquireTokenResult expectedAcquireTokenResult,
                                                             final AtomicInteger renewAccessTokenCallCount,
                                                             final AtomicInteger acquireTokenSilentCallCount,
                                                             final CountDownLatch controllerLatch,
                                                             final Boolean shouldRefresh,
                                                             final Boolean throwRenewAccessTokenError) {
        return new TestBaseController() {
            @Override
            public AcquireTokenResult acquireTokenSilent(final SilentTokenCommandParameters parameters) {
                controllerLatch.countDown();
                acquireTokenSilentCallCount.getAndIncrement();
                if(shouldRefresh){
                    final RefreshOnCommand refreshOnCommand = new RefreshOnCommand(parameters, this, "LocalMSALControllerMockPubId");
                    CommandDispatcher.submitAndForgetReturningFuture(refreshOnCommand);
                }

                return expectedAcquireTokenResult;
            }

            @Override
            public TokenResult renewAccessToken(@NonNull SilentTokenCommandParameters parameters) throws ServiceException {
                if(!throwRenewAccessTokenError) {
                    controllerLatch.countDown();
                    renewAccessTokenCallCount.getAndIncrement();
                }else{
                    throw new ServiceException(SERVICE_NOT_AVAILABLE, "AAD is not available.", 503, null);
                }
                return new TokenResult();
            }
        };
    }

    private static AcquireTokenResult getRefreshExpiredTokenResult() {
        final AccessTokenRecord accessTokenRecord = getRefreshExpiredAccessTokenRecord();
        return getRefreshTokenResult(accessTokenRecord);
    }

    private static AccessTokenRecord getRefreshExpiredAccessTokenRecord() {
        final AccessTokenRecord accessTokenRecord = new AccessTokenRecord();
        accessTokenRecord.setExpiresOn(String.valueOf(Integer.MAX_VALUE));
        accessTokenRecord.setRefreshOn("0");
        return accessTokenRecord;
    }

    private static AcquireTokenResult getRefreshUnexpiredTokenResult() {
        final AccessTokenRecord accessTokenRecord = getRefreshUnexpiredAccessTokenRecord();
        return getRefreshTokenResult(accessTokenRecord);
    }

    private static AcquireTokenResult getRefreshTokenResult(final AccessTokenRecord accessTokenRecord) {
        final CacheRecord.CacheRecordBuilder recordBuilder = CacheRecord.builder().accessToken(accessTokenRecord);
        final List<ICacheRecord> cacheRecordList = new ArrayList<>();
        final ICacheRecord cacheRecord = recordBuilder
                .account(new AccountRecord())
                .accessToken(accessTokenRecord)
                .refreshToken(new RefreshTokenRecord())
                .idToken(new IdTokenRecord())
                .v1IdToken(new IdTokenRecord())
                .build();
        cacheRecordList.add(cacheRecord);
        final ILocalAuthenticationResult localAuthenticationResult = new LocalAuthenticationResult(
                cacheRecord,
                cacheRecordList,
                SdkType.MSAL,
                false
        );

        final AcquireTokenResult tokenResult = new AcquireTokenResult();
        tokenResult.setLocalAuthenticationResult(localAuthenticationResult);
        return tokenResult;
    }

    private static AccessTokenRecord getRefreshUnexpiredAccessTokenRecord() {
        final AccessTokenRecord accessTokenRecord = new AccessTokenRecord();
        accessTokenRecord.setExpiresOn(String.valueOf(Integer.MAX_VALUE));
        accessTokenRecord.setRefreshOn(String.valueOf(Integer.MAX_VALUE - 1));
        return accessTokenRecord;
    }

    private abstract static class TestBaseController extends BaseController {

        @Override
        public AcquireTokenResult acquireToken(InteractiveTokenCommandParameters request) throws Exception {
            return null;
        }

        @Override
        public void onFinishAuthorizationSession(int requestCode, int resultCode, @NonNull PropertyBag data) {}

        @Override
        public AcquireTokenResult acquireTokenSilent(SilentTokenCommandParameters parameters) throws Exception {
            return null;
        }

        @Override
        public List<ICacheRecord> getAccounts(CommandParameters parameters) throws Exception {
            return null;
        }

        @Override
        public boolean removeAccount(RemoveAccountCommandParameters parameters) throws Exception {
            return false;
        }

        @Override
        public boolean getDeviceMode(CommandParameters parameters) throws Exception {
            return false;
        }

        @Override
        public List<ICacheRecord> getCurrentAccount(CommandParameters parameters) throws Exception {
            return null;
        }

        @Override
        public boolean removeCurrentAccount(RemoveAccountCommandParameters parameters) throws Exception {
            return false;
        }

        @Override
        public AuthorizationResult deviceCodeFlowAuthRequest(DeviceCodeFlowCommandParameters parameters) throws Exception {
            return null;
        }

        @Override
        public AcquireTokenResult acquireDeviceCodeFlowToken(AuthorizationResult authorizationResult, DeviceCodeFlowCommandParameters parameters) throws Exception {
            return null;
        }

        @Override
        public GenerateShrResult generateSignedHttpRequest(GenerateShrCommandParameters parameters) throws Exception {
            return null;
        }

    }

    private static CommandParameters getEmptyTestParams() {
        return CommandParameters.builder()
                .platformComponents(AndroidPlatformComponentsFactory.createFromContext(ApplicationProvider.getApplicationContext()))
                .build();
    }

    private static SilentTokenCommandParameters getEmptySilentTokenParameters() {
        return SilentTokenCommandParameters.builder()
                .platformComponents(AndroidPlatformComponentsFactory.createFromContext(ApplicationProvider.getApplicationContext()))
                .build();
    }

}
