package com.maary.liveinpeace.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import java.sql.Date

@Database(entities = [Connection::class], version = 1, exportSchema = false)
public abstract class ConnectionRoomDatabase : RoomDatabase() {

    abstract fun connectionDao(): ConnectionDao

    companion object {
        @Volatile
        private var INSTANCE: ConnectionRoomDatabase? = null

        fun getDatabase(context: Context): ConnectionRoomDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ConnectionRoomDatabase::class.java,
                    "connection_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

//class Converters {
//    @TypeConverter
//    fun fromTimestamp(value: Long?): Date? {
//        return value?.let { Date(it) }
//    }
//
//    @TypeConverter
//    fun dateToTimestamp(date: Date?): Long? {
//        return date?.time?.toLong()
//    }
//}
