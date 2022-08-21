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
import com.microsoft.identity.common.java.logging.DiagnosticContext;
import com.microsoft.identity.common.java.result.ILocalAuthenticationResult;
import com.microsoft.identity.common.java.telemetry.TelemetryEventStrings;
import com.microsoft.identity.common.java.util.ported.InMemoryStorage;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.NonNull;

import static com.microsoft.identity.common.java.eststelemetry.LastRequestTelemetry.FAILED_REQUEST_CAP;
import static com.microsoft.identity.common.java.eststelemetry.LastRequestTelemetryCache.LAST_TELEMETRY_HEADER_STRING_CACHE_KEY;
import static com.microsoft.identity.common.java.eststelemetry.LastRequestTelemetryCache.LAST_TELEMETRY_OBJECT_CACHE_KEY;
import static com.microsoft.identity.common.java.eststelemetry.LastRequestTelemetryCache.LAST_TELEMETRY_SCHEMA_VERSION_CACHE_KEY;
import static com.microsoft.identity.common.java.eststelemetry.SchemaConstants.CURRENT_REQUEST_HEADER_NAME;
import static com.microsoft.identity.common.java.eststelemetry.SchemaConstants.Key.API_ID;
import static com.microsoft.identity.common.java.eststelemetry.SchemaConstants.LAST_REQUEST_HEADER_NAME;
import static com.microsoft.identity.common.java.logging.DiagnosticContext.CORRELATION_ID;

public class EstsTelemetryTest {

    final String correlationId = "SOME_CORRELATION_ID";
    final String apiId = "API_ID";
    final String errorCode = "ERROR_CODE";

    @After
    public void tearDown() {
        DiagnosticContext.INSTANCE.getRequestContext().clear();
    }

    @Test
    public void testEmitWithUnsetCorrelationId() {
        final InMemoryStorage<CurrentRequestTelemetry> inMemoryTelemetryMap = new InMemoryStorage<>();
        final EstsTelemetry telemetry = getTelemetry(inMemoryTelemetryMap, null, null);

        telemetry.emitApiId(apiId);
        Assert.assertEquals(inMemoryTelemetryMap.size(), 0);
    }

    @Test
    public void testEmitApiId() {
        final InMemoryStorage<CurrentRequestTelemetry> inMemoryTelemetryMap = new InMemoryStorage<>();
        final EstsTelemetry telemetry = getTelemetry(inMemoryTelemetryMap, null, null);

        telemetry.initTelemetryForCommand(
                MockCommand.builder()
                        .correlationId(correlationId)
                        .build());

        DiagnosticContext.INSTANCE.getRequestContext().put(CORRELATION_ID, correlationId);
        telemetry.emitApiId(apiId);

        Assert.assertEquals(inMemoryTelemetryMap.size(), 1);
        Assert.assertNotNull(inMemoryTelemetryMap.get(correlationId));
        Assert.assertEquals(apiId, inMemoryTelemetryMap.get(correlationId).getApiId());
    }

    @Test
    public void testEmitPlatformParameters() {
        DiagnosticContext.INSTANCE.getRequestContext().put(CORRELATION_ID, correlationId);
        final EstsTelemetry telemetry = getTelemetry(null, null, new InMemoryStorage());

        final ICommand<Boolean> mockCommand = MockCommand.builder()
                .correlationId(correlationId)
                .isEligibleForEstsTelemetry(true)
                .willReachTokenEndpoint(true)
                .build();

        telemetry.initTelemetryForCommand(mockCommand);
        telemetry.emitApiId(apiId);
        telemetry.emitForceRefresh(true);

        final ConcurrentHashMap<String, String> mProperties = new ConcurrentHashMap<>();
        mProperties.put(TelemetryEventStrings.Key.AT_STATUS, TelemetryEventStrings.Value.TRUE);
        mProperties.put(TelemetryEventStrings.Key.MRRT_STATUS, TelemetryEventStrings.Value.FALSE);
        mProperties.put(TelemetryEventStrings.Key.RT_STATUS, TelemetryEventStrings.Value.TRUE);
        mProperties.put(TelemetryEventStrings.Key.FRT_STATUS, TelemetryEventStrings.Value.FALSE);
        mProperties.put(TelemetryEventStrings.Key.ID_TOKEN_STATUS, TelemetryEventStrings.Value.FALSE);
        mProperties.put(TelemetryEventStrings.Key.ACCOUNT_STATUS, TelemetryEventStrings.Value.TRUE);
        telemetry.emit(mProperties);

        final Map<String, String> headers = telemetry.getTelemetryHeaders();

        Assert.assertEquals(2, headers.size());
        Assert.assertEquals("2|" + apiId + ",1|2,,,,,,,1,0,1,1,0,0", headers.get(CURRENT_REQUEST_HEADER_NAME));
        Assert.assertEquals("2|0|||2,1", headers.get(LAST_REQUEST_HEADER_NAME));
    }

