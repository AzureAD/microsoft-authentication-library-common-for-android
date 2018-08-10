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

/**
 * A class representing a client assertion used by the authorization server to authenticate the client application.
 * Adding support for client authentication for internal use with test execution
 * https://tools.ietf.org/html/rfc7521#section-6.1
 */
public abstract class ClientAssertion {

    private static final String DEFAULT_CLIENT_ASSERTION_TYPE = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";

    private String mClientAssertion;
    private String mClientAssertionType = DEFAULT_CLIENT_ASSERTION_TYPE;

    /**
     * @return mClientAssertion
     */
    public String getClientAssertion() {
        return mClientAssertion;
    }

    /**
     * @param clientAssertion client assertion string.
     */
    public void setClientAssertion(final String clientAssertion) {
        mClientAssertion = clientAssertion;
    }

    /**
     * @return mClientAssertionType of the client assertion.
     */
    public String getClientAssertionType() {
        return mClientAssertionType;
    }

    /**
     * @param clientAssertionType client assertion type of the client assertion.
     */
    public void setClientAssertionType(final String clientAssertionType) {
        mClientAssertionType = clientAssertionType;
    }

}
