package com.havamania

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications WHERE userId = :uid ORDER BY createdAt DESC")
    fun getAllNotifications(uid: String): Flow<List<NotificationItem>>

    @Query("SELECT * FROM notifications WHERE userId = :uid LIMIT 1")
    suspend fun getAllNotificationsFirst(uid: String): List<NotificationItem>

    @Query("SELECT COUNT(*) FROM notifications WHERE userId = :uid")
    suspend fun getTotalCountFirst(uid: String): Int

    @Query("SELECT COUNT(*) FROM notifications WHERE userId = :uid AND isRead = 0")
    fun getUnreadCount(uid: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: NotificationItem)

    @Update
    suspend fun update(notification: NotificationItem)

    @Query("UPDATE notifications SET isRead = 1 WHERE id IN (:ids)")
    suspend fun markAsRead(ids: List<String>)

    @Query("UPDATE notifications SET isRead = 0 WHERE id IN (:ids)")
    suspend fun markAsUnread(ids: List<String>)

    @Query("UPDATE notifications SET isRead = 1 WHERE userId = :uid")
    suspend fun markAllAsRead(uid: String)

    @Query("DELETE FROM notifications WHERE id IN (:ids)")
    suspend fun delete(ids: List<String>)

    @Query("DELETE FROM notifications WHERE userId = :uid")
    suspend fun deleteAll(uid: String)

    @Query("DELETE FROM notifications WHERE userId = :uid AND category = :category")
    suspend fun deleteByCategory(uid: String, category: String)
}

@Database(entities = [NotificationItem::class], version = 4, exportSchema = false)
@TypeConverters(NotificationConverters::class)
abstract class NotificationDatabase : RoomDatabase() {
    abstract fun notificationDao(): NotificationDao

    companion object {
        @Volatile
        private var INSTANCE: NotificationDatabase? = null

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE notifications ADD COLUMN userId TEXT NOT NULL DEFAULT 'legacy'")
            }
        }

        fun getDatabase(context: Context): NotificationDatabase {
            return INSTANCE ?: synchronized(this) {
                try {
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        NotificationDatabase::class.java,
                        "notification_database"
                    )
                    .addMigrations(MIGRATION_3_4)
                    .fallbackToDestructiveMigration()
                    .build()
                    INSTANCE = instance
                    instance
                } catch (e: Exception) {
                    // Critical fallback if DB is totally corrupted
                    context.deleteDatabase("notification_database")
                    Room.databaseBuilder(
                        context.applicationContext,
                        NotificationDatabase::class.java,
                        "notification_database"
                    ).build()
                }
            }
        }
    }
}

class NotificationConverters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromCategory(category: NotificationCategory): String = category.name

    @TypeConverter
    fun toCategory(value: String): NotificationCategory {
        return try {
            NotificationCategory.valueOf(value)
        } catch (e: Exception) {
            NotificationCategory.GENERAL
        }
    }

    @TypeConverter
    fun fromActionType(actionType: NotificationActionType): String = actionType.name

    @TypeConverter
    fun toActionType(value: String): NotificationActionType {
        return try {
            NotificationActionType.valueOf(value)
        } catch (e: Exception) {
            NotificationActionType.NONE
        }
    }

    @TypeConverter
    fun fromTravelNotificationData(value: TravelNotificationData?): String? {
        return value?.let { json.encodeToString(TravelNotificationData.serializer(), it) }
    }

    @TypeConverter
    fun toTravelNotificationData(value: String?): TravelNotificationData? {
        return value?.let {
            try { json.decodeFromString(TravelNotificationData.serializer(), it) } catch(e: Exception) { null }
        }
    }
}
