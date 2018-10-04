// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.common;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.microsoft.identity.common.internal.cache.CacheKeyValueDelegate;
import com.microsoft.identity.common.internal.cache.ICacheKeyValueDelegate;
import com.microsoft.identity.common.internal.dto.AccessTokenRecord;
import com.microsoft.identity.common.internal.dto.AccountRecord;
import com.microsoft.identity.common.internal.dto.Credential;
import com.microsoft.identity.common.internal.dto.CredentialType;
import com.microsoft.identity.common.internal.dto.IdTokenRecord;
import com.microsoft.identity.common.internal.dto.RefreshTokenRecord;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static com.microsoft.identity.common.internal.cache.CacheKeyValueDelegate.CACHE_VALUE_SEPARATOR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class CacheKeyValueDelegateTest {

    private static final String HOME_ACCOUNT_ID = "29f3807a-4fb0-42f2-a44a-236aa0cb3f97.0287f963-2d72-4363-9e3a-5705c5b0f031";
    private static final String ENVIRONMENT = "login.microsoftonline.com";
    private static final String CLIENT_ID = "0287f963-2d72-4363-9e3a-5705c5b0f031";
    private static final String TARGET = "user.read user.write https://graph.windows.net";
    private static final String REALM = "3c62ac97-29eb-4aed-a3c8-add0298508d";
    private static final String CREDENTIAL_TYPE_ACCESS_TOKEN = CredentialType.AccessToken.name().toLowerCase(Locale.US);
    private static final String CREDENTIAL_TYPE_REFRESH_TOKEN = CredentialType.RefreshToken.name().toLowerCase(Locale.US);
    private static final String CREDENTIAL_TYPE_ID_TOKEN = CredentialType.IdToken.name().toLowerCase(Locale.US);
    private static final String LOCAL_ACCOUNT_ID = "90bc88e6-7c76-45e8-a4e3-a0b1dc0a8ce1";
    private static final String AUTHORITY_TYPE = "AAD";
    private static final String ALTERNATIVE_ACCOUNT_ID = "32000000000003bde810";
    private static final String FIRST_NAME = "Jane";
    private static final String FAMILY_NAME = "Doe";
    private static final String AVATAR_URL = "https://fake.cdn.microsoft.com/avatars/1";

    private ICacheKeyValueDelegate mDelegate;

    @Before
    public void setUp() {
        mDelegate = new CacheKeyValueDelegate();
    }

    // AccessTokens
    @Test
    public void accessTokenCreateCacheKeyComplete() {
        final AccessTokenRecord accessToken = new AccessTokenRecord();
        accessToken.setHomeAccountId(HOME_ACCOUNT_ID);
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setClientId(CLIENT_ID);
        accessToken.setRealm(REALM);
        accessToken.setTarget(TARGET);

        final String expectedKey = "" // just for formatting
                + HOME_ACCOUNT_ID + CACHE_VALUE_SEPARATOR
                + ENVIRONMENT + CACHE_VALUE_SEPARATOR
                + CREDENTIAL_TYPE_ACCESS_TOKEN + CACHE_VALUE_SEPARATOR
                + CLIENT_ID + CACHE_VALUE_SEPARATOR
                + REALM + CACHE_VALUE_SEPARATOR
                + TARGET;
        assertEquals(expectedKey, mDelegate.generateCacheKey(accessToken));
    }

    @Test
    public void accessTokenCreateCacheKeyNoHomeAccountId() {
        final AccessTokenRecord accessToken = new AccessTokenRecord();
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setClientId(CLIENT_ID);
        accessToken.setRealm(REALM);
        accessToken.setTarget(TARGET);

        final String expectedKey = "" // just for formatting
                + CACHE_VALUE_SEPARATOR
                + ENVIRONMENT + CACHE_VALUE_SEPARATOR
                + CREDENTIAL_TYPE_ACCESS_TOKEN + CACHE_VALUE_SEPARATOR
                + CLIENT_ID + CACHE_VALUE_SEPARATOR
                + REALM + CACHE_VALUE_SEPARATOR
                + TARGET;
        assertEquals(expectedKey, mDelegate.generateCacheKey(accessToken));
    }

    @Test
    public void accessTokenCreateCacheKeyNoRealm() {
        final AccessTokenRecord accessToken = new AccessTokenRecord();
        accessToken.setHomeAccountId(HOME_ACCOUNT_ID);
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setClientId(CLIENT_ID);
        accessToken.setTarget(TARGET);

        final String expectedKey = "" // just for formatting
                + HOME_ACCOUNT_ID + CACHE_VALUE_SEPARATOR
                + ENVIRONMENT + CACHE_VALUE_SEPARATOR
                + CREDENTIAL_TYPE_ACCESS_TOKEN + CACHE_VALUE_SEPARATOR
                + CLIENT_ID + CACHE_VALUE_SEPARATOR
                + CACHE_VALUE_SEPARATOR
                + TARGET;
        assertEquals(expectedKey, mDelegate.generateCacheKey(accessToken));
    }

    @Test
    public void accessTokenCreateCacheKeyNoTarget() {
        final AccessTokenRecord accessToken = new AccessTokenRecord();
        accessToken.setHomeAccountId(HOME_ACCOUNT_ID);
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setClientId(CLIENT_ID);
        accessToken.setRealm(REALM);

        final String expectedKey = "" // just for formatting
                + HOME_ACCOUNT_ID + CACHE_VALUE_SEPARATOR
                + ENVIRONMENT + CACHE_VALUE_SEPARATOR
                + CREDENTIAL_TYPE_ACCESS_TOKEN + CACHE_VALUE_SEPARATOR
                + CLIENT_ID + CACHE_VALUE_SEPARATOR
                + REALM + CACHE_VALUE_SEPARATOR;

        assertEquals(expectedKey, mDelegate.generateCacheKey(accessToken));
    }

    @Test
    public void accessTokenCreateCacheKeyNoHomeAccountIdNoRealmNoTarget() {
        final AccessTokenRecord accessToken = new AccessTokenRecord();
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setClientId(CLIENT_ID);

        final String expectedKey = "" // just for formatting
                + CACHE_VALUE_SEPARATOR
                + ENVIRONMENT + CACHE_VALUE_SEPARATOR
                + CREDENTIAL_TYPE_ACCESS_TOKEN + CACHE_VALUE_SEPARATOR
                + CLIENT_ID + CACHE_VALUE_SEPARATOR
                + CACHE_VALUE_SEPARATOR;

        assertEquals(expectedKey, mDelegate.generateCacheKey(accessToken));
    }

    @Test
    public void accessTokenCreateCacheKeyNoRealmNoTarget() {
        final AccessTokenRecord accessToken = new AccessTokenRecord();
        accessToken.setHomeAccountId(HOME_ACCOUNT_ID);
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setClientId(CLIENT_ID);

        final String expectedKey = "" // just for formatting
                + HOME_ACCOUNT_ID + CACHE_VALUE_SEPARATOR
                + ENVIRONMENT + CACHE_VALUE_SEPARATOR
                + CREDENTIAL_TYPE_ACCESS_TOKEN + CACHE_VALUE_SEPARATOR
                + CLIENT_ID + CACHE_VALUE_SEPARATOR
                + CACHE_VALUE_SEPARATOR;

        assertEquals(expectedKey, mDelegate.generateCacheKey(accessToken));
    }

    @Test
    public void accessTokenCreateCacheValue() throws JSONException {
        final AccessTokenRecord accessToken = new AccessTokenRecord();
        accessToken.setHomeAccountId(HOME_ACCOUNT_ID);
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setCredentialType(CredentialType.AccessToken.name().toLowerCase(Locale.US));
        accessToken.setClientId(CLIENT_ID);
        accessToken.setRealm(REALM);
        accessToken.setTarget(TARGET);

        final String serializedValue = mDelegate.generateCacheValue(accessToken);

        // Turn the serialized value into a JSONObject and start testing field equality.
        final JSONObject jsonObject = new JSONObject(serializedValue);
        assertEquals(HOME_ACCOUNT_ID, jsonObject.getString(Credential.SerializedNames.HOME_ACCOUNT_ID));
        assertEquals(ENVIRONMENT, jsonObject.getString(Credential.SerializedNames.ENVIRONMENT));
        assertEquals(CredentialType.AccessToken.name().toLowerCase(Locale.US), jsonObject.getString("credential_type"));
        assertEquals(CLIENT_ID, jsonObject.getString(Credential.SerializedNames.CLIENT_ID));
        assertEquals(REALM, jsonObject.getString(AccessTokenRecord.SerializedNames.REALM));
        assertEquals(TARGET, jsonObject.getString(AccessTokenRecord.SerializedNames.TARGET));
    }

    @Test
    public void accessTokenExtraValueSerialization() throws JSONException {
        final AccessTokenRecord accessToken = new AccessTokenRecord();
        accessToken.setHomeAccountId(HOME_ACCOUNT_ID);
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
        assertEquals(HOME_ACCOUNT_ID, derivedCacheValueJsonObject.getString(Credential.SerializedNames.HOME_ACCOUNT_ID));
        assertEquals(ENVIRONMENT, derivedCacheValueJsonObject.getString(Credential.SerializedNames.ENVIRONMENT));
        assertEquals(CredentialType.AccessToken.name().toLowerCase(Locale.US), derivedCacheValueJsonObject.getString("credential_type"));
        assertEquals(CLIENT_ID, derivedCacheValueJsonObject.getString(Credential.SerializedNames.CLIENT_ID));
        assertEquals(REALM, derivedCacheValueJsonObject.getString(AccessTokenRecord.SerializedNames.REALM));
        assertEquals(TARGET, derivedCacheValueJsonObject.getString(AccessTokenRecord.SerializedNames.TARGET));
        assertEquals("bar", derivedCacheValueJsonObject.getString("foo"));

        final JSONArray jsonArr = derivedCacheValueJsonObject.getJSONArray("numbers");
        assertEquals(3, jsonArr.length());

        final JSONObject jsonObj = derivedCacheValueJsonObject.getJSONObject("object");
        assertEquals("object_value", jsonObj.getString("object_key"));
    }

    @Test
    public void accessTokenExtraValueDeserialization() throws JSONException {
        final AccessTokenRecord accessToken = new AccessTokenRecord();
        accessToken.setHomeAccountId(HOME_ACCOUNT_ID);
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

        final AccessTokenRecord deserializedValue = mDelegate.fromCacheValue(serializedValue, AccessTokenRecord.class);
        assertNotNull(deserializedValue);
        assertNull(deserializedValue.getAdditionalFields().get(Credential.SerializedNames.ENVIRONMENT));
        assertEquals(HOME_ACCOUNT_ID, deserializedValue.getHomeAccountId());
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
    public void accountCreateCacheKeyComplete() {
        final AccountRecord account = new AccountRecord();
        account.setHomeAccountId(HOME_ACCOUNT_ID);
        account.setEnvironment(ENVIRONMENT);
        account.setRealm(REALM);

        final String expectedKey = ""
                + HOME_ACCOUNT_ID + CACHE_VALUE_SEPARATOR
                + ENVIRONMENT + CACHE_VALUE_SEPARATOR
                + REALM;

        assertEquals(expectedKey, mDelegate.generateCacheKey(account));
    }

    @Test
    public void accountCreateCacheKeyCompleteNoHomeAccountId() {
        final AccountRecord account = new AccountRecord();
        account.setEnvironment(ENVIRONMENT);
        account.setRealm(REALM);

        final String expectedKey = ""
                + CACHE_VALUE_SEPARATOR
                + ENVIRONMENT + CACHE_VALUE_SEPARATOR
                + REALM;

        assertEquals(expectedKey, mDelegate.generateCacheKey(account));
    }

    @Test
    public void accountCreateCacheKeyCompleteNoRealm() {
        final AccountRecord account = new AccountRecord();
        account.setHomeAccountId(HOME_ACCOUNT_ID);
        account.setEnvironment(ENVIRONMENT);

        final String expectedKey = ""
                + HOME_ACCOUNT_ID + CACHE_VALUE_SEPARATOR
                + ENVIRONMENT + CACHE_VALUE_SEPARATOR;

        assertEquals(expectedKey, mDelegate.generateCacheKey(account));
    }

    @Test
    public void accountCreateCacheKeyCompleteNoHomeAccountIdNoRealm() {
        final AccountRecord account = new AccountRecord();
        account.setEnvironment(ENVIRONMENT);

        final String expectedKey = ""
                + CACHE_VALUE_SEPARATOR
                + ENVIRONMENT + CACHE_VALUE_SEPARATOR;

        assertEquals(expectedKey, mDelegate.generateCacheKey(account));
    }

    @Test
    public void accountCreateCacheValue() throws JSONException {
        final AccountRecord account = new AccountRecord();
        account.setHomeAccountId(HOME_ACCOUNT_ID);
        account.setEnvironment(ENVIRONMENT);
        account.setRealm(REALM);
        account.setLocalAccountId(LOCAL_ACCOUNT_ID);
        account.setAuthorityType(AUTHORITY_TYPE);
        account.setAlternativeAccountId(ALTERNATIVE_ACCOUNT_ID);
        account.setFirstName(FIRST_NAME);
        account.setFamilyName(FAMILY_NAME);
        account.setAvatarUrl(AVATAR_URL);

        final String serializedValue = mDelegate.generateCacheValue(account);

        final JSONObject jsonObject = new JSONObject(serializedValue);
        assertEquals(HOME_ACCOUNT_ID, jsonObject.getString(AccountRecord.SerializedNames.HOME_ACCOUNT_ID));
        assertEquals(ENVIRONMENT, jsonObject.getString(AccountRecord.SerializedNames.ENVIRONMENT));
        assertEquals(REALM, jsonObject.getString(AccountRecord.SerializedNames.REALM));
        assertEquals(LOCAL_ACCOUNT_ID, jsonObject.getString("local_account_id"));
        assertEquals(AUTHORITY_TYPE, jsonObject.getString("authority_type"));
        assertEquals(ALTERNATIVE_ACCOUNT_ID, jsonObject.getString("alternative_account_id"));
        assertEquals(FIRST_NAME, jsonObject.getString("first_name"));
        assertEquals(FAMILY_NAME, jsonObject.getString("family_name"));
        assertEquals(AVATAR_URL, jsonObject.getString("avatar_url"));
    }

    @Test
    public void accountExtraValueSerialization() throws JSONException {
        final AccountRecord account
                = new AccountRecord();
        account.setHomeAccountId(HOME_ACCOUNT_ID);
        account.setEnvironment(ENVIRONMENT);
        account.setRealm(REALM);
        account.setLocalAccountId(LOCAL_ACCOUNT_ID);
        account.setAuthorityType(AUTHORITY_TYPE);
        account.setAlternativeAccountId(ALTERNATIVE_ACCOUNT_ID);
        account.setFirstName(FIRST_NAME);
        account.setFamilyName(FAMILY_NAME);
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
        assertEquals(HOME_ACCOUNT_ID, derivedCacheValueJsonObject.getString(AccountRecord.SerializedNames.HOME_ACCOUNT_ID));
        assertEquals(ENVIRONMENT, derivedCacheValueJsonObject.getString(AccountRecord.SerializedNames.ENVIRONMENT));
        assertEquals(REALM, derivedCacheValueJsonObject.getString(AccountRecord.SerializedNames.REALM));
        assertEquals(LOCAL_ACCOUNT_ID, derivedCacheValueJsonObject.get("local_account_id"));
        assertEquals(AUTHORITY_TYPE, derivedCacheValueJsonObject.get("authority_type"));
        assertEquals(ALTERNATIVE_ACCOUNT_ID, derivedCacheValueJsonObject.get("alternative_account_id"));
        assertEquals(FIRST_NAME, derivedCacheValueJsonObject.get("first_name"));
        assertEquals(FAMILY_NAME, derivedCacheValueJsonObject.get("family_name"));
        assertEquals(AVATAR_URL, derivedCacheValueJsonObject.get("avatar_url"));
        assertEquals("bar", derivedCacheValueJsonObject.getString("foo"));

        final JSONArray jsonArr = derivedCacheValueJsonObject.getJSONArray("numbers");
        assertEquals(3, jsonArr.length());

        final JSONObject jsonObj = derivedCacheValueJsonObject.getJSONObject("object");
        assertEquals("object_value", jsonObj.getString("object_key"));
    }

    @Test
    public void accountExtraValueDeserialization() throws JSONException {
        final AccountRecord account
                = new AccountRecord();
        account.setHomeAccountId(HOME_ACCOUNT_ID);
        account.setEnvironment(ENVIRONMENT);
        account.setRealm(REALM);
        account.setLocalAccountId(LOCAL_ACCOUNT_ID);
        account.setAuthorityType(AUTHORITY_TYPE);
        account.setAlternativeAccountId(ALTERNATIVE_ACCOUNT_ID);
        account.setFirstName(FIRST_NAME);
        account.setFamilyName(FAMILY_NAME);
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

        final AccountRecord deserializedValue
                = mDelegate.fromCacheValue(serializedValue, AccountRecord.class);
        assertNotNull(deserializedValue);
        assertNull(deserializedValue.getAdditionalFields().get(AccountRecord.SerializedNames.ENVIRONMENT));
        assertEquals(HOME_ACCOUNT_ID, deserializedValue.getHomeAccountId());
        assertEquals(ENVIRONMENT, deserializedValue.getEnvironment());
        assertEquals(REALM, deserializedValue.getRealm());
        assertEquals(LOCAL_ACCOUNT_ID, deserializedValue.getLocalAccountId());
        assertEquals(AUTHORITY_TYPE, deserializedValue.getAuthorityType());
        assertEquals(ALTERNATIVE_ACCOUNT_ID, deserializedValue.getAlternativeAccountId());
        assertEquals(FIRST_NAME, deserializedValue.getFirstName());
        assertEquals(FAMILY_NAME, deserializedValue.getFamilyName());
        assertEquals(AVATAR_URL, deserializedValue.getAvatarUrl());
        assertEquals(3, deserializedValue.getAdditionalFields().size());
        assertEquals("bar", deserializedValue.getAdditionalFields().get("foo").getAsString());
        assertEquals(numbers.toString(), deserializedValue.getAdditionalFields().get("numbers").toString());
    }
    // End Accounts

    // RefreshTokens
    @Test
    public void refreshTokenCreateCacheKeyComplete() {
        final RefreshTokenRecord refreshToken = new RefreshTokenRecord();
        refreshToken.setHomeAccountId(HOME_ACCOUNT_ID);
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setCredentialType(CredentialType.RefreshToken.name());
        refreshToken.setClientId(CLIENT_ID);
        refreshToken.setTarget(TARGET);

        final String expectedKey = "" // just for formatting
                + HOME_ACCOUNT_ID + CACHE_VALUE_SEPARATOR
                + ENVIRONMENT + CACHE_VALUE_SEPARATOR
                + CREDENTIAL_TYPE_REFRESH_TOKEN + CACHE_VALUE_SEPARATOR
                + CLIENT_ID + CACHE_VALUE_SEPARATOR
                + CACHE_VALUE_SEPARATOR
                + TARGET;
        assertEquals(expectedKey, mDelegate.generateCacheKey(refreshToken));
    }

    @Test
    public void refreshTokenCreateCacheKeyCompleteWithFoci() {
        final RefreshTokenRecord refreshToken = new RefreshTokenRecord();
        refreshToken.setHomeAccountId(HOME_ACCOUNT_ID);
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setCredentialType(CredentialType.RefreshToken.name());
        refreshToken.setClientId(CLIENT_ID);
        refreshToken.setFamilyId("1");
        refreshToken.setTarget(TARGET);

        final String expectedKey = "" // just for formatting
                + HOME_ACCOUNT_ID + CACHE_VALUE_SEPARATOR
                + ENVIRONMENT + CACHE_VALUE_SEPARATOR
                + CREDENTIAL_TYPE_REFRESH_TOKEN + CACHE_VALUE_SEPARATOR
                + "1" + CACHE_VALUE_SEPARATOR
                + CACHE_VALUE_SEPARATOR
                + TARGET;
        assertEquals(expectedKey, mDelegate.generateCacheKey(refreshToken));
    }

    @Test
    public void refreshTokenCreateCacheKeyCompleteWithFociPrefixed() {
        final RefreshTokenRecord refreshToken = new RefreshTokenRecord();
        refreshToken.setHomeAccountId(HOME_ACCOUNT_ID);
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setCredentialType(CredentialType.RefreshToken.name());
        refreshToken.setClientId(CLIENT_ID);
        refreshToken.setFamilyId("foci-1");
        refreshToken.setTarget(TARGET);

        final String expectedKey = "" // just for formatting
                + HOME_ACCOUNT_ID + CACHE_VALUE_SEPARATOR
                + ENVIRONMENT + CACHE_VALUE_SEPARATOR
                + CREDENTIAL_TYPE_REFRESH_TOKEN + CACHE_VALUE_SEPARATOR
                + "1" + CACHE_VALUE_SEPARATOR
                + CACHE_VALUE_SEPARATOR
                + TARGET;
        assertEquals(expectedKey, mDelegate.generateCacheKey(refreshToken));
    }

    @Test
    public void refreshTokenCreateCacheKeyCompleteWithFociAlternate() {
        final RefreshTokenRecord refreshToken = new RefreshTokenRecord();
        refreshToken.setHomeAccountId(HOME_ACCOUNT_ID);
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setCredentialType(CredentialType.RefreshToken.name());
        refreshToken.setClientId(CLIENT_ID);
        refreshToken.setFamilyId("2");
        refreshToken.setTarget(TARGET);

        final String expectedKey = "" // just for formatting
                + HOME_ACCOUNT_ID + CACHE_VALUE_SEPARATOR
                + ENVIRONMENT + CACHE_VALUE_SEPARATOR
                + CREDENTIAL_TYPE_REFRESH_TOKEN + CACHE_VALUE_SEPARATOR
                + "2" + CACHE_VALUE_SEPARATOR
                + CACHE_VALUE_SEPARATOR
                + TARGET;
        assertEquals(expectedKey, mDelegate.generateCacheKey(refreshToken));
    }

    @Test
    public void refreshTokenCreateCacheKeyCompleteWithFociPrefixedAlternate() {
        final RefreshTokenRecord refreshToken = new RefreshTokenRecord();
        refreshToken.setHomeAccountId(HOME_ACCOUNT_ID);
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setCredentialType(CredentialType.RefreshToken.name());
        refreshToken.setClientId(CLIENT_ID);
        refreshToken.setFamilyId("foci-2");
        refreshToken.setTarget(TARGET);

        final String expectedKey = "" // just for formatting
                + HOME_ACCOUNT_ID + CACHE_VALUE_SEPARATOR
                + ENVIRONMENT + CACHE_VALUE_SEPARATOR
                + CREDENTIAL_TYPE_REFRESH_TOKEN + CACHE_VALUE_SEPARATOR
                + "2" + CACHE_VALUE_SEPARATOR
                + CACHE_VALUE_SEPARATOR
                + TARGET;
        assertEquals(expectedKey, mDelegate.generateCacheKey(refreshToken));
    }

    @Test
    public void refreshTokenCreateCacheKeyNoHomeAccountId() {
        final RefreshTokenRecord refreshToken = new RefreshTokenRecord();
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setCredentialType(CredentialType.RefreshToken.name());
        refreshToken.setClientId(CLIENT_ID);
        refreshToken.setTarget(TARGET);

        final String expectedKey = "" // just for formatting
                + CACHE_VALUE_SEPARATOR
                + ENVIRONMENT + CACHE_VALUE_SEPARATOR
                + CREDENTIAL_TYPE_REFRESH_TOKEN + CACHE_VALUE_SEPARATOR
                + CLIENT_ID + CACHE_VALUE_SEPARATOR
                + CACHE_VALUE_SEPARATOR
                + TARGET;
        assertEquals(expectedKey, mDelegate.generateCacheKey(refreshToken));
    }

    @Test
    public void refreshTokenCreateCacheKeyNoRealm() {
        final RefreshTokenRecord refreshToken = new RefreshTokenRecord();
        refreshToken.setHomeAccountId(HOME_ACCOUNT_ID);
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setCredentialType(CredentialType.RefreshToken.name());
        refreshToken.setClientId(CLIENT_ID);
        refreshToken.setTarget(TARGET);

        final String expectedKey = "" // just for formatting
                + HOME_ACCOUNT_ID + CACHE_VALUE_SEPARATOR
                + ENVIRONMENT + CACHE_VALUE_SEPARATOR
                + CREDENTIAL_TYPE_REFRESH_TOKEN + CACHE_VALUE_SEPARATOR
                + CLIENT_ID + CACHE_VALUE_SEPARATOR
                + CACHE_VALUE_SEPARATOR
                + TARGET;
        assertEquals(expectedKey, mDelegate.generateCacheKey(refreshToken));
    }

    @Test
    public void refreshTokenCreateCacheKeyNoTarget() {
        final RefreshTokenRecord refreshToken = new RefreshTokenRecord();
        refreshToken.setHomeAccountId(HOME_ACCOUNT_ID);
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setCredentialType(CredentialType.RefreshToken.name());
        refreshToken.setClientId(CLIENT_ID);

        final String expectedKey = "" // just for formatting
                + HOME_ACCOUNT_ID + CACHE_VALUE_SEPARATOR
                + ENVIRONMENT + CACHE_VALUE_SEPARATOR
                + CREDENTIAL_TYPE_REFRESH_TOKEN + CACHE_VALUE_SEPARATOR
                + CLIENT_ID + CACHE_VALUE_SEPARATOR
                + CACHE_VALUE_SEPARATOR;

        assertEquals(expectedKey, mDelegate.generateCacheKey(refreshToken));
    }

    @Test
    public void refreshTokenCreateCacheKeyNoHomeAccountIdNoTarget() {
        final RefreshTokenRecord refreshToken = new RefreshTokenRecord();
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setCredentialType(CredentialType.RefreshToken.name());
        refreshToken.setClientId(CLIENT_ID);

        final String expectedKey = "" // just for formatting
                + CACHE_VALUE_SEPARATOR
                + ENVIRONMENT + CACHE_VALUE_SEPARATOR
                + CREDENTIAL_TYPE_REFRESH_TOKEN + CACHE_VALUE_SEPARATOR
                + CLIENT_ID + CACHE_VALUE_SEPARATOR
                + CACHE_VALUE_SEPARATOR;

        assertEquals(expectedKey, mDelegate.generateCacheKey(refreshToken));
    }

    @Test
    public void refreshTokenCreateCacheValue() throws JSONException {
        final RefreshTokenRecord refreshToken = new RefreshTokenRecord();
        refreshToken.setHomeAccountId(HOME_ACCOUNT_ID);
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setCredentialType(CredentialType.RefreshToken.name().toLowerCase(Locale.US));
        refreshToken.setClientId(CLIENT_ID);
        refreshToken.setTarget(TARGET);

        final String serializedValue = mDelegate.generateCacheValue(refreshToken);

        // Turn the serialized value into a JSONObject and start testing field equality.
        final JSONObject jsonObject = new JSONObject(serializedValue);
        assertEquals(HOME_ACCOUNT_ID, jsonObject.getString(RefreshTokenRecord.SerializedNames.HOME_ACCOUNT_ID));
        assertEquals(ENVIRONMENT, jsonObject.getString(RefreshTokenRecord.SerializedNames.ENVIRONMENT));
        assertEquals(CredentialType.RefreshToken.name().toLowerCase(Locale.US), jsonObject.getString("credential_type"));
        assertEquals(CLIENT_ID, jsonObject.getString(RefreshTokenRecord.SerializedNames.CLIENT_ID));
        assertEquals(TARGET, jsonObject.getString(RefreshTokenRecord.SerializedNames.TARGET));
    }

    @Test
    public void refreshTokenExtraValueSerialization() throws JSONException {
        final RefreshTokenRecord refreshToken = new RefreshTokenRecord();
        refreshToken.setHomeAccountId(HOME_ACCOUNT_ID);
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
        assertEquals(HOME_ACCOUNT_ID, derivedCacheValueJsonObject.getString(RefreshTokenRecord.SerializedNames.HOME_ACCOUNT_ID));
        assertEquals(ENVIRONMENT, derivedCacheValueJsonObject.getString(RefreshTokenRecord.SerializedNames.ENVIRONMENT));
        assertEquals(CredentialType.RefreshToken.name().toLowerCase(Locale.US), derivedCacheValueJsonObject.getString("credential_type"));
        assertEquals(CLIENT_ID, derivedCacheValueJsonObject.getString(RefreshTokenRecord.SerializedNames.CLIENT_ID));
        assertEquals(TARGET, derivedCacheValueJsonObject.getString(RefreshTokenRecord.SerializedNames.TARGET));
        assertEquals("bar", derivedCacheValueJsonObject.getString("foo"));

        final JSONArray jsonArr = derivedCacheValueJsonObject.getJSONArray("numbers");
        assertEquals(3, jsonArr.length());

        final JSONObject jsonObj = derivedCacheValueJsonObject.getJSONObject("object");
        assertEquals("object_value", jsonObj.getString("object_key"));
    }

    @Test
    public void refreshTokenExtraValueDeserialization() throws JSONException {
        final RefreshTokenRecord refreshToken = new RefreshTokenRecord();
        refreshToken.setHomeAccountId(HOME_ACCOUNT_ID);
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setCredentialType(CredentialType.RefreshToken.name().toLowerCase(Locale.US));
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

        final RefreshTokenRecord deserializedValue = mDelegate.fromCacheValue(serializedValue, RefreshTokenRecord.class);
        assertNotNull(deserializedValue);
        assertNull(deserializedValue.getAdditionalFields().get(Credential.SerializedNames.ENVIRONMENT));
        assertEquals(HOME_ACCOUNT_ID, deserializedValue.getHomeAccountId());
        assertEquals(ENVIRONMENT, deserializedValue.getEnvironment());
        assertEquals(CredentialType.RefreshToken.name().toLowerCase(Locale.US), deserializedValue.getCredentialType());
        assertEquals(CLIENT_ID, deserializedValue.getClientId());
        assertEquals(TARGET, deserializedValue.getTarget());
        assertEquals(3, deserializedValue.getAdditionalFields().size());
        assertEquals("bar", deserializedValue.getAdditionalFields().get("foo").getAsString());
        assertEquals(numbers.toString(), deserializedValue.getAdditionalFields().get("numbers").toString());
    }
    // End RefreshTokens

    // IdTokens
    @Test
    public void idTokenCreateCacheKeyComplete() {
        final IdTokenRecord idToken = new IdTokenRecord();
        idToken.setHomeAccountId(HOME_ACCOUNT_ID);
        idToken.setEnvironment(ENVIRONMENT);
        idToken.setCredentialType(CredentialType.IdToken.name());
        idToken.setClientId(CLIENT_ID);
        idToken.setRealm(REALM);

        final String expectedKey = "" // just for formatting
                + HOME_ACCOUNT_ID + CACHE_VALUE_SEPARATOR
                + ENVIRONMENT + CACHE_VALUE_SEPARATOR
                + CREDENTIAL_TYPE_ID_TOKEN + CACHE_VALUE_SEPARATOR
                + CLIENT_ID + CACHE_VALUE_SEPARATOR
                + REALM + CACHE_VALUE_SEPARATOR;
        assertEquals(expectedKey, mDelegate.generateCacheKey(idToken));
    }

    @Test
    public void idTokenCreateCacheKeyNoHomeAccountId() {
        final IdTokenRecord idToken = new IdTokenRecord();
        idToken.setEnvironment(ENVIRONMENT);
        idToken.setCredentialType(CredentialType.IdToken.name());
        idToken.setClientId(CLIENT_ID);
        idToken.setRealm(REALM);

        final String expectedKey = "" // just for formatting
                + CACHE_VALUE_SEPARATOR
                + ENVIRONMENT + CACHE_VALUE_SEPARATOR
                + CREDENTIAL_TYPE_ID_TOKEN + CACHE_VALUE_SEPARATOR
                + CLIENT_ID + CACHE_VALUE_SEPARATOR
                + REALM + CACHE_VALUE_SEPARATOR;
        assertEquals(expectedKey, mDelegate.generateCacheKey(idToken));
    }

    @Test
    public void idTokenCreateCacheKeyNoRealm() {
        final IdTokenRecord idToken = new IdTokenRecord();
        idToken.setHomeAccountId(HOME_ACCOUNT_ID);
        idToken.setEnvironment(ENVIRONMENT);
        idToken.setCredentialType(CredentialType.IdToken.name());
        idToken.setClientId(CLIENT_ID);

        final String expectedKey = "" // just for formatting
                + HOME_ACCOUNT_ID + CACHE_VALUE_SEPARATOR
                + ENVIRONMENT + CACHE_VALUE_SEPARATOR
                + CREDENTIAL_TYPE_ID_TOKEN + CACHE_VALUE_SEPARATOR
                + CLIENT_ID + CACHE_VALUE_SEPARATOR
                + CACHE_VALUE_SEPARATOR;
        assertEquals(expectedKey, mDelegate.generateCacheKey(idToken));
    }

    @Test
    public void idTokenCreateCacheValue() throws JSONException {
        final IdTokenRecord idToken = new IdTokenRecord();
        idToken.setHomeAccountId(HOME_ACCOUNT_ID);
        idToken.setEnvironment(ENVIRONMENT);
        idToken.setCredentialType(CredentialType.IdToken.name().toLowerCase(Locale.US));
        idToken.setClientId(CLIENT_ID);
        idToken.setRealm(REALM);

        final String serializedValue = mDelegate.generateCacheValue(idToken);

        // Turn the serialized value into a JSONObject and start testing field equality.
        final JSONObject jsonObject = new JSONObject(serializedValue);
        assertEquals(HOME_ACCOUNT_ID, jsonObject.getString(Credential.SerializedNames.HOME_ACCOUNT_ID));
        assertEquals(ENVIRONMENT, jsonObject.getString(Credential.SerializedNames.ENVIRONMENT));
        assertEquals(CredentialType.IdToken.name().toLowerCase(Locale.US), jsonObject.getString("credential_type"));
        assertEquals(CLIENT_ID, jsonObject.getString(Credential.SerializedNames.CLIENT_ID));
        assertEquals(REALM, jsonObject.getString(IdTokenRecord.SerializedNames.REALM));
    }

    @Test
    public void idTokenExtraValueSerialization() throws JSONException {
        final IdTokenRecord idToken = new IdTokenRecord();
        idToken.setHomeAccountId(HOME_ACCOUNT_ID);
        idToken.setEnvironment(ENVIRONMENT);
        idToken.setCredentialType(CredentialType.IdToken.name().toLowerCase(Locale.US));
        idToken.setClientId(CLIENT_ID);
        idToken.setRealm(REALM);

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

        idToken.setAdditionalFields(additionalFields);

        String serializedValue = mDelegate.generateCacheValue(idToken);
        JSONObject derivedCacheValueJsonObject = new JSONObject(serializedValue);
        assertEquals(HOME_ACCOUNT_ID, derivedCacheValueJsonObject.getString(Credential.SerializedNames.HOME_ACCOUNT_ID));
        assertEquals(ENVIRONMENT, derivedCacheValueJsonObject.getString(Credential.SerializedNames.ENVIRONMENT));
        assertEquals(CredentialType.IdToken.name().toLowerCase(Locale.US), derivedCacheValueJsonObject.getString("credential_type"));
        assertEquals(CLIENT_ID, derivedCacheValueJsonObject.getString(Credential.SerializedNames.CLIENT_ID));
        assertEquals(REALM, derivedCacheValueJsonObject.getString(IdTokenRecord.SerializedNames.REALM));
        assertEquals("bar", derivedCacheValueJsonObject.getString("foo"));

        final JSONArray jsonArr = derivedCacheValueJsonObject.getJSONArray("numbers");
        assertEquals(3, jsonArr.length());

        final JSONObject jsonObj = derivedCacheValueJsonObject.getJSONObject("object");
        assertEquals("object_value", jsonObj.getString("object_key"));
    }

    @Test
    public void idTokenExtraValueDeserialization() throws JSONException {
        final IdTokenRecord idToken = new IdTokenRecord();
        idToken.setHomeAccountId(HOME_ACCOUNT_ID);
        idToken.setEnvironment(ENVIRONMENT);
        idToken.setCredentialType(CredentialType.IdToken.name().toLowerCase(Locale.US));
        idToken.setClientId(CLIENT_ID);
        idToken.setRealm(REALM);

        String serializedValue = mDelegate.generateCacheValue(idToken);

        // Turn the serialized value into a JSONObject and start testing field equality.
        final JSONObject jsonObject = new JSONObject(serializedValue);

        // Add more non-standard data to this object...
        final JSONArray numbers = new JSONArray("[1, 2, 3]");
        final JSONArray objects = new JSONArray("[{\"hello\" : \"hallo\"}, {\"goodbye\" : \"auf wiedersehen\"}]");

        jsonObject.put("foo", "bar");
        jsonObject.put("numbers", numbers);
        jsonObject.put("objects", objects);

        serializedValue = jsonObject.toString();

        final IdTokenRecord deserializedValue = mDelegate.fromCacheValue(serializedValue, IdTokenRecord.class);
        assertNotNull(deserializedValue);
        assertNull(deserializedValue.getAdditionalFields().get(Credential.SerializedNames.ENVIRONMENT));
        assertEquals(HOME_ACCOUNT_ID, deserializedValue.getHomeAccountId());
        assertEquals(ENVIRONMENT, deserializedValue.getEnvironment());
        assertEquals(CredentialType.IdToken.name().toLowerCase(Locale.US), deserializedValue.getCredentialType());
        assertEquals(CLIENT_ID, deserializedValue.getClientId());
        assertEquals(REALM, deserializedValue.getRealm());
        assertEquals(3, deserializedValue.getAdditionalFields().size());
        assertEquals("bar", deserializedValue.getAdditionalFields().get("foo").getAsString());
        assertEquals(numbers.toString(), deserializedValue.getAdditionalFields().get("numbers").toString());
    }
    // End IdTokens
}
