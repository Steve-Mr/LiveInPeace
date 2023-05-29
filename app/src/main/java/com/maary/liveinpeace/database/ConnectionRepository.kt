package com.maary.liveinpeace.database

import androidx.annotation.WorkerThread
import kotlinx.coroutines.flow.Flow
import java.sql.Date

class ConnectionRepository(private val connectionDao: ConnectionDao) {

//    val allConnectionsOnDate: Flow<List<Connection>> = connectionDao.loadAllConnectionsOnDate(queryDate)

    fun getAllConnectionsOnDate(queryDate: String): Flow<List<Connection>> {
        return connectionDao.loadAllConnectionsOnDate(queryDate)
    }

    @WorkerThread
    suspend fun insert(connection: Connection){
        connectionDao.insert(connection)
    }
}