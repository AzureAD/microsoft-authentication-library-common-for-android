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

import com.microsoft.identity.common.java.interfaces.IRefreshTokenCredentialProvider
import com.microsoft.identity.common.java.logging.Logger

/**
 * Consumer of commons needs to implement [IRefreshTokenCredentialProvider] interface
 * and set it using CommonRefreshTokenCredentialProvider.initializeCommonRefreshTokenCredentialProvider(@NonNull refreshTokenCredentialProvider: IRefreshTokenCredentialProvider)
 * to provide prtCredentialHolder to common module.
 */
object CommonRefreshTokenCredentialProvider : IRefreshTokenCredentialProvider {
    private val TAG = CommonRefreshTokenCredentialProvider::class.java.simpleName
    private var mRefreshTokenCredentialProvider: IRefreshTokenCredentialProvider? = null

    // Note : This method should only be invoked by broker module.
    fun initializeCommonRefreshTokenCredentialProvider(refreshTokenCredentialProvider: IRefreshTokenCredentialProvider) {
        val methodTag = "$TAG:initializeCommonRefreshTokenCredentialProvider"
        Logger.info(methodTag, "Initializing common prt credential provider with " + refreshTokenCredentialProvider.javaClass.simpleName)
        mRefreshTokenCredentialProvider = refreshTokenCredentialProvider
    }

    override fun getRefreshTokenCredentialUsingNewNonce(inputUrl : String, username : String, nonce : String) : String? {
        val methodTag = "$TAG:getRefreshTokenCredentialUsingNewNonce";
        if (mRefreshTokenCredentialProvider != null) {
            return mRefreshTokenCredentialProvider!!.getRefreshTokenCredentialUsingNewNonce(inputUrl, username, nonce)
        }
        Logger.warn(methodTag, "mRefreshTokenCredentialHolder is not initialized!")
        return null
    }
}
