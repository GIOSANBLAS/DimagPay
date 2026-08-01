# PayControl ProGuard / R8
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

-keepclassmembers class * {
    @androidx.room.* <methods>;
}

# SQLCipher
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }
-dontwarn net.sqlcipher.**

# Security crypto
-keep class androidx.security.crypto.** { *; }
-dontwarn androidx.security.crypto.**
