package com.maary.liveinpeace.viewmodel

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.PowerManager
import androidx.core.content.ContextCompat.startForegroundService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maary.liveinpeace.database.PreferenceRepository
import com.maary.liveinpeace.service.ForegroundService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WelcomeViewModel @Inject constructor(
    @ApplicationContext private val application: Context,
    private val preferenceRepository: PreferenceRepository
): ViewModel() {

    private val _hasNotificationPermission = MutableStateFlow(false)
    val hasNotificationPermission = _hasNotificationPermission.asStateFlow()

    fun onPermissionResult(isGranted: Boolean) {
        _hasNotificationPermission.value = isGranted
    }

    private val _isIgnoringBatteryOptimizations = MutableStateFlow(false)
    val isIgnoringBatteryOptimizations = _isIgnoringBatteryOptimizations.asStateFlow()

    fun checkBatteryOptimizationStatus() {
        val powerManager = application.getSystemService(Context.POWER_SERVICE) as PowerManager
        val packageName = application.packageName

        _isIgnoringBatteryOptimizations.value = powerManager.isIgnoringBatteryOptimizations(packageName)
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

    fun welcomeFinished() {
        viewModelScope.launch {
            preferenceRepository.setWelcomeFinished(true)
        }
        val intent = Intent(application, ForegroundService::class.java)
        startForegroundService(application, intent)
    }

}