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

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.microsoft.identity.common.components.AndroidPlatformComponentsFactory;
import com.microsoft.identity.common.internal.commands.RefreshOnCommand;
import com.microsoft.identity.common.java.AuthenticationConstants;
import com.microsoft.identity.common.java.cache.CacheRecord;
import com.microsoft.identity.common.java.cache.ICacheRecord;
import com.microsoft.identity.common.java.commands.BaseCommand;
import com.microsoft.identity.common.java.commands.CommandCallback;
import com.microsoft.identity.common.java.commands.EmptyCommandCallback;
import com.microsoft.identity.common.java.commands.SilentTokenCommand;
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
import com.microsoft.identity.common.java.exception.ErrorStrings;
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
import com.microsoft.identity.common.java.ui.PreferredAuthMethod;
import com.microsoft.identity.common.java.util.ported.PropertyBag;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
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

    private static final String TAG = "CommandDispatcherTest";

    private static final AtomicInteger INTEGER = new AtomicInteger(1);
    private static final String TEST_RESULT_STR = "test_result_str";
    private static final AcquireTokenResult TEST_ACQUIRE_TOKEN_REFRESH_EXPIRED_RESULT = getRefreshExpiredTokenResult();
    private static final AcquireTokenResult TEST_ACQUIRE_TOKEN_REFRESH_UNEXPIRED_RESULT = getRefreshUnexpiredTokenResult();

    // ════════════════════════════════════════════════════════════════════════════
    // Constants for State-Based Timeout Detection Tests
    // ════════════════════════════════════════════════════════════════════════════

    /** Duration for slow execution tests - must exceed the 30 second timeout */
    private static final long SLOW_EXECUTION_DURATION_MS = 35000;

    /** Small delay to ensure blocking tasks are fully blocking before submitting test request */
    private static final long TASK_STABILIZATION_DELAY_MS = 100;

    /** Thread pool size for silent requests (matches CommandDispatcher.SILENT_REQUEST_THREAD_POOL_SIZE) */
    private static final int SILENT_THREAD_POOL_SIZE = 5;

    /** Number of concurrent requests for state collision test */
    private static final int CONCURRENT_REQUEST_COUNT = 20;

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

    /**
     * Tests that initializeSilentExecutorWithExpandedPool() correctly expands the thread pool
     * when called with a valid Broker package name (Azure Authenticator).
     */
    @Test
    public void testInitializeSilentExecutorWithExpandedPool_WithValidBrokerPackage() throws Exception {
        CommandDispatcher.clearState();

        // Test with Azure Authenticator package
        CommandDispatcher.initializeSilentExecutorWithExpandedPool(
                AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_PACKAGE_NAME);

        // Verify executor is functional
        verifyExecutorIsFunctional();

        CommandDispatcher.clearState();
    }

    /**
     * Tests that initializeSilentExecutorWithExpandedPool() works with all valid Broker packages.
     */
    @Test
    public void testInitializeSilentExecutorWithExpandedPool_AllBrokerPackages() throws Exception {
        final String[] brokerPackages = {
                AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_PACKAGE_NAME,
                AuthenticationConstants.Broker.LTW_APP_PACKAGE_NAME,
                AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME
        };

        for (final String packageName : brokerPackages) {
            CommandDispatcher.clearState();
            CommandDispatcher.initializeSilentExecutorWithExpandedPool(packageName);
            verifyExecutorIsFunctional();
        }

        CommandDispatcher.clearState();
    }

    /**
     * Tests that initializeSilentExecutorWithExpandedPool() throws ClientException
     * when called with a non-Broker package name.
     */
    @Test
    public void testInitializeSilentExecutorWithExpandedPool_WithNonBrokerPackage_ThrowsException() throws Exception {
        CommandDispatcher.clearState();

        try {
            CommandDispatcher.initializeSilentExecutorWithExpandedPool("com.some.msal.app");
            Assert.fail("Expected ClientException to be thrown for non-Broker package");
        } catch (final ClientException e) {
            Assert.assertEquals(ErrorStrings.BROKER_ONLY_OPERATION, e.getErrorCode());
            // Verify message does NOT contain the package name (no sensitive info leak)
            Assert.assertFalse(e.getMessage().contains("com.some.msal.app"));
        } finally {
            CommandDispatcher.clearState();
        }
    }

    /**
     * Tests that initializeSilentExecutorWithExpandedPool() throws NullPointerException
     * when called with null package name due to @NonNull annotation enforcement.
     * Passing null is a programming error and should fail fast.
     */
    @Test(expected = NullPointerException.class)
    public void testInitializeSilentExecutorWithExpandedPool_WithNullPackage_ThrowsException() throws Exception {
        CommandDispatcher.clearState();
        try {
            CommandDispatcher.initializeSilentExecutorWithExpandedPool(null);
        } finally {
            CommandDispatcher.clearState();
        }
    }

    /**
     * Tests that initializeSilentExecutorWithExpandedPool() throws ClientException
     * when called with empty package name.
     */
    @Test
    public void testInitializeSilentExecutorWithExpandedPool_WithEmptyPackage_ThrowsException()  throws Exception {
        CommandDispatcher.clearState();

        try {
            CommandDispatcher.initializeSilentExecutorWithExpandedPool("");
            Assert.fail("Expected ClientException to be thrown for empty package");
        } catch (final ClientException e) {
            Assert.assertEquals(ErrorStrings.BROKER_ONLY_OPERATION, e.getErrorCode());
        } finally {
            CommandDispatcher.clearState();
        }
    }

    /**
     * Helper method to verify the silent executor is functional after initialization.
     */
    private void verifyExecutorIsFunctional() throws Exception {
        final CountDownLatch testLatch = new CountDownLatch(1);
        final TestCommand testCommand = getTestCommand(testLatch);

        final FinalizableResultFuture<CommandResult> future = CommandDispatcher.submitSilentReturningFuture(testCommand);
        testLatch.await();

        final CommandResult result = future.get();
        Assert.assertEquals(ICommandResult.ResultStatus.COMPLETED, result.getStatus());
        Assert.assertEquals(TEST_RESULT_STR, result.getResult());
        Assert.assertTrue(future.isDone());
        future.isCleanedUp();
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

    // ════════════════════════════════════════════════════════════════════════════
    // State-Based Timeout Detection Tests
    // ════════════════════════════════════════════════════════════════════════════

    /**
     * Test that timeout while queued with saturated pool is classified as
     * TIMED_OUT_THREAD_POOL_SATURATED.
     *
     * <p>Test Strategy:
     * <ol>
     *   <li>Fill all {@link #SILENT_THREAD_POOL_SIZE} thread pool threads with blocking tasks</li>
     *   <li>Submit a new request that will be queued (state: QUEUED)</li>
     *   <li>Request times out after 30 seconds while still in queue</li>
     *   <li>Verify error code is TIMED_OUT_THREAD_POOL_SATURATED</li>
     * </ol>
     *
     * <p>Expected Duration: ~30 seconds (timeout duration)
     *
     * @throws Exception if test setup fails
     */
    @Test
    public void testTimeoutClassification_ThreadPoolSaturated() throws Exception {
        Log.d(TAG, "testTimeoutClassification_ThreadPoolSaturated: Starting test");
        final int POOL_SIZE = SILENT_THREAD_POOL_SIZE;
        final CountDownLatch tasksStarted = new CountDownLatch(POOL_SIZE);
        final CountDownLatch releaseBlockingTasks = new CountDownLatch(1);
        final CountDownLatch tasksCompleted = new CountDownLatch(POOL_SIZE); // Track completion

        try {
            // Fill the thread pool with blocking tasks
            Log.d(TAG, "testTimeoutClassification_ThreadPoolSaturated: Filling thread pool with " + POOL_SIZE + " blocking tasks");
            for (int i = 0; i < POOL_SIZE; i++) {
                BlockingTestCommand blockingCommand = new BlockingTestCommand(
                    getEmptyTestParams(),
                    new EmptyCommandCallback(),
                    tasksStarted,
                    releaseBlockingTasks,
                    tasksCompleted
                );
                CommandDispatcher.submitSilentReturningFuture(blockingCommand);
            }

            // Wait for all blocking tasks to start (threads are now busy)
            Assert.assertTrue("All blocking tasks should start",
                tasksStarted.await(10, TimeUnit.SECONDS));
            Log.d(TAG, "testTimeoutClassification_ThreadPoolSaturated: All blocking tasks started, pool is saturated");

            // Small delay to ensure tasks are fully blocking
            Thread.sleep(TASK_STABILIZATION_DELAY_MS);

            // Submit request - should timeout in queue (all threads busy)
            SilentTokenCommandParameters params = createTestSilentTokenParams();
            SilentTokenCommand command = createTestSilentTokenCommand(params);

            try {
                CommandDispatcher.submitAcquireTokenSilentSync(command);
                Assert.fail("Expected ClientException with TIMED_OUT_THREAD_POOL_SATURATED");
            } catch (ClientException e) {
                // Verify timeout classified as thread pool saturated
                Log.d(TAG, "testTimeoutClassification_ThreadPoolSaturated: Caught expected exception with error code: " + e.getErrorCode());
                Assert.assertEquals("Expected TIMED_OUT_THREAD_POOL_SATURATED but got " + e.getErrorCode(),
                    ClientException.TIMED_OUT_THREAD_POOL_SATURATED, e.getErrorCode());
                Assert.assertTrue("Message should mention thread pool saturated",
                    e.getMessage().contains("Thread pool saturated"));
                Assert.assertNotNull("Correlation ID should be set", e.getCorrelationId());
            }
        } finally {
            // Release blocking tasks
            Log.d(TAG, "testTimeoutClassification_ThreadPoolSaturated: Releasing blocking tasks");
            releaseBlockingTasks.countDown();

            // Wait for all blocking tasks to complete (not just sleep)
            Assert.assertTrue("All blocking tasks should complete",
                tasksCompleted.await(10, TimeUnit.SECONDS));
            Log.d(TAG, "testTimeoutClassification_ThreadPoolSaturated: All blocking tasks completed, test finished");
        }
    }

    /**
     * Test that timeout during execution is classified as TIMED_OUT_EXECUTION.
     *
     * <p>Test Strategy:
     * <ol>
     *   <li>Submit a command that sleeps for {@link #SLOW_EXECUTION_DURATION_MS} (35s)</li>
     *   <li>Command starts executing (state: EXECUTING)</li>
     *   <li>Request times out after 30 seconds while still executing</li>
     *   <li>Verify error code is TIMED_OUT_EXECUTION</li>
     * </ol>
     *
     * <p>Expected Duration: ~30 seconds (timeout duration)
     *
     * @throws Exception if test setup fails
     */
    @Test
    public void testTimeoutClassification_SlowExecution() throws Exception {
        Log.d(TAG, "testTimeoutClassification_SlowExecution: Starting test with execution duration " + SLOW_EXECUTION_DURATION_MS + "ms");
        // Create command that executes slowly (SLOW_EXECUTION_DURATION_MS > 30 second timeout)
        SilentTokenCommandParameters params = createTestSilentTokenParams();
        SilentTokenCommand slowCommand = createSlowExecutionSilentTokenCommand(params, SLOW_EXECUTION_DURATION_MS);

        try {
            CommandDispatcher.submitAcquireTokenSilentSync(slowCommand);
            Assert.fail("Expected ClientException with TIMED_OUT_EXECUTION");
        } catch (ClientException e) {
            // Verify timeout classified as slow execution
            Log.d(TAG, "testTimeoutClassification_SlowExecution: Caught expected exception with error code: " + e.getErrorCode());
            Assert.assertEquals("Expected TIMED_OUT_EXECUTION error code",
                ClientException.TIMED_OUT_EXECUTION, e.getErrorCode());
            Assert.assertTrue("Message should mention slow execution",
                e.getMessage().contains("Slow execution"));
            Assert.assertNotNull("Correlation ID should be set", e.getCorrelationId());
        }

        // Verify cleanup
        Assert.assertEquals("Timeout location map should be cleaned up",
            0, getRequestStateMapSizeViaReflection());
    }

    /**
     * Test that timeout location map is cleaned up after successful request.
     *
     * <p>Verifies that sRequestStateMap entries are properly removed after
     * a request completes successfully via submitAcquireTokenSilentSync().
     * This prevents memory leaks from accumulating tracking state.
     *
     * <p>Note: State tracking only happens for requests going through
     * submitAcquireTokenSilentSync(), which is the real production path.
     *
     * @throws Exception if test fails
     */
    @Test
    public void testTimeoutLocationMap_CleanupAfterSuccess() throws Exception {
        // Get initial map size
        int initialSize = getRequestStateMapSizeViaReflection();

        // Execute successful request through the real entry point
        SilentTokenCommandParameters params = createTestSilentTokenParams();
        SilentTokenCommand successCommand = createTestSilentTokenCommand(params);

        // This is the real production path that tracks and cleans up state
        ILocalAuthenticationResult result = CommandDispatcher.submitAcquireTokenSilentSync(successCommand);
        Assert.assertNotNull("Command should return a result", result);

        // Verify cleanup - map should return to initial size immediately
        // (cleanup happens in submitAcquireTokenSilentSync's finally block)
        int finalSize = getRequestStateMapSizeViaReflection();
        Assert.assertEquals("Timeout location map should be cleaned up after success",
            initialSize, finalSize);
    }

    /**
     * Test that timeout location map is cleaned up after timeout.
     *
     * <p>Verifies that sRequestStateMap entries are properly removed after
     * a request times out. Cleanup happens in submitAcquireTokenSilentSync()'s
     * finally block, which runs immediately when TimeoutException is caught.
     *
     * <p>Expected Duration: ~30 seconds (timeout duration)
     *
     * @throws Exception if test fails
     */
    @Test
    public void testTimeoutLocationMap_CleanupAfterTimeout() throws Exception {
        // Get initial map size
        int initialSize = getRequestStateMapSizeViaReflection();

        // Execute request that will timeout
        SilentTokenCommandParameters params = createTestSilentTokenParams();
        SilentTokenCommand slowCommand = createSlowExecutionSilentTokenCommand(params, SLOW_EXECUTION_DURATION_MS);

        try {
            CommandDispatcher.submitAcquireTokenSilentSync(slowCommand);
            Assert.fail("Expected timeout");
        } catch (ClientException e) {
            // Expected - verify it's a timeout error
            Assert.assertTrue("Should be a timeout error",
                e.getErrorCode().startsWith("timed_out"));
        }

        // Verify cleanup - map should return to initial size
        int finalSize = getRequestStateMapSizeViaReflection();
        Assert.assertEquals("Timeout location map should be cleaned up after timeout",
            initialSize, finalSize);
    }

    /**
     * Test concurrent requests don't cause state collision in timeout tracking.
     *
     * <p>Test Strategy:
     * <ol>
     *   <li>Launch {@link #CONCURRENT_REQUEST_COUNT} threads simultaneously</li>
     *   <li>Each thread submits a unique request via submitAcquireTokenSilentSync()</li>
     *   <li>Wait for all requests to complete</li>
     *   <li>Verify all requests tracked independently (no state collision)</li>
     *   <li>Verify sRequestStateMap is cleaned up (no memory leaks)</li>
     * </ol>
     *
     * <p>This test validates that correlation ID-based tracking correctly
     * isolates concurrent requests through the real production path.
     *
     * @throws Exception if test fails
     */
    @Test
    public void testConcurrentRequests_NoStateCollision() throws Exception {
        final int NUM_REQUESTS = CONCURRENT_REQUEST_COUNT;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch completeLatch = new CountDownLatch(NUM_REQUESTS);
        final AtomicInteger successCount = new AtomicInteger(0);
        final AtomicInteger errorCount = new AtomicInteger(0);
        final AtomicReference<Throwable> firstError = new AtomicReference<>(null);

        // Launch concurrent requests
        for (int i = 0; i < NUM_REQUESTS; i++) {
            new Thread(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready

                    // Use real production path with unique correlation ID
                    SilentTokenCommandParameters params = createTestSilentTokenParams();
                    SilentTokenCommand cmd = createTestSilentTokenCommand(params);

                    // This is the real production path
                    ILocalAuthenticationResult result = 
                        CommandDispatcher.submitAcquireTokenSilentSync(cmd);

                    if (result != null) {
                        successCount.incrementAndGet();
                    } else {
                        errorCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    firstError.compareAndSet(null, e);
                } finally {
                    completeLatch.countDown();
                }
            }).start();
        }

        // Start all requests simultaneously
        startLatch.countDown();

        // Wait for all requests to complete
        Assert.assertTrue("All requests should complete within 60 seconds",
            completeLatch.await(60, TimeUnit.SECONDS));

        // Verify cleanup - should be immediate since cleanup happens in
        // submitAcquireTokenSilentSync's finally block
        int finalSize = getRequestStateMapSizeViaReflection();
        Assert.assertEquals("Timeout location map should be cleaned up",
            0, finalSize);

        // Verify all requests succeeded
        Assert.assertEquals("All requests should succeed",
            NUM_REQUESTS, successCount.get());
        Assert.assertEquals("No requests should fail",
            0, errorCount.get());

        // Fail if any error occurred
        if (firstError.get() != null) {
            Assert.fail("Unexpected error during concurrent execution: " + firstError.get().getMessage());
        }
    }

    /**
     * Test that correlation ID is properly set on timeout exceptions.
     *
     * <p>Verifies that when a timeout occurs, the resulting ClientException
     * contains the same correlation ID that was set on the original command
     * parameters. This is essential for correlating timeout errors with
     * specific requests in logs and telemetry.
     *
     * <p>Expected Duration: ~30 seconds (timeout duration)
     *
     * @throws Exception if test fails
     */
    @Test
    public void testTimeoutException_ContainsCorrelationId() throws Exception {
        final String expectedCorrelationId = java.util.UUID.randomUUID().toString();

        SilentTokenCommandParameters params = SilentTokenCommandParameters.builder()
            .platformComponents(AndroidPlatformComponentsFactory.createFromContext(ApplicationProvider.getApplicationContext()))
            .correlationId(expectedCorrelationId)
            .build();

        SilentTokenCommand slowCommand = createSlowExecutionSilentTokenCommand(params, SLOW_EXECUTION_DURATION_MS);

        try {
            CommandDispatcher.submitAcquireTokenSilentSync(slowCommand);
            Assert.fail("Expected ClientException due to timeout");
        } catch (ClientException e) {
            // Verify it's a timeout error
            Assert.assertTrue("Should be a timeout error",
                e.getErrorCode().startsWith("timed_out"));
            // Verify correlation ID is correctly propagated
            Assert.assertEquals("Correlation ID should match",
                expectedCorrelationId, e.getCorrelationId());
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // Reflection Helper Methods for Timeout Classification Tests
    // ════════════════════════════════════════════════════════════════════════════

    /**
     * Gets the request state map size via reflection.
     * Used to verify cleanup of sRequestStateMap entries after requests complete.
     */
    private int getRequestStateMapSizeViaReflection() throws Exception {
        Field field = CommandDispatcher.class.getDeclaredField("sRequestStateMap");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.concurrent.ConcurrentMap<String, ?> map =
            (java.util.concurrent.ConcurrentMap<String, ?>) field.get(null);
        return map.size();
    }

    /**
     * Creates SilentTokenCommandParameters with a random correlation ID for testing.
     */
    private SilentTokenCommandParameters createTestSilentTokenParams() {
        return SilentTokenCommandParameters.builder()
            .platformComponents(AndroidPlatformComponentsFactory.createFromContext(ApplicationProvider.getApplicationContext()))
            .correlationId(java.util.UUID.randomUUID().toString())
            .build();
    }

    // ════════════════════════════════════════════════════════════════════════════
    // Helper Methods for Creating Test SilentTokenCommands
    // ════════════════════════════════════════════════════════════════════════════

    /**
     * Creates a test SilentTokenCommand that executes quickly.
     */
    private SilentTokenCommand createTestSilentTokenCommand(
            @NonNull final SilentTokenCommandParameters parameters) {
        return new SilentTokenCommand(
            parameters,
            getTestSilentController().asControllerFactory(),
            new EmptyCommandCallback(),
            "test_silent_command"
        );
    }

    /**
     * Creates a SilentTokenCommand that executes slowly for the specified duration.
     */
    private SilentTokenCommand createSlowExecutionSilentTokenCommand(
            @NonNull final SilentTokenCommandParameters parameters,
            final long executionDurationMs) {
        return new SilentTokenCommand(
            parameters,
            getSlowExecutionController(executionDurationMs).asControllerFactory(),
            new EmptyCommandCallback(),
            "slow_silent_command"
        );
    }

    /**
     * Returns a test controller for quick-executing SilentTokenCommands.
     */
    private static BaseController getTestSilentController() {
        return new TestBaseController() {
            @Override
            public AcquireTokenResult acquireTokenSilent(SilentTokenCommandParameters parameters) throws Exception {
                return getRefreshUnexpiredTokenResult();
            }
        };
    }

    /**
     * Returns a controller that sleeps for the specified duration before returning.
     * Used for testing TIMED_OUT_EXECUTION classification.
     */
    private static BaseController getSlowExecutionController(final long executionDurationMs) {
        return new TestBaseController() {
            @Override
            public AcquireTokenResult acquireTokenSilent(SilentTokenCommandParameters parameters) throws Exception {
                Thread.sleep(executionDurationMs);
                return getRefreshUnexpiredTokenResult();
            }
        };
    }

    // ════════════════════════════════════════════════════════════════════════════
    // Test Command Classes
    // ════════════════════════════════════════════════════════════════════════════

    static class ExceptionCommand extends BaseCommand<String> {

        public ExceptionCommand(@NonNull final CommandParameters parameters,
                                @NonNull final CommandCallback callback) {
            super(parameters, getTestController().asControllerFactory(), callback, "test_id");
        }

        @Override
        public String execute() {
            throw new RuntimeException("An unexpected exception!");
        }
    }

    static class CommandThrowingIErrorInformationException extends BaseCommand<String> {
        final String mErrorCode;

        public CommandThrowingIErrorInformationException(@NonNull final CommandParameters parameters,
                                                         @NonNull final CommandCallback callback, String errorCode) {
            super(parameters, getTestController().asControllerFactory(), callback, "test_id");
            mErrorCode = errorCode;
        }

        @Override
        public String execute() {
            throw new TerminalException("An unexpected exception!", new Exception("Exception"), mErrorCode);
        }
    }


    static class TestCommand extends BaseCommand<String> {
        public int value;

        public TestCommand(@NonNull final CommandParameters parameters,
                           @NonNull final CommandCallback callback, int value) {
            super(parameters, getTestController().asControllerFactory(), callback, "test_id");
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
                                                        throwRenewAccessTokenError).asControllerFactory(),
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
                result = getControllerFactory().getDefaultController().acquireTokenSilent((SilentTokenCommandParameters) getParameters());
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
            super(parameters, getTestController().asControllerFactory(), callback, "test_id");
        }

        @Override
        public Object execute() throws Exception {
            Thread.sleep(10000);
            return null;
        }
    }

    /**
     * Command that blocks until released via CountDownLatch.
     * Used to saturate the thread pool for testing queue saturation.
     */
    static class BlockingTestCommand extends BaseCommand<String> {
        private final CountDownLatch started;
        private final CountDownLatch release;
        private final CountDownLatch completed;

        public BlockingTestCommand(
                @NonNull final CommandParameters parameters,
                @NonNull final CommandCallback callback,
                @NonNull final CountDownLatch started,
                @NonNull final CountDownLatch release) {
            this(parameters, callback, started, release, null);
        }

        public BlockingTestCommand(
                @NonNull final CommandParameters parameters,
                @NonNull final CommandCallback callback,
                @NonNull final CountDownLatch started,
                @NonNull final CountDownLatch release,
                @Nullable final CountDownLatch completed) {
            super(parameters, getTestController().asControllerFactory(), callback, "blocking_test_id");
            this.started = started;
            this.release = release;
            this.completed = completed;
        }

        @Override
        public String execute() throws Exception {
            try {
                started.countDown(); // Signal that we've started
                release.await(60, TimeUnit.SECONDS); // Block until released
                return "completed";
            } finally {
                if (completed != null) {
                    completed.countDown(); // Signal completion
                }
            }
        }

        @Override
        public boolean isEligibleForCaching() {
            return false; // Each blocking command is unique
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
                    final RefreshOnCommand refreshOnCommand = new RefreshOnCommand(parameters, this.asControllerFactory(), "LocalMSALControllerMockPubId");
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

        @Override
        public PreferredAuthMethod getPreferredAuthMethod() throws Exception {
            return PreferredAuthMethod.NONE;
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
