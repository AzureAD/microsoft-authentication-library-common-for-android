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

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.microsoft.identity.common.java.authorities.Authority;
import com.microsoft.identity.common.java.commands.parameters.InteractiveTokenCommandParameters;
import com.microsoft.identity.common.java.flighting.CommonFlight;
import com.microsoft.identity.common.java.flighting.CommonFlightsManager;
import com.microsoft.identity.common.java.interfaces.INameValueStorage;
import com.microsoft.identity.common.java.interfaces.IPlatformComponents;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.opentelemetry.AttributeName;
import com.microsoft.identity.common.java.opentelemetry.SpanExtension;
import com.microsoft.identity.common.java.providers.microsoft.MicrosoftAuthorizationErrorResponse;
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationErrorResponse;
import com.microsoft.identity.common.java.providers.oauth2.OpenIdConnectPromptParameter;
import com.microsoft.identity.common.java.util.StringUtil;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.NonNull;

/**
 * Remembers the UPN across a MAM Conditional-Access "install Company Portal" interruption, so that
 * the next interactive request can pre-fill it instead of asking the user to type their address
 * again (Feature AB#3676213).
 * <p>
 * The UPN is the one the server returned on the broker-install redirect
 * ({@code msauth://wpj/?username=<upn>&app_link=...&intuneAppProtection=1}); nothing is inferred
 * beyond that, and only the MAM-CA path is recorded (see {@link MamCaRedirect}). It is written to
 * the platform's <em>encrypted</em>, app-private name-value store rather than held in memory,
 * because installing Company Portal very often kills the calling app's process.
 * <ul>
 *   <li><b>Lifetime.</b> Each record stores <em>when</em> it was written and is judged at read time
 *       against {@link CommonFlight#MAM_CA_UPN_HINT_TTL_SECONDS}, so changing that flight also
 *       governs records already on disk. A record is retired on a successful sign-in, by an explicit
 *       {@link #clearUpnHint}, or by its TTL - not by being read; see {@link #getValidUpnHint}.</li>
 *   <li><b>Sweeping.</b> Every read drops all unusable records, for every client id, so nothing
 *       lingers at rest. It runs whether or not the flight is on, so the kill switch cannot strand
 *       records already written.</li>
 *   <li><b>Keying.</b> Records are keyed by client id as well as by app, because "one process, one
 *       client id" does not hold: a broker hosts the WebView on behalf of every calling app, and one
 *       app may run several clients. This mirrors the credential cache. A hint whose client id
 *       cannot be determined is not stored.</li>
 *   <li><b>Authority binding.</b> Each record remembers the <em>host</em> it came from, and
 *       {@link #applyStoredUpnHintIfAbsent} will not send the UPN to a different one - that host is
 *       the sovereign-cloud boundary. Host and not full url, because apps routinely start at
 *       {@code /common} and retry against {@code /{tenantId}}, which is the very flow this
 *       feature serves.</li>
 *   <li><b>Flighting.</b> Reads and writes are gated by {@link CommonFlight#ENABLE_MAM_CA_UPN_HINT}
 *       (default off). {@link #clearUpnHint(IPlatformComponents, String)} is deliberately not gated,
 *       so cleanup keeps working after the flight is turned off.</li>
 *   <li><b>Telemetry.</b> {@link #applyStoredUpnHintIfAbsent} tags the request's span with
 *       {@link AttributeName#mam_ca_upn_hint_outcome} so the pre-fill's reach can be measured as the
 *       flight ramps; see {@link #reportOutcome}.</li>
 * </ul>
 * Every operation is best-effort: this is a convenience hint, so a storage failure is logged and
 * swallowed rather than allowed to fail the authentication request.
 */
