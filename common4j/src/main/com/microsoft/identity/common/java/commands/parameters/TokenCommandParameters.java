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
package com.microsoft.identity.common.java.commands.parameters;

import static com.microsoft.identity.common.java.AuthenticationConstants.Broker.LOOKUP_MODE_VALUE;
import static com.microsoft.identity.common.java.AuthenticationConstants.Broker.NATIVEBROKER_KEY;
import static com.microsoft.identity.common.java.AuthenticationConstants.Broker.NATIVEBROKER_MODE_KEY;
import static com.microsoft.identity.common.java.AuthenticationConstants.Broker.NATIVEBROKER_VALUE;

import com.google.gson.annotations.Expose;
import com.microsoft.identity.common.java.exception.ArgumentException;
import com.microsoft.identity.common.java.authorities.Authority;
import com.microsoft.identity.common.java.authscheme.AbstractAuthenticationScheme;
import com.microsoft.identity.common.java.dto.IAccountRecord;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.logging.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
public class TokenCommandParameters extends CommandParameters {

    private static final String TAG = TokenCommandParameters.class.getSimpleName();

    private final IAccountRecord account;

    @Expose()
    private final Set<String> scopes;

    @Expose()
    private final Authority authority;

    @Expose()
    private final String claimsRequestJson;

    @Expose()
    private final AbstractAuthenticationScheme authenticationScheme;

    @Expose()
    private final String mamEnrollmentId;

    @Expose()
    private final boolean forceRefresh;

    private final String loginHint;

    private final String domainHint;

    private final List<Map.Entry<String, String>> extraOptions;

    // Only put in the token request body
    private final List<Map.Entry<String, String>> extraTokenBodyParameters;

    public Set<String> getScopes() {
        return this.scopes == null ? null : new HashSet<>(this.scopes);
    }

    public List<Map.Entry<String, String>> getExtraTokenBodyParameters() {
        return this.extraTokenBodyParameters == null ? null : new ArrayList<>(this.extraTokenBodyParameters);
    }

    public String getMamEnrollmentId(){
        return mamEnrollmentId;
    }

    public void validate() throws ArgumentException, ClientException {
        final String methodName = ":validate";

        Logger.verbose(
                TAG + methodName,
                "Validating operation params..."
        );

        boolean validScopeArgument = false;

        if (scopes != null) {
            scopes.removeAll(Arrays.asList("", null));
            if (scopes.size() > 0) {
                validScopeArgument = true;
            }
        }

        if (!validScopeArgument) {
            if (this instanceof SilentTokenCommandParameters) {
                throw new ArgumentException(
                        ArgumentException.ACQUIRE_TOKEN_SILENT_OPERATION_NAME,
                        ArgumentException.SCOPE_ARGUMENT_NAME,
                        "scope is empty or null"
                );
            }

            if (this instanceof InteractiveTokenCommandParameters) {
                throw new ArgumentException(
                        ArgumentException.ACQUIRE_TOKEN_OPERATION_NAME,
                        ArgumentException.SCOPE_ARGUMENT_NAME,
                        "scope is empty or null");
            }

            if (this instanceof DeviceCodeFlowCommandParameters) {
                throw new ArgumentException(
                        ArgumentException.ACQUIRE_TOKEN_WITH_DEVICE_CODE_OPERATION_NAME,
                        ArgumentException.SCOPE_ARGUMENT_NAME,
                        "scope is empty or null");
            }
        }

        // AuthenticationScheme is present...
        if (null == authenticationScheme) {
            if (this instanceof SilentTokenCommandParameters) {
                throw new ArgumentException(
                        ArgumentException.ACQUIRE_TOKEN_SILENT_OPERATION_NAME,
                        ArgumentException.AUTHENTICATION_SCHEME_ARGUMENT_NAME,
                        "authentication scheme is undefined"
                );
            }

            if (this instanceof InteractiveTokenCommandParameters) {
                throw new ArgumentException(
                        ArgumentException.ACQUIRE_TOKEN_OPERATION_NAME,
                        ArgumentException.AUTHENTICATION_SCHEME_ARGUMENT_NAME,
                        "authentication scheme is undefined"
                );
            }

            if (this instanceof DeviceCodeFlowCommandParameters) {
                throw new ArgumentException(
                        ArgumentException.ACQUIRE_TOKEN_WITH_DEVICE_CODE_OPERATION_NAME,
                        ArgumentException.AUTHENTICATION_SCHEME_ARGUMENT_NAME,
                        "authentication scheme is undefined"
                );
            }
        }
    }

    /**
     * Checks if the request is for ESTS' lookup mode.
     * In lookup mode, access token, id token, and scope are all set to "none".
     * This is a special response from ESTS when extra query parameters are sent to indicate a token lookup request.
     *
     * @return true if in lookup mode, false otherwise
     */
    public boolean isLookupMode() {
        if (extraTokenBodyParameters == null) return false;
        boolean hasNativeBrokerIndicator = false;
        boolean hasLookupModeIndicator = false;
        for (final Map.Entry<String, String> entry : extraTokenBodyParameters) {
            if (NATIVEBROKER_KEY.equals(entry.getKey())
                    && NATIVEBROKER_VALUE.equals(entry.getValue())) {
                hasNativeBrokerIndicator = true;
            }
            if (NATIVEBROKER_MODE_KEY.equals(entry.getKey())
                    && LOOKUP_MODE_VALUE.equals(entry.getValue())) {
                hasLookupModeIndicator = true;
            }
        }

        return hasNativeBrokerIndicator
                && hasLookupModeIndicator;
    }
}
