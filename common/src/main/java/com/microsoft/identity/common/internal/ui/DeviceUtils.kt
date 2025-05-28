package com.microsoft.identity.common.internal.ui

import android.app.Activity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import androidx.window.layout.WindowLayoutInfo
import kotlinx.coroutines.launch

object DeviceUtils {

    /**
     * Detects if the device has dual screens (like Surface Duo)
     *
     * @param activity The activity to check
     * @param callback Callback that will receive true if device has dual screens, false otherwise
     */
    fun isDualScreenDevice(
        activity: Activity,
        callback: (Boolean) -> Unit
    ) {
        if (activity !is LifecycleOwner) {
            callback(false)
            return
        }

        val windowInfoTracker = WindowInfoTracker.getOrCreate(activity)
        val lifecycleOwner = activity as LifecycleOwner

        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                windowInfoTracker.windowLayoutInfo(activity)
                    .collect { layoutInfo ->
                        // Check if any display feature is a folding feature (hinge or fold)
                        val isDualScreen = layoutInfo.displayFeatures.any {
                            it is FoldingFeature &&
                                    (it.state == FoldingFeature.State.HALF_OPENED ||
                                            it.state == FoldingFeature.State.FLAT)
                        }
                        callback(isDualScreen)
                    }
            }
        }
    }

    /**
     * Get information about the folding feature if present
     *
     * @param activity The activity to check
     * @param callback Callback that will receive the folding feature or null if none exists
     */
    fun getFoldingFeature(
        activity: Activity,
        callback: (WindowLayoutInfo?) -> Unit
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
                        callback(layoutInfo)
                    }
            }
        }
    }

    fun isLDualScreenDevice(layoutInfo: WindowLayoutInfo): Boolean {
        return layoutInfo.displayFeatures.any {
            it is FoldingFeature &&
                    (it.state == FoldingFeature.State.HALF_OPENED || it.state == FoldingFeature.State.FLAT)
        }
    }

    fun getFoldingFeature(layoutInfo: WindowLayoutInfo): FoldingFeature? {
        return layoutInfo.displayFeatures
            .filterIsInstance<FoldingFeature>()
            .firstOrNull()
    }


}