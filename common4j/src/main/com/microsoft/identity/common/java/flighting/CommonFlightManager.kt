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
 * Class to set Flight Provider for Common Flights
 * Consumer of commons needs to implement [IFlightsProvider] interface
 * and set it using CommonFlightManager.setFlightProvider(@NonNull IFlightsProvider flightProvider)
 * to provide Flight Values for CommonFlights
 * If no Flight Provider is set, default value of the flight will be used
 */
object CommonFlightManager : IFlightsManager {
    private lateinit var mFlightsManager: IFlightsManager
    private lateinit var mFlightProvider: IFlightsProvider

    fun initializeCommonFlightsManager(flightsManager: IFlightsManager) {
        mFlightsManager = flightsManager
        mFlightProvider = flightsManager.getFlightsProvider()
    }

    @JvmStatic
    fun setFlightProvider(flightProvider: IFlightsProvider) {
        mFlightProvider = flightProvider
    }

    /**
     * Checks if a flight is enabled
     * @param flightConfig flight to check
     * @return true if the flight is enabled otherwise returns the defaultValue
     */
    @JvmStatic
    fun isFlightEnabled(flightConfig: IFlightConfig): Boolean {
        return if (mFlightProvider == null) {
            flightConfig.getDefaultValue() as Boolean
        } else mFlightProvider!!.isFlightEnabled(
            flightConfig
        )
    }

    /**
     * Gets the value of an integer flight.
     *
     * @param flightConfig [IFlightConfig] to check
     * @return the flight value if set otherwise returns the defaultValue
     */
    @JvmStatic
    fun getIntValue(flightConfig: IFlightConfig): Int {
        return if (mFlightProvider == null) {
            flightConfig.getDefaultValue() as Int
        } else mFlightProvider!!.getIntValue(
            flightConfig
        )
    }
    override fun getFlightsProvider(): IFlightsProvider {
        return mFlightsManager.getFlightsProvider()
    }

    override fun getFlightsProviderForTenant(tenantId: String): IFlightsProvider {
        return mFlightsManager.getFlightsProviderForTenant(tenantId)
    }
}
