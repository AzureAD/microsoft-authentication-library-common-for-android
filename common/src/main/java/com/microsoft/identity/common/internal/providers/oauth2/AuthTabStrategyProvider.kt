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
package com.microsoft.identity.common.internal.providers.oauth2

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.microsoft.identity.common.logging.Logger

/**
 * Provider/registry for Broker-owned Auth Tab strategy integrations.
 *
 * Common intentionally avoids direct dependencies on browser 1.9.0 APIs.
 */
internal object AuthTabStrategyProvider {

    private val tag = AuthTabStrategyProvider::class.java.simpleName

    /**
     * Factory for creating Auth Tab strategy instances.
     *
     * Parameters: [FragmentActivity], result callback.
     */
    typealias AuthTabStrategyFactory = (FragmentActivity, (Bundle?) -> Unit) -> BrowserLaunchStrategy?

    /**
     * Checker for Auth Tab support.
     *
     * Parameters: [Context], browser package name.
     */
    typealias AuthTabSupportChecker = (Context, String) -> Boolean

    private data class Registration(
        val factory: AuthTabStrategyFactory,
        val checker: AuthTabSupportChecker
    )

    // Volatile guarantees safe publication for immutable Registration instances.
    @Volatile
    private var registration: Registration? = null

    /**
     * Registers the broker-provided Auth Tab implementation.
     *
     * @param factory Factory that creates a [BrowserLaunchStrategy] instance.
     * @param isSupported Checker indicating whether Auth Tab is supported for a browser package.
     */
    @Synchronized
    fun register(
        factory: AuthTabStrategyFactory,
        isSupported: AuthTabSupportChecker
    ) {
        registration = Registration(factory, isSupported)
        Logger.info("$tag:register", "Auth Tab strategy provider registered")
    }

    /**
     * Returns true if an Auth Tab strategy factory has been registered.
     */
    fun isAvailable(): Boolean = registration != null

    /**
     * Returns true when a registered checker reports Auth Tab support for the browser package.
     *
     * Returns false when no checker has been registered.
     */
    fun isAuthTabSupported(context: Context, browserPackage: String): Boolean {
        return registration?.checker?.invoke(context, browserPackage) ?: false
    }

    /**
     * Creates an Auth Tab strategy instance from the registered factory.
     *
     * Returns null when no factory has been registered, or when the registered factory
     * returns null.
     */
    fun createStrategy(
        activity: FragmentActivity,
        onResult: (Bundle?) -> Unit
    ): BrowserLaunchStrategy? {
        return registration?.factory?.invoke(activity, onResult)
    }

    /**
     * Unregisters the provider. Intended for tests and controlled shutdown paths.
     */
    @Synchronized
    fun unregister() {
        registration = null
    }
}
