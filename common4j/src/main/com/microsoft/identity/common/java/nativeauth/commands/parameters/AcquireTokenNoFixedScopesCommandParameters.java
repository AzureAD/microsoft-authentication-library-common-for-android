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
package com.microsoft.identity.common.java.nativeauth.commands.parameters;

import com.google.gson.annotations.Expose;
import com.microsoft.identity.common.java.authscheme.AbstractAuthenticationScheme;
import com.microsoft.identity.common.java.dto.IAccountRecord;
import com.microsoft.identity.common.java.exception.ArgumentException;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.exception.TerminalException;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.AzureActiveDirectory;
import com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.AzureActiveDirectoryCloud;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;

/**
 * AcquireTokenNoFixedScopesCommandParameters defines the parameters used for
 * [AcquireTokenNoFixedScopesCommand] class.
 */
@Getter
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
public class AcquireTokenNoFixedScopesCommandParameters extends BaseNativeAuthCommandParameters {

    private static final String TAG = AcquireTokenNoFixedScopesCommandParameters.class.getSimpleName();

    private static final Object sLock = new Object();

    private final IAccountRecord account;

    @Expose()
    @NonNull
    private final AbstractAuthenticationScheme authenticationScheme;

    @Expose()
    private final boolean forceRefresh;

    private final String loginHint;

    private final List<Map.Entry<String, String>> extraOptions;

    /**
     * Validates the command parameters in this object are consistent and can be used for
     * command execution.
     * @throws ArgumentException
     */
    public void validate() throws ArgumentException {
        final String methodName = ":validate";

        Logger.verbose(
                TAG + methodName,
                "Validating operation params..."
        );

        // AuthenticationScheme is present...
        if (null == authenticationScheme) {
            throw new ArgumentException(
                    ArgumentException.ACQUIRE_TOKEN_NO_FIXED_SCOPE_OPERATION_NAME,
                    ArgumentException.AUTHENTICATION_SCHEME_ARGUMENT_NAME,
                    "authentication scheme is undefined"
            );
        }

        if (getAccount() == null) {
            Logger.warn(TAG, "The account set on silent operation parameters is NULL.");
            // if the authority is B2C, then we do not need check if matches with the account environment
            // as B2C only exists in one cloud and can use custom domains
            // This logic should also apply to CIAM authorities
        }
    }

    /**
     * Note - this method may throw a variety of RuntimeException if we cannot perform cloud
     * discovery to determine the set of cloud aliases.
     * @return true if the authority matches the cloud environment that the account is homed in.
     */
    private boolean authorityMatchesAccountEnvironment() {
        final String methodName = ":authorityMatchesAccountEnvironment";

        final Exception cause;
        final String errorCode;

        try {
            if (!AzureActiveDirectory.isInitialized()) {
                performCloudDiscovery();
            }
            final AzureActiveDirectoryCloud cloud = AzureActiveDirectory.getAzureActiveDirectoryCloudFromHostName(getAccount().getEnvironment());
            return cloud != null && cloud.getPreferredNetworkHostName().equals(getAuthority().getAuthorityURL().getAuthority());
        } catch (final IOException e) {
            cause = e;
            errorCode = ClientException.IO_ERROR;
        } catch (final URISyntaxException e) {
            cause = e;
            errorCode = ClientException.MALFORMED_URL;
        }

        Logger.error(
                TAG + methodName,
                "Unable to perform cloud discovery",
                cause);
        throw new TerminalException(
                "Unable to perform cloud discovery in order to validate request authority",
                cause,
                errorCode);
    }

    private static void performCloudDiscovery()
            throws IOException, URISyntaxException {
        final String methodName = ":performCloudDiscovery";
        Logger.verbose(
                TAG + methodName,
                "Performing cloud discovery..."
        );
        synchronized (sLock) {
            AzureActiveDirectory.performCloudDiscovery();
        }
    }
}
