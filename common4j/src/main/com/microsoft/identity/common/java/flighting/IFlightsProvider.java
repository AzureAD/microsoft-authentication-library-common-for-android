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
package com.microsoft.identity.common.java.flighting;

import org.json.JSONObject;

import lombok.NonNull;

/**
 * Interface for flights provider to read flights values.
 */
public interface IFlightsProvider {
    /**
     * Checks if a flight is enabled.
     *
     * @param flightConfig flight to check
     * @return true if the flight is enabled otherwise returns the defaultValue
     */
    boolean isFlightEnabled(@NonNull IFlightConfig flightConfig);

    /**
     * Gets the value of a boolean flight.
     *
     * @param flightConfig {@link IFlightConfig} to check
     * @return the flight value if set otherwise returns the defaultValue
     */
    boolean getBooleanValue(@NonNull IFlightConfig flightConfig);

    /**
     * Gets the value of an integer flight.
     *
     * @param flightConfig {@link IFlightConfig} to check
     * @return the flight value if set otherwise returns the defaultValue
     */
    int getIntValue(@NonNull IFlightConfig flightConfig);

    /**
     * Gets the value of a double flight.
     *
     * @param flightConfig {@link IFlightConfig} to check
     * @return the flight value if set otherwise returns the defaultValue
     */
    double getDoubleValue(@NonNull IFlightConfig flightConfig);

    /**
     * Gets the value of a flight.
     *
     * @param flightConfig {@link IFlightConfig} flight to check
     * @return the flight value if set otherwise returns the defaultValue
     */
    String getStringValue(@NonNull IFlightConfig flightConfig);

    /**
     * Gets the value of a JSON Object flight.
     *
     * @param flightConfig {@link IFlightConfig} flight to check
     * @return the flight value if set otherwise returns the defaultValue
     */
    JSONObject getJsonValue(@NonNull IFlightConfig flightConfig);
}
