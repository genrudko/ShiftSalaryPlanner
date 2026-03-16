package com.vigilante.shiftsalaryplanner.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HolidayDao {

    @Query("SELECT * FROM holiday_days WHERE scopeCode = :scopeCode ORDER BY date")
    fun observeByScope(scopeCode: String = "RU-FED"): Flow<List<HolidayEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<HolidayEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: HolidayEntity)

    @Query("DELETE FROM holiday_days WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM holiday_days WHERE scopeCode = :scopeCode AND date LIKE :yearMask")
    suspend fun deleteByYear(scopeCode: String, yearMask: String)
}
