package com.microsoft.identity.common.internal.logging;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.microsoft.identity.common.logging.Logger;

import net.jcip.annotations.Immutable;

import java.lang.reflect.Type;
import java.util.Locale;

import com.microsoft.identity.common.logging.Logger.LogLevel;

@Immutable
public class LogLevelDeserializer implements JsonDeserializer<Logger.LogLevel> {

    @Override
    public Logger.LogLevel deserialize(final JsonElement json,
                                       final Type typeOfT,
                                       final JsonDeserializationContext context) throws JsonParseException {
        final String logLevel = json.getAsString().toUpperCase(Locale.US);
        if (logLevel.equals("WARNING")) {
            return LogLevel.WARN;
        }

        return LogLevel.valueOf(logLevel);
    }
}
