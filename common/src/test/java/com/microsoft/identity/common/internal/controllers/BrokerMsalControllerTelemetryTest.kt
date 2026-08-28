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
import com.microsoft.identity.common.java.broker.telemetry.BrokerIpcTelemetry
import com.microsoft.identity.common.java.broker.telemetry.BrokerTelemetryRequest
import com.microsoft.identity.common.java.broker.telemetry.EventCollector
import com.microsoft.identity.common.java.commands.parameters.CommandParameters
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BrokerMsalControllerTelemetryTest {
    /**
     * The blob on the wire must be the negotiated request contract — the correlation ID plus
     * the schema version the broker parses — and nothing else. Asserting the exact field set
     * guards against internal accumulator state leaking across the IPC boundary.
     */
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

        val blob = requestBundle.getString(
            AuthenticationConstants.Broker.BROKER_TELEMETRY_REQUEST
        )
        val json = JSONObject(requireNotNull(blob))
        assertEquals(
            "correlation-id",
            json.getString(BrokerTelemetryRequest.KEY_CORRELATION_ID)
        )
        assertEquals(
            BrokerIpcTelemetry.CURRENT_VERSION,
            json.getString(BrokerTelemetryRequest.KEY_SCHEMA_VERSION)
        )
        assertEquals(2, json.length())
    }

    /**
     * An [EventCollector] may be constructed before the correlation ID is resolved. The blob on
     * the wire must still carry the real ID — a blank value would leave the broker's payload
     * unjoinable to client-side events.
     */
    @Test
    fun addBrokerTelemetryRequest_blankCollectorCorrelationId_adoptsFromParameters() {
        val controller = BrokerMsalController(
            InstrumentationRegistry.getInstrumentation().context,
            MockPlatformComponentsFactory.getNonFunctionalBuilder().build(),
            "broker.package"
        )
        val parameters = CommandParameters.builder()
            .platformComponents(MockPlatformComponentsFactory.getNonFunctionalBuilder().build())
            .correlationId("resolved-correlation-id")
            .build().apply {
                eventCollector = EventCollector("")
            }

        val requestBundle = controller.addBrokerTelemetryRequest(Bundle(), parameters)

        val blob = requestBundle.getString(
            AuthenticationConstants.Broker.BROKER_TELEMETRY_REQUEST
        )
        val json = JSONObject(requireNotNull(blob))
        assertEquals(
            "resolved-correlation-id",
            json.getString(BrokerTelemetryRequest.KEY_CORRELATION_ID)
        )
    }

    /**
     * A null collector must leave the key absent rather than serializing to the literal
     * string "null", so the broker can distinguish "telemetry not requested" from an
     * explicit JSON null.
     */
    @Test
    fun addBrokerTelemetryRequest_nullEventCollector_leavesKeyAbsent() {
        val controller = BrokerMsalController(
            InstrumentationRegistry.getInstrumentation().context,
            MockPlatformComponentsFactory.getNonFunctionalBuilder().build(),
            "broker.package"
        )
        val parameters = CommandParameters.builder()
            .platformComponents(MockPlatformComponentsFactory.getNonFunctionalBuilder().build())
            .build()

        val requestBundle = controller.addBrokerTelemetryRequest(Bundle(), parameters)

        assertFalse(
            requestBundle.containsKey(
                AuthenticationConstants.Broker.BROKER_TELEMETRY_REQUEST
            )
        )
    }
}
