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

import com.microsoft.identity.common.internal.net.HttpResponse;

import java.util.HashMap;
import java.util.List;

public class MockServerResponse {

    public static HttpResponse getMockTokenSuccessResponse() {
        final String mockResponse = "{\n" +
                "\t\"token_type\": \"Bearer\",\n" +
                "\t\"scope\": \"User.Read\",\n" +
                "\t\"expires_in\": 3599,\n" +
                "\t\"ext_expires_in\": 3599,\n" +
                "\t\"access_token\": \"b06d0810-12ff-4a4e-850b-4bda1540d895\",\n" +
                "\t\"refresh_token\": \"6b80f5b5-d53c-4c46-992d-66c5dcd4cfb1\",\n" +
                "\t\"id_token\": \"" + MockTokenCreator.createMockIdToken() + "\",\n" +
                "\t\"client_info\": \"" + MockTokenCreator.createMockRawClientInfo() + "\"\n" +
                "}";
        return new HttpResponse(200, mockResponse, new HashMap<String, List<String>>());
    }

    public static HttpResponse getMockTokenFailureInvalidGrantResponse() {
        final String mockResponse = "{\n" +
                "\t\"error\": \"invalid_grant\",\n" +
                "\t\"error_description\": \"AADSTS70000: Provided grant is invalid or malformed\",\n" +
                "\t\"error_codes\": [70000],\n" +
                "\t\"timestamp\": \"2019-10-23 21:05:16Z\",\n" +
                "\t\"trace_id\": \"8497799a-e9f9-402f-a951-7060b5014600\",\n" +
                "\t\"correlation_id\": \"390d7507-c607-4f05-bb8a-51a2a7a6282b\",\n" +
                "\t\"error_uri\": \"https://login.microsoftonline.com/error?code=70000\",\n" +
                "\t\"suberror\": \"\"\n" +
                "}";
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
        final String mockResponse = "{\n" +
                "\t\"error\": \"invalid_scope\",\n" +
                "\t\"error_description\": \"AADSTS70000: Provided scope is invalid or malformed\",\n" +
                "\t\"error_codes\": [70000],\n" +
                "\t\"timestamp\": \"2019-10-23 21:05:16Z\",\n" +
                "\t\"trace_id\": \"8497799a-e9f9-402f-a951-7060b5014600\",\n" +
                "\t\"correlation_id\": \"390d7507-c607-4f05-bb8a-51a2a7a6282b\",\n" +
                "\t\"error_uri\": \"https://login.microsoftonline.com/error?code=70000\",\n" +
                "\t\"suberror\": \"bad_token\"\n" +
                "}";
        final HttpResponse response = new HttpResponse(
                400,
                mockResponse,
                new HashMap<String, List<String>>()
        );
        return response;
    }

    public static HttpResponse getMockTokenFailureServiceUnavailable() {
        final String mockResponse = "{\n" +
                "\t\"error\": \"service_unavailable\",\n" +
                "\t\"error_description\": \"AADSTS70000: Service is unavailable\",\n" +
                "\t\"error_codes\": [70000],\n" +
                "\t\"timestamp\": \"2019-10-23 21:05:16Z\",\n" +
                "\t\"trace_id\": \"8497799a-e9f9-402f-a951-7060b5014600\",\n" +
                "\t\"correlation_id\": \"390d7507-c607-4f05-bb8a-51a2a7a6282b\",\n" +
                "\t\"error_uri\": \"https://login.microsoftonline.com/error?code=70000\",\n" +
                "\t\"suberror\": \"bad_token\"\n" +
                "}";
        final HttpResponse response = new HttpResponse(
                503,
                mockResponse,
                new HashMap<String, List<String>>()
        );
        return response;
    }

    public static HttpResponse getMockTokenFailureProtectionPolicyRequiredResponse() {
        final String mockResponse = "{\n" +
                "\t\"error\": \"unauthorized_client\",\n" +
                "\t\"error_description\": \"AADSTS53005: Application needs to enforce Intune protection policies\",\n" +
                "\t\"error_codes\": [70000],\n" +
                "\t\"timestamp\": \"2019-10-23 21:05:16Z\",\n" +
                "\t\"trace_id\": \"8497799a-e9f9-402f-a951-7060b5014600\",\n" +
                "\t\"correlation_id\": \"390d7507-c607-4f05-bb8a-51a2a7a6282b\",\n" +
                "\t\"error_uri\": \"https://login.microsoftonline.com/error?code=70000\",\n" +
                "\t\"suberror\": \"protection_policy_required\"\n" +
                "}";
        final HttpResponse response = new HttpResponse(
                400,
                mockResponse,
                new HashMap<String, List<String>>()
        );
        return response;
    }
}
