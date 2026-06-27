package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "design_drafts")
data class DesignDraft(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val canvasWidth: Int = 1080,
    val canvasHeight: Int = 1080,
    val backgroundColor: Int = 0xFFFFFFFF.toInt(),
    val serializedElements: String, // JSON serialization of our Canvas Elements list
    val thumbnailPath: String? = null, // local filepath of the thumbnail preview
    val lastUpdated: Long = System.currentTimeMillis()
)
