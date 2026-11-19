# NavKart Release R8 / ProGuard Hardening Rules
-repackageclasses 'o'
-allowaccessmodification

# Keep Kotlin Metadata & Annotations for Moshi/Reflection
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,RuntimeVisibleAnnotations
-keep class kotlin.Metadata { *; }

# Keep Moshi Models & Codegen Adapters
-keepclassmembers @com.squareup.moshi.JsonClass class * {
    <init>(...);
    @com.squareup.moshi.Json <fields>;
}
-keep class com.example.data.model.** { *; }
-keep class com.example.data.api.* { *; }
-keepclasseswithmembers class * {
    @com.squareup.moshi.Json <fields>;
}

# Retrofit / OkHttp
-keepattributes Exceptions,InnerClasses
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Firebase, Firestore, RTDB, App Check & Credential Manager
-keep @androidx.annotation.Keep class * { *; }
-keepclassmembers class * {
    @com.google.firebase.database.PropertyName <fields>;
    @com.google.firebase.database.PropertyName <methods>;
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.PropertyName <methods>;
}
-keep class com.google.firebase.** { *; }
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }

# Strip verbose debug/verbose logs in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}
