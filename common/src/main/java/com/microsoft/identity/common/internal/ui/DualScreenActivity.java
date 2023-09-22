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
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

import com.microsoft.device.display.DisplayMask;
import com.microsoft.identity.common.R;

import java.util.List;

// This activity readjusts its child layouts so that they're displayed on both single-screen and dual-screen device correctly.
public class DualScreenActivity extends FragmentActivity {

    @Override
    public void setContentView(int layoutResID) {
        initializeContentView();
        final RelativeLayout contentLayout = findViewById(R.id.dual_screen_content);
        LayoutInflater.from(this).inflate(layoutResID, contentLayout);
    }

    private void initializeContentView(){
        super.setContentView(R.layout.dual_screen_layout);
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

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        adjustLayoutForDualScreenActivity();
    }

    private void adjustLayoutForDualScreenActivity() {
        int rotation = getRotation(this);
        boolean isAppSpanned = isAppSpanned(this);
        boolean isHorizontal = rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180;

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

        if (isAppSpanned) {
            if (isHorizontal) {
                int duoHingeWidth = getHinge(this, rotation).width() / 2;

                // WebView is on the right.
                constraintSet.connect(R.id.dual_screen_content, ConstraintSet.LEFT, R.id.vertical_guideline, ConstraintSet.RIGHT, duoHingeWidth);

                // Empty view is on the left.
                constraintSet.connect(R.id.dual_screen_empty_view, ConstraintSet.RIGHT, R.id.vertical_guideline, ConstraintSet.LEFT, 0);
            } else {
                int duoHingeWidth = getHinge(this, rotation).height() / 2;

                // WebView is on the top.
                constraintSet.connect(R.id.dual_screen_content, ConstraintSet.BOTTOM, R.id.horizontal_guideline, ConstraintSet.TOP, duoHingeWidth);

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
        dualScreenLayout.setConstraintSet(constraintSet);
    }

    /**
     * Returns true if the app is being spanned across two screens.
     */
    public boolean isAppSpanned(final Activity activity) {
        if (!isDualScreenDevice(activity)) {
            return false;
        }

        int rotation = getRotation(activity);
        Rect hinge = getHinge(activity, rotation);
        Rect windowRect = getWindowRect(activity);

        if (windowRect.width() > 0 && windowRect.height() > 0) {
            // The windowRect doesn't intersect hinge
            return hinge.intersect(windowRect);
        }

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
     * Returns true if this device supports dual screen mode.
     */
    private boolean isDualScreenDevice(final Context context) {
        final String feature = "com.microsoft.device.display.displaymask";
        final PackageManager pm = context.getPackageManager();

        if (pm.hasSystemFeature(feature)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Returns the area of the display that is not functional for displaying content.
     *
     * @param Context
     * @param rotation Surface.ROTATION_0, Surface.ROTATION_90, Surface.ROTATION_180 or Surface.ROTATION_270
     */
    private Rect getHinge(final Context context,
                          int rotation) {
        // Hinge's coordinates of its 4 edges in different mode
        // Double Landscape Rect(0, 1350 - 1800, 1434)
        // Double Portrait  Rect(1350, 0 - 1434, 1800)
        final DisplayMask displayMask = DisplayMask.fromResourcesRect(context);
        List<Rect> boundings = displayMask.getBoundingRectsForRotation(rotation);
        if (boundings.size() == 0) {
            return new Rect(0, 0, 0, 0);
        }
        return boundings.get(0);
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
