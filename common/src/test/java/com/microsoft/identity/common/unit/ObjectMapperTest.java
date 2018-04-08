package com.microsoft.identity.common.unit;


import com.microsoft.identity.common.internal.net.ObjectMapper;
import com.microsoft.identity.common.internal.providers.oauth2.TokenRequest;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

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
    public void test_JsonToObject(){
        TokenRequest tr = (TokenRequest) ObjectMapper.deserializeJsonStringToObject(JSON_TOKEN_REQUEST, TokenRequest.class);

        Assert.assertEquals(CLIENT_ID, tr.getClientId());
    }

}
