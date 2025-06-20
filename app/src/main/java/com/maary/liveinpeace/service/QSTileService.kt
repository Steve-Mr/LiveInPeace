package com.maary.liveinpeace.service

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import androidx.annotation.RequiresApi
import com.maary.liveinpeace.Constants
import com.maary.liveinpeace.R
import com.maary.liveinpeace.activity.WelcomeActivity
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

    @SuppressLint("StartActivityAndCollapseDeprecated")
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onClick() {
        super.onClick()
        val tile = qsTile

        var intent = Intent(this, WelcomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        serviceScope.launch {
            if (!preferenceRepository.isWelcomeFinished().first()) {
                val pendingIntent = PendingIntent.getActivity(
                    this@QSTileService,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startActivityAndCollapse(pendingIntent)
                } else {
                    startActivityAndCollapse(intent)
                }
                return@launch
            }
            intent = Intent(this@QSTileService, ForegroundService::class.java)
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

        // --- 核心校准逻辑开始 ---
        // 每次磁贴可见时，检查持久化状态和内存状态是否一致
        serviceScope.launch {
            // 从 PreferenceRepository 读取“预期状态”
            val expectedState = preferenceRepository.isServiceRunning().first()
            // 直接从内存中读取服务的“实际状态”
            val actualState = ForegroundService.isRunning
            // 对比两个状态
            if (expectedState && !actualState) {
                // **发现不一致！**
                // 记录显示服务在运行，但内存中它已停止。
                // 这几乎可以肯定是服务被系统强杀了。
                // 1. 修正错误的持久化记录
                preferenceRepository.setServiceRunning(false)
                // 2. 用修正后的、正确的状态（false）来更新磁贴外观
                updateTileState(false)
            } else {
                // 状态一致，一切正常。直接按预期状态更新磁贴即可。
                updateTileState(expectedState)
            }
        }
        // --- 核心校准逻辑结束 ---

        val intentFilter = IntentFilter()
        intentFilter.addAction(Constants.BROADCAST_ACTION_FOREGROUND)
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
}