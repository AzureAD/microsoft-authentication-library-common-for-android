package com.microsoft.identity.common;

import com.microsoft.identity.common.internal.cache.DefaultAccountCacheHelper;
import com.microsoft.identity.common.internal.dto.Account;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import static com.microsoft.identity.common.DefaultAccessTokenCacheHelperTest.ENVIRONMENT;
import static com.microsoft.identity.common.DefaultAccessTokenCacheHelperTest.REALM;
import static com.microsoft.identity.common.DefaultAccessTokenCacheHelperTest.UNIQUE_ID;
import static com.microsoft.identity.common.internal.cache.AbstractCacheHelper.CACHE_VALUE_SEPARATOR;
import static org.junit.Assert.assertEquals;

public class DefaultAccountCacheHelperTest {

    private DefaultAccountCacheHelper mMsalAccountCacheHelper;

    @Before
    public void setUp() {
        mMsalAccountCacheHelper = new DefaultAccountCacheHelper();
    }

    @Test
    public void createCacheKeyComplete() throws Exception {
        final Account account = new Account();
        account.setUniqueId(UNIQUE_ID);
        account.setEnvironment(ENVIRONMENT);
        account.setRealm(REALM);

        final String expectedKey = ""
                + UNIQUE_ID + CACHE_VALUE_SEPARATOR
                + ENVIRONMENT + CACHE_VALUE_SEPARATOR
                + REALM;

        assertEquals(expectedKey, mMsalAccountCacheHelper.createCacheKey(account));
    }

    @Test
    public void createCacheKeyCompleteNoUniqueId() throws Exception {
        final Account account = new Account();
        account.setEnvironment(ENVIRONMENT);
        account.setRealm(REALM);

        final String expectedKey = ""
                + ENVIRONMENT + CACHE_VALUE_SEPARATOR
                + REALM;

        assertEquals(expectedKey, mMsalAccountCacheHelper.createCacheKey(account));
    }

    @Test
    public void createCacheKeyCompleteNoRealm() throws Exception {
        final Account account = new Account();
        account.setUniqueId(UNIQUE_ID);
        account.setEnvironment(ENVIRONMENT);

        final String expectedKey = ""
                + UNIQUE_ID + CACHE_VALUE_SEPARATOR
                + ENVIRONMENT;

        assertEquals(expectedKey, mMsalAccountCacheHelper.createCacheKey(account));
    }

    @Test
    public void createCacheKeyCompleteNoUniqueIdNoRealm() throws Exception {
        final Account account = new Account();
        account.setEnvironment(ENVIRONMENT);

        assertEquals(ENVIRONMENT, mMsalAccountCacheHelper.createCacheKey(account));
    }

    private static final String AUTHORITY_ACCOUNT_ID = "90bc88e6-7c76-45e8-a4e3-a0b1dc0a8ce1";
    private static final String AUTHORITY_TYPE = "AAD";
    private static final String GUEST_ID = "32000000000003bde810";
    private static final String FIRST_NAME = "Jane";
    private static final String LAST_NAME = "Doe";
    private static final String AVATAR_URL = "https://fake.cdn.microsoft.com/avatars/1";

    @Test
    public void createCacheValue() throws Exception {
        final Account account = new Account();
        account.setUniqueId(UNIQUE_ID);
        account.setEnvironment(ENVIRONMENT);
        account.setRealm(REALM);
        account.setAuthorityAccountId(AUTHORITY_ACCOUNT_ID);
        account.setAuthorityType(AUTHORITY_TYPE);
        account.setGuestId(GUEST_ID);
        account.setFirstName(FIRST_NAME);
        account.setLastName(LAST_NAME);
        account.setAvatarUrl(AVATAR_URL);

        final String serializedValue = mMsalAccountCacheHelper.getCacheValue(account);

        final JSONObject jsonObject = new JSONObject(serializedValue);
        assertEquals(UNIQUE_ID, jsonObject.getString("unique_id"));
        assertEquals(ENVIRONMENT, jsonObject.getString("environment"));
        assertEquals(REALM, jsonObject.getString("realm"));
        assertEquals(AUTHORITY_ACCOUNT_ID, jsonObject.getString("authority_account_id"));
        assertEquals(AUTHORITY_TYPE, jsonObject.getString("authority_type"));
        assertEquals(GUEST_ID, jsonObject.getString("guest_id"));
        assertEquals(FIRST_NAME, jsonObject.getString("first_name"));
        assertEquals(LAST_NAME, jsonObject.getString("last_name"));
        assertEquals(AVATAR_URL, jsonObject.getString("avatar_url"));

    }

}
