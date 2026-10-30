package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // Sites
    @Query("SELECT * FROM sites ORDER BY sortOrder ASC, addedAt ASC")
    fun getAllSites(): Flow<List<SiteEntity>>

    @Query("SELECT * FROM sites WHERE isEnabled = 1 ORDER BY sortOrder ASC, addedAt ASC")
    fun getEnabledSites(): Flow<List<SiteEntity>>

    @Query("SELECT * FROM sites ORDER BY sortOrder ASC, addedAt ASC")
    suspend fun getAllSitesOnce(): List<SiteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSite(site: SiteEntity): Long

    @Update
    suspend fun updateSite(site: SiteEntity)

    @Delete
    suspend fun deleteSite(site: SiteEntity)

    @Query("DELETE FROM sites WHERE id = :id")
    suspend fun deleteSiteById(id: Long)

    // Favorites
    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    suspend fun getAllFavoritesOnce(): List<FavoriteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteEntity): Long

    @Delete
    suspend fun deleteFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE url = :url")
    suspend fun deleteFavoriteByUrl(url: String)

    // Favorite Categories
    @Query("SELECT * FROM favorite_categories ORDER BY sortOrder ASC, id ASC")
    fun getAllCategories(): Flow<List<FavoriteCategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: FavoriteCategoryEntity): Long

    @Delete
    suspend fun deleteCategory(category: FavoriteCategoryEntity)

    // History
    @Query("SELECT * FROM history ORDER BY visitedAt DESC")
    fun getAllHistory(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history ORDER BY visitedAt DESC")
    suspend fun getAllHistoryOnce(): List<HistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: HistoryEntity): Long

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun deleteHistoryById(id: Long)

    @Query("DELETE FROM history")
    suspend fun clearHistory()

    // Saved Works (Storage)
    @Query("SELECT * FROM saved_works ORDER BY createdAt DESC")
    fun getAllSavedWorks(): Flow<List<SavedWorkEntity>>

    @Query("SELECT * FROM saved_works ORDER BY createdAt DESC")
    suspend fun getAllSavedWorksOnce(): List<SavedWorkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedWork(work: SavedWorkEntity): Long

    @Delete
    suspend fun deleteSavedWork(work: SavedWorkEntity)

    @Query("DELETE FROM saved_works")
    suspend fun clearSavedWorks()

    // Download Tasks
    @Query("SELECT * FROM download_tasks ORDER BY createdAt DESC")
    fun getAllDownloadTasks(): Flow<List<DownloadTaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownloadTask(task: DownloadTaskEntity): Long

    @Update
    suspend fun updateDownloadTask(task: DownloadTaskEntity)

    @Delete
    suspend fun deleteDownloadTask(task: DownloadTaskEntity)

    @Query("DELETE FROM download_tasks")
    suspend fun clearDownloadTasks()
}