    @Test
    public void testFlushSuccessResultFromCache() {
        final ICommand<Boolean> mockCommand = MockCommand.builder()
                .correlationId(correlationId)
                .build();
        final ILocalAuthenticationResult successResult = MockAuthenticationResult.builder()
                .isServicedFromCache(true)
                .build();
        final ICommandResult mockCommandResult =
                MockCommandResult.<ILocalAuthenticationResult>builder()
                        .correlationId(correlationId)
                        .result(successResult)
                        .resultStatus(ICommandResult.ResultStatus.COMPLETED)
                        .build();

        final InMemoryStorage<CurrentRequestTelemetry> inMemoryTelemetryMap = new InMemoryStorage<>();
        final CurrentRequestTelemetry currentRequestTelemetry = new CurrentRequestTelemetry();
        currentRequestTelemetry.put(API_ID, apiId);
        inMemoryTelemetryMap.put(correlationId, currentRequestTelemetry);

        final InMemoryStorage<String> lastRequestTelemetryMap = new InMemoryStorage<>();
        flush(mockCommand, mockCommandResult, inMemoryTelemetryMap, null, lastRequestTelemetryMap);

        Assert.assertEquals(lastRequestTelemetryMap.size(), 3);
        Assert.assertEquals(lastRequestTelemetryMap.get(LAST_TELEMETRY_SCHEMA_VERSION_CACHE_KEY), "2");
        Assert.assertEquals(lastRequestTelemetryMap.get(LAST_TELEMETRY_HEADER_STRING_CACHE_KEY), "2|1|||2,");
        Assert.assertEquals(lastRequestTelemetryMap.get(LAST_TELEMETRY_OBJECT_CACHE_KEY),
                "{\"silent_successful_count\":1,\"failed_requests\":[],\"schema_version\":\"2\",\"platform_telemetry\":{}}");
    }

    @Test
    public void testFlushSuccessResultFromCache_NoCurrentRequestTelemetry() {
        final ICommand<Boolean> mockCommand = MockCommand.builder()
                .correlationId(correlationId)
                .build();
        final ILocalAuthenticationResult successResult = MockAuthenticationResult.builder()
                .isServicedFromCache(true)
                .build();
        final ICommandResult mockCommandResult =
                MockCommandResult.<ILocalAuthenticationResult>builder()
                        .correlationId(correlationId)
                        .result(successResult)
                        .resultStatus(ICommandResult.ResultStatus.COMPLETED)
                        .build();

        final InMemoryStorage<String> lastRequestTelemetryMap = new InMemoryStorage<>();
        flush(mockCommand, mockCommandResult, null, null, lastRequestTelemetryMap);
        Assert.assertEquals(lastRequestTelemetryMap.size(), 0);
    }

