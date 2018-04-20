package com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory;

import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.internal.cache.MicrosoftStsAccountCredentialAdapter;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftIdToken;
import com.microsoft.identity.common.internal.providers.oauth2.IDToken;
import com.microsoft.identity.common.internal.providers.oauth2.RefreshToken;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class AzureActiveDirectoryRefreshToken extends RefreshToken {

    private boolean mIsFamilyRefreshToken;
    private String mFamilyId;
    private Date mExpiresOn;
    private ClientInfo mClientInfo;
    private IDToken mIdToken;
    private String mScope;
    private String mClientId;

    public AzureActiveDirectoryRefreshToken(AzureActiveDirectoryTokenResponse response) {
        super(response);
        mFamilyId = response.getFamilyId();
        mIsFamilyRefreshToken = !StringExtensions.isNullOrBlank(mFamilyId);
        mExpiresOn = response.mExpiresOn;
        mClientInfo = new ClientInfo(response.getClientInfo());
        mIdToken = new IDToken(response.getIdToken());
        mScope = response.getScope();
        mClientId = response.getClientId();
    }

    public boolean getIsFamilyRefreshToken() {
        return mIsFamilyRefreshToken;
    }

    @Override
    public String getFamilyId() {
        return mFamilyId;
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
        String expiresOn = null;

        if (null != mExpiresOn) {
            final long millis = mExpiresOn.getTime();
            final long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);
            expiresOn = String.valueOf(seconds);
        }

        return expiresOn;
    }

}
