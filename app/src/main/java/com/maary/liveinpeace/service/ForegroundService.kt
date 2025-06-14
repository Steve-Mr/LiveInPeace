package com.maary.liveinpeace.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.maary.liveinpeace.Constants.Companion.ACTION_NAME_SETTINGS
import com.maary.liveinpeace.Constants.Companion.ACTION_TOGGLE_AUTO_CONNECTION_ADJUSTMENT
import com.maary.liveinpeace.Constants.Companion.ALERT_TIME
import com.maary.liveinpeace.Constants.Companion.BROADCAST_ACTION_CONNECTIONS_UPDATE
import com.maary.liveinpeace.Constants.Companion.BROADCAST_ACTION_FOREGROUND
import com.maary.liveinpeace.Constants.Companion.BROADCAST_ACTION_MUTE
import com.maary.liveinpeace.Constants.Companion.BROADCAST_ACTION_SLEEPTIMER_TOGGLE
import com.maary.liveinpeace.Constants.Companion.BROADCAST_ACTION_SLEEPTIMER_UPDATE
import com.maary.liveinpeace.Constants.Companion.BROADCAST_FOREGROUND_INTENT_EXTRA
import com.maary.liveinpeace.Constants.Companion.CHANNEL_ID_DEFAULT
import com.maary.liveinpeace.Constants.Companion.CHANNEL_ID_PROTECT
import com.maary.liveinpeace.Constants.Companion.EXTRA_CONNECTIONS_LIST
import com.maary.liveinpeace.Constants.Companion.ID_NOTIFICATION_ALERT
import com.maary.liveinpeace.Constants.Companion.ID_NOTIFICATION_FOREGROUND
import com.maary.liveinpeace.Constants.Companion.ID_NOTIFICATION_GROUP_FORE
import com.maary.liveinpeace.Constants.Companion.ID_NOTIFICATION_GROUP_PROTECT
import com.maary.liveinpeace.Constants.Companion.ID_NOTIFICATION_PROTECT
import com.maary.liveinpeace.Constants.Companion.MODE_IMG
import com.maary.liveinpeace.Constants.Companion.MODE_NUM
import com.maary.liveinpeace.Constants.Companion.PREF_ENABLE_EAR_PROTECTION
import com.maary.liveinpeace.Constants.Companion.PREF_SERVICE_RUNNING
import com.maary.liveinpeace.Constants.Companion.PREF_WATCHING_CONNECTING_TIME
import com.maary.liveinpeace.DeviceTimer
import com.maary.liveinpeace.MainActivity
import com.maary.liveinpeace.R
import com.maary.liveinpeace.SleepNotification.find
import com.maary.liveinpeace.database.Connection
import com.maary.liveinpeace.database.ConnectionDao
import com.maary.liveinpeace.database.ConnectionRoomDatabase
import com.maary.liveinpeace.database.PreferenceRepository
import com.maary.liveinpeace.receiver.MuteMediaReceiver
import com.maary.liveinpeace.receiver.SettingsReceiver
import com.maary.liveinpeace.receiver.SleepReceiver
import com.maary.liveinpeace.receiver.VolumeReceiver
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import java.text.DateFormat
import java.time.LocalDate
import java.util.Date
import java.util.concurrent.ConcurrentHashMap

@AndroidEntryPoint
class ForegroundService : Service() {

    // Use instance scope for CoroutineScope to easily cancel jobs in onDestroy
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    private lateinit var database: ConnectionRoomDatabase
    private lateinit var connectionDao: ConnectionDao
    private lateinit var audioManager: AudioManager

    @Inject
    lateinit var preferenceRepository: PreferenceRepository

    // Instance variable for device map
    private val deviceMap: MutableMap<String, Connection> = ConcurrentHashMap() // Use ConcurrentHashMap if worried about potential multi-threaded access, otherwise regular HashMap is fine.
    private val deviceTimerMap: MutableMap<String, DeviceTimer> = ConcurrentHashMap()

