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

package com.microsoft.identity.common.internal.broker.ipc;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * An object that acts as a bridge between business logic and communication layer.
 * - Business logic will provide a request bundle, and specify which operation it wants to perform.
 * - Communication layer will determine how to communicate to the targeted service via the provided operation,
 * and pass the request bundle to the service accordingly.
 * <p>
 * Generally, the targeted service is the active broker.
 */
@AllArgsConstructor
public class BrokerOperationBundle {

    public enum Operation {
        MSAL_HELLO,
        MSAL_GET_INTENT_FOR_INTERACTIVE_REQUEST,
        MSAL_ACQUIRE_TOKEN_SILENT,
        MSAL_GET_ACCOUNTS,
        MSAL_REMOVE_ACCOUNT,
        MSAL_GET_DEVICE_MODE,
        MSAL_GET_CURRENT_ACCOUNT_IN_SHARED_DEVICE,
        MSAL_SIGN_OUT_FROM_SHARED_DEVICE,
        BROKER_GET_KEY_FROM_INACTIVE_BROKER
    }

    @Getter
    @NonNull final private Operation operation;

    @Getter
    @NonNull final private String targetBrokerAppPackageName;

    @Getter
    @Nullable final private Bundle bundle;

//    public String getAccountManagerOperationKey(){
//        // TODO
//    }
//
//    public String getContentProviderUriPath(){
//        // TODO
//    }
}
