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
package com.microsoft.identity.common.java.providers

import com.microsoft.identity.common.components.MockPlatformComponentsFactory
import com.microsoft.identity.common.java.authorities.Authority
import com.microsoft.identity.common.java.authorities.UnknownAuthority
import com.microsoft.identity.common.java.commands.parameters.InteractiveTokenCommandParameters
import com.microsoft.identity.common.java.flighting.CommonFlight
import com.microsoft.identity.common.java.flighting.CommonFlightsManager
import com.microsoft.identity.common.java.flighting.MockFlightsManager
import com.microsoft.identity.common.java.flighting.MockFlightsProvider
import com.microsoft.identity.common.java.interfaces.INameValueStorage
import com.microsoft.identity.common.java.interfaces.IPlatformComponents
import com.microsoft.identity.common.java.providers.microsoft.MicrosoftAuthorizationErrorResponse
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationErrorResponse
import com.microsoft.identity.common.java.providers.oauth2.OpenIdConnectPromptParameter
import com.microsoft.identity.common.java.util.ported.InMemoryStorage
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [MamUpnHintStore]: the TTL-bounded, per-client UPN hint that lets the interactive
 * request made after a MAM Conditional Access broker-install interruption pre-fill the address the
 * user already typed (Feature AB#3676213).
 */
class MamUpnHintStoreTest {

    // region storage-level read/write (injected clock)

    @Test
    fun save_thenReadWithinTtl_returnsUpn() {
        val storage: INameValueStorage<String> = InMemoryStorage()
        save(storage, CLIENT_ID, UPN, NOW)

        assertEquals(UPN, MamUpnHintStore.getValidUpnHint(storage, CLIENT_ID, NOW + TTL - 1, TTL))
    }

    @Test
    fun read_doesNotSpendTheHint_secondReadStillReturnsIt() {
        val storage: INameValueStorage<String> = InMemoryStorage()
        save(storage, CLIENT_ID, UPN, NOW)

        assertEquals(UPN, MamUpnHintStore.getValidUpnHint(storage, CLIENT_ID, NOW, TTL))
        assertEquals(
            "reading must not destroy a hint that has not been used yet",
            UPN, MamUpnHintStore.getValidUpnHint(storage, CLIENT_ID, NOW, TTL)
        )
    }

    /**
     * Regression: handling the install redirect finishes the authorization activity before the store
     * listing is launched, which resumes the caller's own account screen mid-flow. That screen reads
     * the hint. If the read spent it, the hint would be gone before the app restart it exists to
     * survive - which is exactly the failure this pins.
     */
    @Test
    fun read_whileStillInTheInstallHandoff_leavesTheHintForTheReadAfterTheRestart() {
        val storage: INameValueStorage<String> = InMemoryStorage()
        save(storage, CLIENT_ID, UPN, NOW)

        // The transient resume, a few tens of millis after the write.
        assertEquals(UPN, MamUpnHintStore.getValidUpnHint(storage, CLIENT_ID, NOW + 34L, TTL))

        // The read that actually matters, after the broker install killed and restarted the app.
        assertEquals(UPN, MamUpnHintStore.getValidUpnHint(storage, CLIENT_ID, NOW + 60_000L, TTL))
    }

    @Test
    fun read_exactlyAtTtl_isExpired() {
        val storage: INameValueStorage<String> = InMemoryStorage()
        save(storage, CLIENT_ID, UPN, NOW)

        assertNull(MamUpnHintStore.getValidUpnHint(storage, CLIENT_ID, NOW + TTL, TTL))
        assertRecordAbsent(storage, CLIENT_ID)
    }

    @Test
    fun read_afterTtl_returnsNullAndDeletesRecord() {
        val storage: INameValueStorage<String> = InMemoryStorage()
        save(storage, CLIENT_ID, UPN, NOW)

        assertNull(MamUpnHintStore.getValidUpnHint(storage, CLIENT_ID, NOW + TTL + 1, TTL))
        assertRecordAbsent(storage, CLIENT_ID)
    }

    @Test
    fun read_clockWentBackwards_returnsNullAndDeletesRecord() {
        val storage: INameValueStorage<String> = InMemoryStorage()
        save(storage, CLIENT_ID, UPN, NOW)

        assertNull(MamUpnHintStore.getValidUpnHint(storage, CLIENT_ID, NOW - 1, TTL))
        assertRecordAbsent(storage, CLIENT_ID)
    }

    /**
     * The other expiry tests all pass through the sweep, which is what normally deletes stale
     * records. This pins that the read validates the record it is about to return in its own right:
     * against a store that cannot enumerate itself the sweep sees nothing, and an expired UPN must
     * still not be handed out.
     */
    @Test
    fun read_whenTheStoreCannotEnumerateItself_stillEnforcesTheTtl() {
        val storage: INameValueStorage<String> = object : InMemoryStorage<String>() {
            override fun getAll(): Map<String, String> = emptyMap()
        }
        save(storage, CLIENT_ID, UPN, NOW)

        assertEquals(
            "a fresh hint is still readable",
            UPN, MamUpnHintStore.getValidUpnHint(storage, CLIENT_ID, NOW, TTL)
        )
        assertNull(
            "an expired hint must not be returned just because the sweep could not see it",
            MamUpnHintStore.getValidUpnHint(storage, CLIENT_ID, NOW + TTL, TTL)
        )
    }

    @Test
    fun read_unreadableRecord_returnsNullAndDeletesRecord() {
        val storage: INameValueStorage<String> = InMemoryStorage()
        // A record written by a future (or corrupted) build that this one cannot parse.
        storage.put(MamUpnHintStore.KEY_PREFIX_RECORD + CLIENT_ID, "not-a-record")

        assertNull(MamUpnHintStore.getValidUpnHint(storage, CLIENT_ID, NOW, TTL))
        assertRecordAbsent(storage, CLIENT_ID)
    }

    @Test
    fun read_recordWithNoTimestamp_returnsNullAndDeletesRecord() {
        val storage: INameValueStorage<String> = InMemoryStorage()
        // Parses, but carries no write time - its age cannot be judged, so it cannot be trusted.
        storage.put(MamUpnHintStore.KEY_PREFIX_RECORD + CLIENT_ID, "{\"upn\":\"$UPN\"}")

        assertNull(MamUpnHintStore.getValidUpnHint(storage, CLIENT_ID, NOW, TTL))
        assertRecordAbsent(storage, CLIENT_ID)
    }

    @Test
    fun read_missingRecord_returnsNull() {
        assertNull(MamUpnHintStore.getValidUpnHint(InMemoryStorage(), CLIENT_ID, NOW, TTL))
    }

    @Test
    fun save_overwritesPreviousHintForSameClient() {
        val storage: INameValueStorage<String> = InMemoryStorage()
        save(storage, CLIENT_ID, UPN, NOW)
        save(storage, CLIENT_ID, OTHER_UPN, NOW + 1)

        assertEquals(OTHER_UPN, MamUpnHintStore.getValidUpnHint(storage, CLIENT_ID, NOW + 1, TTL))
    }

    // endregion

    // region per-client keying

    @Test
    fun read_isScopedToClientId() {
        val storage: INameValueStorage<String> = InMemoryStorage()
        save(storage, CLIENT_ID, UPN, NOW)

        assertNull(
            "one client must not see another client's hint",
            MamUpnHintStore.getValidUpnHint(storage, OTHER_CLIENT_ID, NOW, TTL)
        )
        assertEquals(
            "and the owning client's hint must survive that read",
            UPN, MamUpnHintStore.getValidUpnHint(storage, CLIENT_ID, NOW, TTL)
        )
    }

    @Test
    fun save_twoClients_areStoredIndependently() {
        val storage: INameValueStorage<String> = InMemoryStorage()
        save(storage, CLIENT_ID, UPN, NOW)
        save(storage, OTHER_CLIENT_ID, OTHER_UPN, NOW)

        assertEquals(UPN, MamUpnHintStore.getValidUpnHint(storage, CLIENT_ID, NOW, TTL))
        assertEquals(
            OTHER_UPN,
            MamUpnHintStore.getValidUpnHint(storage, OTHER_CLIENT_ID, NOW, TTL)
        )
    }

    /**
     * A record is only ever written under a real client id, so a read or a clear that cannot supply
     * one has nothing to address. It must be a safe no-op rather than falling back to a shared key -
     * a shared key would let two callers that both failed client-id resolution see each other's hint.
     */
    @Test
    fun readAndClear_withoutAClientId_areSafeNoOps() {
        val storage: INameValueStorage<String> = InMemoryStorage()
        save(storage, CLIENT_ID, UPN, NOW)

        assertNull(MamUpnHintStore.getValidUpnHint(storage, null, NOW, TTL))
        assertNull(MamUpnHintStore.getValidUpnHint(storage, "", NOW, TTL))

        MamUpnHintStore.clearUpnHint(storage, null)
        MamUpnHintStore.clearUpnHint(storage, "")

        assertEquals(
            "another client's hint must be untouched",
            UPN, MamUpnHintStore.getValidUpnHint(storage, CLIENT_ID, NOW, TTL)
        )
    }

    @Test
    fun clear_removesOnlyTheGivenClientsRecord() {
        val storage: INameValueStorage<String> = InMemoryStorage()
        save(storage, CLIENT_ID, UPN, NOW)
        save(storage, OTHER_CLIENT_ID, OTHER_UPN, NOW)

        MamUpnHintStore.clearUpnHint(storage, CLIENT_ID)

        assertRecordAbsent(storage, CLIENT_ID)
        assertEquals(
            OTHER_UPN,
            MamUpnHintStore.getValidUpnHint(storage, OTHER_CLIENT_ID, NOW, TTL)
        )
    }

    // endregion

    // region sweep

    @Test
    fun read_sweepsExpiredRecordsBelongingToOtherClients() {
        val storage: INameValueStorage<String> = InMemoryStorage()
        save(storage, OTHER_CLIENT_ID, OTHER_UPN, NOW)
        save(storage, CLIENT_ID, UPN, NOW + TTL)

        // Reading our own fresh hint must also take out the other client's stale one.
        assertEquals(UPN, MamUpnHintStore.getValidUpnHint(storage, CLIENT_ID, NOW + TTL, TTL))
        assertRecordAbsent(storage, OTHER_CLIENT_ID)
    }

    @Test
    fun read_sweepsResidueLeftByAnEarlierFormat() {
        val storage: INameValueStorage<String> = InMemoryStorage()
        // Keys written by a build that split the record across two entries.
        storage.put("upn.$OTHER_CLIENT_ID", OTHER_UPN)
        storage.put("written_at.$OTHER_CLIENT_ID", NOW.toString())
        save(storage, CLIENT_ID, UPN, NOW)

        assertEquals(UPN, MamUpnHintStore.getValidUpnHint(storage, CLIENT_ID, NOW, TTL))
        assertNull(
            "anything that is not a readable record must be swept",
            storage.get("upn.$OTHER_CLIENT_ID")
        )
        assertNull(storage.get("written_at.$OTHER_CLIENT_ID"))
    }

    @Test
    fun read_leavesOtherClientsStillValidRecordsAlone() {
        val storage: INameValueStorage<String> = InMemoryStorage()
        save(storage, OTHER_CLIENT_ID, OTHER_UPN, NOW)
        save(storage, CLIENT_ID, UPN, NOW)

        assertEquals(UPN, MamUpnHintStore.getValidUpnHint(storage, CLIENT_ID, NOW, TTL))
        assertEquals(
            OTHER_UPN,
            MamUpnHintStore.getValidUpnHint(storage, OTHER_CLIENT_ID, NOW, TTL)
        )
    }

    // endregion

    // region MAM-CA marker gate

    @Test
    fun saveForMamCaInstall_markedRedirect_isStored() {
        setFlights(true)
        val components = components()

        MamUpnHintStore.saveUpnHintForMamCaInstall(components, CLIENT_ID, HOST, redirect(UPN, true))

        assertEquals(UPN, MamUpnHintStore.getValidUpnHint(components, CLIENT_ID))
    }

    @Test
    fun saveForMamCaInstall_unmarkedRedirect_isIgnored() {
        setFlights(true)
        val components = components()

        MamUpnHintStore.saveUpnHintForMamCaInstall(
            components, CLIENT_ID, HOST, redirect(UPN, false)
        )

        assertNull(
            "an ordinary device-registration install must not store a hint",
            MamUpnHintStore.getValidUpnHint(components, CLIENT_ID)
        )
    }

    @Test
    fun saveForMamCaInstall_flightOff_isIgnored() {
        setFlights(false)
        val components = components()

        MamUpnHintStore.saveUpnHintForMamCaInstall(components, CLIENT_ID, HOST, redirect(UPN, true))

        setFlights(true)
        assertNull(MamUpnHintStore.getValidUpnHint(components, CLIENT_ID))
    }

    @Test
    fun saveForMamCaInstall_errorResponse_brokerInstallAndMarked_isStored() {
        setFlights(true)
        val components = components()

        MamUpnHintStore.saveUpnHintForMamCaInstall(
            components, CLIENT_ID, authority(),
            errorResponse(
                MicrosoftAuthorizationErrorResponse.BROKER_NEEDS_TO_BE_INSTALLED, UPN, true
            )
        )

        assertEquals(UPN, MamUpnHintStore.getValidUpnHint(components, CLIENT_ID))
    }

    @Test
    fun saveForMamCaInstall_errorResponse_brokerInstallButUnmarked_isIgnored() {
        setFlights(true)
        val components = components()

        MamUpnHintStore.saveUpnHintForMamCaInstall(
            components, CLIENT_ID, authority(),
            errorResponse(
                MicrosoftAuthorizationErrorResponse.BROKER_NEEDS_TO_BE_INSTALLED, UPN, false
            )
        )

        assertNull(MamUpnHintStore.getValidUpnHint(components, CLIENT_ID))
    }

    @Test
    fun saveForMamCaInstall_errorResponse_otherError_isIgnored() {
        setFlights(true)
        val components = components()

        MamUpnHintStore.saveUpnHintForMamCaInstall(
            components, CLIENT_ID, authority(), errorResponse("some_other_error", UPN, true)
        )

        assertNull(MamUpnHintStore.getValidUpnHint(components, CLIENT_ID))
    }

    @Test
    fun saveForMamCaInstall_errorResponse_null_isIgnored() {
        setFlights(true)
        val components = components()

        MamUpnHintStore.saveUpnHintForMamCaInstall(
            components, CLIENT_ID, authority(), null as AuthorizationErrorResponse?
        )

        assertNull(MamUpnHintStore.getValidUpnHint(components, CLIENT_ID))
    }

    // endregion

    // region flighting

    @Test
    fun save_flightOff_storesNothing() {
        setFlights(false)
        val components = components()

        save(components, CLIENT_ID, UPN)

        setFlights(true)
        assertNull(MamUpnHintStore.getValidUpnHint(components, CLIENT_ID))
    }

    @Test
    fun read_flightOff_returnsNull() {
        setFlights(true)
        val components = components()
        save(components, CLIENT_ID, UPN)

        setFlights(false)

        assertNull(MamUpnHintStore.getValidUpnHint(components, CLIENT_ID))
    }

    /**
     * Turning the flight off is the kill switch, and expiry is driven entirely by reads. If the
     * flight gate also skipped the sweep, throwing that switch would strand every UPN already on
     * disk for good - the one action taken to reduce exposure would maximise it instead.
     */
    @Test
    fun read_flightOff_stillSweepsRecordsAlreadyOnDisk() {
        setFlights(true)
        val components = components()
        val storage = components.storageSupplier
            .getEncryptedNameValueStore(MamUpnHintStore.STORE_NAME, String::class.java)
        // Written far enough in the past that it is expired under any sane TTL.
        save(
            storage, CLIENT_ID, UPN, System.currentTimeMillis() - (10L * 60L * 1000L)
        )
        assertNotNull(storage.get(MamUpnHintStore.KEY_PREFIX_RECORD + CLIENT_ID))

        setFlights(false)
        MamUpnHintStore.getValidUpnHint(components, CLIENT_ID)

        assertNull(
            "an expired record must still be swept once the flight is off",
            storage.get(MamUpnHintStore.KEY_PREFIX_RECORD + CLIENT_ID)
        )
    }

    @Test
    fun clear_isNotGatedOnTheFlight() {
        setFlights(true)
        val components = components()
        save(components, CLIENT_ID, UPN)

        setFlights(false)
        MamUpnHintStore.clearUpnHint(components, CLIENT_ID)

        setFlights(true)
        assertNull(
            "cleanup must work even after the flight is turned off",
            MamUpnHintStore.getValidUpnHint(components, CLIENT_ID)
        )
    }

    @Test
    fun ttl_isReadFromTheFlight() {
        // Deliberately not the default, so this fails if the TTL stops honouring an ECS override.
        setFlights(true, 5)

        assertEquals(5000L, MamUpnHintStore.getTtlMillis())
    }

    /**
     * The three-minute window is the agreed default for this feature. It is tunable through ECS, so
     * this only guards the shipped default against drifting by accident.
     */
    @Test
    fun ttl_defaultsToThreeMinutes() {
        assertEquals(180, CommonFlight.MAM_CA_UPN_HINT_TTL_SECONDS.defaultValue)
    }

    @Test
    fun save_blankUpn_storesNothing() {
        setFlights(true)
        val components = components()

        save(components, CLIENT_ID, "   ")

        assertNull(MamUpnHintStore.getValidUpnHint(components, CLIENT_ID))
    }

    @Test
    fun save_nullComponents_isSwallowed() {
        setFlights(true)

        save(null as IPlatformComponents?, CLIENT_ID, UPN)
        assertNull(MamUpnHintStore.getValidUpnHint(null, CLIENT_ID))
        MamUpnHintStore.clearUpnHint(null as IPlatformComponents?, CLIENT_ID)
    }

    // endregion

    // region parameter pre-fill

    @Test
    fun apply_absentLoginHint_isFilledFromTheStore() {
        setFlights(true)
        val parameters = parameters(null)
        save(parameters.platformComponents, CLIENT_ID, UPN)

        val updated = MamUpnHintStore.applyStoredUpnHintIfAbsent(parameters)

        assertEquals(UPN, updated.loginHint)
        assertNull("the caller's parameters must not be mutated", parameters.loginHint)
    }

    @Test
    fun apply_callerSuppliedLoginHint_isNeverOverwritten() {
        setFlights(true)
        val parameters = parameters(OTHER_UPN)
        save(parameters.platformComponents, CLIENT_ID, UPN)

        val updated = MamUpnHintStore.applyStoredUpnHintIfAbsent(parameters)

        assertSame(parameters, updated)
        assertEquals(OTHER_UPN, updated.loginHint)
    }

    @Test
    fun apply_hintStoredByAnotherClient_isNotUsed() {
        setFlights(true)
        val parameters = parameters(null)
        save(parameters.platformComponents, OTHER_CLIENT_ID, UPN)

        assertSame(parameters, MamUpnHintStore.applyStoredUpnHintIfAbsent(parameters))
    }

    @Test
    fun apply_flightOff_returnsSameInstance() {
        setFlights(true)
        val parameters = parameters(null)
        save(parameters.platformComponents, CLIENT_ID, UPN)

        setFlights(false)

        assertSame(parameters, MamUpnHintStore.applyStoredUpnHintIfAbsent(parameters))
    }

    /**
     * The request an applied hint is attached to can still fail - a flaky network right after a
     * ~100 MB broker install is exactly the case this feature exists for. If applying spent the
     * hint, that failure would leave the user with the empty field the feature is meant to avoid,
     * with nothing left to recover from.
     */
    @Test
    fun apply_doesNotSpendTheHint_soARetryIsStillPreFilled() {
        setFlights(true)
        val first = parameters(null)
        save(first.platformComponents, CLIENT_ID, UPN)

        assertEquals(UPN, MamUpnHintStore.applyStoredUpnHintIfAbsent(first).loginHint)

        val retry = InteractiveTokenCommandParameters.builder()
            .platformComponents(first.platformComponents)
            .clientId(CLIENT_ID)
            .authority(authority())
            .build()
        assertEquals(
            "the hint must survive a failed request", UPN,
            MamUpnHintStore.applyStoredUpnHintIfAbsent(retry).loginHint
        )
    }

    /**
     * Setting `login_hint` is not cosmetic: `BaseController` drops the `prompt` when a hint is
     * present, so injecting a remembered address here would silently suppress the account picker the
     * caller explicitly asked for and sign the user into somebody else's account.
     */
    @Test
    fun apply_callerAskedForTheAccountPicker_hintIsNotInjected() {
        setFlights(true)
        val components = components()
        save(components, CLIENT_ID, UPN)

        val parameters = InteractiveTokenCommandParameters.builder()
            .platformComponents(components)
            .clientId(CLIENT_ID)
            .authority(authority())
            .prompt(OpenIdConnectPromptParameter.SELECT_ACCOUNT)
            .build()

        assertSame(
            "an explicit account picker must never be pre-answered",
            parameters, MamUpnHintStore.applyStoredUpnHintIfAbsent(parameters)
        )
        assertNull(MamUpnHintStore.applyStoredUpnHintIfAbsent(parameters).loginHint)
    }

    @Test
    fun apply_callerAskedToCreateAnAccount_hintIsNotInjected() {
        setFlights(true)
        val components = components()
        save(components, CLIENT_ID, UPN)

        val parameters = InteractiveTokenCommandParameters.builder()
            .platformComponents(components)
            .clientId(CLIENT_ID)
            .authority(authority())
            .prompt(OpenIdConnectPromptParameter.CREATE)
            .build()

        assertSame(parameters, MamUpnHintStore.applyStoredUpnHintIfAbsent(parameters))
    }

    @Test
    fun apply_preservesOtherParameters() {
        setFlights(true)
        val components = components()
        val parameters = InteractiveTokenCommandParameters.builder()
            .platformComponents(components)
            .clientId(CLIENT_ID)
            .authority(authority())
            .redirectUri("msauth://com.contoso.app/signature")
            .build()
        save(components, CLIENT_ID, UPN)

        val updated = MamUpnHintStore.applyStoredUpnHintIfAbsent(parameters)

        assertEquals(UPN, updated.loginHint)
        assertEquals(CLIENT_ID, updated.clientId)
        assertEquals("msauth://com.contoso.app/signature", updated.redirectUri)
        assertSame(components, updated.platformComponents)
    }

    // endregion

    // region authority binding

    /**
     * A UPN is only known to the authority that returned it. Sending it to a different one - most
     * sharply, a different sovereign cloud - would hand an address to a service that never had it,
     * so the hint is bound to the authority host it was captured against.
     */
    @Test
    fun apply_hintStoredAgainstAnotherAuthority_isNotUsed() {
        setFlights(true)
        val components = components()
        save(components, CLIENT_ID, UPN, OTHER_HOST)

        val parameters = InteractiveTokenCommandParameters.builder()
            .platformComponents(components)
            .clientId(CLIENT_ID)
            .authority(authority(HOST))
            .build()

        assertSame(
            "a hint must not cross an authority boundary",
            parameters, MamUpnHintStore.applyStoredUpnHintIfAbsent(parameters)
        )
    }

    /**
     * Declining is not the same as discarding. An unrelated request to another authority in the
     * middle of the install detour must not wipe the hint the user is coming back for; the TTL
     * already bounds how long it lives.
     */
    @Test
    fun apply_authorityMismatch_leavesTheHintForTheAuthorityThatOwnsIt() {
        setFlights(true)
        val components = components()
        save(components, CLIENT_ID, UPN, HOST)

        MamUpnHintStore.applyStoredUpnHintIfAbsent(
            InteractiveTokenCommandParameters.builder()
                .platformComponents(components)
                .clientId(CLIENT_ID)
                .authority(authority(OTHER_HOST))
                .build()
        )

        assertEquals(
            "a request to another authority must not consume the hint", UPN,
            MamUpnHintStore.applyStoredUpnHintIfAbsent(
                InteractiveTokenCommandParameters.builder()
                    .platformComponents(components)
                    .clientId(CLIENT_ID)
                    .authority(authority(HOST))
                    .build()
            ).loginHint
        )
    }

    /**
     * The capture happens against `/common` and the request that follows is usually against the
     * resolved tenant. Binding on the host - rather than the whole url - is what lets the hint
     * survive that, which is the entire flow this feature ships for.
     */
    @Test
    fun apply_sameHostDifferentTenant_isStillUsed() {
        setFlights(true)
        val components = components()
        save(components, CLIENT_ID, UPN, HOST)

        val parameters = InteractiveTokenCommandParameters.builder()
            .platformComponents(components)
            .clientId(CLIENT_ID)
            .authority(
                Authority.getAuthorityFromAuthorityUrl(
                    "https://$HOST/72f988bf-86f1-41af-91ab-2d7cd011db47"
                )
            )
            .build()

        assertEquals(UPN, MamUpnHintStore.applyStoredUpnHintIfAbsent(parameters).loginHint)
    }

    @Test
    fun apply_authorityHostComparisonIgnoresCase() {
        setFlights(true)
        val components = components()
        save(components, CLIENT_ID, UPN, HOST.uppercase())

        val parameters = InteractiveTokenCommandParameters.builder()
            .platformComponents(components)
            .clientId(CLIENT_ID)
            .authority(authority(HOST))
            .build()

        assertEquals(UPN, MamUpnHintStore.applyStoredUpnHintIfAbsent(parameters).loginHint)
    }

    @Test
    fun apply_requestHasNoAuthority_isNotUsed() {
        setFlights(true)
        val components = components()
        save(components, CLIENT_ID, UPN, HOST)

        val parameters = InteractiveTokenCommandParameters.builder()
            .platformComponents(components)
            .clientId(CLIENT_ID)
            .build()

        assertSame(parameters, MamUpnHintStore.applyStoredUpnHintIfAbsent(parameters))
    }

    /**
     * [UnknownAuthority.getAuthorityUri] throws by design. Failing to read a host must decline the
     * hint, not fail the sign-in it was only ever meant to make more convenient.
     */
    @Test
    fun apply_authorityThatCannotProduceAHost_isNotUsedAndDoesNotThrow() {
        setFlights(true)
        val components = components()
        save(components, CLIENT_ID, UPN, HOST)

        val parameters = InteractiveTokenCommandParameters.builder()
            .platformComponents(components)
            .clientId(CLIENT_ID)
            .authority(UnknownAuthority())
            .build()

        assertSame(parameters, MamUpnHintStore.applyStoredUpnHintIfAbsent(parameters))
    }

    /**
     * A record written without a host cannot be shown to belong to the authority being called, so
     * it is never put on the wire. It stays readable for the local, user-visible pre-fill, which
     * happens before there is any authority to check it against.
     */
    @Test
    fun apply_recordWithNoAuthorityHost_isNotUsedButStaysReadable() {
        setFlights(true)
        val components = components()
        save(components, CLIENT_ID, UPN, null)

        val parameters = InteractiveTokenCommandParameters.builder()
            .platformComponents(components)
            .clientId(CLIENT_ID)
            .authority(authority(HOST))
            .build()

        assertSame(parameters, MamUpnHintStore.applyStoredUpnHintIfAbsent(parameters))
        assertEquals(
            "the account screen pre-fill must still work",
            UPN, MamUpnHintStore.getValidUpnHint(components, CLIENT_ID)
        )
    }

    @Test
    fun saveForMamCaInstall_bindsTheHintToTheAuthorityOfTheRedirect() {
        setFlights(true)
        val components = components()

        MamUpnHintStore.saveUpnHintForMamCaInstall(
            components, CLIENT_ID, OTHER_HOST, redirect(UPN, true)
        )

        val parameters = InteractiveTokenCommandParameters.builder()
            .platformComponents(components)
            .clientId(CLIENT_ID)
            .authority(authority(HOST))
            .build()

        assertSame(parameters, MamUpnHintStore.applyStoredUpnHintIfAbsent(parameters))
    }

    // endregion

    // region round trip through a real (mock-backed) store

    @Test
    fun roundTrip_throughPlatformComponents_isScopedAndClearable() {
        setFlights(true)
        val components = components()

        save(components, CLIENT_ID, UPN)
        assertEquals(
            "the record must live under exactly one key",
            setOf(MamUpnHintStore.KEY_PREFIX_RECORD + CLIENT_ID),
            storageOf(components).keySet()
        )

        // Reads leave the record in place so it survives the restart the broker install can cause.
        assertEquals(UPN, MamUpnHintStore.getValidUpnHint(components, CLIENT_ID))
        assertTrue(
            "a read must not spend the hint",
            storageOf(components).keySet().contains(MamUpnHintStore.KEY_PREFIX_RECORD + CLIENT_ID)
        )

        // Nor does carrying it into a request; the request may still fail.
        assertEquals(
            UPN,
            MamUpnHintStore.applyStoredUpnHintIfAbsent(
                InteractiveTokenCommandParameters.builder()
                    .platformComponents(components)
                    .clientId(CLIENT_ID)
                    .authority(authority())
                    .build()
            ).loginHint
        )

        // Signing in successfully is what retires it - see the controllers' clear-on-success.
        MamUpnHintStore.clearUpnHint(components, CLIENT_ID)
        assertFalse(
            "the record must be gone once sign-in has succeeded",
            storageOf(components).keySet().contains(MamUpnHintStore.KEY_PREFIX_RECORD + CLIENT_ID)
        )
        assertNull(MamUpnHintStore.getValidUpnHint(components, CLIENT_ID))
    }

    @Test
    fun save_clientIdUnknown_storesNothing() {
        setFlights(true)
        val components = components()

        save(components, null, UPN)

        assertTrue(
            "a hint with no client id must not be pooled under a shared key",
            storageOf(components).keySet().isEmpty()
        )
    }

    // endregion

    private fun components(): IPlatformComponents =
        MockPlatformComponentsFactory.getNonFunctionalBuilder().build()

    /**
     * Storage-level write. Records are bound to an authority host, so tests that do not care which
     * one get [HOST]; the binding tests pass it explicitly.
     */
    private fun save(
        storage: INameValueStorage<String>,
        clientId: String,
        upn: String,
        nowMillis: Long,
        authorityHost: String? = HOST
    ) = MamUpnHintStore.saveUpnHint(storage, clientId, upn, authorityHost, nowMillis)

    private fun save(
        components: IPlatformComponents?,
        clientId: String?,
        upn: String?,
        authorityHost: String? = HOST
    ) = MamUpnHintStore.saveUpnHint(components, clientId, upn, authorityHost)

    /** An authority whose url parses; only its host matters here. */
    private fun authority(host: String = HOST): Authority =
        Authority.getAuthorityFromAuthorityUrl("https://$host/common")

    private fun storageOf(components: IPlatformComponents): INameValueStorage<String> {
        val storage = components.storageSupplier
            .getEncryptedNameValueStore(MamUpnHintStore.STORE_NAME, String::class.java)
        assertNotNull(storage)
        return storage
    }

    private fun parameters(loginHint: String?): InteractiveTokenCommandParameters =
        InteractiveTokenCommandParameters.builder()
            .platformComponents(components())
            .clientId(CLIENT_ID)
            .authority(authority())
            .loginHint(loginHint)
            .build()

    /** Builds the query parameters of a `msauth://wpj` broker-install redirect. */
    private fun redirect(upn: String, mamCaMarked: Boolean): Map<String, String> {
        val parameters = HashMap<String, String>()
        parameters[MamCaRedirect.KEY_USERNAME] = upn
        parameters["app_link"] = "https://play.google.com/store/apps/details?id=com.contoso.cp"
        if (mamCaMarked) {
            parameters[MamCaRedirect.KEY_INTUNE_APP_PROTECTION] =
                MamCaRedirect.VALUE_INTUNE_APP_PROTECTION_ENABLED
        }
        return parameters
    }

    private fun errorResponse(
        error: String,
        upn: String,
        mamCaInstall: Boolean
    ): AuthorizationErrorResponse {
        val response = AuthorizationErrorResponse(error, "description")
        response.setUpnToWpj(upn)
        response.setMamCaInstall(mamCaInstall)
        return response
    }

    private fun assertRecordAbsent(storage: INameValueStorage<String>, clientId: String) {
        assertNull(storage.get(MamUpnHintStore.KEY_PREFIX_RECORD + clientId))
    }

    /**
     * Sets the flight this store reads for one test.
     *
     * @param upnHintEnabled value of `ENABLE_MAM_CA_UPN_HINT`.
     */
    private fun setFlights(upnHintEnabled: Boolean) {
        val provider = MockFlightsProvider()
        provider.addFlight(
            CommonFlight.ENABLE_MAM_CA_UPN_HINT.key, upnHintEnabled.toString()
        )
        val manager = MockFlightsManager()
        manager.setMockBrokerFlightsProvider(provider)
        CommonFlightsManager.initializeCommonFlightsManager(manager)
    }

    private fun setFlights(upnHintEnabled: Boolean, ttlSeconds: Int) {
        val provider = MockFlightsProvider()
        provider.addFlight(
            CommonFlight.ENABLE_MAM_CA_UPN_HINT.key, upnHintEnabled.toString()
        )
        provider.addFlight(
            CommonFlight.MAM_CA_UPN_HINT_TTL_SECONDS.key, ttlSeconds.toString()
        )
        val manager = MockFlightsManager()
        manager.setMockBrokerFlightsProvider(provider)
        CommonFlightsManager.initializeCommonFlightsManager(manager)
    }

    @After
    fun tearDown() {
        CommonFlightsManager.resetFlightsManager()
    }

    companion object {
        private const val UPN = "user@contoso.onmicrosoft.com"
        private const val OTHER_UPN = "someone.else@contoso.onmicrosoft.com"
        private const val CLIENT_ID = "a-client-id"
        private const val OTHER_CLIENT_ID = "another-client-id"
        private const val NOW = 1_700_000_000_000L
        private const val TTL = 180_000L
        private const val HOST = "login.microsoftonline.com"

        /** A different sovereign cloud - the boundary the authority binding exists to hold. */
        private const val OTHER_HOST = "login.microsoftonline.us"
    }
}
