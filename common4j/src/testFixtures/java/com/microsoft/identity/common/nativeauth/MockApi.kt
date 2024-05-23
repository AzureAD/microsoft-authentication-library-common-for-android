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

package com.microsoft.identity.common.nativeauth

import com.google.gson.annotations.SerializedName
import com.microsoft.identity.common.java.net.HttpConstants
import com.microsoft.identity.common.java.net.UrlConnectionHttpClient
import com.microsoft.identity.common.java.util.ObjectMapper
import org.junit.Assert.assertTrue
import java.net.URL
import java.util.TreeMap

const val CORRELATION_ID =  "correlationId"
const val ENDPOINT = "endpoint"
const val RESPONSE_LIST = "responseList"

/**
 * MockApi class performs the various tasks associated with making request to the mock API
 * for Native Auth. These mock APIs are useful in performing integration tests for
 * Native Auth classes.
 */
class MockApi private constructor(
    private val httpClient: UrlConnectionHttpClient = UrlConnectionHttpClient.getDefaultInstance()
) {
    companion object {

        // The config endpoint for the mock API provides the
        // ability to clients to add a response to the queue, see existing responses and
        // delete all responses in the queue
        // This endpoint allows a client to add a response to the response queue. When the client
        // makes a request with the matching correlation-id, the mock API will return that response
        private val MOCK_ADD_RESPONSE_URL = "${ApiConstants.BASEPATH}/config/response"

        private val headers = TreeMap<String, String?>().also {
            it[HttpConstants.HeaderField.CONTENT_TYPE] = "application/json"
        }

        lateinit var instance: MockApi

        fun create() {
            if (this::instance.isInitialized) {
                throw IllegalStateException("MockApi already initialised")
            } else {
                instance = MockApi()
            }
        }

        private fun getEncodedRequest(request: Request): String {
            return ObjectMapper.serializeObjectToJsonString(request)
        }
    }


    /**
     * Performs a HTTP POST request to the MockAPI for Native Auth. This method validates the request
     * was successful
     */
    fun performRequest(endpointType: MockApiEndpoint, responseType: MockApiResponseType, correlationId: String) {
        val addResponseUrl = URL(MOCK_ADD_RESPONSE_URL)
        val request = Request(
            correlationId = correlationId,
            endpoint = endpointType.stringValue,
            responseList = listOf(responseType.stringValue)
        )
        val encodedRequest = getEncodedRequest(request)

        val result = httpClient.post(
            addResponseUrl,
            headers,
            encodedRequest.toByteArray(charset(ObjectMapper.ENCODING_SCHEME))
        )
        assertTrue(result.statusCode == 200)
    }
}

/**
 * Data class to represent the request object send to the MockAPI for Native Auth
 */
data class Request(
    @SerializedName(CORRELATION_ID) val correlationId: String,
    @SerializedName(ENDPOINT) val endpoint: String,
    @SerializedName(RESPONSE_LIST) val responseList: List<String>
)
