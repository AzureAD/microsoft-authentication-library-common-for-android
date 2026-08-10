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

package com.microsoft.identity.labapi.utilities.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import lombok.Getter;

/**
 * Represents a lab account entry parsed from a JSON string.
 */
@Getter
public class LabJsonStringAccountEntry implements Serializable {

    @SerializedName("Upn")
    private String upn;

    @SerializedName("HomeObjectId")
    private String homeObjectId;

    @SerializedName("HomeTenantId")
    private String homeTenantId;

    @SerializedName("GuestTenantId")
    private String guestTenantId;

    @SerializedName("AssociatedClientId")
    private String associatedClientId;

    @SerializedName("KeyVaultEntry")
    private String keyVaultEntry;

    @SerializedName("AzureEnvironment")
    private String azureEnvironment;

    @SerializedName("CloudUrl")
    private String cloudUrl;


    /**
     * Parses a JSON string into a Map<String, LabJsonStringAccountEntry> using Gson.
     * @param json the JSON string to parse
     * @return a map of key to LabJsonStringAccountEntry
     */
    public static Map<String, LabJsonStringAccountEntry> parseJsonToMap(String json) {
        final Gson gson = new GsonBuilder()
                .registerTypeAdapter(LabJsonStringAccountEntry.class, new CaseInsensitiveDeserializer())
                .create();
        Type type = new TypeToken<Map<String, LabJsonStringAccountEntry>>(){}.getType();
        return gson.fromJson(json, type);
    }

    /**
     * Gson deserializer that matches JSON property names to {@link SerializedName} values
     * in a case-insensitive manner, so {@code upn}, {@code Upn} and {@code UPN} are all
     * treated as the same field.
     */
    private static class CaseInsensitiveDeserializer implements JsonDeserializer<LabJsonStringAccountEntry> {
        @Override
        public LabJsonStringAccountEntry deserialize(JsonElement json, Type typeOfT,
                                                     JsonDeserializationContext context) throws JsonParseException {
            final JsonObject src = json.getAsJsonObject();

            // Build a lowercase-keyed view of the incoming JSON object.
            final Map<String, JsonElement> lowerCased = new HashMap<>();
            for (Map.Entry<String, JsonElement> e : src.entrySet()) {
                lowerCased.put(e.getKey().toLowerCase(Locale.ROOT), e.getValue());
            }

            final LabJsonStringAccountEntry result = new LabJsonStringAccountEntry();
            for (Field field : LabJsonStringAccountEntry.class.getDeclaredFields()) {
                final SerializedName annotation = field.getAnnotation(SerializedName.class);
                if (annotation == null) {
                    continue;
                }
                final JsonElement value = lowerCased.get(annotation.value().toLowerCase(Locale.ROOT));
                if (value == null || value.isJsonNull()) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    field.set(result, context.deserialize(value, field.getGenericType()));
                } catch (IllegalAccessException ex) {
                    throw new JsonParseException("Failed to set field " + field.getName(), ex);
                }
            }
            return result;
        }
    }
}
