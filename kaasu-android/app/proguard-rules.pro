# Kaasu ProGuard rules

# Keep notification listener service
-keep class com.kaasu.app.notification.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers @androidx.room.Entity class * { *; }

# Hilt
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.android.HiltAndroidApp class *

# DataStore
-keep class androidx.datastore.** { *; }

# Vico charts
-keep class com.patrykandpatrick.vico.** { *; }
