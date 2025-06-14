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
import com.maary.liveinpeace.Constants
import com.maary.liveinpeace.Constants.Companion.CHANNEL_ID_WELCOME
import com.maary.liveinpeace.Constants.Companion.ID_NOTIFICATION_GROUP_SETTINGS
import com.maary.liveinpeace.Constants.Companion.ID_NOTIFICATION_WELCOME
import com.maary.liveinpeace.Constants.Companion.REQUESTING_WAIT_MILLIS
import com.maary.liveinpeace.R
import com.maary.liveinpeace.database.PreferenceRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@EntryPoint
@InstallIn(SingletonComponent::class)
interface PreferenceQSTileEntryPoint {
    fun preferenceRepository(): PreferenceRepository
}

class QSTileService: TileService() {

    private val serviceScope = CoroutineScope( SupervisorJob() + Dispatchers.IO)
    private lateinit var preferenceRepository: PreferenceRepository

    override fun onCreate() {
        super.onCreate()
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            PreferenceQSTileEntryPoint::class.java
        )
        preferenceRepository = entryPoint.preferenceRepository()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onClick() {
        super.onClick()
        val tile = qsTile
        var waitMillis = REQUESTING_WAIT_MILLIS

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

        val intent = Intent(this, ForegroundService::class.java)


        serviceScope.launch {
            preferenceRepository.isWelcomeFinished().collect {
                if (!it) {
                    //todo: redirect to welcome activity/screen
                    if ( powerManager.isIgnoringBatteryOptimizations(packageName) &&
                        ActivityCompat.checkSelfPermission(
                            applicationContext, Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED) {
                        preferenceRepository.setWelcomeFinished(true)
                    } else {
                        createWelcomeNotification()
                        Thread.sleep(waitMillis.toLong())
                        waitMillis *= 2
                    }
                }
            }

            if (preferenceRepository.isServiceRunning().first()) {
                stopService(intent)
                preferenceRepository.setServiceRunning(false)
                updateTileState(false)
                tile.updateTile()

            } else {
                startForegroundService(intent)
                preferenceRepository.setServiceRunning(true)
                updateTileState(true)
                tile.updateTile()

            }
        }

        tile.updateTile()
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onStartListening() {
        super.onStartListening()
        // Update tile based on persisted state initially
        serviceScope.launch {
            updateTileState(preferenceRepository.isServiceRunning().first())
        }

        val intentFilter = IntentFilter()
        intentFilter.addAction(Constants.BROADCAST_ACTION_FOREGROUND)
        // Use RECEIVER_NOT_EXPORTED for security with internal broadcasts
        registerReceiver(foregroundServiceReceiver, intentFilter, RECEIVER_NOT_EXPORTED)
    }

    override fun onStopListening() {
        super.onStopListening()
        unregisterReceiver(foregroundServiceReceiver)
    }

    private val foregroundServiceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Constants.BROADCAST_ACTION_FOREGROUND) {
                val isRunning = intent.getBooleanExtra(Constants.BROADCAST_FOREGROUND_INTENT_EXTRA, false)
                Log.d("QSTileService", "Received foreground service state update: isRunning=$isRunning")
                updateTileState(isRunning)
            }
        }
    }

    private fun updateTileState(isRunning: Boolean) {
        val tile = qsTile ?: return // Tile might be null if called before ready

        if (isRunning) {
            tile.state = Tile.STATE_ACTIVE
            tile.icon = Icon.createWithResource(this, R.drawable.icon_qs_one) // Active icon
            tile.label = getString(R.string.qstile_active)
        } else {
            tile.state = Tile.STATE_INACTIVE
            tile.icon = Icon.createWithResource(this, R.drawable.icon_qs_off) // Inactive icon
            tile.label = getString(R.string.qstile_inactive)
        }
        tile.updateTile()
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