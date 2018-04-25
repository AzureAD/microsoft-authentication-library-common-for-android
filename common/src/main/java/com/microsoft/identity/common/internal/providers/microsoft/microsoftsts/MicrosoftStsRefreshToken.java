package com.microsoft.identity.common.internal.providers.microsoft.microsoftsts;

import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.exception.ServiceException;
import com.microsoft.identity.common.internal.cache.SchemaUtil;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.ClientInfo;
import com.microsoft.identity.common.internal.providers.oauth2.IDToken;
import com.microsoft.identity.common.internal.providers.oauth2.RefreshToken;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public class MicrosoftStsRefreshToken extends RefreshToken {

    private boolean mIsFamilyRefreshToken;
    private ClientInfo mClientInfo;
    private IDToken mIdToken;
    private String mClientId;
    private String mScope;
    private Date mExpiresOn;
    private String mFamilyId;

    public MicrosoftStsRefreshToken(MicrosoftStsTokenResponse response) {
        super(response);
        try {
            mClientInfo = new ClientInfo(response.getClientInfo());
            mIdToken = new IDToken(response.getIdToken());
            mClientId = response.getClientId();
            mScope = response.getScope();
            mExpiresOn = response.getExpiresOn();
            mFamilyId = response.getFamilyId();
            mIsFamilyRefreshToken = !StringExtensions.isNullOrBlank(mFamilyId);
        } catch (ServiceException e) {
            // TODO handle this properly
            throw new RuntimeException(e);
        }
    }

    public boolean getIsFamilyRefreshToken() {
        return mIsFamilyRefreshToken;
    }

    @Override
    public String getUniqueUserId() {
        return SchemaUtil.getUniqueId(mClientInfo);
    }

    @Override
    public String getEnvironment() {
        return SchemaUtil.getEnvironment(mIdToken);
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
