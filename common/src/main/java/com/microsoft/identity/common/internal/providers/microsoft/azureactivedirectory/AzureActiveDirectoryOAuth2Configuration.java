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
package com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory;

import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Configuration;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class AzureActiveDirectoryOAuth2Configuration extends OAuth2Configuration {

    private boolean mAuthorityHostValidationEnabled = true;
    private URL mAuthorityUrl;
    private Map<String, String> mFlightParameters = new HashMap<>();
    private AzureActiveDirectorySlice mSlice;
    private boolean mMultipleCloudsSupported;

    /**
     * @return True if authority host validation enabled, false otherwise.
     */
    public boolean isAuthorityHostValidationEnabled() {
        return mAuthorityHostValidationEnabled;
    }

    /**
     * @param authorityHostValidationEnabled boolean
     */
    public void setAuthorityHostValidationEnabled(boolean authorityHostValidationEnabled) {
        mAuthorityHostValidationEnabled = authorityHostValidationEnabled;
    }

    public URL getAuthorityUrl() {
        return this.mAuthorityUrl;
    }

    public void setAuthorityUrl(URL authorityUrl) {
        this.mAuthorityUrl = authorityUrl;
    }

    public Map<String, String> getFlightParameters() {
        return mFlightParameters;
    }

    public void setFlightParameters(Map<String, String> flightParameters) {
        mFlightParameters = flightParameters;
    }

    public AzureActiveDirectorySlice getSlice() {
        return mSlice;
    }

    public void setSlice(AzureActiveDirectorySlice slice) {
        mSlice = slice;
    }

    public void setMultipleCloudsSupported(boolean supported){
        mMultipleCloudsSupported = supported;
    }

    public boolean getMultipleCloudsSupported(){
        return mMultipleCloudsSupported;
    }

}
