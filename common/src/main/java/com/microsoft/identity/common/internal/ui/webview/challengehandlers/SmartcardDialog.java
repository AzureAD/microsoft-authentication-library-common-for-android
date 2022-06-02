package com.microsoft.identity.common.internal.ui.webview.challengehandlers;

import android.app.Activity;
import android.app.Dialog;

//Lists the main methods needed to show a simple dialog in ClientCertAuthChallengeHandler.
//Button listeners can be implemented in child classes.
public abstract class SmartcardDialog {
    protected Activity mActivity;
    protected Dialog mDialog;

    public SmartcardDialog(Activity activity) {
        mActivity = activity;
    }
    //Should build an Android Dialog object and set it to mDialog.
    //Dialog objects must be built/interacted with on the UI thread.
    abstract void createDialog();

    //Should dismiss dialog and call the appropriate methods to help cancel the CBA flow.
    abstract void onCancelCba();

    //Show mDialog on the main thread.
    public void show() {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mDialog.show();
            }
        });
    }

    //Dismiss mDialog.
    public void dismiss() {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mDialog.dismiss();
            }
        });
    }
}
