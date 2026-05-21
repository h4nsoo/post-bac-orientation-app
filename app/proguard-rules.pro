# ── Kotlin ────────────────────────────────────────────────────────────────────

# Preserve Kotlin metadata so that Hilt, coroutines, and reflection-based
# libraries can inspect types at runtime.
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# Preserve line numbers in crash reports.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Suppress false-positive warnings from the Kotlin standard library.
-dontwarn kotlin.**
-dontwarn kotlinx.**

# ── Room ──────────────────────────────────────────────────────────────────────

# Keep all @Entity and @Dao classes.
# Room KSP generates Impl classes that reference entity field names directly;
# obfuscating them would rename columns in existing databases, breaking installs
# that survive a schema version bump without a destructive migration.
-keep class com.example.orientation_app.data.entity.** { *; }
-keep interface com.example.orientation_app.data.dao.** { *; }

# ── Hilt / Dagger ─────────────────────────────────────────────────────────────

# The Hilt Gradle plugin injects its own consumer rules via the AAR; these
# supplement them by suppressing any residual warnings.
-dontwarn dagger.**
-dontwarn javax.inject.**

# ── Google Generative AI / Gemini SDK ─────────────────────────────────────────

# The SDK uses protobuf and gRPC internally; keep the public surface and
# suppress warnings for classes not present on every device.
-keep class com.google.ai.client.generativeai.** { *; }
-dontwarn com.google.ai.client.generativeai.**
-dontwarn com.google.protobuf.**
-dontwarn io.grpc.**
-dontwarn com.google.android.gms.**

# ── Domain models ─────────────────────────────────────────────────────────────

# Keeping domain models preserves readable class names in production crash
# reports, and future serialization won't break if names are retained.
-keep class com.example.orientation_app.domain.model.** { *; }

# ── Coroutines ────────────────────────────────────────────────────────────────

# Required for coroutine stack-trace recovery — without this, crash reports
# lose the coroutine continuation chain.
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ── Standard suppressions ─────────────────────────────────────────────────────

-dontwarn sun.misc.**
-dontwarn java.lang.invoke.**
