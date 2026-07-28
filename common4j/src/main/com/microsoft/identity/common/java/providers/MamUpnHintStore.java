//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.

package com.microsoft.identity.common.java.providers;

import com.microsoft.identity.common.java.commands.parameters.InteractiveTokenCommandParameters;
import com.microsoft.identity.common.java.flighting.CommonFlight;
import com.microsoft.identity.common.java.flighting.CommonFlightsManager;
import com.microsoft.identity.common.java.interfaces.INameValueStorage;
import com.microsoft.identity.common.java.interfaces.IPlatformComponents;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.providers.microsoft.MicrosoftAuthorizationErrorResponse;
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationErrorResponse;
import com.microsoft.identity.common.java.util.StringUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.NonNull;

/**
 * Remembers the UPN across a MAM Conditional-Access "install Company Portal" interruption, so that
 * the next interactive request can pre-fill it instead of asking the user to type their address
 * again (Feature AB#3676213).
 * <p>
 * <b>Why this needs to be persisted.</b> When Conditional Access blocks an interactive request until
 * Company Portal is installed, the user leaves for the Play Store. Installing Company Portal very
 * often kills the calling app's process, so the UPN cannot simply be held in memory - it has to
 * survive process death. It is therefore written to the platform's <em>encrypted</em> name-value
 * store, which is private to the calling app.
 * <p>
 * <b>Where the UPN comes from.</b> The server returns it on the broker-install redirect
 * ({@code msauth://wpj/?username=<upn>&app_link=...&intuneAppProtection=1}). Nothing is inferred or
 * collected here beyond what the server already told us, and only the MAM-CA path is recorded - an
 * ordinary device-registration broker install stores nothing (see {@link MamCaRedirect}).
 * <p>
 * <b>Lifetime.</b> The hint is a short-lived UI convenience and is treated as such:
 * <ul>
 *   <li>Each record stores <em>when it was written</em>, and validity is decided at read time
 *       against {@link CommonFlight#MAM_CA_UPN_HINT_TTL_SECONDS}. Storing the write time rather than
 *       an absolute expiry means changing that flight also governs records already on disk.</li>
 *   <li>Reads are <b>single-use</b>: a hint that is handed out is deleted in the same call, so it
 *       can never be replayed onto a later, unrelated request.</li>
 *   <li>Every read also sweeps out <em>all</em> expired or half-written records, for every client
 *       id, so nothing lingers at rest beyond the window the flow needs.</li>
 * </ul>
 * <p>
 * <b>Keying.</b> The store is app-private, and records are additionally keyed by client id so that
 * two clients hosted in the same app cannot read each other's hint.
 * <p>
 * <b>Flighting.</b> Reads and writes are gated by {@link CommonFlight#ENABLE_MAM_CA_UPN_HINT}
 * (default off), so with the flight off nothing is ever stored and behavior is unchanged.
 * {@link #clearUpnHint(IPlatformComponents, String)} is deliberately <em>not</em> gated so that
 * cleanup always works, including after the flight has been turned off.
 * <p>
 * Every operation is best-effort: this is a convenience hint, so a storage failure is logged and
 * swallowed rather than allowed to fail the authentication request.
 */
public final class MamUpnHintStore {

    private static final String TAG = MamUpnHintStore.class.getSimpleName();

    /**
     * Name of the (encrypted) name-value store backing the hint. Deliberately its own store so the
     * whole feature can be reasoned about - and removed - independently of the token cache.
     */
    static final String STORE_NAME = "com.microsoft.identity.common.broker_install_upn_hint";

    /** Prefix of the key holding the UPN; the client id is appended. */
    static final String KEY_PREFIX_UPN = "upn.";

    /**
     * Prefix of the key holding when the UPN was written, in epoch milliseconds, as a decimal
     * string; the client id is appended.
     */
    static final String KEY_PREFIX_WRITTEN_AT = "written_at.";

    /**
     * Fallback client id for callers that cannot supply one, so such records are still keyed
     * consistently rather than colliding with a real client id.
     */
    static final String UNKNOWN_CLIENT_ID = "unknown";

    private MamUpnHintStore() {
        // Utility class.
    }

