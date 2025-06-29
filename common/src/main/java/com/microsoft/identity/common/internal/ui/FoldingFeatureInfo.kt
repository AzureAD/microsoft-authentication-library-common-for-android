package com.microsoft.identity.common.internal.ui

import android.graphics.Rect
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowLayoutInfo

/**
 * Represents information about a folding feature on a device.
 * This class encapsulates details about the folding feature's state, orientation, and bounds.
 *
 * @property isSeparating Indicates if the folding feature separates the screen into two distinct areas.
 * @property isFlat Indicates if the folding feature is flat (not in use).
 * @property isObstructing Indicates if the folding feature obstructs the view.
 * @property orientation The orientation of the folding feature (vertical or horizontal).
 * @property bounds The bounds of the folding feature.
 */
data class FoldingFeatureInfo(
    val isSeparating: Boolean,
    val isFlat: Boolean,
    val isObstructing: Boolean,
    val orientation: Orientation,
    val bounds: Rect
) {
    enum class Orientation {
        VERTICAL,
        HORIZONTAL
    }

    companion object {

        /**
         * Constructs a [FoldingFeatureInfo] from a [FoldingFeature].
         *
         * @param windowLayoutInfo The [WindowLayoutInfo] containing the folding feature information.
         * @return A new instance of [FoldingFeatureInfo].
         */
        fun constructFromWindowLayoutInfo(windowLayoutInfo: WindowLayoutInfo): FoldingFeatureInfo? {

            val foldingFeature = windowLayoutInfo.displayFeatures.firstOrNull()
            if (foldingFeature is FoldingFeature) {
                val orientation = when (foldingFeature.orientation) {
                    FoldingFeature.Orientation.VERTICAL -> Orientation.VERTICAL
                    FoldingFeature.Orientation.HORIZONTAL -> Orientation.HORIZONTAL
                    else -> throw IllegalArgumentException("Unknown folding feature orientation: ${foldingFeature.orientation}")
                }
                return FoldingFeatureInfo(
                    isSeparating = foldingFeature.isSeparating,
                    isFlat = foldingFeature.state == FoldingFeature.State.FLAT,
                    isObstructing = foldingFeature.occlusionType != FoldingFeature.OcclusionType.NONE,
                    orientation = orientation,
                    bounds = foldingFeature.bounds
                )
            } else {
                return null
            }

        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FoldingFeatureInfo) return false

        if (isSeparating != other.isSeparating) return false
        if (isFlat != other.isFlat) return false
        if (isObstructing != other.isObstructing) return false
        if (orientation != other.orientation) return false
        if (bounds != other.bounds) return false

        return true
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}
