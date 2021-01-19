package com.microsoft.identity.common;

/**
 * A class having the code marker value of particular event's description.
 * One or many of these can be used while capturing {@link CodeMarker} event by sending as an argument in method call to markCode of class {@link CodeMarkerManager}.
 */
public class CodeMarkerConstants {
    public static String BROKER_PROCESS_START = "10111";
    public static String BROKER_PROCESS_END = "10120";
    public static String ACQUIRE_TOKEN_SILENT_START = "10011";
    public static String ACQUIRE_TOKEN_SILENT_EXECUTOR_START = "10012";
    public static String ACQUIRE_TOKEN_SILENT_COMMAND_EXECUTION_START = "10013";
    public static String ACQUIRE_TOKEN_SILENT_COMMAND_EXECUTION_END = "10014";
    public static String ACQUIRE_TOKEN_SILENT_FUTURE_OBJECT_CREATION_END = "10020";
}
