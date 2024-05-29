package com.maary.liveinpeace.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.maary.liveinpeace.Constants.Companion.ACTION_NAME_SETTINGS
import com.maary.liveinpeace.Constants.Companion.ACTION_TOGGLE_AUTO_CONNECTION_ADJUSTMENT
import com.maary.liveinpeace.Constants.Companion.ALERT_TIME
import com.maary.liveinpeace.Constants.Companion.BROADCAST_ACTION_FOREGROUND
import com.maary.liveinpeace.Constants.Companion.BROADCAST_ACTION_MUTE
import com.maary.liveinpeace.Constants.Companion.BROADCAST_ACTION_SLEEPTIMER_TOGGLE
import com.maary.liveinpeace.Constants.Companion.BROADCAST_ACTION_SLEEPTIMER_UPDATE
import com.maary.liveinpeace.Constants.Companion.BROADCAST_FOREGROUND_INTENT_EXTRA
import com.maary.liveinpeace.Constants.Companion.CHANNEL_ID_DEFAULT
import com.maary.liveinpeace.Constants.Companion.CHANNEL_ID_PROTECT
import com.maary.liveinpeace.Constants.Companion.ID_NOTIFICATION_ALERT
import com.maary.liveinpeace.Constants.Companion.ID_NOTIFICATION_FOREGROUND
import com.maary.liveinpeace.Constants.Companion.ID_NOTIFICATION_GROUP_FORE
import com.maary.liveinpeace.Constants.Companion.ID_NOTIFICATION_GROUP_PROTECT
import com.maary.liveinpeace.Constants.Companion.ID_NOTIFICATION_PROTECT
import com.maary.liveinpeace.Constants.Companion.MODE_IMG
import com.maary.liveinpeace.Constants.Companion.MODE_NUM
import com.maary.liveinpeace.Constants.Companion.PREF_ENABLE_EAR_PROTECTION
import com.maary.liveinpeace.Constants.Companion.PREF_ICON
import com.maary.liveinpeace.Constants.Companion.PREF_WATCHING_CONNECTING_TIME
import com.maary.liveinpeace.Constants.Companion.SHARED_PREF
import com.maary.liveinpeace.DeviceMapChangeListener
import com.maary.liveinpeace.DeviceTimer
import com.maary.liveinpeace.R
import com.maary.liveinpeace.SleepNotification.find
import com.maary.liveinpeace.database.Connection
import com.maary.liveinpeace.database.ConnectionDao
import com.maary.liveinpeace.database.ConnectionRoomDatabase
import com.maary.liveinpeace.receiver.MuteMediaReceiver
import com.maary.liveinpeace.receiver.SettingsReceiver
import com.maary.liveinpeace.receiver.SleepReceiver
import com.maary.liveinpeace.receiver.VolumeReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.time.LocalDate
import java.util.Date

class ForegroundService: Service() {

    private lateinit var database: ConnectionRoomDatabase
    private lateinit var connectionDao: ConnectionDao

    private val deviceTimerMap: MutableMap<String, DeviceTimer> = mutableMapOf()

    private val volumeDrawableIds = intArrayOf(
        R.drawable.ic_volume_silent,
        R.drawable.ic_volume_low,
        R.drawable.ic_volume_middle,
        R.drawable.ic_volume_high,
        R.drawable.ic_volume_mega
    )

    private lateinit var volumeComment: Array<String>

    private lateinit var audioManager: AudioManager

    companion object {
        private var isForegroundServiceRunning = false

        @JvmStatic
        fun isForegroundServiceRunning(): Boolean {
            return isForegroundServiceRunning
        }

        private val deviceMap: MutableMap<String, Connection> = mutableMapOf()

        // 在伴生对象中定义一个静态方法，用于其他类访问deviceMap
        fun getConnections(): MutableList<Connection> {
            return deviceMap.values.toMutableList()
        }

        private val deviceMapChangeListeners: MutableList<DeviceMapChangeListener> = mutableListOf()

        fun addDeviceMapChangeListener(listener: DeviceMapChangeListener) {
            deviceMapChangeListeners.add(listener)
        }

        fun removeDeviceMapChangeListener(listener: DeviceMapChangeListener) {
            deviceMapChangeListeners.remove(listener)
        }
    }

