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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import com.microsoft.identity.common.components.MockPlatformComponentsFactory;
import com.microsoft.identity.common.java.commands.parameters.InteractiveTokenCommandParameters;
import com.microsoft.identity.common.java.flighting.CommonFlight;
import com.microsoft.identity.common.java.flighting.CommonFlightsManager;
import com.microsoft.identity.common.java.flighting.MockFlightsManager;
import com.microsoft.identity.common.java.flighting.MockFlightsProvider;
import com.microsoft.identity.common.java.interfaces.INameValueStorage;
import com.microsoft.identity.common.java.interfaces.IPlatformComponents;
import com.microsoft.identity.common.java.providers.microsoft.MicrosoftAuthorizationErrorResponse;
import com.microsoft.identity.common.java.util.ported.InMemoryStorage;

import org.junit.After;
import org.junit.Test;

/**
 * Unit tests for {@link MamUpnHintStore}: the TTL-bounded, self-cleaning UPN hint that lets an
 * interactive request made after a Conditional-Access broker-install interruption pre-fill the
 * address the user already typed (Feature AB#3676213).
 */
public class MamUpnHintStoreTest {

    private static final String UPN = "user@contoso.onmicrosoft.com";
    private static final String OTHER_UPN = "someone.else@contoso.onmicrosoft.com";
    private static final long NOW = 1_700_000_000_000L;

    // region storage-level read/write (injected clock)

    @Test
    public void save_thenGetWithinTtl_returnsUpn() {
        final INameValueStorage<String> storage = new InMemoryStorage<>();
        MamUpnHintStore.saveUpnHint(storage, UPN, NOW, MamUpnHintStore.DEFAULT_TTL_MILLISECONDS);

        assertEquals(UPN, MamUpnHintStore.getValidUpnHint(storage, NOW));
        assertEquals("a hint must stay usable right up to the last millisecond of its TTL",
                UPN,
                MamUpnHintStore.getValidUpnHint(
                        storage, NOW + MamUpnHintStore.DEFAULT_TTL_MILLISECONDS - 1));
    }

    @Test
    public void get_atExactExpiry_returnsNullAndSweepsRecord() {
        final INameValueStorage<String> storage = new InMemoryStorage<>();
        MamUpnHintStore.saveUpnHint(storage, UPN, NOW, MamUpnHintStore.DEFAULT_TTL_MILLISECONDS);

        assertNull("expiry is exclusive - at the expiry instant the hint is gone",
                MamUpnHintStore.getValidUpnHint(
                        storage, NOW + MamUpnHintStore.DEFAULT_TTL_MILLISECONDS));
        assertStoreIsEmpty(storage);
    }

    @Test
    public void get_afterExpiry_returnsNullAndSweepsRecord() {
        final INameValueStorage<String> storage = new InMemoryStorage<>();
        MamUpnHintStore.saveUpnHint(storage, UPN, NOW, MamUpnHintStore.DEFAULT_TTL_MILLISECONDS);

        assertNull(MamUpnHintStore.getValidUpnHint(
                storage, NOW + MamUpnHintStore.DEFAULT_TTL_MILLISECONDS + 1));
        assertStoreIsEmpty(storage);
    }

    @Test
    public void get_upnWithoutExpiry_returnsNullAndSweepsRecord() {
        // Simulates the process dying between the two writes: a UPN we cannot age out must not be
        // handed back, because it would then live forever.
        final INameValueStorage<String> storage = new InMemoryStorage<>();
        storage.put(MamUpnHintStore.KEY_UPN, UPN);

        assertNull(MamUpnHintStore.getValidUpnHint(storage, NOW));
        assertStoreIsEmpty(storage);
    }

    @Test
    public void get_expiryWithoutUpn_returnsNullAndSweepsRecord() {
        final INameValueStorage<String> storage = new InMemoryStorage<>();
        storage.put(MamUpnHintStore.KEY_EXPIRES_AT, String.valueOf(NOW + 1000L));

        assertNull(MamUpnHintStore.getValidUpnHint(storage, NOW));
        assertStoreIsEmpty(storage);
    }

