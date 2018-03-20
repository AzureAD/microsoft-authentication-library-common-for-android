package com.microsoft.identity.common;

import com.microsoft.identity.common.internal.cache.DefaultRefreshTokenCacheHelper;
import com.microsoft.identity.common.internal.dto.CredentialType;
import com.microsoft.identity.common.internal.dto.RefreshToken;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.Locale;

import static com.microsoft.identity.common.DefaultAccessTokenCacheHelperTest.CLIENT_ID;
import static com.microsoft.identity.common.DefaultAccessTokenCacheHelperTest.ENVIRONMENT;
import static com.microsoft.identity.common.DefaultAccessTokenCacheHelperTest.TARGET;
import static com.microsoft.identity.common.DefaultAccessTokenCacheHelperTest.UNIQUE_ID;
import static com.microsoft.identity.common.internal.cache.AbstractCacheHelper.CACHE_VALUE_SEPARATOR;
import static org.junit.Assert.assertEquals;

public class DefaultRefreshTokenCacheHelperTest {

    private static final String CREDENTIAL_TYPE = CredentialType.RefreshToken.name().toLowerCase(Locale.US);

    private DefaultRefreshTokenCacheHelper mDefaultMsalRefreshTokenCacheHelper;

    @Before
    public void setUp() {
        mDefaultMsalRefreshTokenCacheHelper = new DefaultRefreshTokenCacheHelper();
    }

    @Test
    public void createCacheKeyComplete() throws Exception {
        final RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUniqueId(UNIQUE_ID);
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setCredentialType(CredentialType.RefreshToken.name());
        refreshToken.setClientId(CLIENT_ID);
        refreshToken.setTarget(TARGET);

        final String expectedKey = "" // just for formatting
                + UNIQUE_ID + CACHE_VALUE_SEPARATOR
                + ENVIRONMENT + CACHE_VALUE_SEPARATOR
                + CREDENTIAL_TYPE + CACHE_VALUE_SEPARATOR
                + CLIENT_ID + CACHE_VALUE_SEPARATOR
                + TARGET;
        assertEquals(expectedKey, mDefaultMsalRefreshTokenCacheHelper.createCacheKey(refreshToken));
    }

    @Test
    public void createCacheKeyNoUniqueId() throws Exception {
        final RefreshToken refreshToken = new RefreshToken();
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setCredentialType(CredentialType.RefreshToken.name());
        refreshToken.setClientId(CLIENT_ID);
        refreshToken.setTarget(TARGET);

        final String expectedKey = "" // just for formatting
                + ENVIRONMENT + CACHE_VALUE_SEPARATOR
                + CREDENTIAL_TYPE + CACHE_VALUE_SEPARATOR
                + CLIENT_ID + CACHE_VALUE_SEPARATOR
                + TARGET;
        assertEquals(expectedKey, mDefaultMsalRefreshTokenCacheHelper.createCacheKey(refreshToken));
    }

    @Test
    public void createCacheKeyNoRealm() throws Exception {
        final RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUniqueId(UNIQUE_ID);
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setCredentialType(CredentialType.RefreshToken.name());
        refreshToken.setClientId(CLIENT_ID);
        refreshToken.setTarget(TARGET);

        final String expectedKey = "" // just for formatting
                + UNIQUE_ID + CACHE_VALUE_SEPARATOR
                + ENVIRONMENT + CACHE_VALUE_SEPARATOR
                + CREDENTIAL_TYPE + CACHE_VALUE_SEPARATOR
                + CLIENT_ID + CACHE_VALUE_SEPARATOR
                + TARGET;
        assertEquals(expectedKey, mDefaultMsalRefreshTokenCacheHelper.createCacheKey(refreshToken));
    }

    @Test
    public void createCacheKeyNoTarget() throws Exception {
        final RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUniqueId(UNIQUE_ID);
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setCredentialType(CredentialType.RefreshToken.name());
        refreshToken.setClientId(CLIENT_ID);

        final String expectedKey = "" // just for formatting
                + UNIQUE_ID + CACHE_VALUE_SEPARATOR
                + ENVIRONMENT + CACHE_VALUE_SEPARATOR
                + CREDENTIAL_TYPE + CACHE_VALUE_SEPARATOR
                + CLIENT_ID;

        assertEquals(expectedKey, mDefaultMsalRefreshTokenCacheHelper.createCacheKey(refreshToken));
    }

    @Test
    public void createCacheKeyNoUniqueIdNoTarget() throws Exception {
        final RefreshToken refreshToken = new RefreshToken();
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setCredentialType(CredentialType.RefreshToken.name());
        refreshToken.setClientId(CLIENT_ID);

        final String expectedKey = "" // just for formatting
                + ENVIRONMENT + CACHE_VALUE_SEPARATOR
                + CREDENTIAL_TYPE + CACHE_VALUE_SEPARATOR
                + CLIENT_ID;

        assertEquals(expectedKey, mDefaultMsalRefreshTokenCacheHelper.createCacheKey(refreshToken));
    }

    @Test
    public void getCacheValue() throws Exception {
        final RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUniqueId(UNIQUE_ID);
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setCredentialType(CredentialType.RefreshToken.name().toLowerCase(Locale.US));
        refreshToken.setClientId(CLIENT_ID);
        refreshToken.setTarget(TARGET);

        final String serializedValue = mDefaultMsalRefreshTokenCacheHelper.getCacheValue(refreshToken);

        // Turn the serialized value into a JSONObject and start testing field equality.
        final JSONObject jsonObject = new JSONObject(serializedValue);
        assertEquals(UNIQUE_ID, jsonObject.getString("unique_id"));
        assertEquals(ENVIRONMENT, jsonObject.getString("environment"));
        assertEquals(CredentialType.RefreshToken.name().toLowerCase(Locale.US), jsonObject.getString("credential_type"));
        assertEquals(CLIENT_ID, jsonObject.getString("client_id"));
        assertEquals(TARGET, jsonObject.getString("target"));
    }
}
