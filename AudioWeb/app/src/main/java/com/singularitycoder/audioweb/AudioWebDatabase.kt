package com.singularitycoder.audioweb

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        WebPage::class,
    ],
    version = 1,
    exportSchema = false
)
abstract class AudioWebDatabase : RoomDatabase() {
    abstract fun webPageDao(): WebPageDao
}

