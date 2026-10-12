package com.vigilante.shiftsalaryplanner.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ShiftDayDao {

    @Query("SELECT * FROM shift_days ORDER BY date")
    fun observeAll(): Flow<List<ShiftDayEntity>>

    // ISO-8601 YYYY-MM-DD text preserves chronological order. The date is the
    // primary key, so the range lookup can use its existing SQLite index.
    @Query("SELECT * FROM shift_days WHERE date BETWEEN :startDate AND :endDate ORDER BY date")
    suspend fun listBetween(startDate: String, endDate: String): List<ShiftDayEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ShiftDayEntity)

    @Query("DELETE FROM shift_days WHERE date = :date")
    suspend fun deleteByDate(date: String)

    @Query("DELETE FROM shift_days WHERE date BETWEEN :startDate AND :endDate")
    suspend fun deleteByDateRange(startDate: String, endDate: String)

    @Query("DELETE FROM shift_days")
    suspend fun clearAll()
}
