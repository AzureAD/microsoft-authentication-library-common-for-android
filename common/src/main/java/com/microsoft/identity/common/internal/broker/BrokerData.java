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

package com.microsoft.identity.common.internal.broker;

import android.content.Context;

import androidx.annotation.NonNull;

import com.microsoft.identity.common.exception.ClientException;

/**
 * Represents packageName and SignatureHash of a broker app.
 * */
public class BrokerData{
    public final String packageName;
    public final String signatureHash;

    private BrokerData(String packageName, String hash) {
        this.packageName = packageName;
        this.signatureHash = hash;
    }

    /**
     * Given a broker package name, verify its signature and return a BrokerData object.
     *
     * @throws ClientException an exception containing mismatch signature hashes as its error message.
     * */
    public static BrokerData getBrokerDataForBrokerApp(@NonNull final Context context,
                                                       @NonNull String brokerPackageName) throws ClientException {

        // Verify the signature to make sure that we're not binding to malicious apps.
        final BrokerValidator validator = new BrokerValidator(context);
        return new BrokerData(brokerPackageName, validator.verifySignatureAndThrow(brokerPackageName));
    }
}
