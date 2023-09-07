package com.microsoft.identity.common.java.logging

import com.microsoft.identity.common.java.logging.Logger.LogLevel
import java.util.Collections.replaceAll

private const val PARENT_METHOD_INDEX = 2

/**
 * LogSession provides a wrapper on the Logger class from package
 * com.microsoft.identity.common.java.logging
 */
class LogSession {
    companion object {

        /**
         * @param tag          Used to identify the source of a log message.
         *                     It usually identifies the class or activity where the log call occurs.
         * @param logLevel     Denotes log level See{@link LogLevel}
         * @param message      The message to log.
         * @param containsPII  is true if the message may contain PII
         */
        fun log(
            tag: String,
            logLevel: LogLevel,
            message: String,
            containsPII: Boolean = false
        ) {
            when (logLevel) {
                LogLevel.INFO -> {
                    if (containsPII) {
                        Logger.infoPII(tag, message)
                    } else {
                        Logger.info(tag, message)
                    }
                }
                LogLevel.WARN -> {
                    if (containsPII) {
                        Logger.warnPII(tag, message)
                    } else {
                        Logger.warn(tag, message)
                    }
                }
                LogLevel.UNDEFINED, LogLevel.VERBOSE -> {
                    if (containsPII) {
                        Logger.verbosePII(tag, message)
                    } else {
                        Logger.verbose(tag, message)
                    }
                }
                LogLevel.ERROR -> {
                    if (containsPII) {
                        Logger.errorPII(tag, message, null)
                    } else {
                        Logger.error(tag, message, null)
                    }
                }
            }
        }

        /**
         * @param tag          Used to identify the source of a log message.
         *                     It usually identifies the class or activity where the log call occurs.
         */
        fun logMethodCall(tag: String) {
            val methodNameMessage = try {
                val throwable = Throwable()
                "${throwable.stackTrace[PARENT_METHOD_INDEX].methodName.replace(Regex("\\$\\w+"), "")} method was called"
            } catch (exception: ArrayIndexOutOfBoundsException) {
                "<Unidentified method>"
            }

            log(
                tag,
                LogLevel.INFO,
                methodNameMessage,
                false
            )
        }

        /**
         * @param tag          Used to identify the source of a log message.
         *                     It usually identifies the class or activity where the log call occurs.
         * @param methodName   The methodName to log.
         */
        fun logMethodCall(tag: String, methodName: String) {
            Logger.info(tag, methodName)
        }

        /**
         * @param tag          Used to identify the source of a log message.
         *                     It usually identifies the class or activity where the log call occurs.
         * @param containsPII  is true if the message may contain PII
         * @param throwable    The exception to log
         */
        fun logException(tag: String, containsPII: Boolean = false, throwable: Throwable) {
            if (containsPII) {
                Logger.errorPII(
                    tag, "Exception was thrown." +
                            "| Type: $throwable" +
                            "| Reason: ${throwable.message}", throwable
                )
            } else {
                Logger.error(
                    tag, "Exception was thrown." +
                            "| Type: $throwable" +
                            "| Reason: ${throwable.message}", throwable
                )
            }
        }
    }
}
