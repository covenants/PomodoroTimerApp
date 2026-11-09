# Room entities are referenced by generated DAO code and reflection-free adapters.
-keep class com.covenants.pomodorotimer.data.db.** { *; }

# Keep domain model names for readable crash reports and stable enum serialization.
-keepnames class com.covenants.pomodorotimer.core.model.** { *; }
-keepclassmembers enum com.covenants.pomodorotimer.core.model.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Compose and AndroidX ship consumer rules. No reflection-based app code is used.
-dontwarn org.jetbrains.annotations.**