    private val volumeDrawableIds = intArrayOf(
        R.drawable.ic_volume_silent,
        R.drawable.ic_volume_low,
        R.drawable.ic_volume_middle,
        R.drawable.ic_volume_high,
        R.drawable.ic_volume_mega
    )

    private lateinit var volumeComment: Array<String>

    // Method to broadcast the current connection list
    private fun broadcastConnectionsUpdate() {
        val intent = Intent(BROADCAST_ACTION_CONNECTIONS_UPDATE)
        // Convert map values to ArrayList which is Serializable/Parcelable
        val connectionList = ArrayList(deviceMap.values)
        intent.putParcelableArrayListExtra(EXTRA_CONNECTIONS_LIST, connectionList)
        sendBroadcast(intent)
    }


    private fun getVolumePercentage(): Int {
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        // Avoid division by zero if maxVolume is 0
        return if (maxVolume > 0) 100 * currentVolume / maxVolume else 0
    }

    private fun getVolumeLevel(percent: Int): Int {
        return when(percent) {
            in 0..0 -> 0
            in 1..25 -> 1
            in 26..50 -> 2
            in 50..80 -> 3
            else -> 4
        }
    }

    private val volumeChangeReceiver = object : VolumeReceiver() {
        @SuppressLint("MissingPermission")
        override fun updateNotification(context: Context) {
            Log.v("MUTE_TEST", "VOLUME_CHANGE_RECEIVER")
            with(NotificationManagerCompat.from(applicationContext)){
                notify(ID_NOTIFICATION_FOREGROUND, createForegroundNotification(applicationContext))
            }
        }
    }

    private val sleepReceiver = object : SleepReceiver() {
        @SuppressLint("MissingPermission")
        override fun updateNotification(context: Context) {
            with(NotificationManagerCompat.from(applicationContext)){
                notify(ID_NOTIFICATION_FOREGROUND, createForegroundNotification(applicationContext))
            }
        }
    }

    // Saves data for currently connected devices when service stops
    private fun saveDataForActiveConnections() {
        val disconnectedTime = System.currentTimeMillis()
        val currentConnections = deviceMap.toMap() // Create a copy to avoid ConcurrentModificationException if needed
        deviceMap.clear() // Clear the instance map

        currentConnections.forEach { (_, connection) ->
            val connectedTime = connection.connectedTime
            if (connectedTime != null) {
                val connectionTime = disconnectedTime - connectedTime
                serviceScope.launch {
                    try {
                        connectionDao.insert(
                            Connection(
                                name = connection.name,
                                type = connection.type,
                                connectedTime = connection.connectedTime,
                                disconnectedTime = disconnectedTime,
                                duration = connectionTime,
                                date = connection.date
                            )
                        )
                        Log.d("ForegroundService", "Saved connection data for ${connection.name}")
                    } catch (e: Exception) {
                        Log.e("ForegroundService", "Error saving connection data for ${connection.name}", e)
                    }
                }
            }
        }
        // Notify that the connection list is now empty
        broadcastConnectionsUpdate()
    }

