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
package com.microsoft.identity.common.java.nativeauth.providers

import com.microsoft.identity.common.java.AuthenticationConstants
import com.microsoft.identity.common.java.logging.LibraryInfoHelper
import com.microsoft.identity.common.java.platform.Device
import java.util.TreeMap

/**
 * Builds the SDK identity headers that every Native Auth request carries, regardless of protocol
 * version: the caller-supplied correlation id, the SDK product/version fields, and the platform id
 * parameters.
 *
 * This holds only the version-invariant block. Version-specific headers - the `Content-Type`, and
 * any future V2-only headers such as a HAL `Accept` value or caller-supplied interceptor headers -
 * are composed on top by the individual request providers. Keeping the shared surface free of
 * version-specific parameters means V2 can evolve without touching the code path behind the shipped
 * V1 flows, and removing V1 later is a deletion rather than a refactor.
 */
internal object NativeAuthSdkHeaders {

    /**
     * The sentinel used by the command layer when no correlation id was supplied. Requests omit the
     * client-request-id header entirely in that case rather than sending the placeholder.
     */
    const val UNSET_CORRELATION_ID = "UNSET"

    /**
     * Returns the version-invariant Native Auth headers.
     *
     * The returned map is mutable by design: callers add their own `Content-Type` and any
     * version-specific headers before handing it to a request object.
     *
     * @param correlationId the correlation id for the request, or [UNSET_CORRELATION_ID] if none was set.
     */
    fun base(correlationId: String): MutableMap<String, String?> {
        val headers: MutableMap<String, String?> = TreeMap()
        if (correlationId != UNSET_CORRELATION_ID) {
            headers[AuthenticationConstants.AAD.CLIENT_REQUEST_ID] = correlationId
        }
        headers[AuthenticationConstants.SdkPlatformFields.PRODUCT] = LibraryInfoHelper.getLibraryName()
        headers[AuthenticationConstants.SdkPlatformFields.VERSION] = LibraryInfoHelper.getLibraryVersion()
        headers.putAll(Device.getPlatformIdParameters())
        return headers
    }
}
