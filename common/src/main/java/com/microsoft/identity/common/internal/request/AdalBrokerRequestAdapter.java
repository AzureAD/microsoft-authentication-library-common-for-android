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

package com.microsoft.identity.common.internal.request;

import android.accounts.Account;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.internal.authorities.Authority;
import com.microsoft.identity.common.internal.authorities.AzureActiveDirectoryAudience;
import com.microsoft.identity.common.internal.authorities.AzureActiveDirectoryAuthority;
import com.microsoft.identity.common.internal.broker.BrokerRequest;
import com.microsoft.identity.common.internal.broker.BrokerValidator;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.migration.TokenCacheItemMigrationAdapter;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftAuthorizationRequest;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectorySlice;
import com.microsoft.identity.common.internal.providers.oauth2.OpenIdConnectPromptParameter;
import com.microsoft.identity.common.internal.result.AdalBrokerResultAdapter;
import com.microsoft.identity.common.internal.ui.AuthorizationAgent;
import com.microsoft.identity.common.internal.util.StringUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class AdalBrokerRequestAdapter implements IBrokerRequestAdapter {

    private static final String TAG = AdalBrokerResultAdapter.class.getName();

    @Override
    public BrokerRequest brokerRequestFromAcquireTokenParameters(AcquireTokenOperationParameters parameters) {
        throw new UnsupportedOperationException();
    }

    @Override
    public BrokerRequest brokerRequestFromSilentOperationParameters(AcquireTokenSilentOperationParameters parameters) {
        throw new UnsupportedOperationException();
    }

    @Override
    public BrokerAcquireTokenOperationParameters brokerInteractiveParametersFromActivity(@NonNull final Activity callingActivity) {

        final String methodName = "brokerInteractiveParametersFromActivity";
        Logger.verbose(
                TAG + methodName,
                "Constructing BrokerAcquireTokenOperationParameters from activity "
        );
        final BrokerAcquireTokenOperationParameters parameters =
                new BrokerAcquireTokenOperationParameters();

        final Intent intent = callingActivity.getIntent();

        parameters.setActivity(callingActivity);

        parameters.setAppContext(callingActivity.getApplicationContext());

        parameters.setSdkType(SdkType.ADAL);

        int callingAppUid = intent.getIntExtra(
                AuthenticationConstants.Broker.CALLER_INFO_UID, 0
        );
        parameters.setCallerUId(callingAppUid);

        // There are two constants that need to be checked for the presence of the caller pkg name:
        // 1. CALLER_INFO_PACKAGE
        // 2. APP_PACKAGE_NAME
        //
        // But wait! There are also versions of the ADAL library (Android) that did not send this value
        // in those cases, we simply 'lie' and say that the request came from **current** execution
        // context. This will not always be correct. We'll set a flag here to signal when the param
        // is used.
        final boolean callerPackageNameProvided = packageNameWasProvidedInBundle(intent.getExtras());

        parameters.setCallerPackageName(
                getPackageNameFromBundle(
                        intent.getExtras(),
                        callingActivity.getApplicationContext()
                )
        );
        parameters.setCallerAppVersion(intent.getStringExtra(
                AuthenticationConstants.AAD.APP_VERSION)
        );

        final List<Pair<String, String>> extraQP = getExtraQueryParamAsList(
                intent.getStringExtra(AuthenticationConstants.Broker.ACCOUNT_EXTRA_QUERY_PARAM)
        );

        final AzureActiveDirectoryAuthority authority = getRequestAuthorityWithExtraQP(
                intent.getStringExtra(AuthenticationConstants.Broker.ACCOUNT_AUTHORITY),
                extraQP
        );
        // V1 endpoint always add an organizational account if the tenant id is common.
        // We need to explicitly add tenant id as organizations if we want similar behavior from V2 endpoint

        if (authority.getAudience().getTenantId().equalsIgnoreCase(AzureActiveDirectoryAudience.ALL)) {
            authority.getAudience().setTenantId(AzureActiveDirectoryAudience.ORGANIZATIONS);
        }
        parameters.setAuthority(authority);

        parameters.setExtraQueryStringParameters(extraQP);

        String resource = intent.getStringExtra(AuthenticationConstants.Broker.ACCOUNT_RESOURCE);
        Set<String> scopes = new HashSet<>();
        scopes.add(TokenCacheItemMigrationAdapter.getScopeFromResource(resource));
        parameters.setScopes(scopes);

        parameters.setClientId(intent.getStringExtra(
                AuthenticationConstants.Broker.ACCOUNT_CLIENTID_KEY)
        );

        // If the caller package name was provided, compute their redirect
        if (callerPackageNameProvided) {
            // V1 Broker would compute the redirect_uri for the calling package, rather than
            // 'trust' the provided value -- this had the unfortunate consequence of allowing
            // callers to pass non-URL-encoded signature hashes into the library despite the documentation
            // prescribing otherwise. The ADAL.NET implementation unfortunately RELIES on this behavior,
            // forcing customers to use non-encoded values in order to pass validation check inside of
            // ADAL.NET. In order to not regress this experience, the redirect URI must now be computed
            // meaning that the ACCOUNT_REDIRECT parameter is basically ignored.
            parameters.setRedirectUri(
                    BrokerValidator.getBrokerRedirectUri(
                            callingActivity,
                            parameters.getCallerPackageName()
                    )
            );
        } else {
            // The caller's package name was not provided, so we cannot compute the redirect for them.
            // In this case, use the provided value...
            parameters.setRedirectUri(
                    intent.getStringExtra(AuthenticationConstants.Broker.ACCOUNT_REDIRECT)
            );
        }

        parameters.setLoginHint(intent.getStringExtra(AuthenticationConstants.Broker.ACCOUNT_NAME));

        String correlationIdString = intent.getStringExtra(
                AuthenticationConstants.Broker.ACCOUNT_CORRELATIONID
        );
        if (TextUtils.isEmpty(correlationIdString)) {
            Logger.info(TAG, "Correlation id not set by Adal, creating a new one");
            UUID correlationId = UUID.randomUUID();
            correlationIdString = correlationId.toString();
        }
        parameters.setCorrelationId(correlationIdString);

        parameters.setClaimsRequest(intent.getStringExtra(
                AuthenticationConstants.Broker.ACCOUNT_CLAIMS)
        );

        parameters.setOpenIdConnectPromptParameter(
                OpenIdConnectPromptParameter._fromPromptBehavior(
                        intent.getStringExtra(AuthenticationConstants.Broker.ACCOUNT_PROMPT)
                )
        );

        parameters.setAuthorizationAgent(AuthorizationAgent.WEBVIEW);

        return parameters;
    }

    @Override
    public BrokerAcquireTokenSilentOperationParameters brokerSilentParametersFromBundle(Bundle bundle,
                                                                                        Context context,
                                                                                        Account account) {
        final String methodName = ":brokerSilentParametersFromBundle";
        Logger.verbose(
                TAG + methodName,
                "Constructing BrokerAcquireTokenOperationParameters from activity "
        );
        final BrokerAcquireTokenSilentOperationParameters parameters =
                new BrokerAcquireTokenSilentOperationParameters();

        parameters.setAppContext(context);

        parameters.setAccountManagerAccount(account);

        parameters.setSdkType(SdkType.ADAL);

        int callingAppUid = bundle.getInt(
                AuthenticationConstants.Broker.CALLER_INFO_UID
        );
        parameters.setCallerUId(callingAppUid);

        // There are two constants that need to be checked for the presence of the caller pkg name:
        // 1. CALLER_INFO_PACKAGE
        // 2. APP_PACKAGE_NAME
        //
        // But wait! There are also versions of the ADAL library (Android) that did not send this value
        // in those cases, we simply 'lie' and say that the request came from **current** execution
        // context. This will not always be correct. We'll set a flag here to signal when the param
        // is used.
        final boolean callerPackageNameProvided = packageNameWasProvidedInBundle(bundle);

        final String packageName = getPackageNameFromBundle(bundle, context);
        parameters.setCallerPackageName(packageName);

        parameters.setCallerAppVersion(
                bundle.getString(AuthenticationConstants.AAD.APP_VERSION)
        );

        final Authority authority = Authority.getAuthorityFromAuthorityUrl(
                bundle.getString(AuthenticationConstants.Broker.ACCOUNT_AUTHORITY));
        parameters.setAuthority(authority);

        String correlationIdString = bundle.getString(
                AuthenticationConstants.Broker.ACCOUNT_CORRELATIONID
        );
        if (TextUtils.isEmpty(correlationIdString)) {
            Logger.info(TAG, "Correlation id not set by Adal, creating a new one");
            UUID correlationId = UUID.randomUUID();
            correlationIdString = correlationId.toString();
        }
        parameters.setCorrelationId(correlationIdString);

        String resource = bundle.getString(
                AuthenticationConstants.Broker.ACCOUNT_RESOURCE
        );
        Set<String> scopes = new HashSet<>();
        scopes.add(TokenCacheItemMigrationAdapter.getScopeFromResource(resource));
        parameters.setScopes(scopes);

        final String clientId = bundle.getString(
                AuthenticationConstants.Broker.ACCOUNT_CLIENTID_KEY
        );
        parameters.setClientId(clientId);

        parameters.setLocalAccountId(
                bundle.getString(
                        AuthenticationConstants.Broker.ACCOUNT_USERINFO_USERID
                )
        );

        String redirectUri = bundle.getString(AuthenticationConstants.Broker.ACCOUNT_REDIRECT);
        // Adal might not pass in the redirect uri, in that case calculate from broker validator
        if (callerPackageNameProvided || TextUtils.isEmpty(redirectUri)) {
            redirectUri = BrokerValidator.getBrokerRedirectUri(context, packageName);
        }
        parameters.setRedirectUri(redirectUri);

        parameters.setForceRefresh(Boolean.parseBoolean(
                bundle.getString(AuthenticationConstants.Broker.BROKER_FORCE_REFRESH))
        );

        parameters.setClaimsRequest(
                bundle.getString(AuthenticationConstants.Broker.ACCOUNT_CLAIMS)
        );

        parameters.setLoginHint(
                bundle.getString(AuthenticationConstants.Broker.ACCOUNT_NAME)
        );

        final List<Pair<String, String>> extraQP = getExtraQueryParamAsList(
                bundle.getString(AuthenticationConstants.Broker.ACCOUNT_EXTRA_QUERY_PARAM)
        );
        parameters.setExtraQueryStringParameters(extraQP);

        return parameters;
    }

    private boolean packageNameWasProvidedInBundle(@Nullable final Bundle bundle) {
        if (null == bundle) {
            return false;
        }

        final String callerInfoPkg = bundle.getString(AuthenticationConstants.Broker.CALLER_INFO_PACKAGE);
        final String appPkgName = bundle.getString(AuthenticationConstants.AAD.APP_PACKAGE_NAME);

        // If either value is non-null/empty, then the param was populated
        return !TextUtils.isEmpty(callerInfoPkg) || !TextUtils.isEmpty(appPkgName);
    }

    private String getPackageNameFromBundle(final Bundle bundle, final Context context) {
        String packageName = bundle.getString(AuthenticationConstants.Broker.CALLER_INFO_PACKAGE);
        if (TextUtils.isEmpty(packageName)) {
            packageName = bundle.getString(AuthenticationConstants.AAD.APP_PACKAGE_NAME);
            if (TextUtils.isEmpty(packageName)) {
                Logger.warn(TAG, "Caller package name not set by app, getting from context");
                packageName = context.getPackageName();
            }
        }
        return packageName;
    }

    /**
     * Helper to get Extra QP as a List (V2 format) from String (adal format)
     */
    private List<Pair<String, String>> getExtraQueryParamAsList(@Nullable final String extraQueryParamString) {
        final List<Pair<String, String>> extraQPList = new ArrayList<>();
        if (!StringUtil.isEmpty(extraQueryParamString)) {
            final String[] extraQueryParams = extraQueryParamString.split("&");

            for (final String param : extraQueryParams) {
                if (!StringUtil.isEmpty(param)) {
                    String[] split = param.split("=");
                    final String name = split[0];
                    final String value = (split.length > 1) ? split[1] : null;
                    final Pair<String, String> extraQPPair = new Pair<>(name, value);
                    extraQPList.add(extraQPPair);
                }
            }
        }
        return extraQPList;
    }


    /**
     * TODO : Refactor to remove this code and move the logic to better place
     */
    public static AzureActiveDirectoryAuthority getRequestAuthorityWithExtraQP(final String authority,
                                                                               final List<Pair<String, String>> extraQP) {

        final AzureActiveDirectoryAuthority requestAuthority
                = (AzureActiveDirectoryAuthority) Authority.getAuthorityFromAuthorityUrl(authority);

        if (extraQP != null) {
            AzureActiveDirectorySlice slice = new AzureActiveDirectorySlice();
            List<Pair<String, String>> extraQPListCopy = new ArrayList<>(extraQP);

            for (Pair<String, String> parameter : extraQPListCopy) {

                if (StringUtil.isEmpty(parameter.first)) {
                    Logger.warn(TAG, "The extra query parameter.first is empty.");
                } else if (parameter.first.equalsIgnoreCase(MicrosoftAuthorizationRequest.INSTANCE_AWARE)) {
                    Logger.info(TAG,

                            "Set the extra query parameter mMultipleCloudAware" +
                                    " for MicrosoftStsAuthorizationRequest."
                    );

                    Logger.infoPII(
                            TAG,
                            "Set the mMultipleCloudAware to " +
                                    (parameter.second == null ? "null" : parameter.second)
                    );

                    requestAuthority.mMultipleCloudsSupported =
                            null != parameter.second &&
                                    parameter.second.equalsIgnoreCase(Boolean.TRUE.toString());

                    extraQP.remove(parameter);

                } else if (parameter.first.equalsIgnoreCase(AzureActiveDirectorySlice.SLICE_PARAMETER)) {
                    slice.setSlice(parameter.second);
                    extraQP.remove(parameter);
                } else if (parameter.first.equalsIgnoreCase(AzureActiveDirectorySlice.DC_PARAMETER)) {
                    slice.setDataCenter(parameter.second);
                    extraQP.remove(parameter);
                }
            }

            Logger.verbose(TAG, "Set the extra query parameter mSlice" +
                    " for MicrosoftStsAuthorizationRequest."
            );
            Logger.verbosePII(TAG, "Set the mSlice to " + slice.toString());

            requestAuthority.mSlice = slice;
        }

        return requestAuthority;
    }
}