    @Test
    public void get_corruptExpiry_returnsNullAndSweepsRecord() {
        final INameValueStorage<String> storage = new InMemoryStorage<>();
        storage.put(MamUpnHintStore.KEY_UPN, UPN);
        storage.put(MamUpnHintStore.KEY_EXPIRES_AT, "not-a-number");

        assertNull(MamUpnHintStore.getValidUpnHint(storage, NOW));
        assertStoreIsEmpty(storage);
    }

    @Test
    public void get_emptyStore_returnsNull() {
        assertNull(MamUpnHintStore.getValidUpnHint(new InMemoryStorage<String>(), NOW));
    }

    @Test
    public void save_overwritesPreviousHint() {
        final INameValueStorage<String> storage = new InMemoryStorage<>();
        MamUpnHintStore.saveUpnHint(storage, UPN, NOW, MamUpnHintStore.DEFAULT_TTL_MILLISECONDS);
        MamUpnHintStore.saveUpnHint(storage, OTHER_UPN, NOW, MamUpnHintStore.DEFAULT_TTL_MILLISECONDS);

        assertEquals(OTHER_UPN, MamUpnHintStore.getValidUpnHint(storage, NOW));
    }

    @Test
    public void clear_removesBothKeys() {
        final INameValueStorage<String> storage = new InMemoryStorage<>();
        MamUpnHintStore.saveUpnHint(storage, UPN, NOW, MamUpnHintStore.DEFAULT_TTL_MILLISECONDS);

        MamUpnHintStore.clearUpnHint(storage);

        assertStoreIsEmpty(storage);
        assertNull(MamUpnHintStore.getValidUpnHint(storage, NOW));
    }

    // endregion

    // region flight gate

    @Test
    public void save_flightOff_storesNothing() {
        setUpnHintFlight(false);
        final IPlatformComponents components = components();

        MamUpnHintStore.saveUpnHint(components, UPN);

        assertStoreIsEmpty(storageOf(components));
    }

    @Test
    public void saveAndGet_flightOn_roundTripsThroughPlatformStorage() {
        setUpnHintFlight(true);
        final IPlatformComponents components = components();

        MamUpnHintStore.saveUpnHint(components, UPN);

        assertEquals(UPN, MamUpnHintStore.getValidUpnHint(components));
    }

    @Test
    public void get_flightOff_neverReturnsAnExistingHint() {
        setUpnHintFlight(true);
        final IPlatformComponents components = components();
        MamUpnHintStore.saveUpnHint(components, UPN);

        setUpnHintFlight(false);

        assertNull(MamUpnHintStore.getValidUpnHint(components));
    }

    @Test
    public void clear_worksEvenWithFlightOff() {
        // Cleanup is deliberately ungated so a hint stored before the flight was turned off can
        // still be removed.
        setUpnHintFlight(true);
        final IPlatformComponents components = components();
        MamUpnHintStore.saveUpnHint(components, UPN);

        setUpnHintFlight(false);
        MamUpnHintStore.clearUpnHint(components);

        setUpnHintFlight(true);
        assertNull(MamUpnHintStore.getValidUpnHint(components));
    }

    @Test
    public void save_noFlightsManager_defaultsOffAndStoresNothing() {
        final IPlatformComponents components = components();

        MamUpnHintStore.saveUpnHint(components, UPN);

        assertStoreIsEmpty(storageOf(components));
    }

    // endregion

    // region null / blank tolerance

    @Test
    public void save_blankUpnOrNullComponents_isANoOp() {
        setUpnHintFlight(true);
        final IPlatformComponents components = components();

        MamUpnHintStore.saveUpnHint(components, null);
        MamUpnHintStore.saveUpnHint(components, "");
        MamUpnHintStore.saveUpnHint(null, UPN);

        assertStoreIsEmpty(storageOf(components));
    }

    @Test
    public void get_nullComponents_returnsNull() {
        setUpnHintFlight(true);
        assertNull(MamUpnHintStore.getValidUpnHint(null));
    }

    @Test
    public void clear_nullComponents_doesNotThrow() {
        MamUpnHintStore.clearUpnHint((IPlatformComponents) null);
    }

    // endregion

    // region saveUpnHintForBrokerInstall

    @Test
    public void saveForBrokerInstall_brokerInstallError_storesUpn() {
        setUpnHintFlight(true);
        final IPlatformComponents components = components();

        MamUpnHintStore.saveUpnHintForBrokerInstall(components,
                MicrosoftAuthorizationErrorResponse.BROKER_NEEDS_TO_BE_INSTALLED, UPN);

        assertEquals(UPN, MamUpnHintStore.getValidUpnHint(components));
    }

