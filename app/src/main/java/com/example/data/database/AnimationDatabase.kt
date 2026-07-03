package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.AnimationDao
import com.example.data.model.DrawPathEntity
import com.example.data.model.FrameEntity
import com.example.data.model.LayerEntity
import com.example.data.model.ProjectEntity

@Database(
    entities = [
        ProjectEntity::class,
        LayerEntity::class,
        FrameEntity::class,
        DrawPathEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AnimationDatabase : RoomDatabase() {

    abstract fun animationDao(): AnimationDao

    companion object {
        @Volatile
        private var INSTANCE: AnimationDatabase? = null

        fun getDatabase(context: Context): AnimationDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AnimationDatabase::class.java,
                    "animation_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
