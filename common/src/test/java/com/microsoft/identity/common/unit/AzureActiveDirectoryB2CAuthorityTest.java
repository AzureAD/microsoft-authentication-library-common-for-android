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
package com.microsoft.identity.common.unit;

import androidx.annotation.NonNull;

import com.microsoft.identity.common.internal.authorities.Authority;
import com.microsoft.identity.common.internal.authorities.AzureActiveDirectoryB2CAuthority;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.ParameterizedRobolectricTestRunner;

import java.util.Arrays;
import java.util.List;

@RunWith(ParameterizedRobolectricTestRunner.class)
public class AzureActiveDirectoryB2CAuthorityTest {

    private static final String POLICY_NAME = "policyName";
    private static final String TENANT_NAME = "tenantName";
    private static final String TENANT_ID = "tenantID";
    private static final String AAD_B2C_HOSTNAME = "azureADB2CHostname";
    private static final List<String> B2C_AUTHORITY_URLS = Arrays.asList(
            "https://" + AAD_B2C_HOSTNAME + "/tfp/" + TENANT_NAME + "/" + POLICY_NAME,
            "https://" + TENANT_NAME + ".b2clogin.com/tfp/" + TENANT_ID + "/" + POLICY_NAME
    );

    @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
    public static Iterable<String> data() {
        return B2C_AUTHORITY_URLS;
    }

    private final String mB2cAuthorityUrl;

    public AzureActiveDirectoryB2CAuthorityTest(@NonNull final String b2cAuthorityUrl) {
        mB2cAuthorityUrl = b2cAuthorityUrl;
    }

    public String getb2cAuthorityUrl() {
        return mB2cAuthorityUrl;
    }

    @Test
    public void testGetB2CPolicyName() {
        final Authority authority = Authority.getAuthorityFromAuthorityUrl(getb2cAuthorityUrl());
        Assert.assertTrue(authority instanceof AzureActiveDirectoryB2CAuthority);
        final String policyName = ((AzureActiveDirectoryB2CAuthority) authority).getB2CPolicyName();
        Assert.assertEquals(POLICY_NAME, policyName);
    }
}
