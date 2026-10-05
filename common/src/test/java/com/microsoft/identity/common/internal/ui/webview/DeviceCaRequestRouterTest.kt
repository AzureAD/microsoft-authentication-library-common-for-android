// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
package com.microsoft.identity.common.internal.ui.webview

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import android.net.Uri
import android.webkit.WebView
import com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME
import com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.INTUNE_APP_PACKAGE_NAME
import com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.IPPHONE_APP_PACKAGE_NAME
import com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.IPPHONE_APP_SHA512_RELEASE_SIGNATURE
import com.microsoft.identity.common.internal.broker.PackageHelper
import com.microsoft.identity.common.java.flighting.CommonFlight
import com.microsoft.identity.common.java.telemetry.OnboardingTelemetryConstants.STEP_MDM_ENROLLMENT_STARTED
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockConstruction
import org.mockito.Mockito.never
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
class DeviceCaRequestRouterTest {
    private lateinit var activity: Activity
    private lateinit var host: TestHost
    private lateinit var webView: WebView
    private lateinit var router: DeviceCaRequestRouter

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(Activity::class.java).get()
        host = TestHost(activity)
        webView = mock(WebView::class.java)
        router = spy(DeviceCaRequestRouter(host))
    }

    @Test
    fun route_nativeHandoffEnabled_launchesResolvedManagementApp() {
        host.webCpInWebViewEnabled = true
        host.runningOnAuthService = true
        host.enabledFlights += CommonFlight.ENABLE_NATIVE_DEVICE_CA_MANAGEMENT_APP_HANDOFF
        doReturn(INTUNE_APP_PACKAGE_NAME).`when`(router).resolveDeviceManagementAppPackage()
        doNothing().`when`(router).launchReWpjManagementApp(anyString())

        val result = router.route(webView, DEVICE_CA_URL)

        assertSame(DeviceCaRequestRouter.RoutingResult.Completed, result)
        verify(router).launchReWpjManagementApp(INTUNE_APP_PACKAGE_NAME)
        verify(webView).stopLoading()
        assertEquals(1, host.mdmFlowReturnCount)
        assertEquals(listOf(STEP_MDM_ENROLLMENT_STARTED), host.onboardingSteps)
    }

    @Test
    fun route_brokerless_skipsNativeHandoffAndLoadsWebView() {
        host.webCpInWebViewEnabled = true
        host.enabledFlights += CommonFlight.ENABLE_NATIVE_DEVICE_CA_MANAGEMENT_APP_HANDOFF

        val result = router.route(webView, DEVICE_CA_URL)

        assertSame(DeviceCaRequestRouter.RoutingResult.Completed, result)
        verify(router, never()).resolveDeviceManagementAppPackage()
        verify(router, never()).launchReWpjManagementApp(anyString())
        assertEquals(HTTPS_DEVICE_CA_URL, host.loadedUrl)
        assertEquals(1, host.webCpFlowStartedCount)
    }

    @Test
    fun route_webCpDisabled_skipsNativeHandoffAndOpensBrowser() {
        host.runningOnAuthService = true
        host.enabledFlights += CommonFlight.ENABLE_NATIVE_DEVICE_CA_MANAGEMENT_APP_HANDOFF

        val result = router.route(webView, DEVICE_CA_URL)

        assertSame(DeviceCaRequestRouter.RoutingResult.Completed, result)
        verify(router, never()).resolveDeviceManagementAppPackage()
        assertEquals(DEVICE_CA_URL, host.openedBrowserUrl)
        assertEquals(1, host.mdmFlowReturnCount)
    }

    @Test
    fun route_noManagementOwner_usesConfiguredWebViewFallback() {
        host.webCpInWebViewEnabled = true
        host.runningOnAuthService = true
        host.enabledFlights += CommonFlight.ENABLE_NATIVE_DEVICE_CA_MANAGEMENT_APP_HANDOFF
        doReturn(null).`when`(router).resolveDeviceManagementAppPackage()

        val result = router.route(webView, DEVICE_CA_URL)

        assertSame(DeviceCaRequestRouter.RoutingResult.Completed, result)
        verify(router, never()).launchReWpjManagementApp(anyString())
        assertEquals(HTTPS_DEVICE_CA_URL, host.loadedUrl)
    }

    @Test
    fun route_compatibleCompanyPortalLaunch_precedesRoutingGates() {
        host.webCpInWebViewEnabled = true
        host.runningOnAuthService = true
        host.enabledFlights += CommonFlight.ENABLE_NATIVE_DEVICE_CA_MANAGEMENT_APP_HANDOFF

        mockCompatibleCompanyPortal().use {
            val result = router.route(webView, DEVICE_CA_URL)

            assertSame(DeviceCaRequestRouter.RoutingResult.Completed, result)
        }

        assertEquals(1, host.companyPortalLaunchCount)
        verify(router, never()).resolveDeviceManagementAppPackage()
        verify(router, never()).launchReWpjManagementApp(anyString())
        assertNull(host.loadedUrl)
    }

    @Test
    fun route_companyPortalLaunchFailure_continuesNativeRouting() {
        host.webCpInWebViewEnabled = true
        host.runningOnAuthService = true
        host.enabledFlights += CommonFlight.ENABLE_NATIVE_DEVICE_CA_MANAGEMENT_APP_HANDOFF
        host.companyPortalFailure = ActivityNotFoundException("Company Portal unavailable")
        doReturn(null).`when`(router).resolveDeviceManagementAppPackage()

        mockCompatibleCompanyPortal().use {
            val result = router.route(webView, DEVICE_CA_URL)

            assertSame(DeviceCaRequestRouter.RoutingResult.Completed, result)
        }

        verify(router).resolveDeviceManagementAppPackage()
        assertEquals(HTTPS_DEVICE_CA_URL, host.loadedUrl)
    }

    @Test
    fun route_nativeLaunchNotFound_fallsBackToWebViewWhenNoExternalHandlerExists() {
        host.webCpInWebViewEnabled = true
        host.runningOnAuthService = true
        host.enabledFlights += CommonFlight.ENABLE_NATIVE_DEVICE_CA_MANAGEMENT_APP_HANDOFF
        doReturn(INTUNE_APP_PACKAGE_NAME).`when`(router).resolveDeviceManagementAppPackage()
        doThrow(ActivityNotFoundException()).`when`(router).launchReWpjManagementApp(anyString())

        val result = router.route(webView, DEVICE_CA_URL)

        assertSame(DeviceCaRequestRouter.RoutingResult.Completed, result)
        assertEquals(HTTPS_DEVICE_CA_URL, host.loadedUrl)
        assertEquals(1, host.webCpFlowStartedCount)
    }

    @Test
    fun route_nativeLaunchNotFound_launchesTargetedAppLinkFirst() {
        enableNativeHandoff()
        registerHandler(INTUNE_APP_PACKAGE_NAME, INTUNE_APP_PACKAGE_NAME + ".ReWpjActivity", true)
        doReturn(INTUNE_APP_PACKAGE_NAME).`when`(router).resolveDeviceManagementAppPackage()
        doThrow(ActivityNotFoundException()).`when`(router).launchReWpjManagementApp(anyString())

        val result = router.route(webView, DEVICE_CA_URL)

        assertSame(DeviceCaRequestRouter.RoutingResult.Completed, result)
        val intent = Shadows.shadowOf(activity).nextStartedActivity
        assertEquals(HTTPS_DEVICE_CA_URL, intent.dataString)
        assertEquals(INTUNE_APP_PACKAGE_NAME, intent.`package`)
        verify(webView).stopLoading()
        assertEquals(1, host.mdmFlowReturnCount)
    }

    @Test
    fun route_appLinkHandlerMissing_launchesGenericHttpsBeforeWebView() {
        enableNativeHandoff()
        registerHandler("com.contoso.browser", "com.contoso.browser.BrowserActivity", false)
        doReturn(INTUNE_APP_PACKAGE_NAME).`when`(router).resolveDeviceManagementAppPackage()
        doThrow(ActivityNotFoundException()).`when`(router).launchReWpjManagementApp(anyString())

        val result = router.route(webView, DEVICE_CA_URL)

        assertSame(DeviceCaRequestRouter.RoutingResult.Completed, result)
        val intent = Shadows.shadowOf(activity).nextStartedActivity
        assertEquals(HTTPS_DEVICE_CA_URL, intent.dataString)
        assertNull(intent.`package`)
        assertNull(host.loadedUrl)
        verify(webView).stopLoading()
    }

    @Test(expected = IllegalStateException::class)
    fun route_unexpectedNativeLaunchFailure_doesNotFallback() {
        host.webCpInWebViewEnabled = true
        host.runningOnAuthService = true
        host.enabledFlights += CommonFlight.ENABLE_NATIVE_DEVICE_CA_MANAGEMENT_APP_HANDOFF
        doReturn(INTUNE_APP_PACKAGE_NAME).`when`(router).resolveDeviceManagementAppPackage()
        doThrow(IllegalStateException("unexpected failure"))
            .`when`(router).launchReWpjManagementApp(anyString())

        router.route(webView, DEVICE_CA_URL)
    }

    @Test
    fun loadInWebViewOrBrowser_browserFailure_returnsTypedFailure() {
        val failure = ActivityNotFoundException("No browser")
        host.browserFailure = failure

        val result = router.loadInWebViewOrBrowser(DEVICE_CA_URL, webView)

        assertTrue(result is DeviceCaRequestRouter.RoutingResult.Failed)
        assertSame(failure, (result as DeviceCaRequestRouter.RoutingResult.Failed).throwable)
    }

    @Test
    fun loadInWebViewOrBrowser_webViewFailure_returnsTypedFailure() {
        val failure = IllegalStateException("WebView load failed")
        host.webCpInWebViewEnabled = true
        host.webViewFailure = failure

        val result = router.loadInWebViewOrBrowser(DEVICE_CA_URL, webView)

        assertTrue(result is DeviceCaRequestRouter.RoutingResult.Failed)
        assertSame(failure, (result as DeviceCaRequestRouter.RoutingResult.Failed).throwable)
    }

    @Test
    fun loadInWebViewOrBrowser_authorizeOnly_loadsWithoutRequestHeaders() {
        host.webCpInWebViewEnabled = true
        host.enabledFlights += CommonFlight.ENABLE_DEVICE_CA_AUTHORIZE_ONLY_CREDENTIAL_FORWARDING

        val result = router.loadInWebViewOrBrowser(DEVICE_CA_URL, webView)

        assertSame(DeviceCaRequestRouter.RoutingResult.Completed, result)
        verify(webView).loadUrl(HTTPS_DEVICE_CA_URL)
        assertNull(host.loadedUrl)
    }

    @Test
    fun resolveDeviceManagementAppPackage_companyPortalProfileOwner() {
        val devicePolicyManager = mock(DevicePolicyManager::class.java)
        val mockActivity = mock(Activity::class.java)
        doReturn(devicePolicyManager).`when`(mockActivity)
            .getSystemService(Activity.DEVICE_POLICY_SERVICE)
        doReturn(true).`when`(devicePolicyManager)
            .isProfileOwnerApp(COMPANY_PORTAL_APP_PACKAGE_NAME)

        val result = DeviceCaRequestRouter(TestHost(mockActivity))
            .resolveDeviceManagementAppPackage()

        assertEquals(COMPANY_PORTAL_APP_PACKAGE_NAME, result)
    }

    @Test
    fun resolveDeviceManagementAppPackage_googleProfileOwner_targetsIntune() {
        val devicePolicyManager = mock(DevicePolicyManager::class.java)
        val mockActivity = mock(Activity::class.java)
        doReturn(devicePolicyManager).`when`(mockActivity)
            .getSystemService(Activity.DEVICE_POLICY_SERVICE)
        doReturn(true).`when`(devicePolicyManager)
            .isProfileOwnerApp(GOOGLE_DPC_PACKAGE_NAME)

        val result = DeviceCaRequestRouter(TestHost(mockActivity))
            .resolveDeviceManagementAppPackage()

        assertEquals(INTUNE_APP_PACKAGE_NAME, result)
    }

    @Test
    fun resolveDeviceManagementAppPackage_noPolicyManager_returnsNull() {
        val mockActivity = mock(Activity::class.java)

        val result = DeviceCaRequestRouter(TestHost(mockActivity))
            .resolveDeviceManagementAppPackage()

        assertNull(result)
    }

    @Test
    fun launchReWpjManagementApp_targetsRequestedPackage() {
        router.launchReWpjManagementApp(INTUNE_APP_PACKAGE_NAME)

        val intent = Shadows.shadowOf(activity).nextStartedActivity
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals(RE_WPJ_HANDOFF_URI, intent.dataString)
        assertEquals(INTUNE_APP_PACKAGE_NAME, intent.`package`)
    }

    private fun enableNativeHandoff() {
        host.webCpInWebViewEnabled = true
        host.runningOnAuthService = true
        host.enabledFlights += CommonFlight.ENABLE_NATIVE_DEVICE_CA_MANAGEMENT_APP_HANDOFF
    }

    private fun mockCompatibleCompanyPortal() =
        mockConstruction(PackageHelper::class.java) { packageHelper, _ ->
            `when`(packageHelper.isPackageInstalledAndEnabled(IPPHONE_APP_PACKAGE_NAME))
                .thenReturn(true)
            `when`(packageHelper.getSha512SignatureForPackage(IPPHONE_APP_PACKAGE_NAME))
                .thenReturn(IPPHONE_APP_SHA512_RELEASE_SIGNATURE)
            `when`(packageHelper.isPackageInstalledAndEnabled(COMPANY_PORTAL_APP_PACKAGE_NAME))
                .thenReturn(true)
        }

    private fun registerHandler(packageName: String, activityClass: String, targeted: Boolean) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(HTTPS_DEVICE_CA_URL))
        if (targeted) {
            intent.setPackage(packageName)
        }
        val resolveInfo = ResolveInfo().apply {
            activityInfo = ActivityInfo().apply {
                this.packageName = packageName
                name = activityClass
            }
        }
        Shadows.shadowOf(activity.packageManager).addResolveInfoForIntent(intent, resolveInfo)
    }

    private class TestHost(
        private val activity: Activity,
    ) : DeviceCaRequestRouter.Host {
        var runningOnAuthService = false
        var webCpInWebViewEnabled = false
        val enabledFlights = mutableSetOf<CommonFlight>()
        val onboardingSteps = mutableListOf<String>()
        var openedBrowserUrl: String? = null
        var loadedUrl: String? = null
        var browserFailure: Throwable? = null
        var webViewFailure: Throwable? = null
        var companyPortalFailure: Throwable? = null
        var companyPortalLaunchCount = 0
        var mdmFlowReturnCount = 0
        var webCpFlowStartedCount = 0

        override fun activity(): Activity = activity

        override fun isRunningOnAuthService(): Boolean = runningOnAuthService

        override fun isFlightEnabled(flight: CommonFlight): Boolean = flight in enabledFlights

        override fun launchCompanyPortal() {
            companyPortalFailure?.let { throw it }
            companyPortalLaunchCount++
        }

        override fun isWebCpInWebViewEnabled(url: String): Boolean = webCpInWebViewEnabled

        override fun openLinkInBrowser(url: String) {
            browserFailure?.let { throw it }
            openedBrowserUrl = url
        }

        override fun returnMdmFlow() {
            mdmFlowReturnCount++
        }

        override fun recordOnboardingStep(stepId: String) {
            onboardingSteps += stepId
        }

        override fun markWebCpFlowStarted() {
            webCpFlowStartedCount++
        }

        override fun loadUrlWithRequestHeaders(view: WebView, url: String) {
            webViewFailure?.let { throw it }
            loadedUrl = url
        }
    }

    private companion object {
        const val DEVICE_CA_URL = "browser://abcxyz/xyz?ismdmurl=1"
        const val HTTPS_DEVICE_CA_URL = "https://abcxyz/xyz?ismdmurl=1"
        const val RE_WPJ_HANDOFF_URI = "intune-remediation://re-wpj"
        const val GOOGLE_DPC_PACKAGE_NAME = "com.google.android.apps.work.clouddpc"
    }
}