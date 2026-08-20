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
package com.microsoft.identity.common.internal.logging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
@SuppressWarnings("deprecation")
public class LoggerTest {
    private static final String EXTERNAL_LOGGER_IDENTIFIER = "ANDROID_EXTERNAL_LOGGER";

    private final List<LogRecord> mRecords = new ArrayList<>();

    @Before
    public void setUp() throws Exception {
        clearUnderlyingLoggers();
        setEmitDeprecationEvent(true);
        com.microsoft.identity.common.java.logging.Logger.setAllowPii(false);
        com.microsoft.identity.common.java.logging.Logger.setLogLevel(
                com.microsoft.identity.common.java.logging.Logger.LogLevel.VERBOSE);
        com.microsoft.identity.common.java.logging.Logger.setPlatformString("");
        com.microsoft.identity.common.logging.Logger.setAllowLogcat(false);
        mRecords.clear();
    }

    @After
    public void tearDown() throws Exception {
        clearUnderlyingLoggers();
        setEmitDeprecationEvent(true);
        com.microsoft.identity.common.java.logging.Logger.setAllowPii(false);
        com.microsoft.identity.common.java.logging.Logger.setLogLevel(
                com.microsoft.identity.common.java.logging.Logger.LogLevel.VERBOSE);
        com.microsoft.identity.common.logging.Logger.setAllowLogcat(false);
    }

    @Test
    public void getInstance_returnsSingleton() {
        assertSame(Logger.getInstance(), Logger.getInstance());
    }

    @Test
    public void setAllowPii_whenToggled_updatesDelegateState() {
        Logger.setAllowPii(true);
        assertTrue(Logger.getAllowPii());

        Logger.setAllowPii(false);
        assertFalse(Logger.getAllowPii());
    }

    @Test
    public void setAllowLogcat_whenToggled_updatesAndroidLoggerState() {
        Logger.setAllowLogcat(true);
        assertTrue(Logger.getAllowLogcat());

        Logger.setAllowLogcat(false);
        assertFalse(Logger.getAllowLogcat());
    }

    @Test
    public void getDiagnosticContextMetadata_returnsThreadAndCorrelationMetadata() {
        final String metadata = Logger.getDiagnosticContextMetadata();

        assertNotNull(metadata);
        assertTrue(metadata.startsWith("thread_id: "));
        assertTrue(metadata.contains(", correlation_id: UNSET"));
    }

    @Test
    public void setLogLevel_forEachDeprecatedLevel_setsDelegateLevel() throws Exception {
        Logger.getInstance().setLogLevel(Logger.LogLevel.ERROR);
        assertDelegateLogLevel(com.microsoft.identity.common.java.logging.Logger.LogLevel.ERROR);

        Logger.getInstance().setLogLevel(Logger.LogLevel.WARN);
        assertDelegateLogLevel(com.microsoft.identity.common.java.logging.Logger.LogLevel.WARN);

        Logger.getInstance().setLogLevel(Logger.LogLevel.INFO);
        assertDelegateLogLevel(com.microsoft.identity.common.java.logging.Logger.LogLevel.INFO);

        Logger.getInstance().setLogLevel(Logger.LogLevel.VERBOSE);
        assertDelegateLogLevel(com.microsoft.identity.common.java.logging.Logger.LogLevel.VERBOSE);
    }

    @Test
    public void setExternalLogger_whenDelegateEmitsEachLevel_adaptsLevelAndPiiFlag() throws Exception {
        Logger.getInstance().setExternalLogger(new ILoggerCallback() {
            @Override
            public void log(final String tag,
                            final Logger.LogLevel logLevel,
                            final String message,
                            final boolean containsPII) {
                mRecords.add(new LogRecord(tag, logLevel, message, containsPII));
            }
        });

        final com.microsoft.identity.common.java.logging.ILoggerCallback callback =
                getExternalLoggerCallback();

        callback.log("tag-error",
                com.microsoft.identity.common.java.logging.Logger.LogLevel.ERROR,
                "message-error",
                true);
        callback.log("tag-warn",
                com.microsoft.identity.common.java.logging.Logger.LogLevel.WARN,
                "message-warn",
                false);
        callback.log("tag-info",
                com.microsoft.identity.common.java.logging.Logger.LogLevel.INFO,
                "message-info",
                false);
        callback.log("tag-verbose",
                com.microsoft.identity.common.java.logging.Logger.LogLevel.VERBOSE,
                "message-verbose",
                true);

        assertEquals(4, mRecords.size());
        assertLogRecord(0, "tag-error", Logger.LogLevel.ERROR, "message-error", true);
        assertLogRecord(1, "tag-warn", Logger.LogLevel.WARN, "message-warn", false);
        assertLogRecord(2, "tag-info", Logger.LogLevel.INFO, "message-info", false);
        assertLogRecord(3, "tag-verbose", Logger.LogLevel.VERBOSE, "message-verbose", true);
    }

