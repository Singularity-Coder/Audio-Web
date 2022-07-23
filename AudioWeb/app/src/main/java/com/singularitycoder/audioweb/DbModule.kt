package com.singularitycoder.audioweb

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DbModule {

    @Singleton
    @Provides
    fun injectAudioWebRoomDatabase(@ApplicationContext context: Context): AudioWebDatabase {
        return Room.databaseBuilder(context, AudioWebDatabase::class.java, DB_AUDIO_WEB).build()
    }

    @Singleton
    @Provides
    fun injectWebPageDao(db: AudioWebDatabase): WebPageDao = db.webPageDao()
}
