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
package com.microsoft.identity.client.ui.automation.powerlift;

import androidx.annotation.NonNull;

import com.microsoft.identity.client.ui.automation.app.App;

import lombok.Getter;

/**
 * A throwable that contains the incident id details for a PowerLift incident. This throwable should
 * only be used when a PowerLift Incident is created and a PowerLift incident is typically created
 * by an {@link IPowerLiftIntegratedApp}.
 */
@Getter
public class ThrowableWithPowerLiftIncident extends Throwable {

    private final IPowerLiftIntegratedApp mPowerLiftIntegratedApp;
    private final String mIncidentId;
    private final Throwable mOriginalThrowable;

    public ThrowableWithPowerLiftIncident(
            @NonNull final IPowerLiftIntegratedApp powerLiftIntegratedApp,
            @NonNull final String incidentId,
            @NonNull final Throwable throwable) {
        super(
                createMessageWithIncidentId(powerLiftIntegratedApp, incidentId, throwable),
                throwable);
        mPowerLiftIntegratedApp = powerLiftIntegratedApp;
        mIncidentId = incidentId;
        mOriginalThrowable = throwable;
    }

    private static String createMessageWithIncidentId(
            @NonNull final IPowerLiftIntegratedApp powerLiftIntegratedApp,
            @NonNull final String incidentId,
            @NonNull final Throwable throwable) {
        return throwable.getMessage()
                + "\n"
                + "PowerLift Incident Created via "
                + ((App) powerLiftIntegratedApp).getAppName()
                + " - "
                + incidentId.trim();
    }
}
