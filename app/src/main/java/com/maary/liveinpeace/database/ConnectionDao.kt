package com.maary.liveinpeace.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.sql.Date

@Dao
interface ConnectionDao {

    @Query("SELECT * FROM connection_table WHERE date = :queryDate")
    fun loadAllConnectionsOnDate(queryDate: String): Flow<List<Connection>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(connection: Connection)
}