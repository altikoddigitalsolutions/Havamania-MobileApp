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

@Entity(tableName = "notification_pending_deliveries", foreignKeys = [ForeignKey(
    entity = NotificationItem::class, parentColumns = ["id"], childColumns = ["notificationId"],
    onDelete = ForeignKey.CASCADE
)])
data class NotificationPendingDelivery(
    @PrimaryKey val notificationId: String, val userId: String, val processId: String
)

internal object NotificationDeliveryProcess {
    val id: String = java.util.UUID.randomUUID().toString()
}

@Dao
interface NotificationDao {
    @Insert
    suspend fun insertPendingDeliveryRaw(pending: NotificationPendingDelivery)

    /** Never touches the current process's reservations or another account's rows. */
    @Query("DELETE FROM notifications WHERE userId = :uid AND id IN (SELECT notificationId FROM notification_pending_deliveries WHERE userId = :uid AND processId != :processId)")
    suspend fun recoverInterruptedDeliveries(uid: String, processId: String = NotificationDeliveryProcess.id)

    @Query("DELETE FROM notification_pending_deliveries WHERE userId = :uid AND notificationId = :id AND processId = :processId")
    suspend fun completeDelivery(uid: String, id: String, processId: String = NotificationDeliveryProcess.id)

    @Query("SELECT EXISTS(SELECT 1 FROM notification_pending_deliveries WHERE notificationId = :id)")
    suspend fun hasPendingDelivery(id: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM notification_pending_deliveries WHERE notificationId = :id AND userId = :uid AND processId = :processId)")
    suspend fun ownsPendingDelivery(uid: String, id: String, processId: String = NotificationDeliveryProcess.id): Boolean

    @Query("SELECT id FROM notifications WHERE userId = :uid AND deduplicationKey = :key LIMIT 1")
    suspend fun notificationIdWithKey(uid: String, key: String): String?

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

    @Query("SELECT n.* FROM notifications AS n WHERE n.userId = :uid AND NOT EXISTS (SELECT 1 FROM notification_pending_deliveries AS p WHERE p.notificationId = n.id) ORDER BY n.createdAt DESC, n.id ASC")
    fun getAllNotifications(uid: String): Flow<List<NotificationItem>>

    @Query("SELECT n.* FROM notifications AS n WHERE n.userId = :uid AND n.id = :id AND NOT EXISTS (SELECT 1 FROM notification_pending_deliveries AS p WHERE p.notificationId = n.id) LIMIT 1")
    suspend fun getVisibleNotification(uid: String, id: String): NotificationItem?

    // Storage diagnostics include reservations; UI must use the visible Flow queries.
    @Query("SELECT * FROM notifications WHERE userId = :uid LIMIT 1")
    suspend fun getAllNotificationsFirst(uid: String): List<NotificationItem>

    @Query("SELECT COUNT(*) FROM notifications WHERE userId = :uid")
    suspend fun getTotalCountFirst(uid: String): Int

    @Query("SELECT COUNT(*) FROM notifications AS n WHERE n.userId = :uid AND n.isRead = 0 AND NOT EXISTS (SELECT 1 FROM notification_pending_deliveries AS p WHERE p.notificationId = n.id)")
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

    /** Positive: owned reservation, zero: acknowledged/legacy event, -1: rejected or still pending. */
    @Transaction
    suspend fun reserveForDelivery(notification: NotificationItem, processId: String = NotificationDeliveryProcess.id): Long {
        if (isDeletedAccountHash(accountOwnerHash(notification.userId))) return -1
        recoverInterruptedDeliveries(notification.userId, processId)
        val owner = notificationOwner(notification.id)
        if (owner != null) return if (owner == notification.userId && !hasPendingDelivery(notification.id)) 0 else -1
        val key = notification.deduplicationKey
        if (!key.isNullOrEmpty()) {
            val existing = notificationIdWithKey(notification.userId, key)
            if (existing != null) return if (hasPendingDelivery(existing)) -1 else 0
        }
        val inserted = insertIncomingRaw(notification)
        if (inserted > 0) insertPendingDeliveryRaw(NotificationPendingDelivery(notification.id, notification.userId, processId))
        return inserted
    }

    @Update
    suspend fun updateRaw(notification: NotificationItem)

    @Transaction
    suspend fun update(notification: NotificationItem) {
        if (isDeletedAccountHash(accountOwnerHash(notification.userId))) return
        if (notificationOwner(notification.id) != notification.userId) return
        updateRaw(notification)
    }

    @Query("UPDATE notifications SET isRead = 1 WHERE userId = :uid AND id IN (:ids) AND NOT EXISTS (SELECT 1 FROM notification_pending_deliveries AS p WHERE p.notificationId = notifications.id)")
    suspend fun markAsRead(uid: String, ids: List<String>)

    @Query("UPDATE notifications SET isRead = 0 WHERE userId = :uid AND id IN (:ids) AND NOT EXISTS (SELECT 1 FROM notification_pending_deliveries AS p WHERE p.notificationId = notifications.id)")
    suspend fun markAsUnread(uid: String, ids: List<String>)

    @Query("UPDATE notifications SET isRead = 1 WHERE userId = :uid AND NOT EXISTS (SELECT 1 FROM notification_pending_deliveries AS p WHERE p.notificationId = notifications.id)")
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

@Database(entities = [NotificationItem::class, NotificationAccountDeletion::class, NotificationPendingDelivery::class], version = 7, exportSchema = false)
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

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS notification_pending_deliveries (notificationId TEXT NOT NULL, userId TEXT NOT NULL, processId TEXT NOT NULL, PRIMARY KEY(notificationId), FOREIGN KEY(notificationId) REFERENCES notifications(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
            }
        }

        fun getDatabase(context: Context): NotificationDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NotificationDatabase::class.java,
                    "notification_database"
                )
                .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
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
