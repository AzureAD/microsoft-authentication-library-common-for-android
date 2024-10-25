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

import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.net.HttpResponse;
import com.microsoft.identity.common.java.util.ObjectMapper;

import lombok.NonNull;

/**
 * Handles standard successful token responses from the Microsoft STS.
 */
public class MicrosoftStsTokenResponseHandler extends AbstractMicrosoftStsTokenResponseHandler {

    /**
     * Expects JSON response and deserializes it to {@link MicrosoftStsTokenResponse}.
     * @return Deserialized response into MicrosoftStsTokenResponse
     */
    @Override
    protected MicrosoftStsTokenResponse getSuccessfulResponse(@NonNull final HttpResponse httpResponse) {
        return ObjectMapper.deserializeJsonStringToObject(
                httpResponse.getBody(),
                MicrosoftStsTokenResponse.class
        );
    }
}
