package com.microsoft.identity.common.internal.cache;

import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.internal.dto.AccessToken;
import com.microsoft.identity.common.internal.dto.Account;
import com.microsoft.identity.common.internal.dto.AccountCredentialBase;
import com.microsoft.identity.common.internal.dto.Credential;
import com.microsoft.identity.common.internal.dto.IdToken;
import com.microsoft.identity.common.internal.dto.RefreshToken;
import com.microsoft.identity.common.internal.logging.Logger;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static com.microsoft.identity.common.internal.cache.CacheKeyValueDelegate.CacheKeyReplacements.CLIENT_ID;
import static com.microsoft.identity.common.internal.cache.CacheKeyValueDelegate.CacheKeyReplacements.CREDENTIAL_TYPE;
import static com.microsoft.identity.common.internal.cache.CacheKeyValueDelegate.CacheKeyReplacements.ENVIRONMENT;
import static com.microsoft.identity.common.internal.cache.CacheKeyValueDelegate.CacheKeyReplacements.REALM;
import static com.microsoft.identity.common.internal.cache.CacheKeyValueDelegate.CacheKeyReplacements.TARGET;
import static com.microsoft.identity.common.internal.cache.CacheKeyValueDelegate.CacheKeyReplacements.UNIQUE_USER_ID;

/**
 * Uses Gson to serialize instances of <T> into {@link String}s.
 */
public class CacheKeyValueDelegate implements ICacheKeyValueDelegate {

    private static final String TAG = CacheKeyValueDelegate.class.getSimpleName();

    public static final String CACHE_VALUE_SEPARATOR = "-";

    private final Gson mGson;

    public CacheKeyValueDelegate() {
        mGson = new Gson();
        Logger.verbose(TAG, "Init: " + TAG);
    }

    static class CacheKeyReplacements {
        static final String UNIQUE_USER_ID = "<unique_user_id>";
        static final String ENVIRONMENT = "<environment>";
        static final String REALM = "<realm>";
        static final String CREDENTIAL_TYPE = "<credential_type>";
        static final String CLIENT_ID = "<client_id>";
        static final String TARGET = "<target>";
    }

    private static String sanitizeNull(final String input) {
        final String methodName = "sanitizeNull";
        Logger.entering(TAG, methodName, input);

        String outValue = null == input ? "" : input.toLowerCase(Locale.US).trim();

        Logger.exiting(TAG, methodName, outValue);

        return outValue;
    }

    @Override
    public String generateCacheKey(Account account) {
        final String methodName = "generateCacheKey";
        Logger.entering(TAG, methodName, account);

        String cacheKey = UNIQUE_USER_ID
                + CACHE_VALUE_SEPARATOR
                + ENVIRONMENT
                + CACHE_VALUE_SEPARATOR
                + REALM;
        cacheKey = cacheKey.replace(UNIQUE_USER_ID, sanitizeNull(account.getUniqueUserId()));
        cacheKey = cacheKey.replace(ENVIRONMENT, sanitizeNull(account.getEnvironment()));
        cacheKey = cacheKey.replace(REALM, sanitizeNull(account.getRealm()));

        Logger.exiting(TAG, methodName, cacheKey);

        return cacheKey;
    }

    private String generateCacheValueInternal(final Object baseObject) {
        final String methodName = "generateCacheValueInternal";
        Logger.entering(TAG, methodName, baseObject);

        JsonElement outboundElement = mGson.toJsonTree(baseObject);
        JsonObject outboundObject = outboundElement.getAsJsonObject();

        if (baseObject instanceof AccountCredentialBase) {
            final AccountCredentialBase accountCredentialBase = (AccountCredentialBase) baseObject;
            // This basically acts as a custom serializer for AccountCredentialBase objects
            // by iterating over the additionalFields Map and JSON-ifying them
            for (final String key : accountCredentialBase.getAdditionalFields().keySet()) {
                outboundObject.add(key, accountCredentialBase.getAdditionalFields().get(key));
            }
        }

        final String json = mGson.toJson(outboundObject);

        Logger.exiting(TAG, methodName, json);

        return json;
    }

    @Override
    public String generateCacheValue(Account account) {
        final String methodName = "generateCacheValue";
        Logger.entering(TAG, methodName, account);

        final String result = generateCacheValueInternal(account);

        Logger.exiting(TAG, methodName, result);

        return result;
    }

    @Override
    public String generateCacheKey(Credential credential) {
        final String methodName = "generateCacheKey";
        Logger.entering(TAG, methodName, credential);

        String cacheKey =
                UNIQUE_USER_ID + CACHE_VALUE_SEPARATOR
                        + ENVIRONMENT + CACHE_VALUE_SEPARATOR
                        + CREDENTIAL_TYPE + CACHE_VALUE_SEPARATOR
                        + CLIENT_ID + CACHE_VALUE_SEPARATOR
                        + REALM + CACHE_VALUE_SEPARATOR
                        + TARGET;
        cacheKey = cacheKey.replace(UNIQUE_USER_ID, sanitizeNull(credential.getUniqueUserId()));
        cacheKey = cacheKey.replace(ENVIRONMENT, sanitizeNull(credential.getEnvironment()));
        cacheKey = cacheKey.replace(CREDENTIAL_TYPE, sanitizeNull(credential.getCredentialType()));
        cacheKey = cacheKey.replace(CLIENT_ID, sanitizeNull(credential.getClientId()));

        if (credential instanceof AccessToken) {
            final AccessToken accessToken = (AccessToken) credential;
            cacheKey = cacheKey.replace(REALM, sanitizeNull(accessToken.getRealm()));
            cacheKey = cacheKey.replace(TARGET, sanitizeNull(accessToken.getTarget()));
        } else if (credential instanceof RefreshToken) {
            final RefreshToken refreshToken = (RefreshToken) credential;
            cacheKey = cacheKey.replace(REALM, "");
            cacheKey = cacheKey.replace(TARGET, sanitizeNull(refreshToken.getTarget()));
        } else if (credential instanceof IdToken) {
            final IdToken idToken = (IdToken) credential;
            cacheKey = cacheKey.replace(REALM, sanitizeNull(idToken.getRealm()));
            cacheKey = cacheKey.replace(TARGET, "");
        }

        Logger.exiting(TAG, methodName, cacheKey);

        return cacheKey;
    }

