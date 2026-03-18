package com.microsoft.identity.common.java.opentelemetry;
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

public enum SpanName {
    AcquirePrtUsingBrt,
    AcquireTokenInteractive,
    AcquireTokenSilent,
    SetScopeForDMAgentForFoci,
    GetAccounts,
    RemoveAccount,
    WorkplaceJoin,
    ATIInteractively,
    ATISilently,
    WorkplaceLeave,
    DeviceState,
    CertBasedAuth,
    MSAL_PerformIpcStrategy,
    DeviceRegistrationApi,
    DeviceRegistrationIpc,
    WorkplaceJoinApi,
    AcquireTokenDcf,
    AcquireTokenDcfAuthRequest,
    AcquireTokenDcfFetchToken,
    EncryptionManager,
    Passthrough,
    BrokerOperationRequestDispatcher,
    BrokerDiscoveryManagerPerformDiscoveryProcess,
    Fido,
    BrokerAccountServiceRemoveAccounts,
    AcquirePrtUsingTransferToken,
    AcquireTransferTokenUsingPrt,
    SaveTransferTokenToBlockstore,
    GetBackedUpMsaAccounts,
    RefreshTransferToken,
    IsLtwPreInstalled,
    DeleteTransferToken,
    RestoreMsaAccounts,
    OnUpgradeReceiver,
    UpgradeDeviceRegistration,
    RemoveBrokerAccount,
    ProcessNonceFromEstsRedirect,
    DataStoreCorruptionException,
    KeyPairGeneration,
    ProcessCrossCloudRedirect,
    SwitchBrowserResume,
    SwitchBrowserProcess,
    WrappedKeyAlgorithmIdentifier,
    ProcessWebCpRedirects,
    ProvisionResourceAccount,
    ProcessWebsiteRequest,
    GetAllSsoTokens,
    ProcessWebCpEnrollmentRedirect,
    ProcessWebCpAuthorizeUrlRedirect,
    ProcessOpenIdVcRequest,
    PasskeyWebListener,
    InstallCertOnWpj,
    /**
     * Span name for fetching initial ECS flight configurations.
     */
    EcsFlightsFetchConfigs,
    DevicePopMintSignedAccessToken,
    /**
     * Span name for DRS (Device Registration Service) nonce request operations.
     */
    DRSNonceRequest,
    /**
     * Span name for Device POP crypto operations.
     */
    DevicePopCryptoOperation,
    /**
     * Span name for web apps API execute requests.
     */
    ExecuteWebAppsRequest,
    /**
     * Span name for the Browser SSO header generation operation.
     */
    GetBrowserSsoHeaders,
    /**
     * Span name for secret key generation operations.
     */
    SecretKeyGeneration,
    /**
     * Span name for secret key retrieval operations.
     */
    SecretKeyRetrieval,
    /**
     * Span name for WebView target="_blank" navigation interception.
     */
    WebViewTargetBlankNavigation,
    /**
     * Span name for WebView file upload (onShowFileChooser) operations.
     */
    WebViewFileUpload
}
