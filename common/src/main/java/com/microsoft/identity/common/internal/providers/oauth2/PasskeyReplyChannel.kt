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
package com.microsoft.identity.common.internal.providers.oauth2

import androidx.webkit.JavaScriptReplyProxy
import com.microsoft.identity.common.logging.Logger
import org.json.JSONArray
import org.json.JSONObject

/**
 * A communication channel to post replies back to JavaScript code running in a WebView via a
 * [JavaScriptReplyProxy].
 *
 * This class provides methods to send success and error messages back to the JavaScript context.
 * Messages are formatted as JSON arrays containing a status, data (or error message), and request type.
 *
 * @param replyProxy The [JavaScriptReplyProxy] used to send messages back to JavaScript.
 * @param requestType An optional string indicating the type of request being handled. Defaults to "unknown".
 */
class PasskeyReplyChannel(
    private val replyProxy: JavaScriptReplyProxy,
    private val requestType: String = "unknown"
) {
    companion object {
        const val TAG = "PasskeyReplyChannel"
        const val SUCCESS_STATUS = "success"
        const val ERROR_STATUS = "error"
    }


    sealed class ReplyMessage {
        abstract val type: String
        class Success(val json: String, override val type: String) : ReplyMessage()
        class Error(val errorMessage: String, override val type: String) : ReplyMessage()

        override fun toString(): String {
            val (status, data, typeValue) = when (this) {
                is Success -> {
                    val parsedData = runCatching { JSONObject(json) }.getOrElse { json }
                    Triple(SUCCESS_STATUS, parsedData, type)
                }
                is Error -> Triple(ERROR_STATUS, errorMessage, type)
            }
            return JSONArray(listOf(status, data, typeValue)).toString()
        }
    }



    fun postSuccess(json: String) {
        val methodTag = "$TAG:postSuccess"
        val message = ReplyMessage.Success(json, requestType)
        send(message)
        Logger.info(methodTag, "RequestType: $requestType, was successful.")
    }

    fun postError(errorMessage: String) {
        val methodTag = "$TAG:postError"
        val message = ReplyMessage.Error(errorMessage, requestType)
        send(message)
        Logger.error(methodTag, "RequestType: $requestType, failed with error: $errorMessage", null)

    }

    fun postError(throwable: Throwable) {
        val methodTag = "$TAG:postError"
        val  errorMessage = throwable.message ?: "Unknown error"
        val message = ReplyMessage.Error(errorMessage , requestType)
        send(message)
        Logger.error(methodTag, "RequestType: $requestType, failed with error: $errorMessage", throwable)
    }


    //@SuppressLint("RequiresFeature", "OldTargetApi")
    private fun send(message: ReplyMessage) {
        val methodTag = "$TAG:send"
        try {
            replyProxy.postMessage(message.toString())
        }catch (t: Throwable) {
            Logger.error(methodTag, "Reply message failed", t)
        }
    }
}
