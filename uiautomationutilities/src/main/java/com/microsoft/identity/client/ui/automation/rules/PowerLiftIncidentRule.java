/**
 * // Copyright (c) Microsoft Corporation.
 * // All rights reserved.
 * //
 * // This code is licensed under the MIT License.
 * //
 * // Permission is hereby granted, free of charge, to any person obtaining a copy
 * // of this software and associated documentation files(the "Software"), to deal
 * // in the Software without restriction, including without limitation the rights
 * // to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
 * // copies of the Software, and to permit persons to whom the Software is
 * // furnished to do so, subject to the following conditions :
 * //
 * // The above copyright notice and this permission notice shall be included in
 * // all copies or substantial portions of the Software.
 * //
 * // THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * // IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * // FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * // AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * // LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * // OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * // THE SOFTWARE.
 * */

package com.microsoft.identity.client.ui.automation.rules;

import android.text.TextUtils;

import com.microsoft.identity.client.ui.automation.app.App;
import com.microsoft.identity.client.ui.automation.powerlift.IPowerLiftIntegratedApp;
import com.microsoft.identity.client.ui.automation.logging.Logger;
import com.microsoft.identity.client.ui.automation.powerlift.ThrowableWithPowerLiftIncident;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * A Test Rule to create a PowerLift Incident via a broker if a test fails.
 */
public class PowerLiftIncidentRule implements TestRule {

    private final static String TAG = PowerLiftIncidentRule.class.getSimpleName();

    private IPowerLiftIntegratedApp powerLiftIntegratedApp;

    public PowerLiftIncidentRule(final IPowerLiftIntegratedApp powerLiftIntegratedApp) {
        this.powerLiftIntegratedApp = powerLiftIntegratedApp;
    }

    @Override
    public Statement apply(final Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                Logger.i(TAG, "Applying rule....");
                try {
                    base.evaluate();
                } catch (final Throwable originalThrowable) {
                    String powerLiftIncidentDetails = null;
                    try {
                        Logger.e(
                                TAG,
                                "Encountered error during test....creating PowerLift incident.",
                                originalThrowable
                        );
                        powerLiftIncidentDetails = powerLiftIntegratedApp.createPowerLiftIncident();
                    } catch (final Throwable powerLiftError) {
                        Logger.e(
                                TAG,
                                "Oops...something went wrong...unable to create PowerLift incident.",
                                powerLiftError
                        );
                    }
                    if (TextUtils.isEmpty(powerLiftIncidentDetails)) {
                        throw originalThrowable;
                    } else {
                        assert powerLiftIncidentDetails != null;
                        throw new ThrowableWithPowerLiftIncident(
                                powerLiftIntegratedApp,
                                powerLiftIncidentDetails,
                                originalThrowable
                        );
                    }
                }
            }
        };
    }
}
