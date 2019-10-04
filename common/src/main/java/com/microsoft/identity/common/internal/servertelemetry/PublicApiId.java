package com.microsoft.identity.common.internal.servertelemetry;

public final class PublicApiId {

    // Silent Apis
    public static final String BROKER_ACQUIRE_TOKEN_SILENT = "21";
    public static final String LOCAL_ACQUIRE_TOKEN_SILENT = "22";

    // Interactive APIs
    public static final String BROKER_ACQUIRE_TOKEN_INTERACTIVE = "121";
    public static final String LOCAL_ACQUIRE_TOKEN_INTERACTIVE = "122";

    // Get/Remove accounts
    public static final String GET_ACCOUNTS = "921";
    public static final String GET_ACCOUNT = "922";
    public static final String GET_CURRENT_ACCOUNT_ASYNC = "923";
    public static final String REMOVE_ACCOUNT = "924";
    public static final String SIGN_OUT = "925";
}
