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
package com.microsoft.identity.common.java.providers.microsoft.microsoftsts;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;

/**
 * Context that holds Microsoft STS token request and relevant context executing the request.
 * Currently, it holds a response handler that will be used to handle the token response.
 */
@Getter(AccessLevel.PACKAGE)
@Accessors(prefix = "m")
public class MicrosoftStsTokenRequestContext {

    private final MicrosoftStsTokenRequest mRequest;
    private final AbstractMicrosoftStsTokenResponseHandler mTokenResponseHandler;

    /**
     * Constructor of MicrosoftStsTokenRequestContext.
     * @param request Microsoft STS token request to run
     * @param tokenResponseHandler Handler to handle the token response. If not provided default handler will be used
     */
    public MicrosoftStsTokenRequestContext(
            @NonNull final MicrosoftStsTokenRequest request,
            @Nullable final AbstractMicrosoftStsTokenResponseHandler tokenResponseHandler
    ) {
        mRequest = request;
        if (tokenResponseHandler == null) { // use default handler if not provided
            mTokenResponseHandler = new MicrosoftStsTokenResponseHandler();
        } else {
            mTokenResponseHandler = tokenResponseHandler;
        }
    }
}