    private val audioDeviceCallback = object : AudioDeviceCallback() {
        @SuppressLint("MissingPermission")
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {

            val connectedTime = System.currentTimeMillis()

            var deviceAdded = false
            addedDevices?.forEach { deviceInfo ->
                if (deviceInfo.type in listOf(
                        AudioDeviceInfo.TYPE_BUILTIN_EARPIECE,
                        AudioDeviceInfo.TYPE_BUILTIN_MIC,
                        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
                        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER_SAFE,
                        AudioDeviceInfo.TYPE_FM_TUNER,
                        AudioDeviceInfo.TYPE_REMOTE_SUBMIX,
                        AudioDeviceInfo.TYPE_TELEPHONY,
                        28, // Consider defining this constant if it's meaningful
                    )
                ) { return@forEach }

                val deviceName = deviceInfo.productName?.toString()?.trim() ?: "Unknown Device ${deviceInfo.id}" // Handle null productName
                if (deviceName == Build.MODEL || deviceName.isBlank()) return@forEach // Skip self or blank names

                Log.v("MUTE_DEVICE", "Device Added: $deviceName (Type: ${deviceInfo.type})")

                // Add to instance map
                if (!deviceMap.containsKey(deviceName)) {
                    deviceMap[deviceName] = Connection(
                        name = deviceName,
                        type = deviceInfo.type,
                        connectedTime = connectedTime,
                        disconnectedTime = null,
                        duration = null,
                        date = LocalDate.now().toString()
                    )
                    deviceAdded = true

                    serviceScope.launch {
                        //todo race condition

                        // Ear Protection Logic
                        if (preferenceRepository.isEarProtectionOn().first()) {
                            var boolProtected = false
                            // Use loop with counter or check to prevent infinite loop if volume doesn't change
                            var attempts = 100 // Limit attempts
                            while (getVolumePercentage() > 25 && attempts-- > 0) {
                                boolProtected = true
                                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, 0)
                            }
                            attempts = 100 // Reset attempts
                            while (getVolumePercentage() < 10 && attempts-- > 0) {
                                boolProtected = true
                                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, 0)
                            }
                            if (boolProtected) {
                                Log.d("ForegroundService", "Ear protection applied for $deviceName")
                                NotificationManagerCompat.from(applicationContext).apply {
                                    notify(ID_NOTIFICATION_PROTECT, createProtectionNotification())
                                }
                            }
                        }

                        // Start timer only if watching is enabled
                        if (preferenceRepository.getWatchingState().first()) {
                            Log.v("MUTE_DEVICEMAP", "Starting timer for $deviceName")
                            val deviceTimer = DeviceTimer(context = applicationContext, deviceName = deviceName)
                            deviceTimer.start()
                            deviceTimerMap[deviceName] = deviceTimer
                        }
                    }
                }
            }

