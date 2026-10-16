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
package com.microsoft.identity.common.java.cache;

import com.microsoft.identity.common.java.dto.AccessTokenRecord;
import com.microsoft.identity.common.java.dto.AccountCredentialBase;
import com.microsoft.identity.common.java.dto.AccountRecord;
import com.microsoft.identity.common.java.dto.Credential;
import com.microsoft.identity.common.java.dto.CredentialType;
import com.microsoft.identity.common.java.dto.IdTokenRecord;
import com.microsoft.identity.common.java.dto.PrimaryRefreshTokenRecord;
import com.microsoft.identity.common.java.dto.RefreshTokenRecord;
import com.microsoft.identity.common.java.interfaces.INameValueStorage;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.opentelemetry.AttributeName;
import com.microsoft.identity.common.java.opentelemetry.OTelUtility;
import com.microsoft.identity.common.java.opentelemetry.OtelContextExtension;
import com.microsoft.identity.common.java.opentelemetry.SpanExtension;
import com.microsoft.identity.common.java.util.StringUtil;
import com.microsoft.identity.common.java.util.ported.Predicate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.NonNull;

/**
 * Alternative version of {@link SharedPreferencesAccountCredentialCache} that assumes all writes and reads
 * are done through a single-instance and can thereforce be cached in memory.
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class SharedPreferencesAccountCredentialCacheWithMemoryCache extends AbstractAccountCredentialCache {

    private static final String TAG = SharedPreferencesAccountCredentialCacheWithMemoryCache.class.getSimpleName();
    public static final long DEFAULT_MAX_MEMORY_CACHE_BYTES = 16L * 1024 * 1024;
    private static final long RECORD_OVERHEAD_BYTES = 512;
    private static final ThreadPoolExecutor LOAD_EXECUTOR = createLoadExecutor();

    private final ICacheKeyValueDelegate mCacheValueDelegate;
    private final IAccountCredentialCache mDurableCache;
    private final long mMaximumMemoryBytes;

    private final Object mCacheLock = new Object();

    /**
     * Returns the internal cache lock for callers that need to atomically perform
     * multiple filtered reads under a single lock acquisition (e.g., MsalOAuth2TokenCache.load()).
     * The lock is reentrant via Java's synchronized semantics.
     */
    public Object getCacheLock() {
        return mCacheLock;
    }

    private boolean mLoaded = false;
    private boolean mUseMemoryCache = true;
    private long mMemoryBytes;
    private long mPayloadBytes;
    private long mQueryBytes;
    private boolean mRetainLoadedPayloads = true;
    private Map<String, IndexedRecord> mEntryWeights = new HashMap<>();
    private Map<AccountCredentialBase, IndexedRecord> mRecordKeys = new IdentityHashMap<>();
    private final Map<String, AccountCredentialBase> mHotRecords = new LinkedHashMap<>(16, .75f, true);
    private final Map<List<String>, List<Credential>> mCredentialQueries = new LinkedHashMap<>(16, .75f, true);
    private Map<String, AccountRecord> mCachedAccountRecordsWithKeys = new HashMap<>();
    private Map<String, Credential> mCachedCredentialsWithKeys = new HashMap<>();

    /**
     * Constructor of SharedPreferencesAccountCredentialCacheWithMemoryCache.
     *
     * @param accountCacheValueDelegate    ICacheKeyValueDelegate
     * @param sharedPreferencesFileManager INameValueStorage
     */
    public SharedPreferencesAccountCredentialCacheWithMemoryCache(
            @NonNull final ICacheKeyValueDelegate accountCacheValueDelegate,
            @NonNull final INameValueStorage<String> sharedPreferencesFileManager) {
        this(accountCacheValueDelegate, sharedPreferencesFileManager, DEFAULT_MAX_MEMORY_CACHE_BYTES);
    }

    /**
     * Creates a complete metadata index and a bounded read-through record cache. Index and payload accounting share
     * one budget; oversized payloads are read on demand instead of disabling indexed token lookups.
     */
    public SharedPreferencesAccountCredentialCacheWithMemoryCache(
            @NonNull final ICacheKeyValueDelegate accountCacheValueDelegate,
            @NonNull final INameValueStorage<String> sharedPreferencesFileManager,
            final long maximumMemoryBytes) {
        super(sharedPreferencesFileManager);
        if (maximumMemoryBytes <= 0) {
            throw new IllegalArgumentException("Memory-cache budget must be positive");
        }
        Logger.verbose(TAG, "Init: " + TAG);
        mCacheValueDelegate = accountCacheValueDelegate;
        mDurableCache = new SharedPreferencesAccountCredentialCache(accountCacheValueDelegate, sharedPreferencesFileManager);
        mMaximumMemoryBytes = maximumMemoryBytes;
        try {
            LOAD_EXECUTOR.execute(OtelContextExtension.wrap(this::load));
        } catch (final RejectedExecutionException e) {
            synchronized (mCacheLock) {
                disableMemoryCache();
                mLoaded = true;
                mCacheLock.notifyAll();
            }
            Logger.info(TAG, "Memory-cache loader is full; using durable reads");
        }
    }

    private static ThreadPoolExecutor createLoadExecutor() {
        final ThreadPoolExecutor executor = new ThreadPoolExecutor(4, 4, 30, TimeUnit.SECONDS,
                new ArrayBlockingQueue<Runnable>(16), task -> {
                    final Thread thread = new Thread(task, "common-memory-cache-loader");
                    thread.setDaemon(true);
                    return thread;
                });
        executor.allowCoreThreadTimeOut(true);
        return executor;
    }

    /**
     * Drops decoded record payloads, retaining only the bounded metadata needed to avoid whole-cache deserialization.
     * Subsequent reads may retain requested records again within the same budget.
     * May wait for an active cache operation; invoke off the UI thread.
     */
    @Override
    public void trimMemory() {
        synchronized (mCacheLock) {
            mHotRecords.clear();
            mPayloadBytes = 0;
            mRetainLoadedPayloads = false;
        }
    }

    /**
     * Returns accounted metadata and retained record payload bytes, or zero in durable-read mode.
     */
    public long getMemoryCacheSizeBytes() {
        synchronized (mCacheLock) {
            return mMemoryBytes + mPayloadBytes + mQueryBytes;
        }
    }

    /** Returns retained decoded record payload bytes, excluding the compact metadata index. */
    public long getMemoryCachePayloadSizeBytes() {
        synchronized (mCacheLock) {
            return mPayloadBytes;
        }
    }

    private void disableMemoryCache() {
        mUseMemoryCache = false;
        mCachedAccountRecordsWithKeys = new HashMap<>();
        mCachedCredentialsWithKeys = new HashMap<>();
        mEntryWeights = new HashMap<>();
        mRecordKeys = new IdentityHashMap<>();
        mHotRecords.clear();
        clearQueries();
        mMemoryBytes = 0;
        mPayloadBytes = 0;
    }

    private long entryWeight(final String key, final String value) {
        return RECORD_OVERHEAD_BYTES + 2L * (key.length() + (long) value.length());
    }

    private static long stringBytes(final String value) {
        return value == null ? 0 : 2L * value.length();
    }

    private long metadataWeight(final String key, final AccountCredentialBase record) {
        long bytes = RECORD_OVERHEAD_BYTES + stringBytes(key);
        if (record instanceof AccountRecord) {
            final AccountRecord account = (AccountRecord) record;
            return bytes + stringBytes(account.getHomeAccountId()) + stringBytes(account.getEnvironment())
                    + stringBytes(account.getRealm()) + stringBytes(account.getLocalAccountId());
        }
        final Credential credential = (Credential) record;
        bytes += stringBytes(credential.getHomeAccountId()) + stringBytes(credential.getEnvironment())
                + stringBytes(credential.getClientId()) + stringBytes(credential.getCredentialType());
        if (credential instanceof AccessTokenRecord) {
            final AccessTokenRecord token = (AccessTokenRecord) credential;
            bytes += stringBytes(token.getRealm()) + stringBytes(token.getTarget()) + stringBytes(token.getKid())
                    + stringBytes(token.getAccessTokenType()) + stringBytes(token.getRequestedClaims())
                    + stringBytes(token.getApplicationIdentifier()) + stringBytes(token.getMamEnrollmentIdentifier());
        } else if (credential instanceof RefreshTokenRecord) {
            final RefreshTokenRecord token = (RefreshTokenRecord) credential;
            bytes += stringBytes(token.getTarget()) + stringBytes(token.getFamilyId());
        } else if (credential instanceof IdTokenRecord) {
            bytes += stringBytes(((IdTokenRecord) credential).getRealm());
        }
        return bytes;
    }

    private AccountRecord metadata(final AccountRecord account) {
        final AccountRecord metadata = new AccountRecord();
        metadata.setHomeAccountId(account.getHomeAccountId());
        metadata.setEnvironment(account.getEnvironment());
        metadata.setRealm(account.getRealm());
        metadata.setLocalAccountId(account.getLocalAccountId());
        return metadata;
    }

    private Credential metadata(final Credential credential) {
        final Credential metadata;
        if (credential instanceof AccessTokenRecord) {
            final AccessTokenRecord source = (AccessTokenRecord) credential;
            final AccessTokenRecord token = new AccessTokenRecord();
            token.setRealm(source.getRealm());
            token.setTarget(source.getTarget());
            token.setKid(source.getKid());
            token.setAccessTokenType(source.getAccessTokenType());
            token.setRequestedClaims(source.getRequestedClaims());
            token.setApplicationIdentifier(source.getApplicationIdentifier());
            token.setMamEnrollmentIdentifier(source.getMamEnrollmentIdentifier());
            metadata = token;
        } else if (credential instanceof RefreshTokenRecord) {
            final RefreshTokenRecord source = (RefreshTokenRecord) credential;
            final RefreshTokenRecord token = new RefreshTokenRecord();
            token.setTarget(source.getTarget());
            token.setFamilyId(source.getFamilyId());
            metadata = token;
        } else if (credential instanceof IdTokenRecord) {
            final IdTokenRecord token = new IdTokenRecord();
            token.setRealm(((IdTokenRecord) credential).getRealm());
            metadata = token;
        } else if (credential instanceof PrimaryRefreshTokenRecord) {
            metadata = new PrimaryRefreshTokenRecord();
        } else {
            throw new IllegalArgumentException("Unsupported credential metadata type");
        }
        metadata.setHomeAccountId(credential.getHomeAccountId());
        metadata.setEnvironment(credential.getEnvironment());
        metadata.setClientId(credential.getClientId());
        metadata.setCredentialType(credential.getCredentialType());
        return metadata;
    }

    @SuppressWarnings("unchecked")
    private <T extends AccountCredentialBase> T indexRecord(final String key, final T metadata,
                                                          final AccountCredentialBase record,
                                                          final String serialized, final boolean retainPayload) {
        final long weight = metadataWeight(key, metadata);
        final IndexedRecord previous = mEntryWeights.get(key);
        // Keep match references stable across secret/expiry refreshes; only predicate changes require an index rescan.
        if (previous != null && previous.metadata.getClass() == metadata.getClass()
                && previous.metadata.equals(metadata)) {
            if (mHotRecords.remove(key) != null) {
                mPayloadBytes -= previous.payloadBytes;
            }
            previous.payloadBytes = entryWeight(key, serialized);
            if (retainPayload) {
                retainPayload(previous, record);
            }
            return (T) previous.metadata;
        }
        if (weight > mMaximumMemoryBytes - (mMemoryBytes - (previous == null ? 0 : previous.indexBytes))) {
            throw new MemoryCacheCapacityException();
        }
        removeEntryWeight(key);
        final IndexedRecord indexed = new IndexedRecord(key, metadata, weight, entryWeight(key, serialized));
        mEntryWeights.put(key, indexed);
        mRecordKeys.put(metadata, indexed);
        mMemoryBytes += weight;
        evictPayloads(0);
        if (retainPayload) {
            retainPayload(indexed, record);
        }
        return metadata;
    }

    private void evictPayloads(final long incomingBytes) {
        final Iterator<Map.Entry<String, AccountCredentialBase>> entries = mHotRecords.entrySet().iterator();
        while (entries.hasNext() && incomingBytes > mMaximumMemoryBytes - mMemoryBytes - mPayloadBytes - mQueryBytes) {
            final String key = entries.next().getKey();
            mPayloadBytes -= mEntryWeights.get(key).payloadBytes;
            entries.remove();
        }
    }

    private void retainPayload(final IndexedRecord indexed, final AccountCredentialBase record) {
        if (indexed.payloadBytes > mMaximumMemoryBytes - mMemoryBytes - mQueryBytes) {
            return;
        }
        if (mHotRecords.remove(indexed.key) != null) {
            mPayloadBytes -= indexed.payloadBytes;
        }
        evictPayloads(indexed.payloadBytes);
        mHotRecords.put(indexed.key, record);
        mPayloadBytes += indexed.payloadBytes;
    }

    private AccountCredentialBase readRecord(final IndexedRecord indexed) {
        final AccountCredentialBase cached = mHotRecords.get(indexed.key);
        if (cached != null) {
            return cached;
        }
        final AccountCredentialBase record = indexed.metadata instanceof AccountRecord
                ? mDurableCache.getAccount(indexed.key) : mDurableCache.getCredential(indexed.key);
        if (record != null) {
            retainPayload(indexed, record);
        }
        return record;
    }

    private void removeEntryWeight(final String key) {
        clearQueries();
        final IndexedRecord indexed = mEntryWeights.remove(key);
        if (indexed != null) {
            mMemoryBytes -= indexed.indexBytes;
            mRecordKeys.remove(indexed.metadata);
            if (mHotRecords.remove(key) != null) {
                mPayloadBytes -= indexed.payloadBytes;
            }
        }
    }

    private void clearQueries() {
        mCredentialQueries.clear();
        mQueryBytes = 0;
    }

    private static long queryWeight(final List<String> key, final List<Credential> matches) {
        long bytes = 256 + 8L * matches.size();
        for (final String field : key) {
            bytes += stringBytes(field);
        }
        return bytes;
    }

    private List<Credential> matchingCredentials(final String homeAccountId, final String environment,
                                                final CredentialType type, final String clientId,
                                                final String applicationIdentifier, final String mamEnrollmentIdentifier,
                                                final String realm, final String target, final String authScheme,
                                                final String requestedClaims) {
        final List<String> key = Arrays.asList(homeAccountId, environment, type == null ? null : type.name(), clientId,
                applicationIdentifier, mamEnrollmentIdentifier, realm, target, authScheme, requestedClaims);
        final List<Credential> cached = mCredentialQueries.get(key);
        if (cached != null) {
            return cached;
        }
        final List<Credential> matches = getCredentialsFilteredByInternal(
                new ArrayList<>(mCachedCredentialsWithKeys.values()), homeAccountId, environment, type, clientId,
                applicationIdentifier, mamEnrollmentIdentifier, realm, target, authScheme, requestedClaims, null, false);
        final long weight = queryWeight(key, matches);
        final long maximumQueryBytes = Math.min(1024 * 1024, mMaximumMemoryBytes / 16);
        if (matches.size() <= 128 && weight <= maximumQueryBytes && weight <= mMaximumMemoryBytes - mMemoryBytes) {
            final Iterator<Map.Entry<List<String>, List<Credential>>> entries = mCredentialQueries.entrySet().iterator();
            while (entries.hasNext() && (mCredentialQueries.size() >= 64
                    || weight > maximumQueryBytes - mQueryBytes || weight > mMaximumMemoryBytes - mMemoryBytes - mQueryBytes)) {
                final Map.Entry<List<String>, List<Credential>> eldest = entries.next();
                mQueryBytes -= queryWeight(eldest.getKey(), eldest.getValue());
                entries.remove();
            }
            evictPayloads(weight);
            mCredentialQueries.put(key, matches);
            mQueryBytes += weight;
        }
        return matches;
    }

    List<Credential> getCredentialSnapshot(final String homeAccountId, final String environment, final String realm,
                                          final CredentialType accessTokenType, final String clientId,
                                          final String applicationIdentifier, final String mamEnrollmentIdentifier,
                                          final String target, final String authScheme) {
        synchronized (mCacheLock) {
            waitForInitialLoad();
            if (!mUseMemoryCache) {
                return mDurableCache.getCredentials();
            }
            // A fully resident cache smaller than the four selections costs less to snapshot once.
            if (mCachedCredentialsWithKeys.size() <= 4 && mHotRecords.size() == mEntryWeights.size()) {
                return getCredentials();
            }
            final List<Credential> snapshot = getCredentialsFilteredBy(
                    homeAccountId, environment, accessTokenType, clientId, applicationIdentifier,
                    mamEnrollmentIdentifier, realm, target, authScheme, (String) null);
            for (final CredentialType type : new CredentialType[]{
                    CredentialType.RefreshToken, CredentialType.IdToken, CredentialType.V1IdToken}) {
                snapshot.addAll(getCredentialsFilteredBy(
                        homeAccountId, environment, type, clientId, null, null, null, null, null, (String) null));
            }
            return snapshot;
        }
    }

    List<AccountRecord> getAccountsByLocalId(final String environment, final String localAccountId) {
        synchronized (mCacheLock) {
            waitForInitialLoad();
            if (!mUseMemoryCache) {
                final List<AccountRecord> matches = new ArrayList<>();
                for (final AccountRecord account : mDurableCache.getAccounts()) {
                    if (localAccountId.equals(account.getLocalAccountId())
                            && (StringUtil.isNullOrEmpty(environment)
                            || StringUtil.equalsIgnoreCaseTrimBoth(environment, account.getEnvironment()))) {
                        matches.add(account);
                    }
                }
                return matches;
            }
            final List<AccountRecord> matches = new ArrayList<>();
            for (final AccountRecord account : mCachedAccountRecordsWithKeys.values()) {
                if (localAccountId.equals(account.getLocalAccountId())
                        && (StringUtil.isNullOrEmpty(environment)
                        || StringUtil.equalsIgnoreCaseTrimBoth(environment, account.getEnvironment()))) {
                    matches.add(account);
                }
            }
            return cloneItems(matches, TAG);
        }
    }

    List<Credential> getTokenWriteCandidates(final AccessTokenRecord reference, final boolean exactClaims) {
        synchronized (mCacheLock) {
            waitForInitialLoad();
            final List<Credential> records = mUseMemoryCache
                    ? new ArrayList<>(mCachedCredentialsWithKeys.values()) : mDurableCache.getCredentials();
            final List<Credential> matches = getCredentialsFilteredByInternal(
                    records, reference.getHomeAccountId(), reference.getEnvironment(),
                    CredentialType.fromString(reference.getCredentialType()), reference.getClientId(),
                    reference.getApplicationIdentifier(), reference.getMamEnrollmentIdentifier(), reference.getRealm(),
                    null, reference.getAccessTokenType(), reference.getRequestedClaims(), null, exactClaims);
            final List<Credential> snapshot = new ArrayList<>(matches.size());
            for (final Credential credential : matches) {
                try {
                    snapshot.add((Credential) credential.clone());
                } catch (final CloneNotSupportedException failure) {
                    throw new IllegalStateException("Token write metadata must be cloneable", failure);
                }
            }
            return snapshot;
        }
    }

    private static final class IndexedRecord {
        private final String key;
        private final AccountCredentialBase metadata;
        private final long indexBytes;
        private long payloadBytes;

        IndexedRecord(final String key, final AccountCredentialBase metadata,
                      final long indexBytes, final long payloadBytes) {
            this.key = key;
            this.metadata = metadata;
            this.indexBytes = indexBytes;
            this.payloadBytes = payloadBytes;
        }
    }

    private static final class MemoryCacheCapacityException extends IllegalStateException {
        private static final long serialVersionUID = 1L;
    }

    private void load() {
        final long loadStartTime = System.currentTimeMillis();
        final String methodTag = TAG + ":load";
        RuntimeException failure = null;
        boolean oversized = false;
        synchronized (mCacheLock) {
            boolean published = false;
            try {
                if (mUseMemoryCache) {
                    final Map<String, AccountRecord> accounts = loadAccountsWithKeys();
                    final Map<String, Credential> credentials = loadCredentialsWithKeys();
                    mCachedAccountRecordsWithKeys = accounts;
                    mCachedCredentialsWithKeys = credentials;
                    published = true;
                }
            } catch (final MemoryCacheCapacityException e) {
                oversized = true;
            } catch (final RuntimeException e) {
                failure = e;
            } finally {
                if (!published) {
                    disableMemoryCache();
                }
                mLoaded = true;
                mCacheLock.notifyAll();
            }
        }
        if (oversized) {
            Logger.info(methodTag, "Memory mirror exceeds its budget; using durable reads");
        }
        if (failure != null) {
            Logger.error(methodTag, "Memory-cache initialization failed; using durable reads", failure);
        }
        OTelUtility.recordElapsedTime(AttributeName.elapsed_time_in_memory_cache_load.name(), loadStartTime);
    }

    private void waitForInitialLoad() {
        final String methodTag = TAG + ":waitForInitialLoad";

        while (!mLoaded) {
            try {
                mCacheLock.wait();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for the memory cache", e);
            }
        }
    }

    /**
     * Clones each element of {@code items} and returns the cloned list.
     */
    @SuppressWarnings("unchecked")
    @NonNull
    private <T extends AccountCredentialBase> List<T> cloneItems(
            @NonNull final Collection<T> items,
            @NonNull final String methodTag) {
        final List<T> cloned = new ArrayList<>(items.size());
        for (final T metadata : items) {
            final AccountCredentialBase item = readRecord(mRecordKeys.get(metadata));
            if (item == null) {
                continue;
            }
            try {
                cloned.add((T) item.clone());
            } catch (final CloneNotSupportedException e) {
                Logger.error(methodTag, "Failed to clone " + item.getClass().getSimpleName(), e);
            }
        }
        return cloned;
    }

    @Override
    public void saveAccount(@NonNull final AccountRecord accountInput) {
        final String methodTag = TAG + ":saveAccount";

        AccountRecord accountToSave = null;
        try {
            accountToSave = (AccountRecord) accountInput.clone();
        } catch (final CloneNotSupportedException e) {
            Logger.error(methodTag, "Failed to clone AccountRecord", e);
            return;
        }

        Logger.verbose(methodTag, "Saving Account...");
        Logger.verbose(methodTag, "Account type: [" + accountToSave.getClass().getSimpleName() + "]");
        final String cacheKey = mCacheValueDelegate.generateCacheKey(accountToSave);
        Logger.verbosePII(methodTag, "Generated cache key: [" + cacheKey + "]");

        synchronized (mCacheLock) {
            waitForInitialLoad();
            if (!mUseMemoryCache) {
                mDurableCache.saveAccount(accountToSave);
                return;
            }

            // Perform any necessary field merging on the Account to save...
            final AccountRecord existingAccount = getAccount(cacheKey);

            if (null != existingAccount) {
                accountToSave.mergeAdditionalFields(existingAccount);
            }

            final String cacheValue = mCacheValueDelegate.generateCacheValue(accountToSave);
            mSharedPreferencesFileManager.put(cacheKey, cacheValue);
            try {
                final AccountRecord metadata = indexRecord(
                        cacheKey, metadata(accountToSave), accountToSave, cacheValue, true);
                mCachedAccountRecordsWithKeys.put(cacheKey, metadata);
            } catch (final MemoryCacheCapacityException e) {
                disableMemoryCache();
                Logger.info(methodTag, "Memory mirror exceeds its budget; using durable reads");
                return;
            }
        }
    }

    @Override
    public void saveCredential(@NonNull Credential credentialInput) {
        final String methodTag = TAG + ":saveCredential";

        Credential credentialToSave = null;
        try {
            credentialToSave = (Credential) credentialInput.clone();
        } catch (final CloneNotSupportedException e) {
            Logger.error(methodTag, "Failed to clone Credential", e);
            return;
        }

        Logger.verbose(methodTag, "Saving credential...");
        final String cacheKey = mCacheValueDelegate.generateCacheKey(credentialToSave);
        Logger.verbosePII(methodTag, "Generated cache key: [" + cacheKey + "]");

        synchronized (mCacheLock) {
            waitForInitialLoad();
            if (!mUseMemoryCache) {
                mDurableCache.saveCredential(credentialToSave);
                return;
            }

            // Perform any necessary field merging on the Credential to save...
            final Credential existingCredential = getCredential(cacheKey);

            if (null != existingCredential) {
                credentialToSave.mergeAdditionalFields(existingCredential);
            }

            final String cacheValue = mCacheValueDelegate.generateCacheValue(credentialToSave);
            mSharedPreferencesFileManager.put(cacheKey, cacheValue);
            try {
                final Credential metadata = indexRecord(
                        cacheKey, metadata(credentialToSave), credentialToSave, cacheValue, true);
                mCachedCredentialsWithKeys.put(cacheKey, metadata);
            } catch (final MemoryCacheCapacityException e) {
                disableMemoryCache();
                Logger.info(methodTag, "Memory mirror exceeds its budget; using durable reads");
                return;
            }
        }
    }

    @Override
    public AccountRecord getAccount(@NonNull final String cacheKey) {
        final String methodTag = TAG + ":getAccount";

        AccountRecord foundValue;

        synchronized (mCacheLock) {
            waitForInitialLoad();
            if (!mUseMemoryCache) {
                return mDurableCache.getAccount(cacheKey);
            }
            final IndexedRecord indexed = mEntryWeights.get(cacheKey);
            foundValue = indexed == null || !(indexed.metadata instanceof AccountRecord)
                    ? null : (AccountRecord) readRecord(indexed);
        }

        try {
            if (foundValue != null) {
                foundValue = (AccountRecord) foundValue.clone();
            }
        } catch (final CloneNotSupportedException e) {
            Logger.error(methodTag, "Failed to clone AccountRecord", e);
        }
        return foundValue;
    }

    @Override
    @Nullable
    public Credential getCredential(@NonNull final String cacheKey) {
        final String methodTag = TAG + ":getCredential";

        Credential foundValue;

        synchronized (mCacheLock) {
            waitForInitialLoad();
            if (!mUseMemoryCache) {
                return mDurableCache.getCredential(cacheKey);
            }
            final IndexedRecord indexed = mEntryWeights.get(cacheKey);
            foundValue = indexed == null || !(indexed.metadata instanceof Credential)
                    ? null : (Credential) readRecord(indexed);
        }

        try {
            if (foundValue != null) {
                foundValue = (Credential) foundValue.clone();
            }
        } catch (final CloneNotSupportedException e) {
            Logger.error(methodTag, "Failed to clone Credential", e);
        }

        return foundValue;
    }

    @NonNull
    private Map<String, AccountRecord> loadAccountsWithKeys() {
        final String methodTag = TAG + ":loadAccountsWithKeys";

        Logger.verbose(methodTag, "Loading Accounts + keys...");
        final Iterator<Map.Entry<String, String>> cacheValues = mSharedPreferencesFileManager.getAllFilteredByKey(new Predicate<String>() {
            @Override
            public boolean test(String value) {
                return isAccount(value);
            }
        });
        final Map<String, AccountRecord> accounts = new HashMap<>();
        if (cacheValues != null) {
            while (cacheValues.hasNext()) {
                Map.Entry<String, ?> cacheValue = cacheValues.next();
                final String cacheKey = cacheValue.getKey();
                final String serialized = cacheValue.getValue().toString();
                final AccountRecord account = mCacheValueDelegate.fromCacheValue(
                        serialized,
                        AccountRecord.class
                );

                if (null == account) {
                    Logger.warn(methodTag, SharedPreferencesAccountCredentialCache.ACCOUNT_RECORD_DESERIALIZATION_FAILED);
                } else if (SharedPreferencesAccountCredentialCache.EMPTY_ACCOUNT.equals(account)) {
                    Logger.warn(methodTag, "The returned Account was uninitialized. Removing...");
                    mSharedPreferencesFileManager.remove(cacheKey);
                } else {
                    accounts.put(cacheKey, indexRecord(
                            cacheKey, metadata(account), account, serialized, mRetainLoadedPayloads));
                }
            }
        }

        Logger.verbose(methodTag, "Returning [" + accounts.size() + "] Accounts w/ keys...");

        return accounts;
    }

    @Override
    @NonNull
    public List<AccountRecord> getAccounts() {
        final String methodTag = TAG + ":getAccounts";
        Logger.verbose(methodTag, "Loading Accounts...(no arg)");

        synchronized (mCacheLock) {
            waitForInitialLoad();
            if (!mUseMemoryCache) {
                return mDurableCache.getAccounts();
            }
            SpanExtension.current().setAttribute(
                    AttributeName.number_of_accounts_in_cache.name(),
                    mCachedAccountRecordsWithKeys.size());
            SpanExtension.current().setAttribute(
                    AttributeName.number_of_credentials_in_cache.name(),
                    mCachedCredentialsWithKeys.size());
            final List<AccountRecord> accounts = cloneItems(mCachedAccountRecordsWithKeys.values(), methodTag);
            Logger.info(methodTag, "Found [" + accounts.size() + "] Accounts...");
            return accounts;
        }
    }

    @Override
    @NonNull
    public List<AccountRecord> getAccountsFilteredBy(
            @Nullable final String homeAccountId,
            @Nullable final String environment,
            @Nullable final String realm) {
        final String methodTag = TAG + ":getAccountsFilteredBy";
        Logger.verbose(methodTag, "Loading Accounts...");

        synchronized (mCacheLock) {
            waitForInitialLoad();
            if (!mUseMemoryCache) {
                return mDurableCache.getAccountsFilteredBy(homeAccountId, environment, realm);
            }
            SpanExtension.current().setAttribute(
                    AttributeName.number_of_accounts_in_cache.name(), mCachedAccountRecordsWithKeys.size());
            SpanExtension.current().setAttribute(
                    AttributeName.number_of_credentials_in_cache.name(), mCachedCredentialsWithKeys.size());
            final List<AccountRecord> matching = getAccountsFilteredByInternal(
                    homeAccountId, environment, realm, new ArrayList<>(mCachedAccountRecordsWithKeys.values()));
            return cloneItems(matching, methodTag);
        }
    }

    @NonNull
    private Map<String, Credential> loadCredentialsWithKeys() {
        final String methodTag = TAG + ":getCredentialsWithKeys";
        Logger.verbose(methodTag, "Loading Credentials with keys...");

        final Map<String, Credential> credentials = new HashMap<>();
        final Iterator<Map.Entry<String, String>> cacheValues = mSharedPreferencesFileManager.getAllFilteredByKey(new Predicate<String>() {
            @Override
            public boolean test(String value) {
                return isCredential(value);
            }
        });

        while (cacheValues.hasNext()) {
            Map.Entry<String, ?> cacheValue = cacheValues.next();
            final String cacheKey = cacheValue.getKey();
            final String serialized = cacheValue.getValue().toString();
            final Class<? extends AccountCredentialBase> clazz = credentialClassForType(cacheKey);
            final Credential credential = mCacheValueDelegate.fromCacheValue(
                    serialized,
                    clazz
            );

            if (null == credential) {
                Logger.warn(methodTag, SharedPreferencesAccountCredentialCache.CREDENTIAL_DESERIALIZATION_FAILED);
            } else if ((AccessTokenRecord.class == clazz && SharedPreferencesAccountCredentialCache.EMPTY_AT.equals(credential))
                || (RefreshTokenRecord.class == clazz && SharedPreferencesAccountCredentialCache.EMPTY_RT.equals(credential))
                || (IdTokenRecord.class == clazz) && SharedPreferencesAccountCredentialCache.EMPTY_ID.equals(credential)) {
                // The returned credential came back uninitialized...
                // Remove the entry and return null...
                Logger.warn(methodTag, "The returned Credential was uninitialized. Removing...");
                mSharedPreferencesFileManager.remove(cacheKey);
            }
            else {
                credentials.put(cacheKey, indexRecord(
                        cacheKey, metadata(credential), credential, serialized, mRetainLoadedPayloads));
            }
        }

        Logger.verbose(methodTag, "Loaded [" + credentials.size() + "] Credentials...");

        return credentials;
    }

    @Override
    @NonNull
    public List<Credential> getCredentials() {
        final String methodTag = TAG + ":getCredentials";
        Logger.verbose(methodTag, "Loading Credentials...");

        synchronized (mCacheLock) {
            waitForInitialLoad();
            if (!mUseMemoryCache) {
                return mDurableCache.getCredentials();
            }
            return cloneItems(mCachedCredentialsWithKeys.values(), methodTag);
        }
    }

    @Override
    @NonNull
    public List<Credential> getCredentialsFilteredBy(
            @Nullable final String homeAccountId,
            @Nullable final String environment,
            @Nullable final CredentialType credentialType,
            @Nullable final String clientId,
            @Nullable final String applicationIdentifier,
            @Nullable final String mamEnrollmentIdentifier,
            @Nullable final String realm,
            @Nullable final String target,
            @Nullable final String authScheme) {
        final String methodTag = TAG + ":getCredentialsFilteredBy";
        Logger.verbose(methodTag, "getCredentialsFilteredBy()");

        return getCredentialsFilteredBy(homeAccountId, environment, credentialType, clientId,
                applicationIdentifier, mamEnrollmentIdentifier, realm, target, authScheme, (String) null);
    }

    @Override
    public List<Credential> getCredentialsFilteredBy(
            @Nullable final String homeAccountId,
            @Nullable final String environment,
            @Nullable final CredentialType credentialType,
            @Nullable final String clientId,
            @Nullable final String applicationIdentifier,
            @Nullable final String mamEnrollmentIdentifier,
            @Nullable final String realm,
            @Nullable final String target,
            @Nullable final String authScheme,
            @NonNull final List<Credential> inputCredentials) {
        final String methodTag = TAG + ":getCredentialsFilteredBy";
        Logger.verbose(methodTag, "getCredentialsFilteredBy() -- with input list");

        final List<Credential> matchingCredentials = getCredentialsFilteredByInternal(
                inputCredentials,
                homeAccountId,
                environment,
                credentialType,
                clientId,
                applicationIdentifier,
                mamEnrollmentIdentifier,
                realm,
                target,
                authScheme,
                null,
                null,
                false
        );

        Logger.verbose(methodTag, "Found [" + matchingCredentials.size() + "] matching Credentials...");

        return matchingCredentials;
    }

    @Override
    @NonNull
    public List<Credential> getCredentialsFilteredBy(
            @Nullable final String homeAccountId,
            @Nullable final String environment,
            @Nullable final CredentialType credentialType,
            @Nullable final String clientId,
            @Nullable final String applicationIdentifier,
            @Nullable final String mamEnrollmentIdentifier,
            @Nullable final String realm,
            @Nullable final String target,
            @Nullable final String authScheme,
            @Nullable final String requestedClaims) {
        final String methodTag = TAG + ":getCredentialsFilteredBy";
        Logger.verbose(methodTag, "getCredentialsFilteredBy()");

        synchronized (mCacheLock) {
            waitForInitialLoad();
            if (!mUseMemoryCache) {
                return mDurableCache.getCredentialsFilteredBy(homeAccountId, environment, credentialType, clientId,
                        applicationIdentifier, mamEnrollmentIdentifier, realm, target, authScheme, requestedClaims);
            }
            final List<Credential> matching = matchingCredentials(homeAccountId, environment, credentialType, clientId,
                    applicationIdentifier, mamEnrollmentIdentifier, realm, target, authScheme, requestedClaims);
            return cloneItems(matching, methodTag);
        }
    }

    @Override
    @NonNull
    public List<Credential> getCredentialsFilteredBy(
            @Nullable final String homeAccountId,
            @Nullable final String environment,
            @Nullable final CredentialType credentialType,
            @Nullable final String clientId,
            @Nullable final String applicationIdentifier,
            @Nullable final String mamEnrollmentIdentifier,
            @Nullable final String realm,
            @Nullable final String target,
            @Nullable final String authScheme,
            @Nullable final String requestedClaims,
            @NonNull final List<Credential> inputCredentials) {
        final String methodTag = TAG + ":getCredentialsFilteredBy";
        Logger.verbose(methodTag, "getCredentialsFilteredBy()");

        final List<Credential> matchingCredentials = getCredentialsFilteredByInternal(
                inputCredentials,
                homeAccountId,
                environment,
                credentialType,
                clientId,
                applicationIdentifier,
                mamEnrollmentIdentifier,
                realm,
                target,
                authScheme,
                requestedClaims,
                null,
                false
        );

        Logger.verbose(methodTag, "Found [" + matchingCredentials.size() + "] matching Credentials...");

        return matchingCredentials;
    }

    @Override
    @NonNull
    public List<Credential> getCredentialsFilteredBy(
            @Nullable final String homeAccountId,
            @Nullable final String environment,
            @Nullable final CredentialType credentialType,
            @Nullable final String clientId,
            @Nullable final String applicationIdentifier,
            @Nullable final String mamEnrollmentIdentifier,
            @Nullable final String realm,
            @Nullable final String target,
            @Nullable final String authScheme,
            @Nullable final String requestedClaims,
            final boolean mustMatchExactClaims,
            @NonNull final List<Credential> inputCredentials) {
        final String methodTag = TAG + ":getCredentialsFilteredBy";
        Logger.verbose(methodTag, "getCredentialsFilteredBy()");

        final List<Credential> matchingCredentials = getCredentialsFilteredByInternal(
                inputCredentials,
                homeAccountId,
                environment,
                credentialType,
                clientId,
                applicationIdentifier,
                mamEnrollmentIdentifier,
                realm,
                target,
                authScheme,
                requestedClaims,
                null,
                mustMatchExactClaims
        );

        Logger.verbose(methodTag, "Found [" + matchingCredentials.size() + "] matching Credentials...");

        return matchingCredentials;
    }

    @Override
    public List<Credential> getCredentialsFilteredBy(@Nullable final String homeAccountId,
                                                     @Nullable final String environment,
                                                     @NonNull final Set<CredentialType> credentialTypes,
                                                     @Nullable final String clientId,
                                                     @Nullable final String applicationIdentifier,
                                                     @Nullable final String mamEnrollmentIdentifier,
                                                     @Nullable final String realm,
                                                     @Nullable final String target,
                                                     @Nullable final String authScheme,
                                                     @Nullable final String requestedClaims) {
        synchronized (mCacheLock) {
            waitForInitialLoad();
            if (!mUseMemoryCache) {
                return mDurableCache.getCredentialsFilteredBy(homeAccountId, environment, credentialTypes, clientId,
                        applicationIdentifier, mamEnrollmentIdentifier, realm, target, authScheme, requestedClaims);
            }
            final List<Credential> result = new ArrayList<>();
            for (final CredentialType type : credentialTypes) {
                result.addAll(matchingCredentials(homeAccountId, environment, type, clientId,
                        applicationIdentifier, mamEnrollmentIdentifier, realm, target, authScheme,
                        requestedClaims));
            }
            return cloneItems(result, TAG);
        }
    }

    @Override
    public List<Credential> getCredentialsFilteredBy(
            @NonNull List<Credential> inputCredentials,
            @Nullable final String homeAccountId,
            @Nullable final String environment,
            @Nullable final CredentialType credentialType,
            @Nullable final String clientId,
            @Nullable final String applicationIdentifier,
            @Nullable final String mamEnrollmentIdentifier,
            @Nullable final String realm,
            @Nullable final String target,
            @Nullable final String authScheme,
            @Nullable final String requestedClaims,
            @Nullable final String kid ) {
        final List<Credential> result = new ArrayList<>();
        result.addAll(
                getCredentialsFilteredByInternal(
                        inputCredentials,
                        homeAccountId,
                        environment,
                        credentialType,
                        clientId,
                        applicationIdentifier,
                        mamEnrollmentIdentifier,
                        realm,
                        target,
                        authScheme,
                        requestedClaims,
                        kid,
                        false
                )
        );
        return result;
    }

    @Override
    public boolean removeAccount(@NonNull final AccountRecord accountToRemove) {
        final String methodTag = TAG + ":removeAccount";
        Logger.info(methodTag, "Removing Account...");

        final String cacheKey = mCacheValueDelegate.generateCacheKey(accountToRemove);

        synchronized (mCacheLock) {
            waitForInitialLoad();
            if (!mUseMemoryCache) {
                return mDurableCache.removeAccount(accountToRemove);
            }
            boolean accountRemoved = false;
            // Use the in-memory map to check key existence instead of
            // mSharedPreferencesFileManager.keySet() which triggers getAll()
            // and decrypts every value in SharedPreferences just to check a key.
            if (mCachedAccountRecordsWithKeys.containsKey(cacheKey)) {
                SpanExtension.current().setAttribute(
                        AttributeName.cache_key_in_storage_but_not_in_memory.name(), false);
                mSharedPreferencesFileManager.remove(cacheKey);
                accountRemoved = true;
            } else if (mSharedPreferencesFileManager.keySet().contains(cacheKey)) {
                SpanExtension.current().setAttribute(
                        AttributeName.cache_key_in_storage_but_not_in_memory.name(), true);
                mSharedPreferencesFileManager.remove(cacheKey);
                accountRemoved = true;
            }
            Logger.info(methodTag, "Account was removed? [" + accountRemoved + "]");

            mCachedAccountRecordsWithKeys.remove(cacheKey);
            removeEntryWeight(cacheKey);

            return accountRemoved;
        }
    }

    @Override
    public boolean removeCredential(@NonNull final Credential credentialToRemove) {
        final String methodTag = TAG + ":removeCredential";
        Logger.info(methodTag, "Removing Credential...");

        final String cacheKey = mCacheValueDelegate.generateCacheKey(credentialToRemove);

        synchronized (mCacheLock) {
            waitForInitialLoad();
            if (!mUseMemoryCache) {
                return mDurableCache.removeCredential(credentialToRemove);
            }
            boolean credentialRemoved = false;
            // Use the in-memory map to check key existence instead of
            // mSharedPreferencesFileManager.keySet() which triggers getAll()
            // and decrypts every value in SharedPreferences just to check a key.
            if (mCachedCredentialsWithKeys.containsKey(cacheKey)) {
                SpanExtension.current().setAttribute(
                        AttributeName.cache_key_in_storage_but_not_in_memory.name(), false);
                mSharedPreferencesFileManager.remove(cacheKey);
                credentialRemoved = true;
            } else if (mSharedPreferencesFileManager.keySet().contains(cacheKey)) {
                SpanExtension.current().setAttribute(
                        AttributeName.cache_key_in_storage_but_not_in_memory.name(), true);
                mSharedPreferencesFileManager.remove(cacheKey);
                credentialRemoved = true;
            }

            Logger.info(methodTag, "Credential was removed? [" + credentialRemoved + "]");

            mCachedCredentialsWithKeys.remove(cacheKey);
            removeEntryWeight(cacheKey);

            return credentialRemoved;
        }

    }

    @Override
    public void clearAll() {
        final String methodTag = TAG + ":clearAll";
        Logger.info(methodTag, "Clearing all SharedPreferences entries...");
        synchronized (mCacheLock) {
            waitForInitialLoad();
            mSharedPreferencesFileManager.clear();
            mCachedCredentialsWithKeys.clear();
            mCachedAccountRecordsWithKeys.clear();
            mEntryWeights.clear();
            mRecordKeys.clear();
            mHotRecords.clear();
            clearQueries();
            mMemoryBytes = 0;
            mPayloadBytes = 0;
            mUseMemoryCache = true;
        }
        Logger.info(methodTag, "SharedPreferences cleared.");
    }

    @Nullable
    private Class<? extends Credential> credentialClassForType(@NonNull final String cacheKey) {
        final String methodTag = TAG + ":credentialClassForType";
        Logger.verbose(methodTag, "Resolving class for key/CredentialType...");
        Logger.verbosePII(methodTag, "Supplied key: [" + cacheKey + "]");

        final CredentialType targetType = getCredentialTypeForCredentialCacheKey(cacheKey);

        if (targetType == null) {
            return null;
        }

        Logger.verbose(methodTag, "CredentialType matched: [" + targetType + "]");

        return getTargetClassForCredentialType(cacheKey, targetType);
    }

    /**
     * Inspects the supplied cache key to determine the target CredentialType.
     *
     * @param cacheKey The cache key to inspect.
     * @return The CredentialType or null if a proper type cannot be resolved.
     */
    @Nullable
    public static CredentialType getCredentialTypeForCredentialCacheKey(@NonNull final String cacheKey) {
        final String methodTag = TAG + ":getCredentialTypeForCredentialCacheKey";
        if (StringUtil.isNullOrEmpty(cacheKey)) {
            throw new IllegalArgumentException("Param [cacheKey] cannot be null.");
        }

        final Set<String> credentialTypesLowerCase = new HashSet<>();

        for (final String credentialTypeStr : CredentialType.valueSet()) {
            credentialTypesLowerCase.add(credentialTypeStr.toLowerCase(Locale.US));
        }

        CredentialType type = null;
        for (final String credentialTypeStr : credentialTypesLowerCase) {
            if (cacheKey.contains(CacheKeyValueDelegate.CACHE_VALUE_SEPARATOR + credentialTypeStr + CacheKeyValueDelegate.CACHE_VALUE_SEPARATOR)) {
                if (CredentialType.AccessToken.name().equalsIgnoreCase(credentialTypeStr)) {
                    type = CredentialType.AccessToken;
                    break;
                } else if (CredentialType.AccessToken_With_AuthScheme.name().equalsIgnoreCase(credentialTypeStr)) {
                    type = CredentialType.AccessToken_With_AuthScheme;
                    break;
                } else if (CredentialType.RefreshToken.name().equalsIgnoreCase(credentialTypeStr)) {
                    type = CredentialType.RefreshToken;
                    break;
                } else if (CredentialType.IdToken.name().equalsIgnoreCase(credentialTypeStr)) {
                    type = CredentialType.IdToken;
                    break;
                } else if (CredentialType.V1IdToken.name().equalsIgnoreCase(credentialTypeStr)) {
                    type = CredentialType.V1IdToken;
                    break;
                } else if (CredentialType.PrimaryRefreshToken.name().equalsIgnoreCase(credentialTypeStr)) {
                    type = CredentialType.PrimaryRefreshToken;
                    break;
                } else {
                    // TODO Log a warning and skip this value?
                    Logger.warn(methodTag, "Unexpected credential type.");
                }
            }
        }

        return type;
    }

    private static boolean isAccount(@NonNull final String cacheKey) {
        final String methodTag = TAG + ":isAccount";
        Logger.verbosePII(methodTag, "Evaluating cache key: [" + cacheKey + "]");
        boolean isAccount = null == getCredentialTypeForCredentialCacheKey(cacheKey);
        Logger.verbose(methodTag, "isAccount? [" + isAccount + "]");
        return isAccount;
    }

    private static boolean isCredential(@NonNull String cacheKey) {
        final String methodTag = TAG + ":isCredential";
        Logger.verbosePII(methodTag, "Evaluating cache key: [" + cacheKey + "]");
        boolean isCredential = null != getCredentialTypeForCredentialCacheKey(cacheKey);
        Logger.verbose(methodTag, "isCredential? [" + isCredential + "]");
        return isCredential;
    }
}
