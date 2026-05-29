package com.notifyforward.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.notifyforward.app.data.dao.ForwardHistoryDao
import com.notifyforward.app.data.dao.ForwardRuleDao
import com.notifyforward.app.data.entity.ForwardHistory
import com.notifyforward.app.data.entity.ForwardRule

@Database(
    entities = [ForwardRule::class, ForwardHistory::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun forwardRuleDao(): ForwardRuleDao
    abstract fun forwardHistoryDao(): ForwardHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun create(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "notify_forward.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
