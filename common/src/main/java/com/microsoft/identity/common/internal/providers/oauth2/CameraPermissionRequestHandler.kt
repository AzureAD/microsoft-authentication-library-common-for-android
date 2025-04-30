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
import android.content.pm.PackageManager
import android.webkit.PermissionRequest
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.microsoft.identity.common.R
import com.microsoft.identity.common.internal.broker.SdmQrPinManager
import com.microsoft.identity.common.java.ui.PreferredAuthMethod
import com.microsoft.identity.common.logging.Logger

/**
 * Class responsible for handling camera permission requests.
 * Note: This class is compatible only with API level 21 and above;
 * functionality will not behave as expected on lower versions.
 */
class CameraPermissionRequestHandler(fragment: WebViewAuthorizationFragment) {

    private companion object {
        private const val TAG = "CameraPermissionRequestHandler"
        private const val MICROSOFT_CLOUD_URL: String = "https://login.microsoftonline.com/"
        private val cameraResource = arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE)
    }

    private val activityResultLauncher: ActivityResultLauncher<String> =
        fragment.registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            permissionGranted: Boolean ->
            Logger.info(TAG, "Camera permission granted: $permissionGranted")
            if (permissionGranted) {
                grant()
            } else {
                deny()
            }
        }

    private var currentPermissionRequest : PermissionRequest? = null

    /**
     * Keeps track of the camera permission request state.
     */
    private var isGranted = false

    /**
     * Handles the camera permission request.
     * <p>
     * If the request is valid, it will be handled by the qrPinHandler or defaultHandler.
     * If the request is not valid, it will be denied.
     *
     * @param request The permission request to handle.
     */
    fun handle(request: PermissionRequest, context: Context) {
        val  methodTag = "$TAG:handle"
        // Check if the request is valid.
        if (isValid(request)) {
            currentPermissionRequest = request
            isGranted = false
        } else {
            Logger.warn(methodTag, "Permission request is not valid, returning.")
            return
        }
        if(isQrPinRequest()) {
            qrPinHandler(context)
        } else {
            defaultHandler(context)
        }
    }

    /**
     * Call this method to grant the permission to access the camera resource.
     * The granted permission is only valid for the current WebView.
     */
    private fun grant() {
        currentPermissionRequest?.let {
            it.grant(cameraResource)
            isGranted = true
        }
    }

    /**
     * Call this method to deny the permission to access the camera resource.
     */
    private fun deny() {
        currentPermissionRequest?.let {
            it.deny()
            isGranted = false
        }
    }

    /**
     * Check if the given [PermissionRequest] is valid.
     * If valid return true.
     * If not valid, deny the request and return false.
     * <p>
     * A request is considered valid if:
     * - The request is for the camera resource only.
     * - The request is not a repeated request (i.e., not for the same origin and resources).
     * We can only grant or deny permissions for video capture/camera.
     * To avoid unintentionally granting requests for not defined permissions
     */
    private fun isValid(request: PermissionRequest): Boolean {
        val methodTag = "$TAG:isValid"
        if (!isForCamera(request)) {
            Logger.warn(methodTag, "Permission request is not for camera.")
            request.deny()
            return false
        }
        // There is a issue in ESTS UX where it sends multiple camera permission requests.
        // So, if there is already a camera permission request in progress we handle it here.
        if (isRepeatedRequest(request)) {
            Logger.info(methodTag, "Repeated request, permission is granted: $isGranted")
            if (isGranted) {
                request.grant(cameraResource)
            } else {
                request.deny()
            }
            return false
        } else {
            Logger.info(methodTag, "Valid new request.")
            return true
        }
    }

    /**
     * Check if the current permission request is a QR code pin request.
     * <p>
     * A request is considered a QR code pin request if:
     * - The origin of the request is the Microsoft cloud URL.
     * - The device is on SDM QR pin mode.
     *
     * @return true if the current permission request is a QR code pin request, false otherwise.
     */
    private fun isQrPinRequest() : Boolean {
        return currentPermissionRequest?.origin != null
                && MICROSOFT_CLOUD_URL.equals(currentPermissionRequest?.origin.toString(), ignoreCase = true)
                && PreferredAuthMethod.QR.value.equals(SdmQrPinManager.getPreferredAuthConfig())
    }

    /**
     * Handles the camera permission request for the app.
     * If the camera permission is already granted, it will grant the permission.
     * Otherwise, it will request the camera permission.
     */
    private fun defaultHandler(context: Context) {
        val methodTag = "$TAG:defaultHandler"
        if (appHasCameraPermission(context)) {
            Logger.info(methodTag, "App level camera permission already granted, silent grant.")
            grant()
        } else {
            requestCameraPermission()
        }
    }

    /**
     * Handles the camera permission request for QR code scanning.
     * If the camera permission is already granted and the camera consent suppress is enabled,
     * it will grant the permission. otherwise, it will explaining why the camera permission is required.
     * If the camera permission is not granted, it will request the camera permission.
     */
    private fun qrPinHandler(context: Context) {
        val  methodTag = "$TAG:handleQrPin"
        if (appHasCameraPermission(context)) {
            Logger.info(methodTag, "App level camera permission already granted.")
            if (SdmQrPinManager.isCameraConsentSuppressed()) {
                Logger.info(methodTag, "Camera consent suppress is enabled.")
                grant()
            } else {
                Logger.info(methodTag, "Camera consent suppress is not enabled.")
                showQrPinCameraRationale(context)
            }
        } else {
            requestCameraPermission()
        }
    }

    /**
     * Determines whatever if the camera permission has been granted.
     *
     * @return true if the camera permission has been granted, false otherwise.
     */
    private fun appHasCameraPermission(context: Context): Boolean {
        return (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED)
    }

    /**
     * Shows a dialog to the user explaining why the camera permission is required.
     * If the user accepts the dialog, the camera permission request will be launched.
     * If the user denies the dialog, the camera permission request will be denied.
     */
    private fun showQrPinCameraRationale(context: Context) {
        val methodTag = "$TAG:showQrPinCameraRationale"
        val builder = AlertDialog.Builder(context)
        builder.setMessage(R.string.qr_code_rationale_message)
            .setTitle(R.string.qr_code_rationale_header)
            .setCancelable(false)
            .setPositiveButton(R.string.qr_code_rationale_allow) { _, _ ->
                Logger.info(methodTag, "User accepted camera permission rationale.")
                requestCameraPermission()
            }
            .setNegativeButton(R.string.qr_code_rationale_block) { _, _ ->
                Logger.info(methodTag, "User denied camera permission rationale.")
                deny()
            }
        builder.show()
    }

    /**
     * Check if the given permission request is a repeated request.
     * <p>
     * Two requests are considered repeated if:
     * - They are for the same origin.
     * - They are for the same resources.
     *
     * @param permissionRequest The permission request to check.
     * @return true if the request is a repeated request, false otherwise.
     */
    private fun isRepeatedRequest(permissionRequest: PermissionRequest): Boolean {
        currentPermissionRequest.let {
            if (it == null) {
                return false
            }
            if (it.resources.size != permissionRequest.resources.size) {
                return false
            }
            if (it.origin != permissionRequest.origin) {
                return false
            }

        }
        return true
    }

    /**
     * Determines whatever if the given permission request is for the camera resource.
     *
     *
     * Note: This method is only available on API level 21 or higher.
     * Devices running on lower API levels will not be able to grant or deny camera permission requests.
     * getResources() method is only available on API level 21 or higher.
     *
     * @param request The permission request.
     * @return true if the given permission request is for camera, false otherwise.
     */
    private fun isForCamera(request: PermissionRequest): Boolean {
        return request.resources.size == 1 &&
                PermissionRequest.RESOURCE_VIDEO_CAPTURE == request.resources[0]
    }

    /**
     * Launches the camera permission request for the app.
     * Note: if the permission was already granted or denied,
     * the user will not be prompted and it will go directly to the callback.
     * Using the current state of the permission.
     *
     */
    private fun requestCameraPermission() {
        activityResultLauncher.launch(Manifest.permission.CAMERA)
    }
}
