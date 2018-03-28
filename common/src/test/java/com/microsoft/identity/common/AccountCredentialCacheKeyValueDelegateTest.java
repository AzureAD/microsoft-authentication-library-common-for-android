package com.microsoft.identity.common;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.microsoft.identity.common.internal.cache.AccountCredentialCacheKeyValueDelegate;
import com.microsoft.identity.common.internal.cache.IAccountCredentialCacheKeyValueDelegate;
import com.microsoft.identity.common.internal.dto.AccessToken;
import com.microsoft.identity.common.internal.dto.CredentialType;
import com.microsoft.identity.common.internal.dto.RefreshToken;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static com.microsoft.identity.common.internal.cache.AccountCredentialCacheKeyValueDelegate.CACHE_VALUE_SEPARATOR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class AccountCredentialCacheKeyValueDelegateTest {

    private static final String UNIQUE_ID = "29f3807a-4fb0-42f2-a44a-236aa0cb3f97.0287f963-2d72-4363-9e3a-5705c5b0f031";
    private static final String ENVIRONMENT = "login.microsoftonline.com";
    private static final String CLIENT_ID = "0287f963-2d72-4363-9e3a-5705c5b0f031";
    private static final String TARGET = "user.read user.write https://graph.windows.net";
    private static final String REALM = "3c62ac97-29eb-4aed-a3c8-add0298508d";
    private static final String CREDENTIAL_TYPE_ACCESS_TOKEN = CredentialType.AccessToken.name().toLowerCase(Locale.US);
    private static final String CREDENTIAL_TYPE_REFRESH_TOKEN = CredentialType.RefreshToken.name().toLowerCase(Locale.US);

    private IAccountCredentialCacheKeyValueDelegate mDelegate;

    @Before
    public void setUp() {
        mDelegate = new AccountCredentialCacheKeyValueDelegate();
    }

    // AccessTokens
    @Test
    public void accessTokenCreateCacheKeyComplete() throws Exception {
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
                + CREDENTIAL_TYPE_ACCESS_TOKEN + CACHE_VALUE_SEPARATOR
                + CLIENT_ID + CACHE_VALUE_SEPARATOR
                + REALM + CACHE_VALUE_SEPARATOR
                + TARGET;
        assertEquals(expectedKey, mDelegate.generateCacheKey(accessToken));
    }

    @Test
    public void accessTokenCreateCacheKeyNoUniqueId() throws Exception {
        final AccessToken accessToken = new AccessToken();
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setClientId(CLIENT_ID);
        accessToken.setRealm(REALM);
        accessToken.setTarget(TARGET);

        final String expectedKey = "" // just for formatting
                + ENVIRONMENT + CACHE_VALUE_SEPARATOR
                + CREDENTIAL_TYPE_ACCESS_TOKEN + CACHE_VALUE_SEPARATOR
                + CLIENT_ID + CACHE_VALUE_SEPARATOR
                + REALM + CACHE_VALUE_SEPARATOR
                + TARGET;
        assertEquals(expectedKey, mDelegate.generateCacheKey(accessToken));
    }

    @Test
    public void accessTokenCreateCacheKeyNoRealm() throws Exception {
        final AccessToken accessToken = new AccessToken();
        accessToken.setUniqueId(UNIQUE_ID);
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setClientId(CLIENT_ID);
        accessToken.setTarget(TARGET);

        final String expectedKey = "" // just for formatting
                + UNIQUE_ID + CACHE_VALUE_SEPARATOR
                + ENVIRONMENT + CACHE_VALUE_SEPARATOR
                + CREDENTIAL_TYPE_ACCESS_TOKEN + CACHE_VALUE_SEPARATOR
                + CLIENT_ID + CACHE_VALUE_SEPARATOR
                + TARGET;
        assertEquals(expectedKey, mDelegate.generateCacheKey(accessToken));
    }

    @Test
    public void accessTokenCreateCacheKeyNoTarget() throws Exception {
        final AccessToken accessToken = new AccessToken();
        accessToken.setUniqueId(UNIQUE_ID);
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setClientId(CLIENT_ID);
        accessToken.setRealm(REALM);

        final String expectedKey = "" // just for formatting
                + UNIQUE_ID + CACHE_VALUE_SEPARATOR
                + ENVIRONMENT + CACHE_VALUE_SEPARATOR
                + CREDENTIAL_TYPE_ACCESS_TOKEN + CACHE_VALUE_SEPARATOR
                + CLIENT_ID + CACHE_VALUE_SEPARATOR
                + REALM;

        assertEquals(expectedKey, mDelegate.generateCacheKey(accessToken));
    }

    @Test
    public void accessTokenCreateCacheKeyNoUniqueIdNoRealmNoTarget() throws Exception {
        final AccessToken accessToken = new AccessToken();
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setClientId(CLIENT_ID);

        final String expectedKey = "" // just for formatting
                + ENVIRONMENT + CACHE_VALUE_SEPARATOR
                + CREDENTIAL_TYPE_ACCESS_TOKEN + CACHE_VALUE_SEPARATOR
                + CLIENT_ID;

        assertEquals(expectedKey, mDelegate.generateCacheKey(accessToken));
    }

    @Test
    public void accessTokenCreateCacheKeyNoRealmNoTarget() throws Exception {
        final AccessToken accessToken = new AccessToken();
        accessToken.setUniqueId(UNIQUE_ID);
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setClientId(CLIENT_ID);

        final String expectedKey = "" // just for formatting
                + UNIQUE_ID + CACHE_VALUE_SEPARATOR
                + ENVIRONMENT + CACHE_VALUE_SEPARATOR
                + CREDENTIAL_TYPE_ACCESS_TOKEN + CACHE_VALUE_SEPARATOR
                + CLIENT_ID;

        assertEquals(expectedKey, mDelegate.generateCacheKey(accessToken));
    }

    @Test
    public void accessTokenCreateCacheValue() throws Exception {
        final AccessToken accessToken = new AccessToken();
        accessToken.setUniqueId(UNIQUE_ID);
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setCredentialType(CredentialType.AccessToken.name().toLowerCase(Locale.US));
        accessToken.setClientId(CLIENT_ID);
        accessToken.setRealm(REALM);
        accessToken.setTarget(TARGET);

        final String serializedValue = mDelegate.generateCacheValue(accessToken);

        // Turn the serialized value into a JSONObject and start testing field equality.
        final JSONObject jsonObject = new JSONObject(serializedValue);
        assertEquals(UNIQUE_ID, jsonObject.getString("unique_id"));
        assertEquals(ENVIRONMENT, jsonObject.getString("environment"));
        assertEquals(CredentialType.AccessToken.name().toLowerCase(Locale.US), jsonObject.getString("credential_type"));
        assertEquals(CLIENT_ID, jsonObject.getString("client_id"));
        assertEquals(REALM, jsonObject.getString("realm"));
        assertEquals(TARGET, jsonObject.getString("target"));
    }

    @Test
    public void accessTokenExtraValueSerialization() throws Exception {
        final AccessToken accessToken = new AccessToken();
        accessToken.setUniqueId(UNIQUE_ID);
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setCredentialType(CredentialType.AccessToken.name().toLowerCase(Locale.US));
        accessToken.setClientId(CLIENT_ID);
        accessToken.setRealm(REALM);
        accessToken.setTarget(TARGET);

        final Map<String, JsonElement> additionalFields = new HashMap<>();

        // Add some random Json to this object
        JsonElement jsonStr = new JsonPrimitive("bar");
        JsonArray jsonNumberArr = new JsonArray();
        jsonNumberArr.add(1);
        jsonNumberArr.add(2);
        jsonNumberArr.add(3);
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("object_key", new JsonPrimitive("object_value"));

        additionalFields.put("foo", jsonStr);
        additionalFields.put("numbers", jsonNumberArr);
        additionalFields.put("object", jsonObject);

        accessToken.setAdditionalFields(additionalFields);

        String serializedValue = mDelegate.generateCacheValue(accessToken);
        JSONObject derivedCacheValueJsonObject = new JSONObject(serializedValue);
        assertEquals(UNIQUE_ID, derivedCacheValueJsonObject.getString("unique_id"));
        assertEquals(ENVIRONMENT, derivedCacheValueJsonObject.getString("environment"));
        assertEquals(CredentialType.AccessToken.name().toLowerCase(Locale.US), derivedCacheValueJsonObject.getString("credential_type"));
        assertEquals(CLIENT_ID, derivedCacheValueJsonObject.getString("client_id"));
        assertEquals(REALM, derivedCacheValueJsonObject.getString("realm"));
        assertEquals(TARGET, derivedCacheValueJsonObject.getString("target"));
        assertEquals("bar", derivedCacheValueJsonObject.getString("foo"));

        final JSONArray jsonArr = derivedCacheValueJsonObject.getJSONArray("numbers");
        assertEquals(3, jsonArr.length());

        final JSONObject jsonObj = derivedCacheValueJsonObject.getJSONObject("object");
        assertEquals("object_value", jsonObj.getString("object_key"));
    }

    @Test
    public void accessTokenExtraValueDeserialization() throws Exception {
        final AccessToken accessToken = new AccessToken();
        accessToken.setUniqueId(UNIQUE_ID);
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setCredentialType(CredentialType.AccessToken.name().toLowerCase(Locale.US));
        accessToken.setClientId(CLIENT_ID);
        accessToken.setRealm(REALM);
        accessToken.setTarget(TARGET);

        String serializedValue = mDelegate.generateCacheValue(accessToken);

        // Turn the serialized value into a JSONObject and start testing field equality.
        final JSONObject jsonObject = new JSONObject(serializedValue);

        // Add more non-standard data to this object...
        final JSONArray numbers = new JSONArray("[1, 2, 3]");
        final JSONArray objects = new JSONArray("[{\"hello\" : \"hallo\"}, {\"goodbye\" : \"auf wiedersehen\"}]");

        jsonObject.put("foo", "bar");
        jsonObject.put("numbers", numbers);
        jsonObject.put("objects", objects);

        serializedValue = jsonObject.toString();

        final AccessToken deserializedValue = mDelegate.fromCacheValue(serializedValue, AccessToken.class);
        assertNotNull(deserializedValue);
        assertNull(deserializedValue.getAdditionalFields().get("environment"));
        assertEquals(UNIQUE_ID, deserializedValue.getUniqueId());
        assertEquals(ENVIRONMENT, deserializedValue.getEnvironment());
        assertEquals(CredentialType.AccessToken.name().toLowerCase(Locale.US), deserializedValue.getCredentialType());
        assertEquals(CLIENT_ID, deserializedValue.getClientId());
        assertEquals(REALM, deserializedValue.getRealm());
        assertEquals(TARGET, deserializedValue.getTarget());
        assertEquals(3, deserializedValue.getAdditionalFields().size());
        assertEquals("bar", deserializedValue.getAdditionalFields().get("foo").getAsString());
        assertEquals(numbers.toString(), deserializedValue.getAdditionalFields().get("numbers").toString());
    }
    // End AccessTokens

    // Accounts
    @Test
    public void accountCreateCacheKeyComplete() throws Exception {
        final com.microsoft.identity.common.internal.dto.Account account = new com.microsoft.identity.common.internal.dto.Account();
        account.setUniqueId(UNIQUE_ID);
        account.setEnvironment(ENVIRONMENT);
        account.setRealm(REALM);

        final String expectedKey = ""
                + UNIQUE_ID + CACHE_VALUE_SEPARATOR
                + ENVIRONMENT + CACHE_VALUE_SEPARATOR
                + REALM;

        assertEquals(expectedKey, mDelegate.generateCacheKey(account));
    }

    @Test
    public void accountCreateCacheKeyCompleteNoUniqueId() throws Exception {
        final com.microsoft.identity.common.internal.dto.Account account = new com.microsoft.identity.common.internal.dto.Account();
        account.setEnvironment(ENVIRONMENT);
        account.setRealm(REALM);

        final String expectedKey = ""
                + ENVIRONMENT + CACHE_VALUE_SEPARATOR
                + REALM;

        assertEquals(expectedKey, mDelegate.generateCacheKey(account));
    }

    @Test
    public void accountCreateCacheKeyCompleteNoRealm() throws Exception {
        final com.microsoft.identity.common.internal.dto.Account account = new com.microsoft.identity.common.internal.dto.Account();
        account.setUniqueId(UNIQUE_ID);
        account.setEnvironment(ENVIRONMENT);

        final String expectedKey = ""
                + UNIQUE_ID + CACHE_VALUE_SEPARATOR
                + ENVIRONMENT;

        assertEquals(expectedKey, mDelegate.generateCacheKey(account));
    }

    @Test
    public void accountCreateCacheKeyCompleteNoUniqueIdNoRealm() throws Exception {
        final com.microsoft.identity.common.internal.dto.Account account = new com.microsoft.identity.common.internal.dto.Account();
        account.setEnvironment(ENVIRONMENT);

        assertEquals(ENVIRONMENT, mDelegate.generateCacheKey(account));
    }

    private static final String AUTHORITY_ACCOUNT_ID = "90bc88e6-7c76-45e8-a4e3-a0b1dc0a8ce1";
    private static final String AUTHORITY_TYPE = "AAD";
    private static final String GUEST_ID = "32000000000003bde810";
    private static final String FIRST_NAME = "Jane";
    private static final String LAST_NAME = "Doe";
    private static final String AVATAR_URL = "https://fake.cdn.microsoft.com/avatars/1";

    @Test
    public void accountCreateCacheValue() throws Exception {
        final com.microsoft.identity.common.internal.dto.Account account = new com.microsoft.identity.common.internal.dto.Account();
        account.setUniqueId(UNIQUE_ID);
        account.setEnvironment(ENVIRONMENT);
        account.setRealm(REALM);
        account.setAuthorityAccountId(AUTHORITY_ACCOUNT_ID);
        account.setAuthorityType(AUTHORITY_TYPE);
        account.setGuestId(GUEST_ID);
        account.setFirstName(FIRST_NAME);
        account.setLastName(LAST_NAME);
        account.setAvatarUrl(AVATAR_URL);

        final String serializedValue = mDelegate.generateCacheValue(account);

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

    @Test
    public void accountExtraValueSerialization() throws Exception {
        final com.microsoft.identity.common.internal.dto.Account account
                = new com.microsoft.identity.common.internal.dto.Account();
        account.setUniqueId(UNIQUE_ID);
        account.setEnvironment(ENVIRONMENT);
        account.setRealm(REALM);
        account.setAuthorityAccountId(AUTHORITY_ACCOUNT_ID);
        account.setAuthorityType(AUTHORITY_TYPE);
        account.setGuestId(GUEST_ID);
        account.setFirstName(FIRST_NAME);
        account.setLastName(LAST_NAME);
        account.setAvatarUrl(AVATAR_URL);

        final Map<String, JsonElement> additionalFields = new HashMap<>();

        // Add some random Json to this object
        JsonElement jsonStr = new JsonPrimitive("bar");
        JsonArray jsonNumberArr = new JsonArray();
        jsonNumberArr.add(1);
        jsonNumberArr.add(2);
        jsonNumberArr.add(3);
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("object_key", new JsonPrimitive("object_value"));

        additionalFields.put("foo", jsonStr);
        additionalFields.put("numbers", jsonNumberArr);
        additionalFields.put("object", jsonObject);

        account.setAdditionalFields(additionalFields);

        String serializedValue = mDelegate.generateCacheValue(account);
        JSONObject derivedCacheValueJsonObject = new JSONObject(serializedValue);
        assertEquals(UNIQUE_ID, derivedCacheValueJsonObject.getString("unique_id"));
        assertEquals(ENVIRONMENT, derivedCacheValueJsonObject.getString("environment"));
        assertEquals(REALM, derivedCacheValueJsonObject.getString("realm"));
        assertEquals(AUTHORITY_ACCOUNT_ID, derivedCacheValueJsonObject.get("authority_account_id"));
        assertEquals(AUTHORITY_TYPE, derivedCacheValueJsonObject.get("authority_type"));
        assertEquals(GUEST_ID, derivedCacheValueJsonObject.get("guest_id"));
        assertEquals(FIRST_NAME, derivedCacheValueJsonObject.get("first_name"));
        assertEquals(LAST_NAME, derivedCacheValueJsonObject.get("last_name"));
        assertEquals(AVATAR_URL, derivedCacheValueJsonObject.get("avatar_url"));
        assertEquals("bar", derivedCacheValueJsonObject.getString("foo"));

        final JSONArray jsonArr = derivedCacheValueJsonObject.getJSONArray("numbers");
        assertEquals(3, jsonArr.length());

        final JSONObject jsonObj = derivedCacheValueJsonObject.getJSONObject("object");
        assertEquals("object_value", jsonObj.getString("object_key"));
    }

    @Test
    public void accountExtraValueDeserialization() throws Exception {
        final com.microsoft.identity.common.internal.dto.Account account
                = new com.microsoft.identity.common.internal.dto.Account();
        account.setUniqueId(UNIQUE_ID);
        account.setEnvironment(ENVIRONMENT);
        account.setRealm(REALM);
        account.setAuthorityAccountId(AUTHORITY_ACCOUNT_ID);
        account.setAuthorityType(AUTHORITY_TYPE);
        account.setGuestId(GUEST_ID);
        account.setFirstName(FIRST_NAME);
        account.setLastName(LAST_NAME);
        account.setAvatarUrl(AVATAR_URL);

        String serializedValue = mDelegate.generateCacheValue(account);

        // Turn the serialized value into a JSONObject and start testing field equality.
        final JSONObject jsonObject = new JSONObject(serializedValue);

        // Add more non-standard data to this object...
        final JSONArray numbers = new JSONArray("[1, 2, 3]");
        final JSONArray objects = new JSONArray("[{\"hello\" : \"hallo\"}, {\"goodbye\" : \"auf wiedersehen\"}]");

        jsonObject.put("foo", "bar");
        jsonObject.put("numbers", numbers);
        jsonObject.put("objects", objects);

        serializedValue = jsonObject.toString();

        final com.microsoft.identity.common.internal.dto.Account deserializedValue
                = mDelegate.fromCacheValue(serializedValue, com.microsoft.identity.common.internal.dto.Account.class);
        assertNotNull(deserializedValue);
        assertNull(deserializedValue.getAdditionalFields().get("environment"));
        assertEquals(UNIQUE_ID, deserializedValue.getUniqueId());
        assertEquals(ENVIRONMENT, deserializedValue.getEnvironment());
        assertEquals(REALM, deserializedValue.getRealm());
        assertEquals(AUTHORITY_ACCOUNT_ID, deserializedValue.getAuthorityAccountId());
        assertEquals(AUTHORITY_TYPE, deserializedValue.getAuthorityType());
        assertEquals(GUEST_ID, deserializedValue.getGuestId());
        assertEquals(FIRST_NAME, deserializedValue.getFirstName());
        assertEquals(LAST_NAME, deserializedValue.getLastName());
        assertEquals(AVATAR_URL, deserializedValue.getAvatarUrl());
        assertEquals(3, deserializedValue.getAdditionalFields().size());
        assertEquals("bar", deserializedValue.getAdditionalFields().get("foo").getAsString());
        assertEquals(numbers.toString(), deserializedValue.getAdditionalFields().get("numbers").toString());
    }
    // End Accounts

    // RefreshTokens

    @Test
    public void refreshTokenCreateCacheKeyComplete() throws Exception {
        final RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUniqueId(UNIQUE_ID);
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setCredentialType(CredentialType.RefreshToken.name());
        refreshToken.setClientId(CLIENT_ID);
        refreshToken.setTarget(TARGET);

        final String expectedKey = "" // just for formatting
                + UNIQUE_ID + CACHE_VALUE_SEPARATOR
                + ENVIRONMENT + CACHE_VALUE_SEPARATOR
                + CREDENTIAL_TYPE_REFRESH_TOKEN + CACHE_VALUE_SEPARATOR
                + CLIENT_ID + CACHE_VALUE_SEPARATOR
                + TARGET;
        assertEquals(expectedKey, mDelegate.generateCacheKey(refreshToken));
    }

    @Test
    public void refreshTokenCreateCacheKeyNoUniqueId() throws Exception {
        final RefreshToken refreshToken = new RefreshToken();
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setCredentialType(CredentialType.RefreshToken.name());
        refreshToken.setClientId(CLIENT_ID);
        refreshToken.setTarget(TARGET);

        final String expectedKey = "" // just for formatting
                + ENVIRONMENT + CACHE_VALUE_SEPARATOR
                + CREDENTIAL_TYPE_REFRESH_TOKEN + CACHE_VALUE_SEPARATOR
                + CLIENT_ID + CACHE_VALUE_SEPARATOR
                + TARGET;
        assertEquals(expectedKey, mDelegate.generateCacheKey(refreshToken));
    }

    @Test
    public void refreshTokenCreateCacheKeyNoRealm() throws Exception {
        final RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUniqueId(UNIQUE_ID);
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setCredentialType(CredentialType.RefreshToken.name());
        refreshToken.setClientId(CLIENT_ID);
        refreshToken.setTarget(TARGET);

        final String expectedKey = "" // just for formatting
                + UNIQUE_ID + CACHE_VALUE_SEPARATOR
                + ENVIRONMENT + CACHE_VALUE_SEPARATOR
                + CREDENTIAL_TYPE_REFRESH_TOKEN + CACHE_VALUE_SEPARATOR
                + CLIENT_ID + CACHE_VALUE_SEPARATOR
                + TARGET;
        assertEquals(expectedKey, mDelegate.generateCacheKey(refreshToken));
    }

    @Test
    public void refreshTokenCreateCacheKeyNoTarget() throws Exception {
        final RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUniqueId(UNIQUE_ID);
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setCredentialType(CredentialType.RefreshToken.name());
        refreshToken.setClientId(CLIENT_ID);

        final String expectedKey = "" // just for formatting
                + UNIQUE_ID + CACHE_VALUE_SEPARATOR
                + ENVIRONMENT + CACHE_VALUE_SEPARATOR
                + CREDENTIAL_TYPE_REFRESH_TOKEN + CACHE_VALUE_SEPARATOR
                + CLIENT_ID;

        assertEquals(expectedKey, mDelegate.generateCacheKey(refreshToken));
    }

    @Test
    public void refreshTokenCreateCacheKeyNoUniqueIdNoTarget() throws Exception {
        final RefreshToken refreshToken = new RefreshToken();
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setCredentialType(CredentialType.RefreshToken.name());
        refreshToken.setClientId(CLIENT_ID);

        final String expectedKey = "" // just for formatting
                + ENVIRONMENT + CACHE_VALUE_SEPARATOR
                + CREDENTIAL_TYPE_REFRESH_TOKEN + CACHE_VALUE_SEPARATOR
                + CLIENT_ID;

        assertEquals(expectedKey, mDelegate.generateCacheKey(refreshToken));
    }

    @Test
    public void refreshTokenCreateCacheValue() throws Exception {
        final RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUniqueId(UNIQUE_ID);
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setCredentialType(CredentialType.RefreshToken.name().toLowerCase(Locale.US));
        refreshToken.setClientId(CLIENT_ID);
        refreshToken.setTarget(TARGET);

        final String serializedValue = mDelegate.generateCacheValue(refreshToken);

        // Turn the serialized value into a JSONObject and start testing field equality.
        final JSONObject jsonObject = new JSONObject(serializedValue);
        assertEquals(UNIQUE_ID, jsonObject.getString("unique_id"));
        assertEquals(ENVIRONMENT, jsonObject.getString("environment"));
        assertEquals(CredentialType.RefreshToken.name().toLowerCase(Locale.US), jsonObject.getString("credential_type"));
        assertEquals(CLIENT_ID, jsonObject.getString("client_id"));
        assertEquals(TARGET, jsonObject.getString("target"));
    }

    @Test
    public void refreshTokenExtraValueSerialization() throws Exception {
        final RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUniqueId(UNIQUE_ID);
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setCredentialType(CredentialType.RefreshToken.name().toLowerCase(Locale.US));
        refreshToken.setClientId(CLIENT_ID);
        refreshToken.setTarget(TARGET);

        final Map<String, JsonElement> additionalFields = new HashMap<>();

        // Add some random Json to this object
        JsonElement jsonStr = new JsonPrimitive("bar");
        JsonArray jsonNumberArr = new JsonArray();
        jsonNumberArr.add(1);
        jsonNumberArr.add(2);
        jsonNumberArr.add(3);
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("object_key", new JsonPrimitive("object_value"));

        additionalFields.put("foo", jsonStr);
        additionalFields.put("numbers", jsonNumberArr);
        additionalFields.put("object", jsonObject);

        refreshToken.setAdditionalFields(additionalFields);

        String serializedValue = mDelegate.generateCacheValue(refreshToken);
        JSONObject derivedCacheValueJsonObject = new JSONObject(serializedValue);
        assertEquals(UNIQUE_ID, derivedCacheValueJsonObject.getString("unique_id"));
        assertEquals(ENVIRONMENT, derivedCacheValueJsonObject.getString("environment"));
        assertEquals(CredentialType.RefreshToken.name().toLowerCase(Locale.US), derivedCacheValueJsonObject.getString("credential_type"));
        assertEquals(CLIENT_ID, derivedCacheValueJsonObject.getString("client_id"));
        assertEquals(TARGET, derivedCacheValueJsonObject.getString("target"));
        assertEquals("bar", derivedCacheValueJsonObject.getString("foo"));

        final JSONArray jsonArr = derivedCacheValueJsonObject.getJSONArray("numbers");
        assertEquals(3, jsonArr.length());

        final JSONObject jsonObj = derivedCacheValueJsonObject.getJSONObject("object");
        assertEquals("object_value", jsonObj.getString("object_key"));
    }

    @Test
    public void refreshTokenExtraValueDeserialization() throws Exception {
        final RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUniqueId(UNIQUE_ID);
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setCredentialType(CredentialType.AccessToken.name().toLowerCase(Locale.US));
        refreshToken.setClientId(CLIENT_ID);
        refreshToken.setTarget(TARGET);

        String serializedValue = mDelegate.generateCacheValue(refreshToken);

        // Turn the serialized value into a JSONObject and start testing field equality.
        final JSONObject jsonObject = new JSONObject(serializedValue);

        // Add more non-standard data to this object...
        final JSONArray numbers = new JSONArray("[1, 2, 3]");
        final JSONArray objects = new JSONArray("[{\"hello\" : \"hallo\"}, {\"goodbye\" : \"auf wiedersehen\"}]");

        jsonObject.put("foo", "bar");
        jsonObject.put("numbers", numbers);
        jsonObject.put("objects", objects);

        serializedValue = jsonObject.toString();

        final AccessToken deserializedValue = mDelegate.fromCacheValue(serializedValue, AccessToken.class);
        assertNotNull(deserializedValue);
        assertNull(deserializedValue.getAdditionalFields().get("environment"));
        assertEquals(UNIQUE_ID, deserializedValue.getUniqueId());
        assertEquals(ENVIRONMENT, deserializedValue.getEnvironment());
        assertEquals(CredentialType.AccessToken.name().toLowerCase(Locale.US), deserializedValue.getCredentialType());
        assertEquals(CLIENT_ID, deserializedValue.getClientId());
        assertEquals(TARGET, deserializedValue.getTarget());
        assertEquals(3, deserializedValue.getAdditionalFields().size());
        assertEquals("bar", deserializedValue.getAdditionalFields().get("foo").getAsString());
        assertEquals(numbers.toString(), deserializedValue.getAdditionalFields().get("numbers").toString());
    }
    // End RefreshTokens
}
