package com.mela.ussdrunner.data.local.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PresetDao {
    @Query("SELECT * FROM presets ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<PresetEntity>>

    @Query("SELECT * FROM presets WHERE id = :id")
    suspend fun getById(id: String): PresetEntity?

    @Query("SELECT * FROM presets WHERE id = :id")
    fun observeById(id: String): Flow<PresetEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PresetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<PresetEntity>)

    @Update
    suspend fun update(entity: PresetEntity)

    @Delete
    suspend fun delete(entity: PresetEntity)

    @Query("DELETE FROM presets WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM presets")
    suspend fun count(): Int
}
