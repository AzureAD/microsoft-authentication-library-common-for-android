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

/**
 * Provides the locally coupled flight decision for silent-caller ownership validation.
 */
public final class SilentCallerValidationFlights {

    private SilentCallerValidationFlights() {
    }

    /**
     * Returns whether silent-caller ownership validation is enabled by both required flights.
     *
     * <p>The result is evaluated in the current process; flight state is not device-wide atomic.
     *
     * @return {@code true} only when both silent-caller flights are enabled.
     */
    public static boolean isCompleteSolutionEnabled() {
        final IFlightsProvider flightsProvider = CommonFlightsManager.INSTANCE.getFlightsProvider();
        return flightsProvider.isFlightEnabled(CommonFlight.VALIDATE_SILENT_CALLER)
                && flightsProvider.isFlightEnabled(CommonFlight.ENABLE_SILENT_CALLER_PASSTHROUGH_VALIDATION);
    }
}
