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
package com.microsoft.identity.common.internal.broker.ipc

import com.google.gson.annotations.SerializedName
import com.microsoft.identity.common.java.request.SdkType

/**
 * Additional required parameters for WebApps operations.
 */
data class WebAppsAdditionalRequiredParameters(
    // A parameter from Edge which indicates whether the calling application can show UI.
    @SerializedName(FIELD_CAN_SHOW_UI)
    val canShowUi: Boolean = false,

    @SerializedName(FIELD_CALLING_PACKAGE_NAME)
    val callingPackageName: String,

    @SerializedName(FIELD_CALLING_APPLICATION_NAME)
    val callingApplicationName: String,

    @SerializedName(FIELD_CALLING_APPLICATION_VERSION)
    val callingApplicationVersion: String,

    @SerializedName(FIELD_SDK_TYPE)
    val sdkType: SdkType,

    @SerializedName(FIELD_SDK_VERSION)
    val sdkVersion: String
) {
    companion object {
        const val FIELD_CAN_SHOW_UI = "canShowUi"
        const val FIELD_CALLING_PACKAGE_NAME = "callingPackageName"
        const val FIELD_CALLING_APPLICATION_NAME = "callingApplicationName"
        const val FIELD_CALLING_APPLICATION_VERSION = "callingApplicationVersion"
        const val FIELD_SDK_TYPE = "sdkType"
        const val FIELD_SDK_VERSION = "sdkVersion"
    }
}
