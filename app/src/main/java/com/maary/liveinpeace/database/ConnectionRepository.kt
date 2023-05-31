package com.maary.liveinpeace.database

import androidx.annotation.WorkerThread
import kotlinx.coroutines.flow.Flow

class ConnectionRepository(private val connectionDao: ConnectionDao) {

    fun getAllConnectionsOnDate(queryDate: String): Flow<List<Connection>> {
        return connectionDao.loadAllConnectionsOnDate(queryDate)
    }

    fun getSummaryOnDate(queryDate: String): Flow<List<Connection>> {
        return connectionDao.loadSummaryOnDate(queryDate)
    }

    @WorkerThread
    suspend fun insert(connection: Connection){
        connectionDao.insert(connection)
    }
}