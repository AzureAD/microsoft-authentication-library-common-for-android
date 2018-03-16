package com.microsoft.identity.common;

import com.microsoft.identity.common.internal.cache.DefaultMsalAccessTokenCacheHelper;
import com.microsoft.identity.common.internal.dto.AccessToken;
import com.microsoft.identity.common.internal.dto.CredentialType;

import org.junit.Before;
import org.junit.Test;

import java.util.Locale;

import static com.microsoft.identity.common.internal.cache.DefaultMsalAccessTokenCacheHelper.CACHE_VALUE_SEPARATOR;
import static org.junit.Assert.assertEquals;

public class DefaultMsalAccessTokenCacheHelperTest {

    private DefaultMsalAccessTokenCacheHelper mDefaultMsalAccessTokenCacheHelper;

    @Before
    public void setUp() {
        mDefaultMsalAccessTokenCacheHelper = new DefaultMsalAccessTokenCacheHelper();
    }

    @Test
    public void createCacheKeyComplete() throws Exception {
        final String uniqueId = "29f3807a-4fb0-42f2-a44a-236aa0cb3f97.0287f963-2d72-4363-9e3a-5705c5b0f031";
        final String environment = "login.microsoftonline.com";
        final String credentialType = CredentialType.AccessToken.name().toLowerCase(Locale.US);
        final String clientId = "0287f963-2d72-4363-9e3a-5705c5b0f031";
        final String realm = "3c62ac97-29eb-4aed-a3c8-add0298508d";
        final String target = "user.read user.write https://graph.windows.net";

        final AccessToken accessToken = new AccessToken();
        accessToken.setUniqueId(uniqueId);
        accessToken.setEnvironment(environment);
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setClientId(clientId);
        accessToken.setRealm(realm);
        accessToken.setTarget(target);

        final String expectedKey = "" // just for formatting
                + uniqueId + CACHE_VALUE_SEPARATOR
                + environment + CACHE_VALUE_SEPARATOR
                + credentialType + CACHE_VALUE_SEPARATOR
                + clientId + CACHE_VALUE_SEPARATOR
                + realm + CACHE_VALUE_SEPARATOR
                + target;
        assertEquals(expectedKey, mDefaultMsalAccessTokenCacheHelper.createCacheKey(accessToken));
    }

    @Test
    public void createCacheKeyNoUniqueId() throws Exception {
        final String environment = "login.microsoftonline.com";
        final String credentialType = CredentialType.AccessToken.name().toLowerCase(Locale.US);
        final String clientId = "0287f963-2d72-4363-9e3a-5705c5b0f031";
        final String realm = "3c62ac97-29eb-4aed-a3c8-add0298508d";
        final String target = "user.read user.write https://graph.windows.net";

        final AccessToken accessToken = new AccessToken();
        accessToken.setEnvironment(environment);
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setClientId(clientId);
        accessToken.setRealm(realm);
        accessToken.setTarget(target);

        final String expectedKey = "" // just for formatting
                + environment + CACHE_VALUE_SEPARATOR
                + credentialType + CACHE_VALUE_SEPARATOR
                + clientId + CACHE_VALUE_SEPARATOR
                + realm + CACHE_VALUE_SEPARATOR
                + target;
        assertEquals(expectedKey, mDefaultMsalAccessTokenCacheHelper.createCacheKey(accessToken));
    }

    @Test
    public void createCacheKeyNoRealm() throws Exception {
        final String uniqueId = "29f3807a-4fb0-42f2-a44a-236aa0cb3f97.0287f963-2d72-4363-9e3a-5705c5b0f031";
        final String environment = "login.microsoftonline.com";
        final String credentialType = CredentialType.AccessToken.name().toLowerCase(Locale.US);
        final String clientId = "0287f963-2d72-4363-9e3a-5705c5b0f031";
        final String target = "user.read user.write https://graph.windows.net";

        final AccessToken accessToken = new AccessToken();
        accessToken.setUniqueId(uniqueId);
        accessToken.setEnvironment(environment);
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setClientId(clientId);
        accessToken.setTarget(target);

        final String expectedKey = "" // just for formatting
                + uniqueId + CACHE_VALUE_SEPARATOR
                + environment + CACHE_VALUE_SEPARATOR
                + credentialType + CACHE_VALUE_SEPARATOR
                + clientId + CACHE_VALUE_SEPARATOR
                + target;
        assertEquals(expectedKey, mDefaultMsalAccessTokenCacheHelper.createCacheKey(accessToken));
    }

    @Test
    public void createCacheKeyNoTarget() throws Exception {
        final String uniqueId = "29f3807a-4fb0-42f2-a44a-236aa0cb3f97.0287f963-2d72-4363-9e3a-5705c5b0f031";
        final String environment = "login.microsoftonline.com";
        final String credentialType = CredentialType.AccessToken.name().toLowerCase(Locale.US);
        final String clientId = "0287f963-2d72-4363-9e3a-5705c5b0f031";
        final String realm = "3c62ac97-29eb-4aed-a3c8-add0298508d";

        final AccessToken accessToken = new AccessToken();
        accessToken.setUniqueId(uniqueId);
        accessToken.setEnvironment(environment);
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setClientId(clientId);
        accessToken.setRealm(realm);

        final String expectedKey = "" // just for formatting
                + uniqueId + CACHE_VALUE_SEPARATOR
                + environment + CACHE_VALUE_SEPARATOR
                + credentialType + CACHE_VALUE_SEPARATOR
                + clientId + CACHE_VALUE_SEPARATOR
                + realm;

        assertEquals(expectedKey, mDefaultMsalAccessTokenCacheHelper.createCacheKey(accessToken));
    }

    @Test
    public void createCacheKeyNoUniqueIdNoRealmNoTarget() throws Exception {
        final String environment = "login.microsoftonline.com";
        final String credentialType = CredentialType.AccessToken.name().toLowerCase(Locale.US);
        final String clientId = "0287f963-2d72-4363-9e3a-5705c5b0f031";

        final AccessToken accessToken = new AccessToken();
        accessToken.setEnvironment(environment);
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setClientId(clientId);

        final String expectedKey = "" // just for formatting
                + environment + CACHE_VALUE_SEPARATOR
                + credentialType + CACHE_VALUE_SEPARATOR
                + clientId;

        assertEquals(expectedKey, mDefaultMsalAccessTokenCacheHelper.createCacheKey(accessToken));
    }

    @Test
    public void createCacheKeyNoRealmNoTarget() throws Exception {
        final String uniqueId = "29f3807a-4fb0-42f2-a44a-236aa0cb3f97.0287f963-2d72-4363-9e3a-5705c5b0f031";
        final String environment = "login.microsoftonline.com";
        final String credentialType = CredentialType.AccessToken.name().toLowerCase(Locale.US);
        final String clientId = "0287f963-2d72-4363-9e3a-5705c5b0f031";

        final AccessToken accessToken = new AccessToken();
        accessToken.setUniqueId(uniqueId);
        accessToken.setEnvironment(environment);
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setClientId(clientId);

        final String expectedKey = "" // just for formatting
                + uniqueId + CACHE_VALUE_SEPARATOR
                + environment + CACHE_VALUE_SEPARATOR
                + credentialType + CACHE_VALUE_SEPARATOR
                + clientId;

        assertEquals(expectedKey, mDefaultMsalAccessTokenCacheHelper.createCacheKey(accessToken));
    }

}