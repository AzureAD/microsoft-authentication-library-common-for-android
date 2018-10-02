package com.microsoft.identity.common.unit;

import com.microsoft.identity.common.internal.net.ObjectMapper;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsTokenRequest;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.UUID;

@RunWith(JUnit4.class)
public final class TokenRequestTest {


    @Test
    public void testCorrelationIdSerializedCorrectly(){

        UUID correlationId = UUID.randomUUID();
        MicrosoftStsTokenRequest request = new MicrosoftStsTokenRequest();
        request.setCorrelationId(correlationId);

        String jsonRequest = ObjectMapper.serializeObjectToJsonString(request);

        MicrosoftStsTokenRequest deserializedRequest = ObjectMapper.deserializeJsonStringToObject(jsonRequest, MicrosoftStsTokenRequest.class);

        Assert.assertEquals(correlationId, deserializedRequest.getCorrelationId());

    }


}
