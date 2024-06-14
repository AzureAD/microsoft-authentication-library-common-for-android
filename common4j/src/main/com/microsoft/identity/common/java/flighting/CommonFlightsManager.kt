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

import com.microsoft.identity.common.java.logging.Logger
import org.json.JSONObject

/**
 * Consumer of commons needs to implement [IFlightsManager] interface
 * and set it using CommonFlightManager.initializeCommonFlightsManager(@NonNull IFlightsManager flightsManager)
 * to provide Flight Values for CommonFlights
 * If no Flight Provider is set, default value of the flight will be used
 */
object CommonFlightsManager : IFlightsManager {
    private val TAG = CommonFlightsManager::class.java.simpleName
    private var mFlightsManager: IFlightsManager = DefaultValueFlightsManager

    fun initializeCommonFlightsManager(flightsManager: IFlightsManager) {
        val methodTag = "$TAG:initializeCommonFlightsManager"
        Logger.info(methodTag, "initializing common flights manager with " + flightsManager.javaClass.simpleName)
        mFlightsManager = flightsManager
    }

    /**
     * For tests
     */
    fun resetFlightsManager() {
        val methodTag = "$TAG:resetFlightsManager"
        Logger.info(methodTag, "Resetting flights manager to default value.")
        mFlightsManager = DefaultValueFlightsManager
    }

    override fun getFlightsProvider(): IFlightsProvider {
        return mFlightsManager.getFlightsProvider()
    }

    override fun getFlightsProviderForTenant(tenantId: String): IFlightsProvider {
        return mFlightsManager.getFlightsProviderForTenant(tenantId)
    }

    /**
     * Default Flight Provider for Common Flights
     * If no FlightsManager is set by consuming projects like Broker or MSAL, this
     * provider is used as default flights provider for all purposes. It returns
     * default value for the flight config provided.
     */
    private object DefaultValueFlightsProvider : IFlightsProvider {
        override fun isFlightEnabled(flightConfig: IFlightConfig): Boolean {
            return getBooleanValue(flightConfig)
        }

        override fun getBooleanValue(flightConfig: IFlightConfig): Boolean {
            return flightConfig.defaultValue as Boolean
        }

        override fun getIntValue(flightConfig: IFlightConfig): Int {
            return flightConfig.defaultValue as Int
        }

        override fun getDoubleValue(flightConfig: IFlightConfig): Double {
            return flightConfig.defaultValue as Double
        }

        override fun getStringValue(flightConfig: IFlightConfig): String {
            return flightConfig.defaultValue as String
        }

        override fun getJsonValue(flightConfig: IFlightConfig): JSONObject {
            return flightConfig.defaultValue as JSONObject
        }
    }

    /**
     * Default Value FlightsManager for Common Flights
     * If no FlightsManager is set by consuming projects like Broker or MSAL, this
     * provider is used as default flights provider for all purposes. It returns
     * default value for the flight config provided.
     */
    private object DefaultValueFlightsManager : IFlightsManager {
        override fun getFlightsProvider(): IFlightsProvider {
            return DefaultValueFlightsProvider
        }

        override fun getFlightsProviderForTenant(tenantId: String): IFlightsProvider {
            return DefaultValueFlightsProvider
        }
    }
}
