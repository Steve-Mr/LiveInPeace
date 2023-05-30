package com.maary.liveinpeace.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.maary.liveinpeace.Constants.Companion.ACTION_NAME_SETTINGS
import com.maary.liveinpeace.Constants.Companion.ALERT_TIME
import com.maary.liveinpeace.Constants.Companion.BROADCAST_ACTION_MUTE
import com.maary.liveinpeace.Constants.Companion.CHANNEL_ID_ALERT
import com.maary.liveinpeace.Constants.Companion.CHANNEL_ID_DEFAULT
import com.maary.liveinpeace.Constants.Companion.ID_NOTIFICATION_ALERT
import com.maary.liveinpeace.Constants.Companion.ID_NOTIFICATION_FOREGROUND
import com.maary.liveinpeace.Constants.Companion.ID_NOTIFICATION_GROUP_ALERTS
import com.maary.liveinpeace.Constants.Companion.ID_NOTIFICATION_GROUP_FORE
import com.maary.liveinpeace.Constants.Companion.MODE_IMG
import com.maary.liveinpeace.Constants.Companion.MODE_NUM
import com.maary.liveinpeace.Constants.Companion.PREF_ICON
import com.maary.liveinpeace.Constants.Companion.PREF_WATCHING_CONNECTING_TIME
import com.maary.liveinpeace.Constants.Companion.SHARED_PREF
import com.maary.liveinpeace.HistoryActivity
import com.maary.liveinpeace.R
import com.maary.liveinpeace.database.Connection
import com.maary.liveinpeace.database.ConnectionDao
import com.maary.liveinpeace.database.ConnectionRoomDatabase
import com.maary.liveinpeace.receiver.MuteMediaReceiver
import com.maary.liveinpeace.receiver.SettingsReceiver
import com.maary.liveinpeace.receiver.VolumeReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.properties.Delegates

class ForegroundService: Service() {

    private lateinit var database: ConnectionRoomDatabase
    private lateinit var connectionDao: ConnectionDao

    private val deviceTimerMap: MutableMap<String, DeviceTimer> = mutableMapOf()
    private val deviceMap: MutableMap<String, Connection> = mutableMapOf()

    private val volumeDrawableIds = intArrayOf(
        R.drawable.ic_volume_silent,
        R.drawable.ic_volume_low,
        R.drawable.ic_volume_middle,
        R.drawable.ic_volume_high,
        R.drawable.ic_volume_mega
    )

    private var iconMode by Delegates.notNull<Int>()

    private lateinit var volumeComment: Array<String>

    private lateinit var audioManager: AudioManager

    companion object {
        private var isForegroundServiceRunning = false

        @JvmStatic
        fun isForegroundServiceRunning(): Boolean {
            return isForegroundServiceRunning
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
            with(NotificationManagerCompat.from(applicationContext)){
                notify(ID_NOTIFICATION_FOREGROUND, createForegroundNotification(applicationContext))
            }
        }
    }

    private fun deGroupNotification(context: Context){
        var isGrouped = false
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val activeNotifications = notificationManager.activeNotifications
        for (notification in activeNotifications) {
            if (notification.isGroup){
                isGrouped = true
                Log.v("MUTE_NOTIFICATION", "VOLUME CHANGER")
                break
            }
        }
        if (isGrouped) {
            notificationManager.cancelAll()
        }
    }

    private val audioDeviceCallback = object : AudioDeviceCallback() {
        @SuppressLint("MissingPermission")
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {

            val connectedTime = System.currentTimeMillis()

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
                if (deviceName == android.os.Build.MODEL) return@forEach
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
                // 执行其他逻辑，比如将设备信息保存到数据库或日志中
            }

            val sharedPreferences = getSharedPreferences(SHARED_PREF, Context.MODE_PRIVATE)
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

    override fun onCreate() {
        super.onCreate()

        iconMode = getSharedPreferences(SHARED_PREF, Context.MODE_PRIVATE).getInt(PREF_ICON, MODE_IMG)

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.registerAudioDeviceCallback(audioDeviceCallback, null)

        // 注册音量变化广播接收器
        val filter = IntentFilter().apply {
            addAction("android.media.VOLUME_CHANGED_ACTION")
        }
        registerReceiver(volumeChangeReceiver, filter)

        database = ConnectionRoomDatabase.getDatabase(applicationContext)
        connectionDao = database.connectionDao()
    }

    override fun onDestroy() {
        super.onDestroy()

        // 取消注册音量变化广播接收器
        unregisterReceiver(volumeChangeReceiver)
        audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)
        isForegroundServiceRunning = false
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(ID_NOTIFICATION_FOREGROUND, createForegroundNotification(context = applicationContext))
        isForegroundServiceRunning = true

        // 返回 START_STICKY，以确保 Service 在被终止后能够自动重启
        return START_STICKY
    }

