package com.maary.liveinpeace

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.maary.liveinpeace.Constants.Companion.REQUESTING_WAIT_MILLIS

class QSTileService: TileService() {

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onClick() {
        super.onClick()
        val tile = qsTile
        var waitMillis = REQUESTING_WAIT_MILLIS

        while(ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.v("MUTE_", waitMillis.toString())
            requestNotificationsPermission()
            Thread.sleep(waitMillis.toLong())
            waitMillis *= 2
        }


        val intent = Intent(this, ForegroundService::class.java)

        if (!ForegroundService.isForegroundServiceRunning()){
            createNotificationChannel(
                NotificationManager.IMPORTANCE_MIN,
                resources.getString(R.string.default_channel),
                resources.getString(R.string.default_channel_description)
            )
            createNotificationChannel(
                NotificationManager.IMPORTANCE_MIN,
                resources.getString(R.string.channel_settings),
                resources.getString(R.string.settings_channel_description)
            )

            applicationContext.startForegroundService(intent)
            tile.state = Tile.STATE_ACTIVE
            tile.icon = Icon.createWithResource(this, R.drawable.icon_qs_one)
            tile.label = getString(R.string.qstile_active)

        }else{
            applicationContext.stopService(intent)
            tile.state = Tile.STATE_INACTIVE
            tile.icon = Icon.createWithResource(this, R.drawable.icon_qs_off)
            tile.label = getString(R.string.qstile_inactive)
        }
        tile.updateTile()
    }

    override fun onStartListening() {
        super.onStartListening()
        val tile = qsTile

        if (!ForegroundService.isForegroundServiceRunning()){
            tile.state = Tile.STATE_INACTIVE
            tile.icon = Icon.createWithResource(this, R.drawable.icon_qs_off)
            tile.label = getString(R.string.qstile_inactive)

        }else{
            tile.state = Tile.STATE_ACTIVE
            tile.icon = Icon.createWithResource(this, R.drawable.icon_qs_one)
            tile.label = getString(R.string.qstile_active)
        }
        tile.updateTile()
    }

    private fun createNotificationChannel(importance:Int ,name:String, descriptionText: String) {
        //val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(name, name, importance).apply {
            description = descriptionText
        }
        // Register the channel with the system
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun requestNotificationsPermission() = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
    }.let(::startActivityAndCollapse)
}