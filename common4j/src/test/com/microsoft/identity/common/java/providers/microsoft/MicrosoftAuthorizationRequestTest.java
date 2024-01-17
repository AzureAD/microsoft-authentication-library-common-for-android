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
package com.microsoft.identity.common.java.providers.microsoft;

import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.platform.Device;
import com.microsoft.identity.common.java.platform.MockDeviceMetadata;
import com.microsoft.identity.common.java.providers.oauth2.MockAuthorizationRequest;
import com.microsoft.identity.common.java.ui.PreferredAuthMethod;
import com.microsoft.identity.common.java.util.StringUtil;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.microsoft.identity.common.java.providers.Constants.MOCK_PKCE_CHALLENGE;
import static com.microsoft.identity.common.java.providers.Constants.MOCK_STATE;
import static com.microsoft.identity.common.java.providers.Constants.MOCK_STATE_ENCODED;

@RunWith(JUnit4.class)
public class MicrosoftAuthorizationRequestTest {

    @After
    public void tearDown() {
        Device.clearDeviceMetadata();
    }

    public static final String MOCK_AUTHORITY = "http://mock_authority";
    public static final String MOCK_LIBRARY_VERSION = "1.0.5";
    public static final String MOCK_LIBRARY_NAME = "MOCK_LIBRARY_NAME";
    public static final boolean MOCK_MULTIPLE_CLOUD_AWARE = true;
    public static final UUID MOCK_CORRELATION_ID = UUID.randomUUID();
    public static final String MOCK_LOGIN_HINT = "MOCK_LOGIN_HINT";

    // Check that we're not sending anything unexpected to the server side
    // by comparing the resulted URL by-character.
    @Test
    public void testCreateUriFromAuthorizationRequest() throws MalformedURLException, ClientException {
        Device.setDeviceMetadata(new MockDeviceMetadata());

        final MockMicrosoftAuthorizationRequest request = new MockMicrosoftAuthorizationRequest.Builder()
                .setAuthority(new URL(MOCK_AUTHORITY))
                .setLibraryVersion(MOCK_LIBRARY_VERSION)
                .setLibraryName(MOCK_LIBRARY_NAME)
                .setMultipleCloudAware(MOCK_MULTIPLE_CLOUD_AWARE)
                .setCorrelationId(MOCK_CORRELATION_ID)
                .setLoginHint(MOCK_LOGIN_HINT)
                .setPkceChallenge(MOCK_PKCE_CHALLENGE)
                .setState(MOCK_STATE)
                .build();

        Assert.assertEquals(MockAuthorizationRequest.MOCK_AUTH_ENDPOINT +
                        "?login_hint=" + MOCK_LOGIN_HINT +
                        "&client-request-id=" + MOCK_CORRELATION_ID +
                        "&code_challenge=" + MOCK_PKCE_CHALLENGE.getCodeChallenge() +
                        "&code_challenge_method=" + MOCK_PKCE_CHALLENGE.getCodeChallengeMethod() +
                        "&x-client-Ver=" + MOCK_LIBRARY_VERSION +
                        "&x-client-SKU=" + MOCK_LIBRARY_NAME +
                        "&x-client-OS=" + MockDeviceMetadata.TEST_OS_ESTS +
                        "&x-client-CPU=" + MockDeviceMetadata.TEST_CPU +
                        "&x-client-DM=" + MockDeviceMetadata.TEST_DEVICE_MODEL +
                        "&instance_aware=" + MOCK_MULTIPLE_CLOUD_AWARE +
                // Base class fields start here.
                        "&response_type=code" +
                        "&state=" + MOCK_STATE_ENCODED,
                request.getAuthorizationRequestAsHttpRequest().toString());
    }

    // If state is not provided, MicrosoftAuthorizationRequest should generate a default one.
    @Test
    public void testDefaultStateGenerated(){
        final MockMicrosoftAuthorizationRequest request = new MockMicrosoftAuthorizationRequest.Builder().build();
        Assert.assertFalse(StringUtil.isNullOrEmpty(request.getState()));
    }

    // MicrosoftAuthorizationRequest should always generate values from PkceChallenge.
    @Test
    public void testDefaultPkceChallengeGenerated(){
        final MockMicrosoftAuthorizationRequest request = new MockMicrosoftAuthorizationRequest.Builder().build();
        Assert.assertFalse(StringUtil.isNullOrEmpty(request.getPkceCodeChallenge()));
        Assert.assertFalse(StringUtil.isNullOrEmpty(request.getPkceCodeChallengeMethod()));
    }

    // MicrosoftAuthorizationRequest should always contain Device Metadata.
    @Test
    public void testDeviceMetadataGenerated(){
        Device.setDeviceMetadata(new MockDeviceMetadata());

        final MockMicrosoftAuthorizationRequest request = new MockMicrosoftAuthorizationRequest.Builder().build();
        Assert.assertEquals(MockDeviceMetadata.TEST_OS_ESTS, request.getDiagnosticOS());
        Assert.assertEquals(MockDeviceMetadata.TEST_CPU, request.getDiagnosticCPU());
        Assert.assertEquals(MockDeviceMetadata.TEST_DEVICE_MODEL, request.getDiagnosticDM());
    }

    @Test
    public void testMicrosoftAuthorizationRequestWithPreferredAuthMethod(){
        final MockMicrosoftAuthorizationRequest request = new MockMicrosoftAuthorizationRequest.Builder()
                .setPreferredAuthMethod(PreferredAuthMethod.QR)
                .build();
        Assert.assertEquals(String.valueOf(PreferredAuthMethod.QR.code), request.getPreferredAuthMethodCode());
    }
    
    @Test
    public void testMicrosoftAuthorizationRequestWithNoPreferredAuthMethod(){
        final MockMicrosoftAuthorizationRequest request = new MockMicrosoftAuthorizationRequest.Builder()
                .build();
        Assert.assertNull(request.getPreferredAuthMethodCode());
    }
}