    @Test(expected = RuntimeException.class)
    public void setExternalLogger_whenDelegateEmitsUnsupportedLevel_throwsRuntimeException()
            throws Exception {
        Logger.getInstance().setExternalLogger(new ILoggerCallback() {
            @Override
            public void log(final String tag,
                            final Logger.LogLevel logLevel,
                            final String message,
                            final boolean containsPII) {
                mRecords.add(new LogRecord(tag, logLevel, message, containsPII));
            }
        });

        getExternalLoggerCallback().log(
                "tag-no-log",
                com.microsoft.identity.common.java.logging.Logger.LogLevel.NO_LOG,
                "message-no-log",
                false);
    }

    @Test
    public void logMethods_whenDelegateLoggingDisabled_returnWithoutEmittingAsyncWork() {
        Logger.getInstance().setExternalLogger(new ILoggerCallback() {
            @Override
            public void log(final String tag,
                            final Logger.LogLevel logLevel,
                            final String message,
                            final boolean containsPII) {
                mRecords.add(new LogRecord(tag, logLevel, message, containsPII));
            }
        });
        com.microsoft.identity.common.java.logging.Logger.setLogLevel(
                com.microsoft.identity.common.java.logging.Logger.LogLevel.NO_LOG);

        Logger.error("tag", "error", new IllegalStateException("error"));
        Logger.error("tag", "correlation", "error", new IllegalStateException("error"));
        Logger.errorPII("tag", "error-pii", new IllegalStateException("error-pii"));
        Logger.errorPII("tag", "correlation", "error-pii", new IllegalStateException("error-pii"));
        Logger.warn("tag", "warn");
        Logger.warn("tag", "correlation", "warn");
        Logger.warnPII("tag", "warn-pii");
        Logger.warnPII("tag", "correlation", "warn-pii");
        Logger.info("tag", "info");
        Logger.info("tag", "correlation", "info");
        Logger.infoPII("tag", "info-pii");
        Logger.infoPII("tag", "correlation", "info-pii");
        Logger.verbose("tag", "verbose");
        Logger.verbose("tag", "correlation", "verbose");
        Logger.verbosePII("tag", "verbose-pii");
        Logger.verbosePII("tag", "correlation", "verbose-pii");

        assertTrue(mRecords.isEmpty());
    }

    private void assertLogRecord(final int index,
                                 final String tag,
                                 final Logger.LogLevel level,
                                 final String message,
                                 final boolean containsPii) {
        final LogRecord record = mRecords.get(index);
        assertEquals(tag, record.mTag);
        assertEquals(level, record.mLogLevel);
        assertEquals(message, record.mMessage);
        assertEquals(containsPii, record.mContainsPii);
    }

    private static void assertDelegateLogLevel(
            final com.microsoft.identity.common.java.logging.Logger.LogLevel expected)
            throws Exception {
        assertEquals(expected, getStaticField(
                com.microsoft.identity.common.java.logging.Logger.class,
                "sLogLevel"));
    }

    private static void clearUnderlyingLoggers() throws Exception {
        final Map<?, ?> loggers = getLoggersMap();
        loggers.clear();
    }

    private static com.microsoft.identity.common.java.logging.ILoggerCallback getExternalLoggerCallback()
            throws Exception {
        final Object callback = getLoggersMap().get(EXTERNAL_LOGGER_IDENTIFIER);
        assertNotNull(callback);
        return (com.microsoft.identity.common.java.logging.ILoggerCallback) callback;
    }

    private static Map<?, ?> getLoggersMap() throws Exception {
        return (Map<?, ?>) getStaticField(
                com.microsoft.identity.common.java.logging.Logger.class,
                "sLoggers");
    }

    private static Object getStaticField(final Class<?> clazz, final String fieldName)
            throws Exception {
        final Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(null);
    }

    private static void setEmitDeprecationEvent(final boolean value) throws Exception {
        final Field field = Logger.class.getDeclaredField("sEmitDeprecationEvent");
        field.setAccessible(true);
        field.setBoolean(null, value);
    }

    private static final class LogRecord {
        private final String mTag;
        private final Logger.LogLevel mLogLevel;
        private final String mMessage;
        private final boolean mContainsPii;

        private LogRecord(final String tag,
                          final Logger.LogLevel logLevel,
                          final String message,
                          final boolean containsPii) {
            mTag = tag;
            mLogLevel = logLevel;
            mMessage = message;
            mContainsPii = containsPii;
        }
    }
}
