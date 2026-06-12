package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "channels")
data class Channel(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val url: String,
    val category: String,
    val isPreset: Boolean = false,
    val isFavorite: Boolean = false,
    val addedAt: Long = System.currentTimeMillis()
)
