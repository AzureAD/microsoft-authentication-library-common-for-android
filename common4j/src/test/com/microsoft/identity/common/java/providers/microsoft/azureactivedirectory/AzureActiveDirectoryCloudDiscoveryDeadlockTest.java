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
package com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory;

import com.microsoft.identity.common.java.authorities.Authority;
import com.microsoft.identity.common.java.authorities.AzureActiveDirectoryAuthority;
import com.microsoft.identity.common.java.net.HttpUrlConnectionFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import javax.net.ssl.HttpsURLConnection;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;

/**
 * Reproduction tests for deadlock/convoy risks in cloud discovery.
 *
 * These tests verify that long-running network I/O during cloud discovery
 * does not block unrelated synchronized methods on {@link AzureActiveDirectory}
 * or {@link Authority}.
 *
 * On the UNFIXED code, these tests will FAIL (timeout) because:
 * <ul>
 *   <li>{@code performCloudDiscovery()} holds the class monitor during the entire HTTP call</li>
 *   <li>Other synchronized methods ({@code hasCloudHost}, {@code isInitialized}, etc.) are blocked</li>
 *   <li>{@code Authority.performCloudDiscovery()} holds {@code sLock} during the HTTP call,
 *       blocking {@code addKnownAuthorities()}</li>
 * </ul>
 *
 * After the fix, these tests should PASS because network I/O no longer occurs
 * while holding these monitors.
 */
@RunWith(JUnit4.class)
public class AzureActiveDirectoryCloudDiscoveryDeadlockTest {

