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
package com.microsoft.identity.deviceregistration.java.exception

import com.google.gson.JsonArray
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.microsoft.identity.common.java.exception.BaseException
import com.microsoft.identity.common.java.logging.Logger
import java.net.HttpURLConnection

/**
 * DRS could return errors in 2 different formats.
 * (Newer endpoint versions would return an error under odata.error).
 * This class will handle both and parse accordingly.
 *
 * Its exception message might contain a response from DRS (JSON string) or a basic error string.
 *
 * @param httpErrorCode             HTTP Error code of this DRS request.
 * @param errorCode                 Error code from DRS (extracted from fullErrorResponse).
 * @param fullErrorResponse         Full error response (could be a JSON from DRS,
 *                                      or a regular string if the error occurs on the client side - i.e. response parsing failure.)
 * @param extractedErrorMessage     Error message from DRS (extracted from fullErrorResponse).
 * @param operation                 Type of this DRS request, i.e. DeviceJoin.
 * @param time                      Timestamp of the request.
 */
open class DrsErrorResponseException(
    val httpErrorCode: Int,
    errorCode: String,
    fullErrorResponse: String?,
    val extractedErrorMessage: String?,
    val operation: String?,
    val time: String?) : BaseException(errorCode, fullErrorResponse) {
    companion object {
        private val TAG = DrsErrorResponseException::class.simpleName
        private const val CODE = "code"
        private const val SUB_CODE = "subcode"
        private const val MESSAGE = "message"
        private const val OPERATION = "operation"
        private const val TIME = "time"
        private const val REQUEST_ID = "requestid"
        private const val ODATA_ERROR = "odata.error"
        private const val VALUE = "value"
        private const val VALUES = "values"
        private const val ITEM = "item"
        private const val DATE = "date"
        const val DEFAULT_ERROR_CODE = "wpj_unknown_error"

        @JvmStatic
        fun getFromErrorResponse(drsErrorResponse: String?):
                DrsErrorResponseException {
            return getFromErrorResponse(HttpURLConnection.HTTP_OK, drsErrorResponse)
        }

        @JvmStatic
        fun getFromErrorResponse(httpErrorCode: Int, drsErrorResponse: String?):
                DrsErrorResponseException {
            val jsonResponse = getJsonObject(drsErrorResponse)

            return if (jsonResponse.get(ODATA_ERROR) != null) {
                // with odata, the error is formatted differently.
                val odataError = jsonResponse.getAsJsonObject(ODATA_ERROR)
                val valuesJsonArray = odataError.getAsJsonArray(VALUES)

                val exception = DrsErrorResponseException(
                    httpErrorCode = httpErrorCode,
                    errorCode = getFromJsonObject(odataError, CODE, DEFAULT_ERROR_CODE),
                    fullErrorResponse = drsErrorResponse,
                    extractedErrorMessage = getFromJsonObject(odataError.getAsJsonObject(MESSAGE), VALUE),
                    operation = null,
                    time = valuesJsonArray?.let { getFromJsonArray(it, DATE) }
                )

                exception.correlationId = valuesJsonArray?.let { getFromJsonArray(it, REQUEST_ID) }
                exception.subErrorCode = valuesJsonArray?.let { getFromJsonArray(it, SUB_CODE) }
                exception
            } else {
                val exception = DrsErrorResponseException(
                    httpErrorCode = httpErrorCode,
                    errorCode = getFromJsonObject(jsonResponse, CODE, DEFAULT_ERROR_CODE),
                    fullErrorResponse = drsErrorResponse,
                    extractedErrorMessage = getFromJsonObject(jsonResponse, MESSAGE),
                    operation = getFromJsonObject(jsonResponse, OPERATION),
                    time = getFromJsonObject(jsonResponse, TIME)
                )
                exception.subErrorCode = getFromJsonObject(jsonResponse, SUB_CODE)
                exception.correlationId = getFromJsonObject(jsonResponse, REQUEST_ID)
                exception
            }
        }

        private fun getFromJsonObject(obj: JsonObject, key: String, defaultString: String): String {
            val result = obj.get(key)
            if (result == null || result is JsonNull){
                return defaultString
            }

            return result.asString
        }

        private fun getFromJsonObject(obj: JsonObject, key: String): String? {
            val result = obj.get(key)
            if (result == null || result is JsonNull){
                return null
            }

            return result.asString
        }

        private fun getFromJsonArray(array: JsonArray, key: String): String? {
            val element = array.find { jsonElement ->
                jsonElement.asJsonObject.get(ITEM)?.asString.equals(key, ignoreCase = true)
            }

            if (element !is JsonObject){
                return null
            }

            return getFromJsonObject(element.asJsonObject, VALUE)
        }

        /**
         * Parse the error response from DRS and return a JsonObject.
         * If the error response cannot be parsed, return an empty JsonObject.
         */
        private fun getJsonObject(drsErrorResponse: String?): JsonObject {
            val methodTag = "$TAG:getJsonObject"
            return try {
                JsonParser.parseString(drsErrorResponse).asJsonObject
            } catch (e: Throwable) {
                Logger.error(methodTag, "Failed to parse DRS error response", e)
                JsonParser.parseString("{}").asJsonObject
            }
        }
    }
}
