package com.microsoft.identity.common.internal.providers.oauth2

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.microsoft.identity.common.internal.ui.webview.switchbrowser.SwitchBrowserProtocolCoordinator
import com.microsoft.identity.common.logging.Logger

class DUNAActivity: FragmentActivity() {

    companion object {
        private val TAG: String = DUNAActivity::class.java.simpleName
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        val methodTag = "$TAG:onCreate"
        super.onCreate(savedInstanceState)


        Logger.info(methodTag, "DUNAActivity onCreate")
        if (savedInstanceState == null) {
            Logger.info(methodTag, "Extracting browser intent from extras for DUNA flow")
            if (intent == null) {
                Logger.warn(methodTag, "Intent is null")
                finish()
                return
            }
            val browser = intent.getParcelableExtra<Intent>("browser_intent")
            if (browser == null) {
                Logger.warn(methodTag, "No browser intent found in extras")
            }
            Logger.info(methodTag, "Launching Custom Chrome Tab intent for DUNA authentication")
            startActivity(browser)
        } else {
            Logger.info(methodTag, "Activity restored from saved state - Skipping browser intent launch")
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        //setIntent(intent)
        val methodTag = "$TAG:onNewIntent"
        Logger.info(methodTag, "onNewIntent")

        intent?.dataString?.let { intentData ->
            val result = SwitchBrowserProtocolCoordinator.getIntentToResumeWebViewAuth(applicationContext, intentData)
            Logger.info(methodTag, intentData)
            WebViewAuthorizationFragment.dunaIntent = result
        }
        finishAndRemoveTask()
    }



}