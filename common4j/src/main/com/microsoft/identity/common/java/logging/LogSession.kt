package com.microsoft.identity.common.java.logging

private const val PARENT_METHOD_INDEX = 1

class LogSession {
    companion object {
        private val logger: ILoggerCallback = ILoggerCallback { _, _, _, _ -> }

        fun log(
            tag: String,
            logLevel: Logger.LogLevel,
            message: String,
            containsPII: Boolean = false
        ) {
            val methodName = try {
                Throwable().stackTrace[PARENT_METHOD_INDEX].methodName
            } catch (exception: ArrayIndexOutOfBoundsException) {
                "<Unidentified method>"
            }
            logger.log(
                tag,
                logLevel,
                "In $methodName, $message",
                containsPII
            )
        }

        fun logMethodCall(tag: String, containsPII: Boolean = false) {
            val methodNameMessage = try {
                val throwable = Throwable()
                "${throwable.stackTrace[PARENT_METHOD_INDEX].methodName} method was called at line " +
                        "${throwable.stackTrace[PARENT_METHOD_INDEX].lineNumber}"
            } catch (exception: ArrayIndexOutOfBoundsException) {
                "<Unidentified method>"
            }

            logger.log(
                tag,
                Logger.LogLevel.INFO,
                methodNameMessage,
                containsPII
            )
        }

        fun logException(tag: String, containsPII: Boolean = false, throwable: Throwable) {
            logger.log(
                tag, Logger.LogLevel.ERROR,
                "Public Exception was thrown." +
                        "| Type: $throwable" +
                        "| Reason: ${throwable.message}",
                containsPII
            )
        }
    }
}