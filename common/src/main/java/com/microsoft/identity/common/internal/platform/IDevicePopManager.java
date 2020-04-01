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
package com.microsoft.identity.common.internal.platform;

import android.content.Context;

import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.internal.controllers.TaskCompletedCallbackWithError;

import java.net.URL;

/**
 * Internal convenience class interface for PoP related functions.
 */
public interface IDevicePopManager {

    /**
     * Tests if keys exist.
     *
     * @return True if keys exist, false otherwise.
     */
    boolean asymmetricKeyExists();

    /**
     * Tests for existence of keys AND that they match the match the supplied thumbprint.
     *
     * @param thumbprint The thumbprint to match.
     * @return True if keys exist and they match the supplied thumbprint. False if keys do not match
     * or if keys cannot be loaded due to KeyStore errors.
     */
    boolean asymmetricKeyExists(String thumbprint);

    /**
     * Gets the thumbprint of the current KeyPair.
     *
     * @return The thumbprint.
     */
    String getAsymmetricKeyThumbprint() throws ClientException;

    /**
     * Generates asymmetric keys used by pop.
     *
     * @param callback Async callback with thumbprint/exception info.
     */
    void generateAsymmetricKey(Context context, TaskCompletedCallbackWithError<String, ClientException> callback);

    /**
     * Generates asymmetric keys used by pop.
     *
     * @return The generated RSA KeyPair's thumbprint.
     */
    String generateAsymmetricKey(Context context) throws ClientException;

    /**
     * Clears keys, if present.
     */
    boolean clearAsymmetricKey();

    /**
     * API to generate the req_cnf used for auth code redemptions.
     *
     * @return The req_cnf value.
     */
    String getRequestConfirmation() throws ClientException;

    /**
     * Async API to generate the req_cnf used for auth code redemptions.
     *
     * @return The req_cnf value.
     */
    void getRequestConfirmation(TaskCompletedCallbackWithError<String, ClientException> callback);

    /**
     * Api to create the signed PoP access token.
     *
     * @param httpMethod  The HTTP method that will be used with this outbound request.
     * @param requestUrl  The recipient URL of the outbound request.
     * @param accessToken The access_token from which to derive the signed JWT.
     * @param nonce       Arbitrary value used for replay protection by middleware.
     * @return The signed PoP access token.
     */
    String mintSignedAccessToken(String httpMethod,
                                 URL requestUrl,
                                 String accessToken,
                                 String nonce
    ) throws ClientException;
}
