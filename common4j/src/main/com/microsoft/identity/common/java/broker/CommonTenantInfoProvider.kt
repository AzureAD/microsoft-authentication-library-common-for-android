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
package com.microsoft.identity.common.java.broker
import com.microsoft.identity.common.java.interfaces.ITenantInfoProvider
import com.microsoft.identity.common.java.logging.Logger

/**
 * Consumer of commons needs to implement [ITenantInfoProvider] interface
 * and set it using CommonTenantInfoProvider.initializeCommonTenantInfoProvider(@NonNull tenantInfoProvider: ITenantInfoProvider)
 * to provide tenantInfo to common module.
 */
object CommonTenantInfoProvider : ITenantInfoProvider{
    private val TAG = CommonTenantInfoProvider::class.java.simpleName
    private var mTenantInfoProvider: ITenantInfoProvider? = null

    fun initializeCommonTenantInfoProvider(tenantInfoProvider: ITenantInfoProvider) {
        val methodTag = "$TAG:initializeCommonTenantInfoProvider"
        Logger.info(methodTag, "Initializing common tenant information provider with " + tenantInfoProvider.javaClass.simpleName)
        mTenantInfoProvider = tenantInfoProvider
    }

    override fun getTenantId(username: String): String? {
        val methodTag = "$TAG:getTenantId";
        if (mTenantInfoProvider != null) {
            return mTenantInfoProvider!!.getTenantId(username)
        }
        Logger.warn(methodTag, "mTenantInfoProvider is not initialized!")
        return null
    }
}
