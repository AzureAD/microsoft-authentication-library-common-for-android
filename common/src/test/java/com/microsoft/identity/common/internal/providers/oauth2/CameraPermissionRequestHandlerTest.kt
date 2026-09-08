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
package com.microsoft.identity.common.internal.providers.oauth2

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.net.Uri
import android.webkit.PermissionRequest
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.content.ContextCompat
import com.microsoft.identity.common.internal.broker.SdmQrPinManager
import com.microsoft.identity.common.java.opentelemetry.AttributeName
import com.microsoft.identity.common.java.opentelemetry.OTelUtility
import com.microsoft.identity.common.java.opentelemetry.SpanName
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.StatusCode
import org.junit.After
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class CameraPermissionRequestHandlerTest {

    private companion object {
        const val MICROSOFT_CLOUD_URL = "https://login.microsoftonline.com/"
        const val OTHER_URL = "https://example.com/"
        const val QRPIN = "qrpin"
    }

    private lateinit var fragment: WebViewAuthorizationFragment
    private lateinit var launcher: ActivityResultLauncher<String>
    private lateinit var span: Span
    private lateinit var callbackSlot: CapturingSlot<ActivityResultCallback<Boolean>>
    private lateinit var handler: CameraPermissionRequestHandler

    @Before
    fun setUp() {
        fragment = mockk(relaxed = true)
        launcher = mockk(relaxed = true)
        span = mockk(relaxed = true)
        callbackSlot = slot()

        // Capture the permission-result callback registered in the constructor so we can drive
        // the async grant/deny paths in tests.
        every {
            fragment.registerForActivityResult(
                any<ActivityResultContract<String, Boolean>>(),
                capture(callbackSlot)
            )
        } returns launcher

        // createSpan is @JvmStatic, so mockkObject won't intercept it - mockkStatic is required.
        mockkStatic(OTelUtility::class)
        every { OTelUtility.createSpan(SpanName.CameraPermissionRequest.name) } returns span

        mockkObject(SdmQrPinManager)
        // Default: not a QR+PIN config unless a test overrides it.
        every { SdmQrPinManager.getPreferredAuthConfig() } returns null
        every { SdmQrPinManager.isCameraConsentSuppressed() } returns false

        mockkStatic(ContextCompat::class)

        handler = CameraPermissionRequestHandler(fragment)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // region helpers

    private fun cameraRequest(origin: String = OTHER_URL): PermissionRequest {
        val request = mockk<PermissionRequest>(relaxed = true)
        every { request.resources } returns arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE)
        every { request.origin } returns Uri.parse(origin)
        return request
    }

    private fun contextWithCamera(hasHardware: Boolean = true): Context {
        val context = mockk<Context>(relaxed = true)
        val pm = mockk<PackageManager>(relaxed = true)
        every { context.packageManager } returns pm
        every { pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) } returns hasHardware
        return context
    }

    /**
     * A real Robolectric context, needed for flows that construct an [AlertDialog.Builder] -
     * its constructor resolves the dialog theme, which a relaxed mock context cannot supply.
     */
    private fun realContext(): Context = RuntimeEnvironment.getApplication()

    /**
     * Stubs [AlertDialog.Builder] so the rationale dialog can be exercised without touching the
     * library's string resources (which Robolectric doesn't merge into the test resource table).
     * The builder's chained setters return the real constructed instance so button listeners are
     * captured; [show] is a no-op.
     */
    private fun stubRationaleDialogBuilder(
        positiveSlot: CapturingSlot<DialogInterface.OnClickListener> = slot(),
        negativeSlot: CapturingSlot<DialogInterface.OnClickListener> = slot()
    ) {
        mockkConstructor(AlertDialog.Builder::class)
        every { anyConstructed<AlertDialog.Builder>().setMessage(any<Int>()) } answers { self as AlertDialog.Builder }
        every { anyConstructed<AlertDialog.Builder>().setTitle(any<Int>()) } answers { self as AlertDialog.Builder }
        every { anyConstructed<AlertDialog.Builder>().setCancelable(any()) } answers { self as AlertDialog.Builder }
        every {
            anyConstructed<AlertDialog.Builder>().setPositiveButton(any<Int>(), capture(positiveSlot))
        } answers { self as AlertDialog.Builder }
        every {
            anyConstructed<AlertDialog.Builder>().setNegativeButton(any<Int>(), capture(negativeSlot))
        } answers { self as AlertDialog.Builder }
        every { anyConstructed<AlertDialog.Builder>().show() } returns mockk(relaxed = true)
    }

    private fun grantAppCameraPermission(granted: Boolean) {
        every {
            ContextCompat.checkSelfPermission(any(), Manifest.permission.CAMERA)
        } returns if (granted) PackageManager.PERMISSION_GRANTED else PackageManager.PERMISSION_DENIED
    }

    private fun verifyFlow(flow: String) {
        verify { span.setAttribute(AttributeName.camera_permission_flow.name, flow) }
    }

    private fun verifySuccess(result: String) {
        verify { span.setAttribute(AttributeName.camera_permission_result.name, result) }
        verify { span.setStatus(StatusCode.OK) }
        verify { span.end() }
    }

    private fun verifyError(result: String) {
        verify { span.setAttribute(AttributeName.camera_permission_result.name, result) }
        verify { span.setStatus(StatusCode.ERROR) }
        verify { span.end() }
    }

    // endregion

    @Test
    fun handle_nonCameraRequest_deniesWithoutSpan() {
        val request = mockk<PermissionRequest>(relaxed = true)
        every { request.resources } returns arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE)

        handler.handle(request, contextWithCamera())

        verify { request.deny() }
        verify(exactly = 0) { OTelUtility.createSpan(any()) }
    }

    @Test
    fun handle_multiResourceRequest_isNotForCamera_denies() {
        val request = mockk<PermissionRequest>(relaxed = true)
        every { request.resources } returns arrayOf(
            PermissionRequest.RESOURCE_VIDEO_CAPTURE,
            PermissionRequest.RESOURCE_AUDIO_CAPTURE
        )

        handler.handle(request, contextWithCamera())

        verify { request.deny() }
        verify(exactly = 0) { OTelUtility.createSpan(any()) }
    }

    @Test
    fun handle_defaultFlow_appHasPermission_silentGrant() {
        grantAppCameraPermission(true)
        val request = cameraRequest()

        handler.handle(request, contextWithCamera())

        verify { span.setAttribute(AttributeName.has_camera_hardware.name, true) }
        verifyFlow("default_silent_grant")
        verify { request.grant(match { it.size == 1 && it[0] == PermissionRequest.RESOURCE_VIDEO_CAPTURE }) }
        verifySuccess("granted")
    }

    @Test
    fun handle_defaultFlow_noPermission_promptsThenGrantOnCallback() {
        grantAppCameraPermission(false)
        val request = cameraRequest()

        handler.handle(request, contextWithCamera())

        verifyFlow("default_os_prompt")
        verify { launcher.launch(Manifest.permission.CAMERA) }

        callbackSlot.captured.onActivityResult(true)

        verify { request.grant(any()) }
        verifySuccess("granted")
    }

    @Test
    fun handle_defaultFlow_noPermission_promptsThenDenyOnCallback() {
        grantAppCameraPermission(false)
        val request = cameraRequest()

        handler.handle(request, contextWithCamera())

        verifyFlow("default_os_prompt")
        callbackSlot.captured.onActivityResult(false)

        verify { request.deny() }
        verifySuccess("denied")
    }

    @Test
    fun handle_qrPinFlow_permissionGrantedAndConsentSuppressed_silentGrant() {
        grantAppCameraPermission(true)
        every { SdmQrPinManager.getPreferredAuthConfig() } returns QRPIN
        every { SdmQrPinManager.isCameraConsentSuppressed() } returns true
        val request = cameraRequest(MICROSOFT_CLOUD_URL)

        handler.handle(request, contextWithCamera())

        verifyFlow("qrpin_silent_grant")
        verify { request.grant(any()) }
        verifySuccess("granted")
    }

    @Test
    fun handle_qrPinFlow_permissionGrantedConsentNotSuppressed_showsRationaleAllow() {
        grantAppCameraPermission(true)
        every { SdmQrPinManager.getPreferredAuthConfig() } returns QRPIN
        every { SdmQrPinManager.isCameraConsentSuppressed() } returns false
        val positiveSlot = slot<DialogInterface.OnClickListener>()
        // Fully mock the dialog builder so the test doesn't depend on the library's string
        // resources being present in Robolectric's resource table (they aren't merged here).
        stubRationaleDialogBuilder(positiveSlot = positiveSlot)
        val request = cameraRequest(MICROSOFT_CLOUD_URL)

        handler.handle(request, realContext())

        verifyFlow("qrpin_rationale")
        verify(exactly = 0) { request.grant(any()) }
        verify(exactly = 0) { request.deny() }

        // Simulate the user tapping "Allow".
        positiveSlot.captured.onClick(mockk(relaxed = true), DialogInterface.BUTTON_POSITIVE)

        verify { launcher.launch(Manifest.permission.CAMERA) }
    }

    @Test
    fun handle_qrPinFlow_permissionGrantedConsentNotSuppressed_showsRationaleBlock() {
        grantAppCameraPermission(true)
        every { SdmQrPinManager.getPreferredAuthConfig() } returns QRPIN
        every { SdmQrPinManager.isCameraConsentSuppressed() } returns false
        val negativeSlot = slot<DialogInterface.OnClickListener>()
        stubRationaleDialogBuilder(negativeSlot = negativeSlot)
        val request = cameraRequest(MICROSOFT_CLOUD_URL)

        handler.handle(request, realContext())

        verifyFlow("qrpin_rationale")

        // Simulate the user tapping "Block".
        negativeSlot.captured.onClick(mockk(relaxed = true), DialogInterface.BUTTON_NEGATIVE)

        verify { request.deny() }
        verifySuccess("denied")
    }

    @Test
    fun handle_qrPinFlow_noPermission_promptsOs() {
        grantAppCameraPermission(false)
        every { SdmQrPinManager.getPreferredAuthConfig() } returns QRPIN
        val request = cameraRequest(MICROSOFT_CLOUD_URL)

        handler.handle(request, contextWithCamera())

        verifyFlow("qrpin_os_prompt")
        verify { launcher.launch(Manifest.permission.CAMERA) }
    }

    @Test
    fun handle_qrPinConfig_butNonMicrosoftOrigin_usesDefaultFlow() {
        grantAppCameraPermission(true)
        every { SdmQrPinManager.getPreferredAuthConfig() } returns QRPIN
        val request = cameraRequest(OTHER_URL)

        handler.handle(request, contextWithCamera())

        verifyFlow("default_silent_grant")
    }

    @Test
    fun handle_noCameraHardware_recordsAttributeFalse() {
        grantAppCameraPermission(true)
        val request = cameraRequest()

        handler.handle(request, contextWithCamera(hasHardware = false))

        verify { span.setAttribute(AttributeName.has_camera_hardware.name, false) }
    }

    @Test
    fun handle_repeatedRequest_afterGrant_grantsWithoutNewSpan() {
        grantAppCameraPermission(true)
        val request = cameraRequest()

        // First valid request -> silent grant sets isGranted = true.
        handler.handle(request, contextWithCamera())
        verify(exactly = 1) { OTelUtility.createSpan(SpanName.CameraPermissionRequest.name) }

        // Same request again is treated as a repeated request and granted directly.
        handler.handle(request, contextWithCamera())

        verify(exactly = 2) { request.grant(any()) }
        // No additional span is created for the repeated request.
        verify(exactly = 1) { OTelUtility.createSpan(SpanName.CameraPermissionRequest.name) }
    }

    @Test
    fun handle_repeatedRequest_whenNotGranted_denies() {
        grantAppCameraPermission(false)
        val request = cameraRequest()

        // First request goes to the OS prompt; isGranted stays false.
        handler.handle(request, contextWithCamera())
        // Repeated identical request while not granted -> denied.
        handler.handle(request, contextWithCamera())

        verify { request.deny() }
        verify(exactly = 1) { OTelUtility.createSpan(SpanName.CameraPermissionRequest.name) }
    }

    @Test
    fun handle_supersededInFlightRequest_endsPriorSpanWithError() {
        grantAppCameraPermission(false)
        val firstSpan = mockk<Span>(relaxed = true)
        val secondSpan = mockk<Span>(relaxed = true)
        every { OTelUtility.createSpan(SpanName.CameraPermissionRequest.name) } returnsMany listOf(firstSpan, secondSpan)

        // First request leaves a span in-flight (OS prompt, no callback yet).
        handler.handle(cameraRequest(OTHER_URL), contextWithCamera())
        // A new, different request supersedes the first.
        handler.handle(cameraRequest(MICROSOFT_CLOUD_URL), contextWithCamera())

        verify { firstSpan.setAttribute(AttributeName.camera_permission_result.name, "superseded") }
        verify { firstSpan.setStatus(StatusCode.ERROR) }
        verify { firstSpan.end() }
    }

    @Test
    fun handle_dispatchThrows_endsSpanWithErrorAndRethrows() {
        grantAppCameraPermission(true)
        val boom = RuntimeException("boom")
        // isQrPinRequest() is evaluated inside the try block; make it throw.
        every { SdmQrPinManager.getPreferredAuthConfig() } throws boom
        val request = cameraRequest(MICROSOFT_CLOUD_URL)

        try {
            handler.handle(request, contextWithCamera())
            fail("Expected exception to propagate")
        } catch (t: RuntimeException) {
            assertSame(boom, t)
        }

        verify { span.recordException(boom) }
        verifyError("error")
    }

    @Test
    fun cancel_inFlightRequest_endsSpanAsAbandoned() {
        grantAppCameraPermission(false)
        handler.handle(cameraRequest(), contextWithCamera())

        handler.cancel()

        verifyError("abandoned")
    }

    @Test
    fun cancel_completedRequest_doesNotEndSpanAgain() {
        grantAppCameraPermission(true)
        handler.handle(cameraRequest(), contextWithCamera())

        handler.cancel()

        verify(exactly = 1) { span.end() }
    }
}
