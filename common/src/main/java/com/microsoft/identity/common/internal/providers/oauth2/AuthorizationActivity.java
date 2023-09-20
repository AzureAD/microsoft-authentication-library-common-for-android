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
package com.microsoft.identity.common.internal.providers.oauth2;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.microsoft.identity.common.internal.ui.DualScreenActivity;
import com.microsoft.identity.common.logging.Logger;

public class AuthorizationActivity extends DualScreenActivity {

    public static final String TAG = AuthorizationActivity.class.getSimpleName();
    public static final int CAMERA_PERMISSION_REQUEST_CODE = 1;
    private AuthorizationFragment mFragment;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final String methodTag = TAG + ":onCreate";
        final Fragment fragment = AuthorizationActivityFactory.getAuthorizationFragmentFromStartIntent(getIntent());
        if (fragment instanceof AuthorizationFragment) {
            mFragment = (AuthorizationFragment) fragment;
            mFragment.setInstanceState(getIntent().getExtras());
        } else {
            final IllegalStateException ex = new IllegalStateException("Unexpected fragment type.");
            Logger.error(methodTag, "Did not receive AuthorizationFragment from factory", ex);
        }
        requestPermissionOnRuntime();
    }

    private void requestPermissionOnRuntime() {
        final int cameraPermissionStatus = ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
        );
        if (cameraPermissionStatus == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permission already granted", Toast.LENGTH_SHORT).show();
        } else  {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case CAMERA_PERMISSION_REQUEST_CODE: {
                if (permissions[0].equals(Manifest.permission.CAMERA)) {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this, "We have permission we can continue", Toast.LENGTH_SHORT).show();
                        setFragment(mFragment);
                    } else {
                        Toast.makeText(this, "We cannot proceed without the ca", Toast.LENGTH_SHORT).show();

                    }
                }
            }
        }
    }


}
