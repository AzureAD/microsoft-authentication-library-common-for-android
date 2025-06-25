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

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.window.java.layout.WindowInfoTrackerCallbackAdapter;
import androidx.window.layout.DisplayFeature;
import androidx.window.layout.FoldingFeature;
import androidx.window.layout.WindowInfoTracker;

import com.microsoft.identity.common.R;
import com.microsoft.identity.common.java.flighting.CommonFlightsManager;
import com.microsoft.identity.common.java.flighting.CommonFlight;
import com.microsoft.identity.common.logging.Logger;

// This activity readjusts its child layouts so that they're displayed on both single-screen and dual-screen device correctly.
// https://learn.microsoft.com/en-us/previous-versions/dual-screen/android/surface-duo-dimensions
public class DualScreenActivity extends FragmentActivity {

    private FoldingFeature mFoldingFeature;

    @Override
    protected void onStart() {
        super.onStart();
        // Initialize WindowInfoTracker and register for display feature changes
        final WindowInfoTracker windowInfoTracker = WindowInfoTracker.getOrCreate(this);
        final WindowInfoTrackerCallbackAdapter windowInfoTrackerCallback =
                new WindowInfoTrackerCallbackAdapter(windowInfoTracker);

        // The Surface Duo, upon its original release, defaulted to Android 10.
        // The Surface Duo 2, upon its original release, defaulted to Android 11.
        // Hence, windowLayoutInfo will be available for these devices.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            windowInfoTrackerCallback.addWindowLayoutInfoListener(
                    this,
                    getMainExecutor(),
                    windowLayoutInfo -> {
                        mFoldingFeature = null; // Reset the folding feature before checking
                        for (final DisplayFeature displayFeature : windowLayoutInfo.getDisplayFeatures()) {
                            if (displayFeature instanceof FoldingFeature) {
                                mFoldingFeature = (FoldingFeature) displayFeature;
                            }
                        }
                        adjustLayoutForDualScreenActivity();
                    });
        }
    }

    @Override
    public void setContentView(int layoutResID) {
        initializeContentView();

        final RelativeLayout contentLayout = findViewById(com.microsoft.identity.common.R.id.dual_screen_content);
        LayoutInflater.from(this).inflate(layoutResID, contentLayout);
    }

    private void initializeContentView(){
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
        adjustLayoutForDualScreenActivity();
    }

    public void setFragment(@NonNull final Fragment fragment) {
        initializeContentView();
        getSupportFragmentManager()
                .beginTransaction()
                .setTransitionStyle(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .replace(R.id.dual_screen_content, fragment)
                .commit();
    }

    private void adjustLayoutForDualScreenActivity() {
        boolean isAppSpanned = isAppSpanned(this);

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

        if (isAppSpanned && mFoldingFeature != null) {
            if (mFoldingFeature.getOrientation() == FoldingFeature.Orientation.VERTICAL) {
                int hingeWidth = mFoldingFeature.getBounds().width() / 2;

                // WebView is on the right.
                constraintSet.connect(R.id.dual_screen_content, ConstraintSet.LEFT, R.id.vertical_guideline, ConstraintSet.RIGHT, hingeWidth);

                // Empty view is on the left.
                constraintSet.connect(R.id.dual_screen_empty_view, ConstraintSet.RIGHT, R.id.vertical_guideline, ConstraintSet.LEFT, 0);
            } else {
                int hingeHeight = mFoldingFeature.getBounds().height() / 2;

                // WebView is on the top.
                constraintSet.connect(R.id.dual_screen_content, ConstraintSet.BOTTOM, R.id.horizontal_guideline, ConstraintSet.TOP, hingeHeight);

                // Empty view is in the bottom.
                constraintSet.connect(R.id.dual_screen_empty_view, ConstraintSet.TOP, R.id.horizontal_guideline, ConstraintSet.BOTTOM, 0);

                // In spanned vertical mode, keyboard will always be on the lower screen.
                // This means we do not need to shrink the webview.
                getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
            }
        } else {
            // Shrink empty view. If constraint is not set, then its size will be (0,0).
            constraintSet.clear(R.id.dual_screen_empty_view);
        }

        final ConstraintLayout dualScreenLayout = findViewById(R.id.dual_screen_layout);
        constraintSet.applyTo(dualScreenLayout);
        // Request layout to apply the changes
        dualScreenLayout.post(dualScreenLayout::requestLayout);
    }

    /**
     * Returns true if the app is being spanned across two screens.
     */
    public boolean isAppSpanned(final Activity activity) {
        // Check if it's a fold/hinge that separates the display into two distinct areas
        if (mFoldingFeature != null &&
                (mFoldingFeature.getState() == FoldingFeature.State.FLAT ||
                        mFoldingFeature.getState() == FoldingFeature.State.HALF_OPENED)) {
            final Rect windowRect = getWindowRect(activity);
            return mFoldingFeature.getBounds().intersect(windowRect);
        }
        // If no folding feature is present or it doesn't intersect the app window, return false
        return false;
    }

    /**
     * Get the device's rotation.
     *
     * @return Surface.ROTATION_0, Surface.ROTATION_90, Surface.ROTATION_180 or Surface.ROTATION_270
     */
    public int getRotation(Activity activity) {
        WindowManager wm = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
        int rotation = 0;
        if (wm != null) {
            rotation = wm.getDefaultDisplay().getRotation();
        }
        return rotation;
    }

    /**
     * Returns the area of the displaying window.
     */
    private Rect getWindowRect(final Activity activity) {
        Rect windowRect = new Rect();
        activity.getWindowManager().getDefaultDisplay().getRectSize(windowRect);
        return windowRect;
    }
}
