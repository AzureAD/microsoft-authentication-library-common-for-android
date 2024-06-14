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

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link CommonFlightsManager}.

 */
public class CommonFlightsManagerTest {

    @Test
    public void testCommonFlightsManager() {
        // first test when no consumer don't set the flights manager
        Assert.assertFalse(CommonFlightsManager.INSTANCE.getFlightsProvider().isFlightEnabled(MockFlights.ENABLED_FLIGHT));
        Assert.assertFalse(CommonFlightsManager.INSTANCE.getFlightsProvider().isFlightEnabled(MockFlights.DISABLED_FLIGHT));

        // set flights manager
        final MockFlightsProvider flightsProvider = new MockFlightsProvider();
        flightsProvider.addFlight(MockFlights.ENABLED_FLIGHT.getKey(), "true");
        flightsProvider.addFlight(MockFlights.DISABLED_FLIGHT.getKey(), "false");
        MockFlightsManager mockFlightsManager = new MockFlightsManager();
        mockFlightsManager.setMockBrokerFlightsProvider(flightsProvider);
        CommonFlightsManager.INSTANCE.initializeCommonFlightsManager(mockFlightsManager);
        Assert.assertTrue(CommonFlightsManager.INSTANCE.getFlightsProvider().isFlightEnabled(MockFlights.ENABLED_FLIGHT));
        Assert.assertFalse(CommonFlightsManager.INSTANCE.getFlightsProvider().isFlightEnabled(MockFlights.DISABLED_FLIGHT));
    }

    @After
    public void tearDown() {
        CommonFlightsManager.INSTANCE.clearFlightsManager();
    }
}