    private fun notifyDeviceMapChange() {
        deviceMapChangeListeners.forEach { listener ->
            listener.onDeviceMapChanged(deviceMap)
        }
    }

    private fun getVolumePercentage(context: Context): Int {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        return 100 * currentVolume / maxVolume
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
            }        }

    }

    private fun saveDataWhenStop(){
        val disconnectedTime = System.currentTimeMillis()

        for ( (deviceName, connection) in deviceMap){

            val connectedTime = connection.connectedTime
            val connectionTime = disconnectedTime - connectedTime!!

            CoroutineScope(Dispatchers.IO).launch {
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
            }
            deviceMap.remove(deviceName)
        }
        return
    }

    private val audioDeviceCallback = object : AudioDeviceCallback() {
        @SuppressLint("MissingPermission")
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {

            val connectedTime = System.currentTimeMillis()
            val sharedPreferences = getSharedPreferences(SHARED_PREF, Context.MODE_PRIVATE)

            // 在设备连接时记录设备信息和接入时间
            addedDevices?.forEach { deviceInfo ->
                if (deviceInfo.type in listOf(
                        AudioDeviceInfo.TYPE_BUILTIN_EARPIECE,
                        AudioDeviceInfo.TYPE_BUILTIN_MIC,
                        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
                        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER_SAFE,
                        AudioDeviceInfo.TYPE_FM_TUNER,
                        AudioDeviceInfo.TYPE_REMOTE_SUBMIX,
                        AudioDeviceInfo.TYPE_TELEPHONY,
                        28,
                    )
                ) { return@forEach }
                val deviceName = deviceInfo.productName.toString().trim()
                if (deviceName == Build.MODEL) return@forEach
                Log.v("MUTE_DEVICE", deviceName)
                Log.v("MUTE_TYPE", deviceInfo.type.toString())
                deviceMap[deviceName] = Connection(
                    id=1,
                    name = deviceInfo.productName.toString(),
                    type = deviceInfo.type,
                    connectedTime = connectedTime,
                    disconnectedTime = null,
                    duration = null,
                    date = LocalDate.now().toString()
                )
                notifyDeviceMapChange()
                if (sharedPreferences.getBoolean(PREF_ENABLE_EAR_PROTECTION, false)){
                    val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    var boolProtected = false
                    while (getVolumePercentage(applicationContext)>25) {
                        boolProtected = true
                        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, 0)
                    }
                    while (getVolumePercentage(applicationContext)<10) {
                        boolProtected = true
                        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, 0)
                    }
                    if (boolProtected){
                        with(NotificationManagerCompat.from(applicationContext)){
                            notify(ID_NOTIFICATION_PROTECT, createProtectionNotification())
                        }
                    }
                }
                // 执行其他逻辑，比如将设备信息保存到数据库或日志中
            }

            if (sharedPreferences.getBoolean(PREF_WATCHING_CONNECTING_TIME, false)){
                for ((productName, _) in deviceMap){
                    if (deviceTimerMap.containsKey(productName)) continue
                    val deviceTimer = DeviceTimer(context = applicationContext, deviceName = productName)
                    Log.v("MUTE_DEVICEMAP", productName)
                    deviceTimer.start()
                    deviceTimerMap[productName] = deviceTimer
                }
            }

            Log.v("MUTE_MAP", deviceMap.toString())

            // Handle newly added audio devices
            with(NotificationManagerCompat.from(applicationContext)){
                notify(ID_NOTIFICATION_FOREGROUND, createForegroundNotification(applicationContext))
            }
        }

        @SuppressLint("MissingPermission")
        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {

            // 在设备连接时记录设备信息和接入时间
            removedDevices?.forEach { deviceInfo ->
                val deviceName = deviceInfo.productName.toString()
                val disconnectedTime = System.currentTimeMillis()

                if (deviceMap.containsKey(deviceName)){

                    val connectedTime = deviceMap[deviceName]?.connectedTime
                    val connectionTime = disconnectedTime - connectedTime!!

                    if (connectionTime > ALERT_TIME){
                        val notificationManager: NotificationManager =
                            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.cancel(ID_NOTIFICATION_ALERT)
                    }

                    val baseConnection = deviceMap[deviceName]
                    CoroutineScope(Dispatchers.IO).launch {
                        if (baseConnection != null) {
                            connectionDao.insert(
                                Connection(
                                    name = baseConnection.name,
                                    type = baseConnection.type,
                                    connectedTime = baseConnection.connectedTime,
                                    disconnectedTime = disconnectedTime,
                                    duration = connectionTime,
                                    date = baseConnection.date
                                    )
                            )
                        }
                    }

                    deviceMap.remove(deviceName)
                    notifyDeviceMapChange()
                }

                val sharedPreferences = getSharedPreferences(SHARED_PREF, Context.MODE_PRIVATE)
                if (sharedPreferences.getBoolean(PREF_WATCHING_CONNECTING_TIME, false)){
                    if (deviceTimerMap.containsKey(deviceName)){
                        deviceTimerMap[deviceName]?.stop()
                        deviceTimerMap.remove(deviceName)
                    }
                }
                // 执行其他逻辑，比如将设备信息保存到数据库或日志中
            }

            // Handle removed audio devices
            with(NotificationManagerCompat.from(applicationContext)){
                notify(ID_NOTIFICATION_FOREGROUND, createForegroundNotification(applicationContext))
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate() {
        super.onCreate()

        Log.v("MUTE_TEST", "ON_CREATE")

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.registerAudioDeviceCallback(audioDeviceCallback, null)

        // 注册音量变化广播接收器
        val filter = IntentFilter().apply {
            addAction("android.media.VOLUME_CHANGED_ACTION")
        }
        registerReceiver(volumeChangeReceiver, filter)

        val sleepFilter = IntentFilter().apply {
            addAction(BROADCAST_ACTION_SLEEPTIMER_UPDATE)
        }
        registerReceiver(sleepReceiver, sleepFilter, RECEIVER_NOT_EXPORTED)

        database = ConnectionRoomDatabase.getDatabase(applicationContext)
        connectionDao = database.connectionDao()
        startForeground(ID_NOTIFICATION_FOREGROUND, createForegroundNotification(context = applicationContext))
        notifyForegroundServiceState(true)
        Log.v("MUTE_TEST", "ON_CREATE_FINISH")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.v("MUTE_TEST", "ON_START_COMMAND")
        // 返回 START_STICKY，以确保 Service 在被终止后能够自动重启
        return START_STICKY
    }

    override fun onDestroy() {
        notifyForegroundServiceState(false)

        Log.v("MUTE_TEST", "ON_DESTROY")

        saveDataWhenStop()
        // 取消注册音量变化广播接收器
        unregisterReceiver(volumeChangeReceiver)
        unregisterReceiver(sleepReceiver)
        audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(ID_NOTIFICATION_FOREGROUND)
        Log.v("MUTE_TEST", "ON_DESTROY_FINISH")
        super.onDestroy()
    }

    override fun onBind(p0: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

    private fun notifyForegroundServiceState(isRunning: Boolean) {
        isForegroundServiceRunning = isRunning
        val intent = Intent(BROADCAST_ACTION_FOREGROUND)
        intent.putExtra(BROADCAST_FOREGROUND_INTENT_EXTRA, isRunning)
        sendBroadcast(intent)
    }

    @SuppressLint("LaunchActivityFromNotification")
    fun createForegroundNotification(context: Context): Notification {
        val currentVolume = getVolumePercentage(context)
        val currentVolumeLevel = getVolumeLevel(currentVolume)
        volumeComment = resources.getStringArray(R.array.array_volume_comment)
        val nIcon = generateNotificationIcon(context,
            getSharedPreferences(SHARED_PREF, Context.MODE_PRIVATE).getInt(PREF_ICON, MODE_IMG))

        val settingsIntent = Intent(this, SettingsReceiver::class.java).apply {
            action = ACTION_NAME_SETTINGS
        }
        val snoozePendingIntent: PendingIntent =
            PendingIntent.getBroadcast(this, 0, settingsIntent, PendingIntent.FLAG_IMMUTABLE)

        val actionSettings : NotificationCompat.Action = NotificationCompat.Action.Builder(
            R.drawable.ic_baseline_settings_24,
            resources.getString(R.string.settings),
            snoozePendingIntent
        ).build()

        val sharedPreferences = getSharedPreferences(
            SHARED_PREF,
            Context.MODE_PRIVATE
        )

        var protectionActionTitle = R.string.protection
        if (sharedPreferences != null){
            if (sharedPreferences.getBoolean(PREF_ENABLE_EAR_PROTECTION, false)){
                protectionActionTitle = R.string.dont_protect
            }
        }

        val protectionIntent = Intent(this, SettingsReceiver::class.java).apply {
            action = ACTION_TOGGLE_AUTO_CONNECTION_ADJUSTMENT
        }
        val protectionPendingIntent: PendingIntent =
            PendingIntent.getBroadcast(this, 0, protectionIntent, PendingIntent.FLAG_IMMUTABLE)

        val actionProtection : NotificationCompat.Action = NotificationCompat.Action.Builder(
            R.drawable.ic_headphones_protection,
            resources.getString(protectionActionTitle),
            protectionPendingIntent
        ).build()

        val sleepIntent = Intent(context, MuteMediaReceiver::class.java)
        sleepIntent.action = BROADCAST_ACTION_SLEEPTIMER_TOGGLE
        val pendingSleepIntent = PendingIntent.getBroadcast(context, 0, sleepIntent, PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val sleepNotification = find()
        var sleepTitle = resources.getString(R.string.sleep)
        if (sleepNotification != null ){
            sleepTitle = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(sleepNotification.`when`))
        }
        val actionSleepTimer: NotificationCompat.Action = NotificationCompat.Action.Builder (
            R.drawable.ic_tile,
            sleepTitle,
            pendingSleepIntent
        ).build()

        val muteMediaIntent = Intent(context, MuteMediaReceiver::class.java)
        muteMediaIntent.action = BROADCAST_ACTION_MUTE
        val pendingMuteIntent = PendingIntent.getBroadcast(context, 0, muteMediaIntent, PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        // 将 Service 设置为前台服务，并创建一个通知
        return NotificationCompat.Builder(this, CHANNEL_ID_DEFAULT)
            .setContentTitle(getString(R.string.to_be_or_not))
            .setOnlyAlertOnce(true)
            .setContentText(String.format(
                resources.getString(R.string.current_volume_percent),
                volumeComment[currentVolumeLevel],
                currentVolume))
            .setSmallIcon(nIcon)
            .setOngoing(true)
            .setContentIntent(pendingMuteIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(actionSettings)
            .addAction(actionSleepTimer)
            .addAction(actionProtection)
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
            .build()
    }

    @SuppressLint("DiscouragedApi")
    private fun generateNotificationIcon(context: Context, iconMode: Int): IconCompat {
        val currentVolume = getVolumePercentage(context)
        val currentVolumeLevel = getVolumeLevel(currentVolume)
        if (iconMode == MODE_NUM) {
            val resourceId = resources.getIdentifier("num_$currentVolume", "drawable", context.packageName)
            return IconCompat.createWithResource(this, resourceId)
        }
        else {
            return IconCompat.createWithResource(context, volumeDrawableIds[currentVolumeLevel])
        }
    }
}