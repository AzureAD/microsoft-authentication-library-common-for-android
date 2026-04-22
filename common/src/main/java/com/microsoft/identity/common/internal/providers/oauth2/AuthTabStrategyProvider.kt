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
 * Registry for Auth Tab strategy integration provided by consumers outside Common.
 *
 * Common intentionally does not depend on browser 1.9.0 APIs, so external modules register
 * support checks and strategy creation here.
 */
object AuthTabStrategyProvider {

    private typealias StrategyFactory = (FragmentActivity, (Bundle) -> Unit) -> BrowserLaunchStrategy
    private typealias SupportChecker = (Context, String) -> Boolean

    private val lock = Any()

    @Volatile
    private var strategyFactory: StrategyFactory? = null

    @Volatile
    private var supportChecker: SupportChecker? = null

    private val tag = AuthTabStrategyProvider::class.java.simpleName

    /**
     * Registers Auth Tab strategy creation and support checking callbacks.
     */
    fun register(
        factory: (FragmentActivity, (Bundle) -> Unit) -> BrowserLaunchStrategy,
        isSupported: (Context, String) -> Boolean
    ) {
        synchronized(lock) {
            strategyFactory = factory
            supportChecker = isSupported
        }
        Logger.info("$tag:register", "Auth Tab strategy provider registered")
    }

    /**
     * Returns true if Auth Tab is supported for the provided browser package.
     */
    fun isAuthTabSupported(context: Context, browserPackage: String): Boolean {
        val checker = supportChecker ?: return false
        return checker(context, browserPackage)
    }

    /**
     * Creates an Auth Tab browser launch strategy if registered.
     */
    fun createStrategy(
        activity: FragmentActivity,
        onResult: (Bundle) -> Unit
    ): BrowserLaunchStrategy? {
        val factory = strategyFactory ?: return null
        return factory(activity, onResult)
    }

    /**
     * Returns true if an Auth Tab strategy factory has been registered.
     */
    fun isAvailable(): Boolean = strategyFactory != null

    internal fun resetForTest() {
        synchronized(lock) {
            strategyFactory = null
            supportChecker = null
        }
    }
}
