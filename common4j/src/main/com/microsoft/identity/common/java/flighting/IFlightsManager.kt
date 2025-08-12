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
package com.microsoft.identity.common.java.flighting

/**
 * Consumer of commons needs to implement [IFlightsManager] interface
 * and set it using CommonFlightManager.initializeCommonFlightsManager(@NonNull IFlightsManager flightsManager)
 * to provide Flight Values for CommonFlights
 */
interface IFlightsManager {
    /**
     * Flights provider applicable by default. Features should always this
     * unless the feature behaviour is tenant specific one same device.
     * Calls [getFlightsProvider] with a default timeout of 0 milliseconds. 0 indicates no wait.
     */
    fun getFlightsProvider(): IFlightsProvider = getFlightsProvider(0)

    /**
     * Flights provider applicable by default. Features should always use this
     * unless the feature behaviour is tenant specific one same device.
     * @param waitForConfigsWithTimeoutInMs The timeout in milliseconds to wait for initial configurations from source.
     */
    fun getFlightsProvider(waitForConfigsWithTimeoutInMs: Long = 0): IFlightsProvider

    /**
     * Flights provider for the given tenant
     * @param tenantId The tenant ID for which the flights provider is requested.
     * Calls [getFlightsProviderForTenant] with a default timeout of 0 milliseconds. 0 indicates no wait.
     */
    fun getFlightsProviderForTenant(tenantId: String): IFlightsProvider = getFlightsProviderForTenant(tenantId, 0)

    /**
     * Flights provider for the given tenant
     * @param tenantId The tenant ID for which the flights provider is requested.
     * @param waitForConfigsWithTimeoutInMs The timeout in milliseconds to wait for initial configurations from source.
     */
    fun getFlightsProviderForTenant(tenantId: String, waitForConfigsWithTimeoutInMs: Long = 0): IFlightsProvider
}