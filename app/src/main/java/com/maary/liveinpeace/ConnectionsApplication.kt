package com.maary.liveinpeace

import android.app.Application
import com.maary.liveinpeace.database.ConnectionRepository
import com.maary.liveinpeace.database.ConnectionRoomDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class ConnectionsApplication: Application() {

    val applicationScope = CoroutineScope(SupervisorJob())

    val database by lazy { ConnectionRoomDatabase.getDatabase(this) }
    val repository by lazy { ConnectionRepository(database.connectionDao()) }
}