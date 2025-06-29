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
package com.microsoft.identity.common.internal.ui;

import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.window.java.layout.WindowInfoTrackerCallbackAdapter;
import androidx.window.layout.WindowInfoTracker;

import com.microsoft.identity.common.R;
import com.microsoft.identity.common.java.flighting.CommonFlightsManager;
import com.microsoft.identity.common.java.flighting.CommonFlight;
import com.microsoft.identity.common.logging.Logger;

/**
 * This activity is designed to handle dual-screen devices, such as the Surface Duo.
 * <p>
 * This activity readjusts its child layouts so that they're displayed on both single-screen and dual-screen device correctly.
 * It uses the WindowInfoTracker to listen for changes in the display features, such as folding features.
 * <p>
 * documentation:
 * <a href="https://developer.android.com/develop/ui/compose/layouts/adaptive/foldables/learn-about-foldables">Foldables</a>
 * <a href="https://learn.microsoft.com/en-us/previous-versions/dual-screen/android/surface-duo-dimensions">SurfaceDuo</a>
 * <a href="https://learn.microsoft.com/en-us/previous-versions/dual-screen/android/jetpack/window-manager/?tabs=views">Jetpack Window Manager</a>
 */
public class DualScreenActivity extends FragmentActivity {
    private static final String TAG = DualScreenActivity.class.getSimpleName();
    private WindowInfoTrackerCallbackAdapter mWindowInfoTrackerCallback;

    private FoldingFeatureInfo mLastFoldingFeature = null;

    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize WindowInfoTracker and register for display feature changes
        final WindowInfoTracker windowInfoTracker = WindowInfoTracker.getOrCreate(this);
        mWindowInfoTrackerCallback = new WindowInfoTrackerCallbackAdapter(windowInfoTracker);
        mWindowInfoTrackerCallback.addWindowLayoutInfoListener(
                this,
                getMainExecutor(),
                windowLayoutInfo -> {
                    // Create a new folding feature info from the windowLayoutInfo
                    final FoldingFeatureInfo foldingFeatureInfo = FoldingFeatureInfo
                            .Companion
                            .constructFromWindowLayoutInfo(windowLayoutInfo);
                    adjustLayout(foldingFeatureInfo);
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        final String methodTag = TAG + ":onDestroy";
        // Remove the window layout info listener to prevent memory leaks
        if (mWindowInfoTrackerCallback != null) {
            try {
                mWindowInfoTrackerCallback.removeWindowLayoutInfoListener(
                        layoutInfo -> {
                            // This is a no-op, but we need to provide a Consumer to remove the listener.
                        }
                );
                Logger.info(methodTag, "Window layout info listener removed.");
            } catch (final Exception e) {
                Logger.error(methodTag, "Failed to remove window layout info listener", e);
            }
        }
        Log.i(TAG, "DualScreenActivity destroyed.---------------------------------------------------------");
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Re-adjust the layout when the configuration changes, for example, when the device is rotated.
        // On devices running Android 8.1 (API level 27) and below, we rely on this method to adjust the layout.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            final String methodTag = TAG + ":onConfigurationChanged";
            Logger.info(methodTag, "Adjusting layout for dual screen activity due to configuration change.");
            //adjustLayoutForDualScreenActivity();
            Log.i(TAG, "PEDRO: Folding feature legacy");
            adjustLayout(null);
        }
        // For Android 9 (API 28) and above, the WindowInfoTracker handles layout adjustments
    }


    @Override
    public void setContentView(int layoutResID) {
        Log.w(TAG, "setContentView called with layoutResID: " + layoutResID);
        initializeContentView();

        final RelativeLayout contentLayout = findViewById(com.microsoft.identity.common.R.id.dual_screen_content);
        LayoutInflater.from(this).inflate(layoutResID, contentLayout);
    }

