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

    @Query("SELECT COUNT(*) FROM shift_templates")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ShiftTemplateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<ShiftTemplateEntity>)

    @Query("DELETE FROM shift_templates WHERE code = :code")
    suspend fun deleteByCode(code: String)

    @Delete
    suspend fun delete(template: ShiftTemplateEntity)
}
