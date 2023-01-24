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

import com.microsoft.identity.common.java.exception.ClientException;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_DEBUG_SIGNATURE;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_PACKAGE_NAME;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_RELEASE_SIGNATURE;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.BROKER_HOST_APP_PACKAGE_NAME;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.BROKER_HOST_APP_SIGNATURE;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.COMPANY_PORTAL_APP_RELEASE_SIGNATURE;

import lombok.ToString;

/**
 * Represents packageName and SignatureHash of a broker app.
 */
@ToString
public class BrokerData implements Serializable {

    public static final BrokerData MICROSOFT_AUTHENTICATOR_DEBUG = new BrokerData(
            AZURE_AUTHENTICATOR_APP_PACKAGE_NAME,
            AZURE_AUTHENTICATOR_APP_DEBUG_SIGNATURE
    );

    public static final BrokerData MICROSOFT_AUTHENTICATOR_PROD = new BrokerData(
            AZURE_AUTHENTICATOR_APP_PACKAGE_NAME,
            AZURE_AUTHENTICATOR_APP_RELEASE_SIGNATURE
    );

    public static final BrokerData COMPANY_PORTAL = new BrokerData(
            COMPANY_PORTAL_APP_PACKAGE_NAME,
            COMPANY_PORTAL_APP_RELEASE_SIGNATURE
    );

    public static final BrokerData BROKER_HOST = new BrokerData(
            BROKER_HOST_APP_PACKAGE_NAME,
            BROKER_HOST_APP_SIGNATURE
    );

    public static final BrokerData MOCK_LTW = new BrokerData(
            "com.microsoft.mockltw",
            BROKER_HOST_APP_SIGNATURE
    );

    public static final BrokerData MOCK_CP = new BrokerData(
            "com.microsoft.mockcp",
            BROKER_HOST_APP_SIGNATURE
    );

    public static final BrokerData MOCK_AUTHAPP = new BrokerData(
            "com.microsoft.mockauthapp",
            BROKER_HOST_APP_SIGNATURE
    );

    private static final Set<BrokerData> DEBUG_BROKERS = Collections.unmodifiableSet(new HashSet<BrokerData>() {{
        add(MICROSOFT_AUTHENTICATOR_DEBUG);
        add(BROKER_HOST);
        add(MOCK_LTW);
        add(MOCK_CP);
        add(MOCK_AUTHAPP);
    }});

    private static final Set<BrokerData> PROD_BROKERS = Collections.unmodifiableSet(new HashSet<BrokerData>() {{
        add(MICROSOFT_AUTHENTICATOR_PROD);
        add(COMPANY_PORTAL);
    }});

    private static final Set<BrokerData> ALL_BROKERS = Collections.unmodifiableSet(new HashSet<BrokerData>() {{
        addAll(DEBUG_BROKERS);
        addAll(PROD_BROKERS);
    }});

    public final String packageName;
    public final String signatureHash;

    public BrokerData(@NonNull final String packageName,
                       @NonNull final String hash) {
        this.packageName = packageName;
        this.signatureHash = hash;
    }

    /**
     * Given a broker package name, verify its signature and return a BrokerData object.
     *
     * @throws ClientException an exception containing mismatch signature hashes as its error message.
     */
    public static @NonNull BrokerData getBrokerDataForBrokerApp(@NonNull final Context context,
                                                                @NonNull final String brokerPackageName) throws ClientException {

        // Verify the signature to make sure that we're not binding to malicious apps.
        final BrokerValidator validator = new BrokerValidator(context);
        return new BrokerData(brokerPackageName, validator.verifySignatureAndThrow(brokerPackageName));
    }

    public static Set<BrokerData> getProdBrokers() {
        return PROD_BROKERS;
    }

    public static Set<BrokerData> getDebugBrokers() {
        return DEBUG_BROKERS;
    }

    public static Set<BrokerData> getAllBrokers() {
        return ALL_BROKERS;
    }
}
