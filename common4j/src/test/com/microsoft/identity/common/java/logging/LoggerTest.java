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

package com.microsoft.identity.common.java.logging;

import com.microsoft.identity.common.java.nativeauth.controllers.results.ResetPasswordCommandResult;
import com.microsoft.identity.common.java.nativeauth.controllers.results.ResetPasswordStartCommandResult;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import lombok.SneakyThrows;

public class LoggerTest {
    final int DISCARDED_TIME_IN_MILLISECONDS = 100;
    final int TEST_TIME_OUT_IN_MILLISECONDS = 1000;

    final String tag = "TAG";
    final String correlationId = "correlationId";
    final String message = "Message";

    @Before
    public void setUp() {
        Logger.resetLogger();
        DiagnosticContext.INSTANCE.clear();
    }

    @Test(timeout = TEST_TIME_OUT_IN_MILLISECONDS)
    public void testWithContainingPiiAndPiiDisabled() throws InterruptedException {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final String[] result_tag = {null};
        final Logger.LogLevel[] result_logLevel = {Logger.LogLevel.UNDEFINED};
        final String[] result_logMessage = {null};
        final Boolean[] result_containsPII = {null};

        Logger.setLogger("TEST", new ILoggerCallback() {
            @Override
            public void log(String tag, Logger.LogLevel logLevel, String message, boolean containsPII) {
                result_tag[0] = tag;
                result_logLevel[0] = logLevel;
                result_logMessage[0] = message;
                result_containsPII[0] = containsPII;
                countDownLatch.countDown();
            }
        });
        Logger.setAllowPii(false);

        String correlationId = "12345";
        String continuationToken = "ABCDEFG";
        int codeLength = 6;
        String challengeTargetLabel = "s******n@g*****m";
        String challengeChannel = "email";

        final ResetPasswordCommandResult.CodeRequired result = new ResetPasswordCommandResult.CodeRequired(
                correlationId,
                continuationToken,
                codeLength,
                challengeTargetLabel,
                challengeChannel
        );

        Logger.infoWithObject(
                tag,
                "Returning result: ",
                result
        );

        // Logs like this should never be discarded, as Logger will produce a non-PII-containing string
        final boolean shouldLogBeDiscarded = false;

        final Boolean timedOut = !countDownLatch.await(DISCARDED_TIME_IN_MILLISECONDS, TimeUnit.MILLISECONDS);
        Assert.assertEquals(shouldLogBeDiscarded, timedOut);

        Assert.assertEquals(result_tag[0], tag);
        Assert.assertEquals(result_logLevel[0], Logger.LogLevel.INFO);
        Assert.assertTrue(result_logMessage[0].contains("Returning result: "));
        // Since Logger.setAllowPii is false, we expect the Logger to call the PII-safe toString().
        Assert.assertTrue(result_logMessage[0].contains(result.toString()));

        if (result.containsPii()) {
            // Result class contains PII, which we expect not to be part of the log.
            Assert.assertFalse(result_logMessage[0].contains(result.toUnsanitizedString()));
        }

        Assert.assertTrue(result_logMessage[0].contains("12345"));

        // Since Logger.setAllowPii is false, we expect the Logger to not use a string that contains PII
        Assert.assertEquals(result_containsPII[0], false);
    }

    @Test(timeout = TEST_TIME_OUT_IN_MILLISECONDS)
    public void testWithoutContainingPiiAndPiiDisabled() throws InterruptedException {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final String[] result_tag = {null};
        final Logger.LogLevel[] result_logLevel = {Logger.LogLevel.UNDEFINED};
        final String[] result_logMessage = {null};
        final Boolean[] result_containsPII = {null};

        Logger.setLogger("TEST", new ILoggerCallback() {
            @Override
            public void log(String tag, Logger.LogLevel logLevel, String message, boolean containsPII) {
                result_tag[0] = tag;
                result_logLevel[0] = logLevel;
                result_logMessage[0] = message;
                result_containsPII[0] = containsPII;
                countDownLatch.countDown();
            }
        });
        Logger.setAllowPii(false);

        String correlationId = "12345";
        String continuationToken = "ABCDEFG";

        final ResetPasswordCommandResult.PasswordRequired result = new ResetPasswordCommandResult.PasswordRequired(
                correlationId,
                continuationToken
        );

        Logger.infoWithObject(
                tag,
                "Returning result: ",
                result
        );

        // Logs like this should never be discarded, as Logger will produce a non-PII-containing string
        final boolean shouldLogBeDiscarded = false;

        final Boolean timedOut = !countDownLatch.await(DISCARDED_TIME_IN_MILLISECONDS, TimeUnit.MILLISECONDS);
        Assert.assertEquals(shouldLogBeDiscarded, timedOut);

        Assert.assertEquals(result_tag[0], tag);
        Assert.assertEquals(result_logLevel[0], Logger.LogLevel.INFO);
        Assert.assertTrue(result_logMessage[0].contains("Returning result: "));
        // Since Logger.setAllowPii is false, we expect the Logger to call the PII-safe toString().
        Assert.assertTrue(result_logMessage[0].contains(result.toString()));

        if (result.containsPii()) {
            // Result class contains PII, which we expect not to be part of the log.
            Assert.assertFalse(result_logMessage[0].contains(result.toUnsanitizedString()));
        }

        Assert.assertTrue(result_logMessage[0].contains("12345"));
        // Since Logger.setAllowPii is false, we expect the Logger to not use a string that contains PII
        Assert.assertEquals(result_containsPII[0], false);
    }

