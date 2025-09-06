package com.maary.liveinpeace

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.google.android.material.color.DynamicColors
import com.maary.liveinpeace.Constants.Companion.CHANNEL_ID_ALERT
import com.maary.liveinpeace.Constants.Companion.CHANNEL_ID_DEFAULT
import com.maary.liveinpeace.Constants.Companion.CHANNEL_ID_PROTECT
import com.maary.liveinpeace.Constants.Companion.CHANNEL_ID_SETTINGS
import com.maary.liveinpeace.Constants.Companion.CHANNEL_ID_SLEEPTIMER
import com.maary.liveinpeace.Constants.Companion.CHANNEL_ID_WELCOME
import com.maary.liveinpeace.database.ConnectionRepository
import com.maary.liveinpeace.database.ConnectionRoomDatabase
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class LiveInPeaceApplication: Application() {

    val database by lazy { ConnectionRoomDatabase.getDatabase(this) }
    val repository by lazy { ConnectionRepository(database.connectionDao()) }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        DynamicColors.applyToActivitiesIfAvailable(this)
    }

    private fun createNotificationChannels() {

        createNotificationChannel(
            NotificationManager.IMPORTANCE_MIN,
            CHANNEL_ID_DEFAULT,
            resources.getString(R.string.default_channel),
            resources.getString(R.string.default_channel_description)
        )

        createNotificationChannel(
            NotificationManager.IMPORTANCE_MIN,
            CHANNEL_ID_SETTINGS,
            resources.getString(R.string.channel_settings),
            resources.getString(R.string.settings_channel_description)
        )

        createNotificationChannel(
            NotificationManager.IMPORTANCE_HIGH,
            CHANNEL_ID_ALERT,
            resources.getString(R.string.channel_alert),
            resources.getString(R.string.alert_channel_description)
        )

        createNotificationChannel(
            NotificationManager.IMPORTANCE_LOW,
            CHANNEL_ID_PROTECT,
            resources.getString(R.string.channel_protection),
            resources.getString(R.string.protection_channel_description)
        )

        createNotificationChannel(
            NotificationManager.IMPORTANCE_MIN,
            CHANNEL_ID_WELCOME,
            resources.getString(R.string.welcome_channel),
            resources.getString(R.string.welcome_channel_description)
        )

        createNotificationChannel(
            NotificationManager.IMPORTANCE_MIN,
            CHANNEL_ID_SLEEPTIMER,
            resources.getString(R.string.sleeptimer_channel),
            resources.getString(R.string.sleeptimer_channel_description)
        )
    }

    private fun createNotificationChannel(importance:Int, id: String ,name:String, descriptionText: String) {
        //val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(id, name, importance).apply {
            description = descriptionText
        }
        // Register the channel with the system
        val notificationManager: NotificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}