// Deliberately Java, not Kotlin. In this module compileKotlin runs before compileJava, so Lombok has
// not generated its accessors yet and Kotlin cannot see them. This class reads Lombok-generated
// members of same-module classes - InteractiveTokenCommandParameters#getLoginHint()/getPrompt()/
// toBuilder() and AuthorizationErrorResponse#isMamCaInstall()/getUpnToWpj() - which is exactly what
// applyStoredUpnHintIfAbsent and the error-response overload exist to do, so it cannot be factored
// out. Converting this file needs the org.jetbrains.kotlin.plugin.lombok compiler plugin added to
// common4j first; that is a build change for the whole module, not for a feature PR. The tests are
// Kotlin because compileTestKotlin runs after main's Java and therefore does see those accessors.
public final class MamUpnHintStore {

    private static final String TAG = MamUpnHintStore.class.getSimpleName();

    /**
     * Name of the (encrypted) name-value store backing the hint. Deliberately its own store so the
     * whole feature can be reasoned about - and removed - independently of the token cache.
     */
    static final String STORE_NAME = "com.microsoft.identity.common.broker_install_upn_hint";

    /**
     * Prefix of the key holding a record; the client id is appended. One key per record, so that
     * writing one is a single storage operation and can never be torn by process death.
     */
    static final String KEY_PREFIX_RECORD = "record.";

    private static final Gson GSON = new Gson();

    /**
     * Values of {@link AttributeName#mam_ca_upn_hint_outcome}. A small fixed set, deliberately: they
     * are span attribute values, so they must not carry anything request-specific.
     */
    //@VisibleForTesting
    static final String OUTCOME_APPLIED = "applied";

    /** No hint was stored for this client, or it had expired. */
    //@VisibleForTesting
    static final String OUTCOME_NONE_STORED = "none_stored";

    /** Declined: the caller asked the user to pick or create an account. */
    //@VisibleForTesting
    static final String OUTCOME_DECLINED_PROMPT = "declined_prompt";

    /** Declined: the hint was captured against a different authority host. */
    //@VisibleForTesting
    static final String OUTCOME_DECLINED_AUTHORITY_MISMATCH = "declined_authority_mismatch";

