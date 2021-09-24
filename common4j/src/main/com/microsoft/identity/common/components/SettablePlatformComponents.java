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
package com.microsoft.identity.common.components;

import com.microsoft.identity.common.java.cache.IMultiTypeNameValueStorage;
import com.microsoft.identity.common.java.cache.MapBackedPreferencesManager;
import com.microsoft.identity.common.java.commands.ICommand;
import com.microsoft.identity.common.java.commands.parameters.InteractiveTokenCommandParameters;
import com.microsoft.identity.common.java.crypto.CryptoSuite;
import com.microsoft.identity.common.java.crypto.IAndroidKeyStoreKeyManager;
import com.microsoft.identity.common.java.crypto.IDevicePopManager;
import com.microsoft.identity.common.java.crypto.IKeyAccessor;
import com.microsoft.identity.common.java.crypto.SecureHardwareState;
import com.microsoft.identity.common.java.crypto.SigningAlgorithm;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.interfaces.INameValueStorage;
import com.microsoft.identity.common.java.interfaces.IPlatformComponents;
import com.microsoft.identity.common.java.providers.oauth2.IAuthorizationStrategy;
import com.microsoft.identity.common.java.providers.oauth2.IStateGenerator;
import com.microsoft.identity.common.java.strategies.IAuthorizationStrategyFactory;
import com.microsoft.identity.common.java.ui.BrowserDescriptor;
import com.microsoft.identity.common.java.util.IClockSkewManager;
import com.microsoft.identity.common.java.util.IPlatformUtil;
import com.microsoft.identity.common.java.util.TaskCompletedCallbackWithError;
import com.microsoft.identity.common.java.util.ported.InMemoryStorage;

import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.UnrecoverableEntryException;
import java.security.cert.Certificate;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;

/**
 * This class is an implemenation of IPlatformComponents where all attributes are settable.  This
 * is mainly created for testing purposes, included in this package so that it is useful across
 * different packaging.  It contains non-functional implementations of almost every different
 * component by default.
 */
@Builder
@Getter
@Accessors(prefix = "m")
public class SettablePlatformComponents implements IPlatformComponents {

