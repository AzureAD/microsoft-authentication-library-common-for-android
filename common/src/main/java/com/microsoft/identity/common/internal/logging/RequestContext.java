package com.microsoft.identity.common.internal.logging;

import com.google.gson.Gson;

import java.util.HashMap;

// TODO I'm not wedded to this name, but the concept may work for tracking correlationIds
public class RequestContext extends HashMap<String, String> implements IRequestContext {

    private final Gson mGson = new Gson();

    @Override
    public String toJsonString() {
        return mGson.toJson(this);
    }
}
