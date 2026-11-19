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

import com.google.gson.Gson;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@RunWith(JUnit4.class)
public class FailedRequestTest {

    @Test
    public void nullFieldSafety_allEightCombinations_noNpeAndExpectedStrings() {
        final String[] apiIds = new String[]{null, "103"};
        final String[] correlationIds = new String[]{null, "corr-1"};
        final String[] errors = new String[]{null, "invalid_grant"};

        final List<FailedRequest> variants = new ArrayList<>(8);
        final List<String> variantCorrelations = new ArrayList<>(8);

        for (final String apiId : apiIds) {
            for (final String correlationId : correlationIds) {
                for (final String error : errors) {
                    final FailedRequest request = new FailedRequest(apiId, correlationId, error);
                    variants.add(request);
                    variantCorrelations.add(correlationId);

                    final String expectedApiCorr =
                            (apiId == null ? SchemaConstants.Value.EMPTY : apiId)
                                    + SchemaConstants.SEPARATOR_COMMA
                                    + (correlationId == null ? SchemaConstants.Value.EMPTY : correlationId);
                    final String actualApiCorr = request.toApiIdCorrelationString();
                    Assert.assertNotNull(actualApiCorr);
                    Assert.assertEquals(expectedApiCorr, actualApiCorr);
                    Assert.assertFalse(actualApiCorr.contains("null"));

                    final String expectedError = error == null ? SchemaConstants.Value.EMPTY : error;
                    final String actualError = request.toErrorCodeString();
                    Assert.assertNotNull(actualError);
                    Assert.assertEquals(expectedError, actualError);
                    Assert.assertFalse(actualError.contains("null"));

                    final int expectedHash = correlationId == null ? 0 : correlationId.hashCode();
                    Assert.assertEquals(expectedHash, request.hashCode());

                    Assert.assertTrue(request.equals(request));
                    Assert.assertFalse(request.equals(null));
                    Assert.assertFalse(request.equals("not-a-failed-request"));
                }
            }
        }

        Assert.assertEquals(8, variants.size());
        for (int i = 0; i < variants.size(); i++) {
            for (int j = 0; j < variants.size(); j++) {
                final boolean expectedEquals = Objects.equals(
                        variantCorrelations.get(i),
                        variantCorrelations.get(j)
                );
                Assert.assertEquals(expectedEquals, variants.get(i).equals(variants.get(j)));
                if (expectedEquals) {
                    Assert.assertEquals(variants.get(i).hashCode(), variants.get(j).hashCode());
                }
            }
        }
    }

    @Test
    public void equalsAndHashCode_contractWithNullAndNonNullCorrelationId() {
        final FailedRequest nullCorrA = new FailedRequest("103", null, "err-a");
        final FailedRequest nullCorrB = new FailedRequest("204", null, "err-b");
        final FailedRequest nonNullCorrA = new FailedRequest("103", new String("corr-1"), "err-a");
        final FailedRequest nonNullCorrB = new FailedRequest("999", "corr-1", "err-b");
        final FailedRequest distinctCorr = new FailedRequest("103", "corr-2", "err-a");

        // Both correlationIds null: reflexive, symmetric, hashCode == 0
        Assert.assertTrue(nullCorrA.equals(nullCorrA));
        Assert.assertTrue(nullCorrA.equals(nullCorrB));
        Assert.assertTrue(nullCorrB.equals(nullCorrA));
        Assert.assertEquals(0, nullCorrA.hashCode());
        Assert.assertEquals(0, nullCorrB.hashCode());

        // One null and one non-null: false in both directions
        Assert.assertFalse(nullCorrA.equals(nonNullCorrA));
        Assert.assertFalse(nonNullCorrA.equals(nullCorrA));

        // Matching non-null correlationIds: reflexive, symmetric, matching hashCode
        Assert.assertTrue(nonNullCorrA.equals(nonNullCorrA));
        Assert.assertTrue(nonNullCorrA.equals(nonNullCorrB));
        Assert.assertTrue(nonNullCorrB.equals(nonNullCorrA));
        Assert.assertEquals(nonNullCorrA.hashCode(), nonNullCorrB.hashCode());

        // Distinct non-null correlationIds
        Assert.assertFalse(nonNullCorrA.equals(distinctCorr));
        Assert.assertFalse(distinctCorr.equals(nonNullCorrA));
    }

