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
        keyComponents.add(account.getUniqueId());
        keyComponents.add(account.getEnvironment());
        keyComponents.add(account.getRealm());

        return collapseKeyComponents(keyComponents);
    }

    @Override
    public String generateCacheValue(Account account) {
        // TODO serialize extra fields
        return mGson.toJson(account);
    }

    @Override
    public String generateCacheKey(Credential credential) {
        final List<String> keyComponents = new ArrayList<>();
        keyComponents.add(credential.getUniqueId());
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
        JsonElement outboundElement = mGson.toJsonTree(credential);
        JsonObject outboundObject = outboundElement.getAsJsonObject();

        for (final String key : credential.getAdditionalFields().keySet()) {
            outboundObject.add(key, credential.getAdditionalFields().get(key));
        }

        return mGson.toJson(outboundObject);
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

    private static Set<String> getExpectedJsonFields(final Class<?> clazz) {
        final Set<String> serializedNames = new HashSet<>();
        final List<Field> fieldsToInspect = getFieldsUpTo(clazz, AccountCredentialBase.class);
        final List<Field> annotatedFields = getSerializedNameAnnotatedFields(fieldsToInspect);

        for (final Field annotatedField : annotatedFields) {
            final SerializedName serializedName = annotatedField.getAnnotation(SerializedName.class);
            serializedNames.add(serializedName.value());
        }

        return serializedNames;
    }

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

    private static List<Field> getFieldsUpTo(
            final Class<?> startClass,
            @Nullable Class<?> exclusiveParent) {
        List<Field> currentClassFields = new ArrayList<>(Arrays.asList(startClass.getDeclaredFields()));
        Class<?> parentClass = startClass.getSuperclass();

        if (parentClass != null && (exclusiveParent == null || !(parentClass.equals(exclusiveParent)))) {
            List<Field> parentClassFields = getFieldsUpTo(parentClass, exclusiveParent);
            currentClassFields.addAll(parentClassFields);
        }

        return currentClassFields;
    }
}
