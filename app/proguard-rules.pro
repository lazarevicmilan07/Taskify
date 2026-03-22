# Taskify ProGuard Rules

# Keep Room entities and DAOs
-keep class com.taskify.app.data.local.entity.** { *; }
-keep interface com.taskify.app.data.local.dao.** { *; }

# Keep Hilt-generated classes
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.** { *; }

# Keep domain models (serialization-safe)
-keep class com.taskify.app.domain.model.** { *; }

# WorkManager
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Kotlin serialization (if added later)
-keepattributes *Annotation*
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
