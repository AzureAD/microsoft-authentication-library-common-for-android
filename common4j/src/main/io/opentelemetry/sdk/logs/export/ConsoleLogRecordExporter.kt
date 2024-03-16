package io.opentelemetry.sdk.logs.export

import com.microsoft.identity.common.java.logging.ILoggerCallback
import com.microsoft.identity.common.java.logging.Logger
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.logs.data.LogRecordData


class ConsoleLogRecordExporter : LogRecordExporter {



    companion object {
        @JvmStatic
        var callback2 : ILoggerCallback? = null
    }

    /**
     * Exports the collections of given [LogRecordData].
     *
     * @param logs the collection of [LogRecordData] to be exported
     * @return the result of the export, which is often an asynchronous operation
     */
    override fun export(logs: MutableCollection<LogRecordData>): CompletableResultCode {
        logs.forEach { log ->
            println(log)
            callback2?.log("sf", Logger.LogLevel.VERBOSE, "log.body", false)
        }
        return CompletableResultCode.ofSuccess()
    }

    /**
     * Exports the collection of [LogRecordData] that have not yet been exported.
     *
     * @return the result of the flush, which is often an asynchronous operation
     */
    override fun flush(): CompletableResultCode {
        return CompletableResultCode.ofSuccess()
    }

    /**
     * Shutdown the log exporter. Called when [SdkLoggerProvider.shutdown] is called when this
     * exporter is registered to the provider via [BatchLogRecordProcessor] or [ ].
     *
     * @return a [CompletableResultCode] which is completed when shutdown completes
     */
    override fun shutdown(): CompletableResultCode {
        return CompletableResultCode.ofSuccess()
    }
}