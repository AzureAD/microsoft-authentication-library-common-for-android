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
package com.microsoft.identity.common.java.controllers

interface IControllerFactory {

    /**
     * Returns the appropriate MSAL Controller depending on Authority, App and Device state
     *
     * 1) The client indicates it wants to use broker
     * 2) If not AAD Authority use local controller
     * 3) If the the authority is AAD and the Audience is instance of AnyPersonalAccount
     * Use the local controller
     * 4) If broker is not installed use local controller
     * 5) Otherwise return broker controller
     */
    fun getDefaultController(): BaseController

    /**
     * Returns one or more controllers to address a given request.
     *
     * The order of the response matters.  The local controller should be returned first in order to
     * ensure that any local refresh tokens are preferred over the use of the broker
     *
     * Only return the broker controller when the following are true:
     *
     * 1) The client indicates it wants to use broker
     * 2) The authority is AAD
     * 3) The audience is not AnyPersonalAccount
     * 4) The broker is installed
     * 5) The broker redirect URI for the client is registered
     */
    fun getAllControllers(): List<BaseController>
}