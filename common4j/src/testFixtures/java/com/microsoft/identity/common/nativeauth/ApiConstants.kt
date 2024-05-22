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

package com.microsoft.identity.common.nativeauth

import com.microsoft.identity.common.java.nativeauth.BuildValues
import java.net.URL

/**
 * ApiConstants provides various endpoints for mock API for Native Auth endpoints.
 */
interface ApiConstants {
    companion object {
        val BASEPATH = BuildValues.getMockApiUrl()
        private val BASE_REQUEST_PATH = BASEPATH + "1234/"
        val signUpStartRequestUrl = URL(BASE_REQUEST_PATH + "signup/v1.0/start")
        val signUpChallengeRequestUrl = URL(BASE_REQUEST_PATH + "signup/v1.0/challenge")
        val signUpContinueRequestUrl = URL(BASE_REQUEST_PATH + "signup/v1.0/continue")
        val signInInitiateRequestUrl = URL(BASE_REQUEST_PATH + "oauth2/v2.0/initiate")
        val signInChallengeRequestUrl = URL(BASE_REQUEST_PATH + "oauth2/v2.0/challenge")
        val signInTokenRequestUrl = URL(BASE_REQUEST_PATH + "oauth2/v2.0/token")
        val ssprStartRequestUrl = URL(BASE_REQUEST_PATH + "resetpassword/v1.0/start")
        val ssprChallengeRequestUrl = URL(BASE_REQUEST_PATH + "resetpassword/v1.0/challenge")
        val ssprContinueRequestUrl = URL(BASE_REQUEST_PATH + "resetpassword/v1.0/continue")
        val ssprSubmitRequestUrl = URL(BASE_REQUEST_PATH + "resetpassword/v1.0/submit")
        val ssprPollCompletionRequestUrl = URL(BASE_REQUEST_PATH + "resetpassword/v1.0/poll_completion")
        val tokenEndpoint = URL("https://contoso.com/1234/token")
    }
}
