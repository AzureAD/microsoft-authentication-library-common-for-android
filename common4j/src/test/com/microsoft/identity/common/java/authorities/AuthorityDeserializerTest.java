//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.common.java.authorities;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.AzureActiveDirectory;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;

public class AuthorityDeserializerTest {

    private static final String B2C_AUTHORITY = "{" +
            "   \"type\": \"B2C\"," +
            "   \"authority_url\": \"https://login.microsoftonline.com/tfp/msidlabb2c.onmicrosoft.com/B2C_1_SISOPolicy/\"" +
            "}";
    private static final String AAD_AUTHORITY = "{" +
            "   \"type\": \"AAD\"," +
            "   \"authority_url\": \"https://login.microsoftonline.us/common\"" +
            "}";
    private static final String ADFS_AUTHORITY = "{\"type\": \"ADFS\", \"default\": true }";
    private static final String UNKNOWN_AUTHORITY = "{\"type\": \"AAAD\", \"default\": true }";

    private Gson gson;

    @Before
    public void setup() {
        gson = new GsonBuilder()
                .registerTypeAdapter(Authority.class, new AuthorityDeserializer())
                .create();
    }

    @Test
    public void testDeserializeB2C() {
        final Authority authority = gson.fromJson(B2C_AUTHORITY, Authority.class);

        Assert.assertTrue(authority instanceof AzureActiveDirectoryB2CAuthority);
        Assert.assertEquals("https://login.microsoftonline.com/tfp/msidlabb2c.onmicrosoft.com/B2C_1_SISOPolicy/", authority.getAuthorityUri().toString());
    }

    @Test
    public void testDeserializeAAD() {
        final Authority authority = gson.fromJson(AAD_AUTHORITY, Authority.class);

        Assert.assertTrue(authority instanceof AzureActiveDirectoryAuthority);

        Assert.assertEquals("https://login.microsoftonline.us", ((AzureActiveDirectoryAuthority) authority).mAudience.getCloudUrl());
        Assert.assertEquals("common", ((AzureActiveDirectoryAuthority) authority).mAudience.getTenantId());

        Assert.assertEquals("https://login.microsoftonline.us/common", authority.getAuthorityUri().toString());
    }

    // AzureActiveDirectoryAuthority.getAuthorityUri() relies on results from cloud metadata.
    // We should make sure that the result are valid after cloud discovery is done.
    @Test
    public void testDeserializeAfterGettingCloudMetadata() throws Exception {
        AzureActiveDirectory.performCloudDiscovery();

        final Authority authority = gson.fromJson(AAD_AUTHORITY, Authority.class);
        Assert.assertTrue(authority instanceof AzureActiveDirectoryAuthority);

        Assert.assertEquals("https://login.microsoftonline.us", ((AzureActiveDirectoryAuthority) authority).mAudience.getCloudUrl());
        Assert.assertEquals("common", ((AzureActiveDirectoryAuthority) authority).mAudience.getTenantId());

        Assert.assertEquals("https://login.microsoftonline.us/common", authority.getAuthorityUri().toString());
    }

    @Test
    public void testDeserializeADFS() {
        final Authority authority = gson.fromJson(ADFS_AUTHORITY, Authority.class);

        Assert.assertTrue(authority instanceof ActiveDirectoryFederationServicesAuthority);
    }

    @Test
    public void testUnknownAuthority() {
        final Authority authority = gson.fromJson(UNKNOWN_AUTHORITY, Authority.class);

        Assert.assertTrue(authority instanceof UnknownAuthority);
    }
}
