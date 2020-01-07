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

package com.microsoft.identity.common.internal.request;

import android.os.Bundle;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.internal.logging.Logger;

public class BrokerRequestAdapterFactory {

    private static final String TAG = BrokerRequestAdapterFactory.class.getName();

    public static IBrokerRequestAdapter getBrokerRequestAdapter(final Bundle requestBundle) {
        final String methodName = ":getBrokerRequestAdapter";
        if (requestBundle != null &&
                requestBundle.containsKey(AuthenticationConstants.Broker.BROKER_REQUEST_V2)) {
            Logger.info(TAG + methodName, "Request from MSAL, returning MsalBrokerRequestAdapter");
            return new MsalBrokerRequestAdapter();
        } else {
            Logger.info(TAG + methodName, "Request from ADAL, returning AdalBrokerRequestAdapter");
            return new AdalBrokerRequestAdapter();
        }
    }
}