    @Test
    public void testFlushSuccessResultFromCache_LastRequestTelemetryCacheNotInitiated() {
        final ICommand<Boolean> mockCommand = MockCommand.builder()
                .correlationId(correlationId)
                .build();
        final ILocalAuthenticationResult successResult = MockAuthenticationResult.builder()
                .isServicedFromCache(true)
                .build();
        final ICommandResult mockCommandResult =
                MockCommandResult.<ILocalAuthenticationResult>builder()
                        .correlationId(correlationId)
                        .result(successResult)
                        .resultStatus(ICommandResult.ResultStatus.COMPLETED)
                        .build();

        final InMemoryStorage<CurrentRequestTelemetry> inMemoryTelemetryMap = new InMemoryStorage<>();
        final CurrentRequestTelemetry currentRequestTelemetry = new CurrentRequestTelemetry();
        currentRequestTelemetry.put(API_ID, apiId);
        inMemoryTelemetryMap.put(correlationId, currentRequestTelemetry);

        flush(mockCommand, mockCommandResult, inMemoryTelemetryMap, null, null);

        // Good enough as long as it doesn't crash.
    }

    @Test
    public void testFlushSuccessFailureResult() {
        final ICommand<Boolean> mockCommand = MockCommand.builder()
                .correlationId(correlationId)
                .build();

        final BaseException exception = new ServiceException(
                "ERROR_CODE",
                "ERROR_MESSAGE",
                null);

        final ICommandResult mockCommandResult =
                MockCommandResult.<BaseException>builder()
                        .correlationId(correlationId)
                        .result(exception)
                        .resultStatus(ICommandResult.ResultStatus.ERROR)
                        .build();

        final InMemoryStorage<CurrentRequestTelemetry> inMemoryTelemetryMap = new InMemoryStorage<>();
        final CurrentRequestTelemetry currentRequestTelemetry = new CurrentRequestTelemetry();
        currentRequestTelemetry.put(API_ID, apiId);
        inMemoryTelemetryMap.put(correlationId, currentRequestTelemetry);

        final InMemoryStorage<String> lastRequestTelemetryMap = new InMemoryStorage<>();
        flush(mockCommand, mockCommandResult, inMemoryTelemetryMap, null, lastRequestTelemetryMap);

        Assert.assertEquals(lastRequestTelemetryMap.size(), 3);
        Assert.assertEquals(lastRequestTelemetryMap.get(LAST_TELEMETRY_SCHEMA_VERSION_CACHE_KEY), "2");
        Assert.assertEquals(lastRequestTelemetryMap.get(LAST_TELEMETRY_HEADER_STRING_CACHE_KEY),
                "2|0|" + apiId + "," + correlationId + "|" + errorCode + "|2,");
        Assert.assertEquals(lastRequestTelemetryMap.get(LAST_TELEMETRY_OBJECT_CACHE_KEY),
                "{\"silent_successful_count\":0,\"failed_requests\":[{\"mApiId\":\"" + apiId +
                        "\",\"mCorrelationId\":\"" + correlationId +
                        "\",\"mError\":\"" + exception.getErrorCode() + "\"}]," +
                        "\"schema_version\":\"2\",\"platform_telemetry\":{}}");
    }

