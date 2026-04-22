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

/**
 * Registry for broker-provided Auth Tab strategy integration.
 *
 * Common does not depend on AndroidX Browser 1.9.0 APIs directly; the broker registers Auth Tab
 * support and strategy construction through this provider at initialization time.
 *
 * Thread-safety contract: registration is expected to happen during process initialization before
 * concurrent reads. A single volatile [registration] object ensures both factory and checker are
 * observed together.
 */
object AuthTabStrategyProvider {

    private data class Registration(
        val strategyFactory: (FragmentActivity, (Bundle?) -> Unit) -> BrowserLaunchStrategy,
        val supportChecker: (Context, String) -> Boolean
    )

    @Volatile
    private var registration: Registration? = null

    /**
     * Registers broker-provided Auth Tab strategy creation and support checks.
     *
     * @param factory Creates a [BrowserLaunchStrategy] implementation for Auth Tab.
     * @param isSupported Checks if Auth Tab is supported for the provided browser package.
     */
    @Synchronized
    fun register(
        factory: (FragmentActivity, (Bundle?) -> Unit) -> BrowserLaunchStrategy,
        isSupported: (Context, String) -> Boolean
    ) {
        registration = Registration(
            strategyFactory = factory,
            supportChecker = isSupported
        )
    }

    /**
     * Returns whether Auth Tab is supported for [browserPackage].
     *
     * Returns `false` when no support checker is registered.
     */
    fun isAuthTabSupported(context: Context, browserPackage: String): Boolean {
        return registration?.supportChecker?.invoke(context, browserPackage) ?: false
    }

    /**
     * Creates a broker-provided Auth Tab launch strategy.
     *
     * Returns `null` when no strategy factory is registered.
     */
    fun createStrategy(
        activity: FragmentActivity,
        onResult: (Bundle?) -> Unit
    ): BrowserLaunchStrategy? {
        return registration?.strategyFactory?.invoke(activity, onResult)
    }

    /**
     * Returns `true` when an Auth Tab strategy factory has been registered.
     */
    fun isAvailable(): Boolean = registration != null

    /**
     * Clears the registered factory and checker. Visible for unit tests only.
     */
    @Synchronized
    internal fun resetForTest() {
        registration = null
    }
}
