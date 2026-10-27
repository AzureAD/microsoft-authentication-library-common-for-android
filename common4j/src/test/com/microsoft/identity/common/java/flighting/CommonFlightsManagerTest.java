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
        CommonFlightsManager.INSTANCE.resetFlightsManager();
    }

    @Test
    public void testCloudTokenEndpointValidation_defaultsOffAndSupportsOverrides() {
        CommonFlightsManager.INSTANCE.resetFlightsManager();
        Assert.assertEquals("ValidateCloudTokenEndpoint",
                CommonFlight.VALIDATE_CLOUD_TOKEN_ENDPOINT.getKey());
        Assert.assertEquals(Boolean.FALSE,
                CommonFlight.VALIDATE_CLOUD_TOKEN_ENDPOINT.getDefaultValue());
        Assert.assertFalse(CommonFlightsManager.INSTANCE.getFlightsProvider()
                .isFlightEnabled(CommonFlight.VALIDATE_CLOUD_TOKEN_ENDPOINT));

        final MockFlightsProvider provider = new MockFlightsProvider();
        final MockFlightsManager manager = new MockFlightsManager();
        manager.setMockBrokerFlightsProvider(provider);
        CommonFlightsManager.INSTANCE.initializeCommonFlightsManager(manager);

        provider.addFlight(CommonFlight.VALIDATE_CLOUD_TOKEN_ENDPOINT.getKey(), "true");
        Assert.assertTrue(CommonFlightsManager.INSTANCE.getFlightsProvider()
                .isFlightEnabled(CommonFlight.VALIDATE_CLOUD_TOKEN_ENDPOINT));

        provider.addFlight(CommonFlight.VALIDATE_CLOUD_TOKEN_ENDPOINT.getKey(), "false");
        Assert.assertFalse(CommonFlightsManager.INSTANCE.getFlightsProvider()
                .isFlightEnabled(CommonFlight.VALIDATE_CLOUD_TOKEN_ENDPOINT));

        provider.removeFlight(CommonFlight.VALIDATE_CLOUD_TOKEN_ENDPOINT.getKey());
        Assert.assertFalse(CommonFlightsManager.INSTANCE.getFlightsProvider()
                .isFlightEnabled(CommonFlight.VALIDATE_CLOUD_TOKEN_ENDPOINT));
    }

    @Test
    public void testSilentCallerValidationDefaultsOffButCanBeEnabled() {
        CommonFlightsManager.INSTANCE.resetFlightsManager();
        Assert.assertEquals(Boolean.FALSE, CommonFlight.VALIDATE_SILENT_CALLER.getDefaultValue());
        Assert.assertFalse(CommonFlightsManager.INSTANCE.getFlightsProvider()
                .isFlightEnabled(CommonFlight.VALIDATE_SILENT_CALLER));

        final MockFlightsProvider provider = new MockFlightsProvider();
        provider.addFlight(CommonFlight.VALIDATE_SILENT_CALLER.getKey(), "true");
        final MockFlightsManager manager = new MockFlightsManager();
        manager.setMockBrokerFlightsProvider(provider);
        CommonFlightsManager.INSTANCE.initializeCommonFlightsManager(manager);
        Assert.assertTrue(CommonFlightsManager.INSTANCE.getFlightsProvider()
                .isFlightEnabled(CommonFlight.VALIDATE_SILENT_CALLER));
    }

    @Test
    public void testMultipleWindows_defaultsOffWithoutConsumerFlightsManager() {
        CommonFlightsManager.INSTANCE.resetFlightsManager();
        Assert.assertEquals(Boolean.FALSE, CommonFlight.ENABLE_WEBVIEW_MULTIPLE_WINDOWS.getDefaultValue());
        Assert.assertFalse(CommonFlightsManager.INSTANCE.getFlightsProvider()
                .isFlightEnabled(CommonFlight.ENABLE_WEBVIEW_MULTIPLE_WINDOWS));
    }

    @Test
    public void testAuthUxWebMessageListener_defaultsOffWithoutConsumerFlightsManager() {
        CommonFlightsManager.INSTANCE.resetFlightsManager();
        Assert.assertEquals("EnableAuthUxWebMessageListenerTransport",
                CommonFlight.ENABLE_AUTHUX_WEB_MESSAGE_LISTENER_TRANSPORT.getKey());
        Assert.assertEquals(Boolean.FALSE,
                CommonFlight.ENABLE_AUTHUX_WEB_MESSAGE_LISTENER_TRANSPORT.getDefaultValue());
        Assert.assertFalse(CommonFlightsManager.INSTANCE.getFlightsProvider()
                .isFlightEnabled(CommonFlight.ENABLE_AUTHUX_WEB_MESSAGE_LISTENER_TRANSPORT));
    }

    @Test
    public void testAuthUxWebMessageListener_consumerCanEnableDisableAndRemoveOverride() {
        final MockFlightsProvider provider = new MockFlightsProvider();
        final MockFlightsManager manager = new MockFlightsManager();
        manager.setMockBrokerFlightsProvider(provider);
        CommonFlightsManager.INSTANCE.initializeCommonFlightsManager(manager);
        Assert.assertFalse(CommonFlightsManager.INSTANCE.getFlightsProvider()
                .isFlightEnabled(CommonFlight.ENABLE_AUTHUX_WEB_MESSAGE_LISTENER_TRANSPORT));

        provider.addFlight(CommonFlight.ENABLE_AUTHUX_WEB_MESSAGE_LISTENER_TRANSPORT.getKey(), "true");
        Assert.assertTrue(CommonFlightsManager.INSTANCE.getFlightsProvider()
                .isFlightEnabled(CommonFlight.ENABLE_AUTHUX_WEB_MESSAGE_LISTENER_TRANSPORT));

        provider.addFlight(CommonFlight.ENABLE_AUTHUX_WEB_MESSAGE_LISTENER_TRANSPORT.getKey(), "false");
        Assert.assertFalse(CommonFlightsManager.INSTANCE.getFlightsProvider()
                .isFlightEnabled(CommonFlight.ENABLE_AUTHUX_WEB_MESSAGE_LISTENER_TRANSPORT));

        provider.removeFlight(CommonFlight.ENABLE_AUTHUX_WEB_MESSAGE_LISTENER_TRANSPORT.getKey());
        Assert.assertFalse(CommonFlightsManager.INSTANCE.getFlightsProvider()
                .isFlightEnabled(CommonFlight.ENABLE_AUTHUX_WEB_MESSAGE_LISTENER_TRANSPORT));
    }

    @Test
    public void testMultipleWindows_consumerCanExplicitlyEnableAndDisable() {
        final MockFlightsProvider provider = new MockFlightsProvider();
        final MockFlightsManager manager = new MockFlightsManager();
        manager.setMockBrokerFlightsProvider(provider);
        CommonFlightsManager.INSTANCE.initializeCommonFlightsManager(manager);

        provider.addFlight(CommonFlight.ENABLE_WEBVIEW_MULTIPLE_WINDOWS.getKey(), "true");
        Assert.assertTrue(CommonFlightsManager.INSTANCE.getFlightsProvider()
                .isFlightEnabled(CommonFlight.ENABLE_WEBVIEW_MULTIPLE_WINDOWS));

        provider.addFlight(CommonFlight.ENABLE_WEBVIEW_MULTIPLE_WINDOWS.getKey(), "false");
        Assert.assertFalse(CommonFlightsManager.INSTANCE.getFlightsProvider()
                .isFlightEnabled(CommonFlight.ENABLE_WEBVIEW_MULTIPLE_WINDOWS));
    }
}
