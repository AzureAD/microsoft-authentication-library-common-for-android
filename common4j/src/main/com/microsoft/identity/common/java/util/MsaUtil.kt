//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.common.java.util

import com.microsoft.identity.common.java.authorities.Authority
import com.microsoft.identity.common.java.authorities.AzureActiveDirectoryAudience
import com.microsoft.identity.common.java.authorities.AzureActiveDirectoryAuthority
import java.util.Locale

/**
 * Class for various MSA-related utility methods.
 */
class MsaUtil {

    companion object {
        val jwtPurpose = "v2sso"

        /**
         * Given an authority, check if this is an MSA request
         */
        fun isMsaRequest(authority : Authority) : Boolean {
            return if (authority !is AzureActiveDirectoryAuthority) {
                false
            } else {
                // authority has been silently casted to AzureActiveDirectoryAuthority
                val audience = authority.mAudience.tenantId.lowercase(Locale.ROOT)

                // Check if audience is consumers or the MSA Mega Tenant
                audience == AzureActiveDirectoryAudience.MSA_MEGA_TENANT_ID || audience == AzureActiveDirectoryAudience.CONSUMERS
            }
        }
    }
}
