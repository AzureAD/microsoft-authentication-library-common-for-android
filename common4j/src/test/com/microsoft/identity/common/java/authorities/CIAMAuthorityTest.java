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

public class CIAMAuthorityTest {

    private static final String B2C_AUTHORITY = "{" +
            "   \"type\": \"B2C\"," +
            "   \"authority_url\": \"https://login.microsoftonline.com/tfp/msidlabb2c.onmicrosoft.com/B2C_1_SISOPolicy/\"" +
            "}";
    private static final String AAD_AUTHORITY = "{" +
            "   \"type\": \"AAD\"," +
            "   \"authority_url\": \"https://login.microsoftonline.us/common\"" +
            "}";
    private static final String CIAM_AUTHORITY_TENANT_DOMAIN = "{" +
            "   \"type\": \"CIAM\"," +
            "   \"authority_url\": \"https://msidlabciam1.ciamlogin.com/msidlabciam1.onmicrosoft.com\"" +
            "}";
    private static final String CIAM_AUTHORITY_TENANT_GUID = "{" +
            "   \"type\": \"CIAM\"," +
            "   \"authority_url\": \"https://msidlabciam1.ciamlogin.com/d57fb3d4-4b5a-4144-9328-9c1f7d58179d\"" +
            "}";
    private static final String CIAM_AUTHORITY_NO_PATH = "{" +
            "   \"type\": \"CIAM\"," +
            "   \"authority_url\": \"https://msidlabciam1.ciamlogin.com\"" +
            "}";
    private static final String ADFS_AUTHORITY = "{\"type\": \"ADFS\", \"default\": true }";
    private static final String UNKNOWN_AUTHORITY = "{\"type\": \"AAAD\", \"default\": true }";

    @Test
    public void testGetTenantNameVariantUrlFromAuthorityWithoutPathTest1() {
        String input = "https://custom.domain.com";
        final String authorityUrl = CIAMAuthority.getTenantNameVariantUrlFromAuthorityWithoutPath(
                input
        );

        Assert.assertEquals("https://custom.domain.com/custom.onmicrosoft.com", authorityUrl);
    }

    @Test
    public void testGetTenantNameVariantUrlFromAuthorityWithoutPathTest2() {
        String input = "https://tenantName.ciamlogin.com/";
        final String authorityUrl = CIAMAuthority.getTenantNameVariantUrlFromAuthorityWithoutPath(
                input
        );

        Assert.assertEquals("https://tenantName.ciamlogin.com/tenantName.onmicrosoft.com", authorityUrl);
    }

    @Test
    public void testGetTenantIdVariantUrlFromAuthorityWithoutPathTest1() {
        final String authorityUrl = CIAMAuthority.getTenantIdVariantUrlFromAuthorityWithoutPath(
                "https://tenantName.ciamlogin.com/",
                "1234"
        );

        Assert.assertEquals("https://tenantName.ciamlogin.com/1234", authorityUrl);
    }

    @Test
    public void testGetTenantIdVariantUrlFromAuthorityWithoutPathTest2() {
        final String authorityUrl = CIAMAuthority.getTenantIdVariantUrlFromAuthorityWithoutPath(
                "https://tenantName.ciamlogin.com",
                "1234"
        );

        Assert.assertEquals("https://tenantName.ciamlogin.com/1234", authorityUrl);
    }
}
