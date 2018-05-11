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
package com.microsoft.identity.common.internal.providers.oauth2;

import java.net.URL;
import java.util.List;

/*
 * Represents the information returned from the OpenID Provider Configuration Endpoint
 * https://openid.net/specs/openid-connect-discovery-1_0.html#ProviderMetadata
 */

public class OpenIDProviderConfiguration {

    private String mIssuer;
    private URL mAuthorizationEndpoint;
    private URL mTokenEndpoint;
    private URL mUserInfoEndpoint;
    private URL mJWKSUri;
    private List<String> mSupportedScopes;
    private List<String> mResponseTypesSupported;
    private List<String> mACRValuesSupported;
    private List<String> mSubjectTypesSupported;
    private List<String> mIDTokenSigningAlgValuesSupported;
    private List<String> mClaimsSupported;

}
