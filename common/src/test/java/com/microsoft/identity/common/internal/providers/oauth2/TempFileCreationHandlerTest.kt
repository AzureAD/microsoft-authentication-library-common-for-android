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

import android.net.Uri
import android.os.Build
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

/**
 * Unit tests for [TempFileCreationHandler].
 */
@RunWith(RobolectricTestRunner::class)
class TempFileCreationHandlerTest {

    private lateinit var mockCameraLauncher: ActivityResultLauncher<Uri>
    private lateinit var mockFilePickerLauncher: ActivityResultLauncher<String>
    private lateinit var handler: TempFileCreationHandler
    private val context = RuntimeEnvironment.getApplication()

    @Before
    fun setUp() {
        mockCameraLauncher = mockk(relaxed = true)
        mockFilePickerLauncher = mockk(relaxed = true)

        val mockFragment = mockk<WebViewAuthorizationFragment> {
            every {
                registerForActivityResult(any<ActivityResultContracts.TakePicture>(), any())
            } returns mockCameraLauncher
            every {
                registerForActivityResult(any<ActivityResultContracts.GetContent>(), any())
            } returns mockFilePickerLauncher
        }
        handler = TempFileCreationHandler(mockFragment)
    }

    @Test
    fun handle_whenCaptureEnabled_launchesCameraCapture() {
        val fileChooserParams = mockk<WebChromeClient.FileChooserParams> {
            every { isCaptureEnabled } returns true
            every { acceptTypes } returns arrayOf("image/*")
        }
        val mockCallback = mockk<ValueCallback<Array<Uri>>>(relaxed = true)

        handler.handle(mockCallback, fileChooserParams, context)

        verify { mockCameraLauncher.launch(any()) }
        verify(exactly = 0) { mockFilePickerLauncher.launch(any()) }
    }

    @Test
    fun handle_whenCaptureNotEnabled_launchesFilePicker() {
        val fileChooserParams = mockk<WebChromeClient.FileChooserParams> {
            every { isCaptureEnabled } returns false
            every { acceptTypes } returns arrayOf("image/*")
        }
        val mockCallback = mockk<ValueCallback<Array<Uri>>>(relaxed = true)

        handler.handle(mockCallback, fileChooserParams, context)

        verify(exactly = 0) { mockCameraLauncher.launch(any()) }
        verify { mockFilePickerLauncher.launch(any()) }
    }

    @Test
    fun handle_withNoAcceptType_launchesFilePickerWithWildcard() {
        val fileChooserParams = mockk<WebChromeClient.FileChooserParams> {
            every { isCaptureEnabled } returns false
            every { acceptTypes } returns emptyArray()
        }
        val mockCallback = mockk<ValueCallback<Array<Uri>>>(relaxed = true)
        val mimeTypeSlot = slot<String>()

        every { mockFilePickerLauncher.launch(capture(mimeTypeSlot)) } returns Unit

        handler.handle(mockCallback, fileChooserParams, context)

        assertTrue(mimeTypeSlot.captured == "*/*")
    }

    @Test
    fun handle_withSpecificMimeType_launchesFilePickerWithThatType() {
        val expectedMimeType = "application/pdf"
        val fileChooserParams = mockk<WebChromeClient.FileChooserParams> {
            every { isCaptureEnabled } returns false
            every { acceptTypes } returns arrayOf(expectedMimeType)
        }
        val mockCallback = mockk<ValueCallback<Array<Uri>>>(relaxed = true)
        val mimeTypeSlot = slot<String>()

        every { mockFilePickerLauncher.launch(capture(mimeTypeSlot)) } returns Unit

        handler.handle(mockCallback, fileChooserParams, context)

        assertTrue(mimeTypeSlot.captured == expectedMimeType)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.M])
    fun getUriForFile_belowApi24_returnsFileUri() {
        val fileChooserParams = mockk<WebChromeClient.FileChooserParams> {
            every { isCaptureEnabled } returns true
            every { acceptTypes } returns arrayOf("image/*")
        }
        val mockCallback = mockk<ValueCallback<Array<Uri>>>(relaxed = true)
        val captureUriSlot = slot<Uri>()

        every { mockCameraLauncher.launch(capture(captureUriSlot)) } returns Unit

        handler.handle(mockCallback, fileChooserParams, context)

        assertNotNull(captureUriSlot.captured)
        assertTrue("Expected file:// URI on API < 24",
            captureUriSlot.captured.scheme == "file")

        // Clean up the temp file created by the handler
        val tempFilePath = captureUriSlot.captured.path
        if (tempFilePath != null) {
            File(tempFilePath).delete()
        }
    }

    @Test
    fun handle_cameraCapture_createsTempFileInCacheDir() {
        val fileChooserParams = mockk<WebChromeClient.FileChooserParams> {
            every { isCaptureEnabled } returns true
            every { acceptTypes } returns arrayOf("image/*")
        }
        val mockCallback = mockk<ValueCallback<Array<Uri>>>(relaxed = true)
        val cacheFiles = context.cacheDir.listFiles()?.toSet() ?: emptySet<File>()

        handler.handle(mockCallback, fileChooserParams, context)

        val newFiles = context.cacheDir.listFiles()?.toSet() ?: emptySet<File>()
        val createdFiles = newFiles - cacheFiles
        assertFalse("A temp file should have been created in the cache dir", createdFiles.isEmpty())
        createdFiles.forEach { it.delete() }
    }
}
