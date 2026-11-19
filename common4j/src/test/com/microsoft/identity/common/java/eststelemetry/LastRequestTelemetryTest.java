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
package com.microsoft.identity.common.java.eststelemetry;

import com.microsoft.identity.common.java.util.ported.InMemoryStorage;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

@RunWith(JUnit4.class)
public class LastRequestTelemetryTest {

    private static Field getFailedRequestsField() throws NoSuchFieldException {
        final Field field = LastRequestTelemetry.class.getDeclaredField("failedRequests");
        field.setAccessible(true);
        return field;
    }

    @Test
    public void appendFailedRequest_over2500SequentialCalls_flatArrayListFifoAndNoStackOverflow()
            throws Exception {
        final LastRequestTelemetry telemetry =
                new LastRequestTelemetry(SchemaConstants.CURRENT_SCHEMA_VERSION);
        final Field failedRequestsField = getFailedRequestsField();

        for (int i = 0; i < 2500; i++) {
            telemetry.appendFailedRequest("103", "corr-" + i, "err-" + i);
            if (i == 100 || i == 1000 || i == 2499) {
                final Object rawList = failedRequestsField.get(telemetry);
                Assert.assertNotNull(rawList);
                Assert.assertSame(ArrayList.class, rawList.getClass());
            }
        }

        final List<FailedRequest> retained = telemetry.getFailedRequests();
        Assert.assertEquals(LastRequestTelemetry.FAILED_REQUEST_CAP, retained.size());
        for (int idx = 0; idx < LastRequestTelemetry.FAILED_REQUEST_CAP; idx++) {
            final int expectedSeq = 2400 + idx;
            Assert.assertEquals(
                    "103,corr-" + expectedSeq,
                    retained.get(idx).toApiIdCorrelationString()
            );
            Assert.assertEquals("err-" + expectedSeq, retained.get(idx).toErrorCodeString());
        }

        try {
            retained.add(new FailedRequest("103", "corr-unmodifiable", "err"));
            Assert.fail("Expected UnsupportedOperationException on unmodifiable failedRequests list");
        } catch (final UnsupportedOperationException expected) {
            // Expected
        }

        final InMemoryStorage<String> storage = new InMemoryStorage<>();
        final LastRequestTelemetryCache cache = new LastRequestTelemetryCache(storage);
        cache.saveRequestTelemetryToCache(telemetry);

        final LastRequestTelemetry loaded = cache.getRequestTelemetryFromCache();
        Assert.assertNotNull(loaded);
        Assert.assertEquals(LastRequestTelemetry.FAILED_REQUEST_CAP, loaded.getFailedRequests().size());
        Assert.assertEquals(
                telemetry.getCompleteHeaderString(),
                loaded.getCompleteHeaderString()
        );

        loaded.appendFailedRequest("103", "corr-2500", "err-2500");
        Assert.assertSame(ArrayList.class, failedRequestsField.get(loaded).getClass());
        Assert.assertEquals(LastRequestTelemetry.FAILED_REQUEST_CAP, loaded.getFailedRequests().size());
        Assert.assertEquals(
                "103,corr-2401",
                loaded.getFailedRequests().get(0).toApiIdCorrelationString()
        );
        Assert.assertEquals(
                "103,corr-2500",
                loaded.getFailedRequests().get(99).toApiIdCorrelationString()
        );
    }