    @Test(timeout = TEST_TIME_OUT_IN_MILLISECONDS)
    public void testWithContainingPiiAndPiiEnabled() throws InterruptedException {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final String[] result_tag = {null};
        final Logger.LogLevel[] result_logLevel = {Logger.LogLevel.UNDEFINED};
        final String[] result_logMessage = {null};
        final Boolean[] result_containsPII = {null};

        Logger.setLogger("TEST", new ILoggerCallback() {
            @Override
            public void log(String tag, Logger.LogLevel logLevel, String message, boolean containsPII) {
                result_tag[0] = tag;
                result_logLevel[0] = logLevel;
                result_logMessage[0] = message;
                result_containsPII[0] = containsPII;
                countDownLatch.countDown();
            }
        });
        Logger.setAllowPii(true);

        String correlationId = "12345";
        String continuationToken = "ABCDEFG";
        int codeLength = 6;
        String challengeTargetLabel = "s******n@g*****m";
        String challengeChannel = "email";

        final ResetPasswordCommandResult.CodeRequired result = new ResetPasswordCommandResult.CodeRequired(
                correlationId,
                continuationToken,
                codeLength,
                challengeTargetLabel,
                challengeChannel
        );

        Logger.infoWithObject(
                tag,
                "Returning result: ",
                result
        );

        // Logs like this should never be discarded
        final boolean shouldLogBeDiscarded = false;

        final Boolean timedOut = !countDownLatch.await(DISCARDED_TIME_IN_MILLISECONDS, TimeUnit.MILLISECONDS);
        Assert.assertEquals(shouldLogBeDiscarded, timedOut);

        Assert.assertEquals(result_tag[0], tag);
        Assert.assertEquals(result_logLevel[0], Logger.LogLevel.INFO);
        Assert.assertTrue(result_logMessage[0].contains("Returning result: "));

        // Since Logger.setAllowPii is true, we expect the Logger to call the PII-safe toString().
        Assert.assertTrue(result_logMessage[0].contains(result.toUnsanitizedString()));

        Assert.assertTrue(result_logMessage[0].contains("12345"));
        Assert.assertEquals(result_containsPII[0], result.containsPii());
    }

    @Test(timeout = TEST_TIME_OUT_IN_MILLISECONDS)
    public void testWithoutContainingPiiAndPiiEnabled() throws InterruptedException {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final String[] result_tag = {null};
        final Logger.LogLevel[] result_logLevel = {Logger.LogLevel.UNDEFINED};
        final String[] result_logMessage = {null};
        final Boolean[] result_containsPII = {null};

        Logger.setLogger("TEST", new ILoggerCallback() {
            @Override
            public void log(String tag, Logger.LogLevel logLevel, String message, boolean containsPII) {
                result_tag[0] = tag;
                result_logLevel[0] = logLevel;
                result_logMessage[0] = message;
                result_containsPII[0] = containsPII;
                countDownLatch.countDown();
            }
        });
        Logger.setAllowPii(true);

        String correlationId = "12345";
        String continuationToken = "ABCDEFG";

        final ResetPasswordCommandResult.PasswordRequired result = new ResetPasswordCommandResult.PasswordRequired(
                correlationId,
                continuationToken
        );

        Logger.infoWithObject(
                tag,
                "Returning result: ",
                result
        );

        // Logs like this should never be discarded, as Logger will produce a non-PII-containing string
        final boolean shouldLogBeDiscarded = false;

        final Boolean timedOut = !countDownLatch.await(DISCARDED_TIME_IN_MILLISECONDS, TimeUnit.MILLISECONDS);
        Assert.assertEquals(shouldLogBeDiscarded, timedOut);

        Assert.assertEquals(result_tag[0], tag);
        Assert.assertEquals(result_logLevel[0], Logger.LogLevel.INFO);
        Assert.assertTrue(result_logMessage[0].contains("Returning result: "));
        // Since Logger.setAllowPii is true, we expect the Logger to call the PII-safe toString().
        Assert.assertTrue(result_logMessage[0].contains(result.toUnsanitizedString()));

        Assert.assertTrue(result_logMessage[0].contains("12345"));
        Assert.assertEquals(result_containsPII[0], result.containsPii());
    }

