package com.havamania

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY createdAt DESC")
    fun getAllNotifications(): Flow<List<NotificationItem>>

    @Query("SELECT * FROM notifications LIMIT 1")
    suspend fun getAllNotificationsFirst(): List<NotificationItem>

    @Query("SELECT COUNT(*) FROM notifications")
    suspend fun getTotalCountFirst(): Int

    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0")
    fun getUnreadCount(): Flow<Int>

    @Query("SELECT * FROM notifications WHERE id = :id LIMIT 1")
    suspend fun getNotificationById(id: String): NotificationItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: NotificationItem)

    @Update
    suspend fun update(notification: NotificationItem)

    @Query("UPDATE notifications SET isRead = 1 WHERE id IN (:ids)")
    suspend fun markAsRead(ids: List<String>)

    @Query("UPDATE notifications SET isRead = 0 WHERE id IN (:ids)")
    suspend fun markAsUnread(ids: List<String>)

    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllAsRead()

    @Query("DELETE FROM notifications WHERE id IN (:ids)")
    suspend fun delete(ids: List<String>)

    @Query("DELETE FROM notifications")
    suspend fun deleteAll()
}

@Database(entities = [NotificationItem::class], version = 2, exportSchema = false)
@TypeConverters(NotificationConverters::class)
abstract class NotificationDatabase : RoomDatabase() {
    abstract fun notificationDao(): NotificationDao

    companion object {
        @Volatile
        private var INSTANCE: NotificationDatabase? = null

        fun getDatabase(context: Context): NotificationDatabase {
            return INSTANCE ?: synchronized(this) {
                try {
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        NotificationDatabase::class.java,
                        "notification_database"
                    )
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
