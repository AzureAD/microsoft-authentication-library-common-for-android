package com.microsoft.identity.common.internal.ui.webview.challengehandlers;

import android.app.Activity;

import com.microsoft.identity.common.internal.ui.webview.ISmartcardCertBasedAuthManager;

public class SmartcardCertBasedAuthManagerFactory {

    public static ISmartcardCertBasedAuthManager getSmartcardCertBasedAuthManager(Activity activity) {
        //going to just return the YubiKit one for now.
        return new YubiKitCertBasedAuthManager(activity);
    }
}
