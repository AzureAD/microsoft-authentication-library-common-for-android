package com.microsoft.identity.common.internal.servertelemetry;

import java.util.Map;

public interface IRequestTelemetryCache {

    /**
     * Save telemetry associated to the {@link RequestTelemetry} object to the cache
     *
     * @param requestTelemetry
     */
    void saveRequestTelemetryToCache(final RequestTelemetry requestTelemetry);

    /**
     * Get the telemetry associated to the..
     * @return a {@link RequestTelemetry} object
     */
    RequestTelemetry getRequestTelemetryFromCache();


    /**
     * Clear the contents of the cache.
     */
    void clearAll();


}
