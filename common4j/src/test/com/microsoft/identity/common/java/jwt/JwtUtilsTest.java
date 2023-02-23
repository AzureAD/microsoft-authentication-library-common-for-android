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

import static com.microsoft.identity.common.java.AuthenticationConstants.ENCODING_UTF8;

import com.google.gson.Gson;
import com.microsoft.identity.common.java.providers.oauth2.TokenRequest;
import com.microsoft.identity.common.java.util.StringUtil;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class JwtUtilsTest {

    @Test
    public void testGenerateJWT() {
        final JwtRequestHeader jwtRequestHeader = new JwtRequestHeader();
        jwtRequestHeader.setType();
        jwtRequestHeader.setAlg(JwtRequestHeader.ALG_VALUE_HS256);
        jwtRequestHeader.setCtx("ctx");
        final JwtRequestBody jwtRequestBody = new JwtRequestBody();
        jwtRequestBody.setAudience("aud");
        jwtRequestBody.setIssuer("issuer");
        jwtRequestBody.setJwtScope("scopes");
        jwtRequestBody.setRedirectUri("redirecturi");
        jwtRequestBody.setNonce("nonce");
        jwtRequestBody.setGrantType(TokenRequest.GrantTypes.REFRESH_TOKEN);
        jwtRequestBody.setRefreshToken("refresh_token");
        final String headerJson = new Gson().toJson(jwtRequestHeader);
        final String bodyJson = new Gson().toJson(jwtRequestBody);
        final String expectedJwt =
                StringUtil.encodeUrlSafeString(headerJson.getBytes(ENCODING_UTF8)) + "." + StringUtil.encodeUrlSafeString(bodyJson.getBytes(ENCODING_UTF8));

        final String encodedJwt = JwtUtils.generateJWT(jwtRequestHeader, jwtRequestBody);
        Assert.assertEquals(expectedJwt, encodedJwt);;
    }
}
