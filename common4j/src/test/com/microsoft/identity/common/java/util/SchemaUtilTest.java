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
package com.microsoft.identity.common.java.util;

import com.microsoft.identity.common.java.dto.CredentialType;
import com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.AzureActiveDirectoryIdToken;
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsIdToken;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class SchemaUtilTest {

    @Test
    public void getAuthority_nullIdTokenReturnsNull() {
        Assert.assertNull(SchemaUtil.getAuthority(null));
    }

    @Test
    public void getAvatarUrl_nullIdTokenReturnsNull() {
        Assert.assertNull(SchemaUtil.getAvatarUrl(null));
    }

    @Test
    public void getAlternativeAccountId_nullIdTokenReturnsNull() {
        Assert.assertNull(SchemaUtil.getAlternativeAccountId(null));
    }

    @Test
    public void getIdentityProvider_nullIdTokenStringReturnsNull() {
        Assert.assertNull(SchemaUtil.getIdentityProvider(null));
    }

    @Test
    public void getIdentityProvider_malformedTokenReturnsNull() {
        // A non-parseable token string is swallowed and yields null.
        Assert.assertNull(SchemaUtil.getIdentityProvider("not-a-valid-jwt"));
    }

    @Test
    public void getCredentialTypeFromVersion_nullDefaultsToIdToken() {
        Assert.assertEquals(CredentialType.IdToken.name(),
                SchemaUtil.getCredentialTypeFromVersion(null));
    }

    @Test
    public void getCredentialTypeFromVersion_emptyDefaultsToIdToken() {
        Assert.assertEquals(CredentialType.IdToken.name(),
                SchemaUtil.getCredentialTypeFromVersion(""));
    }

    @Test
    public void getCredentialTypeFromVersion_malformedDefaultsToIdToken() {
        Assert.assertEquals(CredentialType.IdToken.name(),
                SchemaUtil.getCredentialTypeFromVersion("not-a-valid-jwt"));
    }

    @Test
    public void getHomeAccountId_nullClientInfoReturnsNull() {
        Assert.assertNull(SchemaUtil.getHomeAccountId(null));
    }

    @Test
    public void getTenantId_nullInputsReturnNull() {
        Assert.assertNull(SchemaUtil.getTenantId(null, null));
    }

    @Test
    public void getDisplayableId_prefersPreferredUsername() {
        final Map<String, String> claims = new HashMap<>();
        claims.put(MicrosoftStsIdToken.PREFERRED_USERNAME, "preferred@contoso.com");
        claims.put(MicrosoftStsIdToken.EMAIL, "email@contoso.com");
        Assert.assertEquals("preferred@contoso.com", SchemaUtil.getDisplayableId(claims));
    }

    @Test
    public void getDisplayableId_fallsBackToEmail() {
        final Map<String, String> claims = new HashMap<>();
        claims.put(MicrosoftStsIdToken.EMAIL, "email@contoso.com");
        Assert.assertEquals("email@contoso.com", SchemaUtil.getDisplayableId(claims));
    }

    @Test
    public void getDisplayableId_fallsBackToUpn() {
        final Map<String, String> claims = new HashMap<>();
        claims.put(AzureActiveDirectoryIdToken.UPN, "upn@contoso.com");
        Assert.assertEquals("upn@contoso.com", SchemaUtil.getDisplayableId(claims));
    }

    @Test
    public void getDisplayableId_missingClaimsReturnsSentinel() {
        final Map<String, String> claims = new HashMap<>();
        Assert.assertEquals(SchemaUtil.MISSING_FROM_THE_TOKEN_RESPONSE,
                SchemaUtil.getDisplayableId(claims));
    }
}
