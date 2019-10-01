package com.microsoft.identity.common.internal.servertelemetry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ServerTelemetryTest {

    @Before
    public void setup() {
        ServerTelemetry.clearCurrentRequestTelemetry();
        ServerTelemetry.clearLastRequestTelemetry();
    }

    @Test
    public void testEmitSuccessValidFields() {
        ServerTelemetry.emit(Schema.Key.API_ID, "101");
        ServerTelemetry.emit(Schema.Key.FORCE_REFRESH, "false");

        String actualResult = ServerTelemetry.getCurrentTelemetryHeaderString();
        String expectedResult = Schema.Value.SCHEMA_VERSION + "|101,0|";

        Assert.assertEquals(expectedResult, actualResult);
    }

    @Test
    public void testHeaderStringWithNullTelemObject() {
        String actualResult = ServerTelemetry.getCurrentTelemetryHeaderString();
        String expectedResult = "";

        Assert.assertEquals(expectedResult, actualResult);
    }

    @Test
    public void testHeaderStringWithNoFields() {
        ServerTelemetry.startScenario();

        String actualResult = ServerTelemetry.getCurrentTelemetryHeaderString();
        String expectedResult = Schema.Value.SCHEMA_VERSION + "|,|";

        Assert.assertEquals(expectedResult, actualResult);
    }

    @Test
    public void testEmitWithInvalidField() {
        ServerTelemetry.startScenario();
        ServerTelemetry.emit("invalid-fake-key", "102");

        String actualResult = ServerTelemetry.getCurrentTelemetryHeaderString();
        String expectedResult = Schema.Value.SCHEMA_VERSION + "|,|";

        Assert.assertEquals(expectedResult, actualResult);
    }

    @Test
    public void testEmitWithOneValidAndOneInvalidField() {
        ServerTelemetry.startScenario();
        ServerTelemetry.emit(Schema.Key.API_ID, "101");
        ServerTelemetry.emit("invalid-fake-key", "102");

        String actualResult = ServerTelemetry.getCurrentTelemetryHeaderString();
        String expectedResult = Schema.Value.SCHEMA_VERSION + "|101,|";

        Assert.assertEquals(expectedResult, actualResult);
    }

    // more tests need to be added

}
