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
import com.microsoft.identity.common.java.providers.oauth2.OpenIdConnectPromptParameter;
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
 *   <li>Reads are non-destructive, because this flow reaches the caller's account screen more than
 *       once - see {@link #getValidUpnHint}. A record is retired on a successful sign-in, by an
 *       explicit {@link #clearUpnHint}, or by its TTL. The TTL is what bounds replay, so it is kept
 *       short; within that window a hint may be offered to more than one request for the same
 *       client, which is why {@link #applyStoredUpnHintIfAbsent} declines to answer a request that
 *       explicitly asked the user to choose an account.</li>
 *   <li>Every read sweeps out <em>all</em> expired or half-written records, for every client id, so
 *       nothing lingers at rest beyond the window the flow needs. The sweep runs whether or not the
 *       flight is on, so turning the flight off cannot strand records already written.</li>
 * </ul>
 * <p>
 * <b>Keying.</b> The store is app-private, so it is already isolated between apps. Records are
 * <em>additionally</em> keyed by client id because "one process, one client id" does not hold: a
 * broker hosts the authorization WebView in its own process on behalf of every calling app, and a
 * single app may run more than one client. This mirrors the credential cache, whose keys include the
 * client id for the same reason. A hint whose client id cannot be determined is not stored at all.
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
    static void saveUpnHint(@Nullable final IPlatformComponents components,
                            @Nullable final String clientId,
                            @Nullable final String upn) {
        final String methodTag = TAG + ":saveUpnHint";

        if (!isEnabled() || StringUtil.isNullOrEmpty(upn)) {
            return;
        }

        if (StringUtil.isNullOrEmpty(clientId)) {
            // A record is addressed by client id, so one written without a real client id could only
            // be found under a shared key - which is precisely the cross-client bleed the keying
            // exists to prevent. Drop it instead; the cost is one un-prefilled field.
            Logger.warn(methodTag, "Not storing a UPN hint: the client id could not be determined.");
            return;
        }

        final INameValueStorage<String> storage = getStorage(components);
        if (storage == null) {
            return;
        }

        try {
            saveUpnHint(storage, clientId, upn, System.currentTimeMillis());
        } catch (final Exception e) {
            Logger.warn(methodTag, "Could not store the UPN hint: " + e.getClass().getSimpleName());
            return;
        }

        Logger.info(methodTag, "Stored a UPN hint for the MAM-CA broker-install flow; it is usable for "
                + (getTtlMillis() / 1000L) + "s, until it is carried into a request.");
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
                            @NonNull final String clientId,
                            @NonNull final String upn,
                            final long nowMillis) {
        // The UPN goes first, deliberately. These are two independent asynchronous writes, so a
        // process death between them tears the record. Writing the UPN first means a tear pairs the
        // new UPN with the previous timestamp, which at worst expires early. The other order pairs
        // the *previous* UPN with a fresh timestamp, which would hand one user's address to the next
        // and silently restart the clock on it.
        storage.put(KEY_PREFIX_UPN + clientId, upn);
        storage.put(KEY_PREFIX_WRITTEN_AT + clientId, String.valueOf(nowMillis));
    }

    /**
     * Returns {@code clientId}'s stored UPN if - and only if - it is still within its TTL. Also
     * sweeps out every expired or half-written record, for every client id.
     * <p>
     * <b>Call this when the account-entry screen is shown, not only when the process starts.</b>
     * Installing the broker does not reliably kill the calling app: when the process survives, the
     * user returns to an already-created screen, so a hint read wired to process or view creation
     * never runs and the field is left empty.
     * <p>
     * <b>Reading does not spend the hint.</b> Handling the install redirect tears down the
     * authorization activity before the store listing is launched, which briefly resumes the
     * caller's own account screen while the flow is still in progress. A read that consumed the
     * hint would therefore destroy it seconds before the app restart it exists to survive, and the
     * damage would be invisible - the field would look pre-filled right up until the process died.
     * The hint is retired instead when it is actually used: {@link #applyStoredUpnHintIfAbsent}
     * clears it once it has been attached to a request, and its TTL bounds it either way. Callers
     * that pre-fill their own UI may call {@link #clearUpnHint} once the user commits.
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
        final INameValueStorage<String> storage = getStorage(components);
        if (storage == null) {
            return null;
        }

        try {
            // Sweep before consulting the flight, not after. Expiry is driven entirely by reads, so
            // gating this behind isEnabled() would mean that turning the flight off - the kill
            // switch - stranded every UPN already on disk permanently. Only handing a hint out is
            // a feature decision; clearing one out is hygiene and has to keep working.
            sweepUnusableRecords(storage, System.currentTimeMillis(), getTtlMillis());

            if (!isEnabled()) {
                return null;
            }

            return getValidUpnHint(storage, clientId, System.currentTimeMillis(), getTtlMillis());
        } catch (final Exception e) {
            // A pre-filled text box must never be the reason an authentication request fails.
            Logger.warn(TAG + ":getValidUpnHint",
                    "Could not read the stored UPN hint: " + e.getClass().getSimpleName());
            return null;
        }
    }

    /**
     * Storage-level read with an injectable clock and TTL. Non-destructive; see
     * {@link #getValidUpnHint(IPlatformComponents, String)} for why.
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

        // Sweep first, so a hint that is itself stale is dropped rather than returned below. This
        // runs before the client-id check because it is not scoped to one client.
        sweepUnusableRecords(storage, nowMillis, ttlMillis);

        if (StringUtil.isNullOrEmpty(clientId)) {
            // Records are only ever written under a real client id, so there is nothing to look up.
            return null;
        }

        final String upn = storage.get(KEY_PREFIX_UPN + clientId);
        // Re-check this record rather than trusting the sweep to have removed it: the sweep depends
        // on the store being able to enumerate itself, and an expired UPN must never be handed out
        // just because a listing came back short.
        if (!isUsable(upn, storage.get(KEY_PREFIX_WRITTEN_AT + clientId), nowMillis, ttlMillis)) {
            return null;
        }

        Logger.info(methodTag, "Returning the stored MAM-CA UPN hint.");
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
        // letting the hint outlive its window. The lower bound matters just as much: a corrupted
        // negative timestamp would make the subtraction below overflow and wrap positive, which
        // reads as "written moments ago" and would keep the record alive forever.
        if (writtenAtMillis < 0L || writtenAtMillis > nowMillis) {
            return false;
        }

        return nowMillis - writtenAtMillis < ttlMillis;
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
        if (storage == null) {
            return;
        }

        try {
            clearUpnHint(storage, clientId);
        } catch (final Exception e) {
            Logger.warn(TAG + ":clearUpnHint",
                    "Could not clear the stored UPN hint: " + e.getClass().getSimpleName());
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
        if (StringUtil.isNullOrEmpty(clientId)) {
            // Records are only ever written under a real client id, so there is nothing to delete.
            return;
        }

        storage.remove(KEY_PREFIX_UPN + clientId);
        storage.remove(KEY_PREFIX_WRITTEN_AT + clientId);
    }

    /**
     * Returns {@code parameters} with {@code login_hint} pre-filled from a still-valid stored UPN.
     * <p>
     * An explicit {@code login_hint} from the caller always wins - it is never overwritten - and the
     * original instance is returned untouched whenever there is nothing useful to add, so this is
     * safe to call on every interactive request.
     * <p>
     * The hint is deliberately <em>not</em> applied when the caller asked to pick or create an
     * account. Setting {@code login_hint} is not merely cosmetic: {@code BaseController} suppresses
     * the account-picker page whenever a hint is present, and on the broker path the hint becomes the
     * account-resolution field. Injecting a remembered address there would silently answer a question
     * the caller explicitly wanted to put to the user.
     * <p>
     * Applying a hint does not delete it; it is retired on a successful sign-in, or by its TTL. The
     * request it was attached to may still fail - a flaky network right after a large broker install
     * is exactly the case this feature exists for - and the hint has to outlive that to be useful.
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

        final OpenIdConnectPromptParameter prompt = parameters.getPrompt();
        if (prompt == OpenIdConnectPromptParameter.SELECT_ACCOUNT
                || prompt == OpenIdConnectPromptParameter.CREATE) {
            Logger.info(methodTag,
                    "Not pre-filling login_hint: the caller asked the user to choose or create an account.");
            return parameters;
        }

        final String upn = getValidUpnHint(parameters.getPlatformComponents(), parameters.getClientId());
        if (StringUtil.isNullOrEmpty(upn)) {
            return parameters;
        }

        Logger.info(methodTag, "Pre-filling login_hint from the stored MAM-CA UPN hint.");
        return parameters.toBuilder().loginHint(upn).build();
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
