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
package com.microsoft.identity.common.internal.broker

import com.google.gson.annotations.SerializedName

/**
 * Data class representing the JSON payload object received from AuthUX.
 *
 * @property correlationId The correlation ID for the request.
 * @property actionName The name of the action being performed.
 * @property actionComponent The component responsible for the action.
 * @property params The parameters for the action, including function and data.
 */
data class AuthUxJsonPayloadObject(
    @SerializedName(SerializedNames.CORRELATIONID)
    val correlationId: String?,

    @SerializedName(SerializedNames.ACTION_NAME)
    val actionName: String?,

    @SerializedName(SerializedNames.ACTION_COMPONENT)
    val actionComponent: String?,

    @SerializedName(SerializedNames.PARAMS)
    val params: AuthUxParams?
)

/**
 * Data class representing the parameters for the action, including function and data.
 *
 * @property function The function to be executed.
 * @property data The data associated with the function.
 */
data class AuthUxParams(
    @SerializedName(SerializedNames.FUNCTION)
    val function: String?,

    @SerializedName(SerializedNames.DATA)
    val data: AuthUxData?
)

/**
 * Data class representing the data associated with the JS API call.
 *
 * @property sessionId The session ID for the request.
 * @property numberMatch The number match value.
 */
data class AuthUxData(
    @SerializedName(SerializedNames.SESSIONID)
    val sessionId: String?,

    @SerializedName(SerializedNames.NUMBERMATCH)
    val numberMatch: String?
)

object SerializedNames {
    const val CORRELATIONID = "correlationID"
    const val ACTION_NAME = "action_name"
    const val ACTION_COMPONENT = "action_component"
    const val PARAMS = "params"
    const val FUNCTION = "function"
    const val DATA = "data"
    const val SESSIONID = "sessionID"
    const val NUMBERMATCH = "numberMatch"
}