// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.common.internal.ui.webview

import android.app.Activity
import android.os.Build
import android.webkit.WebView
import androidx.webkit.ScriptHandler
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.microsoft.identity.common.internal.broker.AuthUxJavaScriptInterface
import com.microsoft.identity.common.internal.numberMatch.NumberMatchHelper
import com.microsoft.identity.common.internal.ui.webview.switchbrowser.SwitchBrowserProtocolCoordinator
import com.microsoft.identity.common.java.flighting.CommonFlight
import com.microsoft.identity.common.java.flighting.CommonFlightsManager
import com.microsoft.identity.common.java.flighting.IFlightsManager
import com.microsoft.identity.common.java.flighting.IFlightsProvider
import com.microsoft.identity.common.logging.Logger
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.*
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

/** Tests the wrapper registration without replacing production native eligibility or factories. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class AuthUxDocumentStartInjectionTest {
    private lateinit var activity: Activity
    private lateinit var client: AzureActiveDirectoryWebViewClient
    private lateinit var view: WebView
    private lateinit var process: MockedStatic<ProcessUtil>
    private lateinit var features: MockedStatic<WebViewFeature>
    private lateinit var webKit: MockedStatic<WebViewCompat>
    private var authProcess = true
    private var baseFlight = true
    private var earlyFlight = true
    private var supported = true
    private var registrationFailure: RuntimeException? = null
    private val registrations = mutableListOf<Registration>()

    /** Mocks only host/provider boundaries; native receiver creation remains real. */
    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(Activity::class.java).get()
        view = mock()
        val flights: IFlightsProvider = mock()
        whenever(flights.isFlightEnabled(any())).thenAnswer {
            when (it.getArgument<CommonFlight>(0)) {
                CommonFlight.ENABLE_JS_API_FOR_AUTHUX -> baseFlight
                CommonFlight.ENABLE_AUTHUX_DOCUMENT_START_SCRIPT -> earlyFlight
                else -> false
            }
        }
        val manager: IFlightsManager = mock()
        whenever(manager.getFlightsProvider(any())).thenReturn(flights)
        CommonFlightsManager.initializeCommonFlightsManager(manager)

        process = mockStatic(ProcessUtil::class.java)
        process.`when`<Boolean> {
            ProcessUtil.isRunningOnAuthService(activity.applicationContext)
        }.thenAnswer { authProcess }
        features = mockStatic(WebViewFeature::class.java)
        features.`when`<Boolean> {
            WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)
        }.thenAnswer { supported }
        webKit = mockStatic(WebViewCompat::class.java)
        webKit.`when`<ScriptHandler> {
            WebViewCompat.addDocumentStartJavaScript(any(), any(), any())
        }.thenAnswer {
            registrationFailure?.let { error -> throw error }
            val registration = Registration(
                it.getArgument(0), it.getArgument(1), it.getArgument(2), mock()
            )
            registrations.add(registration)
            registration.handler
        }
        client = newClient()
    }

    /** Releases static mocks and singleton state between tests. */
    @After
    fun tearDown() {
        client.removeAuthUxDocumentStartScript()
        webKit.close()
        features.close()
        process.close()
        CommonFlightsManager.resetFlightsManager()
        NumberMatchHelper.numberMatchMap.remove("early001")
        NumberMatchHelper.numberMatchMap.remove("early002")
    }

    /** Early injection defaults on while preserving the existing bridge. */
    @Test
    fun newFlightDefaultsOnAndBaseFlightStaysOn() {
        CommonFlightsManager.resetFlightsManager()
        val provider = CommonFlightsManager.getFlightsProvider()
        assertTrue(provider.isFlightEnabled(CommonFlight.ENABLE_AUTHUX_DOCUMENT_START_SCRIPT))
        assertTrue(provider.isFlightEnabled(CommonFlight.ENABLE_JS_API_FOR_AUTHUX))
        assertEquals("EnableAuthUxDocumentStartScript",
            CommonFlight.ENABLE_AUTHUX_DOCUMENT_START_SCRIPT.key)
        client.initializeAuthUxJavaScriptApi(view, ALLOWED)
        verify(view).addJavascriptInterface(any<AuthUxJavaScriptInterface>(), eq("broker"))
        assertEquals(1, registrations.size)
    }

    /** All 16 gate combinations preserve native exposure independently of the early flag/API. */
    @Test
    fun eligibilityMatrixKeepsNativeAndEarlyGatesSeparate() {
        for (auth in listOf(false, true)) {
            for (base in listOf(false, true)) {
                for (early in listOf(false, true)) {
                    for (capability in listOf(false, true)) {
                        authProcess = auth
                        baseFlight = base
                        earlyFlight = early
                        supported = capability
                        val candidate = newClient()
                        val candidateView: WebView = mock()
                        val previous = registrations.size
                        candidate.initializeAuthUxJavaScriptApi(candidateView, ALLOWED)
                        val label = "auth=$auth base=$base early=$early supported=$capability"
                        assertEquals(label, if (auth && base && early && capability) 1 else 0,
                            registrations.size - previous)
                        verify(candidateView, times(if (auth && base) 1 else 0))
                            .addJavascriptInterface(any<AuthUxJavaScriptInterface>(), eq("broker"))
                        candidate.removeAuthUxDocumentStartScript()
                    }
                }
            }
        }
    }

    /** Exact four rules constrain only early injection; provider matching is a device-test concern. */
    @Test
    fun registersExactlyApprovedHttpsDefaultPortRules() {
        client.initializeAuthUxJavaScriptApi(view, ALLOWED)
        assertEquals(setOf(
            "https://*.microsoftonline.com",
            "https://*.microsoftonline.us",
            "https://*.microsoftonline.cn",
            "https://*.microsoft.com"
        ), registrations.single().origins)
        assertSame(view, registrations.single().owner)
        verify(view, never()).loadUrl(any())
        verify(view, never()).evaluateJavascript(any(), anyOrNull())
    }

    /** Registration covers future allowed documents without exposing native APIs on the initial B. */
    @Test
    fun disallowedInitialUrlStillRegistersRestrictedScriptButNotNativeBridge() {
        client.initializeAuthUxJavaScriptApi(view, DISALLOWED)
        assertEquals(1, registrations.size)
        verify(view, never()).addJavascriptInterface(any(), any())
        client.onPageFinished(view, DISALLOWED)
        verify(view, never()).evaluateJavascript(any(), anyOrNull())
    }

    /** Scheme/port restrictions must not migrate into the pre-existing native/late gate. */
    @Test
    fun nativeEligibilityRetainsHostSuffixSemantics() {
        val cases = mapOf(
            ALLOWED to true,
            "https://login.microsoftonline.us/path" to true,
            "https://login.microsoftonline.cn/path" to true,
            "https://login.microsoft.com/path" to true,
            "http://login.microsoftonline.com/path" to true,
            "https://login.microsoftonline.com:8443/path" to true,
            "https://microsoftonline.com/path" to false,
            "https://login.microsoftonline.com.example.test/path" to false,
            DISALLOWED to false
        )
        for ((url, eligible) in cases) {
            val candidate = newClient()
            val candidateView: WebView = mock()
            candidate.initializeAuthUxJavaScriptApi(candidateView, url)
            candidate.onPageFinished(candidateView, url)
            verify(candidateView, times(if (eligible) 1 else 0))
                .addJavascriptInterface(any<AuthUxJavaScriptInterface>(), eq("broker"))
            verify(candidateView, times(if (eligible) 1 else 0))
                .evaluateJavascript(eq(FORWARDING_SCRIPT), isNull())
            candidate.removeAuthUxDocumentStartScript()
        }
    }

    /** Unsupported providers retain the exact existing late-injection script. */
    @Test
    fun unsupportedProviderRetainsLateWrapper() {
        supported = false
        client.initializeAuthUxJavaScriptApi(view, ALLOWED)
        client.onPageFinished(view, ALLOWED)
        webKit.verifyNoInteractions()
        verify(view).evaluateJavascript(eq(FORWARDING_SCRIPT), isNull())
    }

    /** Logs report successful registration and unsupported-provider fallback, not script execution. */
    @Test
    fun logsRegistrationSuccessAndUnsupportedProviderFallback() {
        mockStatic(Logger::class.java).use { logger ->
            supported = false
            client.initializeAuthUxJavaScriptApi(view, ALLOWED)
            supported = true
            client.initializeAuthUxJavaScriptApi(view, ALLOWED)
            client.initializeAuthUxJavaScriptApi(view, ALLOWED)
            client.initializeAuthUxJavaScriptApi(mock(), ALLOWED)

            logger.verify {
                Logger.info(LOG_TAG,
                    "Document-start scripts unsupported; retaining late Auth UX injection.")
            }
            logger.verify({
                Logger.info(LOG_TAG, "Auth UX document-start script registration succeeded.")
            }, times(2))
        }
    }

    /** The guarded early script shares the unmodified late forwarding body and dynamic lookup. */
    @Test
    fun earlyScriptGuardsMainFrameAndExistingReceiverWithoutChangingLateBody() {
        client.initializeAuthUxJavaScriptApi(view, ALLOWED)
        assertEquals(
            "(function() { if (window !== window.top) { return; } " +
                "var bridge = window.broker; " +
                "if (!bridge || typeof bridge.receiveAuthUxMessage !== 'function') { return; } " +
                FORWARDING_SCRIPT + " })();",
            registrations.single().script
        )
        client.onPageFinished(view, ALLOWED)
        verify(view).evaluateJavascript(eq(FORWARDING_SCRIPT), isNull())
    }

    /** Repeated initialization neither duplicates nor removes the same owner's registration. */
    @Test
    fun repeatedInitializationRegistersOncePerOwner() {
        client.initializeAuthUxJavaScriptApi(view, ALLOWED)
        client.initializeAuthUxJavaScriptApi(view, ALLOWED)
        assertEquals(1, registrations.size)
        verify(registrations.single().handler, never()).remove()
        verify(view, times(2)).addJavascriptInterface(any<AuthUxJavaScriptInterface>(), eq("broker"))
    }

    /** Native navigation updates and late eligibility are unchanged, with no per-page script adds. */
    @Test
    fun navigationDoesNotReregisterEarlyScriptOrChangeNativeLifecycle() {
        client.initializeAuthUxJavaScriptApi(view, ALLOWED)
        client.onPageStarted(view, ALLOWED, null)
        client.onPageStarted(view, DISALLOWED, null)
        client.onPageFinished(view, DISALLOWED)
        verify(view, never()).evaluateJavascript(any(), anyOrNull())
        client.onPageStarted(view, ALLOWED, null)
        client.onPageFinished(view, ALLOWED)
        assertEquals(1, registrations.size)
        verify(view, times(3)).addJavascriptInterface(any<AuthUxJavaScriptInterface>(), eq("broker"))
        verify(view).removeJavascriptInterface("broker")
        verify(view).evaluateJavascript(eq(FORWARDING_SCRIPT), isNull())
        verify(registrations.single().handler, never()).remove()
    }

    /** Replacing the WebView removes the old registration before creating the new one. */
    @Test
    fun replacementOwnerRemovesOldHandle() {
        client.initializeAuthUxJavaScriptApi(view, ALLOWED)
        val previous = registrations.single().handler
        val replacement: WebView = mock()
        doAnswer {
            assertEquals("Removal must precede replacement registration", 1, registrations.size)
        }.whenever(previous).remove()
        client.initializeAuthUxJavaScriptApi(replacement, ALLOWED)
        verify(previous).remove()
        assertEquals(2, registrations.size)
        assertSame(replacement, registrations.last().owner)
        client.removeAuthUxDocumentStartScript()
        verify(registrations.last().handler).remove()
    }

    /** Replacement still releases the old owner if the new owner's rollout gate is off. */
    @Test
    fun disabledReplacementReleasesOldOwnerWithoutRegistering() {
        client.initializeAuthUxJavaScriptApi(view, ALLOWED)
        earlyFlight = false
        client.initializeAuthUxJavaScriptApi(mock(), ALLOWED)
        assertEquals(1, registrations.size)
        verify(registrations.single().handler).remove()
        assertNull(ReflectionHelpers.getField<WebView>(client, "mAuthUxDocumentStartScriptOwner"))
    }

    /** Cleanup is safe before initialization, on repetition, and before reusing the owner. */
    @Test
    fun cleanupIsIdempotentAndAllowsReregistration() {
        client.removeAuthUxDocumentStartScript()
        client.initializeAuthUxJavaScriptApi(view, ALLOWED)
        val previous = registrations.single().handler
        client.removeAuthUxDocumentStartScript()
        client.removeAuthUxDocumentStartScript()
        verify(previous).remove()
        assertNull(ReflectionHelpers.getField<WebView>(client, "mAuthUxDocumentStartScriptOwner"))
        assertNull(ReflectionHelpers.getField<ScriptHandler>(
            client, "mAuthUxDocumentStartScriptHandler"))
        client.initializeAuthUxJavaScriptApi(view, ALLOWED)
        assertEquals(2, registrations.size)
    }

    /** The rollout decision is retained for the registered owner's lifetime, not changed per page. */
    @Test
    fun sameOwnerDoesNotRetroactivelyToggleItsRegistration() {
        client.initializeAuthUxJavaScriptApi(view, ALLOWED)
        earlyFlight = false
        client.initializeAuthUxJavaScriptApi(view, ALLOWED)
        assertEquals(1, registrations.size)
        verify(registrations.single().handler, never()).remove()
    }

    /** Registration failures remain visible and do not poison the ownership state. */
    @Test
    fun registrationFailurePropagatesAndDoesNotPreventLaterInitialization() {
        val failure = IllegalStateException("provider registration failure")
        registrationFailure = failure
        mockStatic(Logger::class.java).use { logger ->
            assertSame(failure, assertThrows(IllegalStateException::class.java) {
                client.initializeAuthUxJavaScriptApi(view, ALLOWED)
            })
            logger.verify({
                Logger.info(LOG_TAG, "Auth UX document-start script registration succeeded.")
            }, never())
            assertNull(ReflectionHelpers.getField<WebView>(client, "mAuthUxDocumentStartScriptOwner"))
            registrationFailure = null
            client.initializeAuthUxJavaScriptApi(view, ALLOWED)
            assertEquals(1, registrations.size)
            logger.verify {
                Logger.info(LOG_TAG, "Auth UX document-start script registration succeeded.")
            }
        }
    }

    /** A failing removal must not silently forget the handle or report successful cleanup. */
    @Test
    fun removalFailurePropagatesAndRetainsHandleForRetry() {
        client.initializeAuthUxJavaScriptApi(view, ALLOWED)
        val handler = registrations.single().handler
        doThrow(IllegalStateException("provider removal failure")).doNothing()
            .whenever(handler).remove()
        assertThrows(IllegalStateException::class.java) { client.removeAuthUxDocumentStartScript() }
        assertSame(handler, ReflectionHelpers.getField<ScriptHandler>(
            client, "mAuthUxDocumentStartScriptHandler"))
        client.removeAuthUxDocumentStartScript()
        verify(handler, times(2)).remove()
    }

    /** Early registration preserves real native number matching without any recorder. */
    @Test
    fun initialAndNavigationFactoriesRetainNumberMatchingWithoutRecorder() {
        client.initializeAuthUxJavaScriptApi(view, ALLOWED)
        client.onPageStarted(view, ALLOWED, null)
        val bridges = argumentCaptor<AuthUxJavaScriptInterface>()
        verify(view, times(2)).addJavascriptInterface(bridges.capture(), eq("broker"))
        bridges.allValues.forEachIndexed { index, bridge ->
            val session = if (index == 0) "early001" else "early002"
            bridge.receiveAuthUxMessage(
                """{"correlationID":"test","action_name":"write_data","action_component":"broker","params":{"operation":"number_matching","sessionID":"$session","code_match":"07"}}"""
            )
            assertEquals("07", NumberMatchHelper.numberMatchMap[session])
        }
    }

    private fun newClient() = AzureActiveDirectoryWebViewClient(
        activity, mock(), {}, "msauth://test/redirect",
        mock<SwitchBrowserProtocolCoordinator>(), null, false
    )

    private data class Registration(
        val owner: WebView,
        val script: String,
        val origins: Set<String>,
        val handler: ScriptHandler
    )

    private companion object {
        const val LOG_TAG = "AzureActiveDirectoryWebViewClient:initializeAuthUxDocumentStartScript"
        const val ALLOWED = "https://login.microsoftonline.com/authorize"
        const val DISALLOWED = "https://bridge-negative.example.test/page"
        const val FORWARDING_SCRIPT =
            "window.broker.postMessageToBroker = function(message) { " +
                "    window.broker.receiveAuthUxMessage(JSON.stringify(message)); " +
                "};"
    }
}
