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
import com.microsoft.identity.common.java.util.StringUtil;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.NonNull;

/**
 * Remembers the UPN across a Conditional-Access "install the broker" interruption, so that the next
 * interactive request can pre-fill it instead of asking the user to type their address again
 * (Feature AB#3676213).
 * <p>
 * <b>Why this needs to be persisted.</b> When Conditional Access blocks an interactive request until
 * Company Portal is installed, the user leaves for the Play Store. Installing Company Portal very
 * often kills the calling app's process, so the UPN cannot simply be held in memory - it has to
 * survive process death. It is therefore written to the platform's <em>encrypted</em> name-value
 * store, which is private to the calling app.
 * <p>
 * <b>Where the UPN comes from.</b> The server already returns it on the broker-install redirect
 * ({@code msauth://wpj/?username=<upn>&app_link=...}); {@code AuthorizationResultFactory} parses it
 * onto {@link com.microsoft.identity.common.java.providers.oauth2.AuthorizationErrorResponse#getUpnToWpj()}.
 * Nothing is inferred or collected here beyond what the server already told us.
 * <p>
 * <b>Lifetime.</b> A hint is only ever a short-lived UI convenience, so every record carries an
 * absolute expiry ({@link #DEFAULT_TTL_MILLISECONDS}). Reads are TTL-validated: an expired - or
 * half-written - record is never returned and is deleted in place, so the UPN is not kept at rest
 * any longer than the flow needs it. A hint is also dropped once it has served its purpose (see
 * {@link #clearUpnHint(IPlatformComponents)}).
 * <p>
 * <b>Flighting.</b> Reads and writes are gated by {@link CommonFlight#ENABLE_BROKER_INSTALL_UPN_HINT}
 * (default off), so with the flight off nothing is ever stored and behavior is unchanged. Note that
 * disabling the flight while a hint is already stored leaves that single record on disk until the
 * next read with the flight on (which will find it expired and delete it); it can never be returned
 * in the meantime. {@link #clearUpnHint(IPlatformComponents)} is deliberately <em>not</em> gated so
 * that cleanup always works.
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

    /** Key holding the UPN itself. */
    static final String KEY_UPN = "upn";

    /** Key holding the absolute expiry of the UPN, in epoch milliseconds, as a decimal string. */
    static final String KEY_EXPIRES_AT = "expires_at";

    /**
     * How long a stored UPN stays usable: 7 minutes, matching the broker-install park TTL. It covers
     * a Company Portal download + install + first launch on a slow network plus a little user
     * think-time, while keeping the UPN at rest for a bounded, short window.
     */
    public static final long DEFAULT_TTL_MILLISECONDS = 7L * 60L * 1000L;

    private MamUpnHintStore() {
        // Utility class.
    }

    private static boolean isEnabled() {
        return CommonFlightsManager.INSTANCE.getFlightsProvider()
                .isFlightEnabled(CommonFlight.ENABLE_BROKER_INSTALL_UPN_HINT);
    }

    /**
     * Remembers {@code upn}, but only when the interactive request actually failed because the
     * broker still needs to be installed. Any other failure leaves the store untouched.
     *
     * @param components             platform components of the failed request.
     * @param authorizationErrorCode the error code from the authorization error response.
     * @param upn                    the UPN the server returned on the broker-install redirect.
     */
    public static void saveUpnHintForBrokerInstall(@Nullable final IPlatformComponents components,
                                                   @Nullable final String authorizationErrorCode,
                                                   @Nullable final String upn) {
        if (!MicrosoftAuthorizationErrorResponse.BROKER_NEEDS_TO_BE_INSTALLED
                .equals(authorizationErrorCode)) {
            return;
        }
        saveUpnHint(components, upn);
    }

    /**
     * Stores {@code upn} with a {@link #DEFAULT_TTL_MILLISECONDS} expiry. No-op when the flight is
     * off, when {@code upn} is null/blank, or when the store cannot be opened.
     *
     * @param components platform components providing the storage.
     * @param upn        the UPN to remember.
     */
    public static void saveUpnHint(@Nullable final IPlatformComponents components,
                                   @Nullable final String upn) {
        final String methodTag = TAG + ":saveUpnHint";

        if (!isEnabled() || StringUtil.isNullOrEmpty(upn)) {
            return;
        }

        final INameValueStorage<String> storage = getStorage(components);
        if (storage == null) {
            return;
        }

        saveUpnHint(storage, upn, System.currentTimeMillis(), DEFAULT_TTL_MILLISECONDS);
        Logger.info(methodTag, "Stored a UPN hint for the broker-install flow; it is usable for "
                + (DEFAULT_TTL_MILLISECONDS / 1000L) + "s.");
    }

    /**
     * Storage-level write with an injectable clock and TTL.
     *
     * @param storage    the backing store.
     * @param upn        the UPN to remember.
     * @param nowMillis  the current time in epoch millis.
     * @param ttlMillis  how long the hint stays usable, in millis.
     */
    static void saveUpnHint(@NonNull final INameValueStorage<String> storage,
                            @NonNull final String upn,
                            final long nowMillis,
                            final long ttlMillis) {
        storage.put(KEY_EXPIRES_AT, String.valueOf(nowMillis + ttlMillis));
        storage.put(KEY_UPN, upn);
    }

    /**
     * Returns the stored UPN if - and only if - it is still within its TTL. An expired or
     * half-written record is deleted rather than returned.
     *
     * @param components platform components providing the storage.
     * @return the UPN to pre-fill, or {@code null} if there is no usable hint.
     */
    @Nullable
    public static String getValidUpnHint(@Nullable final IPlatformComponents components) {
        if (!isEnabled()) {
            return null;
        }

        final INameValueStorage<String> storage = getStorage(components);
        if (storage == null) {
            return null;
        }

        return getValidUpnHint(storage, System.currentTimeMillis());
    }

    /**
     * Storage-level read with an injectable clock.
     *
     * @param storage   the backing store.
     * @param nowMillis the current time in epoch millis.
     * @return the UPN if still valid, otherwise {@code null}.
     */
    @Nullable
    static String getValidUpnHint(@NonNull final INameValueStorage<String> storage,
                                  final long nowMillis) {
        final String methodTag = TAG + ":getValidUpnHint";

        final String upn = storage.get(KEY_UPN);
        final String expiresAtRaw = storage.get(KEY_EXPIRES_AT);

        if (StringUtil.isNullOrEmpty(upn) || StringUtil.isNullOrEmpty(expiresAtRaw)) {
            if (!StringUtil.isNullOrEmpty(upn) || !StringUtil.isNullOrEmpty(expiresAtRaw)) {
                // Only half of the record is present - e.g. the process died between the two
                // writes. Without a trustworthy expiry we must not hand the UPN out, so drop it.
                Logger.warn(methodTag, "Found an incomplete UPN hint; discarding it.");
                clearUpnHint(storage);
            }
            return null;
        }

        final long expiresAtMillis;
        try {
            expiresAtMillis = Long.parseLong(expiresAtRaw);
        } catch (final NumberFormatException e) {
            Logger.warn(methodTag, "Stored UPN hint expiry is not a number; discarding the hint.");
            clearUpnHint(storage);
            return null;
        }

        if (nowMillis >= expiresAtMillis) {
            Logger.info(methodTag, "The stored UPN hint has expired; discarding it.");
            clearUpnHint(storage);
            return null;
        }

        return upn;
    }

    /**
     * Deletes any stored UPN. Deliberately not flight-gated so that cleanup always works, including
     * after the flight has been turned off.
     *
     * @param components platform components providing the storage.
     */
    public static void clearUpnHint(@Nullable final IPlatformComponents components) {
        final INameValueStorage<String> storage = getStorage(components);
        if (storage != null) {
            clearUpnHint(storage);
        }
    }

    /**
     * Storage-level delete.
     *
     * @param storage the backing store.
     */
    static void clearUpnHint(@NonNull final INameValueStorage<String> storage) {
        storage.remove(KEY_UPN);
        storage.remove(KEY_EXPIRES_AT);
    }

    /**
     * Returns {@code parameters} with {@code login_hint} pre-filled from a still-valid stored UPN.
     * <p>
     * An explicit {@code login_hint} from the caller always wins - it is never overwritten - and the
     * original instance is returned untouched whenever there is nothing useful to add, so this is
     * safe to call on every interactive request.
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

        final String upn = getValidUpnHint(parameters.getPlatformComponents());
        if (StringUtil.isNullOrEmpty(upn)) {
            return parameters;
        }

        Logger.info(methodTag, "Pre-filling login_hint from the stored broker-install UPN hint.");
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
