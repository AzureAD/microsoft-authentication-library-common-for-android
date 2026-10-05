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
package com.microsoft.identity.common.internal.ui.webview

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import com.microsoft.identity.common.adal.internal.AuthenticationConstants
import com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME
import com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.INTUNE_APP_PACKAGE_NAME
import com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.IPPHONE_APP_PACKAGE_NAME
import com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.IPPHONE_APP_SHA512_RELEASE_SIGNATURE
import com.microsoft.identity.common.internal.broker.PackageHelper
import com.microsoft.identity.common.internal.ui.webview.DeviceCaUrlLaunchTelemetryProperties.AppLinkLaunchOutcome
import com.microsoft.identity.common.internal.ui.webview.DeviceCaUrlLaunchTelemetryProperties.DeviceCaUrlRoutingOutcome
import com.microsoft.identity.common.internal.ui.webview.DeviceCaUrlLaunchTelemetryProperties.DeviceManagementOwner
import com.microsoft.identity.common.java.flighting.CommonFlight
import com.microsoft.identity.common.java.opentelemetry.AttributeName
import com.microsoft.identity.common.java.opentelemetry.SpanExtension
import com.microsoft.identity.common.java.telemetry.OnboardingTelemetryConstants.STEP_COMPANY_PORTAL_LAUNCHED
import com.microsoft.identity.common.java.telemetry.OnboardingTelemetryConstants.STEP_MDM_ENROLLMENT_STARTED
import com.microsoft.identity.common.logging.Logger

/** Routes Device-CA requests to the management app, browser, or current WebView. */
internal class DeviceCaRequestRouter(private val host: Host) {
    internal sealed class RoutingResult {
        data object Completed : RoutingResult()
        data class Failed(val throwable: Throwable) : RoutingResult()
    }

    internal interface Host {
        fun activity(): Activity
        fun isRunningOnAuthService(): Boolean
        fun isFlightEnabled(flight: CommonFlight): Boolean
        fun launchCompanyPortal()
        fun isWebCpInWebViewEnabled(url: String): Boolean
        fun openLinkInBrowser(url: String)
        fun returnMdmFlow()
        fun recordOnboardingStep(stepId: String)
        fun markWebCpFlowStarted()
        fun loadUrlWithRequestHeaders(view: WebView, url: String)
    }

    fun route(view: WebView, url: String): RoutingResult {
        val methodTag = "$TAG:route"
        Logger.info(methodTag, "This is a device CA request.")
        host.recordOnboardingStep(STEP_MDM_ENROLLMENT_STARTED)

        if (shouldLaunchCompanyPortal()) {
            try {
                host.launchCompanyPortal()
                SpanExtension.current().setAttribute(
                    AttributeName.device_ca_management_owner.name,
                    DeviceManagementOwner.NOT_EVALUATED.telemetryValue,
                )
                recordRoutingOutcome(DeviceCaUrlRoutingOutcome.LEGACY_COMPANY_PORTAL_SUCCEEDED)
                return RoutingResult.Completed
            } catch (exception: Exception) {
                Logger.warn(methodTag, "Failed to launch Company Portal; continuing Device CA routing.")
                SpanExtension.current().recordException(exception)
            }
        }

        return routeAfterLegacyCompanyPortal(view, url)
    }

    private fun routeAfterLegacyCompanyPortal(view: WebView, url: String): RoutingResult {
        val methodTag = "$TAG:routeAfterLegacyCompanyPortal"
        val isWebCpInWebViewEnabled = host.isWebCpInWebViewEnabled(url)
        val isNativeHandoffEnabled = isNativeManagementAppHandoffEnabled()
        Logger.info(
            methodTag,
            "Device CA routing eligibility: WebCP in WebView=$isWebCpInWebViewEnabled, " +
                "native management-app handoff=$isNativeHandoffEnabled",
        )
        SpanExtension.current().setAttribute(
            AttributeName.is_webcp_in_webview_enabled.name,
            isWebCpInWebViewEnabled,
        )
        SpanExtension.current().setAttribute(
            AttributeName.is_native_device_ca_management_app_handoff_enabled.name,
            isNativeHandoffEnabled,
        )
        if (!isWebCpInWebViewEnabled || !isNativeHandoffEnabled) {
            SpanExtension.current().setAttribute(
                AttributeName.device_ca_management_owner.name,
                DeviceManagementOwner.NOT_EVALUATED.telemetryValue,
            )
            return loadInWebViewOrBrowser(url, view)
        }

        return routeWithNativeHandoff(view, url)
    }

