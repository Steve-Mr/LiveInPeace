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
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.maary.liveinpeace.Constants
import com.maary.liveinpeace.Constants.Companion.EAR_PROTECTION_LOWER_THRESHOLD
import com.maary.liveinpeace.Constants.Companion.EAR_PROTECTION_UPPER_THRESHOLD
import com.maary.liveinpeace.DeviceTimer
import com.maary.liveinpeace.activity.MainActivity
import com.maary.liveinpeace.R
import com.maary.liveinpeace.SleepNotification.find
import com.maary.liveinpeace.database.Connection
import com.maary.liveinpeace.database.ConnectionDao
import com.maary.liveinpeace.database.ConnectionRoomDatabase
import com.maary.liveinpeace.database.PreferenceRepository
import com.maary.liveinpeace.receiver.MuteMediaReceiver
import com.maary.liveinpeace.receiver.SleepReceiver
import com.maary.liveinpeace.receiver.VolumeReceiver
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.DateFormat
import java.time.LocalDate
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * 前台服务，用于监控音频设备连接、实现护耳模式和管理通知。
 *
 * 重构要点:
 * 1.  **封装和内聚**: 将相关逻辑（如权限检查、通知更新）封装到独立的辅助函数中，提高代码复用性。
 * 2.  **常量化**: 消除魔法数字（如 PendingIntent 的请求码、设备类型 ID），代之以有意义的常量，增强可读性。
 * 3.  **代码简化**: 简化广播接收器的实现，避免代码重复。
 * 4.  **健壮性**: 在所有通知操作前添加统一的权限检查，确保在 Android 13+ 系统上的稳定性。
 * 5.  **可读性**: 优化函数和变量命名，增加注释，使代码意图更清晰。
 * 6.  **结构优化**: `audioDeviceCallback` 内部逻辑被拆分为更小、职责更单一的函数，降低了复杂度和嵌套。
 */
@AndroidEntryPoint
class ForegroundService : Service() {

