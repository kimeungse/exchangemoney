package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// Caches the latest rates
@Entity(tableName = "exchange_rate_cache")
data class ExchangeRateCacheEntity(
    @PrimaryKey val id: Int = 1,
    val krwRate: Double,
    val tjsRate: Double,
    val timestamp: Long = System.currentTimeMillis()
)

// List of recent conversions
@Entity(tableName = "conversion_history")
data class ConversionHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sourceAmount: Double,
    val sourceCurrency: String,
    val targetAmount: Double,
    val targetCurrency: String,
    val timestamp: Long = System.currentTimeMillis()
)

// Daily historical rate entry for 1-year KRW <-> TJS <-> USD tracking
@Entity(tableName = "historical_rates")
data class HistoricalRateEntity(
    @PrimaryKey val dateStr: String, // YYYY-MM-DD
    val epochMillis: Long,
    val krwToUsd: Double,
    val tjsToUsd: Double,
    val krwPerTjs: Double,
    val tjsPer1000Krw: Double
)

@Dao
interface ExchangeDao {
    @Query("SELECT * FROM exchange_rate_cache WHERE id = 1 LIMIT 1")
    suspend fun getExchangeRateCache(): ExchangeRateCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveExchangeRateCache(cache: ExchangeRateCacheEntity)

    @Query("SELECT * FROM conversion_history ORDER BY timestamp DESC LIMIT 30")
    fun getConversionHistory(): Flow<List<ConversionHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoryItem(item: ConversionHistoryEntity)

    @Query("DELETE FROM conversion_history WHERE id = :id")
    suspend fun deleteHistoryItemById(id: Int)

    @Query("DELETE FROM conversion_history")
    suspend fun clearHistory()

    @Query("SELECT * FROM historical_rates WHERE epochMillis BETWEEN :startMillis AND :endMillis ORDER BY epochMillis ASC")
    suspend fun getHistoricalRatesInRange(startMillis: Long, endMillis: Long): List<HistoricalRateEntity>

    @Query("SELECT COUNT(*) FROM historical_rates")
    suspend fun getHistoricalRateCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoricalRates(rates: List<HistoricalRateEntity>)
}

@Database(
    entities = [
        ExchangeRateCacheEntity::class,
        ConversionHistoryEntity::class,
        HistoricalRateEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class ExchangeDatabase : RoomDatabase() {
    abstract fun exchangeDao(): ExchangeDao
}
