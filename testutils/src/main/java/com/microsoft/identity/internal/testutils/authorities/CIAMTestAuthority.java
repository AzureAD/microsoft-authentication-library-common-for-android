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
package com.microsoft.identity.internal.testutils.authorities;

import com.microsoft.identity.common.java.BuildConfig;
import com.microsoft.identity.common.java.authorities.CIAMAuthority;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.AzureActiveDirectorySlice;
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Configuration;
import com.microsoft.identity.common.java.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.java.providers.oauth2.OAuth2StrategyParameters;
import com.microsoft.identity.common.java.util.JsonUtil;
import com.microsoft.identity.common.java.util.StringUtil;
import com.microsoft.identity.internal.testutils.strategies.ResourceOwnerPasswordCredentialsTestStrategy;

import org.json.JSONException;

import java.util.Map;

public class CIAMTestAuthority extends CIAMAuthority {

    private static final String TAG = CIAMTestAuthority.class.getSimpleName();

    public CIAMTestAuthority(String authorityUrl) {
        super(authorityUrl);
    }

    private MicrosoftStsOAuth2Configuration createOAuth2Configuration() {
        final String methodName = ":createOAuth2Configuration";
        final MicrosoftStsOAuth2Configuration config = new MicrosoftStsOAuth2Configuration();
        config.setAuthorityUrl(this.getAuthorityURL());
        config.setMultipleCloudsSupported(false);

        if (mSlice != null) {
            final AzureActiveDirectorySlice slice = new AzureActiveDirectorySlice();
            slice.setSlice(mSlice.getSlice());
            slice.setDataCenter(mSlice.getDataCenter());
            config.setSlice(slice);
        }

        if (!StringUtil.isNullOrEmpty(BuildConfig.SLICE) || !StringUtil.isNullOrEmpty(BuildConfig.DC)) {
            final AzureActiveDirectorySlice slice = new AzureActiveDirectorySlice();
            slice.setSlice(BuildConfig.SLICE);
            slice.setDataCenter(BuildConfig.DC);
            mSlice = slice;
        }

        final String localFlightsFromBuild = BuildConfig.LOCAL_FLIGHTS;
        if (!StringUtil.isNullOrEmpty(localFlightsFromBuild)) {
            try {
                Map<String, String> localFlights = JsonUtil.extractJsonObjectIntoMap(localFlightsFromBuild);
                for (Map.Entry<String, String> entry : localFlights.entrySet()) {
                    config.getFlightParameters().put(entry.getKey(), entry.getValue());
                }
            } catch (JSONException e) {
                Logger.error(
                        TAG + methodName,
                        "Unable to set flight parameters",
                        e);
            }
        }

        return config;
    }

    @Override
    public OAuth2Strategy createOAuth2Strategy(OAuth2StrategyParameters parameters) throws ClientException {
        final MicrosoftStsOAuth2Configuration config = createOAuth2Configuration();

        // return a custom ropc test strategy to perform ropc flow for test automation
        return new ResourceOwnerPasswordCredentialsTestStrategy(config);
    }
}