    private Set<String> savedDiscoveryHosts;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        // Clear the discovery-succeeded set so performCloudDiscovery will attempt the HTTP call
        final Field hostsField = AzureActiveDirectory.class.getDeclaredField("sDiscoverySucceededHosts");
        hostsField.setAccessible(true);
        savedDiscoveryHosts = (Set<String>) hostsField.get(null);
        savedDiscoveryHosts.clear();
    }

    @After
    public void tearDown() throws Exception {
        HttpUrlConnectionFactory.clearMockedConnectionQueue();
    }

    /**
     * Verifies that {@code hasCloudHost()} is not blocked when
     * {@code performCloudDiscovery()} is performing a slow network call.
     *
     * <p><b>Bug:</b> Both methods are {@code static synchronized} on
     * {@code AzureActiveDirectory.class}, so {@code hasCloudHost()} is blocked
     * for the entire duration of the HTTP request inside {@code performCloudDiscovery()}.
     */
    @Test(timeout = 10000)
    public void testHasCloudHostNotBlockedByCloudDiscovery() throws Exception {
        final CountDownLatch httpCallStarted = new CountDownLatch(1);
        final CountDownLatch httpCallCanProceed = new CountDownLatch(1);

        HttpUrlConnectionFactory.addMockedConnection(
                createBlockingMockConnection(httpCallStarted, httpCallCanProceed)
        );

        // Thread A: performCloudDiscovery() — holds AzureActiveDirectory.class monitor
        // during the (blocked) HTTP call.
        final Thread discoveryThread = new Thread(() -> {
            try {
                AzureActiveDirectory.performCloudDiscovery();
            } catch (Exception e) {
                // Expected — mock may not return a fully valid response after unblocking
            }
        }, "DiscoveryThread");
        discoveryThread.setDaemon(true);
        discoveryThread.start();

        // Wait until the HTTP call has started — at this point, the class monitor is held.
        assertTrue("HTTP call should have started within 5s",
                httpCallStarted.await(5, TimeUnit.SECONDS));

        // Thread B: hasCloudHost() — on buggy code this blocks on the class monitor;
        // on fixed code this should return immediately.
        final CountDownLatch readerCompleted = new CountDownLatch(1);
        final Thread readerThread = new Thread(() -> {
            try {
                AzureActiveDirectory.hasCloudHost(
                        new URL("https://login.microsoftonline.com")
                );
            } catch (Exception e) {
                // Ignore
            } finally {
                readerCompleted.countDown();
            }
        }, "HasCloudHostReader");
        readerThread.setDaemon(true);
        readerThread.start();

        // If buggy: readerThread is blocked on class monitor → await times out → test fails
        // If fixed: readerThread returns immediately → await succeeds
        final boolean completed = readerCompleted.await(3, TimeUnit.SECONDS);

        // Cleanup: unblock the HTTP call so threads can exit
        httpCallCanProceed.countDown();
        discoveryThread.join(5000);
        readerThread.join(5000);

        assertTrue(
                "hasCloudHost() was blocked by performCloudDiscovery() holding the class "
                        + "monitor during network I/O — this is a deadlock/convoy risk.",
                completed
        );
    }

    /**
     * Verifies that {@code Authority.addKnownAuthorities()} is not blocked when
     * {@code Authority.getKnownAuthorityResult()} is performing cloud discovery.
     *
     * <p><b>Bug:</b> {@code Authority.performCloudDiscovery()} holds {@code sLock}
     * while calling {@code AzureActiveDirectory.performCloudDiscovery()}.
     * {@code addKnownAuthorities()} also synchronizes on {@code sLock}, so it is
     * blocked for the entire duration of the HTTP call.
     */
    @Test(timeout = 10000)
    public void testAddKnownAuthoritiesNotBlockedByCloudDiscovery() throws Exception {
        final CountDownLatch httpCallStarted = new CountDownLatch(1);
        final CountDownLatch httpCallCanProceed = new CountDownLatch(1);

        HttpUrlConnectionFactory.addMockedConnection(
                createBlockingMockConnection(httpCallStarted, httpCallCanProceed)
        );

        // Thread A: getKnownAuthorityResult() → Authority.performCloudDiscovery()
        //   acquires sLock → AzureActiveDirectory.performCloudDiscovery() → blocks on HTTP.
        //   Both sLock and AzureActiveDirectory.class are held.
        final Thread discoveryThread = new Thread(() -> {
            try {
                Authority.getKnownAuthorityResult(new AzureActiveDirectoryAuthority());
            } catch (Exception e) {
                // Expected
            }
        }, "AuthorityDiscoveryThread");
        discoveryThread.setDaemon(true);
        discoveryThread.start();

        assertTrue("HTTP call should have started within 5s",
                httpCallStarted.await(5, TimeUnit.SECONDS));

        // Thread B: addKnownAuthorities() — needs sLock.
        // On buggy code: blocked because Thread A holds sLock during the HTTP call.
        // On fixed code: sLock is not held during network I/O, so this returns immediately.
        final CountDownLatch addCompleted = new CountDownLatch(1);
        final Thread addThread = new Thread(() -> {
            try {
                Authority.addKnownAuthorities(new ArrayList<>());
            } finally {
                addCompleted.countDown();
            }
        }, "AddKnownAuthoritiesThread");
        addThread.setDaemon(true);
        addThread.start();

        final boolean completed = addCompleted.await(3, TimeUnit.SECONDS);

        // Cleanup
        httpCallCanProceed.countDown();
        discoveryThread.join(5000);
        addThread.join(5000);

        assertTrue(
                "addKnownAuthorities() was blocked by getKnownAuthorityResult() holding sLock "
                        + "during network I/O — this is a convoy/deadlock risk.",
                completed
        );
    }

    /**
     * Creates a mock HTTPS connection that blocks on {@code getResponseCode()},
     * simulating a slow or stalled network call. Signals {@code started} when the
     * blocking call begins, and waits on {@code canProceed} before returning.
     */
    private HttpsURLConnection createBlockingMockConnection(
            final CountDownLatch started,
            final CountDownLatch canProceed) throws Exception {

        final HttpsURLConnection mockConn = Mockito.mock(HttpsURLConnection.class);

        Mockito.when(mockConn.getURL()).thenReturn(
                new URL("https://login.microsoftonline.com/common/discovery/instance")
        );

        // Block on getResponseCode() — this is called by UrlConnectionHttpClient
        // while the caller (performCloudDiscovery) holds the synchronized lock.
        Mockito.when(mockConn.getResponseCode()).thenAnswer(invocation -> {
            started.countDown();
            canProceed.await();
            return HttpsURLConnection.HTTP_OK;
        });

        // Provide minimal response streams for after unblocking
        Mockito.when(mockConn.getInputStream()).thenReturn(
                new ByteArrayInputStream(
                        "{\"tenant_discovery_endpoint\":\"https://x\",\"metadata\":[]}".getBytes()
                )
        );
        Mockito.when(mockConn.getHeaderFields()).thenReturn(Collections.emptyMap());

        return mockConn;
    }

    private static void setStaticField(
            final Class<?> clazz,
            final String fieldName,
            final Object value) throws Exception {
        final Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(null, value);
    }
}
