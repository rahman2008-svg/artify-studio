package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DesignDraftDao {
    @Query("SELECT * FROM design_drafts ORDER BY lastUpdated DESC")
    fun getAllDrafts(): Flow<List<DesignDraft>>

    @Query("SELECT * FROM design_drafts WHERE id = :id LIMIT 1")
    suspend fun getDraftById(id: Int): DesignDraft?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDraft(draft: DesignDraft): Long

    @Query("DELETE FROM design_drafts WHERE id = :id")
    suspend fun deleteDraftById(id: Int)
}