    @Test
    public void saveForBrokerInstall_anyOtherError_storesNothing() {
        setUpnHintFlight(true);
        final IPlatformComponents components = components();

        MamUpnHintStore.saveUpnHintForBrokerInstall(components, "access_denied", UPN);
        MamUpnHintStore.saveUpnHintForBrokerInstall(components, null, UPN);

        assertStoreIsEmpty(storageOf(components));
    }

    // endregion

    // region applyStoredUpnHintIfAbsent

    @Test
    public void apply_noStoredHint_returnsSameInstance() {
        setUpnHintFlight(true);
        final InteractiveTokenCommandParameters parameters = parameters(null);

        assertSame(parameters, MamUpnHintStore.applyStoredUpnHintIfAbsent(parameters));
    }

    @Test
    public void apply_storedHintAndNoLoginHint_prefillsLoginHint() {
        setUpnHintFlight(true);
        final InteractiveTokenCommandParameters parameters = parameters(null);
        MamUpnHintStore.saveUpnHint(parameters.getPlatformComponents(), UPN);

        final InteractiveTokenCommandParameters updated =
                MamUpnHintStore.applyStoredUpnHintIfAbsent(parameters);

        assertEquals(UPN, updated.getLoginHint());
        assertNull("the caller's parameters must not be mutated", parameters.getLoginHint());
    }

    @Test
    public void apply_callerSuppliedLoginHint_isNeverOverwritten() {
        setUpnHintFlight(true);
        final InteractiveTokenCommandParameters parameters = parameters(OTHER_UPN);
        MamUpnHintStore.saveUpnHint(parameters.getPlatformComponents(), UPN);

        final InteractiveTokenCommandParameters updated =
                MamUpnHintStore.applyStoredUpnHintIfAbsent(parameters);

        assertSame(parameters, updated);
        assertEquals(OTHER_UPN, updated.getLoginHint());
    }

    @Test
    public void apply_flightOff_returnsSameInstance() {
        setUpnHintFlight(true);
        final InteractiveTokenCommandParameters parameters = parameters(null);
        MamUpnHintStore.saveUpnHint(parameters.getPlatformComponents(), UPN);

        setUpnHintFlight(false);

        assertSame(parameters, MamUpnHintStore.applyStoredUpnHintIfAbsent(parameters));
    }

    @Test
    public void apply_preservesOtherParameters() {
        setUpnHintFlight(true);
        final IPlatformComponents components = components();
        final InteractiveTokenCommandParameters parameters = InteractiveTokenCommandParameters.builder()
                .platformComponents(components)
                .clientId("a-client-id")
                .redirectUri("msauth://com.contoso.app/signature")
                .build();
        MamUpnHintStore.saveUpnHint(components, UPN);

        final InteractiveTokenCommandParameters updated =
                MamUpnHintStore.applyStoredUpnHintIfAbsent(parameters);

        assertEquals(UPN, updated.getLoginHint());
        assertEquals("a-client-id", updated.getClientId());
        assertEquals("msauth://com.contoso.app/signature", updated.getRedirectUri());
        assertSame(components, updated.getPlatformComponents());
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
                .loginHint(loginHint)
                .build();
    }

    private static void assertStoreIsEmpty(final INameValueStorage<String> storage) {
        assertNull(storage.get(MamUpnHintStore.KEY_UPN));
        assertNull(storage.get(MamUpnHintStore.KEY_EXPIRES_AT));
    }

    /** Enables or disables the {@code ENABLE_BROKER_INSTALL_UPN_HINT} flight for one test. */
    private static void setUpnHintFlight(final boolean enabled) {
        final MockFlightsProvider provider = new MockFlightsProvider();
        provider.addFlight(CommonFlight.ENABLE_BROKER_INSTALL_UPN_HINT.getKey(),
                Boolean.toString(enabled));
        final MockFlightsManager manager = new MockFlightsManager();
        manager.setMockBrokerFlightsProvider(provider);
        CommonFlightsManager.INSTANCE.initializeCommonFlightsManager(manager);
    }

    @After
    public void tearDown() {
        CommonFlightsManager.INSTANCE.resetFlightsManager();
    }
}
