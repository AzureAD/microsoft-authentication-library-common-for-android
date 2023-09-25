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
package com.microsoft.identity.common.java.telemetry;

import com.microsoft.identity.common.java.logging.DiagnosticContext;
import com.microsoft.identity.common.java.logging.RequestContext;
import com.microsoft.identity.common.java.telemetry.events.HttpStartEvent;
import com.microsoft.identity.common.java.telemetry.observers.ITelemetryAggregatedObserver;
import com.microsoft.identity.common.java.telemetry.observers.ITelemetryDefaultObserver;
import com.microsoft.identity.common.java.telemetry.observers.ITelemetryObserver;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.fail;

public class TelemetryTest {

    @Before
    public void setup() {
        new Telemetry.Builder()
                .withTelemetryContext(new MockTelemetryContext())
                .isDebugging(false)
                .defaultConfiguration(new TelemetryConfiguration())
                .build();

        if (DiagnosticContext.INSTANCE.getRequestContext().get(DiagnosticContext.CORRELATION_ID) == null) {
            final RequestContext defaultRequestContext = new RequestContext();
            defaultRequestContext.put(DiagnosticContext.THREAD_ID, String.valueOf(Thread.currentThread().getId()));
            defaultRequestContext.put(DiagnosticContext.CORRELATION_ID, "UNSET");
            DiagnosticContext.INSTANCE.setRequestContext(defaultRequestContext);

        }
    }

    @After
    public void cleanUp() {
        Telemetry.getInstance().removeAllObservers();
    }

    @Test
    public void testTelemetryInstanceCreationSuccess() {
        Telemetry telemetry = Telemetry.getInstance();
        Assert.assertNotNull(telemetry);
    }

