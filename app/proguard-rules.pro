# ─────────────────────────────────────────────────────────────────────────────
# Lorevyn — app/proguard-rules.pro
# R8 rules for release builds.
# ─────────────────────────────────────────────────────────────────────────────

# ── Ktor ─────────────────────────────────────────────────────────────────────
# Ktor's IntelliJ debug detector references JVM management classes
# that do not exist on Android. Safe to ignore entirely in release.
-dontwarn java.lang.management.ManagementFactory
-dontwarn java.lang.management.RuntimeMXBean
-dontwarn io.ktor.util.debug.**

# Ktor uses reflection internally for serialization and engine selection.
-keep class io.ktor.** { *; }
-keepclassmembers class io.ktor.** { *; }

# ── kotlinx.serialization ────────────────────────────────────────────────────
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
# Keep the @Serializable data classes themselves — R8 obfuscating these class
# names orphans the generated serializers. Private file-level classes in Kotlin
# (e.g. SearchResponse, VolumeItem in GoogleBooksApi.kt) are the specific risk.
-keep @kotlinx.serialization.Serializable class ** { *; }
-keep,includedescriptorclasses class com.lorevyn.**$$serializer { *; }
-keepclassmembers class com.lorevyn.** {
    *** Companion;
}
-keepclasseswithmembers class com.lorevyn.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ── Coroutines ───────────────────────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**

# ── Hilt / Dagger ────────────────────────────────────────────────────────────
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.InstallIn class * { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keepclasseswithmembers class * {
    @dagger.hilt.android.lifecycle.HiltViewModel <init>(...);
}

# ── SQLDelight ───────────────────────────────────────────────────────────────
-keep class com.squareup.sqldelight.** { *; }
-keep class app.cash.sqldelight.** { *; }

# ── Coil 3 ───────────────────────────────────────────────────────────────────
# Coil 3 ships its own R8 rules via coil-core. These dontwarn lines silence
# noise from coil3 + its okio transitive deps in release builds.
-dontwarn coil3.**
-dontwarn okio.**

# ── ML Kit (barcode scanning, ISBN scanner) ─────────────────────────────────
# ML Kit loads model classes via reflection at runtime. R8 would otherwise
# strip them and the scanner would silently fail on release builds only.
# Also suppress warnings for GMS tasks internals.
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_barcode.** { *; }
-dontwarn com.google.mlkit.**
-dontwarn com.google.android.gms.**

# ── CameraX ──────────────────────────────────────────────────────────────────
# CameraX uses ServiceLoader and reflection for its provider discovery.
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# ── WorkManager ──────────────────────────────────────────────────────────────
-keep class androidx.work.** { *; }
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker

# ── Play Billing ─────────────────────────────────────────────────────────────
-keep class com.android.billingclient.** { *; }

# ── Lorevyn app classes ───────────────────────────────────────────────────────
# Prevent R8 from stripping Security.kt RSA verification logic.
-keep class com.lorevyn.feature.billing.Security { *; }

# ── General Android / Kotlin ─────────────────────────────────────────────────
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
-keepattributes Exceptions
-keep public class * extends android.app.Application
-keep public class * extends androidx.lifecycle.ViewModel
