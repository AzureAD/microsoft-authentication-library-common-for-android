package com.microsoft.identity.common.java;

import com.microsoft.identity.common.java.interfaces.IKeyPairStorage;

import java.util.HashMap;

public class InMemoryStorage implements IKeyPairStorage {

    final HashMap<String, String> mMap = new HashMap<>();

    @Override
    public String get(String key) {
        return mMap.get(key);
    }

    @Override
    public void put(String key, String value) {
        mMap.put(key, value);
    }
}