    @Test
    public void gsonDeserialization_nullMissingOversizedNullElementsAndNonArrayList()
            throws Exception {
        final Field failedRequestsField = getFailedRequestsField();

        // (a) Omitted and explicit null failed_requests in persisted JSON
        final String[] missingOrNullPayloads = new String[]{
                "{\"schema_version\":\"2\",\"silent_successful_count\":5,\"platform_telemetry\":{}}",
                "{\"schema_version\":\"2\",\"silent_successful_count\":5,\"failed_requests\":null,\"platform_telemetry\":{}}"
        };
        for (final String jsonPayload : missingOrNullPayloads) {
            final InMemoryStorage<String> storage = new InMemoryStorage<>();
            storage.put(LastRequestTelemetryCache.LAST_TELEMETRY_OBJECT_CACHE_KEY, jsonPayload);
            final LastRequestTelemetryCache cache = new LastRequestTelemetryCache(storage);

            final LastRequestTelemetry loaded = cache.getRequestTelemetryFromCache();
            Assert.assertNotNull(loaded);
            final List<FailedRequest> emptyView = loaded.getFailedRequests();
            Assert.assertTrue(emptyView.isEmpty());
            try {
                emptyView.add(new FailedRequest("103", "c1", "e1"));
                Assert.fail("Expected UnsupportedOperationException on empty failedRequests view");
            } catch (final UnsupportedOperationException expected) {
                // Expected
            }

            Assert.assertEquals("5||", loaded.getHeaderStringForFields());
            Assert.assertEquals("2|5|||2,", loaded.getCompleteHeaderString());

            loaded.wipeFailedRequestAndErrorForSubList(
                    Collections.singletonList(new FailedRequest("103", "c1", "e1"))
            );
            loaded.wipeFailedRequestAndErrorForSubList(null);

            loaded.appendFailedRequest("103", "c1", "e1");
            Assert.assertSame(ArrayList.class, failedRequestsField.get(loaded).getClass());
            Assert.assertEquals(1, loaded.getFailedRequests().size());
        }

        // (b) Oversized legacy JSON with 150 entries (0..149) trimmed to 100 (51..150) on append
        final StringBuilder oversizedJson = new StringBuilder();
        oversizedJson.append("{\"schema_version\":\"2\",\"silent_successful_count\":0,\"platform_telemetry\":{},\"failed_requests\":[");
        for (int i = 0; i < 150; i++) {
            if (i > 0) {
                oversizedJson.append(',');
            }
            oversizedJson.append("{\"mApiId\":\"103\",\"mCorrelationId\":\"corr-")
                    .append(i)
                    .append("\",\"mError\":\"err-")
                    .append(i)
                    .append("\"}");
        }
        oversizedJson.append("]}");

        final InMemoryStorage<String> oversizedStorage = new InMemoryStorage<>();
        oversizedStorage.put(
                LastRequestTelemetryCache.LAST_TELEMETRY_OBJECT_CACHE_KEY,
                oversizedJson.toString()
        );
        final LastRequestTelemetry oversizedLoaded =
                new LastRequestTelemetryCache(oversizedStorage).getRequestTelemetryFromCache();
        Assert.assertNotNull(oversizedLoaded);
        Assert.assertEquals(150, oversizedLoaded.getFailedRequests().size());

        oversizedLoaded.appendFailedRequest("103", "corr-150", "err-150");
        Assert.assertSame(ArrayList.class, failedRequestsField.get(oversizedLoaded).getClass());
        Assert.assertEquals(
                LastRequestTelemetry.FAILED_REQUEST_CAP,
                oversizedLoaded.getFailedRequests().size()
        );
        for (int idx = 0; idx < LastRequestTelemetry.FAILED_REQUEST_CAP; idx++) {
            final int expectedSeq = 51 + idx;
            Assert.assertEquals(
                    "103,corr-" + expectedSeq,
                    oversizedLoaded.getFailedRequests().get(idx).toApiIdCorrelationString()
            );
            Assert.assertEquals(
                    "err-" + expectedSeq,
                    oversizedLoaded.getFailedRequests().get(idx).toErrorCodeString()
            );
        }

        // (c) Null elements inside failed_requests skipped cleanly with zero dangling commas
        final InMemoryStorage<String> nullElemStorage = new InMemoryStorage<>();
        nullElemStorage.put(
                LastRequestTelemetryCache.LAST_TELEMETRY_OBJECT_CACHE_KEY,
                "{\"schema_version\":\"2\",\"silent_successful_count\":1,\"platform_telemetry\":{},"
                        + "\"failed_requests\":[null,{\"mApiId\":\"103\",\"mCorrelationId\":\"c1\",\"mError\":\"e1\"},null]}"
        );
        final LastRequestTelemetry singleValidLoaded =
                new LastRequestTelemetryCache(nullElemStorage).getRequestTelemetryFromCache();
        Assert.assertNotNull(singleValidLoaded);
        Assert.assertEquals("1|103,c1|e1", singleValidLoaded.getHeaderStringForFields());
        Assert.assertEquals("2|1|103,c1|e1|2,", singleValidLoaded.getCompleteHeaderString());

        nullElemStorage.put(
                LastRequestTelemetryCache.LAST_TELEMETRY_OBJECT_CACHE_KEY,
                "{\"schema_version\":\"2\",\"silent_successful_count\":1,\"platform_telemetry\":{},"
                        + "\"failed_requests\":[null,{\"mApiId\":\"103\",\"mCorrelationId\":\"c1\",\"mError\":\"e1\"},"
                        + "null,{\"mApiId\":\"104\",\"mCorrelationId\":\"c2\",\"mError\":\"e2\"},null]}"
        );
        final LastRequestTelemetry twoValidLoaded =
                new LastRequestTelemetryCache(nullElemStorage).getRequestTelemetryFromCache();
        Assert.assertNotNull(twoValidLoaded);
        Assert.assertEquals("1|103,c1,104,c2|e1,e2", twoValidLoaded.getHeaderStringForFields());

        nullElemStorage.put(
                LastRequestTelemetryCache.LAST_TELEMETRY_OBJECT_CACHE_KEY,
                "{\"schema_version\":\"2\",\"silent_successful_count\":1,\"platform_telemetry\":{},"
                        + "\"failed_requests\":[null,null]}"
        );
        final LastRequestTelemetry allNullLoaded =
                new LastRequestTelemetryCache(nullElemStorage).getRequestTelemetryFromCache();
        Assert.assertNotNull(allNullLoaded);
        Assert.assertEquals("1||", allNullLoaded.getHeaderStringForFields());

        // (d) Non-ArrayList List instances (Arrays.asList and LinkedList) normalized to ArrayList
        final FailedRequest e1 = new FailedRequest("101", "c1", "e1");
        final FailedRequest e2 = new FailedRequest("102", "c2", "e2");
        final FailedRequest e3 = new FailedRequest("103", "c3", "e3");

        final LastRequestTelemetry fixedSizeListTelemetry =
                new LastRequestTelemetry(SchemaConstants.CURRENT_SCHEMA_VERSION);
        failedRequestsField.set(fixedSizeListTelemetry, Arrays.asList(e1, e2));
        fixedSizeListTelemetry.wipeFailedRequestAndErrorForSubList(Collections.singletonList(e1));
        Assert.assertSame(ArrayList.class, failedRequestsField.get(fixedSizeListTelemetry).getClass());
        Assert.assertEquals(1, fixedSizeListTelemetry.getFailedRequests().size());
        Assert.assertEquals("102,c2", fixedSizeListTelemetry.getFailedRequests().get(0).toApiIdCorrelationString());

        failedRequestsField.set(fixedSizeListTelemetry, Arrays.asList(e1));
        fixedSizeListTelemetry.appendFailedRequest(e2);
        Assert.assertSame(ArrayList.class, failedRequestsField.get(fixedSizeListTelemetry).getClass());
        Assert.assertEquals(2, fixedSizeListTelemetry.getFailedRequests().size());

        final LastRequestTelemetry linkedListTelemetry =
                new LastRequestTelemetry(SchemaConstants.CURRENT_SCHEMA_VERSION);
        failedRequestsField.set(linkedListTelemetry, new LinkedList<>(Arrays.asList(e1, e2)));
        linkedListTelemetry.appendFailedRequest(e3);
        Assert.assertSame(ArrayList.class, failedRequestsField.get(linkedListTelemetry).getClass());
        Assert.assertEquals(3, linkedListTelemetry.getFailedRequests().size());
    }