    @Override
    public String generateCacheValue(Credential credential) {
        final String methodName = "generateCacheValue";
        Logger.entering(TAG, methodName, credential);

        final String result = generateCacheValueInternal(credential);

        Logger.exiting(TAG, methodName, result);

        return result;
    }

    @Override
    public <T extends AccountCredentialBase> T fromCacheValue(String string, Class<? extends AccountCredentialBase> t) {
        final String methodName = "fromCacheValue";
        Logger.entering(TAG, methodName, string, t);

        try {
            final T resultObject = (T) mGson.fromJson(string, t);

            if (!StringExtensions.isNullOrBlank(string)) {
                // Turn the incoming String into a JSONObject
                final JsonObject incomingJson = new JsonParser().parse(string).getAsJsonObject();

                // Get all of the fields we were expecting to get
                final Set<String> expectedFields = getExpectedJsonFields(t);

                // Remove the expected fields from the initial JSONObject
                for (final String expectedField : expectedFields) {
                    incomingJson.remove(expectedField);
                }

                // Add whatever is leftover to the additionalFields Map
                final Map<String, JsonElement> additionalFields = new HashMap<>();

                for (final String key : incomingJson.keySet()) {
                    additionalFields.put(key, incomingJson.get(key));
                }

                resultObject.setAdditionalFields(additionalFields);
            }

            Logger.exiting(TAG, methodName, resultObject);

            // return the fully-formed object
            return resultObject;
        } catch (JsonSyntaxException e) {
            Logger.error(
                    TAG + ":" + methodName,
                    "Failed to parse cache value.",
                    null
            );
            Logger.errorPII(
                    TAG + ":" + methodName,
                    "Parsing failed with Exception",
                    e
            );
            return null;
        }
    }

    /**
     * For the supplied Class, return a Set of expected JSON values as dictated by @SerializedName
     * declared on its Fields.
     *
     * @param clazz The Class to inspect.
     * @return A Set of expected JSON values, as Strings.
     */
    private static Set<String> getExpectedJsonFields(final Class<? extends AccountCredentialBase> clazz) {
        final String methodName = "getExpectedJsonFields";
        Logger.entering(TAG, methodName, clazz);

        final Set<String> serializedNames = new HashSet<>();
        final List<Field> fieldsToInspect = getFieldsUpTo(clazz, AccountCredentialBase.class);
        final List<Field> annotatedFields = getSerializedNameAnnotatedFields(fieldsToInspect);

        for (final Field annotatedField : annotatedFields) {
            final SerializedName serializedName = annotatedField.getAnnotation(SerializedName.class);
            serializedNames.add(serializedName.value());
        }

        Logger.exiting(TAG, methodName, serializedNames);

        return serializedNames;
    }

    /**
     * For the supplied List of Fields, return those which are annotated with @SerializedName.
     *
     * @param fieldsToInspect The Fields to inspect.
     * @return Those Fields which are annotated with @SerializedName.
     */
    private static List<Field> getSerializedNameAnnotatedFields(final List<Field> fieldsToInspect) {
        final String methodName = "getSerializedNameAnnotatedFields";
        Logger.entering(TAG, methodName, fieldsToInspect);

        final List<Field> annotatedFields = new ArrayList<>();

        for (final Field field : fieldsToInspect) {
            SerializedName serializedName = field.getAnnotation(SerializedName.class);
            if (null != serializedName) {
                annotatedFields.add(field);
            }
        }

        Logger.exiting(TAG, methodName, annotatedFields);

        return annotatedFields;
    }

    /**
     * Recursively inspect the supplied Class to obtain its Fields up the inheritance hierarchy
     * to supplied upper-bound Class.
     *
     * @param startClass The base Class to inspect.
     * @param upperBound The Class' upper-bounded inheritor or null, if Object should be used.
     * @return A List of Fields on the supplied object and its superclasses.
     */
    private static List<Field> getFieldsUpTo(
            final Class<?> startClass,
            @Nullable Class<?> upperBound) {
        final String methodName = "getFieldsUpTo";
        Logger.entering(
                TAG,
                methodName,
                startClass.getClass().getSimpleName(),
                null != upperBound ? upperBound.getClass().getSimpleName() : null
        );

        List<Field> currentClassFields = new ArrayList<>(Arrays.asList(startClass.getDeclaredFields()));
        Class<?> parentClass = startClass.getSuperclass();

        if (parentClass != null && (upperBound == null || !(parentClass.equals(upperBound)))) {
            List<Field> parentClassFields = getFieldsUpTo(parentClass, upperBound);
            currentClassFields.addAll(parentClassFields);
        }

        Logger.exiting(TAG, methodName, currentClassFields);

        return currentClassFields;
    }
}
