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
    @PrimaryKey val id: String, // Acts as conversationId
    val userId: String = "legacy",
    val title: String,
    val summary: String,
    val messages: List<AltikodChatMessage>,
    val cityName: String?,
    val timestamp: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

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
        val existing = getTravelPlanByIdRaw(plan.id)
        if (existing != null && existing.userId != plan.userId) {
            return
        }
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
        val existing = getAiHistoryItemRaw(item.id)
        if (existing != null && existing.userId != item.userId) {
            return
        }
        insertAiHistoryRaw(item)
    }

    @Query("DELETE FROM ai_history WHERE id = :id AND userId = :uid")
    suspend fun deleteAiHistory(id: String, uid: String)

    @Query("DELETE FROM ai_history WHERE userId = :uid")
    suspend fun clearAllAiHistory(uid: String)
}

/**
 * Room Database Tanımı
 */
@Database(entities = [WeatherCacheEntity::class, TravelPlanEntity::class, AiHistoryEntity::class], version = 17, exportSchema = false)
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

        fun getDatabase(context: android.content.Context): WeatherDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WeatherDatabase::class.java,
                    "weather_database"
                )
                .addMigrations(MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
