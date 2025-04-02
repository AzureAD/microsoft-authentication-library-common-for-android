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
package com.microsoft.identity.common.internal.broker

import android.os.Bundle

object CommonBrokerRestrictionsManager: IBrokerRestrictionsManager{

    private var brokerRestrictionsManager: IBrokerRestrictionsManager? = null
    override val defaultPackageName: String = "UNSET"

    fun initialize(brokerRestrictionsManager: IBrokerRestrictionsManager) {
            this.brokerRestrictionsManager = brokerRestrictionsManager
    }

    override fun getString(key: String, appPackageName: String, defaultValue: String?): String? {
        brokerRestrictionsManager?.let {
            return if (appPackageName == defaultPackageName) {
                it.getString(key = key, defaultValue = defaultValue)
            } else {
                it.getString(key = key, defaultValue = defaultValue, appPackageName = appPackageName)
            }
        }
        return defaultValue
    }

    override fun getBoolean(key: String, appPackageName: String, defaultValue: Boolean): Boolean {
        brokerRestrictionsManager?.let {
            return if (appPackageName == defaultPackageName) {
                it.getBoolean(key = key, defaultValue = defaultValue)
            } else {
                it.getBoolean(key = key, defaultValue = defaultValue, appPackageName = appPackageName)
            }
        }
        return defaultValue
    }

    override fun getFilteredBundleFromLocalRestrictionManager(bundleOfKeys: Bundle): Bundle {
        brokerRestrictionsManager?.let {
                return it.getFilteredBundleFromLocalRestrictionManager(bundleOfKeys)
        }
        return Bundle()
    }
}