    @Test
    public void testGetObserversEmptySuccess() {
        Assert.assertEquals(0, Telemetry.getInstance().getObservers().size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddNullObserverFailure() {
        Telemetry.getInstance().addObserver(null);
        fail();
    }

    @Test
    public void testRemoveNullObserverSuccess() {
        Telemetry.getInstance().removeObserver((ITelemetryObserver) null);
        Assert.assertEquals(0, Telemetry.getInstance().getObservers().size());
    }

    @Test
    public void testAddAndRemoveObserverSuccess() {
        ITelemetryAggregatedObserver telemetryAggregatedObserver = new ITelemetryAggregatedObserver() {
            @Override
            public void onReceived(Map<String, String> telemetryData) {
            }
        };

        Telemetry.getInstance().addObserver(telemetryAggregatedObserver);

        Assert.assertEquals(1, Telemetry.getInstance().getObservers().size());
        Assert.assertTrue(Telemetry.getInstance().getObservers().contains(telemetryAggregatedObserver));

        Telemetry.getInstance().removeObserver(telemetryAggregatedObserver);
        Assert.assertEquals(0, Telemetry.getInstance().getObservers().size());
        Assert.assertFalse(Telemetry.getInstance().getObservers().contains(telemetryAggregatedObserver));
    }

    @Test
    public void testAddAndRemoveMultipleObserversSuccess() {
        ITelemetryAggregatedObserver telemetryAggregatedObserver = new ITelemetryAggregatedObserver() {
            @Override
            public void onReceived(Map<String, String> telemetryData) {
            }
        };

        ITelemetryDefaultObserver telemetryDefaultObserver = new ITelemetryDefaultObserver() {
            @Override
            public void onReceived(List<Map<String, String>> telemetryData) {
            }
        };

        final ITelemetryObserver telemetryObserver = new ITelemetryObserver() {
            @Override
            public void onReceived(Object telemetryData) {
            }
        };

        Telemetry.getInstance().addObserver(telemetryAggregatedObserver);
        Telemetry.getInstance().addObserver(telemetryDefaultObserver);
        Telemetry.getInstance().addObserver(telemetryObserver);

        Assert.assertEquals(3, Telemetry.getInstance().getObservers().size());
        Assert.assertTrue(Telemetry.getInstance().getObservers().contains(telemetryAggregatedObserver));
        Assert.assertTrue(Telemetry.getInstance().getObservers().contains(telemetryDefaultObserver));
        Assert.assertTrue(Telemetry.getInstance().getObservers().contains(telemetryObserver));

        Telemetry.getInstance().removeObserver(telemetryDefaultObserver);
        Telemetry.getInstance().removeObserver(telemetryAggregatedObserver);
        Telemetry.getInstance().removeObserver(telemetryObserver);

        Assert.assertEquals(0, Telemetry.getInstance().getObservers().size());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetObserversReturnsUnmodifiableListSuccess() {
        Telemetry.getInstance().getObservers().add(new ITelemetryObserver() {
            @Override
            public void onReceived(Object telemetryData) {
            }
        });
    }

    @Test
    public void testBasicDeviceInfoPresentInTelemetry() {
        Telemetry.getInstance().addObserver(new ITelemetryDefaultObserver() {
            @Override
            public void onReceived(List<Map<String, String>> telemetryData) {
                final Map<String, String> mapWithDeviceInfo = telemetryData.get(0);
                Assert.assertTrue(mapWithDeviceInfo.containsKey(TelemetryEventStrings.App.NAME));
                Assert.assertTrue(mapWithDeviceInfo.containsKey(TelemetryEventStrings.App.BUILD));
                Assert.assertTrue(mapWithDeviceInfo.containsKey(TelemetryEventStrings.Device.MODEL));
                Assert.assertTrue(mapWithDeviceInfo.containsKey(TelemetryEventStrings.Device.NAME));
                Assert.assertTrue(mapWithDeviceInfo.containsKey(TelemetryEventStrings.Os.NAME));
                Assert.assertTrue(mapWithDeviceInfo.containsKey(TelemetryEventStrings.Os.VERSION));
                Assert.assertTrue(mapWithDeviceInfo.containsKey(TelemetryEventStrings.Device.TIMEZONE));
            }
        });

        Telemetry.getInstance().flush();
    }


    private void testTelemetryMapHelper() {
        List<Map<String,String>> telemetryData = Telemetry.getInstance().getMap();
        Map<String, String> map = telemetryData.get(0);
        Assert.assertTrue(map.containsKey("Microsoft.MSAL.method"));
    }

    @Test
    public void testTelemetryMap() {
        Telemetry.emit(new HttpStartEvent().putMethod("GET"));
        testTelemetryMapHelper();
        testTelemetryMapHelper();
    }

    @Test
    public void testITelemetryAggregatedObserver() {
        Telemetry.getInstance().addObserver(new ITelemetryAggregatedObserver() {
            @Override
            public void onReceived(Map<String, String> telemetryData) {
                final String errorDomain = telemetryData.get(TelemetryEventStrings.Key.HTTP_ERROR_DOMAIN);
                Assert.assertEquals("TESTDOMAIN", errorDomain);
            }
        });

        Telemetry.emit(new HttpStartEvent().putErrorDomain("TESTDOMAIN"));
        Telemetry.getInstance().flush();
    }

    @Test
    public void testITelemetryDefaultObserver() {
        Telemetry.getInstance().addObserver(new ITelemetryDefaultObserver() {
            @Override
            public void onReceived(List<Map<String, String>> telemetryData) {
                final Map<String, String> mapWithExpectedInfo = telemetryData.get(0);
                final String errorDomain = mapWithExpectedInfo.get(TelemetryEventStrings.Key.HTTP_ERROR_DOMAIN);
                final String method = mapWithExpectedInfo.get(TelemetryEventStrings.Key.HTTP_METHOD);
                Assert.assertEquals("TESTDOMAIN", errorDomain);
                Assert.assertEquals("TESTMETHOD", method);
            }
        });

        Telemetry.emit(new HttpStartEvent()
                .putErrorDomain("TESTDOMAIN")
                .putMethod("TESTMETHOD"));
        Telemetry.getInstance().flush();
    }

}