    @Test
    public void appendAndWipe_nullFailedRequestAndNullCorrelationId_safe() {
        final LastRequestTelemetry telemetry =
                new LastRequestTelemetry(SchemaConstants.CURRENT_SCHEMA_VERSION);

        telemetry.appendFailedRequest((FailedRequest) null);
        Assert.assertTrue(telemetry.getFailedRequests().isEmpty());

        telemetry.appendFailedRequest(null, null, null);
        Assert.assertEquals(1, telemetry.getFailedRequests().size());
        Assert.assertEquals("0|,|", telemetry.getHeaderStringForFields());

        telemetry.wipeFailedRequestAndErrorForSubList(null);
        Assert.assertEquals(1, telemetry.getFailedRequests().size());

        telemetry.wipeFailedRequestAndErrorForSubList(
                Collections.singletonList(new FailedRequest(null, null, null))
        );
        Assert.assertTrue(telemetry.getFailedRequests().isEmpty());
        Assert.assertEquals("0||", telemetry.getHeaderStringForFields());
    }

    @Test
    public void headerSerialization_delimiterSanitizationAndNormalWireIdentity() {
        final LastRequestTelemetry normalTelemetry =
                new LastRequestTelemetry(SchemaConstants.CURRENT_SCHEMA_VERSION);
        normalTelemetry.appendFailedRequest(
                "300",
                "48432850-c45c-486c-a50a-a84cdb220c06",
                "interaction_required"
        );
        normalTelemetry.appendFailedRequest(
                "82",
                "a4e7dfa8-5883-426a-96b5-81a325d0abd5",
                "user_cancelled"
        );

        Assert.assertEquals(
                "0|300,48432850-c45c-486c-a50a-a84cdb220c06,82,a4e7dfa8-5883-426a-96b5-81a325d0abd5|interaction_required,user_cancelled",
                normalTelemetry.getHeaderStringForFields()
        );
        Assert.assertEquals(
                "2|0|300,48432850-c45c-486c-a50a-a84cdb220c06,82,a4e7dfa8-5883-426a-96b5-81a325d0abd5|interaction_required,user_cancelled|2,",
                normalTelemetry.getCompleteHeaderString()
        );

        final LastRequestTelemetry delimitedTelemetry =
                new LastRequestTelemetry(SchemaConstants.CURRENT_SCHEMA_VERSION);
        delimitedTelemetry.incrementSilentSuccessCount();
        delimitedTelemetry.appendFailedRequest("10|3", "corr,1|a", "err|1,a");
        delimitedTelemetry.appendFailedRequest(",204|", "|corr,2,", ",err||2,");
        delimitedTelemetry.appendFailedRequest(null, "corr|3,b", null);

        final int k = 3;
        final String fieldsHeader = delimitedTelemetry.getHeaderStringForFields();
        final String[] innerSegments = fieldsHeader.split("\\|", -1);
        Assert.assertEquals(3, innerSegments.length);
        Assert.assertEquals("1", innerSegments[0]);
        Assert.assertEquals(2 * k, innerSegments[1].split(",", -1).length);
        Assert.assertEquals(k, innerSegments[2].split(",", -1).length);
        Assert.assertEquals(
                "1|10_3,corr_1_a,_204_,_corr_2_,,corr_3_b|err_1_a,_err__2_,",
                fieldsHeader
        );

        final String completeHeader = delimitedTelemetry.getCompleteHeaderString();
        Assert.assertEquals(5, completeHeader.split("\\|", -1).length);
        Assert.assertEquals(
                "2|1|10_3,corr_1_a,_204_,_corr_2_,,corr_3_b|err_1_a,_err__2_,|2,",
                completeHeader
        );
    }
}