    public static final IDevicePopManager NONFUNCTIONAL_POP_MANAGER = new IDevicePopManager() {
        @Override
        public boolean asymmetricKeyExists() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean asymmetricKeyExists(String thumbprint) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getAsymmetricKeyThumbprint() throws ClientException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void generateAsymmetricKey(TaskCompletedCallbackWithError<String, ClientException> callback) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String generateAsymmetricKey() throws ClientException {
            throw new UnsupportedOperationException();
        }

        @Override
        public Date getAsymmetricKeyCreationDate() throws ClientException {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean clearAsymmetricKey() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getRequestConfirmation() throws ClientException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void getRequestConfirmation(TaskCompletedCallbackWithError<String, ClientException> callback) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String sign(SigningAlgorithm alg, String input) throws ClientException {
            throw new UnsupportedOperationException();
        }

        @Override
        public byte[] sign(@NonNull SigningAlgorithm alg, byte[] input) throws ClientException {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean verify(SigningAlgorithm alg, String plainText, String signatureStr) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean verify(@NonNull SigningAlgorithm alg, byte[] plainText, byte[] signatureBytes) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String encrypt(Cipher cipher, String plaintext) throws ClientException {
            throw new UnsupportedOperationException();
        }

        @Override
        public byte[] encrypt(@NonNull Cipher cipher, @NonNull byte[] plaintext) throws ClientException {
            throw new UnsupportedOperationException();
        }

        @Override
        public String decrypt(Cipher cipher, String ciphertext) throws ClientException {
            throw new UnsupportedOperationException();
        }

        @Override
        public byte[] decrypt(@NonNull Cipher cipher, byte[] ciphertext) throws ClientException {
            throw new UnsupportedOperationException();
        }

        @Override
        public SecureHardwareState getSecureHardwareState() throws ClientException {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getPublicKey(PublicKeyFormat format) throws ClientException {
            throw new UnsupportedOperationException();
        }

        @Override
        public PublicKey getPublicKey() throws UnrecoverableEntryException, NoSuchAlgorithmException, KeyStoreException {
            throw new UnsupportedOperationException();
        }

        @Override
        public Certificate[] getCertificateChain() throws ClientException {
            throw new UnsupportedOperationException();
        }

        @Override
        public String mintSignedAccessToken(String httpMethod, long timestamp, URL requestUrl, String accessToken, String nonce) throws ClientException {
            throw new UnsupportedOperationException();
        }

        @Override
        public String mintSignedAccessToken(String httpMethod, long timestamp, URL requestUrl, String accessToken, String nonce, String clientClaims) throws ClientException {
            throw new UnsupportedOperationException();
        }

        @Override
        public String mintSignedHttpRequest(String httpMethod, long timestamp, URL requestUrl, String nonce, String clientClaims) throws ClientException {
            throw new UnsupportedOperationException();
        }

        @Override
        public IAndroidKeyStoreKeyManager<KeyStore.PrivateKeyEntry> getKeyManager() {
            throw new UnsupportedOperationException();
        }
    };
    @Builder.Default
    private final IKeyAccessor mStorageEncryptionManager = new IKeyAccessor() {
        @Override
        public byte[] encrypt(byte[] plaintext) throws ClientException {
            throw new UnsupportedOperationException();
        }

        @Override
        public byte[] decrypt(byte[] ciphertext) throws ClientException {
            throw new UnsupportedOperationException();
        }

        @Override
        public byte[] sign(byte[] text) throws ClientException {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean verify(byte[] text, byte[] signature) throws ClientException {
            throw new UnsupportedOperationException();
        }

        @Override
        public byte[] getThumbprint() throws ClientException {
            throw new UnsupportedOperationException();
        }

        @Override
        public Certificate[] getCertificateChain() throws ClientException {
            throw new UnsupportedOperationException();
        }

        @Override
        public SecureHardwareState getSecureHardwareState() throws ClientException {
            throw new UnsupportedOperationException();
        }

        @Override
        public IKeyAccessor generateDerivedKey(byte[] label, byte[] ctx, CryptoSuite suite) throws ClientException {
            throw new UnsupportedOperationException();
        }
    };

    private final IClockSkewManager mClockSkewManager = new IClockSkewManager() {
        @Override
        public void onTimestampReceived(long referenceTime) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long getSkewMillis() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Date toClientTime(long referenceTime) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Date toReferenceTime(long clientTime) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Date getCurrentClientTime() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Date getAdjustedReferenceTime() {
            throw new UnsupportedOperationException();
        }
    };

    @Override
    public @NonNull IKeyAccessor getStorageEncryptionManager() {
        return null;
    }

    @Override
    public @NonNull IClockSkewManager getClockSkewManager() {
        return null;
    }

    @Override
    public @NonNull IDevicePopManager getDevicePopManager(@Nullable String alias) throws ClientException {
        return mDevicePopManager;
    }

    private final IDevicePopManager mDefaultDevicePopManager = NONFUNCTIONAL_POP_MANAGER;

    @Builder.Default
    @Getter
    private final IDevicePopManager mDevicePopManager = NONFUNCTIONAL_POP_MANAGER;

    private final Map<String, INameValueStorage<?>> mStores = new ConcurrentHashMap<>();

    @Override
    public synchronized <T> INameValueStorage<T> getNameValueStore(String storeName, Class<T> clazz) {
        @SuppressWarnings("unchecked")
        INameValueStorage<T> ret = (INameValueStorage<T>) mStores.get(storeName);
        if (ret == null) {
            mStores.put(storeName, new InMemoryStorage<T>());
            ret = (INameValueStorage<T>) mStores.get(storeName);
        }
        return ret;
    }

    private final Map<String, INameValueStorage<?>> mEncryptedStores = new ConcurrentHashMap<>();

    @Override
    public <T> INameValueStorage<T> getEncryptedNameValueStore(String storeName, IKeyAccessor helper, Class<T> clazz) {
        @SuppressWarnings("unchecked")
        INameValueStorage<T> ret = (INameValueStorage<T>) mEncryptedStores.get(storeName);
        if (ret == null) {
            mEncryptedStores.put(storeName, new InMemoryStorage<>());
            ret = (INameValueStorage<T>) mEncryptedStores.get(storeName);
        }
        return ret;
    }

    private final Map<String, IMultiTypeNameValueStorage> mEncryptedFileStores = new ConcurrentHashMap<>();

    @Override
    public synchronized IMultiTypeNameValueStorage getEncryptedFileStore(String storeName, IKeyAccessor helper) {
        IMultiTypeNameValueStorage ret = mEncryptedFileStores.get(storeName);
        if (ret == null) {
            mEncryptedFileStores.put(storeName, MapBackedPreferencesManager.builder().name(storeName).build());
            ret = (IMultiTypeNameValueStorage) mEncryptedFileStores.get(storeName);
        }
        return ret;
    }

    private final Map<String, IMultiTypeNameValueStorage> mFileStores = new ConcurrentHashMap<>();

    @Override
    public IMultiTypeNameValueStorage getFileStore(String storeName) {
        IMultiTypeNameValueStorage ret = mFileStores.get(storeName);
        if (ret == null) {
            mFileStores.put(storeName, MapBackedPreferencesManager.builder().name(storeName).build());
            ret = (IMultiTypeNameValueStorage) mFileStores.get(storeName);
        }
        return ret;
    }

    @Builder.Default
    private final IAuthorizationStrategyFactory mAuthorizationStrategyFactory = new IAuthorizationStrategyFactory() {
        @Override
        public IAuthorizationStrategy getAuthorizationStrategy(@NonNull InteractiveTokenCommandParameters parameters) {
            throw new UnsupportedOperationException();
        }
    };

    private final IStateGenerator mStateGenerator = new IStateGenerator() {
        @Override
        public @NonNull String generate() {
            throw new UnsupportedOperationException();
        }
    };

    private final IPlatformUtil mPlatformUtil = new IPlatformUtil() {

        @Override
        public List<BrowserDescriptor> getBrowserSafeListForBroker() {
            throw new UnsupportedOperationException();
        }

        @Nullable
        @Override
        public String getInstalledCompanyPortalVersion() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void throwIfNetworkNotAvailable(boolean performPowerOptimizationCheck) throws ClientException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void removeCookiesFromWebView() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isValidCallingApp(@NonNull String redirectUri, @NonNull String packageName) {
            throw new UnsupportedOperationException();
        }

        @Nullable
        @Override
        public String getEnrollmentId(@NonNull String userId, @NonNull String packageName) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void onReturnCommandResult(@NonNull ICommand<?> command) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long getNanosecondTime() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void postCommandResult(@NonNull Runnable runnable) {
            throw new UnsupportedOperationException();
        }
    };
}
