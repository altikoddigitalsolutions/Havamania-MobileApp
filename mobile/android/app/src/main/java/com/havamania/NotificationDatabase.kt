package com.havamania

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

@Entity(tableName = "notification_account_deletions")
data class NotificationAccountDeletion(@PrimaryKey val ownerHash: String)

@Dao
interface NotificationDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun blockDeletedAccountRaw(marker: NotificationAccountDeletion)

    @Query("SELECT EXISTS(SELECT 1 FROM notification_account_deletions WHERE ownerHash = :hash)")
    suspend fun isDeletedAccountHash(hash: String): Boolean

    @Query("SELECT userId FROM notifications WHERE id = :id LIMIT 1")
    suspend fun notificationOwner(id: String): String?

    @Transaction
    suspend fun deleteAccountData(uid: String) {
        blockDeletedAccountRaw(NotificationAccountDeletion(accountOwnerHash(uid)))
        deleteAll(uid)
    }

    @Query("SELECT * FROM notifications WHERE userId = :uid ORDER BY createdAt DESC")
    fun getAllNotifications(uid: String): Flow<List<NotificationItem>>

    @Query("SELECT * FROM notifications WHERE userId = :uid LIMIT 1")
    suspend fun getAllNotificationsFirst(uid: String): List<NotificationItem>

    @Query("SELECT COUNT(*) FROM notifications WHERE userId = :uid")
    suspend fun getTotalCountFirst(uid: String): Int

    @Query("SELECT COUNT(*) FROM notifications WHERE userId = :uid AND isRead = 0")
    fun getUnreadCount(uid: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRaw(notification: NotificationItem)

    @Transaction
    suspend fun insert(notification: NotificationItem) {
        if (isDeletedAccountHash(accountOwnerHash(notification.userId))) return
        val owner = notificationOwner(notification.id)
        if (owner != null && owner != notification.userId) return
        insertRaw(notification)
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIncomingRaw(notification: NotificationItem): Long

    @Transaction
    suspend fun insertIncoming(notification: NotificationItem): Long {
        if (isDeletedAccountHash(accountOwnerHash(notification.userId))) return -1
        return insertIncomingRaw(notification)
    }

    @Update
    suspend fun updateRaw(notification: NotificationItem)

    @Transaction
    suspend fun update(notification: NotificationItem) {
        if (isDeletedAccountHash(accountOwnerHash(notification.userId))) return
        if (notificationOwner(notification.id) != notification.userId) return
        updateRaw(notification)
    }

    @Query("UPDATE notifications SET isRead = 1 WHERE userId = :uid AND id IN (:ids)")
    suspend fun markAsRead(uid: String, ids: List<String>)

    @Query("UPDATE notifications SET isRead = 0 WHERE userId = :uid AND id IN (:ids)")
    suspend fun markAsUnread(uid: String, ids: List<String>)

    @Query("UPDATE notifications SET isRead = 1 WHERE userId = :uid")
    suspend fun markAllAsRead(uid: String)

    @Query("DELETE FROM notifications WHERE userId = :uid AND id IN (:ids)")
    suspend fun delete(uid: String, ids: List<String>)

    @Query("DELETE FROM notifications WHERE userId = :uid")
    suspend fun deleteAll(uid: String)

    @Query("DELETE FROM notifications WHERE userId = :uid AND category = :category")
    suspend fun deleteByCategory(uid: String, category: String)

    @Query("SELECT EXISTS(SELECT 1 FROM notifications WHERE userId = :uid AND deduplicationKey = :key LIMIT 1)")
    suspend fun existsWithKey(uid: String, key: String): Boolean
}

@Database(entities = [NotificationItem::class, NotificationAccountDeletion::class], version = 6, exportSchema = false)
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

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE notifications ADD COLUMN deduplicationKey TEXT")
                database.execSQL("ALTER TABLE notifications ADD COLUMN severity TEXT NOT NULL DEFAULT 'NORMAL'")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS notification_account_deletions (ownerHash TEXT NOT NULL, PRIMARY KEY(ownerHash))")
            }
        }

        fun getDatabase(context: Context): NotificationDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NotificationDatabase::class.java,
                    "notification_database"
                )
                .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
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
