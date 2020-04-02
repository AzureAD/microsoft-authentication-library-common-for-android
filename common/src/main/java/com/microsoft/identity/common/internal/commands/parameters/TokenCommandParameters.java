package com.microsoft.identity.common.internal.commands.parameters;

import android.util.Pair;

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
import lombok.NonNull;
import lombok.experimental.SuperBuilder;

@Getter
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
public class TokenCommandParameters extends CommandParameters {

    private static final String TAG = TokenCommandParameters.class.getSimpleName();

    private Set<String> scopes;

    @NonNull
    private Authority authority;

    private String claimsRequestJson;

    private List<Pair<String, String>> extraQueryStringParameters;

    //    @Singular("extraScopeToConsent")
    private List<String> extraScopesToConsent;

    private IAccountRecord account;

    private AbstractAuthenticationScheme authenticationScheme;

//    TokenCommandParameters(String correlationId, String applicationName, String applicationVersion, String requiredBrokerProtocolVersion, SdkType sdkType, String sdkVersion, Context androidApplicationContext, OAuth2TokenCache oAuth2TokenCache, boolean isSharedDevice, String clientId, String redirectUri, @NonNull Set<String> scopes, Authority authority, String claimsRequestJson, List<Pair<String, String>> extraQueryStringParameters, List<String> extraScopesToConsent, IAccountRecord account, AbstractAuthenticationScheme authenticationScheme) {
//        super(correlationId, applicationName, applicationVersion, requiredBrokerProtocolVersion, sdkType, sdkVersion, androidApplicationContext, oAuth2TokenCache, isSharedDevice, clientId, redirectUri);
//        this.scopes = Collections.unmodifiableSet(scopes);
//        this.authority = authority;
//        this.claimsRequestJson = claimsRequestJson;
//        this.extraQueryStringParameters = Collections.unmodifiableList(extraQueryStringParameters);
//        this.extraScopesToConsent = Collections.unmodifiableList(extraScopesToConsent);
//        this.account = account;
//        this.authenticationScheme = authenticationScheme;
//    }

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
