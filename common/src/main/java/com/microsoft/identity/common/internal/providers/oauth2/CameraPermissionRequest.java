package com.microsoft.identity.common.internal.providers.oauth2;

import android.os.Build;
import android.webkit.PermissionRequest;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.microsoft.identity.common.logging.Logger;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class CameraPermissionRequest {

    private static final String TAG = CameraPermissionRequest.class.getSimpleName();


    public final static String[] CAMERA_RESOURCE = new String[]{
            PermissionRequest.RESOURCE_VIDEO_CAPTURE
    };

    private final PermissionRequest mCameraPermissionRequest;
    private boolean isGranted = false;

    public CameraPermissionRequest(@NonNull final PermissionRequest mCameraPermissionRequest) {
        this.mCameraPermissionRequest = mCameraPermissionRequest;
    }

    /**
     * Call this method to grant the permission to access the camera resource.
     * The granted permission is only valid for the current WebView.
     * <p>
     * Note: This method is only available on API level 21 or higher.
     */
    void grant() {
        mCameraPermissionRequest.grant(CAMERA_RESOURCE);
        isGranted = true;
    }

    /**
     * Call this method to deny the permission to access the camera resource.
     * <p>
     * Note: This method is only available on API level 21 or higher.
     */
    void denny() {
        mCameraPermissionRequest.deny();
        isGranted = false;
    }

    /**
     *
     * @return
     */
    boolean isGranted() {
        return isGranted;
    }

    /**
     * Determines whatever if the given permission request is for the camera resource.
     * <p>
     * Note: This method is only available on API level 21 or higher.
     * Devices running on lower API levels will not be able to grant or deny camera permission requests.
     * getResources() method is only available on API level 21 or higher.
     *
     * @param request The permission request.
     * @return true if the given permission request is for camera, false otherwise.
     */
    static boolean isValidRequest(@NonNull final PermissionRequest request) {
        final String methodTag = TAG + ":validateRequest";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return request.getResources().length == 1 &&
                    PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(request.getResources()[0]);
        }
        Logger.warn(methodTag, "PermissionRequest.getResources() method is not available on API:"
                + Build.VERSION.SDK_INT + ". We cannot determine if the request is for camera.");
        return false;
    }

}
