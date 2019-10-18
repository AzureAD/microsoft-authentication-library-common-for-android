package com.microsoft.identity.common.telemetry;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;

import com.microsoft.identity.common.internal.telemetry.Telemetry;
import com.microsoft.identity.common.internal.telemetry.TelemetryConfiguration;
import com.microsoft.identity.common.internal.telemetry.observers.ITelemetryAggregatedObserver;
import com.microsoft.identity.common.internal.telemetry.observers.ITelemetryDefaultObserver;
import com.microsoft.identity.common.internal.telemetry.observers.ITelemetryObserver;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
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

    private Telemetry setupTelemetry(@NonNull final Context context) {
        TelemetryConfiguration telemetryConfiguration = new TelemetryConfiguration();

        return new Telemetry.Builder()
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
    public void testGetObserversEmpty() {
        Telemetry telemetry = Telemetry.getInstance();
        Assert.assertEquals(0, telemetry.getObservers().size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddNullObserverFailure() {
        Telemetry telemetry = Telemetry.getInstance();
        telemetry.addObserver(null);
        fail();
    }

    @Test
    public void testRemoveNullObserverSuccess() {
        Telemetry telemetry = Telemetry.getInstance();
        telemetry.removeObserver((ITelemetryObserver) null);
        Assert.assertEquals(0, telemetry.getObservers().size());
    }

    @Test
    public void testAddAndRemoveObserverSuccess() {
        Telemetry telemetry = Telemetry.getInstance();

        ITelemetryAggregatedObserver telemetryAggregatedObserver = new ITelemetryAggregatedObserver() {
            @Override
            public void onReceived(Map<String, String> telemetryData) {

            }
        };

        telemetry.addObserver(telemetryAggregatedObserver);

        Assert.assertEquals(1, telemetry.getObservers().size());
        Assert.assertTrue(telemetry.getObservers().contains(telemetryAggregatedObserver));

        telemetry.removeObserver(telemetryAggregatedObserver);
        Assert.assertEquals(0, telemetry.getObservers().size());
        Assert.assertFalse(telemetry.getObservers().contains(telemetryAggregatedObserver));
    }

    @Test
    public void testAddAndRemoveMultipleObserversSuccess() {
        Telemetry telemetry = Telemetry.getInstance();

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

        telemetry.addObserver(telemetryAggregatedObserver);
        telemetry.addObserver(telemetryDefaultObserver);
        telemetry.addObserver(telemetryObserver);

        Assert.assertEquals(3, telemetry.getObservers().size());
        Assert.assertTrue(telemetry.getObservers().contains(telemetryAggregatedObserver));
        Assert.assertTrue(telemetry.getObservers().contains(telemetryDefaultObserver));
        Assert.assertTrue(telemetry.getObservers().contains(telemetryObserver));

        telemetry.removeObserver(telemetryDefaultObserver);
        telemetry.removeObserver(telemetryAggregatedObserver);
        telemetry.removeObserver(telemetryObserver);

        Assert.assertEquals(0, telemetry.getObservers().size());
    }
}
