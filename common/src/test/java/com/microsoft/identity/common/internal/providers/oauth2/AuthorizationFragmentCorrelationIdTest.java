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
package com.microsoft.identity.common.internal.providers.oauth2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import android.content.Context;
import android.os.Bundle;

import androidx.test.core.app.ApplicationProvider;

import com.microsoft.identity.common.internal.telemetry.OnboardingRecorderRegistry;
import com.microsoft.identity.common.internal.telemetry.OnboardingTelemetryRecorder;
import com.microsoft.identity.common.java.logging.DiagnosticContext;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/**
 * AB#3708195: covers the correlation-id half of the onboarding recorder handoff — the wiring
 * between {@link AuthorizationFragment#extractState(Bundle)}, the recorder registry, and the state
 * bundle.
 *
 * <p>The registry's own suite proves storage in isolation; these tests prove the fragment side
 * actually reaches it, including across activity recreation. Without them a regression in the
 * correlation-id plumbing would leave every WebView onboarding hook inert while the registry tests
 * still passed — the exact "green tests, dead feature" state this PR exists to fix.
 *
 * <p>Follows the {@code AuthorizationFragmentUrlTrackingTest} pattern: a minimal concrete subclass
 * exercising the base-class behaviour directly, rather than driving a full Fragment lifecycle.
 */
@RunWith(RobolectricTestRunner.class)
public class AuthorizationFragmentCorrelationIdTest {

    private static final String CORRELATION_ID = "e4f1a0c2-0000-4a1b-9f3e-000000000001";

    /** Minimal concrete subclass; the behaviour under test all lives in the base class. */
    private static class TestAuthorizationFragment extends AuthorizationFragment {
        String correlationId() {
            return mCorrelationId;
        }
    }

    private TestAuthorizationFragment mFragment;

    @Before
    public void setUp() {
        mFragment = new TestAuthorizationFragment();
        OnboardingRecorderRegistry.clearForTest();
    }

    @After
    public void tearDown() {
        OnboardingRecorderRegistry.clearForTest();
    }

    @Test
    public void testExtractState_CapturesCorrelationIdFromBundle() {
        final Bundle state = new Bundle();
        state.putString(DiagnosticContext.CORRELATION_ID, CORRELATION_ID);

        mFragment.extractState(state);

        assertEquals(CORRELATION_ID, mFragment.correlationId());
    }

    @Test
    public void testExtractedCorrelationId_ResolvesTheRegisteredRecorder() {
        // The whole point of capturing the id: it has to be the key the owner registered under.
        final OnboardingTelemetryRecorder recorder = newRecorder();
        OnboardingRecorderRegistry.register(CORRELATION_ID, recorder);

        final Bundle state = new Bundle();
        state.putString(DiagnosticContext.CORRELATION_ID, CORRELATION_ID);
        mFragment.extractState(state);

        assertSame(recorder, OnboardingRecorderRegistry.get(mFragment.correlationId()));
    }

    @Test
    public void testCorrelationId_SurvivesSaveAndRestore() {
        // The regression this guards: onSaveInstanceState originally did not round-trip
        // CORRELATION_ID, so a recreated fragment (a config change AuthorizationActivity does not
        // declare, e.g. uiMode) read null back, blanked its diagnostic context, and could no longer
        // resolve its recorder. The Intent survives recreation but this fragment reads the bundle.
        final OnboardingTelemetryRecorder recorder = newRecorder();
        OnboardingRecorderRegistry.register(CORRELATION_ID, recorder);

        final Bundle initialState = new Bundle();
        initialState.putString(DiagnosticContext.CORRELATION_ID, CORRELATION_ID);
        mFragment.extractState(initialState);

        // Activity recreation: the framework hands back only what onSaveInstanceState wrote.
        final Bundle savedState = new Bundle();
        mFragment.onSaveInstanceState(savedState);

        final TestAuthorizationFragment recreated = new TestAuthorizationFragment();
        recreated.extractState(savedState);

        assertEquals("the correlation id must round-trip through the saved bundle",
                CORRELATION_ID, recreated.correlationId());
        assertNotNull("the recorder must still be resolvable after recreation",
                OnboardingRecorderRegistry.get(recreated.correlationId()));
        assertSame(recorder, OnboardingRecorderRegistry.get(recreated.correlationId()));
    }

    @Test
    public void testNullCorrelationId_IsNotWrittenToTheSavedBundle() {
        // A fragment that never saw a correlation id (an MSAL client with no diagnostic context)
        // must not persist a null under the key. Storing one is harmless today only because
        // RequestContext extends HashMap and the sole reader substitutes a UUID for null — safety
        // that would evaporate if the map type ever became a ConcurrentHashMap/Hashtable, where
        // put(key, null) throws. Leaving the key absent keeps this identical to the already-handled
        // "never saved" case rather than depending on null surviving every layer below.
        final TestAuthorizationFragment fragment = new TestAuthorizationFragment();
        fragment.extractState(new Bundle()); // no CORRELATION_ID present -> mCorrelationId is null

        final Bundle savedState = new Bundle();
        fragment.onSaveInstanceState(savedState);

        assertFalse("a null correlation id must not be written under the key",
                savedState.containsKey(DiagnosticContext.CORRELATION_ID));

        // And the recreation path still behaves: absent key reads back as null, no throw.
        final TestAuthorizationFragment recreated = new TestAuthorizationFragment();
        recreated.extractState(savedState);
        assertNull(recreated.correlationId());
    }

    @Test
    public void testNoRecorderRegistered_ResolvesToNullNotThrow() {
        // The MSAL-client path: no onboarding seed, so nothing is ever registered. The host resolves
        // unconditionally and must simply stay inert.
        final Bundle state = new Bundle();
        state.putString(DiagnosticContext.CORRELATION_ID, CORRELATION_ID);
        mFragment.extractState(state);

        assertNull(OnboardingRecorderRegistry.get(mFragment.correlationId()));
    }

    @Test
    public void testMissingCorrelationId_ResolvesToNullNotThrow() {
        // A bundle without the key must not blow up the fragment or the registry lookup.
        mFragment.extractState(new Bundle());

        assertNull(mFragment.correlationId());
        assertNull(OnboardingRecorderRegistry.get(mFragment.correlationId()));
    }

    @Test
    public void testUnsetSentinelCorrelationId_DoesNotResolveAnotherRequestsRecorder() {
        // A request whose thread never had a request context carries the shared UNSET sentinel into
        // the Intent extra and therefore into this bundle. It must not act as a key.
        OnboardingRecorderRegistry.register(CORRELATION_ID, newRecorder());

        final Bundle state = new Bundle();
        state.putString(DiagnosticContext.CORRELATION_ID, DiagnosticContext.UNSET_CORRELATION_ID);
        mFragment.extractState(state);

        assertNull("the sentinel must never resolve a recorder",
                OnboardingRecorderRegistry.get(mFragment.correlationId()));
    }

    private OnboardingTelemetryRecorder newRecorder() {
        return new OnboardingTelemetryRecorder(
                "{\"schema_version\":\"1.0.0\","
                        + "\"session_correlation_id\":\"test-uuid-123\","
                        + "\"onboarding_mode\":\"brokered\"}",
                "test-client-id",
                "scope1",
                ApplicationProvider.getApplicationContext());
    }
}
