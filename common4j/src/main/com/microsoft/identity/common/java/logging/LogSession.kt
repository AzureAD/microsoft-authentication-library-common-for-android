package com.microsoft.identity.common.java.logging

import com.microsoft.identity.common.java.logging.Logger.LogLevel
import java.util.Collections.replaceAll

private const val PARENT_METHOD_INDEX = 2

class LogSession {
    companion object {

        fun log(
            tag: String,
            logLevel: LogLevel,
            message: String,
            containsPII: Boolean = false,
            throwable: Throwable? = null
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
                    if (throwable != null) {
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
                    } else {
                        Logger.error(tag, message, throwable)
                    }
                }
            }
        }

        fun logMethodCall(tag: String, containsPII: Boolean = false) {
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
                containsPII
            )
        }

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
