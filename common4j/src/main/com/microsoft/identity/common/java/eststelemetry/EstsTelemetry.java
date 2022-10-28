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
package com.microsoft.identity.common.java.eststelemetry;

import com.microsoft.identity.common.java.commands.ICommand;
import com.microsoft.identity.common.java.commands.ICommandResult;
import com.microsoft.identity.common.java.exception.BaseException;
import com.microsoft.identity.common.java.exception.ServiceException;
import com.microsoft.identity.common.java.interfaces.INameValueStorage;
import com.microsoft.identity.common.java.interfaces.IPlatformComponents;
import com.microsoft.identity.common.java.logging.DiagnosticContext;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.result.ILocalAuthenticationResult;
import com.microsoft.identity.common.java.util.StringUtil;
import com.microsoft.identity.common.java.util.ported.InMemoryStorage;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.NonNull;

/**
 * Manages telemetry to be sent to ESTS via token requests.
 */
public class EstsTelemetry {
    private final static String TAG = EstsTelemetry.class.getSimpleName();

    /**
     * The name of the storage file on disk for the last request telemetry.
     */
    private static final String LAST_REQUEST_TELEMETRY_STORAGE_FILE =
            "com.microsoft.identity.client.last_request_telemetry";

    private static final String SUPPLEMENTAL_TELEMETRY_DATA_CACHE_FILE_NAME =
            "com.microsoft.identity.client.supplemental_telemetry_data_cache";

    private static volatile EstsTelemetry sEstsTelemetryInstance = null;
    private LastRequestTelemetryCache mLastRequestTelemetryCache;
    private final INameValueStorage<CurrentRequestTelemetry> mTelemetryMap;
    private final INameValueStorage<Set<FailedRequest>> mSentFailedRequests;

    /**
     * A supplemental cache that can used to store telemetry that is captured outside of the
     * DiagnosticContext. We have lots of code that is executed outside of a DiagnosticContext i.e.
     * ThreadLocal is not populated with a correlation Id. Since the current telemetry design is
     * strictly around DiagnosticContext, therefore we need this supplemental cache to capture these
     * fields that are emitted in code that is running outside that context. This fields are the
     * ones determined by {@link SchemaConstants#isOfflineEmitAllowedForThisField(String)}.
     */
    private INameValueStorage<String> mSupplementalTelemetryDataCache;

    EstsTelemetry() {
        this(new InMemoryStorage<CurrentRequestTelemetry>(),
                new InMemoryStorage<Set<FailedRequest>>());
    }

    // Exposed for testing only.
    EstsTelemetry(@NonNull final INameValueStorage<CurrentRequestTelemetry> telemetryMap,
                  @NonNull final INameValueStorage<Set<FailedRequest>> sentFailedRequestsMap) {
        mTelemetryMap = telemetryMap;
        mSentFailedRequests = sentFailedRequestsMap;
    }

    /**
     * Get an instance of {@link EstsTelemetry}. This method will return an existing
     * instance of EstsTelemetry or create and return a new instance if the existing instance is null.
     *
     * @return EstsTelemetry object instance
     */
    public static synchronized EstsTelemetry getInstance() {
        if (sEstsTelemetryInstance == null) {
            sEstsTelemetryInstance = new EstsTelemetry();
        }

        return sEstsTelemetryInstance;
    }

    //@VisibleForTesting
    public synchronized void clear() {
        mTelemetryMap.clear();
        mSentFailedRequests.clear();
        if (mLastRequestTelemetryCache != null) {
            mLastRequestTelemetryCache.clear();
        }
    }

    /**
     * Bootstrap an instance of {@link EstsTelemetry}.
     * Must be invoked prior to any operation on this object.
     */
    public synchronized void setUp(@NonNull final LastRequestTelemetryCache lastRequestTelemetryCache) {
        if (this.mLastRequestTelemetryCache == null) {
            this.mLastRequestTelemetryCache = lastRequestTelemetryCache;
        }
    }

    /**
     * Bootstrap an instance of {@link EstsTelemetry}.
     * Must be invoked prior to any operation on this object.
     */
    public synchronized void setUp(@NonNull final IPlatformComponents platformComponents) {
        if (this.mLastRequestTelemetryCache == null) {
            this.mLastRequestTelemetryCache = new LastRequestTelemetryCache(
                    platformComponents.getNameValueStore(LAST_REQUEST_TELEMETRY_STORAGE_FILE, String.class));
        }

        if (mSupplementalTelemetryDataCache == null) {
            mSupplementalTelemetryDataCache = platformComponents.getNameValueStore(
                    SUPPLEMENTAL_TELEMETRY_DATA_CACHE_FILE_NAME, String.class
            );
        }
    }

