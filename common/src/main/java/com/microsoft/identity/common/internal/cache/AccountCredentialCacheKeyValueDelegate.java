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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Uses Gson to serialize instances of <T> into {@link String}s.
 */
public class AccountCredentialCacheKeyValueDelegate implements IAccountCredentialCacheKeyValueDelegate {

    public static final String CACHE_VALUE_SEPARATOR = "-";

    private final Gson mGson;

    public AccountCredentialCacheKeyValueDelegate() {
        mGson = new Gson();
    }


    protected final String collapseKeyComponents(final List<String> keyComponents) {
        String cacheKey = "";

        for (String keyComponent : keyComponents) {
            if (!StringExtensions.isNullOrBlank(keyComponent)) {
                keyComponent = keyComponent.toLowerCase(Locale.US);
                cacheKey += keyComponent + CACHE_VALUE_SEPARATOR;
            }
        }

        if (cacheKey.endsWith(CACHE_VALUE_SEPARATOR)) {
            cacheKey = cacheKey.substring(0, cacheKey.length() - 1);
        }

        return cacheKey;
    }

    @Override
    public String generateCacheKey(Account account) {
        final List<String> keyComponents = new ArrayList<>();
        keyComponents.add(account.getUniqueUserId());
        keyComponents.add(account.getEnvironment());
        keyComponents.add(account.getRealm());

        return collapseKeyComponents(keyComponents);
    }

    private String generateCacheValueInternal(final AccountCredentialBase baseObject) {
        JsonElement outboundElement = mGson.toJsonTree(baseObject);
        JsonObject outboundObject = outboundElement.getAsJsonObject();

        // This basically acts as a custom serializer for AccountCredentialBase objects
        // by iterating over the additionalFields Map and JSON-ifying them
        for (final String key : baseObject.getAdditionalFields().keySet()) {
            outboundObject.add(key, baseObject.getAdditionalFields().get(key));
        }

        return mGson.toJson(outboundObject);
    }

    @Override
    public String generateCacheValue(Account account) {
        return generateCacheValueInternal(account);
    }

    @Override
    public String generateCacheKey(Credential credential) {
        final List<String> keyComponents = new ArrayList<>();
        keyComponents.add(credential.getUniqueUserId());
        keyComponents.add(credential.getEnvironment());
        keyComponents.add(credential.getCredentialType());
        keyComponents.add(credential.getClientId());

        if (credential instanceof AccessToken) {
            keyComponents.add(((AccessToken) credential).getRealm());
            keyComponents.add(((AccessToken) credential).getTarget());
        } else if (credential instanceof RefreshToken) {
            keyComponents.add(((RefreshToken) credential).getTarget());
        } else if (credential instanceof IdToken) {
            keyComponents.add(((IdToken) credential).getRealm());
        }

        return collapseKeyComponents(keyComponents);
    }

    @Override
    public String generateCacheValue(Credential credential) {
        return generateCacheValueInternal(credential);
    }

    @Override
    public <T extends AccountCredentialBase> T fromCacheValue(String string, Class<? extends AccountCredentialBase> t) {
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

            // return the fully-formed object
            return resultObject;
        } catch (JsonSyntaxException e) {
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
        final Set<String> serializedNames = new HashSet<>();
        final List<Field> fieldsToInspect = getFieldsUpTo(clazz, AccountCredentialBase.class);
        final List<Field> annotatedFields = getSerializedNameAnnotatedFields(fieldsToInspect);

        for (final Field annotatedField : annotatedFields) {
            final SerializedName serializedName = annotatedField.getAnnotation(SerializedName.class);
            serializedNames.add(serializedName.value());
        }

        return serializedNames;
    }

    /**
     * For the supplied List of Fields, return those which are annotated with @SerializedName.
     *
     * @param fieldsToInspect The Fields to inspect.
     * @return Those Fields which are annotated with @SerializedName.
     */
    private static List<Field> getSerializedNameAnnotatedFields(final List<Field> fieldsToInspect) {
        final List<Field> annotatedFields = new ArrayList<>();

        for (final Field field : fieldsToInspect) {
            SerializedName serializedName = field.getAnnotation(SerializedName.class);
            if (null != serializedName) {
                annotatedFields.add(field);
            }
        }

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
        List<Field> currentClassFields = new ArrayList<>(Arrays.asList(startClass.getDeclaredFields()));
        Class<?> parentClass = startClass.getSuperclass();

        if (parentClass != null && (upperBound == null || !(parentClass.equals(upperBound)))) {
            List<Field> parentClassFields = getFieldsUpTo(parentClass, upperBound);
            currentClassFields.addAll(parentClassFields);
        }

        return currentClassFields;
    }
}
