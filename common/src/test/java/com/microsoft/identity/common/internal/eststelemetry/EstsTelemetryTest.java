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
package com.microsoft.identity.common.internal.eststelemetry;

import androidx.test.core.app.ApplicationProvider;

import com.microsoft.identity.common.internal.controllers.CommandDispatcher;
import com.microsoft.identity.common.internal.logging.DiagnosticContext;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.HashMap;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
public class EstsTelemetryTest {

    @Before
    public void setup() {
        EstsTelemetry.getInstance().setupLastRequestTelemetryCache(ApplicationProvider.getApplicationContext());
        CommandDispatcher.initializeDiagnosticContext(null);
    }

    @After
    public void cleanup() {
        EstsTelemetry.getInstance().flush();
    }

    @Test
    public void testEmitSuccessValidFields() {
        EstsTelemetry.getInstance().emit(Schema.Key.API_ID, "101");
        EstsTelemetry.getInstance().emit(Schema.Key.FORCE_REFRESH, "false");

        String actualResult = EstsTelemetry.getInstance().getCurrentTelemetryHeaderString();
        String expectedResult = Schema.CURRENT_SCHEMA_VERSION + "|101,0|,,,,,";

        Assert.assertEquals(expectedResult, actualResult);
    }

    @Test
    public void testEmitAvoidOverwrite() {
        EstsTelemetry.getInstance().emit(Schema.Key.API_ID, "101");
        EstsTelemetry.getInstance().emit(Schema.Key.API_ID, "102");
        EstsTelemetry.getInstance().emit(Schema.Key.API_ID, "103");
        EstsTelemetry.getInstance().emit(Schema.Key.FORCE_REFRESH, "false");

        String actualResult = EstsTelemetry.getInstance().getCurrentTelemetryHeaderString();
        String expectedResult = Schema.CURRENT_SCHEMA_VERSION + "|101,0|,,,,,";

        Assert.assertEquals(expectedResult, actualResult);
    }

    @Test
    public void testHeaderStringWithNullTelemObject() {
        String actualResult = EstsTelemetry.getInstance().getCurrentTelemetryHeaderString();
        String expectedResult = null;

        Assert.assertEquals(expectedResult, actualResult);
    }

    @Test
    public void testHeaderStringWithNoFields() {
        String actualResult = EstsTelemetry.getInstance().getCurrentTelemetryHeaderString();
        String expectedResult = null;

        Assert.assertEquals(expectedResult, actualResult);
    }

    @Test
    public void testEmitWithInvalidField() {
        EstsTelemetry.getInstance().emit("invalid-fake-key", "102");

        String actualResult = EstsTelemetry.getInstance().getCurrentTelemetryHeaderString();
        String expectedResult = Schema.CURRENT_SCHEMA_VERSION + "|,|,,,,,";

        Assert.assertEquals(expectedResult, actualResult);
    }

    @Test
    public void testEmitWithOneValidAndOneInvalidField() {
        EstsTelemetry.getInstance().emitApiId("101");
        EstsTelemetry.getInstance().emit("invalid-fake-key", "102");

        String actualResult = EstsTelemetry.getInstance().getCurrentTelemetryHeaderString();
        String expectedResult = Schema.CURRENT_SCHEMA_VERSION + "|101,|,,,,,";

        Assert.assertEquals(expectedResult, actualResult);
    }

    @Test
    public void testEmitEntireMap() {
        Map<String, String> telemetry = new HashMap<>();
        telemetry.put(Schema.Key.API_ID, "101");
        telemetry.put(Schema.Key.FORCE_REFRESH, "0");
        EstsTelemetry.getInstance().emit(telemetry);

        String actualResult = EstsTelemetry.getInstance().getCurrentTelemetryHeaderString();
        String expectedResult = Schema.CURRENT_SCHEMA_VERSION + "|101,0|,,,,,";

        Assert.assertEquals(expectedResult, actualResult);
    }

    @Test
    public void testEmitNullMap() {
        Map<String, String> telemetry = null;
        EstsTelemetry.getInstance().emit(telemetry);

        String actualResult = EstsTelemetry.getInstance().getCurrentTelemetryHeaderString();
        String expectedResult = null;

        Assert.assertEquals(expectedResult, actualResult);
    }

    @Test
    public void testLastTelemetryHeaderString() {
        EstsTelemetry.getInstance().emit(Schema.Key.API_ID, "101");

        EstsTelemetry.getInstance().flush();

        String actualResult = EstsTelemetry.getInstance().getLastTelemetryHeaderString();

        final String correlationId = DiagnosticContext.getRequestContext().get(DiagnosticContext.CORRELATION_ID);

        String expectedResult = Schema.CURRENT_SCHEMA_VERSION + "|101," + correlationId + ",|";

        Assert.assertEquals(expectedResult, actualResult);
    }

    //TODO: add more tests

}
