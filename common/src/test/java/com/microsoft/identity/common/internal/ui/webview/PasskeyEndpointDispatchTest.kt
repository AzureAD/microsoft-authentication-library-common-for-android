// Copyright (c) Microsoft Corporation.
// All rights reserved.
// Licensed under the MIT License.

package com.microsoft.identity.common.internal.ui.webview

import android.app.Activity
import android.net.Uri
import android.webkit.WebResourceRequest
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.testing.TestLifecycleOwner
import com.microsoft.identity.common.internal.fido.*
import com.microsoft.identity.common.internal.ui.webview.switchbrowser.SwitchBrowserProtocolCoordinator
import com.microsoft.identity.common.java.flighting.*
import com.microsoft.identity.common.java.constants.FidoConstants
import com.microsoft.identity.common.java.opentelemetry.*
import com.microsoft.identity.common.java.ui.webview.authorization.IAuthorizationCompletionCallback
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanContext
import io.opentelemetry.api.trace.StatusCode
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PasskeyEndpointDispatchTest {
    private val policy = PasskeyEndpointPolicy(
        setOf("request.example"), setOf("https://response.example/common/credential")
    )
    private val callback = mock(IAuthorizationCompletionCallback::class.java)
    private val span = mock(Span::class.java)
    private val lifecycleOwner = TestLifecycleOwner()
    private val originalBrokerCheck = FidoManagerFactory.isBrokerHosted
    private lateinit var activity: Activity
    private lateinit var webView: ExtendedTestWebView
    private var observed = true
    private var enforced = false
    private var policySpans = 0
    private var policySelections = 0
    private var providerSelections = 0

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        webView = spy(ExtendedTestWebView())
        webView.setViewTreeLifecycleOwner(lifecycleOwner)
        FidoManagerFactory.isBrokerHosted = { true }
        assertTrue(FidoManagerFactory.setProvider(activity, object : IFidoManagerProvider {
            override fun getFidoManager(activity: Activity): IFidoManager {
                providerSelections++
                return TestFidoManager()
            }
        }))
        val provider = mock(IFlightsProvider::class.java)
        `when`(provider.isFlightEnabled(any(IFlightConfig::class.java))).thenAnswer {
            when (val flight = it.getArgument<IFlightConfig>(0)) {
                CommonFlight.ENABLE_PASSKEY_ENDPOINT_POLICY_OBSERVATION -> observed
                CommonFlight.ENFORCE_PASSKEY_ENDPOINT_POLICY -> enforced
                else -> flight.defaultValue == true
            }
        }
        CommonFlightsManager.initializeCommonFlightsManager(object : IFlightsManager {
            override fun getFlightsProvider(waitForConfigsWithTimeoutInMs: Long) = provider
            override fun getFlightsProviderForTenant(tenantId: String, waitForConfigsWithTimeoutInMs: Long) = provider
        })
        OTelUtility.setSpanFactory(object : IOTelSpanFactory {
            override fun createSpan(name: String): Span {
                if (name == SpanName.ProcessPasskeyEndpointPolicy.name) {
                    policySpans++
                    return span
                }
                return Span.getInvalid()
            }
            override fun createSpan(name: String, callingPackageName: String) = createSpan(name)
            override fun createSpanFromParent(name: String, parentSpanContext: SpanContext?) = createSpan(name)
            override fun createSpanFromParent(name: String, parentSpanContext: SpanContext?, callingPackageName: String) =
                createSpan(name)
        })
    }

    @After
    fun tearDown() {
        FidoManagerFactory.setProvider(activity, null)
        FidoManagerFactory.isBrokerHosted = originalBrokerCheck
        CommonFlightsManager.resetFlightsManager()
        OTelUtility.setSpanFactory(DefaultOTelSpanFactory())
        activity.finish()
    }

    private fun client(endpointPolicy: PasskeyEndpointPolicy = policy): AzureActiveDirectoryWebViewClient =
        object : AzureActiveDirectoryWebViewClient(
            activity, callback, mock(OnPageLoadedCallback::class.java), "app://callback",
            mock(SwitchBrowserProtocolCoordinator::class.java), null, false
        ) {
            override fun getPasskeyEndpointPolicy(): PasskeyEndpointPolicy {
                policySelections++
                return endpointPolicy
            }
        }.apply { setRequestUrl("https://request.example/authorize") }

    private fun dispatch(
        submitUrl: String,
        endpointPolicy: PasskeyEndpointPolicy = policy,
        version: String = "1.0",
        modernCallback: Boolean = false,
        challengeValue: String = "sample-challenge",
        requestUrl: String = "https://request.example/authorize"
    ) {
        val query = Uri.Builder()
            .appendQueryParameter("challenge", challengeValue)
            .appendQueryParameter("relyingPartyIdentifier", "rp.example")
            .appendQueryParameter("version", version)
            .appendQueryParameter("submitUrl", submitUrl)
            .appendQueryParameter("context", "sample-context sample-flow-token")
            .build().encodedQuery
        val url = "${FidoConstants.PASSKEY_PROTOCOL_REDIRECT}?$query"
        val client = client(endpointPolicy)
        client.setRequestUrl(requestUrl)
        if (modernCallback) {
            val request = mock(WebResourceRequest::class.java)
            `when`(request.url).thenReturn(Uri.parse(url))
            `when`(request.isForMainFrame).thenReturn(true)
            assertTrue(client.shouldOverrideUrlLoading(webView, request))
        } else {
            assertTrue(client.shouldOverrideUrlLoading(webView, url))
        }
    }

    @Test
    fun enforcementOffUnlistedResponseStillReceivesAssertion() {
        dispatch("https://unlisted.example/common/credential")
        assertEquals(1, providerSelections)
        assertTrue(webView.isRegularAssertion())
        val headers = requireNotNull(webView.headers)
        verify(webView).loadUrl("https://unlisted.example/common/credential", headers)
        verify(span).setAttribute(AttributeName.passkey_endpoint_policy_action.name, "WOULD_REJECT")
        verify(span).setStatus(StatusCode.OK)
        verify(span).end()
        verifyNoInteractions(callback)
    }

    @Test
    fun enforcementOffApprovedResponseWorks() {
        dispatch("https://response.example/common/credential")
        assertTrue(webView.isRegularAssertion())
        verify(span).setAttribute(AttributeName.passkey_endpoint_policy_action.name, "ALLOW")
    }

    @Test
    fun enforcementOnUnlistedResponseBlockedBeforeProviderSelection() {
        enforced = true
        dispatch("https://unlisted.example/common/credential")
        assertEquals(0, providerSelections)
        assertFalse(webView.urlLoaded)
        verify(webView).stopLoading()
        verify(span).setAttribute(AttributeName.passkey_endpoint_policy_action.name, "BLOCK")
        verify(span).setStatus(StatusCode.ERROR)
        verify(span).end()
        assertFalse(mockingDetails(callback).invocations.isEmpty())
    }

    @Test
    fun enforcementOnApprovedResponseWorks() {
        enforced = true
        dispatch("https://response.example/common/credential", modernCallback = true)
        assertEquals(1, providerSelections)
        assertTrue(webView.isRegularAssertion())
        verify(span).setAttribute(AttributeName.passkey_endpoint_policy_enforced.name, true)
        verify(span).setAttribute(AttributeName.passkey_endpoint_policy_action.name, "ALLOW")
    }

    @Test
    fun observationOnEnforcementOffObservesOnModernCallback() {
        dispatch("https://unlisted.example/common/credential", PasskeyEndpointPolicy.DEFAULT, modernCallback = true)
        assertEquals(1, policySpans)
        assertEquals(1, providerSelections)
        assertTrue(webView.isRegularAssertion())
        verify(span).setAttribute(AttributeName.passkey_endpoint_policy_enforced.name, false)
        verify(span).setAttribute(AttributeName.passkey_endpoint_policy_action.name, "WOULD_REJECT")
        verify(span).setStatus(StatusCode.OK)
        verify(span).end()
    }

    @Test
    fun observationOffEnforcementOffSkipsPolicy() {
        observed = false
        dispatch("https://unlisted.example/common/credential", PasskeyEndpointPolicy.DEFAULT)
        assertEquals(0, policySelections)
        assertEquals(0, policySpans)
        assertEquals(1, providerSelections)
        assertTrue(webView.isRegularAssertion())
        verifyNoInteractions(span, callback)
    }

    @Test
    fun observationOffEnforcementOnStillSkipsPolicy() {
        observed = false
        enforced = true
        dispatch("https://unlisted.example/common/credential", PasskeyEndpointPolicy.DEFAULT, modernCallback = true)
        assertEquals(0, policySelections)
        assertEquals(0, policySpans)
        assertEquals(1, providerSelections)
        assertTrue(webView.isRegularAssertion())
        verifyNoInteractions(span, callback)
        verify(webView, never()).stopLoading()
    }

    @Test
    fun observationOffPreservesLegacyErrorResponse() {
        observed = false
        enforced = true
        dispatch("https://unlisted.example/common/credential", PasskeyEndpointPolicy.DEFAULT, version = "unsupported")
        assertEquals(0, policySelections)
        assertEquals(0, policySpans)
        assertEquals(1, providerSelections)
        assertTrue(webView.hasContext())
        assertFalse(webView.isRegularAssertion())
        verifyNoInteractions(span, callback)
    }

    @Test
    fun emptyListsShadowModePreservesResponse() {
        dispatch("https://response.example/common/credential", PasskeyEndpointPolicy.DEFAULT)
        assertTrue(webView.isRegularAssertion())
        verify(span).setAttribute(AttributeName.passkey_endpoint_policy_configured.name, false)
        verify(span).setAttribute(AttributeName.passkey_endpoint_policy_action.name, "WOULD_REJECT")
    }

    @Test
    fun emptyListsEnforcementRejects() {
        enforced = true
        dispatch("https://response.example/common/credential", PasskeyEndpointPolicy.DEFAULT)
        assertEquals(0, providerSelections)
        assertFalse(webView.urlLoaded)
        verify(span).setAttribute(AttributeName.passkey_endpoint_policy_action.name, "BLOCK")
    }

    @Test
    fun invalidChallengeShadowModePreservesErrorResponse() {
        dispatch("https://unlisted.example/common/credential", version = "unsupported")
        assertTrue(webView.urlLoaded)
        assertTrue(webView.hasContext())
        assertFalse(webView.isRegularAssertion())
        verify(span).setStatus(StatusCode.OK)
    }

    @Test
    fun invalidChallengeEnforcementCannotSendErrorToRejectedEndpoint() {
        enforced = true
        dispatch("https://unlisted.example/common/credential", version = "unsupported")
        assertEquals(0, providerSelections)
        assertFalse(webView.urlLoaded)
    }

    @Test
    fun providerFailureShadowModePreservesErrorResponse() {
        dispatch(
            "https://unlisted.example/common/credential",
            challengeValue = TestFidoManager.EXCEPTION_CHALLENGE
        )
        assertEquals(1, providerSelections)
        assertTrue(webView.hasContext())
        assertFalse(webView.isRegularAssertion())
        val headers = requireNotNull(webView.headers)
        verify(webView).loadUrl("https://unlisted.example/common/credential", headers)
        verify(span).setStatus(StatusCode.OK)
    }

    @Test
    fun enforcementUsesOriginalRequestNotCurrentWebViewUrl() {
        enforced = true
        doReturn("https://request.example/authorize").`when`(webView).getUrl()
        dispatch("https://response.example/common/credential", requestUrl = "https://unlisted.example/authorize")
        assertEquals(0, providerSelections)
        assertFalse(webView.urlLoaded)
        verify(webView).stopLoading()
        verify(span).setAttribute(AttributeName.passkey_incoming_endpoint_result.name, "HOST_NOT_ALLOWED")
        verify(span).setAttribute(AttributeName.passkey_submit_endpoint_result.name, "ALLOWED")
    }

    @Test
    fun policyTelemetryDoesNotContainRawProtocolData() {
        dispatch("https://unlisted.example/common/credential?value=private-query")
        mockingDetails(span).invocations.forEach { invocation ->
            invocation.arguments.filterIsInstance<String>().forEach { value ->
                listOf(
                    "request.example", "unlisted.example", "rp.example",
                    "sample-context", "sample-flow-token", "sample-challenge", "private-query"
                ).forEach { assertFalse(value.contains(it)) }
            }
        }
        assertEquals(1, policySpans)
    }

    @Test
    fun observationDefaultsOnAndEnforcementDefaultsOff() {
        assertEquals(true, CommonFlight.ENABLE_PASSKEY_ENDPOINT_POLICY_OBSERVATION.defaultValue)
        assertEquals(false, CommonFlight.ENFORCE_PASSKEY_ENDPOINT_POLICY.defaultValue)
        assertFalse(AttributeName.values().any { it.name == "passkey_endpoint_policy_is_main_frame" })
    }
}
