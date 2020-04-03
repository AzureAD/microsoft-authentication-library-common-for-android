package com.microsoft.identity.common.internal.commands.parameters;

import android.util.Pair;

import com.google.gson.annotations.Expose;
import com.microsoft.identity.common.exception.ArgumentException;
import com.microsoft.identity.common.internal.authorities.Authority;
import com.microsoft.identity.common.internal.authscheme.AbstractAuthenticationScheme;
import com.microsoft.identity.common.internal.dto.IAccountRecord;
import com.microsoft.identity.common.internal.logging.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
public class TokenCommandParameters extends CommandParameters {

    private static final String TAG = TokenCommandParameters.class.getSimpleName();

    private List<Pair<String, String>> extraQueryStringParameters;

    private List<String> extraScopesToConsent;

    private IAccountRecord account;

    @Expose()
    private Set<String> scopes;

    @Expose()
    private Authority authority;

    @Expose()
    private String claimsRequestJson;

    @Expose()
    private AbstractAuthenticationScheme authenticationScheme;

    public void validate() throws ArgumentException {
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
        }
    }
}
