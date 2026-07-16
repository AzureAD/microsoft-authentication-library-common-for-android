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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URLDecoder
import java.net.URLEncoder

/**
 * Proof-of-concept for design risk **R5** of the MAM "Broker-Install Request Resume" feature
 * (see design-docs/[Android] MAM Broker-Install Resume).
 *
 * R5 asked: *"Does appending our install `referrer` to the Company Portal Play Store link break
 * the [BrokerInstallLinkValidator] allow-list, or fail to survive Play delivery?"*
 *
 * R5 decomposes into two independent halves:
 *
 *  - **(a) Allow-list + encoding contract** — pure logic, fully verifiable here on the JVM. This
 *    test resolves it: it exercises the REAL [BrokerInstallLinkValidator] against a Play link that
 *    carries the LOCKED referrer contract `src=mamca&originPkg=..&redirectUri=..&cid=..`, and proves
 *    the round-trip survives the nasty base64 `+ / =` characters in a real `redirectUri`.
 *
 *  - **(b) Play Store actually delivers the referrer to the installed CP** — an empirical property of
 *    Google Play + the Play Install Referrer API that CANNOT be verified on the JVM. It requires a
 *    device/emulator with Play and a reader app. The manual procedure + reader snippet live in the
 *    design doc (§ "R5 PoC"). This test deliberately does not claim to cover (b).
 *
 * What this PoC establishes:
 *  1. A correctly-encoded packed referrer keeps the Play link on the allow-list — so we may re-validate
 *     after appending (defense in depth) instead of blindly launching.
 *  2. Packing everything into a SINGLE `referrer` value is REQUIRED: appending the fields as separate
 *     top-level query params is (correctly) rejected by the validator. This is the design guardrail.
 *  3. The encode/decode contract recovers `src`, `originPkg`, `redirectUri` (including `:// +/ =`), and
 *     `cid` byte-for-byte — the failure mode that would otherwise silently corrupt the `cid`/redirect.
 *  4. The `market://` fallback carries the same referrer but is NOT an https link (so it must be
 *     launched directly, never fed through the https-only validator).
 *
 * The [MamResumeReferrerPoc] helper below is intentionally kept inside the test (nothing lands in
 * `src/main`). It is the shape of the production builder/parser that graduates into common4j during
 * PBI-2 (AB#3686094) and the CP-side reader.
 */
class MamResumeReferrerPocTest {

    // A realistic redirect URI: msauth://<pkg>/<base64 SHA-1 signature>. The base64 payload contains
    // the exact characters (`+`, `/`, `=`) that naive encoding corrupts — the whole point of the test.
    private val originPkg = "com.microsoft.office.outlook"
    private val redirectUri = "msauth://com.microsoft.office.outlook/GC+pJ8k9dItg3F1lZ7q2rY0aBcD="
    private val cid = "3f2504e0-4f89-11d3-9a0c-0305e82c3301"
    private val cpPlayId = "com.microsoft.windowsintune.companyportal"

    // region (a) Allow-list + encoding contract — verifiable on the JVM

    @Test
    fun packedReferrerOnPlayLink_staysOnAllowlist() {
        val playLink = MamResumeReferrerPoc.buildAppLinkWithReferrer(
            appLink = "https://play.google.com/store/apps/details?id=$cpPlayId",
            originPkg = originPkg,
            redirectUri = redirectUri,
            cid = cid
        )

        // The whole packed contract rides as ONE `referrer=` param value, so the validator sees only
        // {id, referrer} — both allow-listed by hasOnlyAllowedExtras(). We CAN safely re-validate.
        assertTrue(
            "Packed referrer must keep the CP Play link on the allow-list. Link was:\n$playLink",
            BrokerInstallLinkValidator.isSafeBrokerInstallLink(playLink)
        )
    }

    @Test
    fun separateParams_areRejected_provingWhyWePack() {
        // The WRONG way: append the contract fields as separate top-level params.
        val badLink = "https://play.google.com/store/apps/details?id=$cpPlayId" +
                "&src=mamca&originPkg=$originPkg&cid=$cid"

        // hasOnlyAllowedExtras() rejects any extra param that is not `id`/`referrer`. This is exactly
        // why the contract packs everything into a single referrer value.
        assertFalse(
            "Separate top-level params must be rejected (documents the packing requirement).",
            BrokerInstallLinkValidator.isSafeBrokerInstallLink(badLink)
        )
    }

    @Test
    fun referrer_roundTrips_throughPlayDelivery_thenCpParse() {
        val playLink = MamResumeReferrerPoc.buildAppLinkWithReferrer(
            appLink = "https://play.google.com/store/apps/details?id=$cpPlayId",
            originPkg = originPkg,
            redirectUri = redirectUri,
            cid = cid
        )

        // Simulate what Google Play hands to Company Portal: the value of the `referrer` param,
        // URL-decoded once by Play (== our inner packed string).
        val referrerAsCpReceivesIt = MamResumeReferrerPoc.extractReferrerAsPlayWouldDeliver(playLink)

        // CP-side parse (blueprint: AccountTransfer.getAccountTransferDataFromReferrer).
        val parsed = MamResumeReferrerPoc.parseReferrer(referrerAsCpReceivesIt)

        assertEquals("mamca", parsed["src"])
        assertEquals(originPkg, parsed["originPkg"])
        assertEquals("cid survived", cid, parsed["cid"])
        // The critical assertion: the base64 `+ / =` in the redirect URI is recovered byte-for-byte.
        assertEquals("redirectUri (with + / =) survived", redirectUri, parsed["redirectUri"])
    }

