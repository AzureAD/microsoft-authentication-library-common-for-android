package com.microsoft.identity.common.internal.request;

import android.accounts.Account;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.internal.broker.BrokerRequest;
import com.microsoft.identity.common.internal.logging.DiagnosticContext;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.util.QueryParamsAdapter;

public class MsalBrokerRequestAdapter implements IBrokerRequestAdapter {

    private static final String TAG = MsalBrokerRequestAdapter.class.getName();

    @Override
    public Bundle bundleFromAcquireTokenParameters(@NonNull final AcquireTokenOperationParameters parameters) {

        Logger.verbose(TAG, "Constructing result bundle from AcquireTokenOperationParameters.");

        final BrokerRequest brokerRequest =  new BrokerRequest.Builder()
                .authority(parameters.getAuthority().getAuthorityURL().toString())
                .scope(TextUtils.join( " ", parameters.getScopes()))
                .redirect(parameters.getRedirectUri())
                .clientId(parameters.getClientId())
                .username(parameters.getLoginHint())
                .extraQueryStringParameter(QueryParamsAdapter._toJson(parameters.getExtraQueryStringParameters()))
                .prompt(parameters.getOpenIdConnectPromptParameter().name())
                .claims(parameters.getClaimsRequestJson())
                .correlationId(DiagnosticContext.getRequestContext().get(DiagnosticContext.CORRELATION_ID))
                .applicationName(parameters.getApplicationName())
                .applicationVersion(parameters.getApplicationVersion())
                .msalVersion(parameters.getSdkVersion())
                .build();

        final Bundle requestBundle = new Bundle();
        requestBundle.putSerializable(AuthenticationConstants.Broker.BROKER_REQUEST_V2, brokerRequest);

        return requestBundle;
    }

    @Override
    public Bundle bundleFromSilentOperationParameters(@NonNull final AcquireTokenSilentOperationParameters parameters) {

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
                .forceRefresh(parameters.getForceRefresh())
                .correlationId(DiagnosticContext.getRequestContext().get(DiagnosticContext.CORRELATION_ID))
                .applicationName(parameters.getApplicationName())
                .applicationVersion(parameters.getApplicationVersion())
                .msalVersion(parameters.getSdkVersion())
                .build();

        final Bundle requestBundle = new Bundle();
        requestBundle.putSerializable(AuthenticationConstants.Broker.BROKER_REQUEST_V2, brokerRequest);

        return requestBundle;
    }

    @Override
    public BrokerAcquireTokenOperationParameters brokerParametersFromActivity(@NonNull final Activity callingActivity) {
        return null;
    }

    @Override
    public BrokerAcquireTokenSilentOperationParameters brokerSilentParametersFromBundle(@NonNull final Bundle bundle,
                                                                                        @NonNull final Context context,
                                                                                        @NonNull final Account account) {
        return null;
    }
}
