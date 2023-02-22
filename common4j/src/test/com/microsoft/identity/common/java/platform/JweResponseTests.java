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
package com.microsoft.identity.common.java.platform;

import com.microsoft.identity.common.java.util.Supplier;

import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import lombok.NonNull;

/**
 * Tests for {@link JweResponse}.
 */
@RunWith(JUnit4.class)
public class JweResponseTests {

    @Test
    public void testParseJwe() {
        final StringBuilder builder = new StringBuilder();
        final String header = "eyJlbmMiOiJBMjU2R0NNIiwiYWxnIjoiUlNBLU9BRVAifQ";
        builder.append(header).append(".")
                .append("AIMpIcH77YJ_c5hSUxtR-Ja0bSawRHMaoT_hkNBD87vgI2IVjRaJoHDv8NXz72Ryjh1Wrk6jEIUeB197srVMgVw1IiVWL16KORZUfb4tX3ho4W9KN0y8AO9wVfmJuzR-eWsaqHKrW7SQo68nguxZ-HrXwAOCOGK3Abm47rXKsjBjgNa9zeLCpowMVI7ZKAJzxjPGuJ_eqClFTCCfC3BMUOH0TzHc4vFGQyMnOqfHIg1dd48jFZ6ObBNsu1tikaKIYA8M47dYEK9f5NtRTAKUxhoifROK2rdTTODJwTfjZqH_WEbCcL14CpaIgxouYJiFxaSVy0qIxICxOZDXzDRTodQ.")
                .append("9pLxwR4TjvMrPN6l.")
                .append("JQ.")
                .append("X-7yuKygZuh53C2MYTP8xg");
        final String jwe = builder.toString();
        final JweResponse jweResponse = JweResponse.parseJwe(jwe);
        Assert.assertNotNull(jweResponse);
        Assert.assertNotNull(jweResponse.getJweHeader());
        Assert.assertNotNull(jweResponse.getEncryptedKey());
        Assert.assertNotNull(jweResponse.getIv());
        Assert.assertNotNull(jweResponse.getPayload());
        Assert.assertNotNull(jweResponse.getAAD());
        Assert.assertNotNull(jweResponse.getAuthenticationTag());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseJweInvalidJweLength() {
        final String header = "eyJlbmMiOiJBMjU2R0NNIiwiYWxnIjoiUlNBLU9BRVAifQ";
        final String jwe = header + "." +
                "AIMpIcH77YJ_c5hSUxtR-Ja0bSawRHMaoT_hkNBD87vgI2IVjRaJoHDv8NXz72Ryjh1Wrk6jEIUeB197srVMgVw1IiVWL16KORZUfb4tX3ho4W9KN0y8AO9wVfmJuzR-eWsaqHKrW7SQo68nguxZ-HrXwAOCOGK3Abm47rXKsjBjgNa9zeLCpowMVI7ZKAJzxjPGuJ_eqClFTCCfC3BMUOH0TzHc4vFGQyMnOqfHIg1dd48jFZ6ObBNsu1tikaKIYA8M47dYEK9f5NtRTAKUxhoifROK2rdTTODJwTfjZqH_WEbCcL14CpaIgxouYJiFxaSVy0qIxICxOZDXzDRTodQ." +
                "9pLxwR4TjvMrPN6l.";
        final JweResponse jweResponse = JweResponse.parseJwe(jwe);
    }

    @Test(expected = JSONException.class)
    public void testParseJweJson() {
        final String json = "{\"refresh_token\":\"ab.cd.ef.gh\"}";
        final JweResponse jweResponse = JweResponse.parseJwe(json);
    }

    @Test
    public void testParseJweJsonNoTag() {
        final StringBuilder builder = new StringBuilder();
        final String header = "eyJlbmMiOiJBMjU2R0NNIiwiYWxnIjoiUlNBLU9BRVAifQ";
        builder.append(header).append(".")
                .append("AIMpIcH77YJ_c5hSUxtR-Ja0bSawRHMaoT_hkNBD87vgI2IVjRaJoHDv8NXz72Ryjh1Wrk6jEIUeB197srVMgVw1IiVWL16KORZUfb4tX3ho4W9KN0y8AO9wVfmJuzR-eWsaqHKrW7SQo68nguxZ-HrXwAOCOGK3Abm47rXKsjBjgNa9zeLCpowMVI7ZKAJzxjPGuJ_eqClFTCCfC3BMUOH0TzHc4vFGQyMnOqfHIg1dd48jFZ6ObBNsu1tikaKIYA8M47dYEK9f5NtRTAKUxhoifROK2rdTTODJwTfjZqH_WEbCcL14CpaIgxouYJiFxaSVy0qIxICxOZDXzDRTodQ.")
                .append("9pLxwR4TjvMrPN6l.")
                .append("JQ.");
        final String jwe = builder.toString();
        final JweResponse jweResponse = JweResponse.parseJwe(jwe);
        Assert.assertNotNull(jweResponse);
        Assert.assertNotNull(jweResponse.getJweHeader());
        Assert.assertNotNull(jweResponse.getEncryptedKey());
        Assert.assertNotNull(jweResponse.getIv());
        Assert.assertNotNull(jweResponse.getPayload());
        Assert.assertNull(jweResponse.getAuthenticationTag());
    }

    @Test
    public void testParseJweMalformedHeader() {
        final StringBuilder builder = new StringBuilder();
        final String header = "eyJlb";
        builder.append(header).append(".")
                .append("AIMpIcH77YJ_c5hSUxtR-Ja0bSawRHMaoT_hkNBD87vgI2IVjRaJoHDv8NXz72Ryjh1Wrk6jEIUeB197srVMgVw1IiVWL16KORZUfb4tX3ho4W9KN0y8AO9wVfmJuzR-eWsaqHKrW7SQo68nguxZ-HrXwAOCOGK3Abm47rXKsjBjgNa9zeLCpowMVI7ZKAJzxjPGuJ_eqClFTCCfC3BMUOH0TzHc4vFGQyMnOqfHIg1dd48jFZ6ObBNsu1tikaKIYA8M47dYEK9f5NtRTAKUxhoifROK2rdTTODJwTfjZqH_WEbCcL14CpaIgxouYJiFxaSVy0qIxICxOZDXzDRTodQ.")
                .append("9pLxwR4TjvMrPN6l.")
                .append("JQ.")
                .append("X-7yuKygZuh53C2MYTP8xg");
        final String jwe = builder.toString();
        try {
            final JweResponse jweResponse = JweResponse.parseJwe(jwe);
            Assert.fail("parseJwe unexpectedly passed.");
        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testParseJweMalformedValues() {
        final StringBuilder builder = new StringBuilder();
        final String header = "eyJlbmMiOiJBMjU2R0NNIiwiYWxnIjoiUlNBLU9BRVAifQ";
        builder.append(header).append(".")
                .append("AIMpIcH77.")
                .append("9pLxw.")
                .append("J.")
                .append("X-7y=");
        final String jwe = builder.toString();
        final JweResponse jweResponse = JweResponse.parseJwe(jwe);
        Assert.assertNotNull(jweResponse);
        Assert.assertNotNull(jweResponse.getJweHeader());
        testMalformedValue(jweResponse::getEncryptedKey, "getEncryptedKey");
        testMalformedValue(jweResponse::getIv, "getIv");
        testMalformedValue(jweResponse::getPayload, "getPayload");
        testMalformedValue(jweResponse::getAuthenticationTag, "getAuthenticationTag");
    }

    private void testMalformedValue(@NonNull final Supplier<byte[]> getter, @NonNull final String methodName) {
        try {
            getter.get();
            Assert.fail(methodName + " unexpectedly passed.");
        } catch (final IllegalArgumentException ignored) {
        }
    }
}
