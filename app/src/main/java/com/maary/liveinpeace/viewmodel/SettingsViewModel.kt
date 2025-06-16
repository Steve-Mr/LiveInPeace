package com.maary.liveinpeace.viewmodel

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maary.liveinpeace.Constants.Companion.EAR_PROTECTION_LOWER_THRESHOLD
import com.maary.liveinpeace.Constants.Companion.EAR_PROTECTION_UPPER_THRESHOLD
import com.maary.liveinpeace.database.PreferenceRepository
import com.maary.liveinpeace.service.ForegroundService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val application: Context,
    private val preferenceRepository: PreferenceRepository
): ViewModel() {

    val foregroundSwitchState: StateFlow<Boolean> = preferenceRepository.isServiceRunning()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun foregroundSwitch() {
        if(!foregroundSwitchState.value) {
            startForegroundService()
        } else {
            stopForegroundService()
        }
    }

    private fun startForegroundService() {
        viewModelScope.launch {
            val intent = Intent(application, ForegroundService::class.java)
            application.startForegroundService(intent)
        }
    }

    private fun stopForegroundService() {
        viewModelScope.launch {
            val intent = Intent(application, ForegroundService::class.java)
            application.stopService(intent)
        }
    }

    val alertSwitchState: StateFlow<Boolean> = preferenceRepository.getWatchingState()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun alertSwitch() {
        viewModelScope.launch {
            preferenceRepository.setWatchingState(!alertSwitchState.value)
        }
    }

    val protectionSwitchState: StateFlow<Boolean> = preferenceRepository.isEarProtectionOn()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun protectionSwitch() {
        viewModelScope.launch {
            preferenceRepository.setEarProtection(!protectionSwitchState.value)
        }
    }

    val showIconState: StateFlow<Boolean> = preferenceRepository.isHideInLauncher()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun toggleShowIcon() {
        viewModelScope.launch {
            val newState = !showIconState.value

            // 1. 执行系统操作
            val packageManager = application.packageManager
            val componentName = ComponentName(application, "${application.packageName}.MainActivityAlias")
            val enabledState = if (newState) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            packageManager.setComponentEnabledSetting(componentName, enabledState, PackageManager.DONT_KILL_APP)

            // 2. 将新状态通知 Repository
            preferenceRepository.setShowIcon()
        }
    }

    val earProtectionThreshold: StateFlow<IntRange> = preferenceRepository.getEarProtectionThreshold()
        .stateIn(
            scope = viewModelScope,
            // 当 UI 订阅时开始收集数据，并在最后一个订阅者消失 5 秒后停止，以节省资源
            started = SharingStarted.WhileSubscribed(5000),
            // 提供一个初始值，它只在仓库的真实值返回之前短暂使用
            initialValue = EAR_PROTECTION_LOWER_THRESHOLD..EAR_PROTECTION_UPPER_THRESHOLD
        )

    fun setEarProtectionThreshold(range: IntRange) {
        viewModelScope.launch {
            preferenceRepository.setEarProtectionThreshold(range)
        }
    }

}