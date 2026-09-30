// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// MIT License
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be
// included in all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.common.internal.broker;

import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;

import com.microsoft.identity.common.internal.activebrokerdiscovery.IBrokerDiscoveryClient;
import com.microsoft.identity.common.java.constants.SharedDeviceModeConstants;
import com.microsoft.identity.common.java.exception.ClientException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class SDMBroadcastReceiverTest {
    private static final BrokerData ACTIVE_BROKER =
            new BrokerData("com.example.broker", "signature");
    private static final String BROADCAST_PERMISSION =
            ACTIVE_BROKER.getPackageName() + SharedDeviceModeConstants.BROADCAST_PERMISSION_SUFFIX;
    private static final BrokerData SECOND_BROKER =
            new BrokerData("com.example.secondbroker", "signature");
    private static final String SECOND_BROADCAST_PERMISSION =
            SECOND_BROKER.getPackageName()
                    + SharedDeviceModeConstants.BROADCAST_PERMISSION_SUFFIX;

    private Context mContext;
    private IBrokerDiscoveryClient mBrokerDiscoveryClient;
    private SDMBroadcastReceiver.BrokerCapabilityProvider mBrokerCapabilityProvider;
    private SDMBroadcastReceiver.DeviceModeProvider mDeviceModeProvider;
    private PackageHelper mPackageHelper;
    private SDMBroadcastReceiver.SharedDeviceModeCallback mCallback;

    @Before
    public void setUp() {
        mContext = mock(Context.class);
        mBrokerDiscoveryClient = mock(IBrokerDiscoveryClient.class);
        mBrokerCapabilityProvider =
                mock(SDMBroadcastReceiver.BrokerCapabilityProvider.class);
        mDeviceModeProvider = mock(SDMBroadcastReceiver.DeviceModeProvider.class);
        mPackageHelper = mock(PackageHelper.class);
        mCallback = mock(SDMBroadcastReceiver.SharedDeviceModeCallback.class);
        SDMBroadcastReceiver.resetForTest();
    }

    @After
    public void tearDown() {
        SDMBroadcastReceiver.resetForTest();
    }

    @Test
    public void initialize_capabilityDisabled_registersCompatibilityReceiver() throws Exception {
        when(mBrokerDiscoveryClient.getActiveBroker(false)).thenReturn(ACTIVE_BROKER);
        when(mBrokerCapabilityProvider.isSdmBroadcastProtectionEnabled(ACTIVE_BROKER))
                .thenReturn(false);

        SDMBroadcastReceiver.initialize(
                mContext,
                mCallback,
                mBrokerDiscoveryClient,
                mBrokerCapabilityProvider,
                mPackageHelper,
                mDeviceModeProvider
        );

        verify(mContext).registerReceiver(
                any(BroadcastReceiver.class),
                any(IntentFilter.class)
        );
    }

    @Test
    public void initialize_capabilityEnabled_registersProtectedReceiver() throws Exception {
        when(mBrokerDiscoveryClient.getActiveBroker(false)).thenReturn(ACTIVE_BROKER);
        when(mBrokerCapabilityProvider.isSdmBroadcastProtectionEnabled(ACTIVE_BROKER))
                .thenReturn(true);
        when(mPackageHelper.isSignaturePermissionGrantedToPackage(
                BROADCAST_PERMISSION,
                ACTIVE_BROKER.getPackageName()
        )).thenReturn(true);

        SDMBroadcastReceiver.initialize(
                mContext,
                mCallback,
                mBrokerDiscoveryClient,
                mBrokerCapabilityProvider,
                mPackageHelper,
                mDeviceModeProvider
        );

        verify(mContext).registerReceiver(
                any(BroadcastReceiver.class),
                any(IntentFilter.class),
                eq(BROADCAST_PERMISSION),
                isNull()
        );
    }

    @Test
    public void initialize_capabilityEnabledWithInvalidPermission_doesNotRegister()
            throws Exception {
        when(mBrokerDiscoveryClient.getActiveBroker(false)).thenReturn(ACTIVE_BROKER);
        when(mBrokerCapabilityProvider.isSdmBroadcastProtectionEnabled(ACTIVE_BROKER))
                .thenReturn(true);
        when(mPackageHelper.isSignaturePermissionGrantedToPackage(
                BROADCAST_PERMISSION,
                ACTIVE_BROKER.getPackageName()
        )).thenReturn(false);

        SDMBroadcastReceiver.initialize(
                mContext,
                mCallback,
                mBrokerDiscoveryClient,
                mBrokerCapabilityProvider,
                mPackageHelper,
                mDeviceModeProvider
        );

        verify(mContext, never()).registerReceiver(
                any(BroadcastReceiver.class),
                any(IntentFilter.class)
        );
        verify(mContext, never()).registerReceiver(
                any(BroadcastReceiver.class),
                any(IntentFilter.class),
                any(String.class),
                isNull()
        );
    }

    @Test
    public void initialize_capabilityQueryFails_doesNotRegister() throws Exception {
        when(mBrokerDiscoveryClient.getActiveBroker(false)).thenReturn(ACTIVE_BROKER);
        when(mBrokerCapabilityProvider.isSdmBroadcastProtectionEnabled(ACTIVE_BROKER))
                .thenThrow(new ClientException("test_error", "test"));

        SDMBroadcastReceiver.initialize(
                mContext,
                mCallback,
                mBrokerDiscoveryClient,
                mBrokerCapabilityProvider,
                mPackageHelper,
                mDeviceModeProvider
        );

        verify(mContext, never()).registerReceiver(
                any(BroadcastReceiver.class),
                any(IntentFilter.class)
        );
    }

    @Test
    public void initialize_resolvesStateWithoutHoldingClassMonitor() throws Exception {
        when(mBrokerDiscoveryClient.getActiveBroker(false)).thenAnswer(invocation -> {
            assertFalse(Thread.holdsLock(SDMBroadcastReceiver.class));
            return ACTIVE_BROKER;
        });
        when(mBrokerCapabilityProvider.isSdmBroadcastProtectionEnabled(ACTIVE_BROKER))
                .thenAnswer(invocation -> {
                    assertFalse(Thread.holdsLock(SDMBroadcastReceiver.class));
                    return false;
                });

        SDMBroadcastReceiver.initialize(
                mContext,
                mCallback,
                mBrokerDiscoveryClient,
                mBrokerCapabilityProvider,
                mPackageHelper,
                mDeviceModeProvider
        );
    }

    @Test
    public void initialize_protectedReceiverWithCapabilityFailure_preservesRegistrationAndCallback()
            throws Exception {
        final SDMBroadcastReceiver.SharedDeviceModeCallback updatedCallback =
                mock(SDMBroadcastReceiver.SharedDeviceModeCallback.class);
        final ArgumentCaptor<BroadcastReceiver> receiverCaptor =
                ArgumentCaptor.forClass(BroadcastReceiver.class);
        when(mBrokerDiscoveryClient.getActiveBroker(false)).thenReturn(ACTIVE_BROKER);
        when(mBrokerCapabilityProvider.isSdmBroadcastProtectionEnabled(ACTIVE_BROKER))
                .thenReturn(true)
                .thenThrow(new ClientException("test_error", "test"));
        when(mPackageHelper.isSignaturePermissionGrantedToPackage(
                BROADCAST_PERMISSION,
                ACTIVE_BROKER.getPackageName()
        )).thenReturn(true);

        SDMBroadcastReceiver.initialize(
                mContext,
                mCallback,
                mBrokerDiscoveryClient,
                mBrokerCapabilityProvider,
                mPackageHelper,
                mDeviceModeProvider
        );
        SDMBroadcastReceiver.initialize(
                mContext,
                updatedCallback,
                mBrokerDiscoveryClient,
                mBrokerCapabilityProvider,
                mPackageHelper,
                mDeviceModeProvider
        );

        verify(mContext, never()).unregisterReceiver(any(BroadcastReceiver.class));
        verify(mContext).registerReceiver(
                receiverCaptor.capture(),
                any(IntentFilter.class),
                eq(BROADCAST_PERMISSION),
                isNull()
        );

        final Intent intent = new Intent(
                SharedDeviceModeConstants.CURRENT_ACCOUNT_CHANGED_BROADCAST_IDENTIFIER
        );
        intent.putExtra(
                SharedDeviceModeConstants.BROADCAST_TYPE_KEY,
                SharedDeviceModeConstants.BROADCAST_TYPE_GLOBAL_SIGN_OUT
        );
        receiverCaptor.getValue().onReceive(mContext, intent);

        verify(updatedCallback).onGlobalSignOut();
        verify(mCallback, never()).onGlobalSignOut();
    }

    @Test
    public void initialize_compatibilityReceiverWithCapabilityFailure_clearsRegistration()
            throws Exception {
        when(mBrokerDiscoveryClient.getActiveBroker(false)).thenReturn(ACTIVE_BROKER);
        when(mBrokerCapabilityProvider.isSdmBroadcastProtectionEnabled(ACTIVE_BROKER))
                .thenReturn(false)
                .thenThrow(new ClientException("test_error", "test"));

        SDMBroadcastReceiver.initialize(
                mContext,
                mCallback,
                mBrokerDiscoveryClient,
                mBrokerCapabilityProvider,
                mPackageHelper,
                mDeviceModeProvider
        );
        SDMBroadcastReceiver.initialize(
                mContext,
                mCallback,
                mBrokerDiscoveryClient,
                mBrokerCapabilityProvider,
                mPackageHelper,
                mDeviceModeProvider
        );

        verify(mContext).unregisterReceiver(any(BroadcastReceiver.class));
    }

    @Test
    public void initialize_protectedReplacementFails_preservesPreviousRegistration()
            throws Exception {
        when(mBrokerDiscoveryClient.getActiveBroker(false))
                .thenReturn(ACTIVE_BROKER, SECOND_BROKER, ACTIVE_BROKER);
        when(mBrokerCapabilityProvider.isSdmBroadcastProtectionEnabled(ACTIVE_BROKER))
                .thenReturn(true);
        when(mBrokerCapabilityProvider.isSdmBroadcastProtectionEnabled(SECOND_BROKER))
                .thenReturn(true);
        when(mPackageHelper.isSignaturePermissionGrantedToPackage(
                BROADCAST_PERMISSION,
                ACTIVE_BROKER.getPackageName()
        )).thenReturn(true);
        when(mPackageHelper.isSignaturePermissionGrantedToPackage(
                SECOND_BROADCAST_PERMISSION,
                SECOND_BROKER.getPackageName()
        )).thenReturn(true);
        when(mContext.registerReceiver(
                any(BroadcastReceiver.class),
                any(IntentFilter.class),
                any(String.class),
                isNull()
        )).thenReturn(null).thenThrow(new SecurityException("test"));

        SDMBroadcastReceiver.initialize(
                mContext,
                mCallback,
                mBrokerDiscoveryClient,
                mBrokerCapabilityProvider,
                mPackageHelper,
                mDeviceModeProvider
        );
        SDMBroadcastReceiver.initialize(
                mContext,
                mCallback,
                mBrokerDiscoveryClient,
                mBrokerCapabilityProvider,
                mPackageHelper,
                mDeviceModeProvider
        );
        SDMBroadcastReceiver.initialize(
                mContext,
                mCallback,
                mBrokerDiscoveryClient,
                mBrokerCapabilityProvider,
                mPackageHelper,
                mDeviceModeProvider
        );

        verify(mContext, times(2)).registerReceiver(
                any(BroadcastReceiver.class),
                any(IntentFilter.class),
                any(String.class),
                isNull()
        );
        verify(mContext, never()).unregisterReceiver(any(BroadcastReceiver.class));
    }

    @Test
    public void initialize_protectedReplacementSucceeds_registersBeforeUnregisteringPrevious()
            throws Exception {
        when(mBrokerDiscoveryClient.getActiveBroker(false))
                .thenReturn(ACTIVE_BROKER, SECOND_BROKER);
        when(mBrokerCapabilityProvider.isSdmBroadcastProtectionEnabled(ACTIVE_BROKER))
                .thenReturn(true);
        when(mBrokerCapabilityProvider.isSdmBroadcastProtectionEnabled(SECOND_BROKER))
                .thenReturn(true);
        when(mPackageHelper.isSignaturePermissionGrantedToPackage(
                BROADCAST_PERMISSION,
                ACTIVE_BROKER.getPackageName()
        )).thenReturn(true);
        when(mPackageHelper.isSignaturePermissionGrantedToPackage(
                SECOND_BROADCAST_PERMISSION,
                SECOND_BROKER.getPackageName()
        )).thenReturn(true);

        SDMBroadcastReceiver.initialize(
                mContext,
                mCallback,
                mBrokerDiscoveryClient,
                mBrokerCapabilityProvider,
                mPackageHelper,
                mDeviceModeProvider
        );
        SDMBroadcastReceiver.initialize(
                mContext,
                mCallback,
                mBrokerDiscoveryClient,
                mBrokerCapabilityProvider,
                mPackageHelper,
                mDeviceModeProvider
        );

        final InOrder inOrder = inOrder(mContext);
        inOrder.verify(mContext).registerReceiver(
                any(BroadcastReceiver.class),
                any(IntentFilter.class),
                eq(BROADCAST_PERMISSION),
                isNull()
        );
        inOrder.verify(mContext).registerReceiver(
                any(BroadcastReceiver.class),
                any(IntentFilter.class),
                eq(SECOND_BROADCAST_PERMISSION),
                isNull()
        );
        inOrder.verify(mContext).unregisterReceiver(any(BroadcastReceiver.class));
    }

    @Test
    public void initialize_sameState_isIdempotent() throws Exception {
        when(mBrokerDiscoveryClient.getActiveBroker(false)).thenReturn(ACTIVE_BROKER);
        when(mBrokerCapabilityProvider.isSdmBroadcastProtectionEnabled(ACTIVE_BROKER))
                .thenReturn(false);

        SDMBroadcastReceiver.initialize(
                mContext,
                mCallback,
                mBrokerDiscoveryClient,
                mBrokerCapabilityProvider,
                mPackageHelper,
                mDeviceModeProvider
        );
        SDMBroadcastReceiver.initialize(
                mContext,
                mock(SDMBroadcastReceiver.SharedDeviceModeCallback.class),
                mBrokerDiscoveryClient,
                mBrokerCapabilityProvider,
                mPackageHelper,
                mDeviceModeProvider
        );

        verify(mContext, times(1)).registerReceiver(
                any(BroadcastReceiver.class),
                any(IntentFilter.class)
        );
    }

    @Test
    public void initialize_afterInvalidState_canRetry() throws Exception {
        when(mBrokerDiscoveryClient.getActiveBroker(false)).thenReturn(ACTIVE_BROKER);
        when(mBrokerCapabilityProvider.isSdmBroadcastProtectionEnabled(ACTIVE_BROKER))
                .thenReturn(true);
        when(mPackageHelper.isSignaturePermissionGrantedToPackage(
                BROADCAST_PERMISSION,
                ACTIVE_BROKER.getPackageName()
        )).thenReturn(false, true);

        SDMBroadcastReceiver.initialize(
                mContext,
                mCallback,
                mBrokerDiscoveryClient,
                mBrokerCapabilityProvider,
                mPackageHelper,
                mDeviceModeProvider
        );
        SDMBroadcastReceiver.initialize(
                mContext,
                mCallback,
                mBrokerDiscoveryClient,
                mBrokerCapabilityProvider,
                mPackageHelper,
                mDeviceModeProvider
        );

        verify(mContext).registerReceiver(
                any(BroadcastReceiver.class),
                any(IntentFilter.class),
                eq(BROADCAST_PERMISSION),
                isNull()
        );
    }

    @Test
    public void initialize_modeChanges_reregistersReceiver() throws Exception {
        when(mBrokerDiscoveryClient.getActiveBroker(false)).thenReturn(ACTIVE_BROKER);
        when(mBrokerCapabilityProvider.isSdmBroadcastProtectionEnabled(ACTIVE_BROKER))
                .thenReturn(false, true);
        when(mPackageHelper.isSignaturePermissionGrantedToPackage(
                BROADCAST_PERMISSION,
                ACTIVE_BROKER.getPackageName()
        )).thenReturn(true);

        SDMBroadcastReceiver.initialize(
                mContext,
                mCallback,
                mBrokerDiscoveryClient,
                mBrokerCapabilityProvider,
                mPackageHelper,
                mDeviceModeProvider
        );
        SDMBroadcastReceiver.initialize(
                mContext,
                mCallback,
                mBrokerDiscoveryClient,
                mBrokerCapabilityProvider,
                mPackageHelper,
                mDeviceModeProvider
        );

        verify(mContext).unregisterReceiver(any(BroadcastReceiver.class));
        verify(mContext).registerReceiver(
                any(BroadcastReceiver.class),
                any(IntentFilter.class),
                eq(BROADCAST_PERMISSION),
                isNull()
        );
    }

    @Test
    public void registerReceiver_protectedOnApi32_usesPermissionOverload() {
        final BroadcastReceiver receiver = mock(BroadcastReceiver.class);
        final IntentFilter filter = mock(IntentFilter.class);
        final SDMBroadcastReceiver.ReceiverRegistrar receiverRegistrar =
                mock(SDMBroadcastReceiver.ReceiverRegistrar.class);

        SDMBroadcastReceiver.registerReceiver(
                receiverRegistrar,
                receiver,
                filter,
                BROADCAST_PERMISSION,
                Build.VERSION_CODES.S_V2
        );

        verify(receiverRegistrar).registerProtected(
                receiver,
                filter,
                BROADCAST_PERMISSION,
                false
        );
    }

    @Test
    public void registerReceiver_protectedOnApi33_usesPermissionAndExportedFlag() {
        final BroadcastReceiver receiver = mock(BroadcastReceiver.class);
        final IntentFilter filter = mock(IntentFilter.class);
        final SDMBroadcastReceiver.ReceiverRegistrar receiverRegistrar =
                mock(SDMBroadcastReceiver.ReceiverRegistrar.class);

        SDMBroadcastReceiver.registerReceiver(
                receiverRegistrar,
                receiver,
                filter,
                BROADCAST_PERMISSION,
                Build.VERSION_CODES.TIRAMISU
        );

        verify(receiverRegistrar).registerProtected(
                receiver,
                filter,
                BROADCAST_PERMISSION,
                true
        );
    }

}