    private void initializeContentView() {
        super.setContentView(R.layout.dual_screen_layout);
        if (CommonFlightsManager.INSTANCE.getFlightsProvider().isFlightEnabled(CommonFlight.ENABLE_HANDLING_FOR_EDGE_TO_EDGE)) {
            try {
                ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (view, insets) -> {
                    int topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
                    int bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
                    int leftInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).left;
                    int rightInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).right;

                    view.setPadding(leftInset, topInset, rightInset, bottomInset);
                    return insets;
                });
            } catch (final Throwable throwable) {
                Logger.warn("DualScreenActivity:initializeContentView", "Failed to set OnApplyWindowInsetsListener");
            }
        }
        //adjustLayoutForDualScreenActivity();
        //adjustLayout(null);
        handleSingleScreenMode();
    }

    public void setFragment(@NonNull final Fragment fragment) {
        initializeContentView();
        getSupportFragmentManager()
                .beginTransaction()
                .setTransitionStyle(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .replace(R.id.dual_screen_content, fragment)
                .commit();
    }

    /**
     * private void adjustLayoutForDualScreenActivity() {
     * // Base layout for dual screen activity.
     * final ConstraintSet constraintSet = new ConstraintSet();
     * constraintSet.connect(R.id.dual_screen_content, ConstraintSet.LEFT, R.id.dual_screen_layout, ConstraintSet.LEFT, 0);
     * constraintSet.connect(R.id.dual_screen_content, ConstraintSet.RIGHT, R.id.dual_screen_layout, ConstraintSet.RIGHT, 0);
     * constraintSet.connect(R.id.dual_screen_content, ConstraintSet.TOP, R.id.dual_screen_layout, ConstraintSet.TOP, 0);
     * constraintSet.connect(R.id.dual_screen_content, ConstraintSet.BOTTOM, R.id.dual_screen_layout, ConstraintSet.BOTTOM, 0);
     * <p>
     * constraintSet.connect(R.id.dual_screen_empty_view, ConstraintSet.LEFT, R.id.dual_screen_layout, ConstraintSet.LEFT, 0);
     * constraintSet.connect(R.id.dual_screen_empty_view, ConstraintSet.RIGHT, R.id.dual_screen_layout, ConstraintSet.RIGHT, 0);
     * constraintSet.connect(R.id.dual_screen_empty_view, ConstraintSet.TOP, R.id.dual_screen_layout, ConstraintSet.TOP, 0);
     * constraintSet.connect(R.id.dual_screen_empty_view, ConstraintSet.BOTTOM, R.id.dual_screen_layout, ConstraintSet.BOTTOM, 0);
     * <p>
     * getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
     * <p>
     * if (isFoldingFeatureSeparating()) {
     * if (isVertical()) {
     * int hingeWidth = getFoldingFeatureRectBounds().width() / 2;
     * <p>
     * // WebView is on the right.
     * constraintSet.connect(R.id.dual_screen_content, ConstraintSet.LEFT, R.id.vertical_guideline, ConstraintSet.RIGHT, hingeWidth);
     * <p>
     * // Empty view is on the left.
     * constraintSet.connect(R.id.dual_screen_empty_view, ConstraintSet.RIGHT, R.id.vertical_guideline, ConstraintSet.LEFT, 0);
     * } else {
     * int hingeHeight = getFoldingFeatureRectBounds().height() / 2;
     * <p>
     * // WebView is on the top.
     * constraintSet.connect(R.id.dual_screen_content, ConstraintSet.BOTTOM, R.id.horizontal_guideline, ConstraintSet.TOP, hingeHeight);
     * <p>
     * // Empty view is in the bottom.
     * constraintSet.connect(R.id.dual_screen_empty_view, ConstraintSet.TOP, R.id.horizontal_guideline, ConstraintSet.BOTTOM, 0);
     * <p>
     * // In spanned vertical mode, keyboard will always be on the lower screen.
     * // This means we do not need to shrink the webview.
     * getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
     * }
     * } else {
     * // Shrink empty view. If constraint is not set, then its size will be (0,0).
     * constraintSet.clear(R.id.dual_screen_empty_view);
     * }
     * <p>
     * final ConstraintLayout dualScreenLayout = findViewById(R.id.dual_screen_layout);
     * constraintSet.applyTo(dualScreenLayout);
     * // Request layout to apply the changes
     * dualScreenLayout.post(dualScreenLayout::requestLayout);
     * if (mFoldingStateCallback != null) {
     * mFoldingStateCallback.onFoldingStateChanged(mFoldingFeature);
     * }
     * }
     */

    private void handleSingleScreenMode() {
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        final ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.connect(R.id.dual_screen_content, ConstraintSet.LEFT, R.id.dual_screen_layout, ConstraintSet.LEFT, 0);
        constraintSet.connect(R.id.dual_screen_content, ConstraintSet.RIGHT, R.id.dual_screen_layout, ConstraintSet.RIGHT, 0);
        constraintSet.connect(R.id.dual_screen_content, ConstraintSet.TOP, R.id.dual_screen_layout, ConstraintSet.TOP, 0);
        constraintSet.connect(R.id.dual_screen_content, ConstraintSet.BOTTOM, R.id.dual_screen_layout, ConstraintSet.BOTTOM, 0);

        final ConstraintLayout dualScreenLayout = findViewById(R.id.dual_screen_layout);
        constraintSet.applyTo(dualScreenLayout);
    }

    private synchronized void adjustLayout(final FoldingFeatureInfo foldingFeatureInfo) {
        // Get current time to check against debounce period
        Log.i(TAG, "=================  Current feature info: " + foldingFeatureInfo + " Previous: " + mLastFoldingFeature);
        final boolean isNewFeature = compareFoldingFeatures(foldingFeatureInfo, mLastFoldingFeature);
        mLastFoldingFeature = foldingFeatureInfo;
        if (isNewFeature) {
            Log.i(TAG, "No change in folding feature, skipping layout adjustment.");
            return; // No change in folding feature, skip layout adjustment
        }
        if (foldingFeatureInfo == null) {
            Log.i(TAG, "Defaulting to single screen mode.");
            handleSingleScreenMode();
        } else if (foldingFeatureInfo.isSeparating()) {
            Log.i(TAG, "Handle dual screen mode with separating folding feature.");
            handleDualScreenMode(foldingFeatureInfo);
        } else {
            Log.i(TAG, "Handle single screen mode.");
            handleSingleScreenMode();
        }
        mLastFoldingFeature = foldingFeatureInfo;
    }

    private boolean compareFoldingFeatures(final FoldingFeatureInfo newFeature, final FoldingFeatureInfo oldFeature) {
        if (newFeature == null && oldFeature == null) {
            return true; // Both are null, considered equal
        }
        if (newFeature == null || oldFeature == null) {
            return false; // One is null, the other is not
        }
        return newFeature.equals(oldFeature); // Compare properties of the features
    }

    private void handleDualScreenMode(final FoldingFeatureInfo foldingFeatureInfo) {
        final ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.connect(R.id.dual_screen_content, ConstraintSet.LEFT, R.id.dual_screen_layout, ConstraintSet.LEFT, 0);
        constraintSet.connect(R.id.dual_screen_content, ConstraintSet.RIGHT, R.id.dual_screen_layout, ConstraintSet.RIGHT, 0);
        constraintSet.connect(R.id.dual_screen_content, ConstraintSet.TOP, R.id.dual_screen_layout, ConstraintSet.TOP, 0);
        constraintSet.connect(R.id.dual_screen_content, ConstraintSet.BOTTOM, R.id.dual_screen_layout, ConstraintSet.BOTTOM, 0);

        constraintSet.connect(R.id.dual_screen_empty_view, ConstraintSet.LEFT, R.id.dual_screen_layout, ConstraintSet.LEFT, 0);
        constraintSet.connect(R.id.dual_screen_empty_view, ConstraintSet.RIGHT, R.id.dual_screen_layout, ConstraintSet.RIGHT, 0);
        constraintSet.connect(R.id.dual_screen_empty_view, ConstraintSet.TOP, R.id.dual_screen_layout, ConstraintSet.TOP, 0);
        constraintSet.connect(R.id.dual_screen_empty_view, ConstraintSet.BOTTOM, R.id.dual_screen_layout, ConstraintSet.BOTTOM, 0);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        if (foldingFeatureInfo.getOrientation() == FoldingFeatureInfo.Orientation.VERTICAL) {
            int hingeWidth = foldingFeatureInfo.getBounds().width() / 2;

            // WebView is on the right.
            constraintSet.connect(R.id.dual_screen_content, ConstraintSet.LEFT, R.id.vertical_guideline, ConstraintSet.RIGHT, hingeWidth);

            // Empty view is on the left.
            constraintSet.connect(R.id.dual_screen_empty_view, ConstraintSet.RIGHT, R.id.vertical_guideline, ConstraintSet.LEFT, 0);
        } else {
            int hingeHeight = foldingFeatureInfo.getBounds().height() / 2;

            // WebView is on the top.
            constraintSet.connect(R.id.dual_screen_content, ConstraintSet.BOTTOM, R.id.horizontal_guideline, ConstraintSet.TOP, hingeHeight);

            // Empty view is in the bottom.
            constraintSet.connect(R.id.dual_screen_empty_view, ConstraintSet.TOP, R.id.horizontal_guideline, ConstraintSet.BOTTOM, 0);

            // In spanned vertical mode, keyboard will always be on the lower screen.
            // This means we do not need to shrink the webview.
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        }
        final ConstraintLayout dualScreenLayout = findViewById(R.id.dual_screen_layout);

      //  final ConstraintLayout dualScreenLayout = findViewById(R.id.dual_screen_layout);
        constraintSet.applyTo(dualScreenLayout);
        //dualScreenLayout.requestLayout();
/*    dualScreenLayout.post(() -> {
        constraintSet.applyTo(dualScreenLayout);
        dualScreenLayout.requestLayout(); // Explicit request to re-layout

    });*/
        // Request layout to apply the changes
         //dualScreenLayout.post(dualScreenLayout::requestLayout);

    }


}