    private fun routeWithNativeHandoff(view: WebView, url: String): RoutingResult {
        val methodTag = "$TAG:routeWithNativeHandoff"
        Logger.info(methodTag, "Checking for a supported management owner in the current Android user.")
        val managementAppPackage = resolveDeviceManagementAppPackage()
        if (managementAppPackage == null) {
            Logger.info(
                methodTag,
                "No supported management owner is visible in the current Android user. " +
                    "Continuing Device CA routing.",
            )
            return loadInWebViewOrBrowser(url, view)
        }

        Logger.info(
            methodTag,
            "Supported management owner found. Attempting targeted re-WPJ handoff to: " +
                managementAppPackage,
        )
        return try {
            launchReWpjManagementApp(managementAppPackage)
            Logger.info(methodTag, "Targeted re-WPJ handoff started. Stopping WebView and returning MDM_FLOW.")
            view.stopLoading()
            recordCompanyPortalLaunch(managementAppPackage)
            host.returnMdmFlow()
            recordRoutingOutcome(DeviceCaUrlRoutingOutcome.NATIVE_HANDOFF_SUCCEEDED)
            RoutingResult.Completed
        } catch (exception: RuntimeException) {
            if (exception !is ActivityNotFoundException && exception !is SecurityException) {
                throw exception
            }
            Logger.error(
                methodTag,
                "Failed to launch the device management app. Starting App Link fallback.",
                exception,
            )
            SpanExtension.current().recordException(exception)
            fallbackToAppLinkGenericHttpsOrWebView(view, url, managementAppPackage)
        }
    }

    fun resolveDeviceManagementAppPackage(): String? {
        val methodTag = "$TAG:resolveDeviceManagementAppPackage"
        val devicePolicyManager = host.activity().getSystemService(Activity.DEVICE_POLICY_SERVICE)
            as? DevicePolicyManager
        if (devicePolicyManager == null) {
            Logger.warn(methodTag, "DevicePolicyManager is unavailable. No management owner can be detected.")
            setManagementOwner(DeviceManagementOwner.DEVICE_POLICY_MANAGER_UNAVAILABLE)
            return null
        }

        if (devicePolicyManager.isProfileOwnerApp(COMPANY_PORTAL_APP_PACKAGE_NAME)) {
            Logger.info(methodTag, "Company Portal is the profile owner in the current Android user.")
            setManagementOwner(DeviceManagementOwner.COMPANY_PORTAL_PROFILE)
            return COMPANY_PORTAL_APP_PACKAGE_NAME
        }
        if (devicePolicyManager.isProfileOwnerApp(GOOGLE_DPC_PACKAGE_NAME)) {
            Logger.info(methodTag, "Google DPC is the profile owner in the current Android user.")
            setManagementOwner(DeviceManagementOwner.GOOGLE_DPC_PROFILE)
            return INTUNE_APP_PACKAGE_NAME
        }
        if (devicePolicyManager.isDeviceOwnerApp(GOOGLE_DPC_PACKAGE_NAME)) {
            Logger.info(methodTag, "Google DPC is the device owner.")
            setManagementOwner(DeviceManagementOwner.GOOGLE_DPC_DEVICE)
            return INTUNE_APP_PACKAGE_NAME
        }

        Logger.info(methodTag, "No supported profile owner or device owner was detected.")
        setManagementOwner(DeviceManagementOwner.NONE)
        return null
    }

