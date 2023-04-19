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

import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.AzureActiveDirectorySlice;
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Configuration;
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Strategy;
import com.microsoft.identity.common.java.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.java.providers.oauth2.OAuth2StrategyParameters;
import com.microsoft.identity.common.java.providers.oauth2.OpenIdProviderConfiguration;

import lombok.NonNull;

/**
 * Authority class to support CIAM Authority type.
 *
 * This class utilizes {@link OpenIdProviderConfiguration} to supply endpoints directly from Open ID
 */
public class CIAMAuthority extends Authority {
    private static transient final String TAG = CIAMAuthority.class.getSimpleName();

    // Adding this flag to indicate whether or not we should load the OpenId Configuration as part of handling
    // CIAM flows. This is currently relevant for fetching the authorization endpoint from OpenId rather than
    // using the authority itself + adding the default authorization endpoint.
    private final boolean CIAM_USE_OPENID_CONFIGURATION = true;

    public static final String CIAM_LOGIN_URL_SEGMENT = "ciamlogin.com";

    public CIAMAuthority(@NonNull final String authorityUrl) {
        mAuthorityTypeString = Authority.CIAM;
        mAuthorityUrlString = authorityUrl;
    }

    private MicrosoftStsOAuth2Configuration createOAuth2Configuration() {
        final String methodName = ":createOAuth2Configuration";
        Logger.verbose(
                TAG + methodName,
                "Creating OAuth2Configuration"
        );
        final MicrosoftStsOAuth2Configuration config = new MicrosoftStsOAuth2Configuration();
        config.setAuthorityUrl(this.getAuthorityURL());
        config.setMultipleCloudsSupported(false);

        if (mSlice != null) {
            Logger.info(
                    TAG + methodName,
                    "Setting slice parameters..."
            );
            final AzureActiveDirectorySlice slice = new AzureActiveDirectorySlice();
            slice.setSlice(mSlice.getSlice());
            slice.setDataCenter(mSlice.getDataCenter());
            config.setSlice(slice);
        }

        return config;
    }

    @Override
    public OAuth2Strategy createOAuth2Strategy(OAuth2StrategyParameters parameters) throws ClientException {
        final MicrosoftStsOAuth2Configuration config = createOAuth2Configuration();

        // CIAM Authorities fetch endpoints from open id configuration, communicate that to
        // strategy through parameters
        parameters.setUsingOpenIdConfiguration(CIAM_USE_OPENID_CONFIGURATION);

        final MicrosoftStsOAuth2Strategy strategy = new MicrosoftStsOAuth2Strategy(config, parameters);
        return strategy;
    }

    /**
     * This method takes a CIAM authority string of format "tenant.ciamlogin.com" or "https://tenant.ciamlogin.com"
     * and converts it into a full authority url with a path segment of format "/tenant.onmicrosoft.com"
     * @param authorityNoPath authority to be transformed
     * @return full CIAM authority with path
     */
    public static String getFullAuthorityUrlFromAuthorityWithoutPath(@NonNull String authorityNoPath){
        // Remove "https://" if it was included as part of the authority
        if (authorityNoPath.startsWith("https://")){
            authorityNoPath = authorityNoPath.substring(8);
        }
        if (authorityNoPath.endsWith("/")){
            authorityNoPath = authorityNoPath.substring(0, authorityNoPath.length() - 1);
        }
        // Split environment to isolate the tenant
        final String tenant = authorityNoPath.split("\\.")[0];
        return "https://" + authorityNoPath + "/" + tenant + ".onmicrosoft.com";
    }
}
