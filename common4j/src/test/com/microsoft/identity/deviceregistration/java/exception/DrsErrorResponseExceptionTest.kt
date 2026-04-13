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

import com.microsoft.identity.deviceregistration.java.exception.DrsErrorResponseException
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class
DrsErrorResponseExceptionTest {

    companion object {
        private const val CODE = "invalid_request"
        private const val SUB_CODE = "error_directory_quota_exceeded"
        private const val MESSAGE = "User '0c95131f-020b-48e0-999f-2553604a2bd0' is not eligible to enroll a device of type 'Android'. Reason 'DeviceCapReached'."
        private const val OPERATION = "DeviceJoin"
        private const val TIME = "12-30-2023 17:00:40Z"
        private const val REQUEST_ID = "1083df47-c860-48c8-b2b9-caf93288058f"
        private const val ERROR_RESPONSE = "{\"code\":\"${CODE}\"," +
                "\"subcode\":\"${SUB_CODE}\"," +
                "\"message\":\"${MESSAGE}\"," +
                "\"operation\":\"${OPERATION}\"," +
                "\"requestid\":\"${REQUEST_ID}\"," +
                "\"time\":\"${TIME}\"}"

        private const val ODATA_CODE = "directory_error"
        private const val ODATA_SUB_CODE = "error_user_not_registered_device_owner"
        private const val ODATA_MESSAGE = "User '0c95131f-020b-48e0-999f-2553604a2bd0' is not eligible to enroll a device of type 'Android'. Reason 'DeviceCapReached'."
        private const val ODATA_TIME = "05-30-2025 22:52:53Z"
        private const val ODATA_REQUEST_ID = "44abc15a-9a48-48ba-b4f0-f4d547847801"
        private const val ODATA_ERROR_RESPONSE = "{\"odata.error\":" +
                "{\"code\":\"$ODATA_CODE\"," +
                "\"message\":{\"lang\":\"en\"," +
                "\"value\":\"$ODATA_MESSAGE\"}," +
                "\"values\":[{\"item\":\"subCode\"," +
                "\"value\":\"$ODATA_SUB_CODE\"}," +
                "{\"item\":\"requestId\",\"value\":\"$ODATA_REQUEST_ID\"}," +
                "{\"item\":\"date\",\"value\":\"$ODATA_TIME\"}]}}"
    }

    @Test
    fun testWithInvalidErrorMessage() {
        val drsErrorResponseException = DrsErrorResponseException.getFromErrorResponse(400, "invalid_error_message")
        Assert.assertEquals(400, drsErrorResponseException.httpErrorCode)
        Assert.assertEquals("invalid_error_message", drsErrorResponseException.message)
        Assert.assertEquals(DrsErrorResponseException.DEFAULT_ERROR_CODE, drsErrorResponseException.errorCode)
        Assert.assertNull(drsErrorResponseException.subErrorCode)
        Assert.assertNull(drsErrorResponseException.extractedErrorMessage)
        Assert.assertNull(drsErrorResponseException.operation)
        Assert.assertNull(drsErrorResponseException.time)
        Assert.assertNull(drsErrorResponseException.correlationId)
    }

    @Test
    fun testWithNullErrorMessage() {
        val drsErrorResponseException = DrsErrorResponseException.getFromErrorResponse(400, null)
        Assert.assertEquals(400, drsErrorResponseException.httpErrorCode)
        Assert.assertEquals(DrsErrorResponseException.DEFAULT_ERROR_CODE, drsErrorResponseException.errorCode)
        Assert.assertNull(drsErrorResponseException.message)
        Assert.assertNull(drsErrorResponseException.subErrorCode)
        Assert.assertNull(drsErrorResponseException.extractedErrorMessage)
        Assert.assertNull(drsErrorResponseException.operation)
        Assert.assertNull(drsErrorResponseException.time)
        Assert.assertNull(drsErrorResponseException.correlationId)
    }

    @Test
    fun testValidErrorMessage() {
        val drsErrorResponseException = DrsErrorResponseException.getFromErrorResponse(400, ERROR_RESPONSE)
        Assert.assertEquals(400, drsErrorResponseException.httpErrorCode)
        Assert.assertEquals(ERROR_RESPONSE, drsErrorResponseException.message)
        Assert.assertEquals(CODE, drsErrorResponseException.errorCode)
        Assert.assertEquals(SUB_CODE, drsErrorResponseException.subErrorCode)
        Assert.assertEquals(MESSAGE, drsErrorResponseException.extractedErrorMessage)
        Assert.assertEquals(OPERATION, drsErrorResponseException.operation)
        Assert.assertEquals(TIME, drsErrorResponseException.time)
        Assert.assertEquals(REQUEST_ID, drsErrorResponseException.correlationId)
    }

    @Test
    fun testODataErrorMessage(){
        val drsErrorResponseException = DrsErrorResponseException.getFromErrorResponse(400, ODATA_ERROR_RESPONSE)
        Assert.assertEquals(400, drsErrorResponseException.httpErrorCode)
        Assert.assertEquals(ODATA_ERROR_RESPONSE, drsErrorResponseException.message)
        Assert.assertEquals(ODATA_CODE, drsErrorResponseException.errorCode)
        Assert.assertEquals(ODATA_SUB_CODE, drsErrorResponseException.subErrorCode)
        Assert.assertEquals(ODATA_MESSAGE, drsErrorResponseException.extractedErrorMessage)
        Assert.assertNull(drsErrorResponseException.operation)
        Assert.assertEquals(ODATA_TIME, drsErrorResponseException.time)
        Assert.assertEquals(ODATA_REQUEST_ID, drsErrorResponseException.correlationId)
    }
}