    @Test
    public void delimiterSanitization_pipesAndCommasReplacedWithUnderscore_normalUnchanged() {
        final String normalError = "interaction_required";
        final FailedRequest normal = new FailedRequest(
                "300",
                "48432850-c45c-486c-a50a-a84cdb220c06",
                normalError
        );
        Assert.assertEquals(
                "300,48432850-c45c-486c-a50a-a84cdb220c06",
                normal.toApiIdCorrelationString()
        );
        Assert.assertSame(normalError, normal.toErrorCodeString());

        final FailedRequest emptyFields = new FailedRequest("", "", "");
        Assert.assertEquals(",", emptyFields.toApiIdCorrelationString());
        Assert.assertEquals("", emptyFields.toErrorCodeString());

        final FailedRequest pipeOnlyAndCommaOnly = new FailedRequest("a|b", "c,d", "e,f");
        Assert.assertEquals("a_b,c_d", pipeOnlyAndCommaOnly.toApiIdCorrelationString());
        Assert.assertEquals("e_f", pipeOnlyAndCommaOnly.toErrorCodeString());

        final FailedRequest delimited = new FailedRequest(
                "10|3,4",
                "|corr,,id|",
                "err|or,code||,,"
        );
        final String sanitizedApiCorr = delimited.toApiIdCorrelationString();
        final String sanitizedError = delimited.toErrorCodeString();

        Assert.assertEquals("10_3_4,_corr__id_", sanitizedApiCorr);
        Assert.assertEquals(-1, sanitizedApiCorr.indexOf('|'));
        Assert.assertEquals(2, sanitizedApiCorr.split(",", -1).length);

        Assert.assertEquals("err_or_code____", sanitizedError);
        Assert.assertEquals(-1, sanitizedError.indexOf('|'));
        Assert.assertEquals(-1, sanitizedError.indexOf(','));
    }

    @Test
    public void gsonDeserialization_unsafeAllocatorWithNullAndDelimitedFields_safeAndSanitized() {
        final Gson gson = new Gson();

        final FailedRequest fromEmptyJson = gson.fromJson("{}", FailedRequest.class);
        Assert.assertNotNull(fromEmptyJson);
        Assert.assertEquals(",", fromEmptyJson.toApiIdCorrelationString());
        Assert.assertEquals("", fromEmptyJson.toErrorCodeString());
        Assert.assertEquals(0, fromEmptyJson.hashCode());
        Assert.assertTrue(fromEmptyJson.equals(new FailedRequest(null, null, null)));

        final FailedRequest fromDelimitedJson = gson.fromJson(
                "{\"mApiId\":\"1|2\",\"mCorrelationId\":null,\"mError\":\"a,b\"}",
                FailedRequest.class
        );
        Assert.assertNotNull(fromDelimitedJson);
        Assert.assertEquals("1_2,", fromDelimitedJson.toApiIdCorrelationString());
        Assert.assertEquals("a_b", fromDelimitedJson.toErrorCodeString());
        Assert.assertEquals(0, fromDelimitedJson.hashCode());
        Assert.assertTrue(fromDelimitedJson.equals(new FailedRequest("999", null, "other")));
        Assert.assertFalse(fromDelimitedJson.equals(new FailedRequest("1|2", "corr-1", "a,b")));

        final String serializedJson = gson.toJson(new FailedRequest("103", "corr-1", "invalid_grant"));
        Assert.assertTrue(serializedJson.contains("\"mApiId\":\"103\""));
        Assert.assertTrue(serializedJson.contains("\"mCorrelationId\":\"corr-1\""));
        Assert.assertTrue(serializedJson.contains("\"mError\":\"invalid_grant\""));
    }
}