    @Test
    public void testFlushSuccessServiceExceptionFailureResult_ReachTokenEndpoint() {
        final ICommand<Boolean> mockCommand = MockCommand.builder()
                .correlationId(correlationId)
                .willReachTokenEndpoint(true)
                .build();

        final ServiceException exception = new ServiceException(
                "ERROR_CODE",
                "ERROR_MESSAGE",
                400,
                null);

        final ICommandResult mockCommandResult =
                MockCommandResult.<ServiceException>builder()
                        .correlationId(correlationId)
                        .result(exception)
                        .resultStatus(ICommandResult.ResultStatus.ERROR)
                        .build();

        final InMemoryStorage<String> lastRequestTelemetryMap = new InMemoryStorage<>();
        lastRequestTelemetryMap.put(LAST_TELEMETRY_OBJECT_CACHE_KEY,
                "{\"silent_successful_count\":5,\"failed_requests\":[" +
                        "{\"mApiId\":\"API_1\",\"mCorrelationId\":\"COL_ID_1\",\"mError\":\"ERR_1\"}," +
                        "{\"mApiId\":\"API_2\",\"mCorrelationId\":\"COL_ID_2\",\"mError\":\"ERR_2\"}," +
                        "{\"mApiId\":\"API_3\",\"mCorrelationId\":\"COL_ID_3\",\"mError\":\"ERR_3\"}," +
                        "{\"mApiId\":\"API_4\",\"mCorrelationId\":\"COL_ID_4\",\"mError\":\"ERR_4\"}," +
                        "{\"mApiId\":\"API_5\",\"mCorrelationId\":\"COL_ID_5\",\"mError\":\"ERR_5\"}]," +
                        "\"schema_version\":\"2\",\"platform_telemetry\":{}}");

        final InMemoryStorage<Set<FailedRequest>> sentFailedRequestsMap = new InMemoryStorage<>();
        final HashSet<FailedRequest> failedRequestSet = new HashSet<>();
        failedRequestSet.add(new FailedRequest("API_1", "COL_ID_1", "ERR_1"));
        failedRequestSet.add(new FailedRequest("API_2", "COL_ID_2", "ERR_2"));
        failedRequestSet.add(new FailedRequest("API_3", "COL_ID_3", "ERR_3"));
        failedRequestSet.add(new FailedRequest("API_4", "COL_ID_4", "ERR_4"));
        failedRequestSet.add(new FailedRequest("API_5", "COL_ID_5", "ERR_5"));
        sentFailedRequestsMap.put(correlationId, failedRequestSet);

        final InMemoryStorage<CurrentRequestTelemetry> inMemoryTelemetryMap = new InMemoryStorage<>();
        final CurrentRequestTelemetry currentRequestTelemetry = new CurrentRequestTelemetry();
        currentRequestTelemetry.put(API_ID, apiId);
        inMemoryTelemetryMap.put(correlationId, currentRequestTelemetry);

        flush(mockCommand, mockCommandResult, inMemoryTelemetryMap, sentFailedRequestsMap, lastRequestTelemetryMap);

        Assert.assertEquals(lastRequestTelemetryMap.size(), 3);
        Assert.assertEquals(lastRequestTelemetryMap.get(LAST_TELEMETRY_SCHEMA_VERSION_CACHE_KEY), "2");
        Assert.assertEquals(lastRequestTelemetryMap.get(LAST_TELEMETRY_HEADER_STRING_CACHE_KEY),
                "2|0|" + apiId + "," + correlationId + "|" + errorCode + "|2,");
        Assert.assertEquals(lastRequestTelemetryMap.get(LAST_TELEMETRY_OBJECT_CACHE_KEY),
                "{\"silent_successful_count\":0,\"failed_requests\":[{\"mApiId\":\"" + apiId +
                        "\",\"mCorrelationId\":\"" + correlationId +
                        "\",\"mError\":\"" + exception.getErrorCode() + "\"}]," +
                        "\"schema_version\":\"2\",\"platform_telemetry\":{}}");
    }

    @Test
    public void testGetTelemetryHeaders_NoCurrentTelemetry() {
        DiagnosticContext.INSTANCE.getRequestContext().put(CORRELATION_ID, correlationId);

        final EstsTelemetry telemetry = getTelemetry(null, null, null);

        final Map<String, String> headers = telemetry.getTelemetryHeaders();
        Assert.assertTrue(headers.isEmpty());
    }

    @Test
    public void testGetTelemetryHeaders_CorrelationIdNotSet() {
        DiagnosticContext.INSTANCE.getRequestContext().clear();

        final InMemoryStorage<CurrentRequestTelemetry> inMemoryTelemetryMap = new InMemoryStorage<>();
        final CurrentRequestTelemetry currentRequestTelemetry = new CurrentRequestTelemetry();
        currentRequestTelemetry.put(API_ID, apiId);
        inMemoryTelemetryMap.put(correlationId, currentRequestTelemetry);

        final EstsTelemetry telemetry = getTelemetry(inMemoryTelemetryMap, null, new InMemoryStorage<String>());

        final Map<String, String> headers = telemetry.getTelemetryHeaders();
        Assert.assertTrue(headers.isEmpty());
    }

