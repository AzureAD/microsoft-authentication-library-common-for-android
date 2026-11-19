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
package com.microsoft.identity.common.java.throttling;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.microsoft.identity.common.java.authorities.Authority;
import com.microsoft.identity.common.java.authscheme.AbstractAuthenticationScheme;
import com.microsoft.identity.common.java.broker.IBrokerAccount;
import com.microsoft.identity.common.java.commands.parameters.IBrokerTokenCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.RopcTokenCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.TokenCommandParameters;
import com.microsoft.identity.common.java.dto.IAccountRecord;
import com.microsoft.identity.common.java.request.BrokerRequestType;
import com.microsoft.identity.common.java.util.StringUtil;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.annotation.Nullable;

import lombok.NonNull;

/**
 * Cryptographic SHA-256 cache key and hashed user-account eviction tag container for Broker
 * server-directed token throttling. Never retains raw UPNs, account IDs, claims, or scopes.
 */
public final class TokenThrottleKey {

    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();
    private static final int KEY_PREFIX_LENGTH = 8;
    private static final String ACCOUNT_TAG_PREFIX = "acct:";
    private static final String DCF_SESSION_PREFIX = "dcf:";
    private static final String HASHED_FALLBACK_PREFIX = "fallback_sha256:";
    private static final Gson CANONICAL_JSON_GSON = new GsonBuilder()
            .serializeNulls()
            .disableHtmlEscaping()
            .create();

    @NonNull
    private final ThrottleScopeType mScopeType;

    @NonNull
    private final String mSha256Hex;

    @NonNull
    private final Set<String> mAccountTagHashes;

    private TokenThrottleKey(
            @NonNull final ThrottleScopeType scopeType,
            @NonNull final String sha256Hex,
            @NonNull final Set<String> accountTagHashes) {
        mScopeType = scopeType;
        mSha256Hex = sha256Hex;
        mAccountTagHashes = accountTagHashes.isEmpty()
                ? Collections.<String>emptySet()
                : Collections.unmodifiableSet(new LinkedHashSet<>(accountTagHashes));
    }

    /**
     * Returns the throttling scope category.
     *
     * @return non-null {@link ThrottleScopeType}.
     */
    @NonNull
    public ThrottleScopeType getScopeType() {
        return mScopeType;
    }

    /**
     * Returns the 64-character lowercase hexadecimal SHA-256 digest of the canonical request tuple.
     *
     * @return 64-character lowercase hex digest.
     */
    @NonNull
    public String getSha256Hex() {
        return mSha256Hex;
    }

    /**
     * Returns the first 8 hex characters of {@link #getSha256Hex()} for non-PII telemetry.
     *
     * @return 8-character lowercase hex prefix.
     */
    @NonNull
    public String getKeyPrefix() {
        return mSha256Hex.substring(0, KEY_PREFIX_LENGTH);
    }

    /**
     * Returns the unmodifiable set of hashed user account tags ({@code SHA-256("acct:" + canonicalId)})
     * used for device-wide account removal eviction. Always empty for {@link ThrottleScopeType#DEVICE_TOKEN}.
     *
     * @return unmodifiable set of 64-character lowercase hex account tag digests.
     */
    @NonNull
    public Set<String> getAccountTagHashes() {
        return Collections.unmodifiableSet(mAccountTagHashes);
    }

