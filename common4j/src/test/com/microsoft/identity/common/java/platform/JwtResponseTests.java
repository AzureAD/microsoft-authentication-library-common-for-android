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

import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link JweResponse}.
 */
@RunWith(JUnit4.class)
public class JwtResponseTests {

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
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseJweInvalidJweLength() {
        final StringBuilder builder = new StringBuilder();
        final String header = "eyJlbmMiOiJBMjU2R0NNIiwiYWxnIjoiUlNBLU9BRVAifQ";
        builder.append(header).append(".")
                .append("AIMpIcH77YJ_c5hSUxtR-Ja0bSawRHMaoT_hkNBD87vgI2IVjRaJoHDv8NXz72Ryjh1Wrk6jEIUeB197srVMgVw1IiVWL16KORZUfb4tX3ho4W9KN0y8AO9wVfmJuzR-eWsaqHKrW7SQo68nguxZ-HrXwAOCOGK3Abm47rXKsjBjgNa9zeLCpowMVI7ZKAJzxjPGuJ_eqClFTCCfC3BMUOH0TzHc4vFGQyMnOqfHIg1dd48jFZ6ObBNsu1tikaKIYA8M47dYEK9f5NtRTAKUxhoifROK2rdTTODJwTfjZqH_WEbCcL14CpaIgxouYJiFxaSVy0qIxICxOZDXzDRTodQ.")
                .append("9pLxwR4TjvMrPN6l.");
        final String jwe = builder.toString();
        final JweResponse jweResponse = JweResponse.parseJwe(jwe);
    }

    @Test(expected = JSONException.class)
    public void testParseJweJson() {
        final StringBuilder builder = new StringBuilder();
        final String json = "{\"refresh_token\":\"ab.cd.ef.gh\"}";
        final JweResponse jweResponse = JweResponse.parseJwe(json);
    }
}
