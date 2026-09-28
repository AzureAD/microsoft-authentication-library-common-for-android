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
package com.microsoft.identity.common.internal.fido

import android.app.Activity
import android.content.Context
import android.os.Process
import androidx.annotation.VisibleForTesting
import com.microsoft.identity.common.internal.broker.BrokerData
import com.microsoft.identity.common.internal.broker.BrokerValidator
import com.microsoft.identity.common.logging.Logger

/**
 * Chooses the [IFidoManager] that fulfils a WebView passkey challenge.
 *
 * A host may register an [IFidoManagerProvider] to handle ceremonies through a mechanism this
 * library has no knowledge of. Hosts that register nothing, and any challenge the provider
 * declines, use Credential Manager.
 */
object FidoManagerFactory {

    private val TAG = FidoManagerFactory::class.simpleName.toString()

    @Volatile
    private var provider: IFidoManagerProvider? = null

    // Seam for this library's own tests, which do not run inside a signed broker app.
    @VisibleForTesting
    internal var isBrokerHosted: (Context) -> Boolean = ::isSignedBrokerApp

    /**
     * Registers the provider consulted for subsequent challenges. Ignored unless the calling app is
     * a broker, so a library embedded in any other app cannot take over passkey handling.
     *
     * @param context context of the app registering the provider.
     * @param provider provider to consult for subsequent challenges, or null to unregister.
     * @return true when the provider was accepted; callers that expect to be a broker should treat
     * false as a misconfiguration rather than ignoring it.
     */
    @JvmStatic
    fun setProvider(context: Context, provider: IFidoManagerProvider?): Boolean {
        val methodTag = "$TAG:setProvider"
        if (!isBrokerHosted(context)) {
            Logger.warn(
                methodTag,
                "Ignoring a passkey provider from " + context.packageName + "; not a broker app."
            )
            return false
        }
        Logger.info(
            methodTag,
            if (provider == null) {
                "Host passkey provider cleared."
            } else {
                "Host passkey provider registered: " + provider.javaClass.simpleName
            }
        )
        this.provider = provider
        return true
    }

    // Matches against every known broker, prod and debug alike: this identifies the process we are
    // already running in, so the debug/prod split that BrokerData.getKnownBrokerApps() applies to
    // peer discovery would only break debug brokers here.
    //
    // The package name is taken from our own uid rather than from the supplied Context, whose
    // getPackageName() any caller can point at an installed broker via createPackageContext().
    private fun isSignedBrokerApp(context: Context): Boolean {
        val ownPackages = context.packageManager.getPackagesForUid(Process.myUid())?.toSet().orEmpty()
        val validator = BrokerValidator(context)
        return BrokerData.allBrokers.any {
            ownPackages.contains(it.packageName) && validator.isSignedByKnownKeys(it)
        }
    }

    /**
     * @param activity foreground Activity hosting the WebView; also the context for the Credential
     * Manager fallback.
     * @param legacyManager legacy GMS FIDO2 manager, when applicable.
     * @return the manager to fulfil this challenge; never null.
     */
    @JvmStatic
    fun getFidoManager(
        activity: Activity,
        legacyManager: IFidoManager?
    ): IFidoManager {
        val methodTag = "$TAG:getFidoManager"
        val manager = provider?.getFidoManager(activity)
            ?: CredManFidoManager(activity, legacyManager)
        Logger.info(
            methodTag,
            "Fulfilling passkey challenge with " + manager.javaClass.simpleName
        )
        return manager
    }
}
