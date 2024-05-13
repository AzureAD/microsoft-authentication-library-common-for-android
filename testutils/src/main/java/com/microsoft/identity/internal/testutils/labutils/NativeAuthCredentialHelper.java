package com.microsoft.identity.internal.testutils.labutils;

import com.microsoft.identity.common.java.util.StringUtil;
import com.microsoft.identity.internal.testutils.BuildConfig;

public class NativeAuthCredentialHelper {
   public static String getNativeAuthSignInUsername() {
      String username = BuildConfig.NATIVE_AUTH_SIGNIN_TEST_USERNAME;
      if (StringUtil.isNullOrEmpty(username)) {
         throw new IllegalStateException("env var NATIVE_AUTH_SIGNIN_TEST_USERNAME value not set");
      } else {
         return username;
      }
   }

   public static String getNativeAuthSignInPassword() {
      String password = BuildConfig.NATIVE_AUTH_SIGNIN_TEST_PASSWORD;
      if (StringUtil.isNullOrEmpty(password)) {
         throw new IllegalStateException("env var NATIVE_AUTH_SIGNIN_TEST_PASSWORD value not set");
      } else {
         return password;
      }
   }
}