    private static boolean isEnabled() {
        return CommonFlightsManager.INSTANCE.getFlightsProvider()
                .isFlightEnabled(CommonFlight.ENABLE_MAM_CA_UPN_HINT);
    }

    /**
     * How long a stored UPN stays usable, in milliseconds, per
     * {@link CommonFlight#MAM_CA_UPN_HINT_TTL_SECONDS}.
     *
     * @return the TTL in milliseconds.
     */
    public static long getTtlMillis() {
        return 1000L * CommonFlightsManager.INSTANCE.getFlightsProvider()
                .getIntValue(CommonFlight.MAM_CA_UPN_HINT_TTL_SECONDS);
    }

    /**
     * Remembers the UPN the server returned on a MAM Conditional Access broker-install redirect.
     * <p>
     * No-op unless the redirect is marked as the MAM-CA path, so an ordinary device-registration
     * broker install stores nothing.
     *
     * @param components         platform components providing the storage.
     * @param clientId           client id of the request being interrupted.
     * @param redirectParameters query parameters of the {@code msauth://wpj} broker-install redirect.
     */
    public static void saveUpnHintForMamCaInstall(@Nullable final IPlatformComponents components,
                                                  @Nullable final String clientId,
                                                  @Nullable final Map<String, String> redirectParameters) {
        if (!isEnabled() || !MamCaRedirect.isMamCaInstall(redirectParameters)) {
            return;
        }
        saveUpnHint(components, clientId, MamCaRedirect.getUsername(redirectParameters));
    }

    /**
     * Remembers the UPN the server returned on a MAM Conditional Access broker-install redirect,
     * given an already-parsed authorization error response.
     * <p>
     * No-op unless the request actually failed because the broker still needs to be installed
     * <em>and</em> the redirect was marked as the MAM-CA path.
     *
     * @param components   platform components providing the storage.
     * @param clientId     client id of the request being interrupted.
     * @param errorResponse the authorization error response.
     */
    public static void saveUpnHintForMamCaInstall(@Nullable final IPlatformComponents components,
                                                  @Nullable final String clientId,
                                                  @Nullable final AuthorizationErrorResponse errorResponse) {
        if (errorResponse == null
                || !MicrosoftAuthorizationErrorResponse.BROKER_NEEDS_TO_BE_INSTALLED
                        .equals(errorResponse.getError())) {
            return;
        }

        if (!errorResponse.isMamCaInstall()) {
            // An ordinary device-registration broker install; nothing to remember.
            return;
        }

        saveUpnHint(components, clientId, errorResponse.getUpnToWpj());
    }

    /**
     * Stores {@code upn} for {@code clientId}, stamped with the current time. No-op when the flight
     * is off, when {@code upn} is null/blank, or when the store cannot be opened.
     *
     * @param components platform components providing the storage.
     * @param clientId   client id the hint belongs to.
     * @param upn        the UPN to remember.
     */
    public static void saveUpnHint(@Nullable final IPlatformComponents components,
                                   @Nullable final String clientId,
                                   @Nullable final String upn) {
        final String methodTag = TAG + ":saveUpnHint";

        if (!isEnabled() || StringUtil.isNullOrEmpty(upn)) {
            return;
        }

        final INameValueStorage<String> storage = getStorage(components);
        if (storage == null) {
            return;
        }

        saveUpnHint(storage, clientId, upn, System.currentTimeMillis());
        Logger.info(methodTag, "Stored a UPN hint for the MAM-CA broker-install flow; it is usable for "
                + (getTtlMillis() / 1000L) + "s and only once.");
    }

    /**
     * Storage-level write with an injectable clock.
     *
     * @param storage   the backing store.
     * @param clientId  client id the hint belongs to.
     * @param upn       the UPN to remember.
     * @param nowMillis the current time in epoch millis.
     */
    static void saveUpnHint(@NonNull final INameValueStorage<String> storage,
                            @Nullable final String clientId,
                            @NonNull final String upn,
                            final long nowMillis) {
        final String suffix = keySuffix(clientId);
        storage.put(KEY_PREFIX_WRITTEN_AT + suffix, String.valueOf(nowMillis));
        storage.put(KEY_PREFIX_UPN + suffix, upn);
    }

