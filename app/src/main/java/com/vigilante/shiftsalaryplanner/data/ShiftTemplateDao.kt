package com.vigilante.shiftsalaryplanner.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import androidx.room.Delete

@Dao
interface ShiftTemplateDao {

    @Query("SELECT * FROM shift_templates ORDER BY sortOrder, title")
    fun observeAll(): Flow<List<ShiftTemplateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ShiftTemplateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<ShiftTemplateEntity>)

    @Delete
    suspend fun delete(template: ShiftTemplateEntity)
}
