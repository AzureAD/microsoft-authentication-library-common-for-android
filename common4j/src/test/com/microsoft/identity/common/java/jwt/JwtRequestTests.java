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
package com.microsoft.identity.common.java.jwt;

import com.microsoft.identity.common.java.providers.oauth2.TokenRequest;
import com.microsoft.identity.common.java.util.ObjectMapper;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Map;

/**
 * Tests for {@link JwtRequestHeader}, {@link JwtRequestBody}, {@link AbstractJwtRequest}.
 */
@RunWith(JUnit4.class)
public class JwtRequestTests {

    @Test
    public void testJwtHeader() {
        final JwtRequestHeader header = new JwtRequestHeader();
        header.setType();
        header.setAlg(JwtRequestHeader.ALG_VALUE_HS256);
        header.setKId("session");
        header.setCtx("context");
        final Map<String, Object> map = ObjectMapper.serializeObjectHashMap(header);
        Assert.assertEquals("JWT", map.get(AbstractJwtRequest.ClaimNames.TYPE));
        Assert.assertEquals(JwtRequestHeader.ALG_VALUE_HS256, map.get(AbstractJwtRequest.ClaimNames.ALG));
        Assert.assertEquals("session", map.get(AbstractJwtRequest.ClaimNames.KID));
        Assert.assertEquals("context", map.get(AbstractJwtRequest.ClaimNames.CTX));
    }

    @Test
    public void testJwtHeader_serialize() {
        final JwtRequestHeader header = new JwtRequestHeader();
        header.setType();
        header.setAlg(JwtRequestHeader.ALG_VALUE_HS256);
        header.setKId("session");
        header.setCtx("context");
        header.setKdfVersion(2);
        final String expectedHeaderJson = "{\"typ\":\"JWT\",\"ctx\":\"context\",\"alg\":\"HS256\",\"kid\":\"session\",\"kdf_ver\":2}";
        final String headerJson = ObjectMapper.serializeObjectToJsonString(header);
        Assert.assertEquals(expectedHeaderJson, headerJson);
    }
    @Test
    public void testJwtHeader_serialize_defaultKdf() {
        final JwtRequestHeader header = new JwtRequestHeader();
        header.setType();
        header.setAlg(JwtRequestHeader.ALG_VALUE_HS256);
        header.setKId("session");
        header.setCtx("context");
        final String expectedHeaderJson = "{\"typ\":\"JWT\",\"ctx\":\"context\",\"alg\":\"HS256\",\"kid\":\"session\",\"kdf_ver\":1}";
        final String headerJson = ObjectMapper.serializeObjectToJsonString(header);
        Assert.assertEquals(expectedHeaderJson, headerJson);
    }

    @Test
    public void testJwtHeader_deserialize() {
        final String headerJson = "{\"typ\" : \"JWT\", \"ctx\": \"testCtx\", \"kdf_ver\": 2, \"alg\" : \"RS256\", \"kid\" : \"testKid\"}";
        final JwtRequestHeader header = ObjectMapper.deserializeJsonStringToObject(headerJson, JwtRequestHeader.class);
        Assert.assertEquals("JWT", header.getType());
        Assert.assertEquals("testCtx", header.getCtx());
        Assert.assertEquals("RS256", header.getAlg());
        Assert.assertEquals("testKid", header.getKId());
        Assert.assertEquals(2, header.getKdfVersion());
    }

    @Test
    public void testJwtHeader_deserialize_no_Kdf() {
        final String headerJson = "{\"typ\" : \"JWT\", \"ctx\": \"testCtx\", \"alg\" : \"RS256\"}";
        final JwtRequestHeader header = ObjectMapper.deserializeJsonStringToObject(headerJson, JwtRequestHeader.class);
        Assert.assertEquals("JWT", header.getType());
        Assert.assertEquals("testCtx", header.getCtx());
        Assert.assertEquals("RS256", header.getAlg());
        Assert.assertEquals(1, header.getKdfVersion());
    }

    @Test
    public void testJwtBody() {
        final JwtRequestBody body = new JwtRequestBody();
        body.setJwtScope("scope");
        body.setAudience("aud");
        body.setIssuer("issuer");
        body.setGrantType(TokenRequest.GrantTypes.JWT_BEARER);
        body.setNonce("request_nonce");
        body.setRedirectUri("redirect_uri");
        body.setResource("resource");
        body.setIat(1000);
        body.setNBF(2000);
        body.setExp(3000, 100);
        final Map<String, Object> map = ObjectMapper.serializeObjectHashMap(body);
        Assert.assertEquals("scope", map.get(AbstractJwtRequest.ClaimNames.SCOPE));
        Assert.assertEquals("aud", map.get(AbstractJwtRequest.ClaimNames.AUDIENCE));
        Assert.assertEquals("issuer", map.get(AbstractJwtRequest.ClaimNames.ISSUER));
        Assert.assertEquals(TokenRequest.GrantTypes.JWT_BEARER,
                map.get(AbstractJwtRequest.ClaimNames.GRANT_TYPE));
        Assert.assertEquals("resource", map.get(AbstractJwtRequest.ClaimNames.RESOURCE));
        Assert.assertEquals("request_nonce", map.get(AbstractJwtRequest.ClaimNames.NONCE));
        Assert.assertEquals("redirect_uri", map.get(AbstractJwtRequest.ClaimNames.REDIRECT_URI));
        Assert.assertEquals("1000", body.getIat());
        Assert.assertEquals("2000", body.getNbf());
        Assert.assertEquals("3100", body.getExp());
    }
}
