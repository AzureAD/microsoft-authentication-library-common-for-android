package com.microsoft.identity.common;

/**
 * A class having the codemarker value of particular events descriptions.
 * one or many of these can be used while capturing codemarker event by sending as an artument in method call to markCode of class CodeMarkerManager.
 */
public class CodeMarkersConstants {
    public static String BROKER_PROCESS_START = "10111";
    public static String BROKER_PROCESS_END = "10120";
    public static String ACQUIRE_TOKEN_SILENT_START = "10011";
    public static String ACQUIRE_TOKEN_SILENT_EXECUTOR_START = "10012";
    public static String ACQUIRE_TOKEN_SILENT_COMMAND_EXECUTION_START = "10013";
    public static String ACQUIRE_TOKEN_SILENT_COMMAND_EXECUTION_END = "10014";
    public static String ACQUIRE_TOKEN_SILENT_FUTURE_OBJECT_CREATION_END = "10020";
}
