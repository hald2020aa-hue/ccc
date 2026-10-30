package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sites")
data class SiteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val url: String,
    val domain: String,
    val isEnabled: Boolean = true,
    val sortOrder: Int = 0,
    val iconKey: String = "globe",
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val siteName: String,
    val category: String = "الكل",
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorite_categories")
data class FavoriteCategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sortOrder: Int = 0
)

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val siteName: String,
    val visitedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "saved_works")
data class SavedWorkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val sourceUrl: String,
    val localFilePath: String,
    val fileSizeBytes: Long,
    val fileType: String, // "DOWNLOAD", "PAGE", "IMPORT"
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "download_tasks")
data class DownloadTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val status: String, // "DOWNLOADING", "COMPLETED", "FAILED", "PAUSED"
    val progress: Int = 0,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 0L,
    val localPath: String = "",
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
