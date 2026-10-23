package com.havamania

import androidx.annotation.Keep
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer

/**
 * TypeConverter for AI Chat Messages
 */
class ChatTypeConverters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromChatMessageList(value: List<AltikodChatMessage>): String {
        return json.encodeToString(ListSerializer(AltikodChatMessage.serializer()), value)
    }

    @TypeConverter
    fun toChatMessageList(value: String): List<AltikodChatMessage> {
        return try {
            json.decodeFromString(ListSerializer(AltikodChatMessage.serializer()), value)
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromForecastSnapshot(value: ForecastSnapshot?): String? {
        return value?.let { json.encodeToString(ForecastSnapshot.serializer(), it) }
    }

    @TypeConverter
    fun toForecastSnapshot(value: String?): ForecastSnapshot? {
        return value?.let {
            try { json.decodeFromString(ForecastSnapshot.serializer(), it) } catch(e: Exception) { null }
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

    @TypeConverter
    fun fromTravelWeatherAnalysisList(value: List<TravelWeatherAnalysis>): String {
        return json.encodeToString(ListSerializer(TravelWeatherAnalysis.serializer()), value)
    }

    @TypeConverter
    fun toTravelWeatherAnalysisList(value: String): List<TravelWeatherAnalysis> {
        return try {
            json.decodeFromString(ListSerializer(TravelWeatherAnalysis.serializer()), value)
        } catch (e: Exception) {
            emptyList()
        }
    }
}

/**
 * Hava durumu verilerini veritabanında saklamak için Entity
 */
@Entity(tableName = "weather_cache")
@IgnoreExtraProperties
@Keep
data class WeatherCacheEntity(
    @PrimaryKey val cityName: String,
    val jsonData: String, // WeatherData nesnesi JSON olarak saklanacak
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Seyahat Planlarını saklamak için Entity
 */
@Entity(
    tableName = "travel_plans",
    indices = [Index(value = ["userId"])]
)
@IgnoreExtraProperties
@Keep
data class TravelPlanEntity(
    @PrimaryKey val id: String = "",
    var userId: String = "legacy",
    var city: String = "",
    var district: String? = null,
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var originCity: String? = null,
    var originDistrict: String? = null,
    var originLatitude: Double? = null,
    var originLongitude: Double? = null,
    var tripType: String = "",
    var startDate: Long = 0L,
    var endDate: Long = 0L,
    var createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis(),
    var archivedAt: Long? = null,
    var lastAnalysisAt: Long? = null,
    var weatherSummary: String? = null,
    var packingAdvice: String? = null,
    var mustSee: String? = null,
    var foodAdvice: String? = null,
    var localAdvice: String? = null,
    var aiSuggestion: String? = null,
    var comfortScore: Int? = null,
    var userNote: String? = null,
    var userRating: Int? = 0,
    var lastWeatherAnalysisText: String? = null,
    var lastWeatherAnalysisDate: Long? = null,
    var lastForecastSnapshot: ForecastSnapshot? = null,
    var previousForecastSnapshot: ForecastSnapshot? = null,
    var nextAnalysisEligibleDate: Long? = null,
    @get:PropertyName("weatherAnalysisStatus")
    @set:PropertyName("weatherAnalysisStatus")
    var weatherAnalysisStatus: String = "WAITING_FOR_WINDOW",
    @ColumnInfo(defaultValue = "0")
    @get:PropertyName("isArchived")
    @set:PropertyName("isArchived")
    var isArchived: Boolean = false,
    @get:PropertyName("analyses")
    @set:PropertyName("analyses")
    var analyses: List<TravelWeatherAnalysis> = emptyList(),
    @get:PropertyName("lastDailyNotificationDate")
    @set:PropertyName("lastDailyNotificationDate")
    var lastDailyNotificationDate: String? = null,
    @ColumnInfo(defaultValue = "0")
    @get:PropertyName("isDemo")
    @set:PropertyName("isDemo")
    var isDemo: Boolean = false,
    @get:PropertyName("departureTime")
    @set:PropertyName("departureTime")
    var departureTime: String? = null,
    @get:PropertyName("routeWeatherSummary")
    @set:PropertyName("routeWeatherSummary")
    var routeWeatherSummary: String? = null,
    @get:PropertyName("lastRouteAnalysisAt")
    @set:PropertyName("lastRouteAnalysisAt")
    var lastRouteAnalysisAt: Long? = null
) {
    fun toDomain() = TravelPlan(
        id = id,
        userId = userId,
        city = city,
        district = district,
        latitude = latitude,
        longitude = longitude,
        originCity = originCity,
        originDistrict = originDistrict,
        originLatitude = originLatitude,
        originLongitude = originLongitude,
        tripType = try { TripType.valueOf(tripType) } catch (e: Exception) { TripType.OTHER },
        startDate = java.time.Instant.ofEpochMilli(startDate).atZone(java.time.ZoneId.systemDefault()).toLocalDate(),
        endDate = java.time.Instant.ofEpochMilli(endDate).atZone(java.time.ZoneId.systemDefault()).toLocalDate(),
        createdAt = createdAt,
        updatedAt = updatedAt,
        archivedAt = archivedAt,
        lastAnalysisAt = lastAnalysisAt ?: lastWeatherAnalysisDate,
        weatherSummary = weatherSummary,
        packingAdvice = packingAdvice,
        mustSee = mustSee,
        foodAdvice = foodAdvice,
        localAdvice = localAdvice,
        aiSuggestion = aiSuggestion,
        comfortScore = comfortScore,
        userNote = userNote,
        userRating = userRating,
        isAnalyzing = false,
        weatherAnalysisStatus = try { TravelWeatherAnalysisStatus.valueOf(weatherAnalysisStatus) } catch (e: Exception) { TravelWeatherAnalysisStatus.WAITING_FOR_WINDOW },
        isArchived = isArchived,
        analyses = analyses,
        lastDailyNotificationDate = lastDailyNotificationDate,
        isDemo = isDemo,
        lastForecastSnapshot = lastForecastSnapshot,
        previousForecastSnapshot = previousForecastSnapshot,
        departureTime = departureTime,
        routeWeatherSummary = routeWeatherSummary,
        lastRouteAnalysisAt = lastRouteAnalysisAt
    )
}

/**
 * AI Analiz Geçmişini saklamak için Entity
 */
@Entity(
    tableName = "ai_history",
    indices = [Index(value = ["userId"])]
)
@IgnoreExtraProperties
@Keep
data class AiHistoryEntity(
    @PrimaryKey val id: String = "", // Acts as conversationId
    val userId: String = "legacy",
    val title: String = "",
    val summary: String = "",
    val messages: List<AltikodChatMessage> = emptyList(),
    val cityName: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    @get:com.google.firebase.firestore.Exclude
    @ColumnInfo(defaultValue = "NULL")
    val syncedUpdatedAt: Long? = null,
    @get:com.google.firebase.firestore.Exclude
    @ColumnInfo(defaultValue = "0")
    val pendingUpload: Boolean = false
)

/** Empty recordId means the entire user's history. Kept until remote acknowledgement. */
@Entity(tableName = "ai_history_deletions", primaryKeys = ["userId", "recordId"])
data class HistoryDeletionRequest(val userId: String, val recordId: String, @ColumnInfo(defaultValue = "0") val completed: Boolean = false)

/**
 * Veritabanı Erişim Nesnesi (DAO)
 */
@Dao
interface WeatherDao {
    @Query("SELECT * FROM weather_cache WHERE cityName = :city LIMIT 1")
    suspend fun getCachedWeather(city: String): WeatherCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeatherRaw(weather: WeatherCacheEntity)

    @Query("DELETE FROM weather_cache WHERE cityName NOT IN (SELECT cityName FROM weather_cache ORDER BY timestamp DESC, cityName LIMIT 100)")
    suspend fun pruneWeatherCache()

    @Transaction
    suspend fun insertWeather(weather: WeatherCacheEntity) {
        insertWeatherRaw(weather)
        pruneWeatherCache()
    }

    @Query("DELETE FROM weather_cache WHERE cityName = :legacyKey OR substr(cityName, 1, length(:prefix)) = :prefix")
    suspend fun deleteWeatherByPrefix(prefix: String, legacyKey: String)

    @Query("DELETE FROM weather_cache WHERE cityName = :city")
    suspend fun deleteWeather(city: String)

    @Query("SELECT * FROM travel_plans WHERE id = :id AND userId = :uid LIMIT 1")
    suspend fun getTravelPlanById(id: String, uid: String): TravelPlanEntity?

    @Query("SELECT * FROM travel_plans WHERE id = :id AND userId = :uid LIMIT 1")
    fun getTravelPlanByIdFlow(id: String, uid: String): kotlinx.coroutines.flow.Flow<TravelPlanEntity?>

    @Query("SELECT * FROM travel_plans WHERE userId = :uid ORDER BY startDate ASC")
    fun getAllTravelPlansFlow(uid: String): kotlinx.coroutines.flow.Flow<List<TravelPlanEntity>>

    @Query("SELECT * FROM travel_plans WHERE userId = :uid ORDER BY startDate ASC")
    suspend fun getAllTravelPlans(uid: String): List<TravelPlanEntity>

    @Query("SELECT * FROM travel_plans WHERE userId = :uid AND isDemo = 0 ORDER BY startDate ASC")
    suspend fun getUserTravelPlans(uid: String): List<TravelPlanEntity>

    @Query("SELECT * FROM travel_plans WHERE id = :id LIMIT 1")
    suspend fun getTravelPlanByIdRaw(id: String): TravelPlanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTravelPlanRaw(plan: TravelPlanEntity)

    @Transaction
    suspend fun insertTravelPlan(plan: TravelPlanEntity) {
        if (hasTripDeletion(plan.userId, plan.id)) return
        val existing = getTravelPlanByIdRaw(plan.id)
        if (existing != null && existing.userId != plan.userId) {
            return
        }
        if (pendingTripUploadVersion(plan.userId, plan.id) != null) return
        insertTravelPlanRaw(plan)
    }

    @Query("DELETE FROM travel_plans WHERE id = :id AND userId = :uid")
    suspend fun deleteTravelPlan(id: String, uid: String)

    @Query("DELETE FROM travel_plans WHERE userId = :uid")
    suspend fun clearAllTravelPlans(uid: String)

    @Query("DELETE FROM weather_cache")
    suspend fun clearAllWeatherCache()

    // AI History
    @Query("SELECT * FROM ai_history WHERE userId = :uid ORDER BY updatedAt DESC")
    suspend fun getAllAiHistory(uid: String): List<AiHistoryEntity>

    @Query("SELECT * FROM ai_history WHERE id = :id AND userId = :uid LIMIT 1")
    suspend fun getAiHistoryItem(id: String, uid: String): AiHistoryEntity?

    @Query("SELECT * FROM ai_history WHERE id = :id LIMIT 1")
    suspend fun getAiHistoryItemRaw(id: String): AiHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAiHistoryRaw(item: AiHistoryEntity)

    @Transaction
    suspend fun insertAiHistory(item: AiHistoryEntity) {
        check(!hasHistoryDeletion(item.userId, item.id)) { "History deletion is pending" }
        val existing = getAiHistoryItemRaw(item.id)
        if (existing != null && existing.userId != item.userId) {
            return
        }
        insertAiHistoryRaw(item)
    }

    @Query("DELETE FROM ai_history WHERE id = :id AND userId = :uid")
    suspend fun deleteAiHistory(id: String, uid: String)

    @Transaction
    suspend fun mergeAiHistory(item: AiHistoryEntity) {
        if (hasHistoryDeletion(item.userId, item.id)) return
        val existing = getAiHistoryItemRaw(item.id)
        if (existing != null && (existing.userId != item.userId || existing.updatedAt > item.updatedAt || existing.pendingUpload)) return
        insertAiHistoryRaw(item)
    }

    @Query("SELECT * FROM ai_history WHERE userId = :uid AND pendingUpload = 1 AND NOT EXISTS (SELECT 1 FROM ai_history_deletions d WHERE d.userId = :uid AND (d.recordId = '' OR d.recordId = ai_history.id)) ORDER BY updatedAt")
    suspend fun getPendingAiHistory(uid: String): List<AiHistoryEntity>

    @Query("UPDATE ai_history SET pendingUpload = 0, syncedUpdatedAt = :version WHERE id = :id AND userId = :uid AND updatedAt = :version")
    suspend fun acknowledgeAiHistory(id: String, uid: String, version: Long)

    @Query("UPDATE ai_history SET pendingUpload = 0 WHERE userId = :uid AND (:id IS NULL OR id = :id)")
    suspend fun stopAiHistoryUpload(uid: String, id: String?)

    @Query("SELECT EXISTS(SELECT 1 FROM ai_history_deletions WHERE userId = :uid AND (recordId = '' OR recordId = :id))")
    suspend fun hasHistoryDeletion(uid: String, id: String): Boolean

    @Query("SELECT * FROM ai_history_deletions WHERE userId = :uid AND completed = 0 ORDER BY recordId")
    suspend fun getHistoryDeletions(uid: String): List<HistoryDeletionRequest>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertHistoryDeletion(request: HistoryDeletionRequest)

    @Query("DELETE FROM ai_history_deletions WHERE userId = :uid AND recordId = :id")
    suspend fun removeHistoryDeletion(uid: String, id: String)

    @Transaction
    suspend fun enqueueHistoryDeletion(uid: String, id: String?) {
        insertHistoryDeletion(HistoryDeletionRequest(uid, id ?: ""))
        stopAiHistoryUpload(uid, id)
    }

    @Query("UPDATE ai_history_deletions SET completed = 1 WHERE userId = :uid AND recordId = :id")
    suspend fun acknowledgeHistoryDeletion(uid: String, id: String)

    @Query("DELETE FROM ai_history_deletions WHERE userId = :uid")
    suspend fun clearHistoryDeletions(uid: String)

    @Transaction
    suspend fun completeHistoryDeletion(uid: String, id: String) {
        if (id.isEmpty()) {
            getAllAiHistory(uid).forEach { insertHistoryDeletion(HistoryDeletionRequest(uid, it.id, true)) }
            clearAllAiHistory(uid)
            removeHistoryDeletion(uid, id)
        } else {
            deleteAiHistory(id, uid)
            acknowledgeHistoryDeletion(uid, id)
        }
    }

    /** Full, server-confirmed collection only. Unknown/local-only records are retained. */
    @Transaction
    suspend fun reconcileAiHistory(uid: String, items: List<AiHistoryEntity>, serverIds: Set<String>) {
        items.filter { it.userId == uid }.forEach { mergeAiHistory(it.copy(syncedUpdatedAt = it.updatedAt, pendingUpload = false)) }
        getAllAiHistory(uid).forEach { local ->
            if (local.id !in serverIds && local.syncedUpdatedAt == local.updatedAt && !local.pendingUpload) {
                deleteAiHistory(local.id, uid)
            }
        }
    }

    @Query("DELETE FROM ai_history WHERE id = :id AND userId = :uid AND updatedAt <= :removedUpdatedAt")
    suspend fun deleteAiHistoryIfUnchanged(id: String, uid: String, removedUpdatedAt: Long)

    @Query("SELECT COUNT(*) FROM trip_deletions WHERE userId = :uid AND completed = 0")
    fun pendingTripDeletionCount(uid: String): kotlinx.coroutines.flow.Flow<Int>

    @Query("SELECT COUNT(*) FROM trip_uploads WHERE userId = :uid AND NOT EXISTS (SELECT 1 FROM trip_deletions d WHERE d.userId = :uid AND d.recordId = trip_uploads.recordId)")
    fun pendingTripUploadCount(uid: String): kotlinx.coroutines.flow.Flow<Int>

    @Query("SELECT revision FROM trip_uploads WHERE userId = :uid AND recordId = :id")
    suspend fun pendingTripUploadVersion(uid: String, id: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueueTripUpload(request: TripUploadRequest)

    @Query("SELECT * FROM trip_uploads WHERE userId = :uid")
    suspend fun pendingTripUploads(uid: String): List<TripUploadRequest>

    @Query("DELETE FROM trip_uploads WHERE userId = :uid AND recordId = :id AND revision = :revision")
    suspend fun acknowledgeTripUpload(uid: String, id: String, revision: Long)

    @Query("DELETE FROM trip_uploads WHERE userId = :uid")
    suspend fun clearTripUploads(uid: String)

    @Transaction
    suspend fun saveLocalTrip(plan: TravelPlanEntity) {
        if (hasTripDeletion(plan.userId, plan.id)) return
        val old = getTravelPlanByIdRaw(plan.id)
        if (old != null && old.userId != plan.userId) return
        val revision = maxOf(plan.updatedAt, (old?.updatedAt ?: 0) + 1)
        insertTravelPlanRaw(plan.copy(updatedAt = revision))
        if (plan.userId != "legacy") enqueueTripUpload(TripUploadRequest(plan.userId, plan.id, revision))
    }

    @Query("SELECT EXISTS(SELECT 1 FROM trip_deletions WHERE userId = :uid AND recordId = :id)")
    suspend fun hasTripDeletion(uid: String, id: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueueTripDeletion(request: TripDeletionRequest)

    @Query("SELECT * FROM trip_deletions WHERE userId = :uid AND completed = 0")
    suspend fun pendingTripDeletions(uid: String): List<TripDeletionRequest>

    @Query("UPDATE trip_deletions SET completed = 1 WHERE userId = :uid AND recordId = :id")
    suspend fun acknowledgeTripDeletion(uid: String, id: String)

    @Transaction
    suspend fun completeTripDeletion(uid: String, id: String) {
        deleteTravelPlan(id, uid)
        pendingTripUploadVersion(uid, id)?.let { acknowledgeTripUpload(uid, id, it) }
        acknowledgeTripDeletion(uid, id)
    }

    @Query("DELETE FROM trip_deletions WHERE userId = :uid")
    suspend fun clearTripDeletions(uid: String)

    @Query("DELETE FROM ai_history WHERE userId = :uid")
    suspend fun clearAllAiHistory(uid: String)
}

/**
 * Room Database Tanımı
 */
@Entity(tableName = "trip_deletions", primaryKeys = ["userId", "recordId"])
data class TripDeletionRequest(val userId: String, val recordId: String, val completed: Boolean = false)

@Entity(tableName = "trip_uploads", primaryKeys = ["userId", "recordId"])
data class TripUploadRequest(val userId: String, val recordId: String, val revision: Long)

@Database(entities = [WeatherCacheEntity::class, TravelPlanEntity::class, AiHistoryEntity::class, HistoryDeletionRequest::class, TripDeletionRequest::class, TripUploadRequest::class], version = 22, exportSchema = false)
@TypeConverters(ChatTypeConverters::class)
abstract class WeatherDatabase : RoomDatabase() {
    abstract fun weatherDao(): WeatherDao

    companion object {
        @Volatile
        private var INSTANCE: WeatherDatabase? = null

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE travel_plans ADD COLUMN isDemo INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE travel_plans ADD COLUMN userId TEXT NOT NULL DEFAULT 'legacy'")
                database.execSQL("ALTER TABLE ai_history ADD COLUMN userId TEXT NOT NULL DEFAULT 'legacy'")
            }
        }

        // The historical release jumped directly from 11 to 13.
        val MIGRATION_11_13 = object : Migration(11, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE travel_plans ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                database.execSQL("UPDATE travel_plans SET updatedAt = createdAt")
                database.execSQL("ALTER TABLE travel_plans ADD COLUMN archivedAt INTEGER")
                database.execSQL("ALTER TABLE travel_plans ADD COLUMN lastAnalysisAt INTEGER")
                database.execSQL("UPDATE travel_plans SET lastAnalysisAt = lastWeatherAnalysisDate")
                for (column in listOf("packingAdvice", "mustSee", "foodAdvice", "localAdvice")) {
                    database.execSQL("ALTER TABLE travel_plans ADD COLUMN $column TEXT")
                }
                database.execSQL("ALTER TABLE travel_plans ADD COLUMN comfortScore INTEGER")
                database.execSQL("ALTER TABLE ai_history ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                database.execSQL("UPDATE ai_history SET updatedAt = timestamp")
                database.execSQL("UPDATE travel_plans SET weatherAnalysisStatus = 'WAITING_FOR_WINDOW' WHERE weatherAnalysisStatus = 'TOO_EARLY'")
            }
        }

        /** İlçe bazlı konum seçimi + opsiyonel kalkış noktası alanları (v14). */
        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE travel_plans ADD COLUMN district TEXT")
                database.execSQL("ALTER TABLE travel_plans ADD COLUMN originCity TEXT")
                database.execSQL("ALTER TABLE travel_plans ADD COLUMN originDistrict TEXT")
                database.execSQL("ALTER TABLE travel_plans ADD COLUMN originLatitude REAL")
                database.execSQL("ALTER TABLE travel_plans ADD COLUMN originLongitude REAL")
            }
        }

        /** Yola çıkış saati alanı (v15). */
        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE travel_plans ADD COLUMN departureTime TEXT")
            }
        }

        /** Güzergâh hava özeti ve analiz tarihi (v16). */
        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE travel_plans ADD COLUMN routeWeatherSummary TEXT")
                database.execSQL("ALTER TABLE travel_plans ADD COLUMN lastRouteAnalysisAt INTEGER")
            }
        }

        /** Kullanıcı bazlı sorgular için indeksleme (v17). */
        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE INDEX IF NOT EXISTS index_travel_plans_userId ON travel_plans(userId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_ai_history_userId ON ai_history(userId)")
            }
        }

        val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE ai_history ADD COLUMN syncedUpdatedAt INTEGER DEFAULT NULL")
            }
        }

        val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE ai_history ADD COLUMN pendingUpload INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS trip_uploads (userId TEXT NOT NULL, recordId TEXT NOT NULL, revision INTEGER NOT NULL, PRIMARY KEY(userId, recordId))")
            }
        }

        val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS trip_deletions (userId TEXT NOT NULL, recordId TEXT NOT NULL, completed INTEGER NOT NULL, PRIMARY KEY(userId, recordId))")
            }
        }

        val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS ai_history_deletions (userId TEXT NOT NULL, recordId TEXT NOT NULL, completed INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(userId, recordId))")
            }
        }

        fun getDatabase(context: android.content.Context): WeatherDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WeatherDatabase::class.java,
                    "weather_database"
                )
                .addMigrations(MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21, MIGRATION_21_22)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
