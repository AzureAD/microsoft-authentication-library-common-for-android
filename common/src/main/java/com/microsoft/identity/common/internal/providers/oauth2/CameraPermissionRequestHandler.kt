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

import android.webkit.PermissionRequest
import com.microsoft.identity.common.logging.Logger

/**
 * Class responsible for handling camera permission requests.
 * Note: This class is compatible only with API level 21 and above;
 * functionality will not behave as expected on lower versions.
 */
class CameraPermissionRequestHandler {

    private var currentPermissionRequest : PermissionRequest? = null

    /**
     * Check if the permission was granted.
     *
     * @return true if the permission is granted.
     */
    private var isGranted = false

    /**
     * Call this method to grant the permission to access the camera resource.
     * The granted permission is only valid for the current WebView.
     *
     *
     * Note: This method is only available on API level 21 or higher.
     */
    fun grant() {
        currentPermissionRequest?.let {
            it.grant(cameraResource)
            isGranted = true
        }
    }

    /**
     * Call this method to deny the permission to access the camera resource.
     *
     *
     * Note: This method is only available on API level 21 or higher.
     */
    fun deny() {
        currentPermissionRequest?.let {
            it.deny()
            isGranted = false
        }
    }

    /**
     * Check if the given [PermissionRequest] is valid.
     * If valid, set the current permission request and return true.
     * If not valid, deny the request and return false.
     * <p>
     * A request is considered valid if:
     * - The request is for the camera resource only.
     * - The request is not a repeated request (i.e., not for the same origin and resources).
     *
     */
    fun setIfValid(request: PermissionRequest): Boolean {
        val methodTag = "$TAG:setIfValid"
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
            setCurrentPermissionRequest(request)
            return true
        }
    }

    /**
     * Set the current permission request.
     *
     * @param permissionRequest The permission request to set.
     */
    private fun setCurrentPermissionRequest(permissionRequest: PermissionRequest) {
        currentPermissionRequest = permissionRequest
        isGranted = false
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

    companion object {
        private const val TAG = "CameraPermissionRequestHandler"
        private val cameraResource = arrayOf(
            PermissionRequest.RESOURCE_VIDEO_CAPTURE
        )
    }
}