    /**
     * Builds an {@link ThrottleScopeType#AT_OR_RT} throttle key from {@link TokenCommandParameters}.
     *
     * @param parameters                 token command parameters.
     * @param authorityOverride          optional authority URL override.
     * @param fallbackAccountOrSessionId optional session fallback (e.g., {@code "dcf:" + deviceCode});
     *                                   hashed before account lowercasing and excluded from
     *                                   {@link #getAccountTagHashes()} unless also in {@code extraRawAccountIds}.
     * @param extraRawAccountIds         optional extra user account identifiers for eviction tagging.
     * @return canonical {@link TokenThrottleKey}.
     */
    @NonNull
    public static TokenThrottleKey forAtOrRt(
            @NonNull final TokenCommandParameters parameters,
            @Nullable final String authorityOverride,
            @Nullable final String fallbackAccountOrSessionId,
            @Nullable final Collection<String> extraRawAccountIds) {
        final String authorityUrl = resolveAuthorityUrl(parameters, authorityOverride);
        final String effectiveClientId = resolveEffectiveClientId(parameters);
        final AbstractAuthenticationScheme scheme = parameters.getAuthenticationScheme();
        final String authSchemeName = scheme != null && scheme.getName() != null ? scheme.getName() : "";
        final String mamEnrollmentId = parameters.getMamEnrollmentId();
        final String brokerRequestTypeName = resolveBrokerRequestTypeName(parameters);

        final IBrokerTokenCommandParameters brokerParams =
                parameters instanceof IBrokerTokenCommandParameters
                        ? (IBrokerTokenCommandParameters) parameters
                        : null;
        final IAccountRecord accountRecord = parameters.getAccount();
        final IBrokerAccount brokerAccount =
                brokerParams != null ? brokerParams.getBrokerAccount() : null;
        final String ropcUsername =
                parameters instanceof RopcTokenCommandParameters
                        ? ((RopcTokenCommandParameters) parameters).getUsername()
                        : null;

        final String resolvedUserAccountId = firstNonEmpty(
                brokerParams != null ? brokerParams.getHomeAccountId() : null,
                accountRecord != null ? accountRecord.getHomeAccountId() : null,
                brokerParams != null ? brokerParams.getLocalAccountId() : null,
                accountRecord != null ? accountRecord.getLocalAccountId() : null,
                brokerAccount != null ? brokerAccount.getUsername() : null,
                accountRecord != null ? accountRecord.getUsername() : null,
                ropcUsername,
                parameters.getLoginHint()
        );
        final String primaryAccountId;
        if (!resolvedUserAccountId.isEmpty()) {
            primaryAccountId = resolvedUserAccountId;
        } else if (fallbackAccountOrSessionId != null && !StringUtil.isNullOrEmpty(fallbackAccountOrSessionId)) {
            primaryAccountId = hashFallbackSessionId(fallbackAccountOrSessionId);
        } else {
            primaryAccountId = "";
        }

        final List<String> userAccountIds = new ArrayList<>();
        if (brokerParams != null) {
            addIfNonEmpty(userAccountIds, brokerParams.getHomeAccountId());
            addIfNonEmpty(userAccountIds, brokerParams.getLocalAccountId());
            if (brokerAccount != null) {
                addIfNonEmpty(userAccountIds, brokerAccount.getUsername());
            }
        }
        if (accountRecord != null) {
            addIfNonEmpty(userAccountIds, accountRecord.getHomeAccountId());
            addIfNonEmpty(userAccountIds, accountRecord.getLocalAccountId());
            addIfNonEmpty(userAccountIds, accountRecord.getUsername());
        }
        addIfNonEmpty(userAccountIds, ropcUsername);
        addIfNonEmpty(userAccountIds, parameters.getLoginHint());
        if (extraRawAccountIds != null) {
            for (final String extraId : extraRawAccountIds) {
                addIfNonEmpty(userAccountIds, extraId);
            }
        }

        return forAtOrRt(
                authorityUrl,
                effectiveClientId,
                parameters.getScopes(),
                parameters.getClaimsRequestJson(),
                primaryAccountId,
                authSchemeName,
                mamEnrollmentId,
                brokerRequestTypeName,
                userAccountIds
        );
    }

