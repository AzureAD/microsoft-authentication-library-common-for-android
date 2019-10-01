package com.microsoft.identity.common.internal.servertelemetry;

public interface IRequestTelemetryCache {

    /**
     * Save telemetry associated to the {@link RequestTelemetry} object to the cache
     *
     * @param requestTelemetry
     */
    void saveRequestTelemetryToCache(final RequestTelemetry requestTelemetry);

    /**
     * Get the telemetry from the cache
     *
     * @return a {@link RequestTelemetry} object
     */
    RequestTelemetry getRequestTelemetryFromCache();


    /**
     * Clear the contents of the cache.
     */
    void clearAll();


}
