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
package com.microsoft.identity.common.java.providers;

import com.microsoft.identity.common.java.providers.oauth2.PkceChallenge;
import com.microsoft.identity.common.java.util.StringUtil;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Constants {

    public static final String MOCK_CLIENT_ID = "MOCK_CLIENT_ID";
    public static final String MOCK_CLAIMS = "{\"access_token\":{\"xms_cc\":{\"values\":[\"cp1\",\"llt\"]}}}";
    public static final String MOCK_CLAIMS_ENCODED = "%7B%22access_token%22%3A%7B%22xms_cc%22%3A%7B%22values%22%3A%5B%22cp1%22%2C%22llt%22%5D%7D%7D%7D";
    public static final String MOCK_RESPONSE_TYPE = "MOCK_RESPONSE_TYPE";
    public static final String MOCK_REDIRECT_URI = "MOCK_REDIRECT_URI";
    public static final String MOCK_STATE = "MOCK_STATE";
    public static final String MOCK_STATE_ENCODED = "TU9DS19TVEFURQ";
    public static final String MOCK_SCOPE = "MOCK_SCOPE MOCK_SCOPE2";
    public static final String MOCK_SCOPE_ENCODED = "MOCK_SCOPE+MOCK_SCOPE2";
    public static final String MOCK_QUERY_1 = "MOCK_QUERY_1";
    public static final String MOCK_QUERY_2 = "MOCK_QUERY_2";
    public static final String MOCK_VALUE_1 = "MOCK_VALUE_1";
    public static final String MOCK_VALUE_2 = "MOCK_VALUE_2";
    public static final String MOCK_HEADER_1 = "MOCK_HEADER_1";
    public static final String MOCK_HEADER_2 = "MOCK_HEADER_2";
    public static final List<Map.Entry<String, String>> MOCK_EXTRA_QUERY_PARAMS = new ArrayList<Map.Entry<String, String>>(){{
        add(new AbstractMap.SimpleEntry<>(MOCK_QUERY_1, MOCK_VALUE_1));
        add(new AbstractMap.SimpleEntry<>(MOCK_QUERY_2, MOCK_VALUE_2));
    }};
    public static final HashMap<String, String> MOCK_REQUEST_HEADERS =  new HashMap<String, String>(){{
        put(MOCK_HEADER_1, MOCK_VALUE_1);
        put(MOCK_HEADER_2, MOCK_VALUE_2);
    }};
    public static final String MOCK_AUTH_CODE = "some_authorization_code";
    public static final String MOCK_AUTH_CODE_AND_STATE = "code=" + MOCK_AUTH_CODE + "&state=" + MOCK_STATE_ENCODED;
    public static final String MOCK_ERROR_CODE = "mock_error_code";
    public static final String MOCK_ERROR_MESSAGE = "mock_error_msg";
    public static final String MOCK_ERROR_DESCRIPTION = "access_denied_error_description";
    public static final String MOCK_CORRELATION_ID = "correlationId";
    public static final String MOCK_FRAGMENT_STRING = "#_=_";
    public static final String BROKER_INSTALLATION_REQUIRED_BROWSER_REDIRECT_URI = "msauth://com.msft.identity.client.sample.local/1wIqXSqBj7w%2Bh11ZifsnqwgyKrY%3D?wpj=1&username=mock%40test.onmicrosoft.com&app_link=https%3a%2f%2fplay.google.com%2fstore%2fapps%2fdetails%3fid%3dcom.azure.authenticator";
    public static final String BROKER_INSTALLATION_REQUIRED_WEBVIEW_REDIRECT_URI = "msauth://wpj/?username=mock%40test.onmicrosoft.com&app_link=https%3a%2f%2fplay.google.com%2fstore%2fapps%2fdetails%3fid%3dcom.azure.authenticator%26referrer%3dcom.msft.identity.client.sample.local";
    public static final String WPJ_REQUIRED_REDIRECT_URI = "msauth://wpj/?username=mock%40test.onmicrosoft.com&client_info=SOME_GUID";
    public static final String SUCCEED_REDIRECT_URI = "msauth://com.msft.identity.client.sample.local/1wIqXSqBj7w%2Bh11ZifsnqwgyKrY%3D?code=[SOME_CODE]&state=1254%3af57ca8fb-94f5-4ddc-8e84-4fa0ec9d6320-643fd790-537f-423d-b943-9024c6e249ac&session_state=a7c008dc-3820-493a-82e8-680a45a53ebc";
    public static final String ERROR_RESPONSE_REDIRECT_URI = "msauth://com.msft.identity.client.sample.local/1wIqXSqBj7w%2Bh11ZifsnqwgyKrY%3D?error=invalid_request&error_description=AADSTS900144%3a+The+request+body+must+contain+the+following+parameter%3a+%27response_type%27.%0d%0aTrace+ID%3a+9bee0922-7158-44b3-8b22-4addd9225901%0d%0aCorrelation+ID%3a+6abae31d-3a08-4b95-8299-7d7ace789a59%0d%0aTimestamp%3a+2021-07-19+23%3a21%3a09Z&error_uri=https%3a%2f%2flogin.microsoftonline.com%2ferror%3fcode%3d900144&state=1254%3a0ea28b21-8a94-4e5d-a0ff-3a5d3cc07720-9536ce32-aabc-4b0e-98f8-e55cf00e7cfe";
    public static final String CANCEL_RESPONSE_REDIRECT_URI = "msauth://com.msft.identity.client.sample.local/1wIqXSqBj7w%2Bh11ZifsnqwgyKrY%3D?error=access_denied&error_subcode=cancel&state=1254%3a0ea28b21-8a94-4e5d-a0ff-3a5d3cc07720-9536ce32-aabc-4b0e-98f8-e55cf00e7cfe";
    public static final String MALFORMED_REDIRECT_URI = ":/";
    public static final String MOCK_WPJ_USERNAME = "mock@test.onmicrosoft.com";
    public static final PkceChallenge MOCK_PKCE_CHALLENGE = PkceChallenge.newPkceChallenge();
}