    override fun onBind(p0: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

    @SuppressLint("LaunchActivityFromNotification")
    fun createForegroundNotification(context: Context): Notification {
        val currentVolume = getVolumePercentage(context)
        val currentVolumeLevel = getVolumeLevel(currentVolume)
        volumeComment = resources.getStringArray(R.array.array_volume_comment)
        val nIcon = generateNotificationIcon(context, iconMode)

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

        val historyIntent = Intent(this, HistoryActivity::class.java)
        historyIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        val pendingHistoryIntent = PendingIntent.getActivity(context, 0, historyIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val actionHistory: NotificationCompat.Action = NotificationCompat.Action.Builder(
            R.drawable.ic_action_history,
            resources.getString(R.string.history),
            pendingHistoryIntent
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
            .addAction(actionHistory)
            .setGroup(ID_NOTIFICATION_GROUP_FORE)
            .setGroupSummary(false)
            .build()
    }

    private fun generateNotificationIcon(context: Context, iconMode: Int): IconCompat {
        val currentVolume = getVolumePercentage(context)
        val currentVolumeLevel = getVolumeLevel(currentVolume)
        if (iconMode == MODE_NUM){
            var count = currentVolume
            var isCountLow = false
            var isCountHigh = false
            val immutableBackground = BitmapFactory.decodeResource(context.resources,
                R.drawable.ic_notification_background
            )
            val background = immutableBackground.copy(Bitmap.Config.ARGB_8888, true)

            val paint = Paint().apply {
                color = Color.WHITE
                textSize = 100f
                typeface = context.resources.getFont(R.font.ndot_45)
                isFakeBoldText = true
                isAntiAlias = true
            }

            if (count == 100){
                count = 99
                isCountHigh = true
            }

            if (count < 10) {
                count += 10
                isCountLow = true
            }

            val textBounds = Rect()
            paint.getTextBounds(count.toString(), 0, count.toString().length, textBounds)
            val textWidth = textBounds.width()
            val textHeight = textBounds.height()

            val canvas = Canvas(background)
            val canvasWidth = canvas.width
            val canvasHeight = canvas.height
            val textSize = (canvasWidth / textWidth * textHeight).coerceAtMost(canvasHeight)
            paint.textSize = textSize.toFloat()

            if (isCountLow){
                count-=10
            }

            var textToDraw = count.toString()
            if (isCountHigh) textToDraw = "!!"
            paint.getTextBounds(count.toString(), 0, count.toString().length, textBounds)
            canvas.drawText(textToDraw, (canvasWidth - textBounds.width()) / 2f, (canvasHeight + textBounds.height()) / 2f, paint)

            return IconCompat.createWithBitmap(background)
        }
        else {
            return IconCompat.createWithResource(context, volumeDrawableIds[currentVolumeLevel])
        }
    }

    class DeviceTimer(private val context: Context, private val deviceName: String) : Runnable {
        private val handler = Handler(Looper.getMainLooper())
        private var isRunning = false
        private var notifyCount = 0

        @SuppressLint("MissingPermission")
        override fun run() {
            // 延迟一定时间后再执行任务
            val delayMillis = ALERT_TIME

            if (notifyCount == 0) {
                // 第一次执行，延迟 ALERT_TIME 时间后执行 notify
                handler.postDelayed({
                    with(NotificationManagerCompat.from(context)) {
                        notify(ID_NOTIFICATION_ALERT, createTimerNotification(context = context, deviceName = deviceName))
                    }
                    notifyCount++
                    // 继续执行后续任务
                    handler.postDelayed(this, delayMillis)
                }, delayMillis)
            } else if (notifyCount < MAX_NOTIFY_COUNT) {
                // 后续执行，达到通知次数后停止任务
                with(NotificationManagerCompat.from(context)) {
                    notify(ID_NOTIFICATION_ALERT, createTimerNotification(context = context, deviceName = deviceName))
                }
                notifyCount++
                if (notifyCount < MAX_NOTIFY_COUNT) {
                    // 继续执行后续任务
                    handler.postDelayed(this, delayMillis)
                }
            } else {
                // 达到通知次数后停止任务执行
                stop()
                return
            }
        }

        fun start() {
            if (!isRunning) {
                isRunning = true
                notifyCount = 0
                handler.post(this)
                Log.v("MUTE_TIMER", "TIMER_STARTED")
            }
        }

        fun stop() {
            if (isRunning) {
                isRunning = false
                notifyCount = 0
                handler.removeCallbacks(this)
            }
        }

        companion object {
            private const val MAX_NOTIFY_COUNT = 1
        }

        private fun createTimerNotification(context: Context, deviceName: String) : Notification {
            return NotificationCompat.Builder(context, CHANNEL_ID_ALERT)
                .setContentTitle(context.getString(R.string.alert))
                .setContentText(String.format(
                    context.resources.getString(R.string.device_connected_too_long),
                    deviceName
                ))
                .setSmallIcon(R.drawable.ic_headphone)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setGroupSummary(false)
                .setGroup(ID_NOTIFICATION_GROUP_ALERTS)
                .build()
        }
    }
}