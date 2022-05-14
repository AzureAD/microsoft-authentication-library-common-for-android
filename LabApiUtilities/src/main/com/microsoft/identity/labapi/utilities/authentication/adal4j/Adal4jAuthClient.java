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
package com.microsoft.identity.labapi.utilities.authentication.adal4j;

import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.identity.labapi.utilities.authentication.IAuthenticationResult;
import com.microsoft.identity.labapi.utilities.authentication.ITokenParameters;
import com.microsoft.identity.labapi.utilities.authentication.client.IPublicAuthClient;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import lombok.NonNull;
import lombok.SneakyThrows;

/**
 * An implementation of {@link IPublicAuthClient} using ADAL4J library.
 */
public class Adal4jAuthClient implements IPublicAuthClient {

    final ExecutorService service = Executors.newFixedThreadPool(3);

    @Override
    @SneakyThrows
    public IAuthenticationResult acquireToken(@NonNull final String username,
                                              @NonNull final String password,
                                              @NonNull final ITokenParameters tokenParameters) {
        final AuthenticationContext context = new AuthenticationContext(
                tokenParameters.getAuthority(), false, service
        );

        final Future<AuthenticationResult> future = context.acquireToken(
                tokenParameters.getResource(),
                tokenParameters.getClientId(),
                username,
                password,
                null
        );
        return new Adal4jAuthenticationResult(future.get());
    }
}