            if (deviceAdded) {
                Log.v("MUTE_MAP", "Device map updated: ${deviceMap.keys}")
                // Broadcast the updated list
                broadcastConnectionsUpdate()
                // Update the foreground notification
                NotificationManagerCompat.from(applicationContext).apply {
                    notify(ID_NOTIFICATION_FOREGROUND, createForegroundNotification(applicationContext))
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            var deviceRemoved = false

            removedDevices?.forEach { deviceInfo ->
                val deviceName = deviceInfo.productName?.toString()?.trim() ?: return@forEach // Skip if no name
                val disconnectedTime = System.currentTimeMillis()

                if (deviceMap.containsKey(deviceName)) {
                    deviceRemoved = true
                    val connection = deviceMap.remove(deviceName) // Remove from instance map

                    Log.v("MUTE_DEVICE", "Device Removed: $deviceName (Type: ${deviceInfo.type})")

                    if (connection?.connectedTime != null) {
                        val connectionTime = disconnectedTime - connection.connectedTime

                        // Cancel alert notification if connection was long
                        if (connectionTime > ALERT_TIME) {
                            val notificationManager: NotificationManager =
                                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                            notificationManager.cancel(ID_NOTIFICATION_ALERT)
                        }

                        // Save data using instance scope
                        serviceScope.launch {
                            try {
                                connectionDao.insert(
                                    Connection(
                                        name = connection.name,
                                        type = connection.type,
                                        connectedTime = connection.connectedTime,
                                        disconnectedTime = disconnectedTime,
                                        duration = connectionTime,
                                        date = connection.date
                                    )
                                )
                                Log.d("ForegroundService", "Saved connection data for removed device ${connection.name}")
                            } catch (e: Exception) {
                                Log.e("ForegroundService", "Error saving connection data for removed device ${connection.name}", e)
                            }
                        }
                    }

                    // Stop and remove timer if watching is enabled
                    serviceScope.launch {
                        if (preferenceRepository.getWatchingState().first()) {
                            deviceTimerMap.remove(deviceName)?.let {
                                Log.v("MUTE_DEVICEMAP", "Stopping timer for $deviceName")
                                it.stop()
                            }
                        }
                    }
                }

                if (deviceRemoved) {
                    Log.v("MUTE_MAP", "Device map updated: ${deviceMap.keys}")
                    // Broadcast the updated list
                    broadcastConnectionsUpdate()
                    // Update the foreground notification
                    NotificationManagerCompat.from(applicationContext).apply {
                        notify(ID_NOTIFICATION_FOREGROUND, createForegroundNotification(applicationContext))
                    }
                    deviceRemoved = false
                }
            }


        }
    }


    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate() {
        super.onCreate()


        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.registerAudioDeviceCallback(audioDeviceCallback, null) // Consider using a Handler for the callback thread if needed

        // Initialize Database and DAO
        database = ConnectionRoomDatabase.getDatabase(applicationContext)
        connectionDao = database.connectionDao()

        // Load comments
        volumeComment = resources.getStringArray(R.array.array_volume_comment)

        // Register Receivers
        val volumeFilter = IntentFilter().apply {
            addAction("android.media.VOLUME_CHANGED_ACTION")
        } // Use constant
        // Check for permission before registering if targeting Android 14+ for non-exported receivers needing permissions
        registerReceiver(volumeChangeReceiver, volumeFilter)

        val sleepFilter = IntentFilter(BROADCAST_ACTION_SLEEPTIMER_UPDATE)
        // Use RECEIVER_NOT_EXPORTED for internal broadcasts
        registerReceiver(sleepReceiver, sleepFilter, RECEIVER_NOT_EXPORTED)

        // Start foreground
        startForeground(ID_NOTIFICATION_FOREGROUND, createForegroundNotification(applicationContext))

        // Update persisted state and notify components
        setServiceRunningState(true)

        Log.d("ForegroundService", "onCreate Finished")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("ForegroundService", "onStartCommand received")

        // Ensure the foreground notification is up-to-date if started again
        NotificationManagerCompat.from(applicationContext).apply {
            if (ActivityCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                notify(ID_NOTIFICATION_FOREGROUND, createForegroundNotification(applicationContext))
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        Log.d("ForegroundService", "onDestroy - Cleaning up...")

        // Set persisted state to false *before* cleanup, in case cleanup fails
        setServiceRunningState(false)

        // Save data for any remaining active connections
        saveDataForActiveConnections()

        // Cancel all coroutines started by this service instance
        serviceScope.cancel()

        // Stop and clear all timers
        try {
            deviceTimerMap.values.forEach { it.stop() }
            deviceTimerMap.clear()
            Log.d("ForegroundService", "Device timers stopped and cleared.")
        } catch (e: Exception){
            Log.e("ForegroundService", "Error stopping timers", e)
        }


        // Unregister receivers and callbacks
        try {
            unregisterReceiver(volumeChangeReceiver)
            Log.d("ForegroundService", "Volume receiver unregistered.")
        } catch (e: IllegalArgumentException) {
            Log.w("ForegroundService", "Volume receiver already unregistered?", e)
        }
        try {
            unregisterReceiver(sleepReceiver)
            Log.d("ForegroundService", "Sleep receiver unregistered.")
        } catch (e: IllegalArgumentException) {
            Log.w("ForegroundService", "Sleep receiver already unregistered?", e)
        }
        try {
            audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)
            Log.d("ForegroundService", "Audio device callback unregistered.")
        } catch (e: Exception) {
            Log.e("ForegroundService", "Error unregistering audio callback", e)
        }

        // Stop foreground service removal notification
        stopForeground(STOP_FOREGROUND_REMOVE)

         val notificationManager: NotificationManager =
             getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
         notificationManager.cancel(ID_NOTIFICATION_FOREGROUND)
        Log.d("ForegroundService", "onDestroy Finished")
        super.onDestroy()
    }


    // Required method for Service, return null for non-bound service
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    // Helper to update persisted state and send broadcast
    private fun setServiceRunningState(isRunning: Boolean) {
        // Update Preferences
        serviceScope.launch {
            preferenceRepository.setServiceRunning(isRunning)
        }
        // Send broadcast to notify components like QSTileService
        val intent = Intent(BROADCAST_ACTION_FOREGROUND)
        intent.putExtra(BROADCAST_FOREGROUND_INTENT_EXTRA, isRunning)
        sendBroadcast(intent)
        Log.d("ForegroundService", "Service running state set to $isRunning and broadcast sent.")
    }


    @SuppressLint("LaunchActivityFromNotification") // If PendingIntent launches Activity
    fun createForegroundNotification(context: Context): Notification {
        val currentVolume = getVolumePercentage()
        val currentVolumeLevel = getVolumeLevel(currentVolume)
        // Ensure volumeComment is initialized
        val comment = if (::volumeComment.isInitialized && currentVolumeLevel < volumeComment.size) {
            volumeComment[currentVolumeLevel]
        } else {
            "Volume" // Fallback
        }

        val nIcon = generateNotificationIcon(context)

        // --- Intents for Actions ---
        val settingsIntent = Intent(this, MainActivity::class.java).apply {
            action = ACTION_NAME_SETTINGS
        }
        val settingsPendingIntent: PendingIntent =
            PendingIntent.getBroadcast(this, 0, settingsIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT) // Use UPDATE_CURRENT if intent extras change

        val sleepIntent = Intent(context, MuteMediaReceiver::class.java).apply {
            action = BROADCAST_ACTION_SLEEPTIMER_TOGGLE
        }
        val pendingSleepIntent = PendingIntent.getBroadcast(context, 2, sleepIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT) // Different request code (2)

        val muteMediaIntent = Intent(context, MuteMediaReceiver::class.java).apply {
            action = BROADCAST_ACTION_MUTE
        }
        val pendingMuteIntent = PendingIntent.getBroadcast(context, 3, muteMediaIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT) // Different request code (3)

        val sleepNotification = find() // From SleepNotification object
        val sleepTitle = if (sleepNotification != null) {
            DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(sleepNotification.`when`))
        } else {
            resources.getString(R.string.sleep)
        }

        // --- Build Actions ---
        val actionSettings : NotificationCompat.Action = NotificationCompat.Action.Builder(
            R.drawable.ic_baseline_settings_24,
            resources.getString(R.string.settings),
            settingsPendingIntent
        ).build()

        val actionSleepTimer: NotificationCompat.Action = NotificationCompat.Action.Builder (
            R.drawable.ic_tile, // Consider a sleep-specific icon
            sleepTitle,
            pendingSleepIntent
        ).build()


        // Build Notification
        return NotificationCompat.Builder(this, CHANNEL_ID_DEFAULT)
            .setContentTitle(getString(R.string.to_be_or_not)) // Consider more descriptive title
            .setOnlyAlertOnce(true)
            .setContentText(String.format(
                resources.getString(R.string.current_volume_percent),
                comment,
                currentVolume))
            .setSmallIcon(nIcon)
            .setOngoing(true)
            .setContentIntent(pendingMuteIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(actionSettings)
            .addAction(actionSleepTimer)
            .setGroup(ID_NOTIFICATION_GROUP_FORE)
            .setGroupSummary(false)
            .build()
    }

    fun createProtectionNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID_PROTECT)
            .setContentTitle(getString(R.string.ears_protected))
            .setSmallIcon(R.drawable.ic_headphones_protection)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setGroup(ID_NOTIFICATION_GROUP_PROTECT)
            .setTimeoutAfter(3000)
            .setGroupSummary(false)
            .build()
    }

    @SuppressLint("DiscouragedApi")
    private fun generateNotificationIcon(context: Context): IconCompat {
        val currentVolume = getVolumePercentage()

        val resourceName = "num_${currentVolume}"
        val resourceId = resources.getIdentifier(resourceName, "drawable", context.packageName)
        return if (resourceId != 0) IconCompat.createWithResource(this, resourceId)
        else IconCompat.createWithResource(context, volumeDrawableIds[getVolumeLevel(currentVolume)]) // Fallback to image mode

    }
}