    /** Declined: the request's own authority host could not be determined. */
    //@VisibleForTesting
    static final String OUTCOME_DECLINED_NO_REQUEST_HOST = "declined_no_request_host";

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
     * @param authorityHost      host of the authority that issued the redirect; the hint will only
     *                           ever be sent back to this host.
     * @param redirectParameters query parameters of the {@code msauth://wpj} broker-install redirect.
     */
    public static void saveUpnHintForMamCaInstall(@Nullable final IPlatformComponents components,
                                                  @Nullable final String clientId,
                                                  @Nullable final String authorityHost,
                                                  @Nullable final Map<String, String> redirectParameters) {
        final String methodTag = TAG + ":saveUpnHintForMamCaInstall";

        if (!isEnabled()) {
            Logger.verbose(methodTag, "Not storing a UPN hint: the flight is off.");
            return;
        }

        if (!MamCaRedirect.isMamCaInstall(redirectParameters)) {
            // An ordinary device-registration broker install; nothing to remember. Logged so that
            // the two kinds of broker-install redirect can be told apart in a capture.
            Logger.verbose(methodTag,
                    "Not storing a UPN hint: this broker install is not the MAM Conditional Access path.");
            return;
        }

        saveUpnHint(components, clientId, MamCaRedirect.getUsername(redirectParameters), authorityHost);
    }

    /**
     * Remembers the UPN the server returned on a MAM Conditional Access broker-install redirect,
     * given an already-parsed authorization error response.
     * <p>
     * No-op unless the request actually failed because the broker still needs to be installed
     * <em>and</em> the redirect was marked as the MAM-CA path.
     *
     * @param components    platform components providing the storage.
     * @param clientId      client id of the request being interrupted.
     * @param authority     the authority the interrupted request was made against; the hint will
     *                      only ever be sent back to this authority's host.
     * @param errorResponse the authorization error response.
     */
    public static void saveUpnHintForMamCaInstall(@Nullable final IPlatformComponents components,
                                                  @Nullable final String clientId,
                                                  @Nullable final Authority authority,
                                                  @Nullable final AuthorizationErrorResponse errorResponse) {
        final String methodTag = TAG + ":saveUpnHintForMamCaInstall";

        if (!isEnabled()) {
            Logger.verbose(methodTag, "Not storing a UPN hint: the flight is off.");
            return;
        }

        if (errorResponse == null
                || !MicrosoftAuthorizationErrorResponse.BROKER_NEEDS_TO_BE_INSTALLED
                        .equals(errorResponse.getError())) {
            // The request failed for some other reason, so there is no install to come back from.
            Logger.verbose(methodTag,
                    "Not storing a UPN hint: this failure is not a broker-install interrupt.");
            return;
        }

        if (!errorResponse.isMamCaInstall()) {
            Logger.verbose(methodTag,
                    "Not storing a UPN hint: this broker install is not the MAM Conditional Access path.");
            return;
        }

        saveUpnHint(components, clientId, errorResponse.getUpnToWpj(), hostOf(authority));
    }

    /**
     * Stores {@code upn} for {@code clientId}, stamped with the current time and bound to
     * {@code authorityHost}. No-op when the flight is off, when {@code upn} is null/blank, or when
     * the store cannot be opened.
     *
     * @param components    platform components providing the storage.
     * @param clientId      client id the hint belongs to.
     * @param upn           the UPN to remember.
     * @param authorityHost host of the authority the hint came from, if it could be determined.
     */
    //@VisibleForTesting
    static void saveUpnHint(@Nullable final IPlatformComponents components,
                            @Nullable final String clientId,
                            @Nullable final String upn,
                            @Nullable final String authorityHost) {
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

        final String normalizedHost = normalizeHost(authorityHost);
        try {
            saveUpnHint(storage, clientId, upn, normalizedHost, System.currentTimeMillis());
        } catch (final Exception e) {
            Logger.warn(methodTag, "Could not store the UPN hint: " + e.getClass().getSimpleName());
            return;
        }

        Logger.info(methodTag, "Stored a UPN hint for the MAM-CA broker-install flow; it is usable for "
                + (getTtlMillis() / 1000L) + "s, or until sign-in succeeds.");

        if (normalizedHost == null) {
            // Still worth storing: a host SDK can offer it in its own account field, where the user
            // sees it and nothing is transmitted. It just cannot be put on the wire automatically.
            Logger.warn(methodTag, "The authority host could not be determined, so the hint will not "
                    + "be attached to a request automatically.");
        }
    }

    /**
     * Storage-level write with an injectable clock.
     *
     * @param storage       the backing store.
     * @param clientId      client id the hint belongs to.
     * @param upn           the UPN to remember.
     * @param authorityHost host of the authority the hint came from, already normalized.
     * @param nowMillis     the current time in epoch millis.
     */
    //@VisibleForTesting
    static void saveUpnHint(@NonNull final INameValueStorage<String> storage,
                            @NonNull final String clientId,
                            @NonNull final String upn,
                            @Nullable final String authorityHost,
                            final long nowMillis) {
        // A single write, deliberately. The three fields only mean anything together - a UPN with
        // someone else's timestamp expires at the wrong moment, and a UPN with someone else's host
        // would be sent to the wrong cloud - so they must never be observable half-updated. Storing
        // them as one value makes that structurally impossible rather than a matter of ordering.
        storage.put(KEY_PREFIX_RECORD + clientId, GSON.toJson(new Record(upn, nowMillis, authorityHost)));
    }

    /**
     * Returns {@code clientId}'s stored UPN if - and only if - it is still within its TTL. Also
     * sweeps out every unusable record, for every client id.
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
     * The hint is retired instead on a successful sign-in, by an explicit {@link #clearUpnHint}, or
     * by its TTL. Note that {@link #applyStoredUpnHintIfAbsent} does not delete it either: the
     * request it was attached to can still fail, and the hint has to outlive that. Callers that
     * pre-fill their own UI may call {@link #clearUpnHint} once the user commits.
     * <p>
     * <b>This overload is not bound to an authority</b>, because there is not one yet: it exists to
     * fill in a text box the user is looking at and can edit, which transmits nothing. The authority
     * check belongs where the UPN is actually put on the wire, which is
     * {@link #applyStoredUpnHintIfAbsent}.
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
        final Record record = readValidRecord(components, clientId);
        if (record == null) {
            return null;
        }

        Logger.info(TAG + ":getValidUpnHint", "Returning the stored MAM-CA UPN hint.");
        return record.getUpn();
    }

    /**
     * Opens the store, sweeps it, and returns {@code clientId}'s record if it is still usable.
     *
     * @param components platform components providing the storage.
     * @param clientId   client id to read the record for.
     * @return the record, or {@code null} if there is no usable one.
     */
    @Nullable
    private static Record readValidRecord(@Nullable final IPlatformComponents components,
                                          @Nullable final String clientId) {
        final INameValueStorage<String> storage = getStorage(components);
        if (storage == null) {
            return null;
        }

        final long nowMillis = System.currentTimeMillis();
        final long ttlMillis = getTtlMillis();
        try {
            // The one place the store is swept, and it runs before the flight is consulted, not
            // after. Expiry is driven entirely by reads, so gating this behind isEnabled() would
            // mean that turning the flight off - the kill switch - stranded every UPN already on
            // disk permanently. Only handing a hint out is a feature decision; clearing one out is
            // hygiene and has to keep working.
            sweepUnusableRecords(storage, nowMillis, ttlMillis);

            if (!isEnabled()) {
                return null;
            }

            return getValidRecord(storage, clientId, nowMillis, ttlMillis);
        } catch (final Exception e) {
            // A pre-filled text box must never be the reason an authentication request fails.
            Logger.warn(TAG + ":readValidRecord",
                    "Could not read the stored UPN hint: " + e.getClass().getSimpleName());
            return null;
        }
    }

    /**
     * Storage-level read of the whole record, with an injectable clock and TTL. Non-destructive, and
     * does not sweep - {@link #readValidRecord} does that once, before calling this.
     *
     * @param storage   the backing store.
     * @param clientId  client id to read the record for.
     * @param nowMillis the current time in epoch millis.
     * @param ttlMillis how long a hint stays usable, in millis.
     * @return the record if still valid, otherwise {@code null}.
     */
    //@VisibleForTesting
    @Nullable
    static Record getValidRecord(@NonNull final INameValueStorage<String> storage,
                                 @Nullable final String clientId,
                                 final long nowMillis,
                                 final long ttlMillis) {
        if (StringUtil.isNullOrEmpty(clientId)) {
            // Records are only ever written under a real client id, so there is nothing to look up.
            return null;
        }

        final Record record = deserialize(storage.get(KEY_PREFIX_RECORD + clientId));
        // Judge this record on its own terms rather than trusting the sweep to have removed it: the
        // sweep depends on the store being able to enumerate itself, and an expired UPN must never
        // be handed out just because a listing came back short.
        if (!isUsable(record, nowMillis, ttlMillis)) {
            Logger.verbose(TAG + ":getValidRecord", "No usable stored record for this client id.");
            return null;
        }

        return record;
    }

    /**
     * Deletes everything in the store that can no longer be handed out: expired records, records
     * that cannot be read back, and any key that is not a record at all. The store belongs to this
     * feature alone, so anything unrecognized in it is residue and is cleaned up on sight.
     *
     * @param storage   the backing store.
     * @param nowMillis the current time in epoch millis.
     * @param ttlMillis how long a hint stays usable, in millis.
     */
    //@VisibleForTesting
    static void sweepUnusableRecords(@NonNull final INameValueStorage<String> storage,
                                     final long nowMillis,
                                     final long ttlMillis) {
        final String methodTag = TAG + ":sweepUnusableRecords";

        final Map<String, String> all = storage.getAll();
        final List<String> unusable = new ArrayList<>();
        for (final Map.Entry<String, String> entry : all.entrySet()) {
            final String key = entry.getKey();
            if (!key.startsWith(KEY_PREFIX_RECORD)
                    || !isUsable(deserialize(entry.getValue()), nowMillis, ttlMillis)) {
                unusable.add(key);
            }
        }

        if (unusable.isEmpty()) {
            return;
        }

        Logger.info(methodTag, "Discarding " + unusable.size()
                + " expired or unreadable UPN hint record(s).");
        for (final String key : unusable) {
            storage.remove(key);
        }
    }

    @Nullable
    private static Record deserialize(@Nullable final String raw) {
        if (StringUtil.isNullOrEmpty(raw)) {
            return null;
        }

        try {
            return GSON.fromJson(raw, Record.class);
        } catch (final Exception e) {
            // Anything we cannot read back is treated as absent, and the sweep removes it.
            return null;
        }
    }

    private static boolean isUsable(@Nullable final Record record,
                                    final long nowMillis,
                                    final long ttlMillis) {
        if (record == null || StringUtil.isNullOrEmpty(record.getUpn())) {
            return false;
        }

        final long writtenAtMillis = record.getWrittenAtMillis();

        // writtenAt > now means the clock moved backwards; treat it as untrustworthy rather than
        // letting the hint outlive its window. The lower bound matters just as much: a record whose
        // timestamp is missing or corrupt would otherwise be measured from the epoch, and a negative
        // one would make the subtraction below overflow and wrap positive, which reads as "written
        // moments ago" and would keep the record alive forever.
        if (writtenAtMillis <= 0L || writtenAtMillis > nowMillis) {
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

        storage.remove(KEY_PREFIX_RECORD + clientId);
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
     * Nor is it applied to an authority whose host differs from the one the hint was captured
     * against. This is the point at which the UPN leaves the device, so it is the point at which
     * that has to be checked; a mismatch is declined but the record is left alone, because a request
     * to some other authority must not destroy a hint the user is about to return for.
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
            // The caller already knows who is signing in. Deliberately not reported: this is the
            // ordinary case for most callers and says nothing about how the feature is doing.
            return parameters;
        }

        final OpenIdConnectPromptParameter prompt = parameters.getPrompt();
        if (prompt == OpenIdConnectPromptParameter.SELECT_ACCOUNT
                || prompt == OpenIdConnectPromptParameter.CREATE) {
            Logger.info(methodTag,
                    "Not pre-filling login_hint: the caller asked the user to choose or create an account.");
            reportOutcome(OUTCOME_DECLINED_PROMPT);
            return parameters;
        }

        // Read first, so that the sweep still runs even when the hint turns out not to be applicable.
        final Record record = readValidRecord(parameters.getPlatformComponents(), parameters.getClientId());
        if (record == null || StringUtil.isNullOrEmpty(record.getUpn())) {
            reportOutcome(OUTCOME_NONE_STORED);
            return parameters;
        }

        final String requestHost = hostOf(parameters.getAuthority());
        if (requestHost == null) {
            Logger.info(methodTag,
                    "Not pre-filling login_hint: the request's authority host could not be determined.");
            reportOutcome(OUTCOME_DECLINED_NO_REQUEST_HOST);
            return parameters;
        }

        // A record with no host of its own fails this too, which is the intent: if we could not tell
        // where the hint came from, we cannot tell that it is safe to send anywhere.
        if (!requestHost.equals(record.getAuthorityHost())) {
            Logger.info(methodTag,
                    "Not pre-filling login_hint: the hint was stored against a different authority.");
            reportOutcome(OUTCOME_DECLINED_AUTHORITY_MISMATCH);
            return parameters;
        }

        Logger.info(methodTag, "Pre-filling login_hint from the stored MAM-CA UPN hint.");
        reportOutcome(OUTCOME_APPLIED);
        return parameters.toBuilder().loginHint(record.getUpn()).build();
    }

    /**
     * Tags the request's span with what the pre-fill did, so the feature's reach can be measured
     * while the flight ramps instead of inferred from logs. The value is one of a small fixed set,
     * so this cannot inflate attribute cardinality.
     * <p>
     * Nothing is reported when the caller supplied its own {@code login_hint}: that is the ordinary
     * case and would drown the signal. Nothing is reported on the retirement path either - a hint
     * that was applied already tagged this same span with {@link #OUTCOME_APPLIED}, and whether the
     * request then succeeded is already on it.
     * <p>
     * Note that the flight-off counterfactual - "how many requests would have been pre-filled" - is
     * not obtainable and is not attempted: writes are gated by the same flight, so with it off no
     * hint is ever stored and there is nothing to have counted. Making it observable would mean
     * persisting UPNs for users who are not in the experiment, which is not a trade worth making for
     * a ramp metric.
     *
     * @param outcome one of the {@code OUTCOME_*} constants.
     */
    private static void reportOutcome(@NonNull final String outcome) {
        try {
            SpanExtension.current().setAttribute(AttributeName.mam_ca_upn_hint_outcome.name(), outcome);
        } catch (final Exception e) {
            // Telemetry must never be the reason a sign-in fails.
            Logger.verbose(TAG + ":reportOutcome",
                    "Could not report the outcome: " + e.getClass().getSimpleName());
        }
    }

    /**
     * Host of {@code authority}, normalized, or {@code null} when it cannot be determined.
     *
     * @param authority the authority, which may be null or carry a malformed url.
     * @return the host, lower-cased, or {@code null}.
     */
    @Nullable
    private static String hostOf(@Nullable final Authority authority) {
        if (authority == null) {
            return null;
        }

        try {
            final URI uri = authority.getAuthorityUri();
            return normalizeHost(uri == null ? null : uri.getHost());
        } catch (final Exception e) {
            // Authority#getAuthorityUri throws on a malformed url. An authority we cannot parse is
            // simply one we will not pre-fill for; it must never be a reason a request fails.
            Logger.warn(TAG + ":hostOf",
                    "Could not read the authority host: " + e.getClass().getSimpleName());
            return null;
        }
    }

    /**
     * Host names are case-insensitive, so they are compared lower-cased.
     *
     * @param host the raw host.
     * @return the normalized host, or {@code null} when there is nothing usable.
     */
    @Nullable
    private static String normalizeHost(@Nullable final String host) {
        return StringUtil.isNullOrEmpty(host) ? null : host.trim().toLowerCase(Locale.ROOT);
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

    /**
     * One stored hint. Written and read as a single serialized value so that a record is never
     * observable half-updated: the UPN, the moment it was captured, and the authority it was
     * captured against only mean anything together.
     * <p>
     * Fields are named explicitly for serialization so that an obfuscated build reads back records
     * written by a non-obfuscated one.
     */
    static final class Record {

        @SerializedName("upn")
        @Nullable
        private String upn;

        @SerializedName("written_at")
        private long writtenAtMillis;

        /** Host of the authority this UPN came from; null when it could not be determined. */
        @SerializedName("authority_host")
        @Nullable
        private String authorityHost;

        /** For the deserializer. */
        Record() {
        }

        Record(@Nullable final String upn,
               final long writtenAtMillis,
               @Nullable final String authorityHost) {
            this.upn = upn;
            this.writtenAtMillis = writtenAtMillis;
            this.authorityHost = authorityHost;
        }

        @Nullable
        String getUpn() {
            return upn;
        }

        long getWrittenAtMillis() {
            return writtenAtMillis;
        }

        @Nullable
        String getAuthorityHost() {
            return authorityHost;
        }
    }
}
