# Contact Explorer ProGuard / R8 Rules

-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep Room generated classes and entities
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }

# Keep all Contact Explorer app, data, plugin, and core classes
-keep class com.example.contactexplorer.** { *; }

# Keep Kotlin reflection metadata (needed for KClass comparisons)
-keep class kotlin.reflect.** { *; }
-keep class kotlin.Metadata { *; }

# Prevent R8 from stripping Compose functions referenced dynamically
-keep,allowoptimization class * {
    @androidx.compose.runtime.Composable <methods>;
}
