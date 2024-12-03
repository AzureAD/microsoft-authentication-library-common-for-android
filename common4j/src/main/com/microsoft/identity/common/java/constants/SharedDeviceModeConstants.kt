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

package com.microsoft.identity.common.java.constants

/**
 * Constants related to Shared Device Mode
 */
class SharedDeviceModeConstants {
    companion object {
        /**
         * BroadcastId of the Account Change operation.
         */
        const val CURRENT_ACCOUNT_CHANGED_BROADCAST_IDENTIFIER = "com.microsoft.identity.client.sharedmode.CURRENT_ACCOUNT_CHANGED"

        /**
         * Broadcast type key sent as extra in the broadcast intent
         */
        const val BROADCAST_TYPE_KEY = "BROADCAST_TYPE"

        /**
         * Broadcast type for SDM registration start
         */
        const val BROADCAST_TYPE_SDM_REGISTRATION_START = "SDM_REGISTRATION_START"

        /**
         * Broadcast type for SDM registration complete
         */
        const val BROADCAST_TYPE_SDM_REGISTERED = "SDM_REGISTERED"

        /**
         * Broadcast type for SDM GLOBAL_SIGN_OUT
         */
        const val BROADCAST_TYPE_GLOBAL_SIGN_OUT = "GLOBAL_SIGN_OUT"

        /**
         * Prefix for the account name used for the Device Account
         * when performing userless shared device registration using preauthorized challenge
         * The full account name is this prefix followed by the tenant-id
         * where the device is getting registered.
         */
        const val DEVICE_WORK_ACCOUNT_FOR_TENANT_PREFIX = "Device Work account for Tenant:"
    }
}
