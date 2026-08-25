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
@Entity(tableName = "travel_plans")
@IgnoreExtraProperties
@Keep
data class TravelPlanEntity(
    @PrimaryKey val id: String = "",
    val userId: String = "legacy",
    val city: String = "",
    val district: String? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val originCity: String? = null,
    val originDistrict: String? = null,
    val originLatitude: Double? = null,
    val originLongitude: Double? = null,
    val tripType: String = "",
    val startDate: Long = 0L,
    val endDate: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val archivedAt: Long? = null,
    val lastAnalysisAt: Long? = null,
    val weatherSummary: String? = null,
    val packingAdvice: String? = null,
    val mustSee: String? = null,
    val foodAdvice: String? = null,
    val localAdvice: String? = null,
    val aiSuggestion: String? = null,
    val comfortScore: Int? = null,
    val userNote: String? = null,
    val userRating: Int? = 0,
    val lastWeatherAnalysisText: String? = null,
    val lastWeatherAnalysisDate: Long? = null,
    val lastForecastSnapshot: ForecastSnapshot? = null,
    val previousForecastSnapshot: ForecastSnapshot? = null,
    val nextAnalysisEligibleDate: Long? = null,
    val weatherAnalysisStatus: String = "WAITING_FOR_WINDOW",
    @ColumnInfo(defaultValue = "0")
    @get:PropertyName("isArchived")
    @set:PropertyName("isArchived")
    var isArchived: Boolean = false,
    val analyses: List<TravelWeatherAnalysis> = emptyList(),
    val lastDailyNotificationDate: String? = null,
    @ColumnInfo(defaultValue = "0")
    @get:PropertyName("isDemo")
    @set:PropertyName("isDemo")
    var isDemo: Boolean = false,
    val departureTime: String? = null,
    val routeWeatherSummary: String? = null,
    val lastRouteAnalysisAt: Long? = null
)

/**
 * AI Analiz Geçmişini saklamak için Entity
 */
@Entity(tableName = "ai_history")
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
    suspend fun insertWeather(weather: WeatherCacheEntity)

    @Query("DELETE FROM weather_cache WHERE cityName = :city")
    suspend fun deleteWeather(city: String)

    // Travel Plans
    @Query("SELECT * FROM travel_plans WHERE userId = :uid ORDER BY startDate ASC")
    fun getAllTravelPlansFlow(uid: String): kotlinx.coroutines.flow.Flow<List<TravelPlanEntity>>

    @Query("SELECT * FROM travel_plans WHERE userId = :uid ORDER BY startDate ASC")
    suspend fun getAllTravelPlans(uid: String): List<TravelPlanEntity>

    @Query("SELECT * FROM travel_plans WHERE userId = :uid AND isDemo = 0 ORDER BY startDate ASC")
    suspend fun getUserTravelPlans(uid: String): List<TravelPlanEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTravelPlan(plan: TravelPlanEntity)

    @Query("DELETE FROM travel_plans WHERE id = :id")
    suspend fun deleteTravelPlan(id: String)

    @Query("DELETE FROM travel_plans WHERE userId = :uid")
    suspend fun clearAllTravelPlans(uid: String)

    @Query("DELETE FROM weather_cache")
    suspend fun clearAllWeatherCache()

    // AI History
    @Query("SELECT * FROM ai_history WHERE userId = :uid ORDER BY updatedAt DESC")
    suspend fun getAllAiHistory(uid: String): List<AiHistoryEntity>

    @Query("SELECT * FROM ai_history WHERE id = :id LIMIT 1")
    suspend fun getAiHistoryItem(id: String): AiHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAiHistory(item: AiHistoryEntity)

    @Query("DELETE FROM ai_history WHERE id = :id")
    suspend fun deleteAiHistory(id: String)

    @Query("DELETE FROM ai_history WHERE userId = :uid")
    suspend fun clearAllAiHistory(uid: String)
}

/**
 * Room Database Tanımı
 */
@Database(entities = [WeatherCacheEntity::class, TravelPlanEntity::class, AiHistoryEntity::class], version = 16, exportSchema = false)
@TypeConverters(ChatTypeConverters::class)
abstract class WeatherDatabase : RoomDatabase() {
    abstract fun weatherDao(): WeatherDao

    companion object {
        @Volatile
        private var INSTANCE: WeatherDatabase? = null

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE travel_plans ADD COLUMN userId TEXT NOT NULL DEFAULT 'legacy'")
                database.execSQL("ALTER TABLE ai_history ADD COLUMN userId TEXT NOT NULL DEFAULT 'legacy'")
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

        fun getDatabase(context: android.content.Context): WeatherDatabase {
            return INSTANCE ?: synchronized(this) {
                try {
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        WeatherDatabase::class.java,
                        "weather_database"
                    )
                    .addMigrations(MIGRATION_10_11, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16)
                    .fallbackToDestructiveMigration()
                    .build()
                    INSTANCE = instance
                    instance
                } catch (e: Exception) {
                    context.deleteDatabase("weather_database")
                    Room.databaseBuilder(
                        context.applicationContext,
                        WeatherDatabase::class.java,
                        "weather_database"
                    ).build()
                }
            }
        }
    }
}
