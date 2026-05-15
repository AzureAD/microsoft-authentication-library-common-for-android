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
package com.microsoft.identity.common.java.result;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link AcquireTokenResult} accessors, including the onboarding telemetry blob
 * field used to convey broker-side onboarding telemetry back to the client.
 */
public class AcquireTokenResultTest {

    @Test
    public void onboardingBlob_DefaultsToNull() {
        final AcquireTokenResult result = new AcquireTokenResult();
        Assert.assertNull(result.getOnboardingBlob());
    }

    @Test
    public void onboardingBlob_RoundTripsThroughSetter() {
        final String blobJson = "{\"schema_version\":\"1.0.0\","
                + "\"session_correlation_id\":\"abc-123\","
                + "\"onboarding_mode\":\"brokered\"}";
        final AcquireTokenResult result = new AcquireTokenResult();
        result.setOnboardingBlob(blobJson);
        Assert.assertEquals(blobJson, result.getOnboardingBlob());
    }

    @Test
    public void onboardingBlob_NullSetterClearsValue() {
        final AcquireTokenResult result = new AcquireTokenResult();
        result.setOnboardingBlob("non-null");
        result.setOnboardingBlob(null);
        Assert.assertNull(result.getOnboardingBlob());
    }
}
