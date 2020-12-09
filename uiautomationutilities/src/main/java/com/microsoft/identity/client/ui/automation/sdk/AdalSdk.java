package com.microsoft.identity.client.ui.automation.sdk;

import android.content.Context;

import androidx.annotation.NonNull;

import com.microsoft.aad.adal.AuthenticationCallback;
import com.microsoft.aad.adal.AuthenticationContext;
import com.microsoft.aad.adal.AuthenticationResult;
import com.microsoft.identity.client.ui.automation.interaction.OnInteractionRequired;

import java.util.Map;
import java.util.TreeMap;

/**
 * A Sdk wrapper for Azure Active Directory Authentication Library (ADAL) which implements
 * both the methods of acquire token interactively and silently and returns back the
 * AuthResult, ADAL tests can leverage this sdk for acquiring token with specific
 * parameters and get back the final result.
 */
public class AdalSdk implements IAuthSdk {

    protected Map<String, String> upnUserIdMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    @Override
    public AuthResult acquireTokenInteractive(@NonNull final AuthTestParams authTestParams,
                                              OnInteractionRequired interactionRequiredCallback) {
        final AuthenticationContext authenticationContext = createAuthContext(
                authTestParams.getActivity(),
                authTestParams.getAuthority()
        );

        final ResultFuture<AuthenticationResult, Exception> future = new ResultFuture<>();

        authenticationContext.acquireToken(
                authTestParams.getActivity(), authTestParams.getResource(),
                authTestParams.getClientId(), authTestParams.getRedirectUri(),
                authTestParams.getLoginHint(),
                authTestParams.getPromptParameter(), authTestParams.getExtraQueryParameters(),
                getAuthenticationCallback(future)
        );

        if(interactionRequiredCallback!=null) {
            interactionRequiredCallback.handleUserInteraction();
        }

        try {
            final AuthenticationResult result = future.get();
            return new AuthResult(result);
        } catch (Exception exception) {
            return new AuthResult(exception);
        }
    }

    @Override
    public AuthResult acquireTokenSilent(AuthTestParams authTestParams) {
        final AuthenticationContext authenticationContext = createAuthContext(
                authTestParams.getActivity(),
                authTestParams.getAuthority()
        );

        final ResultFuture<AuthenticationResult, Exception> future = new ResultFuture<>();

        authenticationContext.acquireTokenSilentAsync(
                authTestParams.getResource(), authTestParams.getClientId(),
                authTestParams.getLoginHint(), null,
                getAuthenticationCallback(future)
        );

        try {
            final AuthenticationResult result = future.get();
            return new AuthResult(result);
        } catch (Exception exception) {
            return new AuthResult(exception);
        }
    }

    private AuthenticationContext createAuthContext(final Context context, final String authority) {
        return new AuthenticationContext(context, authority, true);
    }

    private AuthenticationCallback getAuthenticationCallback(
            final ResultFuture<AuthenticationResult, Exception> future) {
        return new AuthenticationCallback<AuthenticationResult>() {
            @Override
            public void onSuccess(final AuthenticationResult authenticationResult) {
                upnUserIdMap.put(
                        authenticationResult.getUserInfo().getDisplayableId(),
                        authenticationResult.getUserInfo().getUserId()
                );
                future.setResult(authenticationResult);
            }

            @Override
            public void onError(final Exception e) {
                future.setException(e);
            }
        };
    }
}
