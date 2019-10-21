package com.microsoft.identity.common.internal.telemetry;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;

import com.microsoft.identity.common.internal.telemetry.events.ApiStartEvent;
import com.microsoft.identity.common.internal.telemetry.observers.ITelemetryAggregatedObserver;
import com.microsoft.identity.common.internal.telemetry.observers.ITelemetryDefaultObserver;
import com.microsoft.identity.common.internal.telemetry.observers.ITelemetryObserver;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.fail;

@RunWith(RobolectricTestRunner.class)
public class TelemetryTest {

    @Before
    public void setup() {
        final Context context = ApplicationProvider.getApplicationContext();
        setupTelemetry(context);
    }

    @After
    public void cleanUp() {
        Telemetry.getInstance().removeAllObservers();
    }

    private void setupTelemetry(@NonNull final Context context) {
        TelemetryConfiguration telemetryConfiguration = new TelemetryConfiguration();

        new Telemetry.Builder()
                .withContext(context)
                .defaultConfiguration(telemetryConfiguration)
                .build();
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

        ITelemetryObserver telemetryObserver = new ITelemetryObserver() {
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

    @Test
    public void testITelemetryAggregatedObserver() {
        Telemetry.getInstance().addObserver(new ITelemetryAggregatedObserver() {
            @Override
            public void onReceived(Map<String, String> telemetryData) {
                final String apiId = telemetryData.get(TelemetryEventStrings.Key.API_ID);
                Assert.assertEquals("100", apiId);
            }
        });

        Telemetry.emit(new ApiStartEvent().putApiId("100"));
        Telemetry.getInstance().flush();
    }

    @Test
    public void testITelemetryDefaultObserver() {
        Telemetry.getInstance().addObserver(new ITelemetryDefaultObserver() {
            @Override
            public void onReceived(List<Map<String, String>> telemetryData) {
                final Map<String, String> mapWithExpectedInfo = telemetryData.get(0);
                final String apiId = mapWithExpectedInfo.get(TelemetryEventStrings.Key.API_ID);
                final String authorityType = mapWithExpectedInfo.get(TelemetryEventStrings.Key.AUTHORITY_TYPE);
                Assert.assertEquals("100", apiId);
                Assert.assertEquals("AAD", authorityType);
            }
        });

        Telemetry.emit(new ApiStartEvent()
                .putApiId("100")
                .putAuthorityType("AAD"));
        Telemetry.getInstance().flush();
    }

}
