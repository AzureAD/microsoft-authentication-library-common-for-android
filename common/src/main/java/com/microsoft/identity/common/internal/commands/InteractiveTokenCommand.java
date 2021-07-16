//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.common.internal.commands;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;

import com.microsoft.identity.common.java.WarningType;
import com.microsoft.identity.common.internal.commands.parameters.InteractiveTokenCommandParameters;
import com.microsoft.identity.common.internal.controllers.BaseController;
import com.microsoft.identity.common.internal.result.AcquireTokenResult;
import com.microsoft.identity.common.logging.Logger;

import java.util.List;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
public class InteractiveTokenCommand extends TokenCommand {
    private static final String TAG = InteractiveTokenCommand.class.getSimpleName();
    //Normally all tasks have an affinity unless configured explicitly for multi-window support to not have one
    private boolean mHasTaskAffinity = true;
    private int mTaskId = 0;

    public InteractiveTokenCommand(@NonNull final InteractiveTokenCommandParameters parameters,
                                   @NonNull final BaseController controller,
                                   @SuppressWarnings(WarningType.rawtype_warning) @NonNull final CommandCallback callback,
                                   @NonNull final String publicApiId) {
        super(parameters, controller, callback, publicApiId);
        checkAndRecordTaskInformation(parameters);
    }

    public InteractiveTokenCommand(@NonNull InteractiveTokenCommandParameters parameters,
                                   @NonNull List<BaseController> controllers,
                                   @SuppressWarnings(WarningType.rawtype_warning) @NonNull CommandCallback callback,
                                   @NonNull final String publicApiId) {
        super(parameters, controllers, callback, publicApiId);
        checkAndRecordTaskInformation(parameters);
    }

    private void checkAndRecordTaskInformation(@NonNull final InteractiveTokenCommandParameters parameters){
        final String methodName = ":checkAndRecordTaskInformation";
        final Context applicationContext = parameters.getAndroidApplicationContext();
        final PackageManager packageManager = applicationContext.getPackageManager();
        try {
            final ComponentName componentName = parameters.getActivity().getComponentName();
            final ActivityInfo startActivityInfo = componentName != null ? packageManager.getActivityInfo(componentName, 0) : null;
            if (startActivityInfo == null || startActivityInfo.taskAffinity == null){
                mHasTaskAffinity = false;
                mTaskId = parameters.getActivity().getTaskId();
            }
        } catch (final PackageManager.NameNotFoundException e) {
            Logger.warn(
                    TAG + methodName,
                    "Unable to get ActivityInfo for activity provided to start authorization."
            );
        }
    }

    public boolean getHasTaskAffinity(){
        return mHasTaskAffinity;
    }

    public int getTaskId(){
        return mTaskId;
    }


    @Override
    public AcquireTokenResult execute() throws Exception {
        final String methodName = ":execute";
        if (getParameters() instanceof InteractiveTokenCommandParameters) {
            Logger.info(
                    TAG + methodName,
                    "Executing interactive token command..."
            );

            return getDefaultController()
                    .acquireToken(
                            (InteractiveTokenCommandParameters) getParameters()
                    );
        } else {
            throw new IllegalArgumentException("Invalid operation parameters");
        }
    }

    @Override
    public void notify(int requestCode, int resultCode, final Intent data) {
        getDefaultController().completeAcquireToken(requestCode, resultCode, data);
    }

    @Override
    public boolean isEligibleForEstsTelemetry() {
        return true;
    }
}
