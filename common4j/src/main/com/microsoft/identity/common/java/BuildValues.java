package com.microsoft.identity.common.java;

import androidx.annotation.NonNull;

//Used as a wrapper for setting and accessing values through either the generated BuildConfig.java class,
//or from parameters set via the NativeAuthPublicClientApplicationConfiguration.kt file
public class BuildValues {
    //Appended to the URL constructed in NativeAuthOAuth2Configuration, used for making calls to tenants on test slices
    @NonNull
    private static String DC = BuildConfig.DC;

    public static String getDC()
    {
        return DC;
    }

    public static void setDC(String dc) {
        DC = dc;
    }

    //The mock API authority used for testing will be rejected by validation logic run on instantiation. This flag is used to bypass those checks in various points in the application
    @NonNull
    private static Boolean USE_REAL_AUTHORITY = BuildConfig.USE_REAL_AUTHORITY;

    public static Boolean shouldUseReadAuthority()
    {
        return USE_REAL_AUTHORITY;
    }

    public static void setUseRealAuthority(Boolean ura) {
        USE_REAL_AUTHORITY = ura;
    }
}
