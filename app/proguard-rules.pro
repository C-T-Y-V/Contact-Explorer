# Contact Explorer ProGuard / R8 Rules

-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep Room generated classes, builders, and DAO implementations
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**
-keep class * extends androidx.room.RoomDatabase {
    public <init>();
    *;
}
-keep class **.*_Impl {
    public <init>(...);
    *;
}
-keepclassmembers class * extends androidx.room.RoomDatabase {
    *;
}
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class * implements androidx.room.RoomDatabase* { *; }

# Keep all Contact Explorer app, data, plugin, and core classes completely
-keep class com.example.contactexplorer.** { *; }
-keepclassmembers class com.example.contactexplorer.** { *; }

# Keep Kotlin reflection metadata & Coroutines
-keep class kotlin.reflect.** { *; }
-keep class kotlin.Metadata { *; }
-keep class kotlinx.coroutines.** { *; }

# Prevent R8 from stripping Compose functions and ViewModels
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    public <init>(...);
    *;
}
-keep,allowoptimization class * {
    @androidx.compose.runtime.Composable <methods>;
}
