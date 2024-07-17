//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.common.internal.broker.ipc

import android.os.Bundle
import com.microsoft.identity.common.java.logging.Logger
import com.microsoft.identity.common.java.opentelemetry.AttributeName
import com.microsoft.identity.common.java.opentelemetry.SpanExtension

/**
 * An IPC Strategy wrapper with backups.
 * If the primary one fails, the backups will be used "best-effort".
 * If all mechanisms fail, only the exception of the main one will be thrown.
 * */
class IpcStrategyWithBackup (
    private val primary: IIpcStrategy,
    private val backup: List<IIpcStrategy>): IIpcStrategy{
    companion object{
        val TAG = IpcStrategyWithBackup::class.simpleName
    }

    override fun communicateToBroker(bundle: BrokerOperationBundle): Bundle? {
        val methodTag = "$TAG:communicateToBroker"
        return try {
            primary.communicateToBroker(bundle)
        } catch (t: Throwable) {
            Logger.info(methodTag, "Primary ipc failed : ${t.message}")
            val usedStrategies = mutableListOf<String>()
            for(ipc in backup) {
                try {
                    usedStrategies.add(ipc.getType().name)
                    SpanExtension.current().setAttribute(
                        AttributeName.backup_ipc_used.name,
                        usedStrategies.joinToString(",")
                    )
                    val result = ipc.communicateToBroker(bundle)
                    Logger.info(methodTag, "${ipc.getType().name} backup ipc succeeded.")
                    return result
                } catch (t: Throwable) {
                    Logger.info(methodTag, "${ipc.getType().name} backup ipc failed : ${t.message}")
                }
            }
            // If all backup fails... throw the original error.
            throw t
        }
    }

    override fun isSupportedByTargetedBroker(targetedBrokerPackageName: String): Boolean {
        return primary.isSupportedByTargetedBroker(targetedBrokerPackageName)
    }

    override fun getType(): IIpcStrategy.Type {
        return primary.getType()
    }
}