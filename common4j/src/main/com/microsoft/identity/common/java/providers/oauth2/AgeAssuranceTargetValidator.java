// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.identity.common.java.providers.oauth2;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.exception.ServiceException;
import com.microsoft.identity.common.java.result.AcquireTokenResult;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * Binds an ID-token age-assurance target receipt to explicit targets in that operation's claims.
 * This does not interpret age-assurance status or outcome signs, authorize API access, or inspect
 * access tokens. Target names are opaque; "all" is a literal identity, not a wildcard.
 * Matching ignores identity case and order, but requires a plus or minus sign on every returned
 * identity. Missing, malformed, duplicate, or extra receipt entries are rejected regardless of
 * status, including unknown status (TU0). A matching negative entry is a valid receipt, not
 * evidence of adult authorization.
 */
public final class AgeAssuranceTargetValidator {
    private static final String TARGET_PREFIX = "target-";
    private static final String RECEIPT_CLAIM = "xms_agvt";

    private AgeAssuranceTargetValidator() {
    }

    /**
     * Whether this operation explicitly requests ID-token targets. This lets a controller retain
     * its existing untargeted result handling while validating targeted results before caching.
     */
    public static boolean requiresValidation(@Nullable final String claimsRequestJson)
            throws ClientException {
        return !requestedTargets(claimsRequestJson).isEmpty();
    }

    /**
     * Validates a successful controller result before success delivery or client-side persistence.
     * Unsuccessful results retain their original error.
     */
    public static void validateResult(@Nullable final String claimsRequestJson,
                                      @Nullable final AcquireTokenResult result) throws ClientException {
        if (result != null && result.getSucceeded()) {
            validate(claimsRequestJson, result.getLocalAuthenticationResult() == null
                    ? null : result.getLocalAuthenticationResult().getIdToken());
        }
    }

    /**
     * Validates the ID token against an immutable, operation-local claims JSON string.
     * Without explicit ID-token targets, the ID token is not parsed and behavior is unchanged.
     *
     * @throws ClientException with age_assurance_target_mismatch when the signed receipt is
     *                        missing, malformed, duplicated or does not match the requested set
     */
    public static void validate(@Nullable final String claimsRequestJson,
                                @Nullable final String idToken) throws ClientException {
        final Set<String> requested = requestedTargets(claimsRequestJson);
        if (requested.isEmpty()) {
            return;
        }

        final Object receipt;
        try {
            receipt = new IDToken(idToken).getTokenClaims().get(RECEIPT_CLAIM);
        } catch (final ServiceException | IllegalArgumentException e) {
            // Do not attach parser exceptions, which may contain token material.
            throw mismatch();
        }
        if (!(receipt instanceof String)) {
            throw mismatch();
        }

        final Set<String> returned = new HashSet<>();
        for (final String entry : ((String) receipt).split(",", -1)) {
            if (entry.length() < 2 || (entry.charAt(0) != '+' && entry.charAt(0) != '-')) {
                throw mismatch();
            }
            final String identity = normalizeIdentity(entry.substring(1));
            if (!returned.add(identity)) {
                throw mismatch();
            }
        }
        if (!requested.equals(returned)) {
            throw mismatch();
        }
    }

    private static Set<String> requestedTargets(@Nullable final String json) throws ClientException {
        final Set<String> result = new HashSet<>();
        if (json == null || json.isEmpty()) {
            return result;
        }
        final JsonElement root;
        try {
            root = JsonParser.parseString(json);
        } catch (final JsonParseException e) {
            // Invalid claims requests remain the responsibility of the existing request pipeline.
            return result;
        }
        final JsonElement idToken = member(root, "id_token");
        final JsonElement status = member(idToken, "xms_agvs");
        final JsonElement properties = member(status, "additionalProperties");
        if (properties == null || !properties.isJsonArray()) {
            return result;
        }
        for (final JsonElement property : properties.getAsJsonArray()) {
            if (property.isJsonPrimitive() && property.getAsJsonPrimitive().isString()) {
                final String value = property.getAsString();
                if (value.regionMatches(true, 0, TARGET_PREFIX, 0, TARGET_PREFIX.length())) {
                    result.add(normalizeIdentity(value.substring(TARGET_PREFIX.length())));
                }
            }
        }
        return result;
    }

    @Nullable
    private static JsonElement member(@Nullable final JsonElement element, final String name) {
        return element instanceof JsonObject ? element.getAsJsonObject().get(name) : null;
    }

    private static String normalizeIdentity(final String identity) throws ClientException {
        if (identity.isEmpty() || identity.charAt(0) == '+' || identity.charAt(0) == '-') {
            throw mismatch();
        }
        for (int i = 0; i < identity.length(); i++) {
            final char character = identity.charAt(i);
            if (character == ',' || Character.isWhitespace(character)
                    || Character.isSpaceChar(character) || character == '\uFEFF') {
                throw mismatch();
            }
        }
        return identity.toLowerCase(Locale.ROOT);
    }

    private static ClientException mismatch() {
        return new ClientException(ClientException.AGE_ASSURANCE_TARGET_MISMATCH,
                "The ID token age-assurance targets do not match the requested targets.");
    }
}