    /**
     * Builds an {@link ThrottleScopeType#AT_OR_RT} throttle key from explicit request components.
     *
     * @param authorityUrl          authority URL.
     * @param effectiveClientId     client ID (or {@code brkClientId:childClientId} for NAA).
     * @param scopes                requested scopes.
     * @param claimsRequestJson     optional claims JSON.
     * @param primaryAccountId      primary account or session identifier.
     * @param authSchemeName        authentication scheme name.
     * @param mamEnrollmentId       optional MAM enrollment ID.
     * @param brokerRequestTypeName optional broker request type name (defaults to {@code REGULAR}).
     * @param rawAccountIds         optional raw user account identifiers for eviction tagging.
     * @return canonical {@link TokenThrottleKey}.
     */
    @NonNull
    public static TokenThrottleKey forAtOrRt(
            @NonNull final String authorityUrl,
            @NonNull final String effectiveClientId,
            @Nullable final Collection<String> scopes,
            @Nullable final String claimsRequestJson,
            @NonNull final String primaryAccountId,
            @Nullable final String authSchemeName,
            @Nullable final String mamEnrollmentId,
            @Nullable final String brokerRequestTypeName,
            @Nullable final Collection<String> rawAccountIds) {
        final String canonicalAuthorityUrl = canonicalizeUrlOrResource(authorityUrl);
        final String canonicalClientId = canonicalizeIdentifier(effectiveClientId);
        final String canonicalScopes = canonicalizeScopesCollection(scopes);
        final String claimsHash = sha256Hex(canonicalizeClaimsJson(claimsRequestJson));
        final String canonicalPrimaryAccount = canonicalizePrimaryAccountOrSessionId(primaryAccountId);
        final String normalizedScheme = trimOrEmpty(authSchemeName);
        final String normalizedMamId = trimOrEmpty(mamEnrollmentId);
        final String normalizedRequestType = !StringUtil.isNullOrEmpty(brokerRequestTypeName)
                ? brokerRequestTypeName.trim()
                : BrokerRequestType.REGULAR.name();

        final String canonicalTuple = "v1|AT_OR_RT|"
                + canonicalAuthorityUrl + "|"
                + canonicalClientId + "|"
                + canonicalScopes + "|"
                + claimsHash + "|"
                + canonicalPrimaryAccount + "|"
                + normalizedScheme + "|"
                + normalizedMamId + "|"
                + normalizedRequestType;

        return new TokenThrottleKey(
                ThrottleScopeType.AT_OR_RT,
                sha256Hex(canonicalTuple),
                buildAccountTagSet(primaryAccountId, rawAccountIds)
        );
    }

    /**
     * Builds a {@link ThrottleScopeType#BROKER_PRT} throttle key.
     *
     * @param prtHomeAuthority PRT home authority URL.
     * @param primaryAccountId primary account identifier.
     * @param prtGrantKind     PRT exchange/grant kind.
     * @param rawAccountIds    optional extra account identifiers.
     * @return canonical {@link TokenThrottleKey}.
     */
    @NonNull
    public static TokenThrottleKey forBrokerPrt(
            @NonNull final String prtHomeAuthority,
            @NonNull final String primaryAccountId,
            @NonNull final String prtGrantKind,
            @Nullable final Collection<String> rawAccountIds) {
        final String canonicalTuple = "v1|BROKER_PRT|"
                + canonicalizeUrlOrResource(prtHomeAuthority) + "|"
                + canonicalizeAccountId(primaryAccountId) + "|"
                + trimOrEmpty(prtGrantKind).toLowerCase(Locale.ROOT);

        return new TokenThrottleKey(
                ThrottleScopeType.BROKER_PRT,
                sha256Hex(canonicalTuple),
                buildAccountTagSet(primaryAccountId, rawAccountIds)
        );
    }

    /**
     * Builds a {@link ThrottleScopeType#DEVICE_TOKEN} throttle key with empty user
     * {@link #getAccountTagHashes()} so shared-device user sign-out preserves WPJ device state.
     *
     * @param homeAuthority      WPJ home authority URL.
     * @param clientId           calling client ID.
     * @param resource           target resource.
     * @param scope              optional scopes string.
     * @param deviceIdOrTenantId device registration ID (or tenant ID fallback).
     * @return canonical {@link TokenThrottleKey}.
     */
    @NonNull
    public static TokenThrottleKey forDeviceToken(
            @NonNull final String homeAuthority,
            @NonNull final String clientId,
            @NonNull final String resource,
            @Nullable final String scope,
            @NonNull final String deviceIdOrTenantId) {
        final String canonicalTuple = "v1|DEVICE_TOKEN|"
                + canonicalizeUrlOrResource(homeAuthority) + "|"
                + canonicalizeIdentifier(clientId) + "|"
                + canonicalizeUrlOrResource(resource) + "|"
                + canonicalizeScopesString(scope) + "|"
                + canonicalizeAccountId(deviceIdOrTenantId);

        return new TokenThrottleKey(
                ThrottleScopeType.DEVICE_TOKEN,
                sha256Hex(canonicalTuple),
                Collections.<String>emptySet()
        );
    }

