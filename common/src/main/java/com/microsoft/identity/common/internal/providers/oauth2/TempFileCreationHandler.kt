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

import android.content.Context
import android.net.Uri
import android.os.Build
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.MainThread
import androidx.core.content.FileProvider
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.util.FileUtil
import com.microsoft.identity.common.logging.Logger
import java.io.File

/**
 * Handler for creating and managing temporary files required during WebView file chooser interactions.
 *
 * This class handles the [WebChromeClient.onShowFileChooser] callback, enabling file input elements
 * (such as image uploads) within WebView-based authentication pages. When a capture is needed,
 * it creates a temporary file in the app's private cache directory and provides a content URI
 * for use by camera or file-picker Intents.
 *
 * Temporary files are stored in the app's private cache directory and are cleaned up automatically
 * after the file chooser flow completes.
 *
 * Note: This class is compatible only with API level 21 and above.
 */
class TempFileCreationHandler(fragment: WebViewAuthorizationFragment) {

    private companion object {
        private const val TAG = "TempFileCreationHandler"

        /**
         * Default prefix applied to temporary file names created by this handler.
         */
        private const val TEMP_FILE_PREFIX = "ms_auth_"

        /**
         * Default suffix (extension) applied to temporary image files created by this handler.
         */
        private const val TEMP_FILE_SUFFIX = ".jpg"

        /**
         * The FileProvider authority suffix used to generate content URIs for temporary files on API 24+.
         * The full authority is `{applicationId}.ms_auth_provider`.
         */
        const val FILE_PROVIDER_AUTHORITY_SUFFIX = ".ms_auth_provider"
    }

    /**
     * Pending callback to deliver selected file URIs back to the WebView.
     * Accessed only on the main thread (via [handle] which is called from [android.app.Activity.runOnUiThread]
     * and activity result callbacks which are always delivered on the main thread).
     */
    private var pendingFilePathCallback: ValueCallback<Array<Uri>>? = null

    /**
     * Temporary file created for camera capture, to be cleaned up after the flow completes.
     * Accessed only on the main thread (see [pendingFilePathCallback]).
     */
    private var currentTempFile: File? = null

    /**
     * Launcher for capturing an image with the device camera.
     * Returns `true` if the image was saved successfully to the output URI.
     */
    private val cameraCaptureLauncher: ActivityResultLauncher<Uri> =
        fragment.registerForActivityResult(ActivityResultContracts.TakePicture()) { imageSaved ->
            val methodTag = "$TAG:cameraCaptureLauncher"
            if (imageSaved) {
                val tempFile = currentTempFile
                if (tempFile != null) {
                    Logger.info(methodTag, "Camera capture succeeded.")
                    deliverResult(arrayOf(getUriForFile(fragment.requireContext(), tempFile)))
                } else {
                    Logger.warn(methodTag, "Camera capture succeeded but temp file reference is null.")
                    cancelPendingCallback()
                }
            } else {
                Logger.info(methodTag, "Camera capture cancelled or failed.")
                cancelPendingCallback()
            }
            cleanupTempFile()
        }

