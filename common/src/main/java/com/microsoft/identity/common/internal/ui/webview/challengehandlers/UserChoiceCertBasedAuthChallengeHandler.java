package com.microsoft.identity.common.internal.ui.webview.challengehandlers;

import android.app.Activity;
import android.os.Build;
import android.webkit.ClientCertRequest;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.microsoft.identity.common.java.providers.RawAuthorizationResult;

public class UserChoiceCertBasedAuthChallengeHandler implements ICertBasedAuthChallengeHandler {
    private static final String TAG = UserChoiceCertBasedAuthChallengeHandler.class.getSimpleName();
    private final Activity mActivity;
    protected final AbstractSmartcardCertBasedAuthManager mSmartcardCertBasedAuthManager;
    private final DialogHolder mDialogHolder;

    public UserChoiceCertBasedAuthChallengeHandler(@NonNull final Activity activity,
                                                   @NonNull final AbstractSmartcardCertBasedAuthManager smartcardCertBasedAuthManager,
                                                   @NonNull final DialogHolder dialogHolder) {
        mActivity = activity;
        mSmartcardCertBasedAuthManager = smartcardCertBasedAuthManager;
        mDialogHolder = dialogHolder;
    }


    /**
     * Emit telemetry for results from certificate based authentication (CBA) if CBA occurred.
     *
     * @param response a RawAuthorizationResult object received upon a challenge response received.
     */
    @Override
    public void emitTelemetryForCertBasedAuthResults(@NonNull RawAuthorizationResult response) {

    }

    /**
     * Clean up logic to run when ICertBasedAuthChallengeHandler is no longer going to be used.
     */
    @Override
    public void cleanUp() {

    }

    /**
     * Process difference kinds of challenge request.
     *
     * @param request challenge request
     * @return GenericResponse
     */
    @Override
    public Void processChallenge(ClientCertRequest request) {
        //Show SmartcardUserChoiceDialog
        SmartcardUserChoiceDialog dialog = new SmartcardUserChoiceDialog(mActivity);
        mDialogHolder.showDialog(dialog);
        mSmartcardCertBasedAuthManager.setConnectionCallback(new AbstractSmartcardCertBasedAuthManager.IConnectionCallback() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onCreateConnection(final boolean isNfc) {
                new SmartcardCertBasedAuthChallengeHandler(mActivity, mSmartcardCertBasedAuthManager, mDialogHolder, isNfc).processChallenge(request);
            }

            @Override
            public void onClosedConnection() {

            }
        });
        mSmartcardCertBasedAuthManager.startNfcDiscovery(mActivity);
        return null;
    }
}
