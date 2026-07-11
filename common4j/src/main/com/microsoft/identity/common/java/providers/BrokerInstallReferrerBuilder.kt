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
package com.microsoft.identity.common.java.providers

import com.microsoft.identity.common.java.util.CommonURIBuilder
import java.net.URISyntaxException

/**
 * Sets the Play Store **install referrer** to the calling app's package, matching Company Portal's
 * existing {@code InstallReferrerReceiver} contract: CP reads the referrer as a bare origin-package
 * string, stores it, and on foreground redirects the user back to that app after install.
 *
 * <p>No correlation id or redirect uri is sent through the referrer. The referrer is a one-way
 * channel (app -> Google Play -> Company Portal) and CP's redirect is package-based, so nothing can
 * round-trip back to us here. The resume correlation id is held in-process by
 * [BrokerInstallResumeCoordinator], and the resume is driven by the app returning to the foreground
 * after CP's redirect — so the referrer only needs to tell CP which app to send the user back to.
 * The link must already be allowlisted by [BrokerInstallLinkValidator]; this builder does not alter
 * the destination.
 */
object BrokerInstallReferrerBuilder {

    private const val PARAM_REFERRER = "referrer"

    /**
     * Sets [installUrl]'s `referrer` parameter to [originPackage].
     *
     * @param installUrl Allowlisted Play/fwlink install URL.
     * @param originPackage Package that triggered the install; Company Portal uses it to redirect the
     *   user back to the calling app after install.
     * @return URL with the referrer set, or the original [installUrl] if it cannot be parsed.
     */
    @JvmStatic
    fun withInstallReferrer(
        installUrl: String,
        originPackage: String
    ): String {
        val builder: CommonURIBuilder = try {
            CommonURIBuilder(installUrl)
        } catch (e: URISyntaxException) {
            return installUrl
        }
        builder.setParameter(PARAM_REFERRER, originPackage)
        return builder.toString()
    }
}
