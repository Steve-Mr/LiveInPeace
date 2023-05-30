package com.maary.liveinpeace.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.sql.Date

@Dao
interface ConnectionDao {

    @Query("SELECT * FROM connection_table WHERE date = :queryDate ORDER BY connected_time DESC")
    fun loadAllConnectionsOnDate(queryDate: String): Flow<List<Connection>>

    @Query("SELECT id, name, type, 0 AS connected_time, 0 AS disconnected_time, SUM(duration) AS duration, date FROM connection_table WHERE date = :queryDate GROUP BY name")
    fun loadSummaryOnDate(queryDate: String): Flow<List<Connection>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(connection: Connection)
}