    @Test
    public void testGetTelemetryHeaders_EmptyLastTelemetryCache() {
        DiagnosticContext.INSTANCE.getRequestContext().put(CORRELATION_ID, correlationId);

        final InMemoryStorage<CurrentRequestTelemetry> inMemoryTelemetryMap = new InMemoryStorage<>();
        final CurrentRequestTelemetry currentRequestTelemetry = new CurrentRequestTelemetry();
        currentRequestTelemetry.put(API_ID, apiId);
        inMemoryTelemetryMap.put(correlationId, currentRequestTelemetry);

        final EstsTelemetry telemetry = getTelemetry(inMemoryTelemetryMap, null, new InMemoryStorage<String>());

        final Map<String, String> headers = telemetry.getTelemetryHeaders();

        Assert.assertEquals(2, headers.size());
        Assert.assertEquals("2|API_ID,0|2,,,,,,,,,,,,", headers.get(CURRENT_REQUEST_HEADER_NAME));
        Assert.assertEquals("2|0|||2,1", headers.get(LAST_REQUEST_HEADER_NAME));
    }

    @Test
    public void testGetTelemetryHeaders_WithLastTelemetryCache() {
        DiagnosticContext.INSTANCE.getRequestContext().put(CORRELATION_ID, correlationId);

        final InMemoryStorage<CurrentRequestTelemetry> inMemoryTelemetryMap = new InMemoryStorage<>();
        final CurrentRequestTelemetry currentRequestTelemetry = new CurrentRequestTelemetry();
        currentRequestTelemetry.put(API_ID, apiId);
        inMemoryTelemetryMap.put(correlationId, currentRequestTelemetry);

        final InMemoryStorage<String> lastRequestTelemetryMap = new InMemoryStorage<>();
        lastRequestTelemetryMap.put(LAST_TELEMETRY_OBJECT_CACHE_KEY,
                "{\"silent_successful_count\":5,\"failed_requests\":[" +
                        "{\"mApiId\":\"API_1\",\"mCorrelationId\":\"COL_ID_1\",\"mError\":\"ERR_1\"}," +
                        "{\"mApiId\":\"API_2\",\"mCorrelationId\":\"COL_ID_2\",\"mError\":\"ERR_2\"}," +
                        "{\"mApiId\":\"API_3\",\"mCorrelationId\":\"COL_ID_3\",\"mError\":\"ERR_3\"}," +
                        "{\"mApiId\":\"API_4\",\"mCorrelationId\":\"COL_ID_4\",\"mError\":\"ERR_4\"}," +
                        "{\"mApiId\":\"API_5\",\"mCorrelationId\":\"COL_ID_5\",\"mError\":\"ERR_5\"}]," +
                        "\"schema_version\":\"2\",\"platform_telemetry\":{}}");

        final EstsTelemetry telemetry = getTelemetry(inMemoryTelemetryMap, null, lastRequestTelemetryMap);

        final Map<String, String> headers = telemetry.getTelemetryHeaders();

        Assert.assertEquals(2, headers.size());
        Assert.assertEquals("2|API_ID,0|2,,,,,,,,,,,,", headers.get(CURRENT_REQUEST_HEADER_NAME));
        Assert.assertEquals("2|5|API_1,COL_ID_1,API_2,COL_ID_2,API_3,COL_ID_3,API_4,COL_ID_4,API_5,COL_ID_5|ERR_1,ERR_2,ERR_3,ERR_4,ERR_5|2,1",
                headers.get(LAST_REQUEST_HEADER_NAME));
    }

    @Test
    public void testLastRequestTelemetryFailedRequestListIsCapped() {
        final LastRequestTelemetry lastRequestTelemetry = new LastRequestTelemetry(
                SchemaConstants.CURRENT_SCHEMA_VERSION
        );

        for (int i = 0; i < (FAILED_REQUEST_CAP + 20); i++) {
            final String correlation = UUID.randomUUID().toString();
            final FailedRequest failedRequest = new FailedRequest(apiId, correlation, errorCode);
            lastRequestTelemetry.appendFailedRequest(failedRequest);
            final List<FailedRequest> failedRequests = lastRequestTelemetry.getFailedRequests();
            Assert.assertTrue(failedRequests.size() <= FAILED_REQUEST_CAP);
        }

        Assert.assertEquals(FAILED_REQUEST_CAP, lastRequestTelemetry.getFailedRequests().size());
    }

