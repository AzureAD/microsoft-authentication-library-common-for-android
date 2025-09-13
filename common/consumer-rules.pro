# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Program Files\Android\android-sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

##---------------Begin: proguard configuration for Common  --------
# Intentionally blank, left to consumers of common to implement.
# keep with optmizations and shrinking, but do not obfuscate
-keep,allowoptimization,allowshrinking class !com.microsoft.identity.common.java.nativeauth.**, !com.microsoft.identity.common.nativeauth.**, com.microsoft.identity.** { *; }
-keep class * extends com.microsoft.identity.common.java.authorities.Authority { *; }
-keep class * extends com.microsoft.identity.common.java.authorities.AzureActiveDirectoryAudience { *; }
-keep class * extends com.microsoft.identity.common.java.cache.ICacheRecord { *; }
-keep class * extends com.microsoft.identity.common.java.cache.ITokenCacheItem { *; }
-keep class * extends com.microsoft.identity.common.java.authscheme.AbstractAuthenticationScheme { *; }
-keep class com.microsoft.identity.common.internal.broker.AuthUxJsonPayload { *; }


#For Android Credential Manager: https://developer.android.com/training/sign-in/passkeys#proguard
-if class androidx.credentials.CredentialManager
-keep class androidx.credentials.playservices.** {
  *;
}

# Runtime annotations
-keep class net.jcip.annotations.GuardedBy
-keep class net.jcip.annotations.Immutable
-keep class net.jcip.annotations.ThreadSafe

# Compile time annotations
-dontwarn edu.umd.cs.findbugs.annotations.NonNull
-dontwarn edu.umd.cs.findbugs.annotations.Nullable
-dontwarn edu.umd.cs.findbugs.annotations.SuppressFBWarnings

-keepattributes SourceFile,LineNumberTable

##---------------Begin: proguard configuration for Nimbus  ----------
# Intentionally blank, left to consumers of common to implement.

##---------------Begin: proguard configuration for Lombok  ----------
-dontwarn lombok.**

##---------------Begin: proguard configuration for Gson  --------
# Gson uses generic type information stored in a class file when working with fields. Proguard
# removes such information by default, so configure it to keep all of it.
-keepattributes Signature,SourceFile,LineNumberTable

# For using GSON @Expose annotation
-keepattributes *Annotation*

# Gson specific classes
-dontwarn sun.misc.**
#-keep class com.google.gson.stream.** { *; }

# Prevent proguard from stripping interface information from TypeAdapter, TypeAdapterFactory,
# JsonSerializer, JsonDeserializer instances (so they can be used in @JsonAdapter)
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken { *; }

##---------------Begin: proguard configuration for OpenTelemetry  --------
# keep everything in this package from being removed or renamed
-keep class io.opentelemetry.** { *; }

# Prevent R8 from leaving Data object members always null
-keepclassmembers class com.microsoft.identity.** {
  @com.google.gson.annotations.SerializedName <fields>;
  @com.squareup.moshi.Json <fields>;
}

## Other
# Compile time annotation
-dontwarn com.google.auto.value.AutoValue$CopyAnnotations
-dontwarn com.google.auto.value.AutoValue
-dontwarn com.google.auto.value.extension.memoized.Memoized
