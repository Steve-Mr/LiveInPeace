package com.maary.liveinpeace.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Connection::class], version = 2, exportSchema = false)
abstract class ConnectionRoomDatabase : RoomDatabase() {

    abstract fun connectionDao(): ConnectionDao

    companion object {
        @Volatile
        private var INSTANCE: ConnectionRoomDatabase? = null

        // 2. 定义从版本 1 到 2 的迁移策略
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // SQLite 的 ALTER TABLE 命令一次只能添加一列，所以需要两条语句
                // 注意：这里的数据类型 INTEGER 对应 Kotlin 的 Int? (Nullable)
                // 如果你的实体中定义的是非空 Int，这里可能需要指定 DEFAULT 0
                db.execSQL("ALTER TABLE connection_table ADD COLUMN start_volume INTEGER")
                db.execSQL("ALTER TABLE connection_table ADD COLUMN end_volume INTEGER")
            }
        }

        fun getDatabase(context: Context): ConnectionRoomDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                                context.applicationContext,
                                ConnectionRoomDatabase::class.java,
                                "connection_database"
                            ).addMigrations(MIGRATION_1_2).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