    @Test(timeout = TEST_TIME_OUT_IN_MILLISECONDS)
    public void logWithVerbose() throws InterruptedException {
        final Logger.LogLevel logLevel = Logger.LogLevel.VERBOSE;
        final boolean containsPII = false;

        Logger.setLogLevel(logLevel);
        testLogger(tag, logLevel, correlationId, message, containsPII, new IOperationToTest() {
            @Override
            public void execute() {
                Logger.verbose(tag, correlationId, message);
            }
        }, false);
    }

    @Test(timeout = TEST_TIME_OUT_IN_MILLISECONDS)
    public void logWithInfo() throws InterruptedException {
        final Logger.LogLevel logLevel = Logger.LogLevel.INFO;
        final boolean containsPII = false;

        Logger.setLogLevel(logLevel);
        testLogger(tag, logLevel, correlationId, message, containsPII, new IOperationToTest() {
            @Override
            public void execute() {
                Logger.info(tag, correlationId, message);
            }
        }, false);
    }

    @Test(timeout = TEST_TIME_OUT_IN_MILLISECONDS)
    public void logWithError() throws InterruptedException {
        final Logger.LogLevel logLevel = Logger.LogLevel.ERROR;
        final boolean containsPII = false;

        Logger.setLogLevel(logLevel);
        testLogger(tag, logLevel, correlationId, message, containsPII, new IOperationToTest() {
            @Override
            public void execute() {
                Logger.error(tag, correlationId, message, null);
            }
        }, false);
    }

    @Test(timeout = TEST_TIME_OUT_IN_MILLISECONDS)
    public void logWithWarning() throws InterruptedException {
        final Logger.LogLevel logLevel = Logger.LogLevel.WARN;
        final boolean containsPII = false;

        Logger.setLogLevel(logLevel);
        testLogger(tag, logLevel, correlationId, message, containsPII, new IOperationToTest() {
            @Override
            public void execute() {
                Logger.warn(tag, correlationId, message);
            }
        }, false);
    }

    @Test(timeout = TEST_TIME_OUT_IN_MILLISECONDS)
    public void setLogLevelInError_LogWithVerbose() throws InterruptedException {
        final Logger.LogLevel logLevel = Logger.LogLevel.VERBOSE;
        final boolean containsPII = false;

        Logger.setLogLevel(Logger.LogLevel.ERROR);
        testLogger(tag, logLevel, correlationId, message, containsPII, new IOperationToTest() {
            @Override
            public void execute() {
                Logger.verbose(tag, correlationId, message);
            }
        }, true);
    }

    @Test(timeout = TEST_TIME_OUT_IN_MILLISECONDS)
    public void setLogLevelAsNoLog_LogWithVerbose() throws InterruptedException {
        final boolean containsPII = false;

        Logger.setLogLevel(Logger.LogLevel.NO_LOG);
        testLogger(tag, Logger.LogLevel.VERBOSE, correlationId, message, containsPII, new IOperationToTest() {
            @Override
            public void execute() {
                Logger.verbose(tag, correlationId, message);
            }
        }, true);

        testLogger(tag, Logger.LogLevel.WARN, correlationId, message, containsPII, new IOperationToTest() {
            @Override
            public void execute() {
                Logger.warn(tag, correlationId, message);
            }
        }, true);
    }

    @Test(timeout = TEST_TIME_OUT_IN_MILLISECONDS)
    public void logPiiWhenDisabled() throws InterruptedException {
        final Logger.LogLevel logLevel = Logger.LogLevel.VERBOSE;
        final boolean containsPII = true;

        Logger.setAllowPii(false);
        testLogger(tag, logLevel, correlationId, message, containsPII, new IOperationToTest() {
            @Override
            public void execute() {
                Logger.verbosePII(tag, correlationId, message);
            }
        }, true);
    }

