package com.microsoft.identity.client.ui.automation.logging.formatter;

import android.util.Log;

import com.microsoft.identity.client.ui.automation.logging.LogLevel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class LogcatLikeFormatter implements ILogFormatter {

    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    @Override
    public String format(LogLevel logLevel, String tag, String message, Throwable throwable) {
        final String logMessage = getUTCDateTimeAsString() + ": " + logLevel.getLabel() + "/" + tag
                + ": " + message;
        return logMessage + (throwable == null ? "" : '\n' + Log.getStackTraceString(throwable));
    }

    private static String getUTCDateTimeAsString() {
        final SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        return dateFormat.format(new Date());
    }
}
