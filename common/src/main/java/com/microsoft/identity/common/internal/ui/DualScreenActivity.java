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
import android.graphics.Rect;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.window.layout.FoldingFeature;

import com.microsoft.identity.common.R;


// This activity readjusts its child layouts so that they're displayed on both single-screen and dual-screen device correctly.
public class DualScreenActivity extends FragmentActivity {

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(R.layout.dual_screen_layout);
        adjustLayoutForDualScreenActivity();
        final RelativeLayout contentLayout = findViewById(R.id.dual_screen_content);
        LayoutInflater.from(this).inflate(layoutResID, contentLayout);
    }

    public void setFragment(@NonNull final Fragment fragment) {
        super.setContentView(R.layout.dual_screen_layout);
        adjustLayoutForDualScreenActivity();
        getSupportFragmentManager()
                .beginTransaction()
                .setTransitionStyle(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .replace(R.id.dual_screen_content, fragment)
                .commit();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        adjustLayoutForDualScreenActivity();
    }


    private void adjustLayoutForDualScreenActivity() {
        setBaseLayoutForDualScreen();
        DeviceUtils.INSTANCE.getFoldingFeaturesSuspendedForJava(this,
                (foldingFeature) -> {
                    runOnUiThread(() -> {

                        Log.i("DualScreenActivity", "Folding feature: " + foldingFeature);
                        // If the device is not dual-screen, then we do not need to adjust the layout.
                        if (foldingFeature != null && foldingFeature.isSeparating()) {
                            Log.i("DualScreenActivity", "isSeparating: " + foldingFeature.isSeparating());
                            final Rect hinge = foldingFeature.getBounds();
                            if (isAppSpanned(hinge)) {
                                Log.i("DualScreenActivity", "Orientation: " + foldingFeature.getOrientation());
                                if (foldingFeature.getOrientation() == FoldingFeature.Orientation.HORIZONTAL) {
                                    adjustLayoutSpannedHorizontal(hinge);
                                } else {
                                    adjustLayoutSpannedVertical(hinge);
                                }
                            }
                        } else {
                            setBaseLayoutForNotDual();
                        }
                    });
                    return null;
                });


//        DeviceUtils.INSTANCE.getFoldingFeatures(this, (foldingFeature) -> {
//            runOnUiThread(() -> {
//                Log.i("DualScreenActivity", "Folding feature: " + foldingFeature);
//                // If the device is not dual-screen, then we do not need to adjust the layout.
//                if (foldingFeature != null && foldingFeature.isSeparating()) {
//                    Log.i("DualScreenActivity", "isSeparating: " + foldingFeature.isSeparating());
//                    final Rect hinge = foldingFeature.getBounds();
//                    if (isAppSpanned(hinge)) {
//                        Log.i("DualScreenActivity", "Orientation: " + foldingFeature.getOrientation());
//                        if (foldingFeature.getOrientation() == FoldingFeature.Orientation.HORIZONTAL) {
//                           adjustLayoutSpannedHorizontal(hinge);
//                        } else {
//                           adjustLayoutSpannedVertical(hinge);
//                        }
//                    }
//                }
//            });
//            return null;
//        });
    }

    /**
     * Returns true if the app is being spanned across two screens.
     */
    public boolean isAppSpanned(final Rect hinge) {
        final Rect windowRect = getWindowRect();
        if (windowRect.width() > 0 && windowRect.height() > 0) {
            // The windowRect doesn't intersect hinge
            return hinge.intersect(windowRect);
        }
        return false;
    }


    /**
     * Returns the area of the displaying window.
     */
    private Rect getWindowRect() {
        Rect windowRect = new Rect();
        this.getWindowManager().getDefaultDisplay().getRectSize(windowRect);
        return windowRect;
    }


    private ConstraintSet getCommonConstraintSet()  {
        Log.i("DualScreenActivity", "Creating common ConstraintSet for dual screen layout.");
        final ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.connect(R.id.dual_screen_content, ConstraintSet.LEFT, R.id.dual_screen_layout, ConstraintSet.LEFT, 0);
        constraintSet.connect(R.id.dual_screen_content, ConstraintSet.RIGHT, R.id.dual_screen_layout, ConstraintSet.RIGHT, 0);
        constraintSet.connect(R.id.dual_screen_content, ConstraintSet.TOP, R.id.dual_screen_layout, ConstraintSet.TOP, 0);
        constraintSet.connect(R.id.dual_screen_content, ConstraintSet.BOTTOM, R.id.dual_screen_layout, ConstraintSet.BOTTOM, 0);

        constraintSet.connect(R.id.dual_screen_empty_view, ConstraintSet.LEFT, R.id.dual_screen_layout, ConstraintSet.LEFT, 0);
        constraintSet.connect(R.id.dual_screen_empty_view, ConstraintSet.RIGHT, R.id.dual_screen_layout, ConstraintSet.RIGHT, 0);
        constraintSet.connect(R.id.dual_screen_empty_view, ConstraintSet.TOP, R.id.dual_screen_layout, ConstraintSet.TOP, 0);
        constraintSet.connect(R.id.dual_screen_empty_view, ConstraintSet.BOTTOM, R.id.dual_screen_layout, ConstraintSet.BOTTOM, 0);
        return constraintSet;
    }

    private void setConstraintSetForDualScreenLayout(@NonNull final ConstraintSet constraintSet) {
        final ConstraintLayout dualScreenLayout = findViewById(R.id.dual_screen_layout);
        dualScreenLayout.setConstraintSet(constraintSet);
    }

    private void setBaseLayoutForDualScreen() {
        Log.i("DualScreenActivity", "Setting base layout for dual screen.");
        final ConstraintSet constraintSet = getCommonConstraintSet();
       // getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        setConstraintSetForDualScreenLayout(constraintSet);
    }

    private void setBaseLayoutForNotDual() {
        Log.i("DualScreenActivity", "Setting base layout for not dual screen.");
        final ConstraintSet constraintSet = getCommonConstraintSet();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        constraintSet.clear(R.id.dual_screen_empty_view);
        setConstraintSetForDualScreenLayout(constraintSet);
    }

    private void adjustLayoutSpannedVertical(@NonNull final Rect hinge) {
        Log.i("DualScreenActivity", "Vertical, hinge height: " + hinge );

        final ConstraintSet constraintSet = getCommonConstraintSet();
        int duoHingeWidth = hinge.width() / 2;
        // WebView is on the right.
        constraintSet.connect(R.id.dual_screen_content, ConstraintSet.LEFT, R.id.vertical_guideline, ConstraintSet.RIGHT, duoHingeWidth);
        // Empty view is on the left.
        constraintSet.connect(R.id.dual_screen_empty_view, ConstraintSet.RIGHT, R.id.vertical_guideline, ConstraintSet.LEFT, 0);

        setConstraintSetForDualScreenLayout(constraintSet);
    }

    private void adjustLayoutSpannedHorizontal(@NonNull final Rect hinge) {
        Log.i("DualScreenActivity", "Horizontal, hinge height: " + hinge );

        final ConstraintSet constraintSet = getCommonConstraintSet();
        int duoHingeWidth = hinge.height() / 2;
        // WebView is on the top.
        constraintSet.connect(R.id.dual_screen_content, ConstraintSet.BOTTOM, R.id.horizontal_guideline, ConstraintSet.TOP, duoHingeWidth);
        // Empty view is in the bottom.
        constraintSet.connect(R.id.dual_screen_empty_view, ConstraintSet.TOP, R.id.horizontal_guideline, ConstraintSet.BOTTOM, 0);
        // In spanned vertical mode, keyboard will always be on the lower screen.
        // This means we do not need to shrink the webview.
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        setConstraintSetForDualScreenLayout(constraintSet);
    }







}