    @Test(timeout = TEST_TIME_OUT_IN_MILLISECONDS)
    public void logWithDiagnosticContext() throws InterruptedException {
        final Logger.LogLevel logLevel = Logger.LogLevel.VERBOSE;
        final boolean containsPII = false;
        final String newCorrelationId = "NEW_CORRELATION_ID";
        final String newThreadId = String.valueOf(Thread.currentThread().getId());

        final RequestContext requestContext = new RequestContext();
        requestContext.put(DiagnosticContext.CORRELATION_ID, newCorrelationId);
        DiagnosticContext.INSTANCE.setRequestContext(requestContext);

        Logger.setAllowPii(false);
        testLogger(tag, logLevel, newCorrelationId, newThreadId, containsPII, new IOperationToTest() {
            @Override
            public void execute() {
                Logger.verbose(tag, "");
            }
        }, false);
    }

    // Each thread should print a different thread name (and correlation ID, if set differently) to the log.
    @Test(timeout = TEST_TIME_OUT_IN_MILLISECONDS)
    public void logWithDiagnosticContext_Multithread() throws InterruptedException {
        final Logger.LogLevel logLevel = Logger.LogLevel.VERBOSE;
        final boolean containsPII = false;

        final String correlationId_1 = "CORRELATIONID_1";
        final String correlationId_2 = "CORRELATIONID_2";
        final String threadId_1 = String.valueOf(Thread.currentThread().getId());

        final RequestContext requestContext = new RequestContext();
        requestContext.put(DiagnosticContext.CORRELATION_ID, correlationId_1);
        DiagnosticContext.INSTANCE.setRequestContext(requestContext);

        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final String[] threadId_2 = {null};

        // Spin a background thread.
        new Thread(new Runnable() {
            @SneakyThrows
            @Override
            public void run() {
                threadId_2[0] = String.valueOf(Thread.currentThread().getId());

                final RequestContext requestContext = new RequestContext();
                requestContext.put(DiagnosticContext.CORRELATION_ID, correlationId_2);
                DiagnosticContext.INSTANCE.setRequestContext(requestContext);

                testLogger(tag, logLevel, correlationId_2, threadId_2[0], containsPII, new IOperationToTest() {
                    @Override
                    public void execute() {
                        Logger.verbose(tag, "");
                    }
                }, false);
                countDownLatch.countDown();
            }
        }).start();

        countDownLatch.await();
        testLogger(tag, logLevel, correlationId_1, threadId_1, containsPII, new IOperationToTest() {
            @Override
            public void execute() {
                Logger.verbose(tag, "");
            }
        }, false);

        Assert.assertNotEquals(threadId_2[0], threadId_1);
    }

    private interface IOperationToTest {
        void execute();
    }

    private void testLogger(String expectedTag,
                            Logger.LogLevel expectedLogLevel,
                            String expectedCorrelationId,
                            String expectedMessage,
                            boolean expectedContainsPii,
                            IOperationToTest operation,
                            boolean shouldLogBeDiscarded) throws InterruptedException {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final String[] result_tag = {null};
        final Logger.LogLevel[] result_logLevel = {Logger.LogLevel.UNDEFINED};
        final String[] result_logMessage = {null};
        final Boolean[] result_containsPII = {null};

        Logger.setLogger("TEST", new ILoggerCallback() {
            @Override
            public void log(String tag, Logger.LogLevel logLevel, String message, boolean containsPII) {
                result_tag[0] = tag;
                result_logLevel[0] = logLevel;
                result_logMessage[0] = message;
                result_containsPII[0] = containsPII;
                countDownLatch.countDown();
            }
        });

        operation.execute();
        final Boolean timedOut = !countDownLatch.await(DISCARDED_TIME_IN_MILLISECONDS, TimeUnit.MILLISECONDS);
        Assert.assertEquals(shouldLogBeDiscarded, timedOut);

        if (shouldLogBeDiscarded) {
            Assert.assertEquals(result_tag[0], null);
            Assert.assertEquals(result_logLevel[0], Logger.LogLevel.UNDEFINED);
        } else {
            Assert.assertEquals(result_tag[0], expectedTag);
            Assert.assertEquals(result_logLevel[0], expectedLogLevel);
            Assert.assertTrue(result_logMessage[0].contains(expectedMessage));
            Assert.assertTrue(result_logMessage[0].contains(expectedCorrelationId));
            Assert.assertEquals(result_containsPII[0], expectedContainsPii);
        }
    }
}
