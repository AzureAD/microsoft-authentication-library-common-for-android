package com.microsoft.identity.common.internal.broker;

import android.app.Activity;
import android.content.Intent;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.exception.ErrorStrings;
import com.microsoft.identity.common.internal.logging.Logger;


public class BrokerTokenProvider {

    private static final String TAG = BrokerTokenProvider.class.getSimpleName();

    /**
     * Acquire token interactively, will prompt user if possible.
     * See {@link MicrosoftAuthServiceHandler#getIntentForInteractiveRequest(final Context context)} for details.
     */
    public void acquireTokenInteractively(final Activity activity)
            throws ClientException {
        final String methodName = ":acquireTokenInteractively";

        if (activity == null) {
            throw new ClientException(ErrorStrings.EMPTY_ANDROID_CONTEXT);
        }

        final Intent brokerIntent = MicrosoftAuthServiceHandler.getInstance().getIntentForInteractiveRequest(activity);
        activity.startActivityForResult(brokerIntent, AuthenticationConstants.UIRequest.BROWSER_FLOW);

        Logger.verbose(TAG + methodName, "Calling activity. " + "Pid:" + android.os.Process.myPid()
                + " tid:" + android.os.Process.myTid() + "uid:" + android.os.Process.myUid());
    }


}
