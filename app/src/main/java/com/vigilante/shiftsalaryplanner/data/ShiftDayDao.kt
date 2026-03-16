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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ShiftDayEntity)

    @Query("DELETE FROM shift_days WHERE date = :date")
    suspend fun deleteByDate(date: String)
}