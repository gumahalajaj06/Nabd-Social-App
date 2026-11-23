# Nabd (com.gumah.nabd) — Clean R8 / ProGuard Rules for Play Protect & Release Integrity

-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep Firestore & Domain Models
-keep @androidx.annotation.Keep class * { *; }
-keepclassmembers class com.example.data.model.** { *; }
-keep class com.example.data.model.** { *; }

# Keep Room Database & Entities
-keep class * extends androidx.room.RoomDatabase
-keepclassmembers class com.example.data.local.** { *; }

# Keep Firebase Firestore & Auth reflection members
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.PropertyName <methods>;
    @com.google.firebase.firestore.DocumentId <fields>;
    @com.google.firebase.firestore.ServerTimestamp <fields>;
}

# Keep AndroidX Credential Manager & Google Identity
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }

# Keep Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**