    companion object {
        private const val TAG = "ForegroundService"

        // 为 PendingIntent 定义请求码常量，避免使用魔法数字
        private const val REQUEST_CODE_SETTINGS = 0
        private const val REQUEST_CODE_SLEEP_TIMER = 2
        private const val REQUEST_CODE_MUTE = 3

        // 为未知的设备类型 `28` 定义一个有意义的常量名
        private const val TYPE_UNKNOWN_DEVICE_28 = 28
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val audioManager: AudioManager by lazy {
        getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    @Inject
    lateinit var preferenceRepository: PreferenceRepository

    private lateinit var connectionDao: ConnectionDao
    private lateinit var volumeComment: Array<String>

    // 使用 ConcurrentHashMap 保证多线程访问的安全性
    private val deviceMap = ConcurrentHashMap<String, Connection>()
    private val deviceTimerMap = ConcurrentHashMap<String, DeviceTimer>()
    private val protectionJobs = ConcurrentHashMap<String, Job>()
    private val deviceMapMutex = Mutex()

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service creating...")

        initializeDependencies()
        registerReceiversAndCallbacks()

        // 启动前台服务，并立即更新一次通知状态
        startForegroundWithNotification()
        setServiceRunningState(true)

        Log.d(TAG, "Service created successfully.")
    }

    private fun initializeDependencies() {
        connectionDao = ConnectionRoomDatabase.getDatabase(applicationContext).connectionDao()
        volumeComment = resources.getStringArray(R.array.array_volume_comment)
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerReceiversAndCallbacks() {
        // 注册音频设备回调
        audioManager.registerAudioDeviceCallback(audioDeviceCallback, null)

        // 注册音量变化接收器
        val volumeFilter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        registerReceiver(volumeChangeReceiver, volumeFilter)

        // 注册休眠定时器更新接收器
        val sleepFilter = IntentFilter(Constants.BROADCAST_ACTION_SLEEPTIMER_UPDATE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(sleepReceiver, sleepFilter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(sleepReceiver, sleepFilter)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startForegroundWithNotification() {
        if (!hasNotificationPermission()) {
            Log.w(TAG, "Missing POST_NOTIFICATIONS permission. Cannot start foreground service with notification.")
            // 即使没有权限，也需要调用 startForeground，否则服务可能被系统杀死
            // 可以提供一个不含任何信息的最小化 Notification
            startForeground(Constants.ID_NOTIFICATION_FOREGROUND, NotificationCompat.Builder(this, Constants.CHANNEL_ID_DEFAULT).build())
            return
        }
        startForeground(Constants.ID_NOTIFICATION_FOREGROUND, createForegroundNotification(this))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand received.")
        // 确保服务被重新创建时，通知内容是最新的
        updateForegroundNotification()
        return START_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "Service destroying...")

        // 在清理资源前，先更新服务状态
        setServiceRunningState(false)

        saveDataForActiveConnections()
        cleanupResources()

        // 停止前台服务并移除通知
        stopForeground(STOP_FOREGROUND_REMOVE)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(Constants.ID_NOTIFICATION_FOREGROUND)

        Log.d(TAG, "Service destroyed.")
        super.onDestroy()
    }

    private fun cleanupResources() {
        // 取消所有由该服务启动的协程
        serviceScope.cancel()

        // 停止并清理所有设备计时器
        deviceTimerMap.values.forEach { it.stop() }
        deviceTimerMap.clear()

        // 安全地反注册所有接收器和回调
        safeUnregisterReceiver(volumeChangeReceiver)
        safeUnregisterReceiver(sleepReceiver)
        try {
            audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering audio callback", e)
        }
    }

    private fun safeUnregisterReceiver(receiver: android.content.BroadcastReceiver) {
        try {
            unregisterReceiver(receiver)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "${receiver::class.java.simpleName} was not registered or already unregistered.", e)
        }
    }

    private fun saveDataForActiveConnections() {
        val disconnectedTime = System.currentTimeMillis()
        val currentConnections = deviceMap.toMap() // 创建副本以安全遍历
        deviceMap.clear()

        currentConnections.forEach { (_, connection) ->
            val connectedTime = connection.connectedTime ?: return@forEach
            val connectionTime = disconnectedTime - connectedTime

            serviceScope.launch {
                try {
                    val finalConnection = connection.copy(
                        disconnectedTime = disconnectedTime,
                        duration = connectionTime
                    )
                    connectionDao.insert(finalConnection)
                    Log.d(TAG, "Saved connection data for ${connection.name}")
                } catch (e: Exception) {
                    Log.e(TAG, "Error saving connection data for ${connection.name}", e)
                }
            }
        }
        broadcastConnectionsUpdate()
    }

    /**
     * 音频设备连接状态的回调处理。
     * 内部逻辑被拆分为多个辅助函数，以提高清晰度和可维护性。
     */
    private val audioDeviceCallback = object : AudioDeviceCallback() {
        private val CALLBACK_TAG = "AudioDeviceCallback"
        private val VOLUME_ADJUST_ATTEMPTS = 100

        private val IGNORED_DEVICE_TYPES = setOf(
            AudioDeviceInfo.TYPE_BUILTIN_EARPIECE,
            AudioDeviceInfo.TYPE_BUILTIN_MIC,
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER_SAFE,
            AudioDeviceInfo.TYPE_FM_TUNER,
            AudioDeviceInfo.TYPE_REMOTE_SUBMIX,
            AudioDeviceInfo.TYPE_TELEPHONY,
            TYPE_UNKNOWN_DEVICE_28 // 使用常量代替魔法数字
        )

        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            if (addedDevices.isNullOrEmpty()) return

            serviceScope.launch {
                val isWatchingEnabled = preferenceRepository.getWatchingState().first()
                val isEarProtectionOn = preferenceRepository.isEarProtectionOn().first()

                addedDevices.filterNot { shouldIgnoreDevice(it) }.forEach { deviceInfo ->
                    val deviceName = getDeviceName(deviceInfo)
                    val wasAdded = processNewDevice(deviceInfo, deviceName, isWatchingEnabled)
                    if (wasAdded && isEarProtectionOn) {
                        applyEarProtection(deviceName)
                    }
                }
                onDeviceListChanged()
            }
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            if (removedDevices.isNullOrEmpty()) return

            serviceScope.launch {
                removedDevices.filterNot { shouldIgnoreDevice(it) }.forEach { deviceInfo ->
                    val deviceName = getDeviceName(deviceInfo)
                    processRemovedDevice(deviceName)?.let { connectionToSave ->
                        saveConnectionToDatabase(connectionToSave)
                    }
                }
                onDeviceListChanged()
            }
        }

        private fun shouldIgnoreDevice(deviceInfo: AudioDeviceInfo): Boolean {
            val deviceName = getDeviceName(deviceInfo)
            // 忽略本机、无名设备或特定类型的设备
            return deviceName.isEmpty() || deviceName == Build.MODEL || deviceInfo.type in IGNORED_DEVICE_TYPES
        }

        private fun getDeviceName(deviceInfo: AudioDeviceInfo): String {
            return deviceInfo.productName?.toString()?.trim() ?: ""
        }

        private suspend fun processNewDevice(
            deviceInfo: AudioDeviceInfo,
            deviceName: String,
            isWatching: Boolean
        ): Boolean {
            var wasAdded = false
            deviceMapMutex.withLock {
                if (!deviceMap.containsKey(deviceName)) {
                    Log.d(CALLBACK_TAG, "Device Added: $deviceName")
                    deviceMap[deviceName] = Connection(
                        name = deviceName,
                        type = deviceInfo.type,
                        connectedTime = System.currentTimeMillis(),
                        disconnectedTime = null,
                        duration = null,
                        date = LocalDate.now().toString()
                    )
                    wasAdded = true

                    if (isWatching) {
                        Log.d(CALLBACK_TAG, "Starting timer for $deviceName")
                        DeviceTimer(applicationContext, deviceName).also {
                            it.start()
                            deviceTimerMap[deviceName] = it
                        }
                    }
                }
            }
            return wasAdded
        }

        private suspend fun processRemovedDevice(deviceName: String): Connection? {
            var connectionToSave: Connection? = null
            deviceMapMutex.withLock {
                if (deviceMap.containsKey(deviceName)) {
                    Log.d(CALLBACK_TAG, "Device Removed: $deviceName")

                    // 停止相关任务和计时器
                    protectionJobs.remove(deviceName)?.cancel()
                    deviceTimerMap.remove(deviceName)?.stop()

                    val connection = deviceMap.remove(deviceName)
                    if (connection?.connectedTime != null) {
                        val disconnectedTime = System.currentTimeMillis()
                        val duration = disconnectedTime - connection.connectedTime
                        connectionToSave = connection.copy(
                            disconnectedTime = disconnectedTime,
                            duration = duration
                        )
                        if (duration > Constants.ALERT_TIME) {
                            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                            notificationManager.cancel(Constants.ID_NOTIFICATION_ALERT)
                        }
                    }
                }
            }
            return connectionToSave
        }

        private suspend fun saveConnectionToDatabase(connection: Connection) {
            try {
                connectionDao.insert(connection)
                Log.d(CALLBACK_TAG, "Saved connection data for ${connection.name}")
            } catch (e: Exception) {
                Log.e(CALLBACK_TAG, "Error saving connection data", e)
            }
        }

        private fun applyEarProtection(deviceName: String) {
            val protectionJob = serviceScope.launch {
                try {
                    Log.d(CALLBACK_TAG, "Applying ear protection for $deviceName")
                    var protectionApplied = false

                    val threshold = preferenceRepository.getEarProtectionThreshold().first()

                    // 调整音量到安全范围
                    while (getVolumePercentage() > threshold.last && isActive) {
                        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, 0)
                        protectionApplied = true
                    }
                    while (getVolumePercentage() < threshold.first && isActive) {
                        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, 0)
                        protectionApplied = true
                    }

                    if (protectionApplied) {
                        Log.d(CALLBACK_TAG, "Ear protection applied for $deviceName.")
                        showProtectionNotification()
                    }
                } catch (e: CancellationException) {
                    Log.d(CALLBACK_TAG, "Protection job for $deviceName was cancelled.")
                } finally {
                    protectionJobs.remove(deviceName)
                }
            }
            protectionJobs[deviceName] = protectionJob
        }
    }

