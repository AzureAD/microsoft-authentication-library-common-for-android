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
package com.microsoft.identity.common.unit;

import com.microsoft.identity.common.internal.net.ObjectMapper;
import com.microsoft.identity.common.internal.providers.oauth2.TokenRequest;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.UnsupportedEncodingException;

@RunWith(JUnit4.class)
public class ObjectMapperTest {

    public final static String CLIENT_ID = "1234";
    public final static String GRANT_TYPE = "client_credentials";
    public final static String CLIENT_SECRET = "secret";
    public final static String CLIENT_ASSERTION_TYPE = "assertion_type";
    public final static String CLIENT_ASSERTION = "assertion";
    public final static String SCOPES = "openid profile mail.read mail.send";
    public final static String JSON_TOKEN_REQUEST = "{" +
            "client_id: '" + CLIENT_ID + "'}";


    @Test
    public void test_ObjectToFormUrlEncoding() throws UnsupportedEncodingException {

        TokenRequest tr = new TokenRequest();

        tr.setClientAssertion(CLIENT_ASSERTION);
        tr.setClientAssertionType(CLIENT_ASSERTION_TYPE);

        String tokenRequestEncoded = ObjectMapper.serializeObjectToFormUrlEncoded(tr);

        String expected = "client_assertion=" + CLIENT_ASSERTION + "&client_assertion_type=" + CLIENT_ASSERTION_TYPE;

        Assert.assertEquals(expected, tokenRequestEncoded);

    }


    @Test
    public void test_JsonToObject() {
        TokenRequest tr = ObjectMapper.deserializeJsonStringToObject(JSON_TOKEN_REQUEST, TokenRequest.class);

        Assert.assertEquals(CLIENT_ID, tr.getClientId());
    }

}
