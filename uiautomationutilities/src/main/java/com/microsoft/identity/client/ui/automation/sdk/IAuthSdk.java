package com.microsoft.identity.client.ui.automation.sdk;

import com.microsoft.identity.client.ui.automation.TokenRequestTimeout;
import com.microsoft.identity.client.ui.automation.interaction.OnInteractionRequired;

/**
 * An interface describing methods of acquire token. Implementing this interface
 * on a test facilitates specifying the necessary parameters required to acquire
 * token either interactively or silently.
 */
public interface IAuthSdk {

    /**
     * Get the token that can be used to verify user and granted permissions.
     *
     * @return A resultant token or an exception occurred while acquiring token interactively
     */
    AuthResult acquireTokenInteractive(AuthTestParams authTestParams,
                                       OnInteractionRequired interactionRequiredCallback, TokenRequestTimeout tokenRequestTimeout);

    /**
     * Get the token that can be used to verify user and granted permissions.
     *
     * @return A resultant token or an exception occurred while acquiring token silently
     */
    AuthResult acquireTokenSilent(AuthTestParams authTestParams, TokenRequestTimeout tokenRequestTimeout);
}
