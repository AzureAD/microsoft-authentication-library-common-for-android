package com.microsoft.identity.common.internal.request;

import android.accounts.Account;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Pair;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.internal.authorities.Authority;
import com.microsoft.identity.common.internal.broker.BrokerRequest;
import com.microsoft.identity.common.internal.broker.BrokerValidator;
import com.microsoft.identity.common.internal.logging.DiagnosticContext;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.providers.oauth2.OpenIdConnectPromptParameter;
import com.microsoft.identity.common.internal.ui.AuthorizationAgent;
import com.microsoft.identity.common.internal.util.QueryParamsAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MsalBrokerRequestAdapter implements IBrokerRequestAdapter {

    private static final String TAG = MsalBrokerRequestAdapter.class.getName();

    @Override
    public BrokerRequest brokerRequestFromAcquireTokenParameters(@NonNull final AcquireTokenOperationParameters parameters) {

        Logger.verbose(TAG, "Constructing result bundle from AcquireTokenOperationParameters.");

        final BrokerRequest brokerRequest =  new BrokerRequest.Builder()
                .authority(parameters.getAuthority().getAuthorityURL().toString())
                .scope(TextUtils.join( " ", parameters.getScopes()))
                .redirect(parameters.getRedirectUri())
                .clientId(parameters.getClientId())
                .username(parameters.getLoginHint())
                .extraQueryStringParameter(
                        parameters.getExtraQueryStringParameters() != null ?
                                QueryParamsAdapter._toJson(parameters.getExtraQueryStringParameters())
                                : null
                ).prompt(parameters.getOpenIdConnectPromptParameter().name())
                .claims(parameters.getClaimsRequestJson())
                .forceRefresh(!TextUtils.isEmpty(parameters.getClaimsRequestJson()))
                .correlationId(DiagnosticContext.getRequestContext().get(DiagnosticContext.CORRELATION_ID))
                .applicationName(parameters.getApplicationName())
                .applicationVersion(parameters.getApplicationVersion())
                .msalVersion(parameters.getSdkVersion())
                .build();

        return brokerRequest;
    }

    @Override
    public BrokerRequest brokerRequestFromSilentOperationParameters(@NonNull final AcquireTokenSilentOperationParameters parameters) {

        Logger.verbose(TAG, "Constructing result bundle from AcquireTokenSilentOperationParameters.");

        final BrokerRequest brokerRequest =  new BrokerRequest.Builder()
                .authority(parameters.getAuthority().getAuthorityURL().toString())
                .scope(TextUtils.join( " ", parameters.getScopes()))
                .redirect(parameters.getRedirectUri())
                .clientId(parameters.getClientId())
                .homeAccountId(parameters.getAccount().getHomeAccountId())
                .localAccountId(parameters.getAccount().getLocalAccountId())
                .username(parameters.getAccount().getUsername())
                .claims(parameters.getClaimsRequestJson())
                .forceRefresh(parameters.getForceRefresh() || !TextUtils.isEmpty(parameters.getClaimsRequestJson()))
                .correlationId(DiagnosticContext.getRequestContext().get(DiagnosticContext.CORRELATION_ID))
                .applicationName(parameters.getApplicationName())
                .applicationVersion(parameters.getApplicationVersion())
                .msalVersion(parameters.getSdkVersion())
                .build();

        return brokerRequest;
    }

    @Override
    public BrokerAcquireTokenOperationParameters brokerInteractiveParametersFromActivity(
            @NonNull final Activity callingActivity) {

        Logger.verbose(TAG, "Constructing BrokerAcquireTokenOperationParameters from calling activity");

        final BrokerAcquireTokenOperationParameters parameters =
                new BrokerAcquireTokenOperationParameters();

        final Intent intent = callingActivity.getIntent();

        final BrokerRequest brokerRequest = (BrokerRequest) intent.getSerializableExtra(
                AuthenticationConstants.Broker.BROKER_REQUEST_V2);

        parameters.setActivity(callingActivity);

        parameters.setAppContext(callingActivity.getApplicationContext());

        parameters.setSdkType(SdkType.MSAL);

        int callingAppUid = intent.getIntExtra(
                AuthenticationConstants.Broker.CALLER_INFO_UID, 0
        );
        parameters.setCallerUId(callingAppUid);

        parameters.setCallerPackageName(brokerRequest.getApplicationName());

        parameters.setCallerAppVersion(brokerRequest.getApplicationVersion());

        List<Pair<String, String>> extraQP = new ArrayList<>();

        if (!TextUtils.isEmpty(brokerRequest.getExtraQueryStringParameter())) {
            extraQP = QueryParamsAdapter._fromJson(brokerRequest.getExtraQueryStringParameter());
            parameters.setExtraQueryStringParameters(extraQP);
        }

        parameters.setAuthority(
                AdalBrokerRequestAdapter.getRequestAuthorityWithExtraQP(
                        brokerRequest.getAuthority(),
                        extraQP
                )
        );

        parameters.setScopes(getScopesAsSet(brokerRequest.getScope()));

        parameters.setClientId(brokerRequest.getClientId());

        // set redirect uri using caller package name
        parameters.setRedirectUri(BrokerValidator.getBrokerRedirectUri(
                parameters.getAppContext(),
                parameters.getCallerPackageName())
        );

        parameters.setLoginHint(brokerRequest.getUserName());

        String correlationIdString = brokerRequest.getCorrelationId();

        if (TextUtils.isEmpty(correlationIdString)) {
            UUID correlationId = UUID.randomUUID();
            correlationIdString = correlationId.toString();
        }
        parameters.setCorrelationId(correlationIdString);

        parameters.setClaimsRequest(brokerRequest.getClaims());

        parameters.setOpenIdConnectPromptParameter(
                OpenIdConnectPromptParameter.valueOf(brokerRequest.getPrompt())
        );

        parameters.setAuthorizationAgent(AuthorizationAgent.WEBVIEW);

        return parameters;

    }

    @Override
    public BrokerAcquireTokenSilentOperationParameters brokerSilentParametersFromBundle(
            @NonNull final Bundle bundle,
            @NonNull final Context context,
            @NonNull final Account account) {

        Logger.verbose(TAG, "Constructing BrokerAcquireTokenSilentOperationParameters from result bundle");

        final BrokerRequest brokerRequest = (BrokerRequest) bundle.getSerializable(
                AuthenticationConstants.Broker.BROKER_REQUEST_V2
        );

        final BrokerAcquireTokenSilentOperationParameters parameters =
                new BrokerAcquireTokenSilentOperationParameters();

        parameters.setAppContext(context);

        parameters.setAccountManagerAccount(account);

        parameters.setSdkType(SdkType.MSAL);

        int callingAppUid = bundle.getInt(
                AuthenticationConstants.Broker.CALLER_INFO_UID
        );
        parameters.setCallerUId(callingAppUid);

        parameters.setCallerPackageName(brokerRequest.getApplicationName());

        parameters.setCallerAppVersion(brokerRequest.getApplicationVersion());

        final Authority authority = Authority.getAuthorityFromAuthorityUrl(
                brokerRequest.getAuthority()
        );
        parameters.setAuthority(authority);

        String correlationIdString = bundle.getString(
                brokerRequest.getCorrelationId()
        );
        if (TextUtils.isEmpty(correlationIdString)) {
            UUID correlationId = UUID.randomUUID();
            correlationIdString = correlationId.toString();
        }
        parameters.setCorrelationId(correlationIdString);

        parameters.setScopes(
                getScopesAsSet(brokerRequest.getScope())
        );

        parameters.setClientId(brokerRequest.getClientId());

        parameters.setForceRefresh(brokerRequest.getForceRefresh());

        parameters.setClaimsRequest(brokerRequest.getClaims());

        parameters.setLoginHint(brokerRequest.getUserName());

        if(!TextUtils.isEmpty(brokerRequest.getExtraQueryStringParameter())) {
            parameters.setExtraQueryStringParameters(
                    QueryParamsAdapter._fromJson(brokerRequest.getExtraQueryStringParameter())
            );
        }

        return parameters;
    }

    private Set<String> getScopesAsSet(final String scopeString) {
        if (TextUtils.isEmpty(scopeString)) {
            return new HashSet<>();
        }
        final String[] scopes = scopeString.split(" ");
        return new HashSet<>(Arrays.asList(scopes));
    }
}
