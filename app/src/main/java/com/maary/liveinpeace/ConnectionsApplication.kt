package com.maary.liveinpeace

import android.app.Application
import com.google.android.material.color.DynamicColors
import com.maary.liveinpeace.database.ConnectionRepository
import com.maary.liveinpeace.database.ConnectionRoomDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class ConnectionsApplication: Application() {

    val database by lazy { ConnectionRoomDatabase.getDatabase(this) }
    val repository by lazy { ConnectionRepository(database.connectionDao()) }

    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}