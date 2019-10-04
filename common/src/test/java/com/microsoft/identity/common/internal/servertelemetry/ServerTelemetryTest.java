package com.microsoft.identity.common.internal.servertelemetry;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.HashMap;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
public class ServerTelemetryTest {

    @Before
    public void setup() {
        ServerTelemetry.initializeServerTelemetry(ApplicationProvider.getApplicationContext());
    }

    @Test
    public void testEmitSuccessValidFields() {
        ServerTelemetry.emit(Schema.Key.API_ID, "101");
        ServerTelemetry.emit(Schema.Key.FORCE_REFRESH, "false");

        String actualResult = ServerTelemetry.getCurrentTelemetryHeaderString();
        String expectedResult = Schema.Value.SCHEMA_VERSION + "|101,0|,,,,,";

        Assert.assertEquals(expectedResult, actualResult);
    }

    @Test
    public void testEmitAvoidOverwrite() {
        ServerTelemetry.emit(Schema.Key.API_ID, "101");
        ServerTelemetry.emit(Schema.Key.API_ID, "102");
        ServerTelemetry.emit(Schema.Key.API_ID, "103");
        ServerTelemetry.emit(Schema.Key.FORCE_REFRESH, "false");

        String actualResult = ServerTelemetry.getCurrentTelemetryHeaderString();
        String expectedResult = Schema.Value.SCHEMA_VERSION + "|101,0|,,,,,";

        Assert.assertEquals(expectedResult, actualResult);
    }

    @Test
    public void testHeaderStringWithNullTelemObject() {
        String actualResult = ServerTelemetry.getCurrentTelemetryHeaderString();
        String expectedResult = null;

        Assert.assertEquals(expectedResult, actualResult);
    }

    @Test
    public void testHeaderStringWithNoFields() {
        String actualResult = ServerTelemetry.getCurrentTelemetryHeaderString();
        String expectedResult = null;

        Assert.assertEquals(expectedResult, actualResult);
    }

    @Test
    public void testEmitWithInvalidField() {
        ServerTelemetry.emit("invalid-fake-key", "102");

        String actualResult = ServerTelemetry.getCurrentTelemetryHeaderString();
        String expectedResult = Schema.Value.SCHEMA_VERSION + "|,|,,,,,";

        Assert.assertEquals(expectedResult, actualResult);
    }

    @Test
    public void testEmitWithOneValidAndOneInvalidField() {
        ServerTelemetry.emitApiId("101");
        ServerTelemetry.emit("invalid-fake-key", "102");

        String actualResult = ServerTelemetry.getCurrentTelemetryHeaderString();
        String expectedResult = Schema.Value.SCHEMA_VERSION + "|101,|,,,,,";

        Assert.assertEquals(expectedResult, actualResult);
    }

    @Test
    public void testEmptyHeaderStrings() {
        Map<String, String> headerStrings = ServerTelemetry.getTelemetryHeaders();
        Assert.assertEquals(0, headerStrings.size());
    }

    @Test
    public void testEmitEntireMap() {
        Map<String, String> telemetry = new HashMap<>();
        telemetry.put(Schema.Key.API_ID, "101");
        telemetry.put(Schema.Key.FORCE_REFRESH, "0");
        ServerTelemetry.emit(telemetry);

        String actualResult = ServerTelemetry.getCurrentTelemetryHeaderString();
        String expectedResult = Schema.Value.SCHEMA_VERSION + "|101,0|,,,,,";

        Assert.assertEquals(expectedResult, actualResult);
    }

    @Test
    public void testEmitNullMap() {
        Map<String, String> telemetry = null;
        ServerTelemetry.emit(telemetry);

        String actualResult = ServerTelemetry.getCurrentTelemetryHeaderString();
        String expectedResult = null;

        Assert.assertEquals(expectedResult, actualResult);
    }

    //TODO: add more tests

}
