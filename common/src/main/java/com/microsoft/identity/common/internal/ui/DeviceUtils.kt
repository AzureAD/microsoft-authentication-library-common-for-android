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
package com.microsoft.identity.common.internal.ui

import android.app.Activity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

object DeviceUtils {
    /**
     * Get information about the folding feature if present
     *
     * @param activity The activity to check
     * @param callback Callback that will receive the folding feature or null if none exists
     */
    fun getFoldingFeatures(
        activity: Activity,
        callback: (FoldingFeature?) -> Unit
    ) {
        if (activity !is LifecycleOwner) {
            callback(null)
            return
        }

        val windowInfoTracker = WindowInfoTracker.getOrCreate(activity)
        val lifecycleOwner = activity as LifecycleOwner

        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                windowInfoTracker.windowLayoutInfo(activity)
                    .collect { layoutInfo ->
                        callback(
                            layoutInfo.displayFeatures
                                .filterIsInstance<FoldingFeature>()
                                .firstOrNull()
                        )
                    }
            }
        }
    }


    /**
     * Suspended version of getFoldingFeatures that returns folding feature information
     *
     * @param activity The activity to check
     * @return The folding feature or null if none exists
     */
    suspend fun getFoldingFeaturesSuspended(activity: Activity): FoldingFeature? {
        if (activity !is LifecycleOwner) {
            return null
        }

        val windowInfoTracker = WindowInfoTracker.getOrCreate(activity)

        // Get the first value from the flow
        val layoutInfo = windowInfoTracker.windowLayoutInfo(activity).firstOrNull() ?: return null

        return layoutInfo.displayFeatures
            .filterIsInstance<FoldingFeature>()
            .firstOrNull()
    }

    /**
     * Java-friendly wrapper for getFoldingFeaturesSuspended
     *
     * @param activity The activity to check
     * @param callback Callback that will receive the folding feature when complete
     */
    fun getFoldingFeaturesSuspendedForJava(
        activity: Activity,
        callback: (FoldingFeature?) -> Unit
    ) {
        if (activity !is LifecycleOwner) {
            callback(null)
            return
        }

        val lifecycleOwner = activity as LifecycleOwner
        lifecycleOwner.lifecycleScope.launch {
            val foldingFeature = getFoldingFeaturesSuspended(activity)
            callback(foldingFeature)
        }
    }


}