    @Test
    public void testSendSubsequentCachedCommand() {
        DiagnosticContext.INSTANCE.getRequestContext().put(CORRELATION_ID, correlationId);

        final EstsTelemetry telemetry = getTelemetry(null, null, new InMemoryStorage());

        final ICommand<Boolean> mockCommand = MockCommand.builder()
                .correlationId(correlationId)
                .isEligibleForEstsTelemetry(true)
                .willReachTokenEndpoint(true)
                .build();

        final ILocalAuthenticationResult cachedSuccessResult = MockAuthenticationResult.builder()
                .isServicedFromCache(true)
                .build();

        final ICommandResult mockCommandResult =
                MockCommandResult.<ILocalAuthenticationResult>builder()
                        .correlationId(correlationId)
                        .result(cachedSuccessResult)
                        .resultStatus(ICommandResult.ResultStatus.COMPLETED)
                        .build();

        // First request. cached.
        telemetry.initTelemetryForCommand(mockCommand);
        telemetry.emitApiId(apiId);
        telemetry.emitForceRefresh(false);
        telemetry.flush(mockCommand, mockCommandResult);

        // 2nd Request. cached.
        telemetry.initTelemetryForCommand(mockCommand);
        telemetry.emitApiId(apiId);
        telemetry.emitForceRefresh(false);
        telemetry.flush(mockCommand, mockCommandResult);

        // 3rd request.
        telemetry.initTelemetryForCommand(mockCommand);
        telemetry.emitApiId(apiId);
        telemetry.emitForceRefresh(true);

        final Map<String, String> headers = telemetry.getTelemetryHeaders();

        Assert.assertEquals(2, headers.size());
        Assert.assertEquals("2|" + apiId + ",1|2,,,,,,,,,,,,", headers.get(CURRENT_REQUEST_HEADER_NAME));
        Assert.assertEquals("2|2|||2,1", headers.get(LAST_REQUEST_HEADER_NAME));
    }

    private void flush(@NonNull ICommand<Boolean> mockCommand,
                       @NonNull ICommandResult mockCommandResult,
                       @Nullable InMemoryStorage<CurrentRequestTelemetry> inMemoryTelemetryMap,
                       @Nullable InMemoryStorage<Set<FailedRequest>> sentFailedRequestsMap,
                       @Nullable InMemoryStorage<String> lastRequestTelemetryMap) {
        inMemoryTelemetryMap = inMemoryTelemetryMap != null ? inMemoryTelemetryMap : new InMemoryStorage<CurrentRequestTelemetry>();
        sentFailedRequestsMap = sentFailedRequestsMap != null ? sentFailedRequestsMap : new InMemoryStorage<Set<FailedRequest>>();

        final EstsTelemetry telemetry = getTelemetry(inMemoryTelemetryMap, sentFailedRequestsMap, lastRequestTelemetryMap);
        telemetry.flush(mockCommand, mockCommandResult);
        Assert.assertEquals(inMemoryTelemetryMap.size(), 0);
    }

    private EstsTelemetry getTelemetry(@Nullable final InMemoryStorage<CurrentRequestTelemetry> mTelemetryMap,
                                       @Nullable final InMemoryStorage<Set<FailedRequest>> sentFailedRequestsMap,
                                       @Nullable final InMemoryStorage<String> lastRequestTelemetryMap) {

        final EstsTelemetry telemetry = new EstsTelemetry(
                mTelemetryMap != null ? mTelemetryMap : new InMemoryStorage<CurrentRequestTelemetry>(),
                sentFailedRequestsMap != null ? sentFailedRequestsMap : new InMemoryStorage<Set<FailedRequest>>()
        );

        if (lastRequestTelemetryMap != null) {
            telemetry.setUp(new LastRequestTelemetryCache(lastRequestTelemetryMap));
        }

        return telemetry;
    }
}
