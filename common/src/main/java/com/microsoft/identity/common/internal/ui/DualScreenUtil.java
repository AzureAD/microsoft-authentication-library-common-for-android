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
import android.graphics.Rect;
import android.view.WindowManager;

import com.microsoft.device.display.DisplayMask;

import java.util.List;

// Adapt from https://github.com/microsoft/surface-duo-sdk-samples/blob/master/utils/src/main/java/com/microsoft/device/display/samples/utils/ScreenHelper.java
public class DualScreenUtil {

    public static boolean isAppSpanned(final Activity activity) {
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

    private static boolean isDualScreenDevice(final Context context) {
        final String feature = "com.microsoft.device.display.displaymask";
        final PackageManager pm = context.getPackageManager();

        if (pm.hasSystemFeature(feature)) {
            return true;
        } else {
            return false;
        }
    }

    private static Rect getHinge(final Activity activity,
                                 int rotation) {
        // Hinge's coordinates of its 4 edges in different mode
        // Double Landscape Rect(0, 1350 - 1800, 1434)
        // Double Portrait  Rect(1350, 0 - 1434, 1800)
        final DisplayMask displayMask = DisplayMask.fromResourcesRect(activity);
        List<Rect> boundings = displayMask.getBoundingRectsForRotation(rotation);
        if (boundings.size() == 0) {
            return new Rect(0, 0, 0, 0);
        }
        return boundings.get(0);
    }

    private static Rect getWindowRect(final Activity activity) {
        Rect windowRect = new Rect();
        activity.getWindowManager().getDefaultDisplay().getRectSize(windowRect);
        return windowRect;
    }

    public static int getRotation(Activity activity) {
        WindowManager wm = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
        int rotation = 0;
        if (wm != null) {
            rotation = wm.getDefaultDisplay().getRotation();
        }
        return rotation;
    }

}
