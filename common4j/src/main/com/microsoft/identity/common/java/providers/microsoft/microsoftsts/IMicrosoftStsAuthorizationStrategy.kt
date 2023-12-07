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
package com.microsoft.identity.common.java.providers.microsoft.microsoftsts

import com.microsoft.identity.common.java.commands.parameters.BrokerInteractiveTokenCommandParameters
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.util.ported.PropertyBag

/**
 * Provides methods to perform authorization to acquire token from
 * Microsoft STS
 */
interface IMicrosoftStsAuthorizationStrategy {
    /**
     * Acquire authorization for given parameters
     * @param parameters Incoming request parameters
     * @return [MicrosoftStsAuthorizationRequest]
     */
    @Throws(ClientException::class)
    fun createAuthorizationRequest(parameters: BrokerInteractiveTokenCommandParameters): MicrosoftStsAuthorizationRequest

    /**
     * Request authorization
     * @param authorizationRequest [MicrosoftStsAuthorizationRequest]
     * @return [MicrosoftStsAuthorizationResult]
     */
    @Throws(ClientException::class)
    fun requestAuthorization(authorizationRequest: MicrosoftStsAuthorizationRequest): MicrosoftStsAuthorizationResult

    /**
     * To signal complete complete authorization.
     * @param requestCode Interactive request's request code
     * @param resultCode Interactive request's result code
     * @param data values used to pass to complete authorization.
     */
    fun completeAuthorization(requestCode: Int, resultCode: Int, data: PropertyBag)
}