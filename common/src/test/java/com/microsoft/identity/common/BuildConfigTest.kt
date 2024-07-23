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
package com.microsoft.identity.common

import com.microsoft.identity.common4j.env.BuildReason
import com.microsoft.identity.common4j.env.BuildReason.Companion.isBuildReason
import org.junit.Test

/**
 * Tests for making sure compile time flags aren't turned on in PROD build.
 **/
class BuildConfigTest {

    @Test
    fun failIfBypassRedirecUriCheckEnabled(){
        // Do not run this on scheduled test.
        if (isBuildReason(BuildReason.Schedule)) {
            return
        }

        assert(!BuildConfig.bypassRedirectUriCheck)
    }

    @Test
    fun failIfTrustDebugBrokerFlagEnabled(){
        // Do not run this on scheduled test.
        if (isBuildReason(BuildReason.Schedule)) {
            return
        }

        assert(!BuildConfig.trustDebugBrokerFlag)
    }
}
