package com.havamania

import androidx.room.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer

/**
 * TypeConverter for AI Chat Messages
 */
class ChatTypeConverters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromChatMessageList(value: List<AltikodChatMessage>): String {
        return json.encodeToString(value)
    }

    @TypeConverter
    fun toChatMessageList(value: String): List<AltikodChatMessage> {
        return try {
            json.decodeFromString(value)
        } catch (e: Exception) {
            emptyList()
        }
    }
}

/**
 * Hava durumu verilerini veritabanında saklamak için Entity
 */
@Entity(tableName = "weather_cache")
data class WeatherCacheEntity(
    @PrimaryKey val cityName: String,
    val jsonData: String, // WeatherData nesnesi JSON olarak saklanacak
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Seyahat Planlarını saklamak için Entity
 */
@Entity(tableName = "travel_plans")
data class TravelPlanEntity(
    @PrimaryKey val id: String,
    val city: String,
    val tripType: String,
    val startDate: Long,
    val endDate: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val weatherSummary: String? = null,
    val aiSuggestion: String? = null
)

/**
 * AI Analiz Geçmişini saklamak için Entity
 */
@Entity(tableName = "ai_history")
data class AiHistoryEntity(
    @PrimaryKey val id: String,
    val title: String,
    val summary: String,
    val messages: List<AltikodChatMessage>,
    val cityName: String?,
    val timestamp: Long = System.currentTimeMillis()
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
    @Query("SELECT * FROM travel_plans ORDER BY startDate ASC")
    suspend fun getAllTravelPlans(): List<TravelPlanEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTravelPlan(plan: TravelPlanEntity)

    @Query("DELETE FROM travel_plans WHERE id = :id")
    suspend fun deleteTravelPlan(id: String)

    @Query("DELETE FROM travel_plans")
    suspend fun clearAllTravelPlans()

    // AI History
    @Query("SELECT * FROM ai_history ORDER BY timestamp DESC")
    suspend fun getAllAiHistory(): List<AiHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAiHistory(item: AiHistoryEntity)

    @Query("DELETE FROM ai_history WHERE id = :id")
    suspend fun deleteAiHistory(id: String)

    @Query("DELETE FROM ai_history")
    suspend fun clearAllAiHistory()
}

/**
 * Room Database Tanımı
 */
@Database(entities = [WeatherCacheEntity::class, TravelPlanEntity::class, AiHistoryEntity::class], version = 4, exportSchema = false)
@TypeConverters(ChatTypeConverters::class)
abstract class WeatherDatabase : RoomDatabase() {
    abstract fun weatherDao(): WeatherDao

    companion object {
        @Volatile
        private var INSTANCE: WeatherDatabase? = null

        fun getDatabase(context: android.content.Context): WeatherDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WeatherDatabase::class.java,
                    "weather_database"
                )
                .fallbackToDestructiveMigration() // Şimdilik geliştirme için kolaylık
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