    /**
     * Builds a {@link ThrottleScopeType#FOCI_PROBE} throttle key.
     *
     * @param authorityUrl     authority URL.
     * @param clientId         probed client ID.
     * @param scopes           optional scopes string.
     * @param primaryAccountId primary account identifier.
     * @param rawAccountIds    optional extra account identifiers.
     * @return canonical {@link TokenThrottleKey}.
     */
    @NonNull
    public static TokenThrottleKey forFociProbe(
            @NonNull final String authorityUrl,
            @NonNull final String clientId,
            @Nullable final String scopes,
            @NonNull final String primaryAccountId,
            @Nullable final Collection<String> rawAccountIds) {
        final String canonicalTuple = "v1|FOCI_PROBE|"
                + canonicalizeUrlOrResource(authorityUrl) + "|"
                + canonicalizeIdentifier(clientId) + "|"
                + canonicalizeScopesString(scopes) + "|"
                + canonicalizeAccountId(primaryAccountId);

        return new TokenThrottleKey(
                ThrottleScopeType.FOCI_PROBE,
                sha256Hex(canonicalTuple),
                buildAccountTagSet(primaryAccountId, rawAccountIds)
        );
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof TokenThrottleKey)) {
            return false;
        }
        final TokenThrottleKey other = (TokenThrottleKey) obj;
        return mScopeType == other.mScopeType && mSha256Hex.equals(other.mSha256Hex);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mScopeType, mSha256Hex);
    }

    @Override
    @NonNull
    public String toString() {
        return "TokenThrottleKey{scopeType=" + mScopeType + ", keyPrefix='" + getKeyPrefix() + "'}";
    }

    @NonNull
    static String sha256Hex(@NonNull final String input) {
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            final byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            final char[] hexChars = new char[hashBytes.length * 2];
            for (int i = 0; i < hashBytes.length; i++) {
                final int value = hashBytes[i] & 0xFF;
                hexChars[i * 2] = HEX_DIGITS[value >>> 4];
                hexChars[i * 2 + 1] = HEX_DIGITS[value & 0x0F];
            }
            return new String(hexChars);
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 MessageDigest unavailable", e);
        }
    }

    @Nullable
    static String hashAccountTag(@Nullable final String rawAccountId) {
        if (StringUtil.isNullOrEmpty(rawAccountId)) {
            return null;
        }
        return sha256Hex(ACCOUNT_TAG_PREFIX + canonicalizeAccountId(rawAccountId));
    }

    @NonNull
    static Set<String> hashAccountTags(@Nullable final String... rawAccountIds) {
        if (rawAccountIds == null || rawAccountIds.length == 0) {
            return Collections.emptySet();
        }
        final Set<String> tags = new LinkedHashSet<>();
        for (final String rawId : rawAccountIds) {
            final String hashed = hashAccountTag(rawId);
            if (hashed != null) {
                tags.add(hashed);
            }
        }
        return tags;
    }

    @NonNull
    private static Set<String> buildAccountTagSet(
            @Nullable final String primaryAccountId,
            @Nullable final Collection<String> rawAccountIds) {
        final Set<String> tags = new LinkedHashSet<>();
        if (!isFallbackSessionMarker(primaryAccountId)) {
            final String primaryTag = hashAccountTag(primaryAccountId);
            if (primaryTag != null) {
                tags.add(primaryTag);
            }
        }
        if (rawAccountIds != null) {
            for (final String rawId : rawAccountIds) {
                final String tag = hashAccountTag(rawId);
                if (tag != null) {
                    tags.add(tag);
                }
            }
        }
        return tags;
    }

    private static boolean isFallbackSessionMarker(@Nullable final String value) {
        if (value == null || StringUtil.isNullOrEmpty(value)) {
            return false;
        }
        final String trimmed = value.trim();
        return trimmed.startsWith(HASHED_FALLBACK_PREFIX)
                || (trimmed.length() >= DCF_SESSION_PREFIX.length()
                && trimmed.regionMatches(true, 0, DCF_SESSION_PREFIX, 0, DCF_SESSION_PREFIX.length()));
    }

    @NonNull
    private static String hashFallbackSessionId(@NonNull final String rawFallback) {
        final String trimmed = rawFallback.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        if (trimmed.startsWith(HASHED_FALLBACK_PREFIX)) {
            return trimmed;
        }
        if (trimmed.length() >= DCF_SESSION_PREFIX.length()
                && trimmed.regionMatches(true, 0, DCF_SESSION_PREFIX, 0, DCF_SESSION_PREFIX.length())) {
            return HASHED_FALLBACK_PREFIX
                    + sha256Hex(DCF_SESSION_PREFIX + trimmed.substring(DCF_SESSION_PREFIX.length()));
        }
        return HASHED_FALLBACK_PREFIX + sha256Hex(trimmed);
    }

    @NonNull
    private static String canonicalizePrimaryAccountOrSessionId(@Nullable final String value) {
        if (value == null || StringUtil.isNullOrEmpty(value)) {
            return "";
        }
        final String trimmed = value.trim();
        if (isFallbackSessionMarker(trimmed)) {
            return hashFallbackSessionId(trimmed);
        }
        return canonicalizeAccountId(trimmed);
    }

    @NonNull
    private static String resolveAuthorityUrl(
            @NonNull final TokenCommandParameters parameters,
            @Nullable final String authorityOverride) {
        if (!StringUtil.isNullOrEmpty(authorityOverride)) {
            return authorityOverride;
        }
        final Authority authority = parameters.getAuthority();
        if (authority == null) {
            return "";
        }
        try {
            final URL url = authority.getAuthorityURL();
            return url != null ? url.toString() : "";
        } catch (final RuntimeException ignored) {
            return "";
        }
    }

    @NonNull
    private static String resolveEffectiveClientId(@NonNull final TokenCommandParameters parameters) {
        final String clientId = trimOrEmpty(parameters.getClientId());
        if (parameters.hasNestedAppParameters()) {
            return clientId + ":" + trimOrEmpty(parameters.getChildClientId());
        }
        return clientId;
    }

    @NonNull
    private static String resolveBrokerRequestTypeName(@NonNull final TokenCommandParameters parameters) {
        if (parameters instanceof IBrokerTokenCommandParameters) {
            final BrokerRequestType requestType = ((IBrokerTokenCommandParameters) parameters).getRequestType();
            if (requestType != null) {
                return requestType.name();
            }
        }
        return BrokerRequestType.REGULAR.name();
    }

    @NonNull
    private static String firstNonEmpty(@Nullable final String... candidates) {
        if (candidates == null) {
            return "";
        }
        for (final String candidate : candidates) {
            if (!StringUtil.isNullOrEmpty(candidate)) {
                return candidate.trim();
            }
        }
        return "";
    }

    private static void addIfNonEmpty(@NonNull final List<String> target, @Nullable final String value) {
        if (!StringUtil.isNullOrEmpty(value)) {
            target.add(value.trim());
        }
    }

    @NonNull
    private static String canonicalizeUrlOrResource(@Nullable final String value) {
        if (StringUtil.isNullOrEmpty(value)) {
            return "";
        }
        final String lower = value.trim().toLowerCase(Locale.ROOT);
        int end = lower.length();
        while (end > 0 && lower.charAt(end - 1) == '/') {
            end--;
        }
        return lower.substring(0, end);
    }

    @NonNull
    private static String canonicalizeIdentifier(@Nullable final String value) {
        if (StringUtil.isNullOrEmpty(value)) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    @NonNull
    private static String canonicalizeAccountId(@Nullable final String value) {
        if (StringUtil.isNullOrEmpty(value)) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    @NonNull
    private static String trimOrEmpty(@Nullable final String value) {
        return StringUtil.isNullOrEmpty(value) ? "" : value.trim();
    }

    @NonNull
    private static String canonicalizeScopesCollection(@Nullable final Collection<String> scopes) {
        if (scopes == null || scopes.isEmpty()) {
            return "";
        }
        final TreeSet<String> sortedScopes = new TreeSet<>();
        for (final String rawScope : scopes) {
            collectWhitespaceDelimitedTokens(sortedScopes, rawScope);
        }
        return joinSortedTokens(sortedScopes);
    }

    @NonNull
    private static String canonicalizeScopesString(@Nullable final String scopes) {
        if (StringUtil.isNullOrEmpty(scopes)) {
            return "";
        }
        final TreeSet<String> sortedScopes = new TreeSet<>();
        collectWhitespaceDelimitedTokens(sortedScopes, scopes);
        return joinSortedTokens(sortedScopes);
    }

    private static void collectWhitespaceDelimitedTokens(
            @NonNull final TreeSet<String> target,
            @Nullable final String raw) {
        if (raw == null) {
            return;
        }
        final int len = raw.length();
        int tokenStart = -1;
        for (int i = 0; i < len; i++) {
            final char ch = raw.charAt(i);
            if (Character.isWhitespace(ch)) {
                if (tokenStart >= 0) {
                    target.add(raw.substring(tokenStart, i).toLowerCase(Locale.ROOT));
                    tokenStart = -1;
                }
            } else if (tokenStart < 0) {
                tokenStart = i;
            }
        }
        if (tokenStart >= 0) {
            target.add(raw.substring(tokenStart, len).toLowerCase(Locale.ROOT));
        }
    }

    @NonNull
    private static String joinSortedTokens(@NonNull final TreeSet<String> sortedTokens) {
        if (sortedTokens.isEmpty()) {
            return "";
        }
        final StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (final String token : sortedTokens) {
            if (!first) {
                sb.append(' ');
            }
            sb.append(token);
            first = false;
        }
        return sb.toString();
    }

    @NonNull
    private static String canonicalizeClaimsJson(@Nullable final String claimsRequestJson) {
        if (StringUtil.isNullOrEmpty(claimsRequestJson)) {
            return "";
        }
        final String trimmed = claimsRequestJson.trim();
        try {
            final JsonElement parsed = CANONICAL_JSON_GSON.fromJson(trimmed, JsonElement.class);
            if (parsed == null || parsed.isJsonNull()) {
                return "";
            }
            return CANONICAL_JSON_GSON.toJson(sortJsonElementKeys(parsed));
        } catch (final JsonSyntaxException ignored) {
            return trimmed;
        }
    }

    @NonNull
    private static JsonElement sortJsonElementKeys(@NonNull final JsonElement element) {
        if (element.isJsonObject()) {
            final JsonObject inputObj = element.getAsJsonObject();
            final TreeMap<String, JsonElement> sortedEntries = new TreeMap<>();
            for (final Map.Entry<String, JsonElement> entry : inputObj.entrySet()) {
                sortedEntries.put(entry.getKey(), sortJsonElementKeys(entry.getValue()));
            }
            final JsonObject sortedObj = new JsonObject();
            for (final Map.Entry<String, JsonElement> entry : sortedEntries.entrySet()) {
                sortedObj.add(entry.getKey(), entry.getValue());
            }
            return sortedObj;
        }
        if (element.isJsonArray()) {
            final JsonArray inputArr = element.getAsJsonArray();
            final JsonArray sortedArr = new JsonArray();
            for (int i = 0; i < inputArr.size(); i++) {
                sortedArr.add(sortJsonElementKeys(inputArr.get(i)));
            }
            return sortedArr;
        }
        return element;
    }
}
