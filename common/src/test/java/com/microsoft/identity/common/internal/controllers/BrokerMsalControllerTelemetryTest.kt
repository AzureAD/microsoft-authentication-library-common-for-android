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
package com.microsoft.identity.common.internal.controllers

import android.os.Bundle
import androidx.test.platform.app.InstrumentationRegistry
import com.microsoft.identity.common.components.MockPlatformComponentsFactory
import com.microsoft.identity.common.java.AuthenticationConstants
import com.microsoft.identity.common.java.broker.telemetry.EventCollector
import com.microsoft.identity.common.java.commands.parameters.CommandParameters
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BrokerMsalControllerTelemetryTest {
    @Test
    fun addBrokerTelemetryRequest_eventCollector_putsSerializedRequestOnBundle() {
        val controller = BrokerMsalController(
            InstrumentationRegistry.getInstrumentation().context,
            MockPlatformComponentsFactory.getNonFunctionalBuilder().build(),
            "broker.package"
        )
        val parameters = CommandParameters.builder()
            .platformComponents(MockPlatformComponentsFactory.getNonFunctionalBuilder().build())
            .build().apply {
                eventCollector = EventCollector("correlation-id")
            }

        val requestBundle = controller.addBrokerTelemetryRequest(Bundle(), parameters)

        assertTrue(
            requestBundle.containsKey(
                AuthenticationConstants.BrokerContentProvider.BROKER_TELEMETRY_REQUEST
            )
        )
        assertNotNull(
            requestBundle.getString(
                AuthenticationConstants.BrokerContentProvider.BROKER_TELEMETRY_REQUEST
            )
        )
    }
}
