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

import java.util.HashMap;
import java.util.Map;

import lombok.NonNull;

public class MockFlightsProvider implements IFlightsProvider {
    private final Map<String, String> mFlights;
    public MockFlightsProvider() {
        mFlights = new HashMap<>();
    }

    public void addFlight(String key, String value) {
        mFlights.put(key, value);
    }

    public void removeFlight(String key) {
        mFlights.remove(key);
    }
    @Override
    public boolean isFlightEnabled(IFlightConfig flightConfig) {
        return Boolean.parseBoolean(
                mFlights.getOrDefault(
                        flightConfig.getKey(), flightConfig.getDefaultValue().toString()));
    }

    @Override
    public boolean getBooleanValue(IFlightConfig flightConfig) {
        return false;
    }

    @Override
    public int getIntValue(IFlightConfig flightConfig) {
        return 0;
    }

    @Override
    public double getDoubleValue(IFlightConfig flightConfig) {
        return 0;
    }

    @Override
    public String getStringValue(IFlightConfig flightConfig) {
        return null;
    }

    @Override
    public JSONObject getJsonValue(@NonNull IFlightConfig flightConfig) {
        return null;
    }
}
