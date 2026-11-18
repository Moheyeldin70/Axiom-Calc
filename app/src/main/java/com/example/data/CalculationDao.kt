package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CalculationDao {
    @Query("SELECT * FROM calculation_history ORDER BY timestamp DESC, id DESC LIMIT 150")
    fun observeHistory(): Flow<List<CalculationRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: CalculationRecord)

    @Query("DELETE FROM calculation_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM calculation_history")
    suspend fun clearAll()
}
