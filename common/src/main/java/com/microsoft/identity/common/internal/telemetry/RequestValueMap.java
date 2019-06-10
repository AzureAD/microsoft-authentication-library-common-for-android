package com.microsoft.identity.common.internal.telemetry;

import java.util.HashMap;
import java.util.Map;

public class RequestValueMap {
    private Map<String, EventValueMap> mRequestValueMap;

    public RequestValueMap() {
        mRequestValueMap = new HashMap<>();
    }

    public RequestValueMap(final String requestId, final EventValueMap eventValueMap) {
        mRequestValueMap = new HashMap<>();
        mRequestValueMap.put(requestId, eventValueMap);
    }

    public void put(final String requestId, final EventValueMap eventValueMap) {
        if (mRequestValueMap == null) {
            mRequestValueMap = new HashMap<>();
        }

        if (mRequestValueMap.get(requestId) != null) {
            mRequestValueMap.get(requestId).put(eventValueMap);
        } else {
            mRequestValueMap.put(requestId, eventValueMap);
        }
    }
}
