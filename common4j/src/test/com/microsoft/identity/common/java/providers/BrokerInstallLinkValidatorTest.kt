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

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrokerInstallLinkValidatorTest {

    // region Positive cases

    @Test
    fun acceptsAuthenticatorPlayLink() {
        assertTrue(
            BrokerInstallLinkValidator.isSafeBrokerInstallLink(
                "https://play.google.com/store/apps/details?id=com.azure.authenticator"
            )
        )
    }

    @Test
    fun acceptsCompanyPortalPlayLink() {
        assertTrue(
            BrokerInstallLinkValidator.isSafeBrokerInstallLink(
                "https://play.google.com/store/apps/details?id=com.microsoft.windowsintune.companyportal"
            )
        )
    }

    @Test
    fun acceptsPlayLinkWithReferrer() {
        assertTrue(
            BrokerInstallLinkValidator.isSafeBrokerInstallLink(
                "https://play.google.com/store/apps/details?id=com.azure.authenticator&referrer=com.contoso.app"
            )
        )
    }

    @Test
    fun acceptsChinaFwlinkWithTrailingSlash() {
        assertTrue(
            BrokerInstallLinkValidator.isSafeBrokerInstallLink(
                "https://go.microsoft.com/fwlink/?linkid=2134649"
            )
        )
    }

    @Test
    fun acceptsChinaFwlinkWithoutTrailingSlash() {
        assertTrue(
            BrokerInstallLinkValidator.isSafeBrokerInstallLink(
                "https://go.microsoft.com/fwlink?linkid=2134649"
            )
        )
    }

    @Test
    fun acceptsHttpsCaseInsensitiveScheme() {
        assertTrue(
            BrokerInstallLinkValidator.isSafeBrokerInstallLink(
                "HTTPS://play.google.com/store/apps/details?id=com.azure.authenticator"
            )
        )
    }

    @Test
    fun acceptsMixedCaseHost() {
        assertTrue(
            BrokerInstallLinkValidator.isSafeBrokerInstallLink(
                "https://Play.Google.Com/store/apps/details?id=com.azure.authenticator"
            )
        )
    }

    // endregion

    // region Negative cases - input shape

    @Test
    fun rejectsNull() {
        assertFalse(BrokerInstallLinkValidator.isSafeBrokerInstallLink(null))
    }

    @Test
    fun rejectsEmpty() {
        assertFalse(BrokerInstallLinkValidator.isSafeBrokerInstallLink(""))
    }

    @Test
    fun rejectsWhitespace() {
        assertFalse(BrokerInstallLinkValidator.isSafeBrokerInstallLink("   "))
    }

    @Test
    fun rejectsMalformedUri() {
        assertFalse(BrokerInstallLinkValidator.isSafeBrokerInstallLink(":/"))
    }

    // endregion

    // region Negative cases - scheme

    @Test
    fun rejectsHttpScheme() {
        assertFalse(
            BrokerInstallLinkValidator.isSafeBrokerInstallLink(
                "http://play.google.com/store/apps/details?id=com.azure.authenticator"
            )
        )
    }

    @Test
    fun rejectsJavascriptScheme() {
        assertFalse(BrokerInstallLinkValidator.isSafeBrokerInstallLink("javascript:alert(1)"))
    }

    @Test
    fun rejectsIntentScheme() {
        assertFalse(
            BrokerInstallLinkValidator.isSafeBrokerInstallLink(
                "intent://x#Intent;scheme=https;end"
            )
        )
    }

    @Test
    fun rejectsFileScheme() {
        assertFalse(BrokerInstallLinkValidator.isSafeBrokerInstallLink("file:///etc/passwd"))
    }

    @Test
    fun rejectsDataScheme() {
        // data: with literal '<' is not even a valid URI; should be rejected without throwing.
        assertFalse(BrokerInstallLinkValidator.isSafeBrokerInstallLink("data:text/html,<script>"))
    }

    @Test
    fun rejectsContentScheme() {
        assertFalse(BrokerInstallLinkValidator.isSafeBrokerInstallLink("content://x"))
    }

    @Test
    fun rejectsAppScheme() {
        assertFalse(BrokerInstallLinkValidator.isSafeBrokerInstallLink("app://x"))
    }

    @Test
    fun rejectsMarketScheme() {
        assertFalse(
            BrokerInstallLinkValidator.isSafeBrokerInstallLink(
                "market://details?id=com.azure.authenticator"
            )
        )
    }

    // endregion

    // region Negative cases - host

    @Test
    fun rejectsArbitraryHost() {
        assertFalse(
            BrokerInstallLinkValidator.isSafeBrokerInstallLink(
                "https://attacker.com/store/apps/details?id=com.azure.authenticator"
            )
        )
    }

    @Test
    fun rejectsHostSuffixAttack() {
        assertFalse(
            BrokerInstallLinkValidator.isSafeBrokerInstallLink(
                "https://play.google.com.attacker.tld/store/apps/details?id=com.azure.authenticator"
            )
        )
    }

    @Test
    fun rejectsEmbeddedUserInfo() {
        assertFalse(
            BrokerInstallLinkValidator.isSafeBrokerInstallLink(
                "https://attacker.com@play.google.com/store/apps/details?id=com.azure.authenticator"
            )
        )
    }

    @Test
    fun rejectsNonDefaultPort() {
        assertFalse(
            BrokerInstallLinkValidator.isSafeBrokerInstallLink(
                "https://play.google.com:8443/store/apps/details?id=com.azure.authenticator"
            )
        )
    }

    // endregion

    // region Negative cases - path

    @Test
    fun rejectsWrongPathOnPlayHost() {
        assertFalse(
            BrokerInstallLinkValidator.isSafeBrokerInstallLink(
                "https://play.google.com/some/other/path?id=com.azure.authenticator"
            )
        )
    }

    @Test
    fun rejectsWrongPathOnFwlinkHost() {
        assertFalse(
            BrokerInstallLinkValidator.isSafeBrokerInstallLink(
                "https://go.microsoft.com/anything?linkid=2134649"
            )
        )
    }

    // endregion

    // region Negative cases - query

    @Test
    fun rejectsNonAllowlistedPackage() {
        assertFalse(
            BrokerInstallLinkValidator.isSafeBrokerInstallLink(
                "https://play.google.com/store/apps/details?id=com.attacker.malware"
            )
        )
    }

    @Test
    fun rejectsMissingId() {
        assertFalse(
            BrokerInstallLinkValidator.isSafeBrokerInstallLink(
                "https://play.google.com/store/apps/details"
            )
        )
    }

    @Test
    fun rejectsDuplicateId() {
        assertFalse(
            BrokerInstallLinkValidator.isSafeBrokerInstallLink(
                "https://play.google.com/store/apps/details?id=com.azure.authenticator&id=com.attacker"
            )
        )
    }

    @Test
    fun rejectsUnknownExtraParameter() {
        assertFalse(
            BrokerInstallLinkValidator.isSafeBrokerInstallLink(
                "https://play.google.com/store/apps/details?id=com.azure.authenticator&unexpected=x"
            )
        )
    }

    @Test
    fun rejectsNonAllowlistedFwlinkId() {
        assertFalse(
            BrokerInstallLinkValidator.isSafeBrokerInstallLink(
                "https://go.microsoft.com/fwlink/?linkid=99999"
            )
        )
    }

    @Test
    fun rejectsMissingLinkId() {
        assertFalse(
            BrokerInstallLinkValidator.isSafeBrokerInstallLink(
                "https://go.microsoft.com/fwlink/"
            )
        )
    }

    // endregion

    // region Negative cases - fragment

    @Test
    fun rejectsFragment() {
        assertFalse(
            BrokerInstallLinkValidator.isSafeBrokerInstallLink(
                "https://play.google.com/store/apps/details?id=com.azure.authenticator#fragment"
            )
        )
    }

    // endregion
}