    /**
     * Returns {@code clientId}'s stored UPN if - and only if - it is still within its TTL, and
     * deletes it in the same call so it is used at most once. Also sweeps out every expired or
     * half-written record, for every client id.
     * <p>
     * <b>Call this when the account-entry screen is shown, not only when the process starts.</b>
     * Installing the broker does not reliably kill the calling app: when the process survives, the
     * user returns to an already-created screen, so a hint read wired to process or view creation
     * never runs and the field is left empty. Reading on every presentation is safe - the hint is
     * single-use and self-clearing, so a read that finds nothing simply returns {@code null}.
     * <p>
     * Apps whose interactive requests flow through {@link InteractiveTokenCommandParameters} do not
     * need this at all; {@link #applyStoredUpnHintIfAbsent} already applies the hint for them.
     *
     * @param components platform components providing the storage.
     * @param clientId   client id to read the hint for.
     * @return the UPN to pre-fill, or {@code null} if there is no usable hint.
     */
    @Nullable
    public static String getValidUpnHint(@Nullable final IPlatformComponents components,
                                         @Nullable final String clientId) {
        if (!isEnabled()) {
            return null;
        }

        final INameValueStorage<String> storage = getStorage(components);
        if (storage == null) {
            return null;
        }

        return getValidUpnHint(storage, clientId, System.currentTimeMillis(), getTtlMillis());
    }

    /**
     * Storage-level read with an injectable clock and TTL.
     *
     * @param storage   the backing store.
     * @param clientId  client id to read the hint for.
     * @param nowMillis the current time in epoch millis.
     * @param ttlMillis how long a hint stays usable, in millis.
     * @return the UPN if still valid, otherwise {@code null}.
     */
    @Nullable
    static String getValidUpnHint(@NonNull final INameValueStorage<String> storage,
                                  @Nullable final String clientId,
                                  final long nowMillis,
                                  final long ttlMillis) {
        final String methodTag = TAG + ":getValidUpnHint";

        final String suffix = keySuffix(clientId);
        // Sweep first, so a hint that is itself stale is dropped rather than returned below.
        sweepUnusableRecords(storage, nowMillis, ttlMillis);

        final String upn = storage.get(KEY_PREFIX_UPN + suffix);
        if (StringUtil.isNullOrEmpty(upn)) {
            return null;
        }

        // Single-use: whether or not the caller ends up showing it, this hint is now spent.
        clearUpnHint(storage, clientId);
        Logger.info(methodTag, "Returning the stored MAM-CA UPN hint and clearing it.");
        return upn;
    }

    /**
     * Deletes every record that can no longer be handed out: expired, half-written (the process died
     * between the two writes), or carrying an unparseable timestamp. Runs across all client ids so a
     * hint written for a client that is never asked for again does not linger at rest.
     *
     * @param storage   the backing store.
     * @param nowMillis the current time in epoch millis.
     * @param ttlMillis how long a hint stays usable, in millis.
     */
    private static void sweepUnusableRecords(@NonNull final INameValueStorage<String> storage,
                                             final long nowMillis,
                                             final long ttlMillis) {
        final String methodTag = TAG + ":sweepUnusableRecords";

        final Map<String, String> all = storage.getAll();
        final Set<String> suffixes = new HashSet<>();
        for (final String key : all.keySet()) {
            if (key.startsWith(KEY_PREFIX_UPN)) {
                suffixes.add(key.substring(KEY_PREFIX_UPN.length()));
            } else if (key.startsWith(KEY_PREFIX_WRITTEN_AT)) {
                suffixes.add(key.substring(KEY_PREFIX_WRITTEN_AT.length()));
            }
        }

        final List<String> unusable = new ArrayList<>();
        for (final String suffix : suffixes) {
            if (!isUsable(all.get(KEY_PREFIX_UPN + suffix),
                    all.get(KEY_PREFIX_WRITTEN_AT + suffix), nowMillis, ttlMillis)) {
                unusable.add(suffix);
            }
        }

        if (unusable.isEmpty()) {
            return;
        }

        Logger.info(methodTag, "Discarding " + unusable.size()
                + " expired or incomplete UPN hint record(s).");
        for (final String suffix : unusable) {
            storage.remove(KEY_PREFIX_UPN + suffix);
            storage.remove(KEY_PREFIX_WRITTEN_AT + suffix);
        }
    }

