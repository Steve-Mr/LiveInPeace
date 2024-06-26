package com.maary.liveinpeace.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import com.maary.liveinpeace.Constants
import com.maary.liveinpeace.Constants.Companion.BROADCAST_ACTION_FOREGROUND
import com.maary.liveinpeace.Constants.Companion.BROADCAST_FOREGROUND_INTENT_EXTRA
import com.maary.liveinpeace.Constants.Companion.CHANNEL_ID_ALERT
import com.maary.liveinpeace.Constants.Companion.CHANNEL_ID_DEFAULT
import com.maary.liveinpeace.Constants.Companion.CHANNEL_ID_PROTECT
import com.maary.liveinpeace.Constants.Companion.CHANNEL_ID_SETTINGS
import com.maary.liveinpeace.Constants.Companion.CHANNEL_ID_SLEEPTIMER
import com.maary.liveinpeace.Constants.Companion.CHANNEL_ID_WELCOME
import com.maary.liveinpeace.Constants.Companion.ID_NOTIFICATION_GROUP_SETTINGS
import com.maary.liveinpeace.Constants.Companion.ID_NOTIFICATION_SETTINGS
import com.maary.liveinpeace.Constants.Companion.ID_NOTIFICATION_WELCOME
import com.maary.liveinpeace.Constants.Companion.PREF_WELCOME_FINISHED
import com.maary.liveinpeace.Constants.Companion.REQUESTING_WAIT_MILLIS
import com.maary.liveinpeace.Constants.Companion.SHARED_PREF
import com.maary.liveinpeace.R

class QSTileService: TileService() {

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onClick() {
        super.onClick()
        val tile = qsTile
        var waitMillis = REQUESTING_WAIT_MILLIS

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notificationManager.getNotificationChannel(CHANNEL_ID_DEFAULT) == null){
            createNotificationChannel(
                NotificationManager.IMPORTANCE_MIN,
                CHANNEL_ID_DEFAULT,
                resources.getString(R.string.default_channel),
                resources.getString(R.string.default_channel_description)
            )
        }
        if (notificationManager.getNotificationChannel(CHANNEL_ID_SETTINGS) == null) {
            createNotificationChannel(
                NotificationManager.IMPORTANCE_MIN,
                CHANNEL_ID_SETTINGS,
                resources.getString(R.string.channel_settings),
                resources.getString(R.string.settings_channel_description)
            )
        }
        if (notificationManager.getNotificationChannel(CHANNEL_ID_ALERT) == null) {
            createNotificationChannel(
                NotificationManager.IMPORTANCE_HIGH,
                CHANNEL_ID_ALERT,
                resources.getString(R.string.channel_alert),
                resources.getString(R.string.alert_channel_description)
            )
        }
        if (notificationManager.getNotificationChannel(CHANNEL_ID_PROTECT) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID_PROTECT,
                resources.getString(R.string.channel_protection),
                NotificationManager.IMPORTANCE_LOW).apply {
                description = resources.getString(R.string.protection_channel_description)
                enableVibration(false)
                setSound(null, null)
            }
            // Register the channel with the system
            notificationManager.createNotificationChannel(channel)
        }
        if (notificationManager.getNotificationChannel(CHANNEL_ID_WELCOME) == null){
            createNotificationChannel(
                NotificationManager.IMPORTANCE_MIN,
                CHANNEL_ID_WELCOME,
                resources.getString(R.string.welcome_channel),
                resources.getString(R.string.welcome_channel_description)
            )
        }

        if (notificationManager.getNotificationChannel(CHANNEL_ID_SLEEPTIMER) == null){
            createNotificationChannel(
                NotificationManager.IMPORTANCE_MIN,
                CHANNEL_ID_SLEEPTIMER,
                resources.getString(R.string.sleeptimer_channel),
                resources.getString(R.string.sleeptimer_channel_description)
            )
        }

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager

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

        val sharedPref = getSharedPreferences(SHARED_PREF, Context.MODE_PRIVATE)
        while (!sharedPref.getBoolean(PREF_WELCOME_FINISHED, false)){
            if ( powerManager.isIgnoringBatteryOptimizations(packageName) &&
                ActivityCompat.checkSelfPermission(
                    applicationContext, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED) {
                sharedPref.edit {
                    putBoolean(PREF_WELCOME_FINISHED, true)
                }
                break
            } else {
                createWelcomeNotification()
                Thread.sleep(waitMillis.toLong())
                waitMillis *= 2
            }
        }


        val intent = Intent(this, ForegroundService::class.java)

        if (!ForegroundService.isForegroundServiceRunning()){
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

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onStartListening() {
        super.onStartListening()
        val intentFilter = IntentFilter()
        intentFilter.addAction(BROADCAST_ACTION_FOREGROUND)
        registerReceiver(foregroundServiceReceiver, intentFilter, RECEIVER_NOT_EXPORTED)
    }

    override fun onStopListening() {
        super.onStopListening()
        unregisterReceiver(foregroundServiceReceiver)
    }

    private val foregroundServiceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.v("MUTE_QS", "TRIGGERED")

            val isForegroundServiceRunning = intent.getBooleanExtra(
                BROADCAST_FOREGROUND_INTENT_EXTRA, false)
            // 在此处处理前台服务状态的变化
            val tile = qsTile

            if (!isForegroundServiceRunning){
                Log.v("MUTE_QS", "NOT RUNNING")
                tile.state = Tile.STATE_INACTIVE
                tile.icon = Icon.createWithResource(context, R.drawable.icon_qs_off)
                tile.label = getString(R.string.qstile_inactive)
                val foregroundIntent = Intent(context, ForegroundService::class.java)
                applicationContext.startForegroundService(foregroundIntent)
            }else{
                tile.state = Tile.STATE_ACTIVE
                tile.icon = Icon.createWithResource(context, R.drawable.icon_qs_one)
                tile.label = getString(R.string.qstile_active)
            }
            tile.updateTile()
        }
    }

    private fun createNotificationChannel(importance:Int, id: String ,name:String, descriptionText: String) {
        //val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(id, name, importance).apply {
            description = descriptionText
        }
        // Register the channel with the system
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun requestNotificationsPermission() {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(pendingIntent)
        }
    }

    @SuppressLint("BatteryLife")
    private fun createWelcomeNotification() {
        Log.v("MUTE_", "CREATING WELCOME")
        val welcome = NotificationCompat.Builder(this, CHANNEL_ID_WELCOME)
            .setOngoing(true)
            .setSmallIcon(R.drawable.ic_baseline_settings_24)
            .setShowWhen(false)
            .setContentTitle(getString(R.string.welcome))
            .setContentText(getString(R.string.welcome_description))
            .setOnlyAlertOnce(true)
            .setGroupSummary(false)
            .setGroup(ID_NOTIFICATION_GROUP_SETTINGS)

        val batteryIntent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
        batteryIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .data = Uri.parse("package:$packageName")

        val pendingBatteryIntent = PendingIntent.getActivity(this, 0, batteryIntent, PendingIntent.FLAG_IMMUTABLE)

        val batteryAction = NotificationCompat.Action.Builder(
            R.drawable.outline_battery_saver_24,
            getString(R.string.request_permission_battery),
            pendingBatteryIntent
        ).build()

        welcome.addAction(batteryAction)

        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.notify(ID_NOTIFICATION_WELCOME, welcome.build())

    }
}