    /**
     * Launcher for picking a file from the device storage.
     * Returns the URI of the selected file, or `null` if cancelled.
     */
    private val filePickerLauncher: ActivityResultLauncher<String> =
        fragment.registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            val methodTag = "$TAG:filePickerLauncher"
            if (uri != null) {
                Logger.info(methodTag, "File picker selection received.")
                deliverResult(arrayOf(uri))
            } else {
                Logger.info(methodTag, "File picker cancelled.")
                cancelPendingCallback()
            }
        }

    /**
     * Handles a file chooser request from the WebView.
     *
     * Determines whether to launch a camera capture or a generic file picker based on the
     * [fileChooserParams]. For capture mode, a temporary file is created and the camera
     * Intent is launched. Otherwise, a file picker is launched.
     *
     * Must be called on the main thread. Activity result callbacks are also delivered on
     * the main thread, so all accesses to [pendingFilePathCallback] and [currentTempFile]
     * are single-threaded.
     *
     * @param filePathCallback The callback to invoke with the selected file URI(s) once the
     *                         user has completed the selection.
     * @param fileChooserParams Parameters describing the file chooser request from the WebView.
     * @param context The [Context] used to access the cache directory and [FileProvider].
     */
    @MainThread
    fun handle(
        filePathCallback: ValueCallback<Array<Uri>>,
        fileChooserParams: WebChromeClient.FileChooserParams,
        context: Context
    ) {
        val methodTag = "$TAG:handle"
        pendingFilePathCallback = filePathCallback

        if (shouldUseCameraCapture(fileChooserParams)) {
            Logger.info(methodTag, "Launching camera for file chooser.")
            launchCameraCapture(context)
        } else {
            Logger.info(methodTag, "Launching file picker for file chooser.")
            launchFilePicker(fileChooserParams)
        }
    }

    /**
     * Determines whether to use camera capture based on the [fileChooserParams].
     *
     * Returns `true` only if the `capture` attribute is set on the HTML file input element,
     * indicating that the page explicitly requests camera capture.
     */
    private fun shouldUseCameraCapture(fileChooserParams: WebChromeClient.FileChooserParams): Boolean {
        return fileChooserParams.isCaptureEnabled
    }

    /**
     * Creates a temporary file and launches the camera to capture an image into it.
     *
     * If the temporary file cannot be created, the pending callback is cancelled.
     */
    private fun launchCameraCapture(context: Context) {
        val methodTag = "$TAG:launchCameraCapture"
        try {
            val tempFile = FileUtil.createTempFile(TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX, context.cacheDir)
            currentTempFile = tempFile
            val captureUri = getUriForFile(context, tempFile)
            cameraCaptureLauncher.launch(captureUri)
        } catch (e: ClientException) {
            Logger.error(methodTag, "Failed to create temp file for camera capture.", e)
            cancelPendingCallback()
        }
    }

    /**
     * Launches a generic file picker using the first accepted MIME type from [fileChooserParams],
     * defaulting to `*/*` if no accept type is specified.
     */
    private fun launchFilePicker(fileChooserParams: WebChromeClient.FileChooserParams) {
        val acceptTypes = fileChooserParams.acceptTypes
        val mimeType = if (!acceptTypes.isNullOrEmpty() && acceptTypes[0].isNotBlank()) {
            acceptTypes[0]
        } else {
            "*/*"
        }
        filePickerLauncher.launch(mimeType)
    }

    /**
     * Invokes the pending [pendingFilePathCallback] with the given [uris] and clears it.
     */
    private fun deliverResult(uris: Array<Uri>) {
        pendingFilePathCallback?.onReceiveValue(uris)
        pendingFilePathCallback = null
    }

    /**
     * Cancels the pending [pendingFilePathCallback] by invoking it with `null`, then clears it.
     */
    private fun cancelPendingCallback() {
        pendingFilePathCallback?.onReceiveValue(null)
        pendingFilePathCallback = null
    }

    /**
     * Deletes [currentTempFile] on a best-effort basis and clears the reference.
     */
    private fun cleanupTempFile() {
        currentTempFile?.let { FileUtil.deleteFile(it) }
        currentTempFile = null
    }

    /**
     * Returns a [Uri] suitable for passing to a camera Intent.
     *
     * On API 24+ a content URI is produced via [FileProvider] using the authority
     * `{packageName}[FILE_PROVIDER_AUTHORITY_SUFFIX]`. On earlier APIs a `file://` URI is used.
     */
    private fun getUriForFile(context: Context, file: File): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val authority = context.packageName + FILE_PROVIDER_AUTHORITY_SUFFIX
            FileProvider.getUriForFile(context, authority, file)
        } else {
            Uri.fromFile(file)
        }
    }
}
