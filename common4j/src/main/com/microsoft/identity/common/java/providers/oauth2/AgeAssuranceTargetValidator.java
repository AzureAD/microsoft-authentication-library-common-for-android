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

package com.microsoft.identity.common.java.providers.oauth2;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.exception.ServiceException;
import com.microsoft.identity.common.java.result.AcquireTokenResult;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import edu.umd.cs.findbugs.annotations.Nullable;

import static com.microsoft.identity.common.java.AuthenticationConstants.Broker.LOOKUP_MODE_ID_TOKEN_VALUE;

/**
 * Binds an ID-token age-assurance target receipt to explicit targets in that operation's claims.
 * This does not interpret age-assurance status or outcome signs, authorize API access, or inspect
 * access tokens. Target names are opaque; "all" is a literal identity, not a wildcard.
 * Matching ignores identity case and order, but requires a plus or minus sign on every returned
 * identity. With no explicit targets, only an absent/empty receipt or a single signed "all"
 * identity is accepted. Malformed, duplicate, or extra receipt entries are rejected regardless of
 * status, including unknown status (TU0). A matching negative entry is a valid receipt, not
 * evidence of adult authorization.
 */
public final class AgeAssuranceTargetValidator {
    private static final String TARGET_PREFIX = "target-";
    private static final String RECEIPT_CLAIM = "xms_agvt";

    private AgeAssuranceTargetValidator() {
    }

    /**
     * Validates a successful controller result before success delivery or client-side persistence.
     * Unsuccessful results retain their original error.
     */
    public static void validateResult(@Nullable final String claimsRequestJson,
                                      @Nullable final AcquireTokenResult result) throws ClientException {
        validateResult(claimsRequestJson, result, false);
    }

    /**
     * Validates a result using lookup mode from the original operation, never from the response.
     */
    public static void validateResult(@Nullable final String claimsRequestJson,
                                      @Nullable final AcquireTokenResult result,
                                      final boolean isLookupMode) throws ClientException {
        if (result != null && result.getSucceeded()) {
            validate(claimsRequestJson, result.getLocalAuthenticationResult() == null
                    ? null : result.getLocalAuthenticationResult().getIdToken(), isLookupMode);
        }
    }

    /**
     * Validates the ID token against an immutable, operation-local claims JSON string.
     * Without explicit ID-token targets, a genuinely absent ID token is allowed. A present token
     * must have no receipt, an empty string receipt, or exactly one signed "all" identity.
     *
     * @throws ClientException with age_assurance_target_mismatch when the receipt is malformed,
     *                        duplicated, missing for explicit targets, or does not match the set
     */
    public static void validate(@Nullable final String claimsRequestJson,
                                @Nullable final String idToken) throws ClientException {
        validate(claimsRequestJson, idToken, false);
    }

    /**
     * Validates using trusted lookup context from the original operation. Only untargeted lookup
     * operations may return the protocol's exact "none" sentinel; real ID tokens are still checked.
     */
    public static void validate(@Nullable final String claimsRequestJson,
                                @Nullable final String idToken,
                                final boolean isLookupMode) throws ClientException {
        final Set<String> requested = requestedTargets(claimsRequestJson);
        if (requested.isEmpty() && (idToken == null
                || (isLookupMode && LOOKUP_MODE_ID_TOKEN_VALUE.equals(idToken)))) {
            return;
        }

        final Map<String, ?> tokenClaims;
        try {
            tokenClaims = new IDToken(idToken).getTokenClaims();
        } catch (final ServiceException | IllegalArgumentException e) {
            // Do not attach parser exceptions, which may contain token material.
            throw mismatch();
        }
        if (requested.isEmpty() && !tokenClaims.containsKey(RECEIPT_CLAIM)) {
            return;
        }
        final Object receipt = tokenClaims.get(RECEIPT_CLAIM);
        if (!(receipt instanceof String)) {
            throw mismatch();
        }
        if (requested.isEmpty() && ((String) receipt).isEmpty()) {
            return;
        }

        final Set<String> returned = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (final String entry : ((String) receipt).split(",", -1)) {
            if (entry.length() < 2 || (entry.charAt(0) != '+' && entry.charAt(0) != '-')) {
                throw mismatch();
            }
            final String identity = validateIdentity(entry.substring(1));
            if (!returned.add(identity)) {
                throw mismatch();
            }
        }
        if (requested.isEmpty()
                ? returned.size() != 1 || !returned.contains("all")
                : !requested.equals(returned)) {
            throw mismatch();
        }
    }

    private static Set<String> requestedTargets(@Nullable final String json) throws ClientException {
        final Set<String> result = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
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
                    result.add(validateIdentity(value.substring(TARGET_PREFIX.length())));
                }
            }
        }
        return result;
    }

    @Nullable
    private static JsonElement member(@Nullable final JsonElement element, final String name) {
        return element instanceof JsonObject ? element.getAsJsonObject().get(name) : null;
    }

    private static String validateIdentity(final String identity) throws ClientException {
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
        return identity;
    }

    private static ClientException mismatch() {
        return new ClientException(ClientException.AGE_ASSURANCE_TARGET_MISMATCH,
                "The ID token age-assurance targets do not match the requested targets.");
    }
}
