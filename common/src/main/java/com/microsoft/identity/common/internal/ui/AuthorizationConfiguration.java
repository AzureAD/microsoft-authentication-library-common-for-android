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
package com.microsoft.identity.common.internal.ui;

/**
 * Configuration class for Authorization settings
 * Settings include things like:
 * - if using embedded webview
 * - if using browsing
 * - if it is a broker authorization request
 * ...
 */

public class AuthorizationConfiguration {
    // static variable single_instance of type AuthorizationConfiguration.
    private static AuthorizationConfiguration sInstance = null;

    private boolean isBrokerRequest = false;
    private boolean isADALRequest = false;
    private boolean isMSALRequest = false;
    private boolean useEmbeddedWebView = true;
    private boolean useSystemBrowser = false;

    private AuthorizationConfiguration(){
    }

    // static method to create instance of AuthorizationConfiguration class.
    public static AuthorizationConfiguration getInstance() {
        if (sInstance == null) {
            sInstance = new AuthorizationConfiguration();
        }

        return  sInstance;
    }

    public boolean isBrokerRequest() {
        return isBrokerRequest;
    }

    public void setBrokerRequest(boolean brokerRequest) {
        isBrokerRequest = brokerRequest;
    }

    public boolean isADALRequest() {
        return isADALRequest;
    }

    public void setADALRequest(boolean ADALRequest) {
        isADALRequest = ADALRequest;
    }

    public boolean isMSALRequest() {
        return isMSALRequest;
    }

    public void setMSALRequest(boolean MSALRequest) {
        isMSALRequest = MSALRequest;
    }

    public boolean isUseEmbeddedWebView() {
        return useEmbeddedWebView;
    }

    public void setUseEmbeddedWebView(boolean useEmbeddedWebView) {
        this.useEmbeddedWebView = useEmbeddedWebView;
    }

    public boolean isUseSystemBrowser() {
        return useSystemBrowser;
    }

    public void setUseSystemBrowser(boolean useSystemBrowser) {
        this.useSystemBrowser = useSystemBrowser;
    }

}
