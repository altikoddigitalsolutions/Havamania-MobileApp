package com.havamania

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY createdAt DESC")
    fun getAllNotifications(): Flow<List<NotificationItem>>

    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0")
    fun getUnreadCount(): Flow<Int>

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
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NotificationDatabase::class.java,
                    "notification_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class NotificationConverters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromCategory(category: NotificationCategory): String = category.name

    @TypeConverter
    fun toCategory(value: String): NotificationCategory = NotificationCategory.valueOf(value)

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
