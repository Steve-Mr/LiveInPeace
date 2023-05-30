package com.maary.liveinpeace.database

import androidx.annotation.WorkerThread
import kotlinx.coroutines.flow.Flow
import java.sql.Date
import java.time.LocalDate

class ConnectionRepository(private val connectionDao: ConnectionDao) {

//    val allConnectionsOnDate: Flow<List<Connection>> = connectionDao.loadAllConnectionsOnDate(queryDate)

    val allConnectionsToday = connectionDao.loadAllConnectionsOnDate(LocalDate.now().toString())

    val summaryToday:Flow<List<Connection>> = connectionDao.loadSummaryOnDate(LocalDate.now().toString())

    fun getAllConnectionsOnDate(queryDate: String): Flow<List<Connection>> {
        return connectionDao.loadAllConnectionsOnDate(queryDate)
    }

    @WorkerThread
    suspend fun insert(connection: Connection){
        connectionDao.insert(connection)
    }
}