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

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.annotations.SerializedName
import java.lang.reflect.Type

/**
 * Data class representing the JSON payload object received from AuthUX.
 *
 * @property correlationId The correlation ID for the request.
 * @property actionName The name of the action being performed.
 * @property actionComponent The component responsible for the action.
 * @property params The parameters for the action, including function and data.
 */
data class AuthUxJsonPayload(
    val correlationId: String,
    val actionName: String,
    val actionComponent: String,
    val params: AuthUxParams?
)

/**
 * Data class representing the parameters (`params`) object of an AuthUX bridge message.
 *
 * Every field is optional/nullable so the same model can carry different actions and so that an
 * absent field never breaks parsing. Unknown JSON fields added by future Auth UX versions are
 * tolerated and ignored by Gson.
 *
 * @property operation The operation to be executed on the number-match / `write_data` path
 *  (e.g. `number_matching`).
 * @property sessionId Session identifier (`sessionID`).
 * @property codeMatch Number-match value (`code_match`).
 * @property version Payload schema version (`v`) of the telemetry contract.
 * @property errorCode Opaque Auth UX server error code (e.g. an STS error code such as "530003")
 *  carried by the `log_telemetry` action. Treated as an opaque telemetry value only; it is never
 *  used to drive client behavior. Optional/nullable — its absence results in a no-op. Captured as a
 *  string even when sent as a JSON number.
 * @property pageId Auth UX page identifier (`pageId`, e.g. "ConvergedTFA"); telemetry context for
 *  the downstream onboarding sink (AB#3688632).
 * @property trackingId Auth UX tracking identifier (`trackingId`); telemetry context for the
 *  downstream onboarding sink (AB#3688632).
 */
data class AuthUxParams(
    @SerializedName(SerializedNames.OPERATION)
    val operation: String? = null,

    @SerializedName(SerializedNames.SESSION_ID)
    val sessionId: String? = null,

    @SerializedName(SerializedNames.CODE_MATCH)
    val codeMatch: String? = null,

    @SerializedName(SerializedNames.VERSION)
    val version: Int? = null,

    @SerializedName(SerializedNames.ERROR_CODE)
    val errorCode: String? = null,

    @SerializedName(SerializedNames.PAGE_ID)
    val pageId: String? = null,

    @SerializedName(SerializedNames.TRACKING_ID)
    val trackingId: String? = null
)

class AuthUxJsonPayloadKTDeserializer : JsonDeserializer<AuthUxJsonPayload> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): AuthUxJsonPayload {
        val jsonObject = json.asJsonObject

        // Validate required fields
        val correlationId = jsonObject.get(SerializedNames.CORRELATION_ID)?.asString
            ?: throw JsonParseException("correlationID is required and cannot be null")
        val actionName = jsonObject.get(SerializedNames.ACTION_NAME)?.asString
            ?: throw JsonParseException("action_name is required and cannot be null")
        val actionComponent = jsonObject.get(SerializedNames.ACTION_COMPONENT)?.asString
            ?: throw JsonParseException("action_component is required and cannot be null")

        // Deserialize params if present
        val params = jsonObject.get("params")?.let {
            context.deserialize<AuthUxParams>(it, AuthUxParams::class.java)
        }

        return AuthUxJsonPayload(
            correlationId = correlationId,
            actionName = actionName,
            actionComponent = actionComponent,
            params = params
        )
    }
}

object SerializedNames {
    const val CORRELATION_ID = "correlationID"
    const val ACTION_NAME = "action_name"
    const val ACTION_COMPONENT = "action_component"
    const val PARAMS = "params"
    const val OPERATION = "operation"
    const val SESSION_ID = "sessionID"
    const val CODE_MATCH = "code_match"
    const val VERSION = "v"
    const val ERROR_CODE = "errorCode"
    const val PAGE_ID = "pageId"
    const val TRACKING_ID = "trackingId"
}
