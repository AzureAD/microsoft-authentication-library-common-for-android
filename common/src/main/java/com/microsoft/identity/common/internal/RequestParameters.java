package com.microsoft.identity.common.internal;

import java.util.HashMap;
import java.util.Map;

public class RequestParameters {
    private Map<String, Object> mParameterMap = new HashMap<>();
    private static final Character DELIMITER = '&';

    public void put(String key, Object value) {
        mParameterMap.put(key, value);
    }
    public Object get(String key) {
        return mParameterMap.get(key);
    }

    public void remove(String key) {
        mParameterMap.remove(key);
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        for(Map.Entry entry : mParameterMap.entrySet()) {
            if(sb.length() > 0) {
                sb.append(DELIMITER);
            }
            sb.append(entry.getKey()).append('=').append(entry.getValue() == null ? "" : entry.getValue().toString());
        }
        return sb.toString();
    }

    public static class Builder {
        private Map<String, Object> mMap = new HashMap<>();

        public Builder put(String key, Object value) {
            mMap.put(key, value);
            return this;
        }

        public Builder putAll(Map<String, Object> map) {
            mMap.putAll(map);
            return this;
        }

        public Builder remove(String key) {
            mMap.remove(key);
            return this;
        }

        public RequestParameters build() {
            RequestParameters ret = new RequestParameters();
            ret.mParameterMap.putAll(mMap);
            return ret;
        }
    }
}