    private static boolean isUsable(@Nullable final String upn,
                                    @Nullable final String writtenAtRaw,
                                    final long nowMillis,
                                    final long ttlMillis) {
        if (StringUtil.isNullOrEmpty(upn) || StringUtil.isNullOrEmpty(writtenAtRaw)) {
            // A record is only trustworthy with both halves present; without a timestamp we cannot
            // know how old the UPN is, so it must not be handed out.
            return false;
        }

        final long writtenAtMillis;
        try {
            writtenAtMillis = Long.parseLong(writtenAtRaw);
        } catch (final NumberFormatException e) {
            return false;
        }

        // writtenAt > now means the clock moved backwards; treat it as untrustworthy rather than
        // letting the hint outlive its window.
        return writtenAtMillis <= nowMillis && nowMillis - writtenAtMillis < ttlMillis;
    }

    /**
     * Deletes {@code clientId}'s stored UPN. Deliberately not flight-gated so that cleanup always
     * works, including after the flight has been turned off.
     *
     * @param components platform components providing the storage.
     * @param clientId   client id whose hint should be dropped.
     */
    public static void clearUpnHint(@Nullable final IPlatformComponents components,
                                    @Nullable final String clientId) {
        final INameValueStorage<String> storage = getStorage(components);
        if (storage != null) {
            clearUpnHint(storage, clientId);
        }
    }

    /**
     * Storage-level delete.
     *
     * @param storage  the backing store.
     * @param clientId client id whose hint should be dropped.
     */
    static void clearUpnHint(@NonNull final INameValueStorage<String> storage,
                             @Nullable final String clientId) {
        final String suffix = keySuffix(clientId);
        storage.remove(KEY_PREFIX_UPN + suffix);
        storage.remove(KEY_PREFIX_WRITTEN_AT + suffix);
    }

    /**
     * Returns {@code parameters} with {@code login_hint} pre-filled from a still-valid stored UPN.
     * <p>
     * An explicit {@code login_hint} from the caller always wins - it is never overwritten - and the
     * original instance is returned untouched whenever there is nothing useful to add, so this is
     * safe to call on every interactive request.
     * <p>
     * Host SDKs that render their own account-entry UI (rather than going straight to the
     * authorization request) should call {@link #getValidUpnHint(IPlatformComponents, String)}
     * directly to pre-fill that field.
     *
     * @param parameters the interactive request parameters.
     * @return the same instance, or a copy carrying the pre-filled {@code login_hint}.
     */
    @NonNull
    public static InteractiveTokenCommandParameters applyStoredUpnHintIfAbsent(
            @NonNull final InteractiveTokenCommandParameters parameters) {
        final String methodTag = TAG + ":applyStoredUpnHintIfAbsent";

        if (!StringUtil.isNullOrEmpty(parameters.getLoginHint())) {
            // The caller already knows who is signing in.
            return parameters;
        }

        final String upn = getValidUpnHint(parameters.getPlatformComponents(), parameters.getClientId());
        if (StringUtil.isNullOrEmpty(upn)) {
            return parameters;
        }

        Logger.info(methodTag, "Pre-filling login_hint from the stored MAM-CA UPN hint.");
        return parameters.toBuilder().loginHint(upn).build();
    }

    private static String keySuffix(@Nullable final String clientId) {
        return StringUtil.isNullOrEmpty(clientId) ? UNKNOWN_CLIENT_ID : clientId;
    }

    @Nullable
    private static INameValueStorage<String> getStorage(@Nullable final IPlatformComponents components) {
        final String methodTag = TAG + ":getStorage";

        if (components == null) {
            return null;
        }

        try {
            return components.getStorageSupplier().getEncryptedNameValueStore(STORE_NAME, String.class);
        } catch (final Exception e) {
            // This store only ever backs a UI convenience, so no storage problem (missing keys,
            // decryption failure, ...) is worth failing an authentication request over.
            Logger.warn(methodTag,
                    "Could not open the UPN hint store: " + e.getClass().getSimpleName());
            return null;
        }
    }
}