    /**
     * Creates an entry for a Current Telemetry object for the passed in command based on whether
     * the command is eligible for telemetry. Saves the telemetry object to telemetry map.
     *
     * @param command The command for which to capture telemetry
     */
    public void initTelemetryForCommand(@NonNull final ICommand<?> command) {
        if (command.isEligibleForEstsTelemetry()) {
            final CurrentRequestTelemetry currentRequestTelemetry = new CurrentRequestTelemetry();
            mTelemetryMap.put(command.getCorrelationId(), currentRequestTelemetry);
            mSentFailedRequests.put(command.getCorrelationId(), new HashSet<FailedRequest>());
        }
    }

    /**
     * Emit multiple telemetry fields by passing a map of telemetry fields.
     * The fields will be saved in {@link RequestTelemetry} object associated to the current request.
     *
     * @param telemetry a map containing telemetry fields and their values
     */
    public void emit(@Nullable final Map<String, String> telemetry) {
        if (telemetry == null) {
            return;
        }

        for (Map.Entry<String, String> entry : telemetry.entrySet()) {
            emit(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Emit the provided telemetry field. The field will be saved in {@link RequestTelemetry} object
     * associated to the current request.
     *
     * @param key   the key associated to the telemetry field
     * @param value the value associated to the telemetry field
     */
    public void emit(@Nullable final String key, final String value) {
        if (StringUtil.isNullOrEmpty(key)) {
            return;
        }

        final String correlationId = DiagnosticContext.INSTANCE.getRequestContext().get(DiagnosticContext.CORRELATION_ID);
        final String compliantValueString = TelemetryUtils.getSchemaCompliantString(value);
        final CurrentRequestTelemetry currentTelemetryInstance = getCurrentTelemetryInstance(correlationId);
        if (currentTelemetryInstance != null) {
            currentTelemetryInstance.put(key, compliantValueString);
        } else {
            emitToSupplementalTelemetryCache(key, compliantValueString);
        }
    }

    private synchronized void emitToSupplementalTelemetryCache(@NonNull final String key, final String value) {
        if (mSupplementalTelemetryDataCache != null && SchemaConstants.isOfflineEmitAllowedForThisField(key)) {
            mSupplementalTelemetryDataCache.put(key, value);
        }
    }

    /**
     * Emit the provided telemetry field. The field will be saved in {@link RequestTelemetry} object
     * associated to the current request.
     *
     * @param key   the key associated to the telemetry field
     * @param value the value associated to the telemetry field
     */
    public void emit(@Nullable final String key, final int value) {
        emit(key, String.valueOf(value));
    }

    /**
     * Emit the provided telemetry field. The field will be saved in {@link RequestTelemetry} object
     * associated to the current request.
     *
     * @param key   the key associated to the telemetry field
     * @param value the value associated to the telemetry field
     */
    public void emit(@Nullable final String key, final long value) {
        emit(key, String.valueOf(value));
    }

    /**
     * Emit the provided telemetry field. The field will be saved in {@link RequestTelemetry} object
     * associated to the current request.
     *
     * @param key   the key associated to the telemetry field
     * @param value the value associated to the telemetry field
     */
    public void emit(@Nullable final String key, final boolean value) {
        emit(key, TelemetryUtils.getSchemaCompliantStringFromBoolean(value));
    }

    /**
     * Emit the ApiId for the current request. The field will be saved in {@link RequestTelemetry}
     * object associated to the current request.
     *
     * @param apiId the api id to emit to telemetry
     */
    public void emitApiId(@Nullable final String apiId) {
        emit(SchemaConstants.Key.API_ID, apiId);
    }

    /**
     * Emit the forceRefresh value for the current request. The field will be saved in
     * {@link RequestTelemetry} object associated to the current request.
     *
     * @param forceRefresh the force refresh value to emit to telemetry
     */
    public void emitForceRefresh(final boolean forceRefresh) {
        String val = TelemetryUtils.getSchemaCompliantStringFromBoolean(forceRefresh);
        emit(SchemaConstants.Key.FORCE_REFRESH, val);
    }

    /**
     * Flush the telemetry data for the current request to the storage using the {@link LastRequestTelemetry}.
     * Removes the telemetry associated to the correlation id from the telemetry map,
     * and saves it to the cache (SharedPreferences) as the last request telemetry.
     */
    public synchronized void flush(@NonNull final ICommand<?> command,
                                   @NonNull final ICommandResult commandResult) {
        final String methodName = ":flush";

        final String correlationId = command.getCorrelationId();
        if (correlationId == null) {
            Logger.info(TAG + methodName, "correlation ID is null. Nothing to flush.");
            return;
        }

        final CurrentRequestTelemetry currentTelemetry = mTelemetryMap.get(correlationId);
        if (currentTelemetry == null) {
            Logger.info(TAG + methodName, "currentTelemetry is null. Nothing to flush.");
            return;
        }

        // load the last request object from cache
        LastRequestTelemetry lastRequestTelemetry = loadLastRequestTelemetryFromCache();

        // We did not have a last request object in cache, let's create a new one and copySharedValues
        // fields from current request where applicable
        if (lastRequestTelemetry == null) {
            lastRequestTelemetry = new LastRequestTelemetry(currentTelemetry.getSchemaVersion());
            lastRequestTelemetry = (LastRequestTelemetry) lastRequestTelemetry.copySharedValues(currentTelemetry);
        }

        if (isTelemetryLoggedByServer(command, commandResult)) {
            // telemetry headers have been sent to token endpoint and logged by sts
            // this is the time to reset local telemetry state

            // reset silent successful count as we just went to token endpoint
            lastRequestTelemetry.resetSilentSuccessCount();

            // get the failed request set for this request. This includes all failed request
            // data that has been sent to STS in this request.
            Set<FailedRequest> failedRequestSentSet = mSentFailedRequests.get(correlationId);

            // headers have been logged by sts - we don't need to hold on to this data - let's wipe
            lastRequestTelemetry.wipeFailedRequestAndErrorForSubList(failedRequestSentSet);

            if (mSupplementalTelemetryDataCache != null) {
                // headers have been logged by sts - we don't need to hold on to this data - let's wipe
                mSupplementalTelemetryDataCache.clear();
            }
        }

        // get the error encountered during execution of this command
        final String errorCode = getErrorCodeFromCommandResult(commandResult);

        if (errorCode != null) {
            // we have an error, let's append it to the list
            lastRequestTelemetry.appendFailedRequest(
                    currentTelemetry.getApiId(),
                    correlationId,
                    errorCode);
        } else if (commandResult.getResult() != null &&
                commandResult.getResult() instanceof ILocalAuthenticationResult) {
            final ILocalAuthenticationResult localAuthenticationResult = (ILocalAuthenticationResult) commandResult.getResult();
            if (localAuthenticationResult.isServicedFromCache()) {
                // we returned a token from cache, let's increment the silent success count
                lastRequestTelemetry.incrementSilentSuccessCount();
            }
        } // else leave everything as is

        // we're done processing telemetry for this command, let's remove it from the map
        mTelemetryMap.remove(correlationId);
        mSentFailedRequests.remove(correlationId);

        if (mLastRequestTelemetryCache != null) {
            // save the (updated) telemetry object back to telemetry cache
            mLastRequestTelemetryCache.saveRequestTelemetryToCache(lastRequestTelemetry);
        } else {
            Logger.warn(
                    TAG + methodName,
                    "Last Request Telemetry Cache object was null. " +
                            "Unable to save request telemetry to cache."
            );
        }
    }

    /**
     * Loads the last request telemetry instance from cache.
     **/
    @Nullable
    private LastRequestTelemetry loadLastRequestTelemetryFromCache() {
        final String methodName = ":loadLastRequestTelemetry";

        if (mLastRequestTelemetryCache == null) {
            Logger.verbose(
                    TAG + methodName,
                    "Last Request Telemetry Cache has not been initialized. " +
                            "Cannot load Last Request Telemetry data from cache."
            );
            return null;
        }

        return mLastRequestTelemetryCache.getRequestTelemetryFromCache();
    }

    /**
     * Extracts an error code from the given command result, if the result is an ERROR.
     **/
    @Nullable
    private String getErrorCodeFromCommandResult(@NonNull final ICommandResult commandResult) {
        if (commandResult.getStatus() == ICommandResult.ResultStatus.ERROR) {
            final BaseException baseException = (BaseException) commandResult.getResult();
            return baseException.getErrorCode();
        } else if (commandResult.getStatus() == ICommandResult.ResultStatus.CANCEL) {
            return "user_cancel";
        } else {
            return null;
        }
    }

    /**
     * Returns true if the telemetry associated to the given command has been logged by eSTS.
     **/
    private boolean isTelemetryLoggedByServer(@NonNull final ICommand<?> command,
                                              @NonNull final ICommandResult commandResult) {
        // This was a local operation - we didn't reach token endpoint and hence telemetry wasn't sent
        if (!command.willReachTokenEndpoint()) {
            return false;
        }

        if (commandResult.getStatus() == ICommandResult.ResultStatus.ERROR) {
            BaseException baseException = (BaseException) commandResult.getResult();
            if (!(baseException instanceof ServiceException)) {
                // Telemetry not logged by server as the exception is a local exception
                // (request did not reach token endpoint)
                return false;
            } else {
                final ServiceException serviceException = (ServiceException) baseException;
                final int statusCode = serviceException.getHttpStatusCode();
                // for these status codes, headers aren't logged by ests
                return !(statusCode == ServiceException.DEFAULT_STATUS_CODE ||
                        statusCode == 429 ||
                        statusCode >= 500);
            }
        } else if (commandResult.getStatus() == ICommandResult.ResultStatus.CANCEL) {
            // we did not go to token endpoint
            return false;
        } else if (commandResult.getStatus() == ICommandResult.ResultStatus.COMPLETED) {
            if (commandResult.getResult() instanceof ILocalAuthenticationResult) {
                final ILocalAuthenticationResult localAuthenticationResult = (ILocalAuthenticationResult) commandResult.getResult();
                if (localAuthenticationResult.isServicedFromCache()) {
                    // we did not go to token endpoint
                    return false;
                }
            } else {
                // command probably wasn't a token command - we should never get here in that case
                return false;
            }
        }

        // if we get here that means we went to token endpoint and headers were logged by sts
        return true;
    }

    /**
     * Returns a header string from the "Current Telemetry instance" for the eSTS Telemetry.
     */
    @Nullable
    private String getCurrentTelemetryHeaderString() {
        final String methodName = ":getCurrentTelemetryHeaderString";

        final String correlationId = DiagnosticContext.INSTANCE.getRequestContext().get(DiagnosticContext.CORRELATION_ID);
        if (correlationId == null) {
            Logger.warn(TAG + methodName, "correlation ID is null.");
            return null;
        }

        final RequestTelemetry currentTelemetry = mTelemetryMap.get(correlationId);
        if (currentTelemetry == null) {
            Logger.warn(TAG + methodName, "currentTelemetry for correlation ID:" +
                    correlationId + " is null.");
            return null;
        }

        // we are collecting telemetry so now add these to primary Map
        addFromSupplementalTelemetryToCurrentTelemetry();

        return currentTelemetry.getCompleteHeaderString();
    }

    private synchronized void addFromSupplementalTelemetryToCurrentTelemetry() {
        if (mSupplementalTelemetryDataCache != null) {
            EstsTelemetry.getInstance().emit(mSupplementalTelemetryDataCache.getAll());
        }
    }

    /**
     * Returns a header string from the "Last Request Telemetry instance" for the eSTS Telemetry.
     */
    @Nullable
    private synchronized String getLastTelemetryHeaderString() {
        final String methodName = ":getLastTelemetryHeaderString";

        if (mLastRequestTelemetryCache == null) {
            Logger.warn(TAG + methodName, "mLastRequestTelemetryCache is null.");
            return null;
        }

        final String correlationId = DiagnosticContext.INSTANCE.getRequestContext().get(DiagnosticContext.CORRELATION_ID);
        if (correlationId == null) {
            Logger.warn(TAG + methodName, "correlation ID is null.");
            return null;
        }

        final LastRequestTelemetry lastRequestTelemetryFromCache = mLastRequestTelemetryCache.getRequestTelemetryFromCache();
        if (lastRequestTelemetryFromCache == null) {
            // we did not have anything in the telemetry cache for the last request
            // let's create a new object based on the data available from the current request object
            // and return the header string formed via that object
            final CurrentRequestTelemetry currentRequestTelemetry = mTelemetryMap.get(correlationId);
            if (currentRequestTelemetry == null) {
                Logger.warn(TAG + methodName, "currentTelemetry for correlation ID:" +
                        correlationId + " is null.");
                return null;
            }

            // We're trying to send this.. so that if we ever come across this field being null on the server
            // then we know you have a bug in the client.
            final LastRequestTelemetry lastRequestTelemetry = new LastRequestTelemetry(currentRequestTelemetry.getSchemaVersion());
            lastRequestTelemetry.copySharedValues(currentRequestTelemetry);
            lastRequestTelemetry.putInPlatformTelemetry(
                    SchemaConstants.Key.ALL_TELEMETRY_DATA_SENT,
                    SchemaConstants.Value.TRUE
            );
            return lastRequestTelemetry.getCompleteHeaderString();
        }

        // create a copy of the object retrieved from cache
        final LastRequestTelemetry lastRequestTelemetryCopy = new LastRequestTelemetry(lastRequestTelemetryFromCache.getSchemaVersion());
        lastRequestTelemetryCopy.copySharedValues(lastRequestTelemetryFromCache);

        // failed request data from the object retrieved from cache
        final List<FailedRequest> originalFailedRequests = lastRequestTelemetryFromCache.getFailedRequests();

        // get the failed request set for the failed request data that we attempt to send in header
        // as part of this request
        final Set<FailedRequest> failedRequestSentSet = mSentFailedRequests.get(correlationId);

        boolean isAllDataSentInHeader = true;
        for (int i = 0; i < originalFailedRequests.size(); i++) {
            // there is a limit of 8KB for the payload sent in request headers
            // we will be maxing out at 4KB to avoid HTTP 413 errors
            // check if we have enough space in the String to store another failed request/error element
            // if yes, then add it to the failed request array (for the copy)
            if (lastRequestTelemetryCopy.getCompleteHeaderString().length() < SchemaConstants.HEADER_DATA_LIMIT) {
                final FailedRequest failedRequest = originalFailedRequests.get(i);
                lastRequestTelemetryCopy.appendFailedRequest(failedRequest);

                // we have attempted to send these failed requests/errors to the server
                if (failedRequestSentSet != null) {
                    failedRequestSentSet.add(failedRequest);
                }
            } else {
                isAllDataSentInHeader = false;
                // if there is no room for more data, then break out of this loop
                break;
            }
        }

        final String isAllDataSentString = TelemetryUtils.getSchemaCompliantStringFromBoolean(isAllDataSentInHeader);

        lastRequestTelemetryCopy.putInPlatformTelemetry(
                SchemaConstants.Key.ALL_TELEMETRY_DATA_SENT,
                isAllDataSentString
        );

        // return the header string formed for the copy element
        return lastRequestTelemetryCopy.getCompleteHeaderString();
    }

    /**
     * Get the headers for the eSTS Telemetry.
     * These headers can be attached to the requests made to the eSTS.
     *
     * @return a map containing telemetry headers and their values
     */
    @NonNull
    public Map<String, String> getTelemetryHeaders() {
        final String methodName = ":getTelemetryHeaders";

        final Map<String, String> headerMap = new HashMap<>();

        if (!isCurrentTelemetryAvailable()) {
            return headerMap;
        }

        final String currentHeader = getCurrentTelemetryHeaderString();
        final String lastHeader = getLastTelemetryHeaderString();

        if (currentHeader != null) {
            headerMap.put(SchemaConstants.CURRENT_REQUEST_HEADER_NAME, currentHeader);
        } else {
            Logger.verbose(
                    TAG + methodName,
                    "Current Request Telemetry Header is null"
            );
        }

        if (lastHeader != null) {
            headerMap.put(SchemaConstants.LAST_REQUEST_HEADER_NAME, lastHeader);
        } else {
            Logger.verbose(
                    TAG + methodName,
                    "Last Request Telemetry Header is null"
            );
        }

        return Collections.unmodifiableMap(headerMap);
    }

    /**
     * Returns true if there exists a telemetry instance associated to the current correlation ID.
     **/
    private boolean isCurrentTelemetryAvailable() {
        final String correlationId = DiagnosticContext.INSTANCE.getRequestContext().get(DiagnosticContext.CORRELATION_ID);
        final CurrentRequestTelemetry currentRequestTelemetry = getCurrentTelemetryInstance(correlationId);
        return currentRequestTelemetry != null;
    }

    /**
     * Loads telemetry instance associated to the current correlation ID.
     **/
    @Nullable
    private CurrentRequestTelemetry getCurrentTelemetryInstance(final String correlationId) {
        if (mTelemetryMap == null || correlationId == null || correlationId.equals("UNSET")) {
            return null;
        }

        return mTelemetryMap.get(correlationId);
    }
}
