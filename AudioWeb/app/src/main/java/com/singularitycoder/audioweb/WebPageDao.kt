package com.singularitycoder.audioweb

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface WebPageDao {

    // Single Item CRUD ops ------------------------------------------------------------------------------------------------------------------------
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(webPage: WebPage)

    @Transaction
    @Query("SELECT * FROM $TABLE_WEB_PAGE WHERE description LIKE :desc LIMIT 1")
    suspend fun getWebPageByDesc(desc: String): WebPage?

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(webPage: WebPage)

    @Delete
    suspend fun delete(webPage: WebPage)

    // ---------------------------------------------------------------------------------------------------------------------------------------------

    // All of the parameters of the Insert method must either be classes annotated with Entity or collections/array of it.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(webPageList: List<WebPage>)

    @Query("SELECT * FROM $TABLE_WEB_PAGE")
    fun getAllWebPageListLiveData(): LiveData<List<WebPage>>

    @Query("SELECT * FROM $TABLE_WEB_PAGE")
    suspend fun getAll(): List<WebPage>

    @Query("DELETE FROM $TABLE_WEB_PAGE")
    suspend fun deleteAll()
}
