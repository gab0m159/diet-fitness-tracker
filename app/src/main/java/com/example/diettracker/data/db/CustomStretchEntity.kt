package com.example.diettracker.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A user-created stretch.
 *
 * Mirrors the built-in [com.example.diettracker.domain.StretchGuide] fields plus a
 * free-form description and optional media, so a custom entry can be as rich as a
 * bundled one.
 *
 * [mediaUrl] is a photo or video link. Nothing is ever fetched automatically —
 * the value is displayed as text and the UI keeps a placeholder box when empty.
 */
@Entity(tableName = "custom_stretches")
data class CustomStretchEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "name")
    val name: String,

    /** Target muscle, e.g. "胸大肌". Used for grouping. */
    @ColumnInfo(name = "target_muscle")
    val targetMuscle: String,

    @ColumnInfo(name = "hold_seconds_min")
    val holdSecondsMin: Int = 20,

    @ColumnInfo(name = "hold_seconds_max")
    val holdSecondsMax: Int = 30,

    @ColumnInfo(name = "sets")
    val sets: Int = 3,

    /** Step-by-step execution cue. */
    @ColumnInfo(name = "how_to")
    val howTo: String = "",

    /** Longer description: purpose, mistakes, safety notes. */
    @ColumnInfo(name = "description")
    val description: String = "",

    /** Photo or video URL. Display only. */
    @ColumnInfo(name = "media_url")
    val mediaUrl: String = "",

    /** Where the user got it from. */
    @ColumnInfo(name = "source")
    val source: String = "用户自建",

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
