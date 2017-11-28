package com.microsoft.identity.common.adal.internal.cache;

import android.util.Log;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import com.microsoft.identity.common.adal.error.ADALError;

import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public final class DateTimeAdapter implements JsonDeserializer<Date>, JsonSerializer<Date> {

    private static final String TAG = "DateTimeAdapter";

    private final DateFormat mEnUsFormat = DateFormat.getDateTimeInstance(DateFormat.DEFAULT,
            DateFormat.DEFAULT, Locale.US);

    private final DateFormat mLocalFormat = DateFormat.getDateTimeInstance(DateFormat.DEFAULT,
            DateFormat.DEFAULT);

    private final DateFormat mISO8601Format = buildIso8601Format();
    private final DateFormat mEnUs24HourFormat = buildEnUs24HourDateFormat();
    private final DateFormat mLocal24HourFormat = buildLocal24HourDateFormat();

    private static DateFormat buildIso8601Format() {
        DateFormat iso8601Format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        iso8601Format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return iso8601Format;
    }

    /**
     * Add new en-us date format for parsing date string if it doesn't contain AM/PM.
     */
    private static DateFormat buildEnUs24HourDateFormat() {
        return new SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.US);
    }

    /**
     * Add new local date format when parsing date string if it doesn't contain AM/PM.
     */
    private static DateFormat buildLocal24HourDateFormat() {
        return new SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault());
    }

    /**
     * Default constructor for {@link DateTimeAdapter}.
     */
    public DateTimeAdapter() {
        // Default constructor, intentionally empty.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Date deserialize(JsonElement json, Type typeOfT,
                                         JsonDeserializationContext context) throws JsonParseException {
        String jsonString = json.getAsString();

        // Datetime string is serialized with iso8601 format by default, should
        // always try to deserialize with iso8601. But to support the backward
        // compatibility, we also need to deserialize with old format if failing
        // with iso8601.
        try {
            return mISO8601Format.parse(jsonString);
        } catch (final ParseException ignored) {
            Log.v(TAG, "Cannot parse with ISO8601, try again with local format.");
        }

        // fallback logic for old date format parsing.
        try {
            return mLocalFormat.parse(jsonString);
        } catch (final ParseException ignored) {
            Log.v(TAG, "Cannot parse with local format, try again with local 24 hour format.");
        }

        try {
            return mLocal24HourFormat.parse(jsonString);
        } catch (final ParseException ignored) {
            Log.v(TAG, "Cannot parse with local 24 hour format, try again with en us format.");
        }

        try {
            return mEnUsFormat.parse(jsonString);
        } catch (final ParseException ignored) {
            Log.v(TAG, "Cannot parse with en us format, try again with en us 24 hour format.");
        }

        try {
            return mEnUs24HourFormat.parse(jsonString);
        } catch (final ParseException e) {
            Log.e(TAG, "Could not parse date: " + e.getMessage(), e);
        }

        throw new JsonParseException("Could not parse date: " + jsonString);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized JsonElement serialize(Date src, Type typeOfSrc,
                                              JsonSerializationContext context) {
        return new JsonPrimitive(mISO8601Format.format(src));
    }
}