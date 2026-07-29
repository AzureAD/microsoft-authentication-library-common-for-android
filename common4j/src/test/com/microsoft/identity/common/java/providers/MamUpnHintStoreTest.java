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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.microsoft.identity.common.components.MockPlatformComponentsFactory;
import com.microsoft.identity.common.java.commands.parameters.InteractiveTokenCommandParameters;
import com.microsoft.identity.common.java.flighting.CommonFlight;
import com.microsoft.identity.common.java.flighting.CommonFlightsManager;
import com.microsoft.identity.common.java.flighting.MockFlightsManager;
import com.microsoft.identity.common.java.flighting.MockFlightsProvider;
import com.microsoft.identity.common.java.interfaces.INameValueStorage;
import com.microsoft.identity.common.java.interfaces.IPlatformComponents;
import com.microsoft.identity.common.java.providers.microsoft.MicrosoftAuthorizationErrorResponse;
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationErrorResponse;
import com.microsoft.identity.common.java.util.ported.InMemoryStorage;

import org.junit.After;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Unit tests for {@link MamUpnHintStore}: the TTL-bounded, single-use, per-client UPN hint that lets
 * the interactive request made after a MAM Conditional Access broker-install interruption pre-fill
 * the address the user already typed (Feature AB#3676213).
 */
public class MamUpnHintStoreTest {

    private static final String UPN = "user@contoso.onmicrosoft.com";
    private static final String OTHER_UPN = "someone.else@contoso.onmicrosoft.com";
    private static final String CLIENT_ID = "a-client-id";
    private static final String OTHER_CLIENT_ID = "another-client-id";
    private static final long NOW = 1_700_000_000_000L;
    private static final long TTL = 180_000L;

    // region storage-level read/write (injected clock)

    @Test
    public void save_thenReadWithinTtl_returnsUpn() {
        final INameValueStorage<String> storage = new InMemoryStorage<>();
        MamUpnHintStore.saveUpnHint(storage, CLIENT_ID, UPN, NOW);

        assertEquals(UPN, MamUpnHintStore.getValidUpnHint(storage, CLIENT_ID, NOW + TTL - 1, TTL));
    }

    @Test
    public void read_doesNotSpendTheHint_secondReadStillReturnsIt() {
        final INameValueStorage<String> storage = new InMemoryStorage<>();
        MamUpnHintStore.saveUpnHint(storage, CLIENT_ID, UPN, NOW);

        assertEquals(UPN, MamUpnHintStore.getValidUpnHint(storage, CLIENT_ID, NOW, TTL));
        assertEquals("reading must not destroy a hint that has not been used yet",
                UPN, MamUpnHintStore.getValidUpnHint(storage, CLIENT_ID, NOW, TTL));
    }

    /**
     * Regression: handling the install redirect finishes the authorization activity before the store
     * listing is launched, which resumes the caller's own account screen mid-flow. That screen reads
     * the hint. If the read spent it, the hint would be gone before the app restart it exists to
     * survive - which is exactly the failure this pins.
     */
    @Test
    public void read_whileStillInTheInstallHandoff_leavesTheHintForTheReadAfterTheRestart() {
        final INameValueStorage<String> storage = new InMemoryStorage<>();
        MamUpnHintStore.saveUpnHint(storage, CLIENT_ID, UPN, NOW);

        // The transient resume, a few tens of millis after the write.
        assertEquals(UPN, MamUpnHintStore.getValidUpnHint(storage, CLIENT_ID, NOW + 34L, TTL));

        // The read that actually matters, after the broker install killed and restarted the app.
        assertEquals(UPN, MamUpnHintStore.getValidUpnHint(storage, CLIENT_ID, NOW + 60_000L, TTL));
    }

    @Test
    public void read_exactlyAtTtl_isExpired() {
        final INameValueStorage<String> storage = new InMemoryStorage<>();
        MamUpnHintStore.saveUpnHint(storage, CLIENT_ID, UPN, NOW);

        assertNull(MamUpnHintStore.getValidUpnHint(storage, CLIENT_ID, NOW + TTL, TTL));
        assertRecordAbsent(storage, CLIENT_ID);
    }

    @Test
    public void read_afterTtl_returnsNullAndDeletesRecord() {
        final INameValueStorage<String> storage = new InMemoryStorage<>();
        MamUpnHintStore.saveUpnHint(storage, CLIENT_ID, UPN, NOW);

        assertNull(MamUpnHintStore.getValidUpnHint(storage, CLIENT_ID, NOW + TTL + 1, TTL));
        assertRecordAbsent(storage, CLIENT_ID);
    }

    @Test
    public void read_clockWentBackwards_returnsNullAndDeletesRecord() {
        final INameValueStorage<String> storage = new InMemoryStorage<>();
        MamUpnHintStore.saveUpnHint(storage, CLIENT_ID, UPN, NOW);

        assertNull(MamUpnHintStore.getValidUpnHint(storage, CLIENT_ID, NOW - 1, TTL));
        assertRecordAbsent(storage, CLIENT_ID);
    }

    /**
     * The other expiry tests all pass through the sweep, which is what normally deletes stale
     * records. This pins that the read validates the record it is about to return in its own right:
     * against a store that cannot enumerate itself the sweep sees nothing, and an expired UPN must
     * still not be handed out.
     */
    @Test
    public void read_whenTheStoreCannotEnumerateItself_stillEnforcesTheTtl() {
        final INameValueStorage<String> storage = new InMemoryStorage<String>() {
            @Override
            public Map<String, String> getAll() {
                return Collections.emptyMap();
            }
        };
        MamUpnHintStore.saveUpnHint(storage, CLIENT_ID, UPN, NOW);

        assertEquals("a fresh hint is still readable",
                UPN, MamUpnHintStore.getValidUpnHint(storage, CLIENT_ID, NOW, TTL));
        assertNull("an expired hint must not be returned just because the sweep could not see it",
                MamUpnHintStore.getValidUpnHint(storage, CLIENT_ID, NOW + TTL, TTL));
    }

    @Test
    public void read_halfWrittenRecord_returnsNullAndDeletesRecord() {
        final INameValueStorage<String> storage = new InMemoryStorage<>();
        // The two keys are written non-atomically, so a crash in between can leave just the UPN.
        storage.put(MamUpnHintStore.KEY_PREFIX_UPN + CLIENT_ID, UPN);

        assertNull(MamUpnHintStore.getValidUpnHint(storage, CLIENT_ID, NOW, TTL));
        assertRecordAbsent(storage, CLIENT_ID);
    }

    @Test
    public void read_nonNumericWrittenAt_returnsNullAndDeletesRecord() {
        final INameValueStorage<String> storage = new InMemoryStorage<>();
        storage.put(MamUpnHintStore.KEY_PREFIX_UPN + CLIENT_ID, UPN);
        storage.put(MamUpnHintStore.KEY_PREFIX_WRITTEN_AT + CLIENT_ID, "not-a-number");

        assertNull(MamUpnHintStore.getValidUpnHint(storage, CLIENT_ID, NOW, TTL));
        assertRecordAbsent(storage, CLIENT_ID);
    }

    @Test
    public void read_missingRecord_returnsNull() {
        assertNull(MamUpnHintStore.getValidUpnHint(new InMemoryStorage<String>(), CLIENT_ID, NOW, TTL));
    }

    @Test
    public void save_overwritesPreviousHintForSameClient() {
        final INameValueStorage<String> storage = new InMemoryStorage<>();
        MamUpnHintStore.saveUpnHint(storage, CLIENT_ID, UPN, NOW);
        MamUpnHintStore.saveUpnHint(storage, CLIENT_ID, OTHER_UPN, NOW + 1);

        assertEquals(OTHER_UPN, MamUpnHintStore.getValidUpnHint(storage, CLIENT_ID, NOW + 1, TTL));
    }

    // endregion

    // region per-client keying

    @Test
    public void read_isScopedToClientId() {
        final INameValueStorage<String> storage = new InMemoryStorage<>();
        MamUpnHintStore.saveUpnHint(storage, CLIENT_ID, UPN, NOW);

        assertNull("one client must not see another client's hint",
                MamUpnHintStore.getValidUpnHint(storage, OTHER_CLIENT_ID, NOW, TTL));
        assertEquals("and the owning client's hint must survive that read",
                UPN, MamUpnHintStore.getValidUpnHint(storage, CLIENT_ID, NOW, TTL));
    }

    @Test
    public void save_twoClients_areStoredIndependently() {
        final INameValueStorage<String> storage = new InMemoryStorage<>();
        MamUpnHintStore.saveUpnHint(storage, CLIENT_ID, UPN, NOW);
        MamUpnHintStore.saveUpnHint(storage, OTHER_CLIENT_ID, OTHER_UPN, NOW);

        assertEquals(UPN, MamUpnHintStore.getValidUpnHint(storage, CLIENT_ID, NOW, TTL));
        assertEquals(OTHER_UPN, MamUpnHintStore.getValidUpnHint(storage, OTHER_CLIENT_ID, NOW, TTL));
    }

    @Test
    public void save_nullClientId_isKeyedConsistently() {
        final INameValueStorage<String> storage = new InMemoryStorage<>();
        MamUpnHintStore.saveUpnHint(storage, null, UPN, NOW);

        assertNotNull("a null client id must fall back to a stable key",
                storage.get(MamUpnHintStore.KEY_PREFIX_UPN + MamUpnHintStore.UNKNOWN_CLIENT_ID));
        assertEquals(UPN, MamUpnHintStore.getValidUpnHint(storage, null, NOW, TTL));
    }

    @Test
    public void clear_removesOnlyTheGivenClientsRecord() {
        final INameValueStorage<String> storage = new InMemoryStorage<>();
        MamUpnHintStore.saveUpnHint(storage, CLIENT_ID, UPN, NOW);
        MamUpnHintStore.saveUpnHint(storage, OTHER_CLIENT_ID, OTHER_UPN, NOW);

        MamUpnHintStore.clearUpnHint(storage, CLIENT_ID);

        assertRecordAbsent(storage, CLIENT_ID);
        assertEquals(OTHER_UPN, MamUpnHintStore.getValidUpnHint(storage, OTHER_CLIENT_ID, NOW, TTL));
    }

    // endregion

    // region sweep

    @Test
    public void read_sweepsExpiredRecordsBelongingToOtherClients() {
        final INameValueStorage<String> storage = new InMemoryStorage<>();
        MamUpnHintStore.saveUpnHint(storage, OTHER_CLIENT_ID, OTHER_UPN, NOW);
        MamUpnHintStore.saveUpnHint(storage, CLIENT_ID, UPN, NOW + TTL);

        // Reading our own fresh hint must also take out the other client's stale one.
        assertEquals(UPN, MamUpnHintStore.getValidUpnHint(storage, CLIENT_ID, NOW + TTL, TTL));
        assertRecordAbsent(storage, OTHER_CLIENT_ID);
    }

    @Test
    public void read_sweepsHalfWrittenRecordsBelongingToOtherClients() {
        final INameValueStorage<String> storage = new InMemoryStorage<>();
        // A dangling timestamp with no UPN - the other half of a torn write.
        storage.put(MamUpnHintStore.KEY_PREFIX_WRITTEN_AT + OTHER_CLIENT_ID, Long.toString(NOW));
        MamUpnHintStore.saveUpnHint(storage, CLIENT_ID, UPN, NOW);

        assertEquals(UPN, MamUpnHintStore.getValidUpnHint(storage, CLIENT_ID, NOW, TTL));
        assertRecordAbsent(storage, OTHER_CLIENT_ID);
    }

    @Test
    public void read_leavesOtherClientsStillValidRecordsAlone() {
        final INameValueStorage<String> storage = new InMemoryStorage<>();
        MamUpnHintStore.saveUpnHint(storage, OTHER_CLIENT_ID, OTHER_UPN, NOW);
        MamUpnHintStore.saveUpnHint(storage, CLIENT_ID, UPN, NOW);

        assertEquals(UPN, MamUpnHintStore.getValidUpnHint(storage, CLIENT_ID, NOW, TTL));
        assertEquals(OTHER_UPN, MamUpnHintStore.getValidUpnHint(storage, OTHER_CLIENT_ID, NOW, TTL));
    }

    // endregion

    // region MAM-CA marker gate

    @Test
    public void saveForMamCaInstall_markedRedirect_isStored() {
        setFlights(true);
        final IPlatformComponents components = components();

        MamUpnHintStore.saveUpnHintForMamCaInstall(components, CLIENT_ID, redirect(UPN, true));

        assertEquals(UPN, MamUpnHintStore.getValidUpnHint(components, CLIENT_ID));
    }

    @Test
    public void saveForMamCaInstall_unmarkedRedirect_isIgnored() {
        setFlights(true);
        final IPlatformComponents components = components();

        MamUpnHintStore.saveUpnHintForMamCaInstall(components, CLIENT_ID, redirect(UPN, false));

        assertNull("an ordinary device-registration install must not store a hint",
                MamUpnHintStore.getValidUpnHint(components, CLIENT_ID));
    }

    @Test
    public void saveForMamCaInstall_flightOff_isIgnored() {
        setFlights(false);
        final IPlatformComponents components = components();

        MamUpnHintStore.saveUpnHintForMamCaInstall(components, CLIENT_ID, redirect(UPN, true));

        setFlights(true);
        assertNull(MamUpnHintStore.getValidUpnHint(components, CLIENT_ID));
    }

    @Test
    public void saveForMamCaInstall_errorResponse_brokerInstallAndMarked_isStored() {
        setFlights(true);
        final IPlatformComponents components = components();

        MamUpnHintStore.saveUpnHintForMamCaInstall(components, CLIENT_ID,
                errorResponse(MicrosoftAuthorizationErrorResponse.BROKER_NEEDS_TO_BE_INSTALLED, UPN, true));

        assertEquals(UPN, MamUpnHintStore.getValidUpnHint(components, CLIENT_ID));
    }

    @Test
    public void saveForMamCaInstall_errorResponse_brokerInstallButUnmarked_isIgnored() {
        setFlights(true);
        final IPlatformComponents components = components();

        MamUpnHintStore.saveUpnHintForMamCaInstall(components, CLIENT_ID,
                errorResponse(MicrosoftAuthorizationErrorResponse.BROKER_NEEDS_TO_BE_INSTALLED, UPN, false));

        assertNull(MamUpnHintStore.getValidUpnHint(components, CLIENT_ID));
    }

    @Test
    public void saveForMamCaInstall_errorResponse_otherError_isIgnored() {
        setFlights(true);
        final IPlatformComponents components = components();

        MamUpnHintStore.saveUpnHintForMamCaInstall(components, CLIENT_ID,
                errorResponse("some_other_error", UPN, true));

        assertNull(MamUpnHintStore.getValidUpnHint(components, CLIENT_ID));
    }

    @Test
    public void saveForMamCaInstall_errorResponse_null_isIgnored() {
        setFlights(true);
        final IPlatformComponents components = components();

        MamUpnHintStore.saveUpnHintForMamCaInstall(components, CLIENT_ID, (AuthorizationErrorResponse) null);

        assertNull(MamUpnHintStore.getValidUpnHint(components, CLIENT_ID));
    }

    // endregion

    // region flighting

    @Test
    public void save_flightOff_storesNothing() {
        setFlights(false);
        final IPlatformComponents components = components();

        MamUpnHintStore.saveUpnHint(components, CLIENT_ID, UPN);

        setFlights(true);
        assertNull(MamUpnHintStore.getValidUpnHint(components, CLIENT_ID));
    }

    @Test
    public void read_flightOff_returnsNull() {
        setFlights(true);
        final IPlatformComponents components = components();
        MamUpnHintStore.saveUpnHint(components, CLIENT_ID, UPN);

        setFlights(false);

        assertNull(MamUpnHintStore.getValidUpnHint(components, CLIENT_ID));
    }

    @Test
    public void clear_isNotGatedOnTheFlight() {
        setFlights(true);
        final IPlatformComponents components = components();
        MamUpnHintStore.saveUpnHint(components, CLIENT_ID, UPN);

        setFlights(false);
        MamUpnHintStore.clearUpnHint(components, CLIENT_ID);

        setFlights(true);
        assertNull("cleanup must work even after the flight is turned off",
                MamUpnHintStore.getValidUpnHint(components, CLIENT_ID));
    }

    @Test
    public void ttl_isReadFromTheFlight() {
        setFlights(true);

        assertEquals(1000L * (Integer) CommonFlight.MAM_CA_UPN_HINT_TTL_SECONDS.getDefaultValue(),
                MamUpnHintStore.getTtlMillis());
    }

    /**
     * The three-minute window is the agreed default for this feature. It is tunable through ECS, so
     * this only guards the shipped default against drifting by accident.
     */
    @Test
    public void ttl_defaultsToThreeMinutes() {
        assertEquals(180, CommonFlight.MAM_CA_UPN_HINT_TTL_SECONDS.getDefaultValue());
    }

    @Test
    public void save_blankUpn_storesNothing() {
        setFlights(true);
        final IPlatformComponents components = components();

        MamUpnHintStore.saveUpnHint(components, CLIENT_ID, "   ");

        assertNull(MamUpnHintStore.getValidUpnHint(components, CLIENT_ID));
    }

    @Test
    public void save_nullComponents_isSwallowed() {
        setFlights(true);

        MamUpnHintStore.saveUpnHint(null, CLIENT_ID, UPN);
        assertNull(MamUpnHintStore.getValidUpnHint(null, CLIENT_ID));
        MamUpnHintStore.clearUpnHint((IPlatformComponents) null, CLIENT_ID);
    }

    // endregion

    // region parameter pre-fill

    @Test
    public void apply_absentLoginHint_isFilledFromTheStore() {
        setFlights(true);
        final InteractiveTokenCommandParameters parameters = parameters(null);
        MamUpnHintStore.saveUpnHint(parameters.getPlatformComponents(), CLIENT_ID, UPN);

        final InteractiveTokenCommandParameters updated =
                MamUpnHintStore.applyStoredUpnHintIfAbsent(parameters);

        assertEquals(UPN, updated.getLoginHint());
        assertNull("the caller's parameters must not be mutated", parameters.getLoginHint());
    }

    @Test
    public void apply_callerSuppliedLoginHint_isNeverOverwritten() {
        setFlights(true);
        final InteractiveTokenCommandParameters parameters = parameters(OTHER_UPN);
        MamUpnHintStore.saveUpnHint(parameters.getPlatformComponents(), CLIENT_ID, UPN);

        final InteractiveTokenCommandParameters updated =
                MamUpnHintStore.applyStoredUpnHintIfAbsent(parameters);

        assertSame(parameters, updated);
        assertEquals(OTHER_UPN, updated.getLoginHint());
    }

    @Test
    public void apply_hintStoredByAnotherClient_isNotUsed() {
        setFlights(true);
        final InteractiveTokenCommandParameters parameters = parameters(null);
        MamUpnHintStore.saveUpnHint(parameters.getPlatformComponents(), OTHER_CLIENT_ID, UPN);

        assertSame(parameters, MamUpnHintStore.applyStoredUpnHintIfAbsent(parameters));
    }

    @Test
    public void apply_flightOff_returnsSameInstance() {
        setFlights(true);
        final InteractiveTokenCommandParameters parameters = parameters(null);
        MamUpnHintStore.saveUpnHint(parameters.getPlatformComponents(), CLIENT_ID, UPN);

        setFlights(false);

        assertSame(parameters, MamUpnHintStore.applyStoredUpnHintIfAbsent(parameters));
    }

    @Test
    public void apply_isSingleUse_secondRequestIsNotPreFilled() {
        setFlights(true);
        final InteractiveTokenCommandParameters first = parameters(null);
        MamUpnHintStore.saveUpnHint(first.getPlatformComponents(), CLIENT_ID, UPN);

        assertEquals(UPN, MamUpnHintStore.applyStoredUpnHintIfAbsent(first).getLoginHint());

        final InteractiveTokenCommandParameters second = InteractiveTokenCommandParameters.builder()
                .platformComponents(first.getPlatformComponents())
                .clientId(CLIENT_ID)
                .build();
        assertSame(second, MamUpnHintStore.applyStoredUpnHintIfAbsent(second));
    }

    @Test
    public void apply_preservesOtherParameters() {
        setFlights(true);
        final IPlatformComponents components = components();
        final InteractiveTokenCommandParameters parameters = InteractiveTokenCommandParameters.builder()
                .platformComponents(components)
                .clientId(CLIENT_ID)
                .redirectUri("msauth://com.contoso.app/signature")
                .build();
        MamUpnHintStore.saveUpnHint(components, CLIENT_ID, UPN);

        final InteractiveTokenCommandParameters updated =
                MamUpnHintStore.applyStoredUpnHintIfAbsent(parameters);

        assertEquals(UPN, updated.getLoginHint());
        assertEquals(CLIENT_ID, updated.getClientId());
        assertEquals("msauth://com.contoso.app/signature", updated.getRedirectUri());
        assertSame(components, updated.getPlatformComponents());
    }

    // endregion

    // region round trip through a real (mock-backed) store

    @Test
    public void roundTrip_throughPlatformComponents_isSpentOnUseAndScoped() {
        setFlights(true);
        final IPlatformComponents components = components();

        MamUpnHintStore.saveUpnHint(components, CLIENT_ID, UPN);
        assertTrue(storageOf(components).keySet().contains(MamUpnHintStore.KEY_PREFIX_UPN + CLIENT_ID));

        // Reads leave the record in place so it survives the restart the broker install can cause.
        assertEquals(UPN, MamUpnHintStore.getValidUpnHint(components, CLIENT_ID));
        assertTrue("a read must not spend the hint",
                storageOf(components).keySet().contains(MamUpnHintStore.KEY_PREFIX_UPN + CLIENT_ID));

        // Carrying it into a request is what retires it.
        assertEquals(UPN, MamUpnHintStore.applyStoredUpnHintIfAbsent(
                InteractiveTokenCommandParameters.builder()
                        .platformComponents(components)
                        .clientId(CLIENT_ID)
                        .build()).getLoginHint());
        assertFalse("the record must be gone once it has been used",
                storageOf(components).keySet().contains(MamUpnHintStore.KEY_PREFIX_UPN + CLIENT_ID));
        assertNull(MamUpnHintStore.getValidUpnHint(components, CLIENT_ID));
    }

    // endregion

    private static IPlatformComponents components() {
        return MockPlatformComponentsFactory.getNonFunctionalBuilder().build();
    }

    private static INameValueStorage<String> storageOf(final IPlatformComponents components) {
        final INameValueStorage<String> storage = components.getStorageSupplier()
                .getEncryptedNameValueStore(MamUpnHintStore.STORE_NAME, String.class);
        assertNotNull(storage);
        return storage;
    }

    private static InteractiveTokenCommandParameters parameters(final String loginHint) {
        return InteractiveTokenCommandParameters.builder()
                .platformComponents(components())
                .clientId(CLIENT_ID)
                .loginHint(loginHint)
                .build();
    }

    /** Builds the query parameters of a {@code msauth://wpj} broker-install redirect. */
    private static Map<String, String> redirect(final String upn, final boolean mamCaMarked) {
        final Map<String, String> parameters = new HashMap<>();
        parameters.put(MamCaRedirect.KEY_USERNAME, upn);
        parameters.put("app_link", "https://play.google.com/store/apps/details?id=com.contoso.cp");
        if (mamCaMarked) {
            parameters.put(MamCaRedirect.KEY_INTUNE_APP_PROTECTION,
                    MamCaRedirect.VALUE_INTUNE_APP_PROTECTION_ENABLED);
        }
        return parameters;
    }

    private static AuthorizationErrorResponse errorResponse(final String error,
                                                            final String upn,
                                                            final boolean mamCaInstall) {
        final AuthorizationErrorResponse response = new AuthorizationErrorResponse(error, "description");
        response.setUpnToWpj(upn);
        response.setMamCaInstall(mamCaInstall);
        return response;
    }

    private static void assertRecordAbsent(final INameValueStorage<String> storage,
                                           final String clientId) {
        assertNull(storage.get(MamUpnHintStore.KEY_PREFIX_UPN + clientId));
        assertNull(storage.get(MamUpnHintStore.KEY_PREFIX_WRITTEN_AT + clientId));
    }

    /**
     * Sets the flight this store reads for one test.
     *
     * @param upnHintEnabled value of {@code ENABLE_MAM_CA_UPN_HINT}.
     */
    private static void setFlights(final boolean upnHintEnabled) {
        final MockFlightsProvider provider = new MockFlightsProvider();
        provider.addFlight(CommonFlight.ENABLE_MAM_CA_UPN_HINT.getKey(),
                Boolean.toString(upnHintEnabled));
        final MockFlightsManager manager = new MockFlightsManager();
        manager.setMockBrokerFlightsProvider(provider);
        CommonFlightsManager.INSTANCE.initializeCommonFlightsManager(manager);
    }

    @After
    public void tearDown() {
        CommonFlightsManager.INSTANCE.resetFlightsManager();
    }
}