    @Test
    fun referrerLength_isWellWithinPlayLimit() {
        val referrer = MamResumeReferrerPoc.buildPackedReferrer(originPkg, redirectUri, cid)
        // Play's install-referrer practical limit is well over this; the contract is ~170-260 chars.
        assertTrue(
            "Referrer length ${referrer.length} should be comfortably under 1 KB.",
            referrer.length < 1024
        )
    }

    @Test
    fun marketFallback_carriesReferrer_butIsNotHttps() {
        val market = MamResumeReferrerPoc.buildMarketFallback(cpPlayId, originPkg, redirectUri, cid)

        assertTrue("market:// fallback must carry the referrer.", market.contains("referrer="))
        assertTrue("market:// fallback must target CP.", market.contains("id=$cpPlayId"))
        // It is NOT an https link, so it must be launched directly and must never be fed to the
        // https-only allow-list validator (which would — correctly — reject it).
        assertFalse(
            "market:// fallback is not https and is not meant to pass the https validator.",
            BrokerInstallLinkValidator.isSafeBrokerInstallLink(market)
        )
    }

    /**
     * Emits the concrete URLs so a reviewer can copy them straight into the device procedure for the
     * (b) Play-delivery half. Not an assertion — a convenience for the manual PoC.
     */
    @Test
    fun printConcreteUrlsForDeviceTest() {
        val playLink = MamResumeReferrerPoc.buildAppLinkWithReferrer(
            appLink = "https://play.google.com/store/apps/details?id=$cpPlayId",
            originPkg = originPkg,
            redirectUri = redirectUri,
            cid = cid
        )
        val market = MamResumeReferrerPoc.buildMarketFallback(cpPlayId, originPkg, redirectUri, cid)
        println("[R5 PoC] Primary (append to app_link):\n  $playLink")
        println("[R5 PoC] Fallback (market://):\n  $market")
        println("[R5 PoC] Referrer value length: ${MamResumeReferrerPoc.buildPackedReferrer(originPkg, redirectUri, cid).length}")
    }

    // endregion

    /**
     * PoC-shaped builder/parser for the install referrer. Mirrors the LOCKED Contract A/B from the
     * design doc. Graduates to production common4j (builder) + CP (parser) during implementation.
     */
    private object MamResumeReferrerPoc {

        private const val UTF8 = "UTF-8"

        /** Builds the packed referrer value: `src=mamca&originPkg=..&redirectUri=..&cid=..` (inner-encoded). */
        fun buildPackedReferrer(originPkg: String, redirectUri: String, cid: String): String {
            return "src=mamca" +
                    "&originPkg=" + URLEncoder.encode(originPkg, UTF8) +
                    "&redirectUri=" + URLEncoder.encode(redirectUri, UTF8) +
                    "&cid=" + URLEncoder.encode(cid, UTF8)
        }

        /**
         * Appends the packed referrer to the server-provided `app_link` as a single `referrer` param.
         * [CommonURIBuilder] performs the OUTER percent-encoding, guaranteeing exactly one extra param.
         */
        fun buildAppLinkWithReferrer(
            appLink: String,
            originPkg: String,
            redirectUri: String,
            cid: String
        ): String {
            val referrer = buildPackedReferrer(originPkg, redirectUri, cid)
            // Note: CommonURIBuilder.addParameter() is intentionally unsupported in this repo;
            // setParameter() is the sanctioned API and performs the outer percent-encoding.
            return CommonURIBuilder(appLink)
                .setParameter("referrer", referrer)
                .build()
                .toString()
        }

        /** Builds the `market://details?id=CP&referrer=..` fallback used if the app_link append doesn't reach Play. */
        fun buildMarketFallback(
            playId: String,
            originPkg: String,
            redirectUri: String,
            cid: String
        ): String {
            val referrer = buildPackedReferrer(originPkg, redirectUri, cid)
            return "market://details?id=$playId&referrer=" + URLEncoder.encode(referrer, UTF8)
        }

        /**
         * Simulates the value Google Play hands to the installed app via `InstallReferrerClient`:
         * the `referrer` query-param value, URL-decoded once by Play (== our inner packed string).
         */
        fun extractReferrerAsPlayWouldDeliver(playLink: String): String {
            val params = CommonURIBuilder(playLink).queryParams
            val referrer = params.firstOrNull { it.name == "referrer" }?.value
            requireNotNull(referrer) { "No referrer param on $playLink" }
            return referrer
        }

        /** CP-side parse of the packed referrer back into a map (blueprint: AccountTransfer parse). */
        fun parseReferrer(referrer: String): Map<String, String> {
            val out = LinkedHashMap<String, String>()
            for (pair in referrer.split("&")) {
                val idx = pair.indexOf('=')
                if (idx <= 0) continue
                val key = pair.substring(0, idx)
                val value = URLDecoder.decode(pair.substring(idx + 1), UTF8)
                out[key] = value
            }
            return out
        }
    }
}
