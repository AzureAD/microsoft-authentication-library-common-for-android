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

/**
 * Represents packageName and SignatureHash of a broker app.
 */
public class AppData {

    public static final AppData MICROSOFT_AUTHENTICATOR_DEBUG = new AppData(
            AZURE_AUTHENTICATOR_APP_PACKAGE_NAME,
            AZURE_AUTHENTICATOR_APP_DEBUG_SIGNATURE
    );

    public static final AppData MICROSOFT_AUTHENTICATOR_PROD = new AppData(
            AZURE_AUTHENTICATOR_APP_PACKAGE_NAME,
            AZURE_AUTHENTICATOR_APP_RELEASE_SIGNATURE
    );

    public static final AppData COMPANY_PORTAL = new AppData(
            COMPANY_PORTAL_APP_PACKAGE_NAME,
            COMPANY_PORTAL_APP_RELEASE_SIGNATURE
    );

    public static final AppData BROKER_HOST = new AppData(
            BROKER_HOST_APP_PACKAGE_NAME,
            BROKER_HOST_APP_SIGNATURE
    );

    private static final Set<AppData> DEBUG_BROKERS = Collections.unmodifiableSet(new HashSet<AppData>() {{
        add(MICROSOFT_AUTHENTICATOR_DEBUG);
        add(BROKER_HOST);
    }});

    private static final Set<AppData> PROD_BROKERS = Collections.unmodifiableSet(new HashSet<AppData>() {{
        add(MICROSOFT_AUTHENTICATOR_PROD);
        add(COMPANY_PORTAL);
    }});

    private static final Set<AppData> ALL_BROKERS = Collections.unmodifiableSet(new HashSet<AppData>() {{
        addAll(DEBUG_BROKERS);
        addAll(PROD_BROKERS);
    }});

    public final String packageName;
    public final String signatureHash;

    private AppData(@NonNull final String packageName,
                    @NonNull final String hash) {
        this.packageName = packageName;
        this.signatureHash = hash;
    }

    /**
     * Given a broker package name, verify its signature and return a AppData object.
     *
     * @throws ClientException an exception containing mismatch signature hashes as its error message.
     */
    public static @NonNull
    AppData getAppDataForBrokerApp(@NonNull final Context context,
                                      @NonNull final String brokerPackageName) throws ClientException {

        // Verify the signature to make sure that we're not binding to malicious apps.
        final BrokerValidator validator = new BrokerValidator(context);
        return new AppData(brokerPackageName, validator.verifySignatureAndThrow(brokerPackageName));
    }

    public static Set<AppData> getProdBrokers() {
        return PROD_BROKERS;
    }

    public static Set<AppData> getDebugBrokers() {
        return DEBUG_BROKERS;
    }

    public static Set<AppData> getAllBrokers() {
        return ALL_BROKERS;
    }
}
