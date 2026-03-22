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

import com.microsoft.identity.common.java.authscheme.AbstractAuthenticationScheme;
import com.microsoft.identity.common.java.dto.IAccountRecord;
import com.microsoft.identity.common.java.exception.ArgumentException;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.exception.TerminalException;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.AzureActiveDirectory;
import com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.AzureActiveDirectoryCloud;

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

    private final IAccountRecord account;

    @NonNull
    private final AbstractAuthenticationScheme authenticationScheme;

    private final boolean forceRefresh;

    @NonNull
    @Override
    public String toUnsanitizedString() {
        return "AcquireTokenNoFixedScopesCommandParameters(account=" + account + ", authenticationScheme=" + getAuthenticationScheme() + ", forceRefresh=" + forceRefresh + ", authority=" + authority + ", challengeTypes=" + challengeType + ")";
    }

    @Override
    public boolean containsPii() {
        return !toString().equals(toUnsanitizedString());
    }

    @NonNull
    @Override
    public String toString() {
        return "AcquireTokenNoFixedScopesCommandParameters(authenticationScheme=" + getAuthenticationScheme() + ", forceRefresh=" + forceRefresh + ", authority=" + authority + ", challengeTypes=" + challengeType + ")";
    }

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
}
