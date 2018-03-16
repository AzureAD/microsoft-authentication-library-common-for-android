package com.microsoft.identity.common;

import com.microsoft.identity.common.internal.cache.DefaultMsalAccessTokenCacheHelper;
import com.microsoft.identity.common.internal.dto.AccessToken;
import com.microsoft.identity.common.internal.dto.CredentialType;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.Locale;

import static com.microsoft.identity.common.internal.cache.DefaultMsalAccessTokenCacheHelper.CACHE_VALUE_SEPARATOR;
import static org.junit.Assert.assertEquals;

public class DefaultMsalAccessTokenCacheHelperTest {

    static final String UNIQUE_ID = "29f3807a-4fb0-42f2-a44a-236aa0cb3f97.0287f963-2d72-4363-9e3a-5705c5b0f031";
    static final String ENVIRONMENT = "login.microsoftonline.com";
    static final String CLIENT_ID = "0287f963-2d72-4363-9e3a-5705c5b0f031";
    static final String TARGET = "user.read user.write https://graph.windows.net";

    private static final String CREDENTIAL_TYPE = CredentialType.AccessToken.name().toLowerCase(Locale.US);
    private static final String REALM = "3c62ac97-29eb-4aed-a3c8-add0298508d";

    private DefaultMsalAccessTokenCacheHelper mDefaultMsalAccessTokenCacheHelper;

    @Before
    public void setUp() {
        mDefaultMsalAccessTokenCacheHelper = new DefaultMsalAccessTokenCacheHelper();
    }

    @Test
    public void createCacheKeyComplete() throws Exception {
        final AccessToken accessToken = new AccessToken();
        accessToken.setUniqueId(UNIQUE_ID);
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setClientId(CLIENT_ID);
        accessToken.setRealm(REALM);
        accessToken.setTarget(TARGET);

        final String expectedKey = "" // just for formatting
                + UNIQUE_ID + CACHE_VALUE_SEPARATOR
                + ENVIRONMENT + CACHE_VALUE_SEPARATOR
                + CREDENTIAL_TYPE + CACHE_VALUE_SEPARATOR
                + CLIENT_ID + CACHE_VALUE_SEPARATOR
                + REALM + CACHE_VALUE_SEPARATOR
                + TARGET;
        assertEquals(expectedKey, mDefaultMsalAccessTokenCacheHelper.createCacheKey(accessToken));
    }

    @Test
    public void createCacheKeyNoUniqueId() throws Exception {
        final AccessToken accessToken = new AccessToken();
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setClientId(CLIENT_ID);
        accessToken.setRealm(REALM);
        accessToken.setTarget(TARGET);

        final String expectedKey = "" // just for formatting
                + ENVIRONMENT + CACHE_VALUE_SEPARATOR
                + CREDENTIAL_TYPE + CACHE_VALUE_SEPARATOR
                + CLIENT_ID + CACHE_VALUE_SEPARATOR
                + REALM + CACHE_VALUE_SEPARATOR
                + TARGET;
        assertEquals(expectedKey, mDefaultMsalAccessTokenCacheHelper.createCacheKey(accessToken));
    }

    @Test
    public void createCacheKeyNoRealm() throws Exception {
        final AccessToken accessToken = new AccessToken();
        accessToken.setUniqueId(UNIQUE_ID);
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setClientId(CLIENT_ID);
        accessToken.setTarget(TARGET);

        final String expectedKey = "" // just for formatting
                + UNIQUE_ID + CACHE_VALUE_SEPARATOR
                + ENVIRONMENT + CACHE_VALUE_SEPARATOR
                + CREDENTIAL_TYPE + CACHE_VALUE_SEPARATOR
                + CLIENT_ID + CACHE_VALUE_SEPARATOR
                + TARGET;
        assertEquals(expectedKey, mDefaultMsalAccessTokenCacheHelper.createCacheKey(accessToken));
    }

    @Test
    public void createCacheKeyNoTarget() throws Exception {
        final AccessToken accessToken = new AccessToken();
        accessToken.setUniqueId(UNIQUE_ID);
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setClientId(CLIENT_ID);
        accessToken.setRealm(REALM);

        final String expectedKey = "" // just for formatting
                + UNIQUE_ID + CACHE_VALUE_SEPARATOR
                + ENVIRONMENT + CACHE_VALUE_SEPARATOR
                + CREDENTIAL_TYPE + CACHE_VALUE_SEPARATOR
                + CLIENT_ID + CACHE_VALUE_SEPARATOR
                + REALM;

        assertEquals(expectedKey, mDefaultMsalAccessTokenCacheHelper.createCacheKey(accessToken));
    }

    @Test
    public void createCacheKeyNoUniqueIdNoRealmNoTarget() throws Exception {
        final AccessToken accessToken = new AccessToken();
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setClientId(CLIENT_ID);

        final String expectedKey = "" // just for formatting
                + ENVIRONMENT + CACHE_VALUE_SEPARATOR
                + CREDENTIAL_TYPE + CACHE_VALUE_SEPARATOR
                + CLIENT_ID;

        assertEquals(expectedKey, mDefaultMsalAccessTokenCacheHelper.createCacheKey(accessToken));
    }

    @Test
    public void createCacheKeyNoRealmNoTarget() throws Exception {
        final AccessToken accessToken = new AccessToken();
        accessToken.setUniqueId(UNIQUE_ID);
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setClientId(CLIENT_ID);

        final String expectedKey = "" // just for formatting
                + UNIQUE_ID + CACHE_VALUE_SEPARATOR
                + ENVIRONMENT + CACHE_VALUE_SEPARATOR
                + CREDENTIAL_TYPE + CACHE_VALUE_SEPARATOR
                + CLIENT_ID;

        assertEquals(expectedKey, mDefaultMsalAccessTokenCacheHelper.createCacheKey(accessToken));
    }

    @Test
    public void getCacheValue() throws Exception {
        final AccessToken accessToken = new AccessToken();
        accessToken.setUniqueId(UNIQUE_ID);
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setCredentialType(CredentialType.AccessToken.name().toLowerCase(Locale.US));
        accessToken.setClientId(CLIENT_ID);
        accessToken.setRealm(REALM);
        accessToken.setTarget(TARGET);

        final String serializedValue = mDefaultMsalAccessTokenCacheHelper.getCacheValue(accessToken);

        // Turn the serialized value into a JSONObject and start testing field equality.
        final JSONObject jsonObject = new JSONObject(serializedValue);
        assertEquals(UNIQUE_ID, jsonObject.getString("unique_id"));
        assertEquals(ENVIRONMENT, jsonObject.getString("environment"));
        assertEquals(CredentialType.AccessToken.name().toLowerCase(Locale.US), jsonObject.getString("credential_type"));
        assertEquals(CLIENT_ID, jsonObject.getString("client_id"));
        assertEquals(REALM, jsonObject.getString("realm"));
        assertEquals(TARGET, jsonObject.getString("target"));
    }

}