    fun launchReWpjManagementApp(managementAppPackage: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(RE_WPJ_HANDOFF_URI))
            .setPackage(managementAppPackage)
        host.activity().startActivity(intent)
    }

    fun loadInWebViewOrBrowser(originalUrl: String, view: WebView): RoutingResult {
        val methodTag = "$TAG:loadInWebViewOrBrowser"
        if (host.isWebCpInWebViewEnabled(originalUrl)) {
            return loadInWebView(originalUrl, view)
        }

        return try {
            Logger.info(methodTag, "Loading device CA request in browser.")
            SpanExtension.current().setAttribute(AttributeName.is_webcp_in_webview_enabled.name, false)
            host.openLinkInBrowser(originalUrl)
            host.returnMdmFlow()
            recordRoutingOutcome(DeviceCaUrlRoutingOutcome.BROWSER_LAUNCH_SUCCEEDED)
            RoutingResult.Completed
        } catch (throwable: Throwable) {
            Logger.error(methodTag, "Failed to launch device CA URL in browser.", throwable)
            SpanExtension.current().recordException(throwable)
            recordRoutingOutcome(DeviceCaUrlRoutingOutcome.BROWSER_LAUNCH_FAILED)
            RoutingResult.Failed(throwable)
        }
    }

    private fun fallbackToAppLinkGenericHttpsOrWebView(
        view: WebView,
        originalUrl: String,
        managementAppPackage: String,
    ): RoutingResult {
        val httpsUrl = toHttpsUrl(originalUrl)
        if (tryLaunchAppLink(view, httpsUrl, managementAppPackage) ||
            tryLaunchGenericHttps(view, httpsUrl)
        ) {
            return RoutingResult.Completed
        }

        Logger.info("$TAG:fallbackToAppLinkGenericHttpsOrWebView", "Loading the HTTPS fallback in the MSAL WebView.")
        return loadInWebView(httpsUrl, view)
    }

    private fun tryLaunchAppLink(
        view: WebView,
        httpsUrl: String,
        managementAppPackage: String,
    ): Boolean {
        val methodTag = "$TAG:tryLaunchAppLink"
        val appLinkIntent = Intent(Intent.ACTION_VIEW, Uri.parse(httpsUrl))
            .setPackage(managementAppPackage)
        if (appLinkIntent.resolveActivity(host.activity().packageManager) == null) {
            Logger.warn(methodTag, "Management app cannot resolve the HTTPS App Link.")
            SpanExtension.current().setAttribute(
                AttributeName.device_ca_management_app_link_outcome.name,
                AppLinkLaunchOutcome.HANDLER_NOT_FOUND.telemetryValue,
            )
            return false
        }

        return try {
            host.activity().startActivity(appLinkIntent)
            SpanExtension.current().setAttribute(
                AttributeName.device_ca_management_app_link_outcome.name,
                AppLinkLaunchOutcome.LAUNCH_SUCCEEDED.telemetryValue,
            )
            view.stopLoading()
            recordCompanyPortalLaunch(managementAppPackage)
            host.returnMdmFlow()
            recordRoutingOutcome(DeviceCaUrlRoutingOutcome.NATIVE_FAILED_APP_LINK_SUCCEEDED)
            true
        } catch (exception: RuntimeException) {
            if (exception !is ActivityNotFoundException && exception !is SecurityException) {
                throw exception
            }
            Logger.error(methodTag, "Package-targeted App Link launch failed.", exception)
            SpanExtension.current().recordException(exception)
            SpanExtension.current().setAttribute(
                AttributeName.device_ca_management_app_link_outcome.name,
                AppLinkLaunchOutcome.LAUNCH_FAILED.telemetryValue,
            )
            false
        }
    }

    private fun tryLaunchGenericHttps(view: WebView, httpsUrl: String): Boolean {
        val methodTag = "$TAG:tryLaunchGenericHttps"
        val genericHttpsIntent = Intent(Intent.ACTION_VIEW, Uri.parse(httpsUrl))
        if (genericHttpsIntent.resolveActivity(host.activity().packageManager) == null) {
            Logger.warn(methodTag, "No external handler can resolve the generic HTTPS fallback.")
            return false
        }

        return try {
            host.activity().startActivity(genericHttpsIntent)
            view.stopLoading()
            host.returnMdmFlow()
            recordRoutingOutcome(DeviceCaUrlRoutingOutcome.NATIVE_APP_LINK_FAILED_GENERIC_HTTPS_SUCCEEDED)
            true
        } catch (exception: RuntimeException) {
            if (exception !is ActivityNotFoundException && exception !is SecurityException) {
                throw exception
            }
            Logger.error(methodTag, "Failed to launch the generic HTTPS handler.", exception)
            SpanExtension.current().recordException(exception)
            false
        }
    }

    private fun loadInWebView(originalUrl: String, view: WebView): RoutingResult {
        val methodTag = "$TAG:loadInWebView"
        return try {
            host.markWebCpFlowStarted()
            SpanExtension.current().setAttribute(AttributeName.is_webcp_in_webview_enabled.name, true)
            val httpsUrl = toHttpsUrl(originalUrl)
            val authorizeOnlyForwardingEnabled = host.isFlightEnabled(
                CommonFlight.ENABLE_DEVICE_CA_AUTHORIZE_ONLY_CREDENTIAL_FORWARDING,
            )
            SpanExtension.current().setAttribute(
                AttributeName.device_ca_authorize_only_forwarding_enabled.name,
                authorizeOnlyForwardingEnabled,
            )
            if (authorizeOnlyForwardingEnabled) {
                SpanExtension.current().setAttribute(AttributeName.device_ca_request_headers_skipped.name, true)
                view.loadUrl(httpsUrl)
            } else {
                host.loadUrlWithRequestHeaders(view, httpsUrl)
            }
            recordRoutingOutcome(DeviceCaUrlRoutingOutcome.WEBVIEW_LOAD_SUCCEEDED)
            RoutingResult.Completed
        } catch (throwable: Throwable) {
            Logger.error(methodTag, "Failed to load device CA URL in WebView.", throwable)
            SpanExtension.current().recordException(throwable)
            recordRoutingOutcome(DeviceCaUrlRoutingOutcome.WEBVIEW_LOAD_FAILED)
            RoutingResult.Failed(throwable)
        }
    }

    private fun isNativeManagementAppHandoffEnabled(): Boolean =
        host.isRunningOnAuthService() &&
            host.isFlightEnabled(
                CommonFlight.ENABLE_NATIVE_DEVICE_CA_MANAGEMENT_APP_HANDOFF,
            )

    private fun shouldLaunchCompanyPortal(): Boolean {
        val packageHelper = PackageHelper(host.activity().packageManager)
        return packageHelper.isPackageInstalledAndEnabled(IPPHONE_APP_PACKAGE_NAME) &&
            IPPHONE_APP_SHA512_RELEASE_SIGNATURE ==
            packageHelper.getSha512SignatureForPackage(IPPHONE_APP_PACKAGE_NAME) &&
            packageHelper.isPackageInstalledAndEnabled(COMPANY_PORTAL_APP_PACKAGE_NAME)
    }

    private fun recordCompanyPortalLaunch(managementAppPackage: String) {
        if (COMPANY_PORTAL_APP_PACKAGE_NAME == managementAppPackage) {
            host.recordOnboardingStep(STEP_COMPANY_PORTAL_LAUNCHED)
        }
    }

    private fun setManagementOwner(owner: DeviceManagementOwner) {
        SpanExtension.current().setAttribute(
            AttributeName.device_ca_management_owner.name,
            owner.telemetryValue,
        )
    }

    private fun recordRoutingOutcome(outcome: DeviceCaUrlRoutingOutcome) {
        Logger.info("$TAG:recordRoutingOutcome", "Device CA routing outcome: ${outcome.telemetryValue}")
        SpanExtension.current().setAttribute(
            AttributeName.device_ca_routing_outcome.name,
            outcome.telemetryValue,
        )
    }

    private fun toHttpsUrl(url: String): String =
        url.replace(AuthenticationConstants.Broker.BROWSER_EXT_PREFIX, HTTPS_URL_PREFIX)

    private companion object {
        private val TAG = DeviceCaRequestRouter::class.java.simpleName
        private const val GOOGLE_DPC_PACKAGE_NAME = "com.google.android.apps.work.clouddpc"
        private const val HTTPS_URL_PREFIX = "https://"
        private const val RE_WPJ_HANDOFF_URI = "intune-remediation://re-wpj"
    }
}