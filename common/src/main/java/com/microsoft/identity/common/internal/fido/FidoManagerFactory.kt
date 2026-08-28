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

    /**
     * @param provider provider to consult for subsequent challenges, or null to unregister.
     */
    @JvmStatic
    fun setProvider(provider: IFidoManagerProvider?) {
        Logger.info(
            "$TAG:setProvider",
            if (provider == null) {
                "Host passkey provider cleared."
            } else {
                "Host passkey provider registered: " + provider.javaClass.simpleName
            }
        )
        this.provider = provider
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
        val manager = provider?.getFidoManager(activity)
            ?: CredManFidoManager(activity, legacyManager)
        Logger.info(
            "$TAG:getFidoManager",
            "Fulfilling passkey challenge with " + manager.javaClass.simpleName
        )
        return manager
    }
}