    // --- 辅助函数 ---

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * 封装权限检查逻辑，提高代码复用性。
     */
    private fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true
        }
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 统一更新前台服务通知的入口。
     */
    @SuppressLint("MissingPermission")
    private fun updateForegroundNotification() {
        if (!hasNotificationPermission()) {
            Log.w(TAG, "Cannot update notification: Permission denied.")
            return
        }
        NotificationManagerCompat.from(this).notify(
            Constants.ID_NOTIFICATION_FOREGROUND,
            createForegroundNotification(this)
        )
    }

    /**
     * 显示护耳模式已应用的通知。
     */
    @SuppressLint("MissingPermission")
    private fun showProtectionNotification() {
        if (!hasNotificationPermission()) return
        val notification = NotificationCompat.Builder(this, Constants.CHANNEL_ID_PROTECT)
            .setContentTitle(getString(R.string.ears_protected))
            .setSmallIcon(R.drawable.ic_headphones_protection)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setGroup(Constants.ID_NOTIFICATION_GROUP_PROTECT)
            .setTimeoutAfter(3000)
            .build()
        NotificationManagerCompat.from(this).notify(Constants.ID_NOTIFICATION_PROTECT, notification)
    }

    private fun onDeviceListChanged() {
        broadcastConnectionsUpdate()
        updateForegroundNotification()
    }

    private fun broadcastConnectionsUpdate() {
        val intent = Intent(Constants.BROADCAST_ACTION_CONNECTIONS_UPDATE).apply {
            putParcelableArrayListExtra(
                Constants.EXTRA_CONNECTIONS_LIST,
                ArrayList(deviceMap.values)
            )
        }
        sendBroadcast(intent)
    }

    private fun setServiceRunningState(isRunning: Boolean) {
        serviceScope.launch {
            preferenceRepository.setServiceRunning(isRunning)
        }
        val intent = Intent(Constants.BROADCAST_ACTION_FOREGROUND).apply {
            putExtra(Constants.BROADCAST_FOREGROUND_INTENT_EXTRA, isRunning)
        }
        sendBroadcast(intent)
        Log.d(TAG, "Service running state set to $isRunning and broadcast sent.")
    }

    private fun getVolumePercentage(): Int {
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        return if (maxVolume > 0) 100 * currentVolume / maxVolume else 0
    }

    private fun getVolumeLevel(percent: Int): Int {
        return when (percent) {
            0 -> 0
            in 1..25 -> 1
            in 26..50 -> 2
            in 51..80 -> 3
            else -> 4
        }
    }

    // --- 广播接收器 ---

    private val volumeChangeReceiver = object : VolumeReceiver() {
        override fun updateNotification(context: Context) {
            updateForegroundNotification()
        }
    }

    private val sleepReceiver = object : SleepReceiver() {
        override fun updateNotification(context: Context) {
            updateForegroundNotification()
        }
    }

    // --- 通知创建 ---

    private fun createForegroundNotification(context: Context): Notification {
        val currentVolume = getVolumePercentage()
        val volumeLevel = getVolumeLevel(currentVolume)
        val comment = volumeComment.getOrElse(volumeLevel) { "Volume" }

        val contentText = String.format(
            resources.getString(R.string.current_volume_percent),
            comment,
            currentVolume
        )

        return NotificationCompat.Builder(this, Constants.CHANNEL_ID_DEFAULT)
            .setContentTitle(getString(R.string.to_be_or_not))
            .setContentText(contentText)
            .setSmallIcon(generateNotificationIcon(context, currentVolume, volumeLevel))
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setContentIntent(createMutePendingIntent(context))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setGroup(Constants.ID_NOTIFICATION_GROUP_FORE)
            .addAction(createSettingsAction(context))
            .addAction(createSleepTimerAction(context))
            .build()
    }

    private fun createSettingsAction(context: Context): NotificationCompat.Action {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_SETTINGS,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Action.Builder(
            R.drawable.ic_baseline_settings_24,
            resources.getString(R.string.settings),
            pendingIntent
        ).build()
    }

    private fun createSleepTimerAction(context: Context): NotificationCompat.Action {
        val sleepNotification = find()
        val sleepTitle = sleepNotification?.let {
            DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(it.`when`))
        } ?: resources.getString(R.string.sleep)

        val intent = Intent(context, MuteMediaReceiver::class.java).apply {
            action = Constants.BROADCAST_ACTION_SLEEPTIMER_TOGGLE
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_SLEEP_TIMER,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Action.Builder(R.drawable.ic_tile, sleepTitle, pendingIntent).build()
    }

    private fun createMutePendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MuteMediaReceiver::class.java).apply {
            action = Constants.BROADCAST_ACTION_MUTE
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_MUTE,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    @SuppressLint("DiscouragedApi")
    private fun generateNotificationIcon(context: Context, volumePercent: Int, volumeLevel: Int): IconCompat {
        val resourceName = "num_${volumePercent}"
        val resourceId = resources.getIdentifier(resourceName, "drawable", context.packageName)

        return if (resourceId != 0) {
            IconCompat.createWithResource(this, resourceId)
        } else {
            val fallbackIconRes = when(volumeLevel) {
                0 -> R.drawable.ic_volume_silent
                1 -> R.drawable.ic_volume_low
                2 -> R.drawable.ic_volume_middle
                3 -> R.drawable.ic_volume_high
                else -> R.drawable.ic_volume_mega
            }
            IconCompat.createWithResource(context, fallbackIconRes)
        }
    }
}
