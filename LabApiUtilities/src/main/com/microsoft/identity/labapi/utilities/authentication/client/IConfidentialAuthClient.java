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
package com.microsoft.identity.labapi.utilities.authentication.client;

import com.microsoft.identity.labapi.utilities.authentication.IAuthenticationResult;
import com.microsoft.identity.labapi.utilities.authentication.ITokenParameters;
import com.microsoft.identity.labapi.utilities.authentication.common.CertificateCredential;
import com.microsoft.identity.labapi.utilities.authentication.common.ClientAssertion;

/**
 * An interface that describes a confidential authentication client. Such an authentication client
 * has the ability to obtain tokens for an app by using client credentials.
 * <p>
 * The primary purpose of this interface is to decouple the token fetch from a particular
 * implementation i.e. anyone can implement this interface and configure a confidential auth client
 * as they like. For example, one implementation may choose to use msal4j library whereas another
 * interface can choose to use adal4j in its implementation.
 */
public interface IConfidentialAuthClient {

    /**
     * Acquire a token for a confidential client using a client secret.
     *
     * @param clientSecret    the client secret that will be exchanged for a token
     * @param tokenParameters the token parameters to use while acquiring token
     * @return an {@link IAuthenticationResult} containing the result of the token request
     */
    IAuthenticationResult acquireToken(String clientSecret, ITokenParameters tokenParameters);

    /**
     * Acquire a token for a confidential client using a client assertion.
     *
     * @param clientAssertion the client assertion that will be exchanged for a token
     * @param tokenParameters the token parameters to use while acquiring token
     * @return an {@link IAuthenticationResult} containing the result of the token request
     */
    IAuthenticationResult acquireToken(ClientAssertion clientAssertion, ITokenParameters tokenParameters);

    /**
     * Acquire a token for a confidential client using a Certificate.
     *
     * @param certificateCredential the {@link CertificateCredential} that will be exchanged for a token
     * @param tokenParameters       the token parameters to use while acquiring token
     * @return an {@link IAuthenticationResult} containing the result of the token request
     */
    IAuthenticationResult acquireToken(CertificateCredential certificateCredential, ITokenParameters tokenParameters);
}
