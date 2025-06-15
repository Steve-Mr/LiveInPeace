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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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

    private val _showIconState = MutableStateFlow(false)
    val showIconState = _showIconState.asStateFlow()

    fun toggleShowIcon() {
        viewModelScope.launch {
            val packageManager = application.packageManager
            val componentName = ComponentName(application, "${application.packageName}.MainActivityAlias")

            val newState = if(!_showIconState.value) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            packageManager.setComponentEnabledSetting(componentName, newState, PackageManager.DONT_KILL_APP)
            _showIconState.value = !_showIconState.value
            preferenceRepository.setShowIcon(_showIconState.value)
        }
    }

    fun welcomeFinished() {
        viewModelScope.launch {
            preferenceRepository.setWelcomeFinished(true)
        }
        val intent = Intent(application, ForegroundService::class.java)
        startForegroundService(application, intent)
    }

    init {
        preferenceRepository.isShowingIcon().onEach {
            _showIconState.value = it
        }.launchIn(viewModelScope)
    }
}