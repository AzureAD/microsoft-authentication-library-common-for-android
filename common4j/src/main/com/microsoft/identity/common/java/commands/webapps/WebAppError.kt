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
package com.microsoft.identity.common.java.commands.webapps

import com.google.gson.annotations.SerializedName

/**
 * This class represents an error that occurs during WebApps operations.
 */
data class WebAppError(
    @SerializedName(FIELD_CODE)
    val errorCode: String? = BROKER_ERROR_CODE,

    @SerializedName(FIELD_DESCRIPTION)
    val description: String,

    @SerializedName(FIELD_EXTRA)
    val extra: WebAppErrorDetails
) {
    companion object {
        const val FIELD_CODE = "code"
        const val FIELD_DESCRIPTION = "description"
        const val FIELD_EXTRA = "ext"
        const val BROKER_ERROR_CODE = "OSError" // Per the protocol, this is the dedicated error code for broker-related errors.
    }

    /**
     * Secondary constructor that creates a WebAppError from a Throwable.
     * We try to determine the corresponding error status code from the throwable.
     *
     * @param throwable The Throwable that caused the error.
     * @param description A description of the error.
     */
    constructor(throwable: Throwable, description: String) : this(
        errorCode = BROKER_ERROR_CODE,
        description = description,
        extra = WebAppErrorDetails(
            error = 0,
            status = WebAppBrokerErrorCode.fromThrowable(throwable).name
        )
    )

    /**
     * Secondary constructor that creates a WebAppError from a Throwable and MatsProperties.
     * We try to determine the corresponding error status code from the throwable.
     *
     * @param throwable The Throwable that caused the error.
     * @param description A description of the error.
     * @param matsProperties Additional properties related to the error context.
     */
    constructor(throwable: Throwable, description: String, matsProperties: MatsProperties) : this(
        errorCode = BROKER_ERROR_CODE,
        description = description,
        extra = WebAppErrorDetails(
            error = 0,
            status = WebAppBrokerErrorCode.fromThrowable(throwable).name,
            properties = matsProperties
        )
    )
}
