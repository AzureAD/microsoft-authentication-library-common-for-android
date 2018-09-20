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
package com.microsoft.identity.common.internal.providers.microsoft.microsoftsts;

import android.net.Uri;

import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectoryOAuth2Configuration;

import java.net.URL;

public class MicrosoftStsOAuth2Configuration extends AzureActiveDirectoryOAuth2Configuration {

    public URL getAuthorizationEndpoint() {
        return getEndpoint(getAuthorityUrl(), "/oAuth2/v2.0/authorize");
    }

    public URL getTokenEndpoint() {
        return getEndpoint(getAuthorityUrl(), "/oAuth2/v2.0/token");
    }

    private URL getEndpoint(URL root, String endpoint) {
        try {
            Uri authorityUri = Uri.parse(root.toString());
            Uri endpointUri = authorityUri.buildUpon()
                    .appendPath(endpoint)
                    .build();
            return new URL(endpointUri.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;

    }

}
