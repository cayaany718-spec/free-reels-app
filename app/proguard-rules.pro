# -----------------------------------------------------------------------------
# CODE PROTECTION & OBFUSCATION RULES (Chống xem mã nguồn)
# -----------------------------------------------------------------------------

# Enable aggressive package renaming and obfuscation flattening
-repackageclasses 'com.example.shortdrama.obfuscated'
-allowaccessmodification

# Hide original source file names and strip debugging line numbers
-renamesourcefileattribute SourceFile
-keepattributes !SourceFile,!LineNumberTable

# Keep essential runtime annotations for reflection / serialization libraries (like Retrofit / Moshi)
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# --- Moshi Rules (Keep JSON Serialization Adapters safe from obfuscation issues) ---
-keep class com.squareup.moshi.** { *; }
-keep interface com.squareup.moshi.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json *;
}
-keep class *JsonAdapter { *; }

# --- Retrofit Rules ---
-keepattributes Signature
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**
-keepclassmembers class * {
    @retrofit2.http.** <methods>;
}

# --- Room Database Rules ---
-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.Entity
-dontwarn androidx.room.**

# --- Keep OkHttp/Okio rules ---
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
