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

import com.google.gson.JsonParseException;
import com.microsoft.identity.common.java.util.ObjectMapper;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftTokenRequest;
import com.microsoft.identity.common.internal.providers.oauth2.TokenRequest;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResponse;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.Map;

@RunWith(JUnit4.class)
public class ObjectMapperTest {

    public final static String CLIENT_ID = "1234";
    public final static String GRANT_TYPE = "client_credentials";
    public final static String CLIENT_SECRET = "secret";
    public final static String CLIENT_ASSERTION_TYPE = "assertion_type";
    public final static String CLIENT_ASSERTION = "assertion";
    public final static String SCOPES = "openid profile mail.read mail.send";
    public final static String JSON_TOKEN_REQUEST = "{" +
            "client_id: '" + CLIENT_ID + "', id_token: 'idtokenval', other_param: 'other_value' }";
    public final static String JSON_TOKEN_REQUEST_MALFORMED = "{" +
            "client_id: '" + CLIENT_ID + "', id_token: 'idtokenval', other_param: 'other_value' ";
    public final static String JSON_TOKEN_REQUEST_OTHER_VALUE = "{" +
            "client_id: '" + CLIENT_ID + "', id_token: 'idtokenval', other_param: 1, something_else: [ 1, 2 ] }";
    public final static String JSON_TOKEN_REQUEST_OTHER_VALUE_DUPES = "{" +
            "client_id: '" + CLIENT_ID + "', id_token: 'idtokenval1', id_token: 'idtokenval', other_param: 1, something_else: [ 1, 2 ] }";
    public final static String JSON_TOKEN_REQUEST_ARRAY = "[" +
            "'client_id', '" + CLIENT_ID + "', 'id_token', 'idtokenval', 'other_param', 'other_value' ]";


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

    @Test
    public void test_JsonToObjectMS() {
        MicrosoftTokenRequest tr = ObjectMapper.deserializeJsonStringToObject(JSON_TOKEN_REQUEST, MicrosoftTokenRequest.class);

        Assert.assertEquals(CLIENT_ID, tr.getClientId());
        final Iterator<Map.Entry<String, String>> iterator = tr.getExtraParameters().iterator();
        Map.Entry<String, String> param = iterator.next();
        Assert.assertEquals("id_token", param.getKey());
        Assert.assertEquals("idtokenval", param.getValue());
        param = iterator.next();
        Assert.assertEquals("other_param", param.getKey());
        Assert.assertEquals("other_value", param.getValue());
        Assert.assertFalse(iterator.hasNext());

    }
    @Test
    public void test_JsonToObjectMSResponse() {
        TokenResponse tr = ObjectMapper.deserializeJsonStringToObject(JSON_TOKEN_REQUEST, TokenResponse.class);

        Assert.assertEquals("idtokenval", tr.getIdToken());
        final Iterator<Map.Entry<String, String>> iterator = tr.getExtraParameters().iterator();
        Map.Entry<String, String> param = iterator.next();
        Assert.assertEquals("client_id", param.getKey());
        Assert.assertEquals(CLIENT_ID, param.getValue());
        param = iterator.next();
        Assert.assertEquals("other_param", param.getKey());
        Assert.assertEquals("other_value", param.getValue());
        Assert.assertFalse(iterator.hasNext());
    }
    @Test(expected = JsonParseException.class)
    public void test_JsonToObjectResponseMalformed() {
        TokenResponse tr = ObjectMapper.deserializeJsonStringToObject(JSON_TOKEN_REQUEST_MALFORMED, TokenResponse.class);

        Assert.assertEquals("idtokenval", tr.getIdToken());
        final Iterator<Map.Entry<String, String>> iterator = tr.getExtraParameters().iterator();
        Map.Entry<String, String> param = iterator.next();
        Assert.assertEquals("client_id", param.getKey());
        Assert.assertEquals(CLIENT_ID, param.getValue());
        param = iterator.next();
        Assert.assertEquals("other_param", param.getKey());
        Assert.assertEquals("other_value", param.getValue());
        Assert.assertFalse(iterator.hasNext());
    }
    // Here we're leaving off everything that isn't a string, for now.
    @Test
    public void test_JsonToObjectMSResponseNumbersAndStuff() {
        TokenResponse tr = ObjectMapper.deserializeJsonStringToObject(JSON_TOKEN_REQUEST_OTHER_VALUE, TokenResponse.class);

        Assert.assertEquals("idtokenval", tr.getIdToken());
        final Iterator<Map.Entry<String, String>> iterator = tr.getExtraParameters().iterator();
        Map.Entry<String, String> param = iterator.next();
        Assert.assertEquals("client_id", param.getKey());
        Assert.assertEquals(CLIENT_ID, param.getValue());
        Assert.assertFalse(iterator.hasNext());
    }
    // Here we're leaving off everything that isn't a string, for now.  Duplicate values overwrite.
    @Test
    public void test_JsonToObjectMSResponseNumbersAndStuffWithDupes() {
        TokenResponse tr = ObjectMapper.deserializeJsonStringToObject(JSON_TOKEN_REQUEST_OTHER_VALUE_DUPES, TokenResponse.class);

        Assert.assertEquals("idtokenval", tr.getIdToken());
        final Iterator<Map.Entry<String, String>> iterator = tr.getExtraParameters().iterator();
        Map.Entry<String, String> param = iterator.next();
        Assert.assertEquals("client_id", param.getKey());
        Assert.assertEquals(CLIENT_ID, param.getValue());
        Assert.assertFalse(iterator.hasNext());
    }
    @Test(expected = JsonParseException.class)
    public void test_JsonToObjectMSResponseArray() {
        TokenResponse tr = ObjectMapper.deserializeJsonStringToObject(JSON_TOKEN_REQUEST_ARRAY, TokenResponse.class);

        Assert.assertEquals("idtokenval", tr.getIdToken());
        final Iterator<Map.Entry<String, String>> iterator = tr.getExtraParameters().iterator();
        Map.Entry<String, String> param = iterator.next();
        Assert.assertEquals("client_id", param.getKey());
        Assert.assertEquals(CLIENT_ID, param.getValue());
        param = iterator.next();
        Assert.assertEquals("other_param", param.getKey());
        Assert.assertEquals("other_value", param.getValue());
        Assert.assertFalse(iterator.hasNext());
    }

}
