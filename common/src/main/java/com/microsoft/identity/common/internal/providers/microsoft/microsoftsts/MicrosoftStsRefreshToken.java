package com.microsoft.identity.common.internal.providers.microsoft.microsoftsts;

import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.internal.cache.MicrosoftStsAccountCredentialAdapter;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftIdToken;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.ClientInfo;
import com.microsoft.identity.common.internal.providers.oauth2.IDToken;
import com.microsoft.identity.common.internal.providers.oauth2.RefreshToken;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class MicrosoftStsRefreshToken extends RefreshToken {

    private ClientInfo mClientInfo;
    private IDToken mIdToken;
    private String mClientId;
    private String mScope;
    private Date mExpiresOn;
    private String mFamilyId;

    public MicrosoftStsRefreshToken(MicrosoftStsTokenResponse response) {
        super(response);
        mClientInfo = new ClientInfo(response.getClientInfo());
        mIdToken = new IDToken(response.getIdToken());
        mClientId = response.getClientId();
        mScope = response.getScope();
        mExpiresOn = response.getExpiresOn();
        mFamilyId = response.getFamilyId();
    }

    @Override
    public String getUniqueUserId() {
        // TODO refactor
        return MicrosoftStsAccountCredentialAdapter.formatUniqueId(mClientInfo);
    }

    @Override
    public String getEnvironment() {
        // TODO see AzureActiveDirectoryAccount#getEnvironment
        // there's opportunity for code sharing here
        String environment = null;

        if (null != mIdToken && null != mIdToken.getTokenClaims()) {
            environment = mIdToken.getTokenClaims().get(MicrosoftIdToken.ISSUER);
            if (!StringExtensions.isNullOrBlank(environment)) {
                try {
                    environment = new URL(environment).getHost();
                } catch (MalformedURLException e) {
                    // TODO log an error
                }
            }
        }

        return environment;
    }

    @Override
    public String getClientId() {
        return mClientId;
    }

    @Override
    public String getSecret() {
        return getRefreshToken();
    }

    @Override
    public String getTarget() {
        return mScope;
    }

    @Override
    public String getExpiresOn() {
        // TODO see AzureActiveDirectoryRefreshToken (code dupe)
        String expiresOn = null;

        if (null != mExpiresOn) {
            final long millis = mExpiresOn.getTime();
            final long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);
            expiresOn = String.valueOf(seconds);
        }

        return expiresOn;
    }

    @Override
    public String getFamilyId() {
        return mFamilyId;
    }
}
