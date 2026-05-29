# 保留 Gson 序列化的数据类
-keepclassmembers class com.notifyforward.app.model.** { *; }
-keepclassmembers class com.notifyforward.app.data.entity.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase { void <init>(); }

# Room
-keep class * extends androidx.room.RoomDatabase { void <init>(); }
-keep @androidx.room.Entity class * { void <init>(); }
-keep @androidx.room.Dao interface * { *; }

# WorkManager
-keep class * extends androidx.work.Worker { void <init>(android.content.Context, androidx.work.WorkerParameters); }
-keep class * extends androidx.work.CoroutineWorker { void <init>(android.content.Context, androidx.work.WorkerParameters); }

# NotificationListenerService
-keep class * extends android.service.notification.NotificationListenerService { void <init>(); }

# DataStore
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite { *; }
