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
package com.microsoft.identity.internal.testutils.mocks;

import com.microsoft.identity.common.java.net.HttpResponse;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftTokenErrorResponse;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftTokenResponse;
import com.microsoft.identity.common.java.util.ObjectMapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class MockServerResponse {
    private final static long defaultTokenExpiryInSec = 3599;

    public static HttpResponse getMockTokenSuccessResponse() {
        final MicrosoftTokenResponse mockTokenResponse = new MicrosoftTokenResponse();
        mockTokenResponse.setTokenType("Bearer");
        mockTokenResponse.setScope("User.Read");
        mockTokenResponse.setExpiresIn(defaultTokenExpiryInSec);
        mockTokenResponse.setExtExpiresIn(defaultTokenExpiryInSec);
        mockTokenResponse.setAccessToken("b06d0810-12ff-4a4e-850b-4bda1540d895");
        mockTokenResponse.setRefreshToken("6b80f5b5-d53c-4c46-992d-66c5dcd4cfb1");
        mockTokenResponse.setIdToken(MockTokenCreator.createMockIdToken());
        mockTokenResponse.setClientInfo(MockTokenCreator.createMockRawClientInfo());

        final String mockResponse = ObjectMapper.serializeObjectToJsonString(mockTokenResponse);
        return new HttpResponse(200, mockResponse, new HashMap<>());
    }

    public static HttpResponse getMockTokenSuccessResponse(final String localAccountId, final String tenant, final String issuer, final String rawClientInfo, final String accessToken) {
        final MicrosoftTokenResponse mockTokenResponse = new MicrosoftTokenResponse();
        mockTokenResponse.setTokenType("Bearer");
        mockTokenResponse.setScope("User.Read");
        mockTokenResponse.setExpiresIn(defaultTokenExpiryInSec);
        mockTokenResponse.setExtExpiresIn(defaultTokenExpiryInSec);
        mockTokenResponse.setAccessToken(accessToken);
        mockTokenResponse.setRefreshToken("6b80f5b5-d53c-4c46-992d-66c5dcd4cfb1");
        mockTokenResponse.setIdToken(MockTokenCreator.createMockIdTokenWithObjectIdTenantIdAndIssuer(localAccountId, tenant, issuer));
        mockTokenResponse.setClientInfo(rawClientInfo);

        final String mockResponse = ObjectMapper.serializeObjectToJsonString(mockTokenResponse);
        return new HttpResponse(200, mockResponse, new HashMap<>());
    }

    public static HttpResponse getMockTokenFailureInvalidGrantResponse() {
        final MicrosoftTokenErrorResponse tokenErrorResponse = new MicrosoftTokenErrorResponse();
        tokenErrorResponse.setError("invalid_grant");
        tokenErrorResponse.setErrorDescription("AADSTS70000: Provided grant is invalid or malformed");
        tokenErrorResponse.setErrorCodes(new ArrayList<Long>(Arrays.asList(70000L)));
        tokenErrorResponse.setTimeStamp("2019-10-23 21:05:16Z");
        tokenErrorResponse.setTraceId("8497799a-e9f9-402f-a951-7060b5014600");
        tokenErrorResponse.setCorrelationId("390d7507-c607-4f05-bb8a-51a2a7a6282b");
        tokenErrorResponse.setErrorUri("https://login.microsoftonline.com/error?code=70000");
        tokenErrorResponse.setSubError("");

        final String mockResponse = ObjectMapper.serializeObjectToJsonString(tokenErrorResponse);
        final HttpResponse response = new HttpResponse(
                400,
                mockResponse,
                new HashMap<String, List<String>>()
        );
        return response;
    }

    public static HttpResponse getMockCloudDiscoveryResponse() {
        final String mockResponse = "{\n" +
                "   \"tenant_discovery_endpoint\": \"https://login.microsoftonline.com/common/v2.0/.well-known/openid-configuration\",\n" +
                "   \"api-version\": \"1.1\",\n" +
                "   \"metadata\": [\n" +
                "      {\n" +
                "         \"preferred_network\": \"login.microsoftonline.com\",\n" +
                "         \"preferred_cache\": \"login.windows.net\",\n" +
                "         \"aliases\": [\n" +
                "            \"login.microsoftonline.com\",\n" +
                "            \"login.windows.net\",\n" +
                "            \"login.microsoft.com\",\n" +
                "            \"sts.windows.net\"\n" +
                "         ]\n" +
                "      },\n" +
                "      {\n" +
                "         \"preferred_network\": \"login.partner.microsoftonline.cn\",\n" +
                "         \"preferred_cache\": \"login.partner.microsoftonline.cn\",\n" +
                "         \"aliases\": [\n" +
                "            \"login.partner.microsoftonline.cn\",\n" +
                "            \"login.chinacloudapi.cn\"\n" +
                "         ]\n" +
                "      },\n" +
                "      {\n" +
                "         \"preferred_network\": \"login.microsoftonline.de\",\n" +
                "         \"preferred_cache\": \"login.microsoftonline.de\",\n" +
                "         \"aliases\": [\n" +
                "            \"login.microsoftonline.de\"\n" +
                "         ]\n" +
                "      },\n" +
                "      {\n" +
                "         \"preferred_network\": \"login.microsoftonline.us\",\n" +
                "         \"preferred_cache\": \"login.microsoftonline.us\",\n" +
                "         \"aliases\": [\n" +
                "            \"login.microsoftonline.us\",\n" +
                "            \"login.usgovcloudapi.net\"\n" +
                "         ]\n" +
                "      },\n" +
                "      {\n" +
                "         \"preferred_network\": \"login-us.microsoftonline.com\",\n" +
                "         \"preferred_cache\": \"login-us.microsoftonline.com\",\n" +
                "         \"aliases\": [\n" +
                "            \"login-us.microsoftonline.com\"\n" +
                "         ]\n" +
                "      }\n" +
                "   ]\n" +
                "}";
        return new HttpResponse(200, mockResponse, new HashMap<String, List<String>>());
    }

    public static HttpResponse getMockTokenFailureInvalidScopeResponse() {
        final MicrosoftTokenErrorResponse tokenErrorResponse = new MicrosoftTokenErrorResponse();
        tokenErrorResponse.setError("invalid_scope");
        tokenErrorResponse.setErrorDescription("AADSTS70000: Provided scope is invalid or malformed");
        tokenErrorResponse.setErrorCodes(new ArrayList<Long>(Arrays.asList(70000L)));
        tokenErrorResponse.setTimeStamp("2019-10-23 21:05:16Z");
        tokenErrorResponse.setTraceId("8497799a-e9f9-402f-a951-7060b5014600");
        tokenErrorResponse.setCorrelationId("390d7507-c607-4f05-bb8a-51a2a7a6282b");
        tokenErrorResponse.setErrorUri("https://login.microsoftonline.com/error?code=70000");
        tokenErrorResponse.setSubError("bad_token");

        final String mockResponse = ObjectMapper.serializeObjectToJsonString(tokenErrorResponse);
        final HttpResponse response = new HttpResponse(
                400,
                mockResponse,
                new HashMap<String, List<String>>()
        );
        return response;
    }

    public static HttpResponse getMockTokenFailureServiceUnavailable() {
        final MicrosoftTokenErrorResponse tokenErrorResponse = new MicrosoftTokenErrorResponse();
        tokenErrorResponse.setError("service_unavailable");
        tokenErrorResponse.setErrorDescription("AADSTS70000: Service is unavailable");
        tokenErrorResponse.setErrorCodes(new ArrayList<Long>(Arrays.asList(70000L)));
        tokenErrorResponse.setTimeStamp("2019-10-23 21:05:16Z");
        tokenErrorResponse.setTraceId("8497799a-e9f9-402f-a951-7060b5014600");
        tokenErrorResponse.setCorrelationId("390d7507-c607-4f05-bb8a-51a2a7a6282b");
        tokenErrorResponse.setErrorUri("https://login.microsoftonline.com/error?code=70000");
        tokenErrorResponse.setSubError("bad_token");

        final String mockResponse = ObjectMapper.serializeObjectToJsonString(tokenErrorResponse);
        final HttpResponse response = new HttpResponse(
                503,
                mockResponse,
                new HashMap<String, List<String>>()
        );
        return response;
    }

    public static HttpResponse getMockTokenFailureProtectionPolicyRequiredResponse() {
        final MicrosoftTokenErrorResponse tokenErrorResponse = new MicrosoftTokenErrorResponse();
        tokenErrorResponse.setError("unauthorized_client");
        tokenErrorResponse.setErrorDescription("AADSTS53005: Application needs to enforce Intune protection policies");
        tokenErrorResponse.setErrorCodes(new ArrayList<Long>(Arrays.asList(70000L)));
        tokenErrorResponse.setTimeStamp("2019-10-23 21:05:16Z");
        tokenErrorResponse.setTraceId("8497799a-e9f9-402f-a951-7060b5014600");
        tokenErrorResponse.setCorrelationId("390d7507-c607-4f05-bb8a-51a2a7a6282b");
        tokenErrorResponse.setErrorUri("https://login.microsoftonline.com/error?code=70000");
        tokenErrorResponse.setSubError("protection_policy_required");

        final String mockResponse = ObjectMapper.serializeObjectToJsonString(tokenErrorResponse);
        final HttpResponse response = new HttpResponse(
                400,
                mockResponse,
                new HashMap<String, List<String>>()
        );
        return response;
    }
}
